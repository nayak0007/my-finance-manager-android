package com.myfinancemanager.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowOutward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SouthWest
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.myfinancemanager.app.ui.AppUiState
import com.myfinancemanager.app.ui.FeedItem
import com.myfinancemanager.app.ui.components.CategoryBadge
import com.myfinancemanager.app.ui.components.CircleIconButton
import com.myfinancemanager.app.ui.components.DonutRing
import com.myfinancemanager.app.ui.components.EmptyState
import com.myfinancemanager.app.ui.components.ListRow
import com.myfinancemanager.app.ui.components.PillButton
import com.myfinancemanager.app.ui.components.PillButtonVariant
import com.myfinancemanager.app.ui.components.ReceiptCard
import com.myfinancemanager.app.ui.components.SegmentedTabs
import com.myfinancemanager.app.ui.components.StatColumn
import com.myfinancemanager.app.ui.components.SurfaceCard
import com.myfinancemanager.app.ui.components.TrendChart
import com.myfinancemanager.app.ui.theme.AxioLime
import com.myfinancemanager.app.ui.theme.ChartIndigo
import com.myfinancemanager.app.ui.theme.Ink900
import com.myfinancemanager.app.ui.theme.MoneyInvestLight
import com.myfinancemanager.app.ui.theme.MoneyPositiveLight
import com.myfinancemanager.app.ui.theme.OnCardLight
import com.myfinancemanager.app.ui.theme.TabularAmount
import com.myfinancemanager.app.ui.theme.TextMutedDark
import com.myfinancemanager.app.util.Dates
import com.myfinancemanager.app.util.Money
import com.myfinancemanager.app.util.titleCase
import java.time.YearMonth

