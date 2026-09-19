package com.myfinancemanager.app.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.myfinancemanager.app.data.importing.ImportUiBatch
import com.myfinancemanager.app.data.importing.ImportUploadResult
import com.myfinancemanager.app.data.local.entity.AutoCaptureEntity
import com.myfinancemanager.app.data.local.entity.BudgetEntity
import com.myfinancemanager.app.data.local.entity.ExpenseCategory
import com.myfinancemanager.app.data.local.entity.ExpenseEntity
import com.myfinancemanager.app.data.local.entity.ImportBatchEntity
import com.myfinancemanager.app.data.local.entity.IncomeCategory
import com.myfinancemanager.app.data.local.entity.IncomeEntity
import com.myfinancemanager.app.data.local.entity.InsightEntity
import com.myfinancemanager.app.data.local.entity.InvestmentEntity
import com.myfinancemanager.app.data.local.entity.InvestmentType
import com.myfinancemanager.app.data.local.entity.PaymentMode
import com.myfinancemanager.app.data.local.entity.RecordOrigin
import com.myfinancemanager.app.data.local.entity.SenderRuleEntity
import com.myfinancemanager.app.data.parser.ParsedTransaction
import com.myfinancemanager.app.data.prefs.AppPreferences
import com.myfinancemanager.app.data.repository.AuthRepository
import com.myfinancemanager.app.data.repository.FinanceRepository
import com.myfinancemanager.app.data.session.Session
import com.myfinancemanager.app.di.AppContainer
import com.myfinancemanager.app.util.Dates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.YearMonth

data class DashboardState(
    val month: YearMonth = YearMonth.now(),
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val invested: Double = 0.0,
    val currentValue: Double = 0.0,
    val recent: List<FeedItem> = emptyList(),
    val expenseByCategory: List<Pair<String, Double>> = emptyList(),
    val incomeTrend: List<Pair<String, Double>> = emptyList(),
    val expenseTrend: List<Pair<String, Double>> = emptyList()
)

data class FeedItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val amount: Double,
    val date: Long,
    val kind: String,
    val auto: Boolean
)

/** Progress of the background two-way sync, surfaced in Settings. */
data class SyncSnapshot(
    val running: Boolean = false,
    val lastAt: Long? = null,
    val error: String? = null
)

/**
 * Transient state for the sign-in screens.
 *
 * [error] is deliberately kept separate from the flash/snackbar channel: a failed sign-in or
 * sign-up has to stay on screen until the user tries again, whereas flash messages are consumed
 * after a few seconds.
 */
data class AuthStage(
    val message: String? = null,
    val busy: Boolean = false,
    val error: String? = null
)

