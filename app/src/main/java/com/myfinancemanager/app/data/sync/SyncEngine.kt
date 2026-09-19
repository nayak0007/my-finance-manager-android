package com.myfinancemanager.app.data.sync

import com.myfinancemanager.app.data.local.AppDatabase
import com.myfinancemanager.app.data.local.entity.AutoCaptureEntity
import com.myfinancemanager.app.data.local.entity.AutoCaptureStatus
import com.myfinancemanager.app.data.local.entity.BudgetEntity
import com.myfinancemanager.app.data.local.entity.ExpenseCategory
import com.myfinancemanager.app.data.local.entity.ExpenseEntity
import com.myfinancemanager.app.data.local.entity.ImportBatchEntity
import com.myfinancemanager.app.data.local.entity.IncomeCategory
import com.myfinancemanager.app.data.local.entity.IncomeEntity
import com.myfinancemanager.app.data.local.entity.InsightEntity
import com.myfinancemanager.app.data.local.entity.InvestmentEntity
import com.myfinancemanager.app.data.local.entity.InvestmentType
import com.myfinancemanager.app.data.local.entity.ParsedType
import com.myfinancemanager.app.data.local.entity.PaymentMode
import com.myfinancemanager.app.data.local.entity.RecordOrigin
import com.myfinancemanager.app.data.local.entity.RecordStatus
import com.myfinancemanager.app.data.local.entity.SenderRuleEntity
import com.myfinancemanager.app.data.local.entity.SyncKind
import com.myfinancemanager.app.data.local.entity.budgetIdFor
import com.myfinancemanager.app.data.prefs.UserPreferences
import com.myfinancemanager.app.data.remote.ApiErrors.messageFrom
import com.myfinancemanager.app.data.remote.AuthUser
import com.myfinancemanager.app.data.remote.AutoCaptureBody
import com.myfinancemanager.app.data.remote.AutoCaptureReviewBody
import com.myfinancemanager.app.data.remote.BudgetUpsertBody
import com.myfinancemanager.app.data.remote.CaptureSettingsBody
import com.myfinancemanager.app.data.remote.ExpenseBody
import com.myfinancemanager.app.data.remote.FinanceApi
import com.myfinancemanager.app.data.remote.GenerateInsightsBody
import com.myfinancemanager.app.data.remote.IncomeBody
import com.myfinancemanager.app.data.remote.InsightStatusBody
import com.myfinancemanager.app.data.remote.InvestmentBody
import com.myfinancemanager.app.data.remote.RemoteAutoCapture
import com.myfinancemanager.app.data.remote.RemoteBudget
import com.myfinancemanager.app.data.remote.RemoteExpense
import com.myfinancemanager.app.data.remote.RemoteImportBatch
import com.myfinancemanager.app.data.remote.RemoteIncome
import com.myfinancemanager.app.data.remote.RemoteInsight
import com.myfinancemanager.app.data.remote.RemoteInvestment
import com.myfinancemanager.app.data.remote.UpdateProfileBody
import com.myfinancemanager.app.util.Dates
import com.myfinancemanager.app.util.Ids
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Outcome of a single sync pass; surfaced to the user in Settings. */
data class SyncResult(
    val pushed: Int = 0,
    val pulled: Int = 0,
    val failed: Int = 0,
    val message: String? = null
) {
    val ok: Boolean get() = message == null
}

/**
 * Offline-first two-way sync for the three core record families (income, expenses,
 * investments). Room stays the source of truth for the UI; this engine reconciles it with
 * the backend.
 *
 * Each pass runs in a fixed order, all under one mutex so overlapping triggers (app start,
 * sync-now, a burst of edits) collapse into a single in-flight run:
 *
 *  1. **Tombstones** — push deletes first. A record deleted locally while offline must be
 *     removed server-side *before* the pull below, otherwise the pull would resurrect it.
 *  2. **Push** — send every row flagged `dirty`. Rows without a `remoteId` are created,
 *     rows with one are updated. The returned UUID is stored so a record is never pushed
 *     twice. The backend does not de-duplicate on create, so this bookkeeping stands in for
 *     the idempotency key the API does not have.
 *  3. **Pull** — page through the server's collection. Records are matched by `remoteId`,
 *     so a locally created record that was just pushed is updated in place rather than
 *     duplicated.
 *
 * Conflict rule is last-write-wins with local priority: a row that is still `dirty` after
 * the push means the server refused it, so the pull leaves it alone rather than silently
 * discarding what the user typed.
 *
 * The SMS review queue and statement imports ride along in the same pass:
 *
 *  - **Queue** — a captured SMS is mirrored as a pending item. Review is the one thing this
 *    engine does *not* decide locally: the backend writes the transaction on
 *    `POST /auto-capture/{id}/confirm`, so a confirm/reject is replayed here and the resulting
 *    record arrives through the record pull below. Creating it locally as well would count it
 *    twice. Rejects are replayed for the history the server keeps.
 *  - **Imports** — the local import already created its records, so the batch is mirrored as a
 *    backup of the statement file itself (`POST /imports`, multipart). The server re-parses the
 *    upload on its own, which is why its staged rows are never committed from here: that would
 *    be a second copy of every transaction the phone already pushed.
 *  - **Budgets** — one limit per category, keyed by the category in the URL, so re-sending a
 *    limit edits it rather than adding a second row.
 *  - **Settings** — the capture switches and sender lists live in one account document, and the
 *    notifications/insight-frequency pair rides on the profile. Each travels in exactly one
 *    direction per pass: push while the local copy is flagged dirty, pull when it is not. That is
 *    what stops two devices from writing over each other every time they sync.
 *  - **Insights** — generated by the backend (see [generateInsights]); only the save/dismiss
 *    decision is pushed from here.
 */
