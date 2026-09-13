package com.myfinancemanager.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    val ready: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(
    private val container: AppContainer
) : ViewModel() {
    private val auth: AuthRepository = container.authRepository
    private val finance: FinanceRepository = container.financeRepository
    private val month = MutableStateFlow(YearMonth.now())
    private val flash = MutableStateFlow<String?>(null)

    val uiState: StateFlow<AppUiState> = combine(
        auth.session,
        container.userPreferences.prefs,
        month,
        flash
    ) { session, prefs, selectedMonth, message ->
        SessionFrame(session, prefs, selectedMonth, message)
    }.flatMapLatest { frame ->
        val session = frame.session
        if (session == null) {
            flowOf(AppUiState(prefs = frame.prefs, message = frame.message, ready = true))
        } else {
            observeLoggedIn(session, frame.prefs, frame.month, frame.message)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState())

    private fun observeLoggedIn(
        session: Session,
        prefs: AppPreferences,
        selectedMonth: YearMonth,
        message: String?
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
                message = message,
                ready = true
            )
        }
    }

    fun signUp(email: String, password: String, name: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = auth.signUp(email, password, name)
            flash.value = result.exceptionOrNull()?.message
            onDone(result.isSuccess)
        }
    }

    fun login(email: String, password: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = auth.login(email, password)
            flash.value = result.exceptionOrNull()?.message
            onDone(result.isSuccess)
        }
    }

    fun loginGoogle(email: String, name: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = auth.loginWithGoogle(email, name)
            flash.value = result.exceptionOrNull()?.message
            onDone(result.isSuccess)
        }
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
        }
    }

    fun updateIncome(record: IncomeEntity) = viewModelScope.launch { finance.updateIncome(record) }
    fun updateExpense(record: ExpenseEntity) = viewModelScope.launch { finance.updateExpense(record) }
    fun updateInvestment(record: InvestmentEntity) = viewModelScope.launch { finance.updateInvestment(record) }
    fun deleteIncome(id: String) = viewModelScope.launch { finance.deleteIncome(id) }
    fun deleteExpense(id: String) = viewModelScope.launch { finance.deleteExpense(id) }
    fun deleteInvestment(id: String) = viewModelScope.launch { finance.deleteInvestment(id) }

    fun confirmCapture(item: AutoCaptureEntity) = viewModelScope.launch { finance.confirmCapture(item) }
    fun rejectCapture(item: AutoCaptureEntity) = viewModelScope.launch { finance.rejectCapture(item) }

    fun commitImport(fileName: String, selected: List<ParsedTransaction>, onDone: (Int) -> Unit) {
        val userId = uiState.value.session?.userId ?: return
        viewModelScope.launch {
            val batch = finance.commitImport(userId, fileName, selected)
            flash.value = "Imported ${batch.committed} of ${batch.totalParsed} records"
            onDone(batch.committed)
        }
    }

    suspend fun isDuplicate(tx: ParsedTransaction): Boolean {
        val userId = uiState.value.session?.userId ?: return false
        return finance.isDuplicate(userId, tx)
    }

    fun enqueueSms(sender: String, body: String, parsed: ParsedTransaction) {
        val userId = uiState.value.session?.userId ?: return
        viewModelScope.launch { finance.enqueueAutoCapture(userId, sender, body, parsed) }
    }

    fun setSmsEnabled(enabled: Boolean) = viewModelScope.launch { container.userPreferences.setSmsEnabled(enabled) }
    fun setEmailEnabled(enabled: Boolean) = viewModelScope.launch { container.userPreferences.setEmailEnabled(enabled) }
    fun setNotifications(enabled: Boolean) = viewModelScope.launch { container.userPreferences.setNotifications(enabled) }
    fun updateDisplayName(name: String) {
        val userId = uiState.value.session?.userId ?: return
        viewModelScope.launch {
            auth.updateProfile(userId, name, uiState.value.prefs.currencyCode)
            flash.value = "Profile updated"
        }
    }

    fun setCurrency(code: String) = viewModelScope.launch { container.userPreferences.setCurrency(code) }
    fun setInsightFrequency(days: Int) = viewModelScope.launch { container.userPreferences.setInsightFrequency(days) }

    fun upsertBudget(category: ExpenseCategory, limit: Double) {
        val userId = uiState.value.session?.userId ?: return
        viewModelScope.launch { finance.upsertBudget(userId, category, limit) }
    }

    fun upsertSender(sender: String, allowed: Boolean) {
        val userId = uiState.value.session?.userId ?: return
        viewModelScope.launch { finance.upsertSenderRule(userId, sender, allowed) }
    }

    fun deleteSender(id: String) = viewModelScope.launch { finance.deleteSenderRule(id) }

    fun refreshInsights() {
        val state = uiState.value
        val userId = state.session?.userId ?: return
        viewModelScope.launch { finance.refreshInsights(userId, state.prefs.currencyCode) }
    }

    fun dismissInsight(item: InsightEntity) = viewModelScope.launch { finance.updateInsight(item.copy(dismissed = true)) }
    fun saveInsight(item: InsightEntity) = viewModelScope.launch { finance.updateInsight(item.copy(saved = true)) }

    fun exportData(onReady: (String) -> Unit) {
        val userId = uiState.value.session?.userId ?: return
        viewModelScope.launch { onReady(finance.exportCsv(userId)) }
    }

    fun deleteAccount() {
        val userId = uiState.value.session?.userId ?: return
        viewModelScope.launch {
            finance.wipeUserData(userId)
            auth.deleteAccount(userId)
            container.userPreferences.clear()
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
    val message: String?
)