data class AppUiState(
    val session: Session? = null,
    val prefs: AppPreferences = AppPreferences(),
    val dashboard: DashboardState = DashboardState(),
    val incomes: List<IncomeEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val investments: List<InvestmentEntity> = emptyList(),
    val queue: List<AutoCaptureEntity> = emptyList(),
    val queueCount: Int = 0,
    val insights: List<InsightEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val senders: List<SenderRuleEntity> = emptyList(),
    val imports: List<ImportBatchEntity> = emptyList(),
    val message: String? = null,
    /** True while a sign-in or sign-up request is in flight. */
    val authBusy: Boolean = false,
    /** Why the last sign-in or sign-up attempt failed, shown until the next attempt. */
    val authError: String? = null,
    val ready: Boolean = false,
    val sync: SyncSnapshot = SyncSnapshot()
)

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(
    private val container: AppContainer
) : ViewModel() {
    private val auth: AuthRepository = container.authRepository
    private val finance: FinanceRepository = container.financeRepository
    private val month = MutableStateFlow(YearMonth.now())
    private val flash = MutableStateFlow<String?>(null)
    private val authBusy = MutableStateFlow(false)
    private val authError = MutableStateFlow<String?>(null)
    private val syncState = MutableStateFlow(SyncSnapshot())
    private var syncJob: Job? = null

    init {
        // Sync once per signed-in user. The session flow also fires on every access-token
        // refresh, so gating on the user id avoids a full pass each time tokens rotate.
        viewModelScope.launch {
            var signedInUser: String? = null
            auth.session.collect { session ->
                val userId = session?.userId
                when {
                    userId == null -> signedInUser = null
                    userId != signedInUser -> {
                        signedInUser = userId
                        runSync(userId, notifyOnFailure = false)
                    }
                }
            }
        }
    }

    // combine() has typed overloads for at most five flows, so the three transient auth values are
    // folded into one first.
    private val authStage: Flow<AuthStage> = combine(authBusy, authError, flash) { busy, error, message ->
        AuthStage(message = message, busy = busy, error = error)
    }

    val uiState: StateFlow<AppUiState> = combine(
        auth.session,
        container.userPreferences.prefs,
        month,
        authStage,
        syncState
    ) { session, prefs, selectedMonth, stage, sync ->
        SessionFrame(session, prefs, selectedMonth, stage, sync)
    }.flatMapLatest { frame ->
        val session = frame.session
        if (session == null) {
            flowOf(
                AppUiState(
                    prefs = frame.prefs,
                    message = frame.auth.message,
                    authBusy = frame.auth.busy,
                    authError = frame.auth.error,
                    ready = true,
                    sync = frame.sync
                )
            )
        } else {
            observeLoggedIn(session, frame.prefs, frame.month, frame.auth, frame.sync)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState())

    private fun observeLoggedIn(
        session: Session,
        prefs: AppPreferences,
        selectedMonth: YearMonth,
        auth: AuthStage,
        sync: SyncSnapshot
    ): Flow<AppUiState> {
        val records: Flow<Triple<List<IncomeEntity>, List<ExpenseEntity>, List<InvestmentEntity>>> = combine(
            finance.observeIncome(session.userId),
            finance.observeExpense(session.userId),
            finance.observeInvestments(session.userId)
        ) { incomes, expenses, investments ->
            Triple(incomes, expenses, investments)
        }
        val extras: Flow<Triple<List<AutoCaptureEntity>, Int, List<InsightEntity>>> = combine(
            finance.observeQueue(session.userId),
            finance.observeQueueCount(session.userId),
            finance.observeInsights(session.userId)
        ) { queue, queueCount, insights ->
            Triple(queue, queueCount, insights)
        }
        val settings: Flow<Triple<List<BudgetEntity>, List<SenderRuleEntity>, List<ImportBatchEntity>>> = combine(
            finance.observeBudgets(session.userId),
            finance.observeSenderRules(session.userId),
            finance.observeImports(session.userId)
        ) { budgets, senders, imports ->
            Triple(budgets, senders, imports)
        }
        return combine(records, extras, settings) { rec, extra, set ->
            AppUiState(
                session = session,
                prefs = prefs,
                dashboard = buildDashboard(rec.first, rec.second, rec.third, selectedMonth),
                incomes = rec.first,
                expenses = rec.second,
                investments = rec.third,
                queue = extra.first,
                queueCount = extra.second,
                insights = extra.third,
                budgets = set.first,
                senders = set.second,
                imports = set.third,
                message = auth.message,
                authBusy = auth.busy,
                authError = auth.error,
                ready = true,
                sync = sync
            )
        }
    }

    fun signUp(email: String, password: String, name: String, onDone: (Boolean) -> Unit) {
        runAuth(onDone) { auth.signUp(email, password, name) }
    }

    fun login(email: String, password: String, onDone: (Boolean) -> Unit) {
        runAuth(onDone) { auth.login(email, password) }
    }

    /**
     * Google sign-in is not available in this build.
     *
     * Neon Auth does support it, but only through a browser redirect (open the provider, catch
     * the callback), which this app does not implement yet. Saying so plainly is the point: an
     * earlier build fabricated a `google-<uuid>` session that no server had ever issued, so
     * every call with it returned 401.
     */
    fun loginGoogle(onDone: (Boolean) -> Unit) {
        authError.value = "Google sign-in is not available yet. Please use your email and password."
        onDone(false)
    }

    /** Clears a previous failure when the user switches between the log-in and sign-up screens. */
    fun clearAuthError() {
        authError.value = null
    }

    /**
     * Runs a sign-in/sign-up call behind the shared busy flag.
     *
     * The backend is a free-tier instance that can take most of a minute to wake, so without this
     * an impatient second tap fired a second request: the duplicate register then failed with
     * 409 and looked like a failed sign-up even though the account had just been created.
     */
    private fun runAuth(onDone: (Boolean) -> Unit, block: suspend () -> Result<Session>) {
        if (authBusy.value) return
        viewModelScope.launch {
            authBusy.value = true
            authError.value = null
            try {
                val result = block()
                authError.value = result.exceptionOrNull()?.message
                onDone(result.isSuccess)
            } finally {
                authBusy.value = false
            }
        }
    }

    /** Runs a sync on demand and reports the outcome; used by the Settings button. */
    fun syncNow() {
        val userId = uiState.value.session?.userId ?: return
        viewModelScope.launch { runSync(userId, notifyOnFailure = true) }
    }

    fun logout() {
        viewModelScope.launch { auth.logout() }
    }

    fun completeOnboarding() {
        viewModelScope.launch { container.userPreferences.setOnboardingComplete(true) }
    }

    fun setMonth(value: YearMonth) {
        month.value = value
    }

    fun addIncome(
        amount: Double,
        source: String,
        category: IncomeCategory,
        date: Long,
        notes: String,
        recurring: Boolean
    ) {
        val userId = uiState.value.session?.userId ?: return
        viewModelScope.launch {
            val result = finance.addIncome(userId, amount, source, category, date, notes, recurring)
            flash.value = result.exceptionOrNull()?.message ?: "Income saved"
            scheduleSync()
        }
    }

    fun addExpense(
        amount: Double,
        merchant: String,
        category: ExpenseCategory,
        mode: PaymentMode,
        date: Long,
        notes: String,
        recurring: Boolean
    ) {
        val userId = uiState.value.session?.userId ?: return
        viewModelScope.launch {
            val result = finance.addExpense(userId, amount, merchant, category, mode, date, notes, recurring)
            flash.value = result.exceptionOrNull()?.message ?: "Expense saved"
            scheduleSync()
        }
    }

    fun addInvestment(
        name: String,
        type: InvestmentType,
        invested: Double,
        current: Double,
        date: Long,
        broker: String,
        notes: String
    ) {
        val userId = uiState.value.session?.userId ?: return
        viewModelScope.launch {
            val result = finance.addInvestment(userId, name, type, invested, current, date, broker, notes)
            flash.value = result.exceptionOrNull()?.message ?: "Investment saved"
            scheduleSync()
        }
    }

    fun updateIncome(record: IncomeEntity) = viewModelScope.launch {
        finance.updateIncome(record)
        scheduleSync()
    }

    fun updateExpense(record: ExpenseEntity) = viewModelScope.launch {
        finance.updateExpense(record)
        scheduleSync()
    }

    fun updateInvestment(record: InvestmentEntity) = viewModelScope.launch {
        finance.updateInvestment(record)
        scheduleSync()
    }

    fun deleteIncome(id: String) = viewModelScope.launch {
        finance.deleteIncome(id)
        scheduleSync()
    }

    fun deleteExpense(id: String) = viewModelScope.launch {
        finance.deleteExpense(id)
        scheduleSync()
    }

    fun deleteInvestment(id: String) = viewModelScope.launch {
        finance.deleteInvestment(id)
        scheduleSync()
    }

    /**
     * Accepting a capture hands it to the backend, which writes the transaction; the sync that is
     * scheduled right after is what brings the new record back down.
     */
    fun confirmCapture(item: AutoCaptureEntity) = viewModelScope.launch {
        val result = finance.confirmCapture(item)
        flash.value = result.exceptionOrNull()?.message
        if (result.isSuccess) scheduleSync()
    }

    fun rejectCapture(item: AutoCaptureEntity) = viewModelScope.launch {
        finance.rejectCapture(item)
        scheduleSync()
    }

    /**
     * Uploads a picked statement to the backend, which parses it with OpenRouter and stages the
     * rows for review. The phone never parses a statement itself, and this call does not return
     * until the parse is finished, so the result is one of: ready for review, failed server-side,
     * cancelled, or queued (offline).
     */
    suspend fun uploadStatement(uri: Uri, fileName: String): ImportUploadResult {
        val userId = uiState.value.session?.userId ?: return ImportUploadResult.Failed("Not signed in")
        val result = container.importManager.importPickedFile(userId, uri, fileName)
        scheduleSync()
        return result
    }

    /** The same server-side flow for statement text the app supplies (the bundled sample CSV). */
    suspend fun uploadStatementContent(fileName: String, content: String): ImportUploadResult {
        val userId = uiState.value.session?.userId ?: return ImportUploadResult.Failed("Not signed in")
        val result = container.importManager.importContent(userId, fileName, content)
        scheduleSync()
        return result
    }

    /**
     * Re-reads one batch's status from the server, so a parse left running when the import
     * screen went away can be watched again when it comes back.
     */
    suspend fun refreshImport(batch: ImportBatchEntity): ImportBatchEntity? =
        container.importManager.refreshStatus(batch)

    /** Loads the staged rows the server parsed, for the review list. */
    suspend fun stagedRows(batch: ImportBatchEntity): ImportUiBatch =
        withContext(Dispatchers.IO) { container.importManager.loadDetail(batch) }

    /**
     * Sends the review decisions; the backend writes the records, which arrive on this device
     * through the record pull of the sync scheduled by the caller on success.
     */
    suspend fun commitImport(batch: ImportBatchEntity, includedIds: Set<String>): Int {
        val count = container.importManager.commit(batch, includedIds)
        flash.value = "Imported $count transactions"
        scheduleSync()
        return count
    }

    /** Drops a batch (typically a failed parse) on the server and locally. */
    fun discardImport(batch: ImportBatchEntity) {
        viewModelScope.launch {
            container.importManager.discard(batch)
            scheduleSync()
        }
    }

    /**
     * Aborts the import that is in progress: the server cancels the batch and drops its staged
     * rows, and the local row is kept in the history flagged cancelled.
     *
     * @return false when the server could not be reached, so the caller can leave the import
     *         exactly as it was rather than claim a cancellation that did not happen.
     */
    suspend fun cancelImport(batch: ImportBatchEntity): Boolean {
        val cancelled = container.importManager.cancel(batch)
        if (cancelled) scheduleSync()
        return cancelled
    }

    fun enqueueSms(sender: String, body: String, parsed: ParsedTransaction) {
        val userId = uiState.value.session?.userId ?: return
        viewModelScope.launch {
            finance.enqueueAutoCapture(userId, sender, body, parsed)
            scheduleSync()
        }
    }

    fun setSmsEnabled(enabled: Boolean) = viewModelScope.launch { container.userPreferences.setSmsEnabled(enabled) }
    fun setEmailEnabled(enabled: Boolean) = viewModelScope.launch { container.userPreferences.setEmailEnabled(enabled) }
    fun setNotifications(enabled: Boolean) = viewModelScope.launch { container.userPreferences.setNotifications(enabled) }
    fun updateDisplayName(name: String) {
        val userId = uiState.value.session?.userId ?: return
        viewModelScope.launch {
            val result = auth.updateProfile(userId, name, uiState.value.prefs.currencyCode)
            flash.value = result.exceptionOrNull()?.message ?: "Profile updated"
        }
    }

    /** Currency lives in DataStore for instant UI feedback and on the server as the account default. */
    fun setCurrency(code: String) {
        val session = uiState.value.session
        viewModelScope.launch {
            container.userPreferences.setCurrency(code)
            if (session != null) {
                auth.updateProfile(session.userId, session.displayName, code)
            }
        }
    }
    fun setInsightFrequency(days: Int) = viewModelScope.launch { container.userPreferences.setInsightFrequency(days) }

    fun upsertBudget(category: ExpenseCategory, limit: Double) {
        val userId = uiState.value.session?.userId ?: return
        viewModelScope.launch {
            finance.upsertBudget(userId, category, limit)
            scheduleSync()
        }
    }

    fun upsertSender(sender: String, allowed: Boolean) {
        val userId = uiState.value.session?.userId ?: return
        viewModelScope.launch {
            finance.upsertSenderRule(userId, sender, allowed)
            scheduleSync()
        }
    }

    fun deleteSender(id: String) = viewModelScope.launch {
        finance.deleteSenderRule(id)
        scheduleSync()
    }

    /**
     * Insights are generated by the backend from the records it holds, so this needs a connection;
     * when the server has no AI provider configured it answers 503 and that message is shown here.
     */
    fun refreshInsights() {
        val userId = uiState.value.session?.userId ?: return
        viewModelScope.launch {
            val result = container.syncEngine.generateInsights(userId)
            flash.value = result.fold(
                onSuccess = { count -> if (count == 0) "No new insights" else "$count insights generated" },
                onFailure = { it.message }
            )
            if (result.isSuccess) scheduleSync()
        }
    }

    fun dismissInsight(item: InsightEntity) {
        viewModelScope.launch {
            finance.updateInsight(item.copy(dismissed = true))
            scheduleSync()
        }
    }

    fun saveInsight(item: InsightEntity) {
        viewModelScope.launch {
            finance.updateInsight(item.copy(saved = true))
            scheduleSync()
        }
    }

    /** The export is the account's, so a failure has to be reported rather than shared as empty. */
    fun exportData(onReady: (String) -> Unit) {
        viewModelScope.launch {
            val result = container.syncEngine.exportCsv()
            val csv = result.getOrNull()
            if (csv == null) flash.value = result.exceptionOrNull()?.message
            else onReady(csv)
        }
    }

    /**
     * Deletes every record the account owns: the server removes them, then the device forgets
     * the session and its local copy. No password is involved — nothing here can verify one, and
     * Neon Auth has no account-deletion route to verify it against.
     */
    fun deleteAccount() {
        val userId = uiState.value.session?.userId ?: return
        viewModelScope.launch {
            val result = auth.deleteAccount()
            if (result.isFailure) {
                flash.value = result.exceptionOrNull()?.message
                return@launch
            }
            finance.wipeUserData(userId)
            withContext(Dispatchers.IO) { container.statementStore.deleteForUser(userId) }
            auth.clearLocalAccount(userId)
            container.userPreferences.clear()
            flash.value = "Account deleted"
        }
    }

    private suspend fun runSync(userId: String, notifyOnFailure: Boolean) {
        syncState.value = syncState.value.copy(running = true, error = null)
        val result = container.syncEngine.sync(userId)
        syncState.value = SyncSnapshot(running = false, lastAt = Dates.now(), error = result.message)
        if (notifyOnFailure && result.message != null) flash.value = result.message
    }

    /**
     * Collapses the sync triggered by a burst of edits into a single pass once the user stops
     * writing, instead of a network round-trip per keystroke.
     */
    private fun scheduleSync() {
        val userId = uiState.value.session?.userId ?: return
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            delay(SYNC_DEBOUNCE_MS)
            runSync(userId, notifyOnFailure = false)
        }
    }

    fun consumeMessage() {
        flash.value = null
    }

    private fun buildDashboard(
        incomes: List<IncomeEntity>,
        expenses: List<ExpenseEntity>,
        investments: List<InvestmentEntity>,
        selectedMonth: YearMonth
    ): DashboardState {
        fun inMonth(epoch: Long, month: YearMonth) = Dates.toLocalDate(epoch).let {
            it.year == month.year && it.month == month.month
        }
        val monthIncome = incomes.filter { inMonth(it.date, selectedMonth) }
        val monthExpense = expenses.filter { inMonth(it.date, selectedMonth) }
        val feed = (
            monthIncome.map {
                FeedItem(it.id, it.source, it.category.name, it.amount, it.date, "income", it.origin != RecordOrigin.MANUAL)
            } + monthExpense.map {
                FeedItem(it.id, it.merchant, it.category.name, -it.amount, it.date, "expense", it.origin != RecordOrigin.MANUAL)
            } + investments.filter { inMonth(it.date, selectedMonth) }.map {
                FeedItem(it.id, it.instrumentName, it.type.name, it.amountInvested, it.date, "investment", it.origin != RecordOrigin.MANUAL)
            }
            ).sortedByDescending { it.date }.take(8)
        val byCat = monthExpense.groupBy { it.category.name }
            .map { it.key to it.value.sumOf { e -> e.amount } }
            .sortedByDescending { it.second }
        val months = Dates.monthsBack(6)
        return DashboardState(
            month = selectedMonth,
            income = monthIncome.sumOf { it.amount },
            expense = monthExpense.sumOf { it.amount },
            invested = investments.sumOf { it.amountInvested },
            currentValue = investments.sumOf { it.currentValue },
            recent = feed,
            expenseByCategory = byCat,
            incomeTrend = months.map { m -> Dates.formatMonth(m) to incomes.filter { inMonth(it.date, m) }.sumOf { it.amount } },
            expenseTrend = months.map { m -> Dates.formatMonth(m) to expenses.filter { inMonth(it.date, m) }.sumOf { it.amount } }
        )
    }

    companion object {
        private const val SYNC_DEBOUNCE_MS = 1_500L

        fun factory(container: AppContainer): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AppViewModel(container) as T
                }
            }
        }
    }
}

private data class SessionFrame(
    val session: Session?,
    val prefs: AppPreferences,
    val month: YearMonth,
    val auth: AuthStage,
    val sync: SyncSnapshot
)