class SyncEngine(
    private val api: FinanceApi,
    private val db: AppDatabase,
    private val prefs: UserPreferences
) {
    private val mutex = Mutex()

    private val incomeDao = db.incomeDao()
    private val expenseDao = db.expenseDao()
    private val investmentDao = db.investmentDao()
    private val captureDao = db.autoCaptureDao()
    private val importDao = db.importBatchDao()
    private val budgetDao = db.budgetDao()
    private val insightDao = db.insightDao()
    private val senderRuleDao = db.senderRuleDao()
    private val syncDao = db.syncDao()

    private class Tally {
        var pushed = 0
        var pulled = 0
        var failed = 0

        /**
         * Reported when something was refused outright. Records that merely could not be reached
         * stay queued and are retried; ones the server rejects are dropped from the queue, so the
         * user has to be told rather than left believing everything was stored.
         */
        fun note(): String? =
            if (failed == 0) null else "$failed change(s) could not be saved to your account"
    }

    private enum class RemoteDelete { DONE, RETRY }

    /** Runs a full reconciliation pass. Safe to call concurrently; runs are serialised. */
    suspend fun sync(userId: String): SyncResult = mutex.withLock {
        val tally = Tally()
        try {
            flushTombstones(userId)
            pushIncomes(userId, tally)
            pushExpenses(userId, tally)
            pushInvestments(userId, tally)
            pushCaptures(userId, tally)
            pushImports(userId, tally)
            pushBudgets(userId, tally)
            pushInsights(userId, tally)
            syncCaptureSettings(userId, tally)
            syncProfileSettings(tally)
            // Record pulls come last so a capture confirmed a moment ago is already on the
            // server: the transaction it created then lands on the device in this same pass.
            pullIncomes(userId, tally)
            pullExpenses(userId, tally)
            pullInvestments(userId, tally)
            pullCaptures(userId, tally)
            pullImports(userId, tally)
            pullBudgets(userId, tally)
            pullInsights(userId, tally)
            SyncResult(tally.pushed, tally.pulled, tally.failed, tally.note())
        } catch (e: IOException) {
            SyncResult(
                tally.pushed,
                tally.pulled,
                tally.failed,
                "Offline — your changes are saved on this device and will sync later."
            )
        } catch (e: HttpException) {
            SyncResult(tally.pushed, tally.pulled, tally.failed, describe(e))
        } catch (e: Exception) {
            SyncResult(tally.pushed, tally.pulled, tally.failed, e.message ?: "Sync failed")
        }
    }

    // ---- Step 1: remote deletes ---------------------------------------------------------

    private suspend fun flushTombstones(userId: String) {
        syncDao.pending(userId).forEach { tombstone ->
            val remoteId = tombstone.remoteId
            // No remote id means it never reached the server, so the tombstone is spent.
            val settled = remoteId == null ||
                deleteRemote(tombstone.kind, remoteId) == RemoteDelete.DONE
            if (settled) syncDao.remove(tombstone.id)
        }
    }

    private suspend fun deleteRemote(kind: String, remoteId: String): RemoteDelete {
        val response: Response<Unit> = try {
            when (kind) {
                SyncKind.INCOME.name -> api.deleteIncome(remoteId)
                SyncKind.EXPENSE.name -> api.deleteExpense(remoteId)
                SyncKind.INVESTMENT.name -> api.deleteInvestment(remoteId)
                else -> return RemoteDelete.DONE
            }
        } catch (e: IOException) {
            // Still offline: keep the tombstone and let the next pass retry it.
            return RemoteDelete.RETRY
        }
        // Any 4xx counts as settled: 404 means it is already gone, and the rest are
        // payloads the server will never accept, so retrying them forever is pointless.
        return if (response.isSuccessful || response.code() in 400..499) {
            RemoteDelete.DONE
        } else {
            RemoteDelete.RETRY
        }
    }

    // ---- Step 2: push local changes -----------------------------------------------------

    private suspend fun pushIncomes(userId: String, tally: Tally) {
        incomeDao.pendingSync(userId).forEach { record ->
            if (record.amount <= 0.0 || record.source.isBlank()) {
                // The backend requires a strictly positive amount. Park it locally rather
                // than retrying a payload that can never succeed.
                incomeDao.markClean(record.id)
                tally.failed++
                return@forEach
            }
            val body = IncomeBody(
                amount = record.amount,
                source = record.source,
                category = record.category.name,
                transactionDate = Dates.toLocalDate(record.date).toString(),
                origin = record.origin.name,
                recurring = record.recurring,
                notes = record.notes
            )
            try {
                val remote = if (record.remoteId == null) {
                    api.createIncome(body)
                } else {
                    api.updateIncome(record.remoteId, body)
                }
                incomeDao.markSynced(record.id, remote.id, serverMillis(remote.updatedAt))
                tally.pushed++
            } catch (e: HttpException) {
                if (e.code() == 404 && record.remoteId != null) {
                    // Deleted server-side behind our back: re-create it under a new id.
                    if (recreateIncome(record, body, tally)) {
                        return@forEach
                    }
                    tally.failed++
                } else if (e.code() in 400..499) {
                    incomeDao.markClean(record.id)
                    tally.failed++
                } else {
                    tally.failed++
                }
            } catch (e: IOException) {
                tally.failed++
            }
        }
    }

    private suspend fun recreateIncome(record: IncomeEntity, body: IncomeBody, tally: Tally): Boolean =
        try {
            val remote = api.createIncome(body)
            incomeDao.markSynced(record.id, remote.id, serverMillis(remote.updatedAt))
            tally.pushed++
            true
        } catch (e: Exception) {
            false
        }

    private suspend fun pushExpenses(userId: String, tally: Tally) {
        expenseDao.pendingSync(userId).forEach { record ->
            if (record.amount <= 0.0 || record.merchant.isBlank()) {
                expenseDao.markClean(record.id)
                tally.failed++
                return@forEach
            }
            val body = ExpenseBody(
                amount = record.amount,
                merchant = record.merchant,
                category = record.category.name,
                paymentMode = record.paymentMode.name,
                transactionDate = Dates.toLocalDate(record.date).toString(),
                origin = record.origin.name,
                recurring = record.recurring,
                notes = record.notes
            )
            try {
                val remote = if (record.remoteId == null) {
                    api.createExpense(body)
                } else {
                    api.updateExpense(record.remoteId, body)
                }
                expenseDao.markSynced(record.id, remote.id, serverMillis(remote.updatedAt))
                tally.pushed++
            } catch (e: HttpException) {
                if (e.code() == 404 && record.remoteId != null) {
                    if (recreateExpense(record, body, tally)) {
                        return@forEach
                    }
                    tally.failed++
                } else if (e.code() in 400..499) {
                    expenseDao.markClean(record.id)
                    tally.failed++
                } else {
                    tally.failed++
                }
            } catch (e: IOException) {
                tally.failed++
            }
        }
    }

    private suspend fun recreateExpense(record: ExpenseEntity, body: ExpenseBody, tally: Tally): Boolean =
        try {
            val remote = api.createExpense(body)
            expenseDao.markSynced(record.id, remote.id, serverMillis(remote.updatedAt))
            tally.pushed++
            true
        } catch (e: Exception) {
            false
        }

    private suspend fun pushInvestments(userId: String, tally: Tally) {
        investmentDao.pendingSync(userId).forEach { record ->
            if (record.amountInvested <= 0.0 || record.instrumentName.isBlank()) {
                investmentDao.markClean(record.id)
                tally.failed++
                return@forEach
            }
            val body = InvestmentBody(
                instrumentName = record.instrumentName,
                investmentType = record.type.name,
                amountInvested = record.amountInvested,
                currentValue = record.currentValue,
                transactionDate = Dates.toLocalDate(record.date).toString(),
                broker = record.broker.ifBlank { null },
                origin = record.origin.name,
                notes = record.notes
            )
            try {
                val remote = if (record.remoteId == null) {
                    api.createInvestment(body)
                } else {
                    api.updateInvestment(record.remoteId, body)
                }
                investmentDao.markSynced(record.id, remote.id, serverMillis(remote.updatedAt))
                tally.pushed++
            } catch (e: HttpException) {
                if (e.code() == 404 && record.remoteId != null) {
                    if (recreateInvestment(record, body, tally)) {
                        return@forEach
                    }
                    tally.failed++
                } else if (e.code() in 400..499) {
                    investmentDao.markClean(record.id)
                    tally.failed++
                } else {
                    tally.failed++
                }
            } catch (e: IOException) {
                tally.failed++
            }
        }
    }

    private suspend fun recreateInvestment(
        record: InvestmentEntity,
        body: InvestmentBody,
        tally: Tally
    ): Boolean = try {
        val remote = api.createInvestment(body)
        investmentDao.markSynced(record.id, remote.id, serverMillis(remote.updatedAt))
        tally.pushed++
        true
    } catch (e: Exception) {
        false
    }

    // ---- Step 3: pull remote state ------------------------------------------------------

    private suspend fun pullIncomes(userId: String, tally: Tally) {
        var page = 0
        while (page < MAX_PAGES) {
            val response = api.listIncomes(page, PAGE_SIZE)
            response.content.forEach { remote ->
                if (mergeIncome(userId, remote)) tally.pulled++
            }
            if (response.last || response.content.isEmpty()) break
            page++
        }
    }

    private suspend fun mergeIncome(userId: String, remote: RemoteIncome): Boolean {
        val date = parseDate(remote.transactionDate) ?: return false
        val existing = incomeDao.findByRemoteId(remote.id)
        if (existing == null) {
            incomeDao.insert(
                IncomeEntity(
                    id = Ids.new(),
                    userId = userId,
                    amount = remote.amount,
                    source = remote.source.orEmpty(),
                    category = enumOrDefault(remote.category, IncomeCategory.OTHER),
                    date = date,
                    origin = enumOrDefault(remote.origin, RecordOrigin.MANUAL),
                    notes = remote.notes.orEmpty(),
                    recurring = remote.recurring,
                    status = RecordStatus.CONFIRMED,
                    // Remote rows get a remote-derived fingerprint so they can never collide
                    // with the unique local fingerprint index used for on-device de-duplication.
                    fingerprint = Ids.fingerprint(listOf(userId, "income", "remote", remote.id)),
                    createdAt = parseInstant(remote.createdAt) ?: date,
                    updatedAt = parseInstant(remote.updatedAt) ?: date,
                    remoteId = remote.id,
                    dirty = false
                )
            )
            return true
        }
        // Still dirty means the server refused the push; do not discard the user's edit.
        if (existing.dirty) return false
        val updated = existing.copy(
            amount = remote.amount,
            source = remote.source.orEmpty(),
            category = enumOrDefault(remote.category, IncomeCategory.OTHER),
            date = date,
            origin = enumOrDefault(remote.origin, RecordOrigin.MANUAL),
            notes = remote.notes.orEmpty(),
            recurring = remote.recurring,
            updatedAt = parseInstant(remote.updatedAt) ?: existing.updatedAt
        )
        if (updated == existing) return false
        incomeDao.update(updated)
        return true
    }

    private suspend fun pullExpenses(userId: String, tally: Tally) {
        var page = 0
        while (page < MAX_PAGES) {
            val response = api.listExpenses(page, PAGE_SIZE)
            response.content.forEach { remote ->
                if (mergeExpense(userId, remote)) tally.pulled++
            }
            if (response.last || response.content.isEmpty()) break
            page++
        }
    }

    private suspend fun mergeExpense(userId: String, remote: RemoteExpense): Boolean {
        val date = parseDate(remote.transactionDate) ?: return false
        val existing = expenseDao.findByRemoteId(remote.id)
        if (existing == null) {
            expenseDao.insert(
                ExpenseEntity(
                    id = Ids.new(),
                    userId = userId,
                    amount = remote.amount,
                    merchant = remote.merchant.orEmpty(),
                    category = enumOrDefault(remote.category, ExpenseCategory.OTHER),
                    paymentMode = enumOrDefault(remote.paymentMode, PaymentMode.OTHER),
                    date = date,
                    origin = enumOrDefault(remote.origin, RecordOrigin.MANUAL),
                    notes = remote.notes.orEmpty(),
                    recurring = remote.recurring,
                    status = RecordStatus.CONFIRMED,
                    fingerprint = Ids.fingerprint(listOf(userId, "expense", "remote", remote.id)),
                    createdAt = parseInstant(remote.createdAt) ?: date,
                    updatedAt = parseInstant(remote.updatedAt) ?: date,
                    remoteId = remote.id,
                    dirty = false
                )
            )
            return true
        }
        if (existing.dirty) return false
        val updated = existing.copy(
            amount = remote.amount,
            merchant = remote.merchant.orEmpty(),
            category = enumOrDefault(remote.category, ExpenseCategory.OTHER),
            paymentMode = enumOrDefault(remote.paymentMode, PaymentMode.OTHER),
            date = date,
            origin = enumOrDefault(remote.origin, RecordOrigin.MANUAL),
            notes = remote.notes.orEmpty(),
            recurring = remote.recurring,
            updatedAt = parseInstant(remote.updatedAt) ?: existing.updatedAt
        )
        if (updated == existing) return false
        expenseDao.update(updated)
        return true
    }

    private suspend fun pullInvestments(userId: String, tally: Tally) {
        var page = 0
        while (page < MAX_PAGES) {
            val response = api.listInvestments(page, PAGE_SIZE)
            response.content.forEach { remote ->
                if (mergeInvestment(userId, remote)) tally.pulled++
            }
            if (response.last || response.content.isEmpty()) break
            page++
        }
    }

    private suspend fun mergeInvestment(userId: String, remote: RemoteInvestment): Boolean {
        val date = parseDate(remote.transactionDate) ?: return false
        val existing = investmentDao.findByRemoteId(remote.id)
        val current = remote.currentValue ?: remote.amountInvested
        if (existing == null) {
            investmentDao.insert(
                InvestmentEntity(
                    id = Ids.new(),
                    userId = userId,
                    instrumentName = remote.instrumentName.orEmpty(),
                    type = enumOrDefault(remote.investmentType, InvestmentType.OTHER),
                    amountInvested = remote.amountInvested,
                    currentValue = current,
                    date = date,
                    broker = remote.broker.orEmpty(),
                    origin = enumOrDefault(remote.origin, RecordOrigin.MANUAL),
                    notes = remote.notes.orEmpty(),
                    status = RecordStatus.CONFIRMED,
                    fingerprint = Ids.fingerprint(listOf(userId, "investment", "remote", remote.id)),
                    createdAt = parseInstant(remote.createdAt) ?: date,
                    updatedAt = parseInstant(remote.updatedAt) ?: date,
                    remoteId = remote.id,
                    dirty = false
                )
            )
            return true
        }
        if (existing.dirty) return false
        val updated = existing.copy(
            instrumentName = remote.instrumentName.orEmpty(),
            type = enumOrDefault(remote.investmentType, InvestmentType.OTHER),
            amountInvested = remote.amountInvested,
            currentValue = current,
            date = date,
            broker = remote.broker.orEmpty(),
            origin = enumOrDefault(remote.origin, RecordOrigin.MANUAL),
            notes = remote.notes.orEmpty(),
            updatedAt = parseInstant(remote.updatedAt) ?: existing.updatedAt
        )
        if (updated == existing) return false
        investmentDao.update(updated)
        return true
    }

    // ---- Step 4: SMS review queue -------------------------------------------------------

    /**
     * Mirrors captures and replays review decisions. Only the phone ever enqueues items, so this
     * pushes first; a capture that has no server id yet is submitted before its decision is sent,
     * which is what lets a confirm taken offline still reach the backend later.
     */
    private suspend fun pushCaptures(userId: String, tally: Tally) {
        captureDao.pendingSync(userId).forEach { item -> pushCapture(item, tally) }
    }

    private suspend fun pushCapture(item: AutoCaptureEntity, tally: Tally) {
        val remoteId = item.remoteId ?: submitCapture(item, tally) ?: return
        try {
            when (item.status) {
                AutoCaptureStatus.PENDING -> Unit
                AutoCaptureStatus.CONFIRMED -> api.confirmAutoCapture(remoteId, reviewBody(item))
                AutoCaptureStatus.REJECTED -> api.rejectAutoCapture(remoteId)
            }
            captureDao.markSynced(item.id, remoteId)
            tally.pushed++
        } catch (e: HttpException) {
            // 404 means it is gone server-side and a 400 means the server will not take the
            // decision; either way there is nothing left to retry.
            if (e.code() in 400..499) captureDao.markClean(item.id)
            tally.failed++
        } catch (e: IOException) {
            tally.failed++
        }
    }

    /** Mirrors a capture and returns its server id, or null when the submission could not be made. */
    private suspend fun submitCapture(item: AutoCaptureEntity, tally: Tally): String? {
        val created = try {
            api.submitAutoCapture(captureBody(item))
        } catch (e: IOException) {
            tally.failed++
            return null
        } catch (e: HttpException) {
            // A payload the server refuses would be refused forever; stop retrying it.
            if (e.code() in 400..499) captureDao.markClean(item.id)
            tally.failed++
            return null
        }
        // Persisted immediately: if the review call below fails, the next pass has to replay the
        // decision against this item rather than submit the same capture a second time.
        captureDao.setRemoteId(item.id, created.id)
        return created.id
    }

    private fun captureBody(item: AutoCaptureEntity) = AutoCaptureBody(
        // The backend derives the record's origin from the source type, and the app uses the same
        // rule to tell an email alert from an SMS burst.
        sourceType = if (item.sender.contains("@")) "EMAIL" else "SMS",
        sender = item.sender.ifBlank { null },
        rawText = item.rawText,
        parsedType = item.parsedType.name,
        // Sent as well as the review body: the backend mines these keys when a review arrives
        // without explicit values.
        parsedData = buildMap {
            put("type", item.parsedType.name)
            item.parsedAmount?.let { put("amount", it) }
            item.parsedDate?.let { put("date", Dates.toLocalDate(it).toString()) }
            item.parsedParty?.takeIf { it.isNotBlank() }?.let {
                put("merchant", it)
                put("description", it)
            }
            // The local column currently holds the detected record type rather than a category,
            // so it is only forwarded when it says something the type does not already.
            item.parsedCategory
                ?.takeIf { it.isNotBlank() && !it.equals(item.parsedType.name, ignoreCase = true) }
                ?.let { put("category", it) }
        }
    )

    /**
     * The values a confirmed capture contributes to the record the backend writes. `description`
     * and `merchant` carry the same value because income records take their source from the first
     * and expense records take their merchant from the second, while investments use either.
     */
    private fun reviewBody(item: AutoCaptureEntity) = AutoCaptureReviewBody(
        transactionType = item.parsedType.name,
        transactionDate = Dates.toLocalDate(item.parsedDate ?: item.createdAt).toString(),
        description = item.parsedParty?.takeIf { it.isNotBlank() },
        merchant = item.parsedParty?.takeIf { it.isNotBlank() },
        category = DEFAULT_CATEGORY,
        paymentMode = DEFAULT_PAYMENT_MODE,
        amount = item.parsedAmount
    )

    /**
     * The backend owns review outcomes, so status and parsed fields flow down. A row that is still
     * `dirty` keeps its local copy: that means its decision has not been replayed yet.
     */
    private suspend fun pullCaptures(userId: String, tally: Tally) {
        var page = 0
        while (page < MAX_PAGES) {
            val response = api.listAutoCapture(page, PAGE_SIZE)
            response.content.forEach { remote ->
                if (mergeCapture(userId, remote)) tally.pulled++
            }
            if (response.last || response.content.isEmpty()) break
            page++
        }
    }

    private suspend fun mergeCapture(userId: String, remote: RemoteAutoCapture): Boolean {
        val existing = captureDao.findByRemoteId(remote.id)
        if (existing == null) {
            captureDao.insert(
                AutoCaptureEntity(
                    id = Ids.new(),
                    userId = userId,
                    rawText = remote.rawText.orEmpty(),
                    sender = remote.sender.orEmpty(),
                    parsedType = enumOrDefault(remote.parsedType, ParsedType.EXPENSE),
                    parsedAmount = numberValue(remote.parsedData, "amount"),
                    parsedParty = stringValue(remote.parsedData, "merchant", "party", "description"),
                    parsedDate = parseDate(stringValue(remote.parsedData, "date", "transactionDate")),
                    parsedCategory = stringValue(remote.parsedData, "category"),
                    status = enumOrDefault(remote.status, AutoCaptureStatus.PENDING),
                    createdAt = parseInstant(remote.createdAt) ?: Dates.now(),
                    remoteId = remote.id,
                    dirty = false
                )
            )
            return true
        }
        if (existing.dirty) return false
        val status = enumOrDefault(remote.status, existing.status)
        if (status == existing.status) return false
        captureDao.update(existing.copy(status = status))
        return true
    }

    // ---- Step 5: statement imports ------------------------------------------------------

    /**
     * Uploads the private copy of every statement whose upload did not finish — a pick taken
     * offline, or one the in-app flow could not complete. The import screen handles the normal
     * path; this is the retry that makes the queue drain without user action.
     */
    private suspend fun pushImports(userId: String, tally: Tally) {
        importDao.pendingSync(userId).forEach { batch ->
            val file = batch.localPath?.let { File(it) }
            if (file == null || !file.exists()) {
                // The private copy is gone, so there is nothing to upload and never will be.
                importDao.markClean(batch.id)
                tally.failed++
                return@forEach
            }
            if (file.length() > MAX_UPLOAD_BYTES) {
                // The backend caps multipart requests at 25MB; sending this would only earn a 413.
                importDao.markClean(batch.id)
                tally.failed++
                return@forEach
            }
            try {
                val remote = api.uploadImport(multipart(batch, file))
                importDao.markSynced(batch.id, remote.id)
                tally.pushed++
            } catch (e: HttpException) {
                // For example an extension the backend does not parse; retrying cannot help.
                if (e.code() in 400..499) importDao.markClean(batch.id)
                tally.failed++
            } catch (e: IOException) {
                tally.failed++
            }
        }
    }

    /** The multipart field name `file` matches the backend's `@RequestParam("file")`. */
    private fun multipart(batch: ImportBatchEntity, file: File): MultipartBody.Part {
        val mediaType = (batch.contentType?.takeIf { it.isNotBlank() } ?: "application/octet-stream")
            .toMediaTypeOrNull()
        return MultipartBody.Part.createFormData(
            "file",
            batch.sourceFile.ifBlank { file.name },
            file.asRequestBody(mediaType)
        )
    }

    private suspend fun pullImports(userId: String, tally: Tally) {
        var page = 0
        while (page < MAX_PAGES) {
            val response = api.listImports(page, PAGE_SIZE)
            response.content.forEach { remote ->
                if (mergeImport(userId, remote)) tally.pulled++
            }
            if (response.last || response.content.isEmpty()) break
            page++
        }
    }

    /**
     * Mirrors the server's statement history — including batches uploaded from another device
     * or the final states of this device's uploads that arrived after the in-app poll gave up.
     */
    private suspend fun mergeImport(userId: String, remote: RemoteImportBatch): Boolean {
        val existing = importDao.findByRemoteId(remote.id)
        if (existing != null) {
            if (existing.dirty) return false
            val status = remote.status?.lowercase()?.ifBlank { existing.status } ?: existing.status
            val updated = existing.copy(
                status = status,
                totalParsed = remote.totalTransactions,
                errorMessage = remote.errorMessage
            )
            if (updated == existing) return false
            importDao.update(updated)
            return true
        }
        val status = remote.status.orEmpty()
        importDao.insert(
            ImportBatchEntity(
                id = Ids.new(),
                userId = userId,
                sourceFile = remote.fileName.orEmpty().ifBlank { "statement" },
                status = status.ifBlank { "unknown" }.lowercase(),
                totalParsed = remote.totalTransactions,
                committed = if (status.equals("COMMITTED", ignoreCase = true)) remote.totalTransactions else 0,
                createdAt = parseInstant(remote.createdAt) ?: Dates.now(),
                remoteId = remote.id,
                dirty = false,
                errorMessage = remote.errorMessage
            )
        )
        return true
    }

    // ---- Step 6: category budgets -------------------------------------------------------

    private suspend fun pushBudgets(userId: String, tally: Tally) {
        budgetDao.pendingSync(userId).forEach { budget ->
            if (budget.monthlyLimit <= 0.0) {
                // The backend rejects a limit that is not positive.
                budgetDao.markClean(budget.id)
                tally.failed++
                return@forEach
            }
            try {
                val remote = api.upsertBudget(budget.category.name, BudgetUpsertBody(budget.monthlyLimit))
                budgetDao.markSynced(budget.id, remote.id)
                tally.pushed++
            } catch (e: HttpException) {
                // 400s are payload problems the server will never accept. A 404 is different: the
                // endpoint may simply not be deployed yet, so the row stays dirty and is retried
                // rather than silently abandoned.
                if (e.code() in 400..403) budgetDao.markClean(budget.id)
                tally.failed++
            } catch (e: IOException) {
                tally.failed++
            }
        }
    }

    private suspend fun pullBudgets(userId: String, tally: Tally) {
        val budgets = try {
            api.listBudgets()
        } catch (e: HttpException) {
            tally.failed++
            return
        } catch (e: IOException) {
            tally.failed++
            return
        }
        budgets.forEach { remote ->
            if (mergeBudget(userId, remote)) tally.pulled++
        }
    }

    private suspend fun mergeBudget(userId: String, remote: RemoteBudget): Boolean {
        // Budgets are keyed by category, so a category this build does not know is skipped rather
        // than folded onto OTHER, where it would silently overwrite a real limit.
        val category = ExpenseCategory.entries.firstOrNull { it.name.equals(remote.category, ignoreCase = true) }
            ?: return false
        val id = budgetIdFor(userId, category)
        val existing = budgetDao.getById(id)
        // A local edit that has not been pushed yet wins; the push above already had its turn.
        if (existing != null && existing.dirty) return false
        if (existing != null && existing.remoteId == remote.id && existing.monthlyLimit == remote.monthlyLimit) {
            return false
        }
        budgetDao.upsert(
            BudgetEntity(
                id = id,
                userId = userId,
                category = category,
                monthlyLimit = remote.monthlyLimit,
                updatedAt = parseInstant(remote.updatedAt) ?: Dates.now(),
                remoteId = remote.id,
                dirty = false
            )
        )
        return true
    }

    // ---- Step 7: account settings -------------------------------------------------------

    /**
     * Capture switches and sender lists are a single document on the account, so each pass either
     * pushes the local copy or pulls the account's — never both, which would let a pull undo an
     * edit that has not been sent yet.
     */
    private suspend fun syncCaptureSettings(userId: String, tally: Tally) {
        if (prefs.current().captureSettingsDirty) {
            pushCaptureSettings(userId, tally)
        } else {
            pullCaptureSettings(userId, tally)
        }
    }

    private suspend fun pushCaptureSettings(userId: String, tally: Tally) {
        val local = prefs.current()
        val rules = senderRuleDao.rules(userId)
        val body = CaptureSettingsBody(
            // The account's master switch is derived: capturing is on if either channel is.
            enabled = local.smsCaptureEnabled || local.emailCaptureEnabled,
            smsEnabled = local.smsCaptureEnabled,
            emailEnabled = local.emailCaptureEnabled,
            senderAllowList = rules.filter { it.allowed }.map { it.sender }.sorted(),
            senderBlockList = rules.filterNot { it.allowed }.map { it.sender }.sorted()
        )
        try {
            val remote = api.updateCaptureSettings(body)
            prefs.applyRemoteCaptureSettings(remote.smsEnabled, remote.emailEnabled)
            tally.pushed++
        } catch (e: HttpException) {
            // The local copy stays dirty and is retried; a Settings screen that silently dropped
            // the change would be worse than one that reports it later.
            tally.failed++
        } catch (e: IOException) {
            tally.failed++
        }
    }

    private suspend fun pullCaptureSettings(userId: String, tally: Tally) {
        val remote = try {
            api.captureSettings()
        } catch (e: HttpException) {
            tally.failed++
            return
        } catch (e: IOException) {
            tally.failed++
            return
        }
        prefs.applyRemoteCaptureSettings(remote.smsEnabled, remote.emailEnabled)
        // The two lists are rebuilt only when they actually differ, so an unchanged account does
        // not rewrite the sender table (and flicker the observing UI) on every single sync.
        val incoming = (remote.senderAllowList.map { it.uppercase() to true } +
            remote.senderBlockList.map { it.uppercase() to false }).toMap()
        val current = senderRuleDao.rules(userId).associate { it.sender.uppercase() to it.allowed }
        if (current != incoming) {
            senderRuleDao.deleteForUser(userId)
            incoming.forEach { (sender, allowed) ->
                senderRuleDao.upsert(
                    SenderRuleEntity(
                        id = Ids.fingerprint(listOf(userId, sender)),
                        userId = userId,
                        sender = sender,
                        allowed = allowed
                    )
                )
            }
        }
        tally.pulled++
    }

    /** Notifications, insight frequency and currency all live on the user profile. */
    private suspend fun syncProfileSettings(tally: Tally) {
        if (prefs.current().profileSettingsDirty) {
            pushProfileSettings(tally)
        } else {
            pullProfileSettings(tally)
        }
    }

    private suspend fun pushProfileSettings(tally: Tally) {
        val local = prefs.current()
        try {
            val remote = api.updateProfile(
                UpdateProfileBody(
                    currency = local.currencyCode,
                    notificationsEnabled = local.notificationsEnabled,
                    // The account's preferences map is replaced wholesale by the server, so it is
                    // sent complete: this app owns the map and keeps a single key in it.
                    preferences = mapOf(INSIGHT_FREQUENCY_DAYS to local.insightFrequencyDays)
                )
            )
            prefs.applyRemoteProfileSettings(
                notificationsEnabled = remote.notificationsEnabled ?: local.notificationsEnabled,
                insightFrequencyDays = insightFrequencyOf(remote) ?: local.insightFrequencyDays,
                currencyCode = remote.currency ?: local.currencyCode
            )
            tally.pushed++
        } catch (e: HttpException) {
            tally.failed++
        } catch (e: IOException) {
            tally.failed++
        }
    }

    private suspend fun pullProfileSettings(tally: Tally) {
        val remote = try {
            api.profile()
        } catch (e: HttpException) {
            tally.failed++
            return
        } catch (e: IOException) {
            tally.failed++
            return
        }
        val local = prefs.current()
        prefs.applyRemoteProfileSettings(
            notificationsEnabled = remote.notificationsEnabled ?: local.notificationsEnabled,
            insightFrequencyDays = insightFrequencyOf(remote) ?: local.insightFrequencyDays,
            currencyCode = remote.currency ?: local.currencyCode
        )
        tally.pulled++
    }

    private fun insightFrequencyOf(profile: AuthUser): Int? =
        when (val value = profile.preferences?.get(INSIGHT_FREQUENCY_DAYS)) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull()
            else -> null
        }

    // ---- Step 8: insights ---------------------------------------------------------------

    /** Pushes the save/dismiss decisions the user has taken since the last pass. */
    private suspend fun pushInsights(userId: String, tally: Tally) {
        insightDao.pendingSync(userId).forEach { insight ->
            val status = when {
                insight.dismissed -> "DISMISSED"
                insight.saved -> "SAVED"
                else -> "NEW"
            }
            try {
                api.updateInsightStatus(insight.id, InsightStatusBody(status))
                insightDao.markSynced(insight.id)
                tally.pushed++
            } catch (e: HttpException) {
                if (e.code() == 404) {
                    // Gone from the account (superseded by a refresh elsewhere), so there is
                    // nothing left to save or dismiss.
                    insightDao.delete(insight.id)
                } else if (e.code() in 400..499) {
                    insightDao.markSynced(insight.id)
                }
                tally.failed++
            } catch (e: IOException) {
                tally.failed++
            }
        }
    }

    private suspend fun pullInsights(userId: String, tally: Tally) {
        val seen = mutableSetOf<String>()
        var page = 0
        while (page < MAX_PAGES) {
            val response = api.listInsights(page, PAGE_SIZE)
            response.content.forEach { remote ->
                seen += remote.id
                if (mergeInsight(userId, remote)) tally.pulled++
            }
            if (response.last || response.content.isEmpty()) break
            page++
        }
        // Anything the account no longer holds goes too, unless a decision on it is still queued.
        if (seen.isNotEmpty()) insightDao.deleteMissing(userId, seen.toList())
    }

    private suspend fun mergeInsight(userId: String, remote: RemoteInsight): Boolean {
        val existing = insightDao.getById(remote.id)
        if (existing != null && existing.dirty) return false
        val status = remote.status.orEmpty().uppercase()
        val merged = InsightEntity(
            id = remote.id,
            userId = userId,
            title = remote.title,
            insightText = remote.insightText.orEmpty(),
            category = remote.category.orEmpty().ifBlank { "GENERAL" },
            generatedAt = parseInstant(remote.generatedAt) ?: Dates.now(),
            dismissed = status == "DISMISSED",
            saved = status == "SAVED",
            dirty = false
        )
        if (merged == existing) return false
        insightDao.upsert(merged)
        return true
    }

    /**
     * Asks the account for a fresh batch of insights and stores them.
     *
     * Suggestions the user never saved are then dropped from the account as well. Each batch is a
     * set of one-off observations, and keeping every batch forever would turn the tab into an
     * archive of outdated advice; saved and dismissed ones are user decisions, so those stay.
     */
    suspend fun generateInsights(userId: String, months: Int = DEFAULT_INSIGHT_MONTHS): Result<Int> {
        val previous = insightDao.all(userId)
        return try {
            val generated = api.generateInsights(GenerateInsightsBody(months))
            generated.forEach { remote -> insightDao.upsert(mergeable(remote, userId)) }
            val fresh = generated.map { it.id }.toSet()
            previous.filter { it.id !in fresh && !it.saved && !it.dismissed }.forEach { stale ->
                runCatching { api.deleteInsight(stale.id) }
                insightDao.delete(stale.id)
            }
            Result.success(generated.size)
        } catch (e: IOException) {
            Result.failure(
                IOException("Offline — insights are generated by your account, so this needs a connection.")
            )
        } catch (e: HttpException) {
            Result.failure(IllegalStateException(describe(e)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mergeable(remote: RemoteInsight, userId: String): InsightEntity {
        val status = remote.status.orEmpty().uppercase()
        return InsightEntity(
            id = remote.id,
            userId = userId,
            title = remote.title,
            insightText = remote.insightText.orEmpty(),
            category = remote.category.orEmpty().ifBlank { "GENERAL" },
            generatedAt = parseInstant(remote.generatedAt) ?: Dates.now(),
            dismissed = status == "DISMISSED",
            saved = status == "SAVED",
            dirty = false
        )
    }

    /**
     * Builds the CSV from the account's export rather than from local rows, so the file always
     * describes what the account holds. Records edited on this device seconds ago are already
     * pushed by the sync that every edit schedules.
     */
    suspend fun exportCsv(): Result<String> = try {
        val export = api.exportData()
        val rows = StringBuilder()
        rows.appendLine("type,date,party,category,amount,origin,notes")
        export.incomeRecords.forEach {
            rows.appendLine(
                listOf("income", it.transactionDate.orEmpty(), it.source.orEmpty(), it.category.orEmpty(),
                    it.amount.toString(), it.origin.orEmpty(), it.notes.orEmpty()).map(::csv).joinToString(",")
            )
        }
        export.expenseRecords.forEach {
            rows.appendLine(
                listOf("expense", it.transactionDate.orEmpty(), it.merchant.orEmpty(), it.category.orEmpty(),
                    it.amount.toString(), it.origin.orEmpty(), it.notes.orEmpty()).map(::csv).joinToString(",")
            )
        }
        export.investmentRecords.forEach {
            rows.appendLine(
                listOf("investment", it.transactionDate.orEmpty(), it.instrumentName.orEmpty(),
                    it.investmentType.orEmpty(), it.amountInvested.toString(), it.origin.orEmpty(),
                    it.notes.orEmpty()).map(::csv).joinToString(",")
            )
        }
        Result.success(rows.toString())
    } catch (e: IOException) {
        Result.failure(IOException("Offline — the export comes from your account, so this needs a connection."))
    } catch (e: HttpException) {
        Result.failure(IllegalStateException(describe(e)))
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun csv(value: String): String = "\"${value.replace("\"", "\"\"")}\""

    // ---- Helpers ------------------------------------------------------------------------

    private fun describe(e: HttpException): String =
        messageFrom(e.response()?.errorBody()?.string()) ?: "Server error ${e.code()}"

    /** Reads a key from a server-side `parsedData` map, tolerating a value stored as a string. */
    private fun stringValue(data: Map<String, Any?>?, vararg keys: String): String? {
        if (data == null) return null
        keys.forEach { key ->
            data[key]?.toString()?.takeIf { it.isNotBlank() }?.let { return it }
        }
        return null
    }

    private fun numberValue(data: Map<String, Any?>?, vararg keys: String): Double? {
        if (data == null) return null
        keys.forEach { key ->
            when (val value = data[key]) {
                is Number -> return value.toDouble()
                is String -> value.replace(NUMBER_NOISE, "").toDoubleOrNull()?.let { return it }
            }
        }
        return null
    }

    private fun serverMillis(value: String?): Long = parseInstant(value) ?: Dates.now()

    private fun parseDate(value: String?): Long? =
        value?.let { raw -> runCatching { Dates.toEpoch(LocalDate.parse(raw)) }.getOrNull() }

    /**
     * The backend serialises `Instant` as ISO-8601 ("2026-09-15T17:36:53.991095849Z") because
     * `write-dates-as-timestamps` is disabled. The numeric form is tolerated too so a config
     * change server-side cannot silently break ordering.
     */
    private fun parseInstant(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        return runCatching { Instant.parse(value).toEpochMilli() }.getOrNull()
            ?: runCatching { LocalDateTime.parse(value).atZone(ZoneId.systemDefault()) }
                .getOrNull()?.toInstant()?.toEpochMilli()
            ?: value.toDoubleOrNull()?.let { seconds -> (seconds * 1000.0).toLong() }
    }

    /**
     * Maps a backend enum name onto the app's enum, falling back when the server uses a value
     * this build does not know (for example `NET_BANKING`, `WALLET` or `REAL_ESTATE`).
     */
    private inline fun <reified E : Enum<E>> enumOrDefault(name: String?, fallback: E): E =
        name?.uppercase()?.let { runCatching { enumValueOf<E>(it) }.getOrNull() } ?: fallback

    companion object {
        private const val PAGE_SIZE = 200
        private const val MAX_PAGES = 1_000
        private const val DEFAULT_INSIGHT_MONTHS = 3

        /** Key this app owns inside the account's free-form preferences map. */
        private const val INSIGHT_FREQUENCY_DAYS = "insightFrequencyDays"

        /** Matches `spring.servlet.multipart.max-request-size` on the backend (25MB). */
        private const val MAX_UPLOAD_BYTES = 25L * 1024 * 1024
        private const val DEFAULT_CATEGORY = "OTHER"
        private const val DEFAULT_PAYMENT_MODE = "OTHER"
        private val NUMBER_NOISE = Regex("[^0-9.\\-]")
    }
}
