package com.myfinancemanager.app.data.local

import android.content.Context
import android.net.Uri
import com.myfinancemanager.app.util.Ids
import java.io.File
import java.io.IOException

/**
 * Where a statement came from. The picker hands over a content Uri that is only guaranteed to be
 * readable right after the pick, so the file is copied into private storage while it is still
 * available; a sample loaded from assets arrives as text instead.
 */
sealed interface StatementSource {
    val fileName: String

    data class Picked(val uri: Uri, override val fileName: String) : StatementSource

    data class Inline(override val fileName: String, val content: String) : StatementSource
}

/** A private copy of an imported statement, ready to be uploaded. */
data class StoredStatement(val path: String, val contentType: String, val size: Long)

/**
 * Keeps a private copy of every imported statement under `files/statements/<userId>`.
 *
 * The copy is what makes the backend mirror possible at all: `POST /api/v1/imports` is a
 * multipart upload, and the file has to still be readable when the upload runs — possibly many
 * syncs later, after the content Uri has been revoked. It also means a failed upload is retried
 * rather than dropped.
 */
class StatementStore(private val context: Context) {

    fun save(userId: String, source: StatementSource): StoredStatement? {
        val directory = directoryFor(userId)
        if (!directory.exists() && !directory.mkdirs()) return null
        val target = File(directory, "${Ids.new()}-${source.fileName.sanitised()}")
        return try {
            when (source) {
                is StatementSource.Picked -> {
                    val input = context.contentResolver.openInputStream(source.uri) ?: return null
                    input.use { stream -> target.outputStream().use { stream.copyTo(it) } }
                }
                is StatementSource.Inline -> target.writeText(source.content)
            }
            StoredStatement(target.absolutePath, contentTypeOf(source.fileName), target.length())
        } catch (e: IOException) {
            target.delete()
            null
        }
    }

    fun deleteForUser(userId: String) {
        directoryFor(userId).deleteRecursively()
    }

    /** Removes one statement's private copy — used when its import is cancelled. */
    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).delete() }
    }

    private fun directoryFor(userId: String): File = File(context.filesDir, "statements/$userId")

    /**
     * The backend picks its parser by file extension, but the content type it forwards to the
     * external parsing provider should still be accurate.
     */
    private fun contentTypeOf(fileName: String): String = when (fileName.substringAfterLast('.', "").lowercase()) {
        "csv" -> "text/csv"
        "txt" -> "text/plain"
        "pdf" -> "application/pdf"
        "xls" -> "application/vnd.ms-excel"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        else -> "application/octet-stream"
    }

    private fun String.sanitised(): String =
        replace(Regex("[^A-Za-z0-9._-]"), "_").takeLast(120).ifBlank { "statement" }
}
