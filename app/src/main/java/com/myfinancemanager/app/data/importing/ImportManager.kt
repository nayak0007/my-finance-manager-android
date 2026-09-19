package com.myfinancemanager.app.data.importing

import com.myfinancemanager.app.data.local.StatementSource
import com.myfinancemanager.app.data.local.StoredStatement
import com.myfinancemanager.app.data.local.StatementStore
import com.myfinancemanager.app.data.local.dao.ImportBatchDao
import com.myfinancemanager.app.data.local.entity.ImportBatchEntity
import com.myfinancemanager.app.data.remote.FinanceApi
import com.myfinancemanager.app.data.remote.ImportCommitBody
import com.myfinancemanager.app.data.remote.ImportCommitItem
import com.myfinancemanager.app.data.remote.RemoteImportBatch
import com.myfinancemanager.app.data.remote.RemoteImportedTransaction
import com.myfinancemanager.app.util.Dates
import com.myfinancemanager.app.util.Ids
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.IOException

/** A batch as the review UI sees it: the local row plus the server's staged rows. */
data class ImportUiBatch(
    val local: ImportBatchEntity,
    val transactions: List<RemoteImportedTransaction> = emptyList()
)

/** The status a cancelled batch carries, here and on the server. */
const val CANCELLED_IMPORT_STATUS = "cancelled"

/** Outcome of the upload+parse phase, for the UI to react to. */
sealed interface ImportUploadResult {
    /** The parse finished; the batch is ready for review (rows arrive via [loadDetail]). */
    data class Ready(val batch: ImportBatchEntity) : ImportUploadResult

    /** The server could not read the file; [reason] explains why. */
    data class Failed(val reason: String) : ImportUploadResult

    /** The file is on its way to the server; review becomes possible after it parses. */
    data class Queued(val batch: ImportBatchEntity) : ImportUploadResult

    /** The user aborted the import (or cancelled it from another device) while it was parsing. */
    object Cancelled : ImportUploadResult
}

/**
 * The whole statement-import flow, driven by the backend.
 *
 * The phone no longer parses statements. A picked file is copied into [StatementStore] (the
 * picker's Uri is revoked right after the pick, so the copy is what makes retries possible),
 * uploaded to `POST /api/v1/imports`, and the server parses it with OpenRouter over the text it
 * extracts itself. The staged rows are reviewed here and committed with `POST /{id}/commit`; the
 * backend writes the records, and they reach every device through the normal record pull.
 *
 * One import runs at a time. [importPickedFile] therefore does not return until the parse has
 * finished or failed — the caller shows a spinner with a cancel affordance for as long as that
 * takes — and [cancel] is what ends it early.
 */