private enum class DashTab(val label: String) {
    Trends("Trends"),
    Categories("Categories")
}

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
    var tab by remember { mutableStateOf(DashTab.Trends) }

    val budgetTotal = state.budgets.sumOf { it.monthlyLimit }
    val spent = dash.expense
    val ringBase = if (budgetTotal > 0) budgetTotal else dash.income
    val progress = if (ringBase > 0) (spent / ringBase).toFloat() else 0f
    val dayOfMonth = Dates.toLocalDate(Dates.now()).dayOfMonth
    val daysInMonth = dash.month.lengthOfMonth()
    val isCurrentMonth = dash.month == YearMonth.now()
    val daysLeft = if (isCurrentMonth) (daysInMonth - dayOfMonth + 1).coerceAtLeast(1) else daysInMonth
    val safeToSpend = if (budgetTotal > 0) ((budgetTotal - spent) / daysLeft).coerceAtLeast(0.0) else 0.0

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))
            GreetingBar(
            name = state.session?.displayName.orEmpty().ifBlank { "there" },
            queueCount = state.queueCount,
            onQueue = onQueue,
            onSettings = onSettings,
            onImport = onImport
        )
        Spacer(Modifier.height(20.dp))

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            CircleIconButton(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Previous month", onPrevMonth, size = 36.dp, iconSize = 20.dp)
            Text(
                Dates.formatMonth(dash.month),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            CircleIconButton(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Next month", onNextMonth, size = 36.dp, iconSize = 20.dp)
        }

        Spacer(Modifier.height(8.dp))
        SurfaceCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp)
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Spent in ${Dates.formatMonth(dash.month)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(20.dp))
                DonutRing(
                    progress = progress,
                    amountText = Money.format(spent, currency),
                    percentText = "${(progress.coerceIn(0f, 1f) * 100).toInt()}%",
                    progressColor = ChartIndigo
                )
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    StatColumn("Income", Money.format(dash.income, currency), Modifier.weight(1f), showEye = true)
                    VerticalDivider(Modifier.height(28.dp), color = MaterialTheme.colorScheme.outline)
                    StatColumn(
                        "Budget",
                        if (budgetTotal > 0) Money.format(budgetTotal, currency) else "Not set",
                        Modifier.weight(1f)
                    )
                    VerticalDivider(Modifier.height(28.dp), color = MaterialTheme.colorScheme.outline)
                    StatColumn(
                        "Safe to spend",
                        if (budgetTotal > 0) "${Money.format(safeToSpend, currency)}/day" else "--",
                        Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillButton("Income", onAddIncome, Modifier.weight(1f), variant = PillButtonVariant.Outlined, compact = true)
            PillButton(
                "Add expense",
                onAddExpense,
                Modifier.weight(1f),
                variant = PillButtonVariant.Lime,
                compact = true
            )
            PillButton("Invest", onAddInvestment, Modifier.weight(1f), variant = PillButtonVariant.Outlined, compact = true)
        }

        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SegmentedTabs(
                options = DashTab.entries,
                selected = tab,
                labelOf = { it.label },
                onSelect = { tab = it },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(12.dp))
        when (tab) {
            DashTab.Trends -> SurfaceCard(Modifier.fillMaxWidth()) {
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Spending trend", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                        Text("Last 6 months", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(12.dp))
                    TrendChart(dash.expenseTrend)
                    Spacer(Modifier.height(12.dp))
                    ChartLegend()
                }
            }

            DashTab.Categories -> SurfaceCard(Modifier.fillMaxWidth()) {
                Column {
                    Text("Spending by category", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.height(12.dp))
                    if (dash.expenseByCategory.isEmpty()) {
                        EmptyState("No expenses yet", "Add a spend or import a statement to see the breakdown.")
                    } else {
                        val total = dash.expenseByCategory.sumOf { it.second }.coerceAtLeast(0.01)
                        dash.expenseByCategory.forEach { item ->
                            CategoryRow(
                                category = item.first,
                                amount = Money.format(item.second, currency),
                                share = ((item.second / total) * 100).toInt()
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        ReceiptCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Recent activity",
                    style = MaterialTheme.typography.titleLarge,
                    color = OnCardLight,
                    modifier = Modifier.weight(1f)
                )
                Text("This month", style = MaterialTheme.typography.bodySmall, color = TextMutedDark)
            }
            Spacer(Modifier.height(8.dp))
            if (dash.recent.isEmpty()) {
                EmptyState("Nothing this month", "Use the add buttons to log cash, salary, or an investment.")
            } else {
                dash.recent.forEachIndexed { index, item ->
                    FeedRow(item, currency, onOpenItem)
                    if (index != dash.recent.lastIndex) {
                        HorizontalHairline()
                    }
                }
            }
        }
        Spacer(Modifier.height(96.dp))
    }
}

@Composable
private fun GreetingBar(
    name: String,
    queueCount: Int,
    onQueue: () -> Unit,
    onSettings: () -> Unit,
    onImport: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(AxioLime),
            contentAlignment = Alignment.Center
        ) {
            Text(
                name.trim().firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Ink900
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Hello, $name",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Here is where your money went",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        CircleIconButton(Icons.Rounded.FileUpload, "Import statement", onImport)
        Spacer(Modifier.width(4.dp))
        NotificationButton(queueCount, onQueue)
        Spacer(Modifier.width(4.dp))
        CircleIconButton(Icons.Rounded.Settings, "Settings", onSettings)
    }
}

@Composable
private fun NotificationButton(count: Int, onClick: () -> Unit) {
    Box(contentAlignment = Alignment.Center) {
        CircleIconButton(Icons.Rounded.Notifications, "Review queue", onClick)
        if (count > 0) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(AxioLime),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (count > 9) "9+" else "$count",
                    style = MaterialTheme.typography.labelSmall,
                    color = Ink900
                )
            }
        }
    }
}

@Composable
private fun CategoryRow(category: String, amount: String, share: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryBadge(category, size = 40.dp, iconSize = 18.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(category.titleCase(), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
            Text("$share% of spend", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(amount, style = TabularAmount, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun FeedRow(
    item: FeedItem,
    currency: String,
    onOpenItem: (String, String) -> Unit
) {
    val trailing: ImageVector = if (item.kind == "income") Icons.Rounded.SouthWest else Icons.Rounded.ArrowOutward
    val amount = Money.format(kotlin.math.abs(item.amount), currency)
    val amountColor = when (item.kind) {
        "income" -> MoneyPositiveLight
        "investment" -> MoneyInvestLight
        else -> OnCardLight
    }
    ListRow(
        title = item.title,
        meta = "${item.subtitle.titleCase()}${if (item.auto) " · Auto" else ""}",
        amount = amount,
        amountColor = amountColor,
        dateText = Dates.formatShort(item.date),
        badgeCategory = item.subtitle,
        trailingIcon = trailing,
        onClick = { onOpenItem(item.kind, item.id) },
        onLight = true
    )
}

@Composable
private fun HorizontalHairline() {
    androidx.compose.material3.HorizontalDivider(thickness = 1.dp, color = TextMutedDark.copy(alpha = 0.18f))
}

@Composable
private fun ChartLegend() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        LegendItem(ChartIndigo, "Monthly spend")
        LegendItem(AxioLime, "Trend line")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
