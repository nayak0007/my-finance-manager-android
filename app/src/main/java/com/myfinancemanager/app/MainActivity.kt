package com.myfinancemanager.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.content.FileProvider
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.TextButton
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.myfinancemanager.app.ui.AppViewModel
import com.myfinancemanager.app.ui.navigation.Routes
import com.myfinancemanager.app.ui.navigation.TabDest
import com.myfinancemanager.app.ui.screens.AddExpenseScreen
import com.myfinancemanager.app.ui.screens.AddIncomeScreen
import com.myfinancemanager.app.ui.screens.AddInvestmentScreen
import com.myfinancemanager.app.ui.screens.BudgetScreen
import com.myfinancemanager.app.ui.screens.DashboardScreen
import com.myfinancemanager.app.ui.screens.ExpenseDetailScreen
import com.myfinancemanager.app.ui.screens.ExpenseListScreen
import com.myfinancemanager.app.ui.screens.ImportScreen
import com.myfinancemanager.app.ui.screens.IncomeDetailScreen
import com.myfinancemanager.app.ui.screens.IncomeListScreen
import com.myfinancemanager.app.ui.screens.InsightsScreen
import com.myfinancemanager.app.ui.screens.InvestmentDetailScreen
import com.myfinancemanager.app.ui.screens.InvestmentListScreen
import com.myfinancemanager.app.ui.screens.LoginScreen
import com.myfinancemanager.app.ui.screens.OnboardingScreen
import com.myfinancemanager.app.ui.screens.QueueScreen
import com.myfinancemanager.app.ui.screens.ScanInboxEffect
import com.myfinancemanager.app.ui.screens.SenderScreen
import com.myfinancemanager.app.ui.screens.SettingsScreen
import com.myfinancemanager.app.ui.screens.SignupScreen
import com.myfinancemanager.app.ui.theme.MyFinanceTheme
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as MyFinanceApp
        setContent {
            MyFinanceTheme {
                val vm: AppViewModel = viewModel(factory = AppViewModel.factory(app.container))
                FinanceRoot(vm)
            }
        }
    }
}