class ImportManager(
    private val api: FinanceApi,
    private val importBatchDao: ImportBatchDao,
    private val statementStore: StatementStore
) {
    /**
     * Handles a picked file end-to-end: copy, upload, wait for the parse.
     *
     * The batch row is created before the upload so an offline pick is still recorded; any
     * dirty batch's private copy is uploaded by the sync engine on a later pass.
     */
    suspend fun importPickedFile(userId: String, uri: android.net.Uri, fileName: String): ImportUploadResult {
        val stored = statementStore.save(userId, StatementSource.Picked(uri, fileName))
            ?: return ImportUploadResult.Failed("The file could not be copied for upload")
        val batch = createBatch(userId, fileName, stored)
        return uploadAndAwait(batch)
    }

    /** Runs the same flow for statement text the app supplies (the bundled sample CSV). */
    suspend fun importContent(userId: String, fileName: String, content: String): ImportUploadResult {
        val stored = statementStore.save(userId, StatementSource.Inline(fileName, content))
            ?: return ImportUploadResult.Failed("The statement could not be prepared for upload")
        val batch = createBatch(userId, fileName, stored)
        return uploadAndAwait(batch)
    }

    /**
     * Aborts an import the user no longer wants.
     *
     * A batch the server already knows about is cancelled there — the parse's result is
     * discarded and its staged rows are dropped — and the local row is kept in the history
     * flagged `cancelled`. A batch that never reached the server (a pick taken offline, or an
     * upload that was still in flight) is simply forgotten, so no later sync uploads it.
     *
     * @return false when the server could not be reached, in which case nothing has changed and
     *         the caller should leave the import exactly as it was.
     */
    suspend fun cancel(batch: ImportBatchEntity): Boolean {
        // Re-read: the row may have learned its server id while an upload was in flight.
        val current = importBatchDao.findById(batch.id) ?: return true
        val remoteId = current.remoteId
            ?: run {
                // Nothing on the server to cancel, so there is no history entry worth keeping.
                statementStore.delete(current.localPath)
                importBatchDao.delete(current.id)
                return true
            }

        val cancelled = try {
            api.cancelImport(remoteId)
            true
        } catch (e: HttpException) {
            // 404/405: a backend without the cancel route, or a batch that is already gone.
            // Deleting it server-side ends the import just as well.
            if (e.code() == 404 || e.code() == 405) {
                deleteQuietly(remoteId)
                true
            } else {
                false
            }
        } catch (e: IOException) {
            false
        }
        if (!cancelled) return false

        statementStore.delete(current.localPath)
        importBatchDao.update(
            current.copy(status = CANCELLED_IMPORT_STATUS, errorMessage = null, dirty = false)
        )
        return true
    }

    /**
     * Re-reads one batch's status from the server and mirrors it locally, for a parse that was
     * left running when the import screen went away. Returns null when the server is unreachable.
     */
    suspend fun refreshStatus(batch: ImportBatchEntity): ImportBatchEntity? {
        val remoteId = batch.remoteId ?: return null
        return try {
            applyRemoteState(batch, api.getImport(remoteId))
        } catch (e: IOException) {
            null
        }
    }

    /** Fetches the staged rows of a batch that has reached READY_FOR_REVIEW. */
    suspend fun loadDetail(batch: ImportBatchEntity): ImportUiBatch {
        val remoteId = batch.remoteId
            ?: throw IOException("This statement has not reached the server yet")
        val remote = api.getImportDetail(remoteId)
        val remoteBatch = remote.batch
            ?: throw IOException("The server returned no batch for this statement")
        val updated = applyRemoteState(batch, remoteBatch)
        return ImportUiBatch(local = updated, transactions = remote.transactions)
    }

    /**
     * Sends the review decisions to the backend, which writes one record per included row.
     *
     * Every pending staged row is listed: `include = true` for the ones the user kept, `false`
     * for the excluded ones — the server rejects the latter, so a later commit cannot revive
     * them. Returns the number of rows sent for writing.
     */
    suspend fun commit(batch: ImportBatchEntity, includedIds: Set<String>): Int {
        val remoteId = batch.remoteId
            ?: throw IOException("This statement has not reached the server yet")
        val detail = api.getImportDetail(remoteId)
        val pending = detail.transactions.filter { it.status.equals("PENDING", ignoreCase = true) }
        if (pending.isEmpty()) throw IOException("There are no rows left to commit")
        val items = pending.map { staged ->
            ImportCommitItem(
                id = staged.id,
                include = staged.id in includedIds
            )
        }
        val result = api.commitImport(remoteId, ImportCommitBody(items))
        applyRemoteState(batch, result)
        return items.count { it.include }
    }

    /** Deletes a batch (for example a failed parse) on the server and locally. */
    suspend fun discard(batch: ImportBatchEntity) {
        batch.remoteId?.let { deleteQuietly(it) }
        statementStore.delete(batch.localPath)
        importBatchDao.delete(batch.id)
    }

    // ---- Internals ------------------------------------------------------------------------

    private suspend fun createBatch(userId: String, fileName: String, stored: StoredStatement): ImportBatchEntity =
        ImportBatchEntity(
            id = Ids.new(),
            userId = userId,
            sourceFile = fileName,
            status = "queued",
            totalParsed = 0,
            committed = 0,
            createdAt = Dates.now(),
            localPath = stored.path,
            contentType = stored.contentType,
            fileSize = stored.size,
            dirty = true
        ).also { importBatchDao.insert(it) }

    private suspend fun uploadAndAwait(batch: ImportBatchEntity): ImportUploadResult {
        val path = batch.localPath
        val file = path?.let(::File)
        if (file == null || !file.exists()) {
            // The private copy is gone, so no retry can ever succeed.
            importBatchDao.markClean(batch.id)
            return ImportUploadResult.Failed("The file is no longer on this device")
        }
        if (file.length() > MAX_UPLOAD_BYTES) {
            importBatchDao.markClean(batch.id)
            return ImportUploadResult.Failed("The file is larger than the 25 MB upload limit")
        }
        val remote = try {
            api.uploadImport(multipartFor(batch, file))
        } catch (e: IOException) {
            // Offline: the row stays dirty, so the sync engine uploads the copy on the next pass.
            return ImportUploadResult.Queued(batch)
        }
        if (!isImportLive(batch.id)) {
            // Cancelled while the request was in flight. The server created a batch before the
            // cancel could know its id, so cleaning it up is this call's job.
            deleteQuietly(remote.id)
            return ImportUploadResult.Cancelled
        }
        importBatchDao.markSynced(batch.id, remote.id)
        return awaitParse(batch.copy(remoteId = remote.id), remote)
    }

    /**
     * Waits for the server-side parse, for as long as it takes.
     *
     * There is no attempt budget: the user is watching a spinner with a cancel button, and
     * stopping early would hand back an import that is neither cancelled nor finished. Losing
     * the network does not end the wait either — the poll retries, and the batch keeps its
     * server id, so the next sync's pull picks up the final state if this device gives up first.
     */
    private suspend fun awaitParse(batch: ImportBatchEntity, queued: RemoteImportBatch): ImportUploadResult {
        var current = queued
        while (true) {
            when (current.status?.uppercase()) {
                "READY_FOR_REVIEW", "COMMITTED", "PARTIALLY_COMMITTED" ->
                    return if (isImportLive(batch.id)) {
                        ImportUploadResult.Ready(applyRemoteState(batch, current))
                    } else {
                        ImportUploadResult.Cancelled
                    }
                "FAILED" -> return ImportUploadResult.Failed(
                    current.errorMessage ?: "The server could not read this statement"
                )
                "CANCELLED" -> return ImportUploadResult.Cancelled
            }
            delay(POLL_INTERVAL_MS)
            current = try {
                api.getImport(current.id)
            } catch (e: IOException) {
                // Transient; keep the last known state and try again.
                current
            }
        }
    }

    /**
     * True while the local row still exists and the user has not cancelled the import. A batch
     * cancelled mid-parse must never have its result written back into the row — that is what
     * would make a cancelled import reappear as ready for review.
     */
    private suspend fun isImportLive(batchId: String): Boolean {
        val row = importBatchDao.findById(batchId) ?: return false
        return !row.status.equals(CANCELLED_IMPORT_STATUS, ignoreCase = true)
    }

    /** Persists what the server reports and returns the updated local row. */
    private suspend fun applyRemoteState(
        batch: ImportBatchEntity,
        remote: RemoteImportBatch
    ): ImportBatchEntity {
        val updated = batch.copy(
            remoteId = remote.id,
            status = remote.status?.lowercase()?.ifBlank { batch.status } ?: batch.status,
            totalParsed = remote.totalTransactions,
            errorMessage = remote.errorMessage
        )
        importBatchDao.update(updated)
        return updated
    }

    private suspend fun deleteQuietly(remoteId: String) {
        try {
            api.deleteImport(remoteId)
        } catch (e: IOException) {
            // The server keeps a batch we could not delete; the history pull will still show it.
        }
    }

    /** The multipart field name `file` matches the backend's `@RequestParam("file")`. */
    private fun multipartFor(batch: ImportBatchEntity, file: File): MultipartBody.Part {
        val mediaType = (batch.contentType?.takeIf { it.isNotBlank() } ?: "application/octet-stream")
            .toMediaTypeOrNull()
        return MultipartBody.Part.createFormData(
            "file",
            batch.sourceFile.ifBlank { file.name },
            file.asRequestBody(mediaType)
        )
    }

    companion object {
        private const val POLL_INTERVAL_MS = 1_500L

        /** Matches `spring.servlet.multipart.max-request-size` on the backend (25MB). */
        private const val MAX_UPLOAD_BYTES = 25L * 1024 * 1024
    }
}
