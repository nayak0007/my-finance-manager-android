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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.myfinancemanager.app.ui.AppUiState
import com.myfinancemanager.app.ui.components.EmptyState
import com.myfinancemanager.app.ui.components.LineChart
import com.myfinancemanager.app.ui.components.PieChart
import com.myfinancemanager.app.ui.components.QuickAddTile
import com.myfinancemanager.app.ui.components.SectionTitle
import com.myfinancemanager.app.ui.components.SummaryCard
import com.myfinancemanager.app.ui.theme.ExpenseRed
import com.myfinancemanager.app.ui.theme.IncomeGreen
import com.myfinancemanager.app.ui.theme.InvestBlue
import com.myfinancemanager.app.util.Dates
import com.myfinancemanager.app.util.Money
import com.myfinancemanager.app.util.titleCase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: AppUiState,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    onAddInvestment: () -> Unit,
    onImport: () -> Unit,
    onQueue: () -> Unit,
    onSettings: () -> Unit,
    onOpenItem: (String, String) -> Unit
) {
    val dash = state.dashboard
    val currency = state.prefs.currencyCode
    val savings = dash.income - dash.expense
    val pieColors = listOf(
        Color(0xFF0F6E56), Color(0xFF2B6CB0), Color(0xFFC44536), Color(0xFFD69E2E),
        Color(0xFF6B46C1), Color(0xFF319795), Color(0xFFDD6B20), Color(0xFF718096)
    )
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Hello, ${state.session?.displayName.orEmpty().ifBlank { "there" }}") },
            actions = {
                IconButton(onClick = onImport) { Icon(Icons.Filled.Add, contentDescription = "Import") }
                IconButton(onClick = onQueue) {
                    BadgedBox(badge = { if (state.queueCount > 0) Badge { Text("${state.queueCount}") } }) {
                        Icon(Icons.Filled.Notifications, contentDescription = "Review queue")
                    }
                }
                IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, contentDescription = "Settings") }
            }
        )
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevMonth) { Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Previous") }
                Text(Dates.formatMonth(dash.month), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                IconButton(onClick = onNextMonth) { Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Next") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryCard("Income", dash.income, currency, IncomeGreen, Modifier.weight(1f))
                SummaryCard("Expenses", dash.expense, currency, ExpenseRed, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryCard("Invested", dash.invested, currency, InvestBlue, Modifier.weight(1f))
                SummaryCard("Net savings", savings, currency, if (savings >= 0) IncomeGreen else ExpenseRed, Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickAddTile("Add income", onAddIncome)
                QuickAddTile("Add expense", onAddExpense)
                QuickAddTile("Add invest", onAddInvestment)
            }
            SectionTitle("Expense by category")
            if (dash.expenseByCategory.isEmpty()) {
                EmptyState("No expenses yet", "Add a spend or import a statement to see the breakdown.")
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PieChart(dash.expenseByCategory, pieColors)
                    Column(Modifier.padding(start = 12.dp)) {
                        dash.expenseByCategory.take(6).forEachIndexed { i, item ->
                            Text("${item.first.titleCase()}  ${Money.format(item.second, currency)}", color = pieColors[i % pieColors.size])
                        }
                    }
                }
            }
            SectionTitle("Income vs expense")
            LineChart(dash.incomeTrend, IncomeGreen)
            Spacer(Modifier.height(8.dp))
            LineChart(dash.expenseTrend, ExpenseRed)
            SectionTitle("Recent activity")
            if (dash.recent.isEmpty()) {
                EmptyState("Nothing this month", "Use + to log cash, salary, or an investment.")
            } else {
                dash.recent.forEach { item ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onOpenItem(item.kind, item.id) }.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(item.title, fontWeight = FontWeight.Medium)
                            Text(
                                "${item.subtitle.titleCase()} · ${Dates.formatShort(item.date)}${if (item.auto) " · Auto" else ""}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            Money.format(item.amount, currency),
                            color = when (item.kind) {
                                "income" -> IncomeGreen
                                "expense" -> ExpenseRed
                                else -> InvestBlue
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
