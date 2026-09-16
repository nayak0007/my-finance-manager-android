package com.myfinancemanager.app.data.repository

import com.myfinancemanager.app.data.local.dao.AutoCaptureDao
import com.myfinancemanager.app.data.local.dao.BudgetDao
import com.myfinancemanager.app.data.local.dao.ExpenseDao
import com.myfinancemanager.app.data.local.dao.ImportBatchDao
import com.myfinancemanager.app.data.local.dao.IncomeDao
import com.myfinancemanager.app.data.local.dao.InsightDao
import com.myfinancemanager.app.data.local.dao.InvestmentDao
import com.myfinancemanager.app.data.local.dao.SenderRuleDao
import com.myfinancemanager.app.data.local.dao.SyncDao
import com.myfinancemanager.app.data.local.StoredStatement
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
import com.myfinancemanager.app.data.local.entity.SyncTombstoneEntity
import com.myfinancemanager.app.data.local.entity.budgetIdFor
import com.myfinancemanager.app.data.parser.ParsedTransaction
import com.myfinancemanager.app.data.prefs.UserPreferences
import com.myfinancemanager.app.util.Dates
import com.myfinancemanager.app.util.Ids
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.YearMonth

class FinanceRepository(
    private val incomeDao: IncomeDao,
    private val expenseDao: ExpenseDao,
    private val investmentDao: InvestmentDao,
    private val autoCaptureDao: AutoCaptureDao,
    private val importBatchDao: ImportBatchDao,
    private val insightDao: InsightDao,
    private val budgetDao: BudgetDao,
    private val senderRuleDao: SenderRuleDao,
    private val syncDao: SyncDao,
    private val preferences: UserPreferences
) {
    fun observeIncome(userId: String): Flow<List<IncomeEntity>> =
        incomeDao.observe(userId, RecordStatus.CONFIRMED)
    fun observeExpense(userId: String): Flow<List<ExpenseEntity>> =
        expenseDao.observe(userId, RecordStatus.CONFIRMED)
    fun observeInvestments(userId: String): Flow<List<InvestmentEntity>> =
        investmentDao.observe(userId, RecordStatus.CONFIRMED)
    fun observeQueue(userId: String): Flow<List<AutoCaptureEntity>> =
        autoCaptureDao.observeByStatus(userId, AutoCaptureStatus.PENDING)
    fun observeQueueCount(userId: String): Flow<Int> =
        autoCaptureDao.observeCount(userId, AutoCaptureStatus.PENDING)
    fun observeInsights(userId: String): Flow<List<InsightEntity>> = insightDao.observeActive(userId)
    fun observeBudgets(userId: String): Flow<List<BudgetEntity>> = budgetDao.observe(userId)
    fun observeSenderRules(userId: String): Flow<List<SenderRuleEntity>> = senderRuleDao.observe(userId)
    fun observeImports(userId: String): Flow<List<ImportBatchEntity>> = importBatchDao.observe(userId)

    suspend fun addIncome(
        userId: String,
        amount: Double,
        source: String,
        category: IncomeCategory,
        date: Long,
        notes: String,
        recurring: Boolean,
        origin: RecordOrigin = RecordOrigin.MANUAL,
        status: RecordStatus = RecordStatus.CONFIRMED
    ): Result<Unit> {
        val fingerprint = Ids.fingerprint(listOf(userId, "income", amount.toString(), source, date.toString()))
        if (incomeDao.countFingerprint(fingerprint) > 0) {
            return Result.failure(IllegalStateException("Duplicate income entry"))
        }
        val now = Dates.now()
        incomeDao.insert(
            IncomeEntity(
                id = Ids.new(),
                userId = userId,
                amount = amount,
                source = source,
                category = category,
                date = date,
                origin = origin,
                notes = notes,
                recurring = recurring,
                status = status,
                fingerprint = fingerprint,
                createdAt = now,
                updatedAt = now
            )
        )
        return Result.success(Unit)
    }

    suspend fun updateIncome(record: IncomeEntity) {
        incomeDao.update(record.copy(updatedAt = Dates.now(), dirty = true))
    }

    suspend fun deleteIncome(id: String) {
        val existing = incomeDao.getById(id)
        incomeDao.delete(id)
        enqueueTombstone(existing?.userId, id, SyncKind.INCOME, existing?.remoteId)
    }

    suspend fun addExpense(
        userId: String,
        amount: Double,
        merchant: String,
        category: ExpenseCategory,
        paymentMode: PaymentMode,
        date: Long,
        notes: String,
        recurring: Boolean,
        origin: RecordOrigin = RecordOrigin.MANUAL,
        status: RecordStatus = RecordStatus.CONFIRMED
    ): Result<Unit> {
        val fingerprint = Ids.fingerprint(listOf(userId, "expense", amount.toString(), merchant, date.toString()))
        if (expenseDao.countFingerprint(fingerprint) > 0) {
            return Result.failure(IllegalStateException("Duplicate expense entry"))
        }
        val now = Dates.now()
        expenseDao.insert(
            ExpenseEntity(
                id = Ids.new(),
                userId = userId,
                amount = amount,
                merchant = merchant,
                category = category,
                paymentMode = paymentMode,
                date = date,
                origin = origin,
                notes = notes,
                recurring = recurring,
                status = status,
                fingerprint = fingerprint,
                createdAt = now,
                updatedAt = now
            )
        )
        return Result.success(Unit)
    }

    suspend fun updateExpense(record: ExpenseEntity) {
        expenseDao.update(record.copy(updatedAt = Dates.now(), dirty = true))
    }

    suspend fun deleteExpense(id: String) {
        val existing = expenseDao.getById(id)
        expenseDao.delete(id)
        enqueueTombstone(existing?.userId, id, SyncKind.EXPENSE, existing?.remoteId)
    }

    suspend fun addInvestment(
        userId: String,
        name: String,
        type: InvestmentType,
        invested: Double,
        current: Double,
        date: Long,
        broker: String,
        notes: String,
        origin: RecordOrigin = RecordOrigin.MANUAL,
        status: RecordStatus = RecordStatus.CONFIRMED
    ): Result<Unit> {
        val fingerprint = Ids.fingerprint(listOf(userId, "investment", name, invested.toString(), date.toString()))
        if (investmentDao.countFingerprint(fingerprint) > 0) {
            return Result.failure(IllegalStateException("Duplicate investment entry"))
        }
        val now = Dates.now()
        investmentDao.insert(
            InvestmentEntity(
                id = Ids.new(),
                userId = userId,
                instrumentName = name,
                type = type,
                amountInvested = invested,
                currentValue = current,
                date = date,
                broker = broker,
                origin = origin,
                notes = notes,
                status = status,
                fingerprint = fingerprint,
                createdAt = now,
                updatedAt = now
            )
        )
        return Result.success(Unit)
    }

    suspend fun updateInvestment(record: InvestmentEntity) {
        investmentDao.update(record.copy(updatedAt = Dates.now(), dirty = true))
    }

    suspend fun deleteInvestment(id: String) {
        val existing = investmentDao.getById(id)
        investmentDao.delete(id)
        enqueueTombstone(existing?.userId, id, SyncKind.INVESTMENT, existing?.remoteId)
    }

    /**
     * Records the remote half of a delete so the next sync removes the server copy.
     * Skipped when the row was never known locally, or when it never reached the server.
     */
    private suspend fun enqueueTombstone(
        userId: String?,
        localId: String,
        kind: SyncKind,
        remoteId: String?
    ) {
        if (userId == null) return
        syncDao.enqueue(
            SyncTombstoneEntity(id = localId, userId = userId, kind = kind.name, remoteId = remoteId)
        )
    }

    suspend fun enqueueAutoCapture(userId: String, sender: String, rawText: String, parsed: ParsedTransaction) {
        val rule = senderRuleDao.get(userId, sender.uppercase())
        if (rule?.allowed == false) return
        autoCaptureDao.insert(
            AutoCaptureEntity(
                id = Ids.new(),
                userId = userId,
                rawText = rawText,
                sender = sender,
                parsedType = parsed.type,
                parsedAmount = parsed.amount,
                parsedParty = parsed.party,
                parsedDate = parsed.date,
                parsedCategory = parsed.categoryHint,
                status = AutoCaptureStatus.PENDING,
                createdAt = Dates.now()
            )
        )
    }

    /**
     * Records the decision to accept a capture. The transaction itself is written by the backend
     * on `POST /auto-capture/{id}/confirm`, and reaches this device through the normal record
     * pull — building it here as well would count the same payment twice. The row is left dirty so
     * the decision is replayed on the next sync, which is what makes confirming while offline work.
     */
    suspend fun confirmCapture(item: AutoCaptureEntity): Result<Unit> {
        val amount = item.parsedAmount
        if (amount == null || amount <= 0.0) {
            // The backend rejects a transaction without a positive amount, so asking it to write
            // this one would just leave the capture stuck between the queue and the ledger.
            return Result.failure(IllegalStateException("No amount was detected — add this one by hand"))
        }
        autoCaptureDao.update(item.copy(status = AutoCaptureStatus.CONFIRMED, dirty = true))
        return Result.success(Unit)
    }

    suspend fun rejectCapture(item: AutoCaptureEntity) {
        autoCaptureDao.update(item.copy(status = AutoCaptureStatus.REJECTED, dirty = true))
    }

    suspend fun commitImport(
        userId: String,
        fileName: String,
        storedStatement: StoredStatement?,
        selected: List<ParsedTransaction>
    ): ImportBatchEntity {
        var committed = 0
        selected.forEach { tx ->
            val origin = RecordOrigin.IMPORT
            val ok = when (tx.type) {
                ParsedType.INCOME -> addIncome(
                    userId, tx.amount, tx.party, tx.incomeCategory, tx.date, tx.notes, false, origin
                )
                ParsedType.EXPENSE -> addExpense(
                    userId, tx.amount, tx.party, tx.expenseCategory, tx.paymentMode, tx.date, tx.notes, false, origin
                )
                ParsedType.INVESTMENT -> addInvestment(
                    userId, tx.party, InvestmentType.OTHER, tx.amount, tx.amount, tx.date, "", tx.notes, origin
                )
            }
            if (ok.isSuccess) committed++
        }
        val batch = ImportBatchEntity(
            id = Ids.new(),
            userId = userId,
            sourceFile = fileName,
            status = "committed",
            totalParsed = selected.size,
            committed = committed,
            createdAt = Dates.now(),
            localPath = storedStatement?.path,
            contentType = storedStatement?.contentType,
            fileSize = storedStatement?.size ?: 0,
            // Uploaded on the next sync so the statement itself is backed up on the server. The
            // backend's own parse is never committed: the records above are already on their way
            // there, and committing would create a second copy of every transaction.
            dirty = storedStatement != null
        )
        importBatchDao.insert(batch)
        return batch
    }

    suspend fun isDuplicate(userId: String, tx: ParsedTransaction): Boolean {
        val incomeFp = Ids.fingerprint(listOf(userId, "income", tx.amount.toString(), tx.party, tx.date.toString()))
        val expenseFp = Ids.fingerprint(listOf(userId, "expense", tx.amount.toString(), tx.party, tx.date.toString()))
        val invFp = Ids.fingerprint(listOf(userId, "investment", tx.party, tx.amount.toString(), tx.date.toString()))
        return incomeDao.countFingerprint(incomeFp) > 0 ||
            expenseDao.countFingerprint(expenseFp) > 0 ||
            investmentDao.countFingerprint(invFp) > 0
    }

    /** Leaves the row dirty so the limit is pushed on the next sync. */
    suspend fun upsertBudget(userId: String, category: ExpenseCategory, limit: Double) {
        budgetDao.upsert(
            BudgetEntity(
                id = budgetIdFor(userId, category),
                userId = userId,
                category = category,
                monthlyLimit = limit,
                updatedAt = Dates.now(),
                dirty = true
            )
        )
    }

    /**
     * Sender rules are not rows on the account either — they are two lists inside the capture
     * settings document, so any change flags the settings for a push.
     */
    suspend fun upsertSenderRule(userId: String, sender: String, allowed: Boolean) {
        senderRuleDao.upsert(
            SenderRuleEntity(
                id = Ids.fingerprint(listOf(userId, sender)),
                userId = userId,
                sender = sender.uppercase(),
                allowed = allowed
            )
        )
        preferences.markCaptureSettingsDirty()
    }

    suspend fun deleteSenderRule(id: String) {
        senderRuleDao.delete(id)
        preferences.markCaptureSettingsDirty()
    }

    /** A save/dismiss decision is pushed to the account on the next sync. */
    suspend fun updateInsight(item: InsightEntity) = insightDao.update(item.copy(dirty = true))

    suspend fun wipeUserData(userId: String) {
        incomeDao.deleteForUser(userId)
        expenseDao.deleteForUser(userId)
        investmentDao.deleteForUser(userId)
        autoCaptureDao.deleteForUser(userId)
        importBatchDao.deleteForUser(userId)
        insightDao.deleteForUser(userId)
        budgetDao.deleteForUser(userId)
        senderRuleDao.deleteForUser(userId)
        syncDao.deleteForUser(userId)
    }

    fun monthRange(month: YearMonth): Pair<Long, Long> = Dates.startOfMonth(month) to Dates.endOfMonth(month)
}