@Composable
private fun FinanceRoot(viewModel: AppViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    var scanInbox by remember { mutableStateOf(false) }
    var showSmsRationale by remember { mutableStateOf(false) }
    val activity = androidx.compose.ui.platform.LocalContext.current as MainActivity
    val smsPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        val granted = grants[Manifest.permission.READ_SMS] == true
        viewModel.setSmsEnabled(granted)
    }
    val notifyPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.setNotifications(granted)
    }

    LaunchedEffect(state.message) {
        val msg = state.message ?: return@LaunchedEffect
        snackbar.showSnackbar(msg)
        viewModel.consumeMessage()
    }

    LaunchedEffect(state.ready, state.session, state.prefs.onboardingComplete) {
        if (!state.ready) return@LaunchedEffect
        val target = when {
            state.session == null -> Routes.Login
            !state.prefs.onboardingComplete -> Routes.Onboarding
            else -> Routes.Home
        }
        val current = nav.currentDestination?.route
        if (current != target && (state.session == null || target != Routes.Login)) {
            if (state.session == null && current != Routes.Signup) {
                nav.navigate(Routes.Login) {
                    popUpTo(nav.graph.findStartDestination().id) { inclusive = true }
                    launchSingleTop = true
                }
            } else if (state.session != null && current in setOf(Routes.Login, Routes.Signup, Routes.Splash, null)) {
                nav.navigate(target) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    ScanInboxEffect(scanInbox, viewModel) { scanInbox = false }

    if (showSmsRationale) {
        AlertDialog(
            onDismissRequest = { showSmsRationale = false },
            title = { Text("SMS access") },
            text = { Text("SMS access is used only to detect bank and UPI transaction alerts so they can be reviewed before saving. Nothing is shared or sold. You can turn this off in Settings at any time.") },
            confirmButton = {
                TextButton(onClick = {
                    showSmsRationale = false
                    smsPermission.launch(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS))
                }) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { showSmsRationale = false }) { Text("Not now") }
            }
        )
    }

    if (!state.ready) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val tabs = TabDest.entries
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBar = currentRoute in tabs.map { it.route }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(Routes.Home) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = when (tab) {
                                        TabDest.Home -> Icons.Outlined.Home
                                        TabDest.Income -> Icons.Outlined.Payments
                                        TabDest.Expense -> Icons.Outlined.AccountBalance
                                        TabDest.Investments -> Icons.Outlined.TrendingUp
                                        TabDest.Insights -> Icons.Outlined.Lightbulb
                                    },
                                    contentDescription = tab.label
                                )
                            },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.Splash,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.Splash) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
            composable(Routes.Login) {
                LoginScreen(viewModel, state.message) { nav.navigate(Routes.Signup) }
            }
            composable(Routes.Signup) {
                SignupScreen(viewModel, state.message) { nav.popBackStack() }
            }
            composable(Routes.Onboarding) {
                OnboardingScreen { sms, notify ->
                    if (sms) {
                        showSmsRationale = true
                    }
                    if (notify && Build.VERSION.SDK_INT >= 33) {
                        notifyPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    viewModel.completeOnboarding()
                    nav.navigate(Routes.Home) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            composable(Routes.Home) {
                DashboardScreen(
                    state = state,
                    onPrevMonth = { viewModel.setMonth(state.dashboard.month.minusMonths(1)) },
                    onNextMonth = { viewModel.setMonth(state.dashboard.month.plusMonths(1)) },
                    onAddIncome = { nav.navigate(Routes.AddIncome) },
                    onAddExpense = { nav.navigate(Routes.AddExpense) },
                    onAddInvestment = { nav.navigate(Routes.AddInvestment) },
                    onImport = { nav.navigate(Routes.Import) },
                    onQueue = { nav.navigate(Routes.Queue) },
                    onSettings = { nav.navigate(Routes.Settings) },
                    onOpenItem = { kind, id ->
                        when (kind) {
                            "income" -> nav.navigate(Routes.incomeDetail(id))
                            "expense" -> nav.navigate(Routes.expenseDetail(id))
                            else -> nav.navigate(Routes.investmentDetail(id))
                        }
                    }
                )
            }
            composable(Routes.Income) {
                IncomeListScreen(state.incomes, state.prefs.currencyCode, { nav.navigate(Routes.AddIncome) }) {
                    nav.navigate(Routes.incomeDetail(it))
                }
            }
            composable(Routes.Expense) {
                ExpenseListScreen(
                    items = state.expenses,
                    currency = state.prefs.currencyCode,
                    onAdd = { nav.navigate(Routes.AddExpense) },
                    onOpen = { nav.navigate(Routes.expenseDetail(it)) },
                    onBudgets = { nav.navigate(Routes.Budgets) }
                )
            }
            composable(Routes.Investments) {
                InvestmentListScreen(state.investments, state.prefs.currencyCode, { nav.navigate(Routes.AddInvestment) }) {
                    nav.navigate(Routes.investmentDetail(it))
                }
            }
            composable(Routes.Insights) {
                InsightsScreen(state.insights, viewModel::refreshInsights, viewModel::dismissInsight, viewModel::saveInsight)
            }
            composable(Routes.AddIncome) {
                AddIncomeScreen(onBack = { nav.popBackStack() }) { amount, source, cat, date, notes, rec ->
                    viewModel.addIncome(amount, source, cat, date, notes, rec)
                    nav.popBackStack()
                }
            }
            composable(Routes.AddExpense) {
                AddExpenseScreen(onBack = { nav.popBackStack() }) { amount, merchant, cat, mode, date, notes, rec ->
                    viewModel.addExpense(amount, merchant, cat, mode, date, notes, rec)
                    nav.popBackStack()
                }
            }
            composable(Routes.AddInvestment) {
                AddInvestmentScreen(onBack = { nav.popBackStack() }) { name, type, invested, current, date, broker, notes ->
                    viewModel.addInvestment(name, type, invested, current, date, broker, notes)
                    nav.popBackStack()
                }
            }
            composable(Routes.IncomeDetail, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                val id = entry.arguments?.getString("id")
                IncomeDetailScreen(
                    item = state.incomes.firstOrNull { it.id == id },
                    currency = state.prefs.currencyCode,
                    onBack = { nav.popBackStack() },
                    onSave = { viewModel.updateIncome(it); nav.popBackStack() },
                    onDelete = { id?.let(viewModel::deleteIncome); nav.popBackStack() }
                )
            }
            composable(Routes.ExpenseDetail, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                val id = entry.arguments?.getString("id")
                ExpenseDetailScreen(
                    item = state.expenses.firstOrNull { it.id == id },
                    currency = state.prefs.currencyCode,
                    onBack = { nav.popBackStack() },
                    onSave = { viewModel.updateExpense(it); nav.popBackStack() },
                    onDelete = { id?.let(viewModel::deleteExpense); nav.popBackStack() }
                )
            }
            composable(Routes.InvestmentDetail, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                val id = entry.arguments?.getString("id")
                InvestmentDetailScreen(
                    item = state.investments.firstOrNull { it.id == id },
                    currency = state.prefs.currencyCode,
                    onBack = { nav.popBackStack() },
                    onSave = { viewModel.updateInvestment(it); nav.popBackStack() },
                    onDelete = { id?.let(viewModel::deleteInvestment); nav.popBackStack() }
                )
            }
            composable(Routes.Import) {
                ImportScreen(viewModel, state.imports) { nav.popBackStack() }
            }
            composable(Routes.Queue) {
                QueueScreen(
                    items = state.queue,
                    onBack = { nav.popBackStack() },
                    onConfirm = viewModel::confirmCapture,
                    onReject = viewModel::rejectCapture,
                    onScanInbox = {
                        val granted = ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
                        if (granted) scanInbox = true else smsPermission.launch(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS))
                    }
                )
            }
            composable(Routes.Settings) {
                SettingsScreen(
                    prefs = state.prefs,
                    email = state.session?.email.orEmpty(),
                    viewModel = viewModel,
                    onBack = { nav.popBackStack() },
                    onSenders = { nav.navigate(Routes.Senders) },
                    onRequestSms = { showSmsRationale = true },
                    onExport = {
                        viewModel.exportData { csv ->
                            val dir = File(activity.filesDir, "exports").apply { mkdirs() }
                            val file = File(dir, "finance-export.csv")
                            file.writeText(csv)
                            val uri = FileProvider.getUriForFile(
                                activity,
                                "${activity.packageName}.fileprovider",
                                file
                            )
                            val share = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            activity.startActivity(Intent.createChooser(share, "Export data"))
                        }
                    }
                )
            }
            composable(Routes.Budgets) {
                BudgetScreen(state.budgets, state.expenses, state.prefs.currencyCode, { nav.popBackStack() }, viewModel::upsertBudget)
            }
            composable(Routes.Senders) {
                SenderScreen(state.senders, { nav.popBackStack() }, viewModel::upsertSender, viewModel::deleteSender)
            }
        }
    }
}
