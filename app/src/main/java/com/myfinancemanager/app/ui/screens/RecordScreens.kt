package com.myfinancemanager.app.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowOutward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SouthWest
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.myfinancemanager.app.data.local.entity.ExpenseCategory
import com.myfinancemanager.app.data.local.entity.ExpenseEntity
import com.myfinancemanager.app.data.local.entity.IncomeCategory
import com.myfinancemanager.app.data.local.entity.IncomeEntity
import com.myfinancemanager.app.data.local.entity.InvestmentEntity
import com.myfinancemanager.app.data.local.entity.InvestmentType
import com.myfinancemanager.app.data.local.entity.PaymentMode
import com.myfinancemanager.app.data.local.entity.RecordOrigin
import com.myfinancemanager.app.ui.components.CategoryBadge
import com.myfinancemanager.app.ui.components.CircleIconButton
import com.myfinancemanager.app.ui.components.EmptyState
import com.myfinancemanager.app.ui.components.EnumDropdown
import com.myfinancemanager.app.ui.components.FilledTextField
import com.myfinancemanager.app.ui.components.FilterRow
import com.myfinancemanager.app.ui.components.ListRow
import com.myfinancemanager.app.ui.components.MoneyField
import com.myfinancemanager.app.ui.components.OriginChip
import com.myfinancemanager.app.ui.components.PillButton
import com.myfinancemanager.app.ui.components.PillButtonVariant
import com.myfinancemanager.app.ui.components.SectionTitle
import com.myfinancemanager.app.ui.components.SurfaceCard
import com.myfinancemanager.app.ui.components.StatTile
import com.myfinancemanager.app.ui.components.axioClickable
import com.myfinancemanager.app.ui.theme.AxioLime
import com.myfinancemanager.app.ui.theme.Ink900
import com.myfinancemanager.app.ui.theme.LocalMoneyColors
import com.myfinancemanager.app.ui.theme.TabularAmount
import com.myfinancemanager.app.ui.theme.TextSecondaryDark
import com.myfinancemanager.app.util.Dates
import com.myfinancemanager.app.util.Money
import com.myfinancemanager.app.util.titleCase

