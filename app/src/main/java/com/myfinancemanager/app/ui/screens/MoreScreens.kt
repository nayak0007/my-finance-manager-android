package com.myfinancemanager.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.myfinancemanager.app.MyFinanceApp
import com.myfinancemanager.app.data.local.entity.AutoCaptureEntity
import com.myfinancemanager.app.data.local.entity.BudgetEntity
import com.myfinancemanager.app.data.local.entity.ExpenseCategory
import com.myfinancemanager.app.data.local.entity.ExpenseEntity
import com.myfinancemanager.app.data.local.entity.ImportBatchEntity
import com.myfinancemanager.app.data.local.entity.InsightEntity
import com.myfinancemanager.app.data.local.entity.SenderRuleEntity
import com.myfinancemanager.app.data.parser.ParsedTransaction
import com.myfinancemanager.app.data.prefs.AppPreferences
import com.myfinancemanager.app.sms.InboxScanner
import com.myfinancemanager.app.ui.AppViewModel
import com.myfinancemanager.app.ui.components.BudgetBar
import com.myfinancemanager.app.ui.components.EmptyState
import com.myfinancemanager.app.ui.components.EnumDropdown
import com.myfinancemanager.app.ui.components.MoneyField
import com.myfinancemanager.app.ui.components.SectionTitle
import com.myfinancemanager.app.util.Dates
import com.myfinancemanager.app.util.Money
import com.myfinancemanager.app.util.titleCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    viewModel: AppViewModel,
    batches: List<ImportBatchEntity>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var fileName by remember { mutableStateOf<String?>(null) }
    var parsed by remember { mutableStateOf<List<ParsedTransaction>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    val excluded = remember { mutableStateMapOf<Int, Boolean>() }
    val duplicates = remember { mutableStateMapOf<Int, Boolean>() }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val name = uri.lastPathSegment?.substringAfterLast('/') ?: "statement"
        fileName = name
        loading = true
        scope.launch {
            val rows = withContext(Dispatchers.IO) {
                MyFinanceApp.instance.container.statementImporter.parse(uri, name)
            }
            parsed = rows
            rows.forEachIndexed { index, tx ->
                duplicates[index] = viewModel.isDuplicate(tx)
                if (duplicates[index] == true) excluded[index] = true
            }
            loading = false
        }
    }
    Scaffold(topBar = {
        TopAppBar(title = { Text("Smart Import") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
        })
    }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Text("Upload a bank, card, or broker statement (CSV, TXT, or PDF text). Review every line before it is saved.")
            Spacer(Modifier.height(12.dp))
            Button(onClick = { picker.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) {
                Text("Choose statement file")
            }
            OutlinedButton(
                onClick = {
                    loading = true
                    scope.launch {
                        val text = withContext(Dispatchers.IO) {
                            context.assets.open("sample_statement.csv").bufferedReader().readText()
                        }
                        val rows = withContext(Dispatchers.IO) {
                            MyFinanceApp.instance.container.statementImporter.parseContent(text, "sample_statement.csv")
                        }
                        fileName = "sample_statement.csv"
                        parsed = rows
                        rows.forEachIndexed { index, tx ->
                            duplicates[index] = viewModel.isDuplicate(tx)
                            if (duplicates[index] == true) excluded[index] = true
                        }
                        loading = false
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Load sample statement") }
            if (loading) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("Parsing file…")
            }
            fileName?.let { Text("File: $it", modifier = Modifier.padding(top = 8.dp)) }
            if (parsed.isNotEmpty()) {
                Text("${parsed.size} rows parsed. Uncheck duplicates or junk before commit.", modifier = Modifier.padding(vertical = 8.dp))
                LazyColumn(modifier = Modifier.height(320.dp)) {
                    items(parsed.indices.toList()) { index ->
                        val tx = parsed[index]
                        val skip = excluded[index] == true
                        ListItem(
                            headlineContent = { Text("${tx.party} · ${tx.type.name.titleCase()}") },
                            supportingContent = {
                                Text("${Money.format(tx.amount)} · ${Dates.format(tx.date)}${if (duplicates[index] == true) " · duplicate" else ""}")
                            },
                            trailingContent = {
                                Checkbox(checked = !skip, onCheckedChange = { excluded[index] = !it })
                            }
                        )
                    }
                }
                Button(
                    onClick = {
                        val selected = parsed.filterIndexed { i, _ -> excluded[i] != true }
                        viewModel.commitImport(fileName ?: "statement", selected) { onBack() }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Confirm import") }
            }
            SectionTitle("Recent imports")
            if (batches.isEmpty()) Text("No imports yet.")
            batches.take(8).forEach {
                Text("${it.sourceFile} · ${it.committed}/${it.totalParsed} · ${Dates.format(it.createdAt)}")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    items: List<AutoCaptureEntity>,
    onBack: () -> Unit,
    onConfirm: (AutoCaptureEntity) -> Unit,
    onReject: (AutoCaptureEntity) -> Unit,
    onScanInbox: () -> Unit
) {
    Scaffold(topBar = {
        TopAppBar(title = { Text("Review queue") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
        }, actions = {
            TextButton(onClick = onScanInbox) { Text("Scan SMS") }
        })
    }) { padding ->
        if (items.isEmpty()) {
            EmptyState("Queue is clear", "Auto-detected SMS and import drafts will show here for confirmation.", Modifier.padding(padding))
        } else {
            LazyColumn(Modifier.padding(padding)) {
                items(items, key = { it.id }) { item ->
                    Card(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(item.parsedParty ?: "Unknown", fontWeight = FontWeight.SemiBold)
                            Text("${item.parsedType.name.titleCase()} · ${item.parsedAmount?.let { Money.format(it) } ?: "-"}")
                            Text(item.sender, style = MaterialTheme.typography.bodySmall)
                            Text(item.rawText, style = MaterialTheme.typography.bodySmall, maxLines = 4)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                                Button(onClick = { onConfirm(item) }) { Text("Confirm") }
                                OutlinedButton(onClick = { onReject(item) }) { Text("Reject") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InsightsScreen(
    insights: List<InsightEntity>,
    onRefresh: () -> Unit,
    onDismiss: (InsightEntity) -> Unit,
    onSave: (InsightEntity) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Insights", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Suggestions are informational only and are not certified financial, tax, or investment advice.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = onRefresh) { Text("Refresh insights") }
        Spacer(Modifier.height(12.dp))
        if (insights.isEmpty()) {
            EmptyState("No insights yet", "Add a few records, then refresh to generate observations.")
        } else {
            insights.forEach { item ->
                Card(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(item.category.titleCase(), style = MaterialTheme.typography.labelMedium)
                        Text(item.insightText, modifier = Modifier.padding(vertical = 8.dp))
                        Row {
                            TextButton(onClick = { onSave(item) }) { Text(if (item.saved) "Saved" else "Save") }
                            TextButton(onClick = { onDismiss(item) }) { Text("Dismiss") }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    prefs: AppPreferences,
    email: String,
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onSenders: () -> Unit,
    onRequestSms: () -> Unit,
    onExport: () -> Unit
) {
    var display by remember { mutableStateOf(email.substringBefore("@")) }
    Scaffold(topBar = {
        TopAppBar(title = { Text("Settings") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
        })
    }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Account", fontWeight = FontWeight.SemiBold)
            Text(email)
            OutlinedTextField(display, { display = it }, label = { Text("Display name") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { viewModel.updateDisplayName(display) }, modifier = Modifier.fillMaxWidth()) { Text("Save profile") }
            EnumDropdown("Currency", prefs.currencyCode, listOf("INR", "USD", "EUR", "GBP")) { viewModel.setCurrency(it) }
            SectionTitle("Capture")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("SMS auto-capture")
                    Text("Reads bank alerts on device only.", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = prefs.smsCaptureEnabled, onCheckedChange = {
                    if (it) onRequestSms() else viewModel.setSmsEnabled(false)
                })
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Email auto-capture")
                    Text("Connect Gmail later via backend OAuth.", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = prefs.emailCaptureEnabled, onCheckedChange = { viewModel.setEmailEnabled(it) })
            }
            OutlinedButton(onClick = onSenders, modifier = Modifier.fillMaxWidth()) { Text("Sender allow / block lists") }
            SectionTitle("Notifications")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Insight notifications")
                Switch(checked = prefs.notificationsEnabled, onCheckedChange = { viewModel.setNotifications(it) })
            }
            EnumDropdown("Insight frequency (days)", prefs.insightFrequencyDays, listOf(1, 3, 7, 14, 30)) {
                viewModel.setInsightFrequency(it)
            }
            SectionTitle("Data")
            OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) { Text("Export CSV") }
            OutlinedButton(onClick = { viewModel.logout() }, modifier = Modifier.fillMaxWidth()) { Text("Log out") }
            TextButton(onClick = { viewModel.deleteAccount() }) {
                Text("Delete account", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    budgets: List<BudgetEntity>,
    expenses: List<ExpenseEntity>,
    currency: String,
    onBack: () -> Unit,
    onSave: (ExpenseCategory, Double) -> Unit
) {
    var category by remember { mutableStateOf(ExpenseCategory.FOOD) }
    var amount by remember { mutableStateOf("") }
    val month = YearMonth.now()
    val spent = expenses.filter {
        val d = Dates.toLocalDate(it.date)
        d.year == month.year && d.month == month.month
    }.groupBy { it.category }.mapValues { it.value.sumOf { e -> e.amount } }
    Scaffold(topBar = {
        TopAppBar(title = { Text("Category budgets") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
        })
    }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            EnumDropdown("Category", category, ExpenseCategory.entries.toList()) { category = it }
            Spacer(Modifier.height(8.dp))
            MoneyField(amount, { amount = it }, "Monthly limit")
            Spacer(Modifier.height(8.dp))
            Button(onClick = { amount.toDoubleOrNull()?.let { onSave(category, it) } }, modifier = Modifier.fillMaxWidth()) {
                Text("Save budget")
            }
            Spacer(Modifier.height(16.dp))
            if (budgets.isEmpty()) {
                EmptyState("No budgets", "Optional v1 stretch: set a monthly cap per category.")
            } else {
                budgets.forEach { b ->
                    BudgetBar(spent[b.category] ?: 0.0, b.monthlyLimit, currency, b.category.name)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SenderScreen(
    rules: List<SenderRuleEntity>,
    onBack: () -> Unit,
    onSave: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit
) {
    var sender by remember { mutableStateOf("") }
    var allowed by remember { mutableStateOf(true) }
    Scaffold(topBar = {
        TopAppBar(title = { Text("SMS senders") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
        })
    }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(sender, { sender = it }, label = { Text("Sender ID (e.g. VM-HDFCBK)") }, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilterChip(selected = allowed, onClick = { allowed = true }, label = { Text("Allow") })
                Spacer(Modifier.padding(8.dp))
                FilterChip(selected = !allowed, onClick = { allowed = false }, label = { Text("Block") })
            }
            Button(onClick = { if (sender.isNotBlank()) onSave(sender, allowed) }, modifier = Modifier.fillMaxWidth()) {
                Text("Save rule")
            }
            Spacer(Modifier.height(12.dp))
            rules.forEach { rule ->
                ListItem(
                    headlineContent = { Text(rule.sender) },
                    supportingContent = { Text(if (rule.allowed) "Allowed" else "Blocked") },
                    trailingContent = { TextButton(onClick = { onDelete(rule.id) }) { Text("Remove") } }
                )
            }
        }
    }
}

@Composable
fun ScanInboxEffect(enabled: Boolean, viewModel: AppViewModel, onDone: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(enabled) {
        if (!enabled) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            InboxScanner.scan(context).forEach { sms ->
                viewModel.enqueueSms(sms.sender, sms.body, sms.parsed)
            }
        }
        onDone()
    }
}
