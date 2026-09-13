package com.myfinancemanager.app.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.myfinancemanager.app.data.local.entity.ExpenseCategory
import com.myfinancemanager.app.data.local.entity.ExpenseEntity
import com.myfinancemanager.app.data.local.entity.IncomeCategory
import com.myfinancemanager.app.data.local.entity.IncomeEntity
import com.myfinancemanager.app.data.local.entity.InvestmentEntity
import com.myfinancemanager.app.data.local.entity.InvestmentType
import com.myfinancemanager.app.data.local.entity.PaymentMode
import com.myfinancemanager.app.data.local.entity.RecordOrigin
import com.myfinancemanager.app.ui.components.EmptyState
import com.myfinancemanager.app.ui.components.EnumDropdown
import com.myfinancemanager.app.ui.components.FilterRow
import com.myfinancemanager.app.ui.components.MoneyField
import com.myfinancemanager.app.ui.components.OriginChip
import com.myfinancemanager.app.ui.theme.ExpenseRed
import com.myfinancemanager.app.ui.theme.IncomeGreen
import com.myfinancemanager.app.ui.theme.InvestBlue
import com.myfinancemanager.app.util.Dates
import com.myfinancemanager.app.util.Money
import com.myfinancemanager.app.util.titleCase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeListScreen(
    items: List<IncomeEntity>,
    currency: String,
    onAdd: () -> Unit,
    onOpen: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }
    val cats = listOf("All") + IncomeCategory.entries.map { it.name.titleCase() }
    val filtered = items.filter {
        (filter == "All" || it.category.name.titleCase() == filter) &&
            (query.isBlank() || it.source.contains(query, true) || it.notes.contains(query, true))
    }
    Scaffold(
        floatingActionButton = { FloatingActionButton(onClick = onAdd) { Icon(Icons.Filled.Add, contentDescription = "Add income") } }
    ) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 16.dp)) {
            Text("Income", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text("Search source") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            FilterRow(cats, filter) { filter = it }
            if (filtered.isEmpty()) {
                EmptyState("No income yet", "Log salary, freelance, or interest.")
            } else {
                LazyColumn {
                    items(filtered, key = { it.id }) { item ->
                        ListItem(
                            headlineContent = { Text(item.source) },
                            supportingContent = { Text("${item.category.name.titleCase()} · ${Dates.format(item.date)}") },
                            trailingContent = {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(Money.format(item.amount, currency), color = IncomeGreen, fontWeight = FontWeight.SemiBold)
                                    if (item.origin != RecordOrigin.MANUAL) Text("Auto", style = MaterialTheme.typography.labelSmall)
                                }
                            },
                            modifier = Modifier.clickable { onOpen(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(
    items: List<ExpenseEntity>,
    currency: String,
    onAdd: () -> Unit,
    onOpen: (String) -> Unit,
    onBudgets: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }
    val cats = listOf("All") + ExpenseCategory.entries.map { it.name.titleCase() }
    val filtered = items.filter {
        (filter == "All" || it.category.name.titleCase() == filter) &&
            (query.isBlank() || it.merchant.contains(query, true) || it.notes.contains(query, true) || it.paymentMode.name.contains(query, true))
    }
    Scaffold(
        floatingActionButton = { FloatingActionButton(onClick = onAdd) { Icon(Icons.Filled.Add, contentDescription = "Add expense") } }
    ) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Expenses", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                TextButton(onClick = onBudgets) { Text("Budgets") }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text("Search merchant, mode") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            FilterRow(cats, filter) { filter = it }
            if (filtered.isEmpty()) {
                EmptyState("No expenses yet", "Log cash spends or import a card statement.")
            } else {
                LazyColumn {
                    items(filtered, key = { it.id }) { item ->
                        ListItem(
                            headlineContent = { Text(item.merchant) },
                            supportingContent = { Text("${item.category.name.titleCase()} · ${item.paymentMode.name.titleCase()} · ${Dates.format(item.date)}") },
                            trailingContent = {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(Money.format(item.amount, currency), color = ExpenseRed, fontWeight = FontWeight.SemiBold)
                                    if (item.origin != RecordOrigin.MANUAL) Text("Auto", style = MaterialTheme.typography.labelSmall)
                                }
                            },
                            modifier = Modifier.clickable { onOpen(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentListScreen(
    items: List<InvestmentEntity>,
    currency: String,
    onAdd: () -> Unit,
    onOpen: (String) -> Unit
) {
    val invested = items.sumOf { it.amountInvested }
    val current = items.sumOf { it.currentValue }
    val gain = current - invested
    val byType = items.groupBy { it.type }.map { it.key.name.titleCase() to it.value.sumOf { v -> v.currentValue } }
    Scaffold(
        floatingActionButton = { FloatingActionButton(onClick = onAdd) { Icon(Icons.Filled.Add, contentDescription = "Add investment") } }
    ) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 16.dp)) {
            Text("Investments", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
            Text("Invested ${Money.format(invested, currency)}  ·  Value ${Money.format(current, currency)}")
            Text(
                "Gain/loss ${Money.format(gain, currency)} (${"%.1f".format(if (invested == 0.0) 0.0 else gain / invested * 100)}%)",
                color = if (gain >= 0) IncomeGreen else ExpenseRed,
                fontWeight = FontWeight.SemiBold
            )
            if (byType.isNotEmpty()) {
                Text("Allocation", fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 12.dp))
                byType.forEach { Text("${it.first}: ${Money.format(it.second, currency)}") }
            }
            Spacer(Modifier.height(8.dp))
            if (items.isEmpty()) {
                EmptyState("No holdings yet", "Add mutual funds, stocks, FDs, gold, or retirement accounts.")
            } else {
                LazyColumn {
                    items(items, key = { it.id }) { item ->
                        ListItem(
                            headlineContent = { Text(item.instrumentName) },
                            supportingContent = { Text("${item.type.name.titleCase()} · ${item.broker}") },
                            trailingContent = {
                                Text(Money.format(item.currentValue, currency), color = InvestBlue, fontWeight = FontWeight.SemiBold)
                            },
                            modifier = Modifier.clickable { onOpen(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIncomeScreen(onBack: () -> Unit, onSave: (Double, String, IncomeCategory, Long, String, Boolean) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(IncomeCategory.SALARY) }
    var recurring by remember { mutableStateOf(false) }
    var date by remember { mutableStateOf(Dates.now()) }
    FormScaffold("Add income", onBack) {
        MoneyField(amount) { amount = it }
        OutlinedTextField(source, { source = it }, label = { Text("Source") }, modifier = Modifier.fillMaxWidth())
        EnumDropdown("Category", category, IncomeCategory.entries.toList()) { category = it }
        DateField(date) { date = it }
        OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
        RecurringRow(recurring) { recurring = it }
        Button(
            onClick = { amount.toDoubleOrNull()?.let { onSave(it, source, category, date, notes, recurring) } },
            enabled = amount.toDoubleOrNull() != null && source.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(onBack: () -> Unit, onSave: (Double, String, ExpenseCategory, PaymentMode, Long, String, Boolean) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ExpenseCategory.FOOD) }
    var mode by remember { mutableStateOf(PaymentMode.UPI) }
    var recurring by remember { mutableStateOf(false) }
    var date by remember { mutableStateOf(Dates.now()) }
    FormScaffold("Add expense", onBack) {
        MoneyField(amount) { amount = it }
        OutlinedTextField(merchant, { merchant = it }, label = { Text("Merchant / payee") }, modifier = Modifier.fillMaxWidth())
        EnumDropdown("Category", category, ExpenseCategory.entries.toList()) { category = it }
        EnumDropdown("Payment mode", mode, PaymentMode.entries.toList()) { mode = it }
        DateField(date) { date = it }
        OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
        RecurringRow(recurring) { recurring = it }
        Button(
            onClick = { amount.toDoubleOrNull()?.let { onSave(it, merchant, category, mode, date, notes, recurring) } },
            enabled = amount.toDoubleOrNull() != null && merchant.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddInvestmentScreen(onBack: () -> Unit, onSave: (String, InvestmentType, Double, Double, Long, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var broker by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var invested by remember { mutableStateOf("") }
    var current by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(InvestmentType.MUTUAL_FUND) }
    var date by remember { mutableStateOf(Dates.now()) }
    FormScaffold("Add investment", onBack) {
        OutlinedTextField(name, { name = it }, label = { Text("Instrument name") }, modifier = Modifier.fillMaxWidth())
        EnumDropdown("Type", type, InvestmentType.entries.toList()) { type = it }
        MoneyField(invested, { invested = it }, "Amount invested")
        MoneyField(current, { current = it }, "Current value")
        OutlinedTextField(broker, { broker = it }, label = { Text("Broker / platform") }, modifier = Modifier.fillMaxWidth())
        DateField(date) { date = it }
        OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = {
                val inv = invested.toDoubleOrNull() ?: return@Button
                onSave(name, type, inv, current.toDoubleOrNull() ?: inv, date, broker, notes)
            },
            enabled = invested.toDoubleOrNull() != null && name.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeDetailScreen(item: IncomeEntity?, currency: String, onBack: () -> Unit, onSave: (IncomeEntity) -> Unit, onDelete: () -> Unit) {
    if (item == null) {
        Missing(onBack); return
    }
    var source by remember { mutableStateOf(item.source) }
    var notes by remember { mutableStateOf(item.notes) }
    var category by remember { mutableStateOf(item.category) }
    var amount by remember { mutableStateOf(item.amount.toString()) }
    FormScaffold("Income", onBack, onDelete) {
        OriginChip(item.origin.name, item.origin != RecordOrigin.MANUAL)
        MoneyField(amount) { amount = it }
        OutlinedTextField(source, { source = it }, label = { Text("Source") }, modifier = Modifier.fillMaxWidth())
        EnumDropdown("Category", category, IncomeCategory.entries.toList()) { category = it }
        OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
        Text(Dates.format(item.date), style = MaterialTheme.typography.bodySmall)
        Button(onClick = {
            amount.toDoubleOrNull()?.let { onSave(item.copy(amount = it, source = source, notes = notes, category = category)) }
        }, modifier = Modifier.fillMaxWidth()) { Text("Save changes") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailScreen(item: ExpenseEntity?, currency: String, onBack: () -> Unit, onSave: (ExpenseEntity) -> Unit, onDelete: () -> Unit) {
    if (item == null) {
        Missing(onBack); return
    }
    var merchant by remember { mutableStateOf(item.merchant) }
    var notes by remember { mutableStateOf(item.notes) }
    var category by remember { mutableStateOf(item.category) }
    var mode by remember { mutableStateOf(item.paymentMode) }
    var amount by remember { mutableStateOf(item.amount.toString()) }
    FormScaffold("Expense", onBack, onDelete) {
        OriginChip(item.origin.name, item.origin != RecordOrigin.MANUAL)
        MoneyField(amount) { amount = it }
        OutlinedTextField(merchant, { merchant = it }, label = { Text("Merchant") }, modifier = Modifier.fillMaxWidth())
        EnumDropdown("Category", category, ExpenseCategory.entries.toList()) { category = it }
        EnumDropdown("Payment mode", mode, PaymentMode.entries.toList()) { mode = it }
        OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            amount.toDoubleOrNull()?.let {
                onSave(item.copy(amount = it, merchant = merchant, notes = notes, category = category, paymentMode = mode))
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Save changes") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentDetailScreen(item: InvestmentEntity?, currency: String, onBack: () -> Unit, onSave: (InvestmentEntity) -> Unit, onDelete: () -> Unit) {
    if (item == null) {
        Missing(onBack); return
    }
    var name by remember { mutableStateOf(item.instrumentName) }
    var notes by remember { mutableStateOf(item.notes) }
    var broker by remember { mutableStateOf(item.broker) }
    var invested by remember { mutableStateOf(item.amountInvested.toString()) }
    var current by remember { mutableStateOf(item.currentValue.toString()) }
    var type by remember { mutableStateOf(item.type) }
    FormScaffold("Investment", onBack, onDelete) {
        OriginChip(item.origin.name, item.origin != RecordOrigin.MANUAL)
        OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
        EnumDropdown("Type", type, InvestmentType.entries.toList()) { type = it }
        MoneyField(invested, { invested = it }, "Amount invested")
        MoneyField(current, { current = it }, "Current value")
        OutlinedTextField(broker, { broker = it }, label = { Text("Broker") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            val inv = invested.toDoubleOrNull() ?: return@Button
            val cur = current.toDoubleOrNull() ?: inv
            onSave(item.copy(instrumentName = name, type = type, amountInvested = inv, currentValue = cur, broker = broker, notes = notes))
        }, modifier = Modifier.fillMaxWidth()) { Text("Save changes") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormScaffold(title: String, onBack: () -> Unit, onDelete: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    if (onDelete != null) {
                        IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            content()
        }
    }
}

@Composable
private fun RecurringRow(value: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = value, onCheckedChange = onChange)
        Text("Recurring")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(epoch: Long, onChange: (Long) -> Unit) {
    var open by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = Dates.format(epoch),
        onValueChange = {},
        readOnly = true,
        label = { Text("Date") },
        modifier = Modifier.fillMaxWidth().clickable { open = true }
    )
    TextButton(onClick = { open = true }) { Text("Change date") }
    if (open) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = epoch)
        DatePickerDialog(onDismissRequest = { open = false }, confirmButton = {
            TextButton(onClick = {
                picker.selectedDateMillis?.let(onChange)
                open = false
            }) { Text("OK") }
        }) { DatePicker(state = picker) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Missing(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Record not found")
        TextButton(onClick = onBack) { Text("Back") }
    }
}