// ---------------------------------------------------------------------------------------------
// Screens
// ---------------------------------------------------------------------------------------------

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
    val money = LocalMoneyColors.current
    val cats = listOf("All") + IncomeCategory.entries.map { it.name.titleCase() }
    val filtered = items.filter {
        (filter == "All" || it.category.name.titleCase() == filter) &&
            (query.isBlank() || it.source.contains(query, true) || it.notes.contains(query, true))
    }
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        ListHeader(title = "Income", onAdd = onAdd)
        SearchField(query, "Search source") { query = it }
        Spacer(Modifier.height(12.dp))
        FilterRow(cats, filter) { filter = it }
        Spacer(Modifier.height(4.dp))
        if (filtered.isEmpty()) {
            EmptyState("No income yet", "Log salary, freelance, or interest.")
        } else {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 96.dp)) {
                items(filtered, key = { it.id }) { item ->
                    ListRow(
                        title = item.source,
                        meta = "${item.category.name.titleCase()} · ${Dates.format(item.date)}${if (item.origin != RecordOrigin.MANUAL) " · Auto" else ""}",
                        amount = Money.format(item.amount, currency),
                        amountColor = money.positive,
                        badgeCategory = item.category.name,
                        trailingIcon = Icons.Rounded.SouthWest,
                        onClick = { onOpen(item.id) }
                    )
                    RowDivider()
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
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        ListHeader(title = "Expenses", onAdd = onAdd, onBudgets = onBudgets)
        SearchField(query, "Search merchant, mode") { query = it }
        Spacer(Modifier.height(12.dp))
        FilterRow(cats, filter) { filter = it }
        Spacer(Modifier.height(4.dp))
        if (filtered.isEmpty()) {
            EmptyState("No expenses yet", "Log cash spends or import a card statement.")
        } else {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 96.dp)) {
                items(filtered, key = { it.id }) { item ->
                    ListRow(
                        title = item.merchant,
                        meta = "${item.category.name.titleCase()} · ${item.paymentMode.name.titleCase()} · ${Dates.format(item.date)}${if (item.origin != RecordOrigin.MANUAL) " · Auto" else ""}",
                        amount = Money.format(item.amount, currency),
                        amountColor = MaterialTheme.colorScheme.onBackground,
                        badgeCategory = item.category.name,
                        trailingIcon = Icons.Rounded.ArrowOutward,
                        onClick = { onOpen(item.id) }
                    )
                    RowDivider()
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
    val money = LocalMoneyColors.current
    val invested = items.sumOf { it.amountInvested }
    val current = items.sumOf { it.currentValue }
    val gain = current - invested
    val byType = items.groupBy { it.type }.map { it.key.name.titleCase() to it.value.sumOf { v -> v.currentValue } }
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        ListHeader(title = "Investments", onAdd = onAdd)
        SurfaceCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
            Column {
                Text("Portfolio value", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Text(
                    Money.format(current, currency),
                    style = com.myfinancemanager.app.ui.theme.HeroNumber,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("Invested", Money.format(invested, currency), MaterialTheme.colorScheme.onBackground, Modifier.weight(1f))
                    StatTile(
                        "Gain/loss",
                        Money.format(gain, currency),
                        if (gain >= 0) money.positive else money.negative,
                        Modifier.weight(1f)
                    )
                }
            }
        }
        if (byType.isNotEmpty()) {
            SectionTitle("Allocation")
            byType.forEach { entry ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    CategoryBadge(entry.first, size = 36.dp, iconSize = 16.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(entry.first, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
                    Text(Money.format(entry.second, currency), style = TabularAmount, color = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (items.isEmpty()) {
            EmptyState("No holdings yet", "Add mutual funds, stocks, FDs, gold, or retirement accounts.")
        } else {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 96.dp)) {
                items(items, key = { it.id }) { item ->
                    ListRow(
                        title = item.instrumentName,
                        meta = "${item.type.name.titleCase()} · ${item.broker.ifBlank { "No broker" }}",
                        amount = Money.format(item.currentValue, currency),
                        amountColor = money.invest,
                        badgeCategory = item.type.name,
                        trailingIcon = Icons.AutoMirrored.Rounded.TrendingUp,
                        onClick = { onOpen(item.id) }
                    )
                    RowDivider()
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Add / detail forms
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIncomeScreen(onBack: () -> Unit, onSave: (Double, String, IncomeCategory, Long, String, Boolean) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(IncomeCategory.SALARY) }
    var recurring by remember { mutableStateOf(false) }
    var date by remember { mutableStateOf(Dates.now()) }
    FormScaffold(
        title = "Add income",
        onBack = onBack,
        onSave = { amount.toDoubleOrNull()?.let { onSave(it, source, category, date, notes, recurring) } },
        saveEnabled = amount.toDoubleOrNull() != null && source.isNotBlank()
    ) {
        MoneyField(amount) { amount = it }
        FilledTextField(source, { source = it }, label = "Source")
        CategoryPicker(category.name, IncomeCategory.entries.map { it.name }) { category = IncomeCategory.valueOf(it) }
        DateField(date) { date = it }
        FilledTextField(notes, { notes = it }, label = "Notes")
        RecurringRow(recurring) { recurring = it }
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
    FormScaffold(
        title = "Add expense",
        onBack = onBack,
        onSave = { amount.toDoubleOrNull()?.let { onSave(it, merchant, category, mode, date, notes, recurring) } },
        saveEnabled = amount.toDoubleOrNull() != null && merchant.isNotBlank()
    ) {
        MoneyField(amount) { amount = it }
        FilledTextField(merchant, { merchant = it }, label = "Merchant / payee")
        CategoryPicker(category.name, ExpenseCategory.entries.map { it.name }) { category = ExpenseCategory.valueOf(it) }
        EnumDropdown("Payment mode", mode, PaymentMode.entries.toList()) { mode = it }
        DateField(date) { date = it }
        FilledTextField(notes, { notes = it }, label = "Notes")
        RecurringRow(recurring) { recurring = it }
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
    FormScaffold(
        title = "Add investment",
        onBack = onBack,
        onSave = {
            val inv = invested.toDoubleOrNull()
            if (inv != null) {
                onSave(name, type, inv, current.toDoubleOrNull() ?: inv, date, broker, notes)
            }
        },
        saveEnabled = invested.toDoubleOrNull() != null && name.isNotBlank()
    ) {
        FilledTextField(name, { name = it }, label = "Instrument name")
        EnumDropdown("Type", type, InvestmentType.entries.toList()) { type = it }
        MoneyField(invested, "Amount invested") { invested = it }
        MoneyField(current, "Current value") { current = it }
        FilledTextField(broker, { broker = it }, label = "Broker / platform")
        DateField(date) { date = it }
        FilledTextField(notes, { notes = it }, label = "Notes")
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
    FormScaffold(
        title = "Income",
        onBack = onBack,
        onDelete = onDelete,
        onSave = {
            amount.toDoubleOrNull()?.let { onSave(item.copy(amount = it, source = source, notes = notes, category = category)) }
        },
        saveText = "Save changes"
    ) {
        OriginChip(item.origin.name, item.origin != RecordOrigin.MANUAL)
        MoneyField(amount) { amount = it }
        FilledTextField(source, { source = it }, label = "Source")
        CategoryPicker(category.name, IncomeCategory.entries.map { it.name }) { category = IncomeCategory.valueOf(it) }
        FilledTextField(notes, { notes = it }, label = "Notes")
        Text(Dates.format(item.date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    FormScaffold(
        title = "Expense",
        onBack = onBack,
        onDelete = onDelete,
        onSave = {
            amount.toDoubleOrNull()?.let {
                onSave(item.copy(amount = it, merchant = merchant, notes = notes, category = category, paymentMode = mode))
            }
        },
        saveText = "Save changes"
    ) {
        OriginChip(item.origin.name, item.origin != RecordOrigin.MANUAL)
        MoneyField(amount) { amount = it }
        FilledTextField(merchant, { merchant = it }, label = "Merchant")
        CategoryPicker(category.name, ExpenseCategory.entries.map { it.name }) { category = ExpenseCategory.valueOf(it) }
        EnumDropdown("Payment mode", mode, PaymentMode.entries.toList()) { mode = it }
        FilledTextField(notes, { notes = it }, label = "Notes")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentDetailScreen(item: InvestmentEntity?, currency: String, onBack: () -> Unit, onSave: (InvestmentEntity) -> Unit, onDelete: () -> Unit) {
    if (item == null) {
        Missing(onBack); return
    }
    val money = LocalMoneyColors.current
    var name by remember { mutableStateOf(item.instrumentName) }
    var notes by remember { mutableStateOf(item.notes) }
    var broker by remember { mutableStateOf(item.broker) }
    var invested by remember { mutableStateOf(item.amountInvested.toString()) }
    var current by remember { mutableStateOf(item.currentValue.toString()) }
    var type by remember { mutableStateOf(item.type) }
    FormScaffold(
        title = "Investment",
        onBack = onBack,
        onDelete = onDelete,
        onSave = {
            val inv = invested.toDoubleOrNull()
            val cur = current.toDoubleOrNull() ?: inv
            if (inv != null && cur != null) {
                onSave(item.copy(instrumentName = name, type = type, amountInvested = inv, currentValue = cur, broker = broker, notes = notes))
            }
        },
        saveText = "Save changes"
    ) {
        OriginChip(item.origin.name, item.origin != RecordOrigin.MANUAL)
        FilledTextField(name, { name = it }, label = "Name")
        EnumDropdown("Type", type, InvestmentType.entries.toList()) { type = it }
        MoneyField(invested, "Amount invested") { invested = it }
        MoneyField(current, "Current value") { current = it }
        FilledTextField(broker, { broker = it }, label = "Broker")
        FilledTextField(notes, { notes = it }, label = "Notes")
        val gain = (current.toDoubleOrNull() ?: item.currentValue) - (invested.toDoubleOrNull() ?: item.amountInvested)
        Text(
            "Gain/loss ${Money.format(gain, currency)}",
            style = MaterialTheme.typography.bodyLarge,
            color = if (gain >= 0) money.positive else money.negative
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Shared form pieces
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormScaffold(
    title: String,
    onBack: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onSave: (() -> Unit)? = null,
    saveEnabled: Boolean = true,
    saveText: String = "Save",
    content: @Composable () -> Unit
) {
    Scaffold(
        containerColor = Ink900,
        topBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircleIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack, filled = true)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
                if (onDelete != null) {
                    CircleIconButton(Icons.Rounded.Delete, "Delete", onDelete, tint = MaterialTheme.colorScheme.error)
                }
            }
        },
        bottomBar = {
            if (onSave != null) {
                Surface(color = Ink900) {
                    PillButton(
                        text = saveText,
                        onClick = onSave,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .navigationBarsPadding(),
                        variant = PillButtonVariant.Lime,
                        enabled = saveEnabled
                    )
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            content()
            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(epoch: Long, onChange: (Long) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        FilledTextField(
            value = Dates.format(epoch),
            onValueChange = {},
            label = "Date",
            enabled = false,
            trailing = { Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        )
        Box(Modifier.matchParentSize().axioClickable { open = true })
    }
    if (open) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = epoch)
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let(onChange)
                    open = false
                }) { Text("OK", color = AxioLime) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } }
        ) { DatePicker(state = picker) }
    }
}

@Composable
private fun CategoryPicker(selected: String, options: List<String>, onSelect: (String) -> Unit) {
    Column {
        Text(
            "Category",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            options.forEach { name ->
                val isSelected = name == selected
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .border(
                                width = 2.dp,
                                color = if (isSelected) AxioLime else androidx.compose.ui.graphics.Color.Transparent,
                                shape = CircleShape
                            )
                            .axioClickable { onSelect(name) },
                        contentAlignment = Alignment.Center
                    ) {
                        CategoryBadge(name, size = 42.dp, iconSize = 18.dp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        name.titleCase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.onBackground else TextSecondaryDark,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun RecurringRow(value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("Recurring", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
            Text("Repeats every month", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = value,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Ink900,
                checkedTrackColor = AxioLime,
                uncheckedThumbColor = TextSecondaryDark,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        )
    }
}

@Composable
private fun ListHeader(title: String, onAdd: () -> Unit, onBudgets: (() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        if (onBudgets != null) {
            PillButton("Budgets", onBudgets, variant = PillButtonVariant.Outlined, compact = true)
            Spacer(Modifier.width(8.dp))
        }
        PillButton("Add", onAdd, variant = PillButtonVariant.Lime, compact = true, icon = Icons.Rounded.Add)
    }
}

@Composable
private fun SearchField(value: String, placeholder: String, onChange: (String) -> Unit) {
    FilledTextField(
        value = value,
        onValueChange = onChange,
        label = "",
        placeholder = placeholder,
        trailing = { Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
    )
}

@Composable
private fun RowDivider() {
    androidx.compose.material3.HorizontalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Missing(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Record not found", color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        PillButton("Back", onBack, variant = PillButtonVariant.Outlined)
    }
}
