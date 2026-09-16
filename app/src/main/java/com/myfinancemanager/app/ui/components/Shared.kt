package com.myfinancemanager.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.myfinancemanager.app.ui.theme.ExpenseRed
import com.myfinancemanager.app.ui.theme.IncomeGreen
import com.myfinancemanager.app.util.Money
import com.myfinancemanager.app.util.titleCase

@Composable
fun SummaryCard(
    title: String,
    amount: Double,
    currency: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Spacer(Modifier.height(6.dp))
            Text(
                Money.format(amount, currency),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
    }
}

@Composable
fun MoneyField(value: String, label: String = "Amount", onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw -> onValueChange(raw.filter { it.isDigit() || it == '.' }) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun <T> EnumDropdown(
    label: String,
    selected: T,
    options: List<T>,
    labelOf: (T) -> String = { it.toString().titleCase() },
    onSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = labelOf(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth().clickable { expanded = true }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(labelOf(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun OriginChip(origin: String, auto: Boolean) {
    AssistChip(
        onClick = {},
        label = { Text(if (auto) "Auto-detected" else origin.titleCase()) }
    )
}

@Composable
fun BudgetBar(spent: Double, limit: Double, currency: String, category: String) {
    val ratio = if (limit <= 0) 0f else (spent / limit).toFloat().coerceIn(0f, 1.4f)
    val over = spent > limit && limit > 0
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(category.titleCase(), style = MaterialTheme.typography.bodyMedium)
            Text(
                "${Money.format(spent, currency)} / ${Money.format(limit, currency)}",
                style = MaterialTheme.typography.labelMedium,
                color = if (over) ExpenseRed else MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = ratio.coerceAtMost(1f),
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = if (over) ExpenseRed else IncomeGreen
        )
    }
}

@Composable
fun PieChart(slices: List<Pair<String, Double>>, colors: List<Color>, modifier: Modifier = Modifier) {
    val total = slices.sumOf { it.second }.toFloat().coerceAtLeast(0.01f)
    Canvas(modifier = modifier.size(180.dp)) {
        var start = -90f
        slices.forEachIndexed { index, slice ->
            val sweep = ((slice.second.toFloat() / total) * 360f)
            drawArc(
                color = colors[index % colors.size],
                startAngle = start,
                sweepAngle = sweep,
                useCenter = true,
                size = Size(size.minDimension, size.minDimension)
            )
            start += sweep
        }
    }
}

@Composable
fun LineChart(points: List<Pair<String, Double>>, color: Color, modifier: Modifier = Modifier) {
    val max = (points.maxOfOrNull { it.second } ?: 1.0).coerceAtLeast(1.0)
    Canvas(modifier = modifier.fillMaxWidth().height(140.dp)) {
        if (points.size < 2) return@Canvas
        val step = size.width / (points.size - 1).coerceAtLeast(1)
        val path = Path()
        points.forEachIndexed { i, p ->
            val x = i * step
            val y = size.height - ((p.second / max) * size.height).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = color, style = Stroke(width = 6f))
        points.forEachIndexed { i, p ->
            val x = i * step
            val y = size.height - ((p.second / max) * size.height).toFloat()
            drawCircle(color, radius = 8f, center = Offset(x, y))
        }
    }
}

@Composable
fun FilterRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        options.forEach { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelect(option) },
                label = { Text(option) }
            )
        }
    }
}

@Composable
fun RowScope.QuickAddTile(title: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.weight(1f).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Box(Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
            Text(title, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}
