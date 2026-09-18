package com.myfinancemanager.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowOutward
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.LocalGroceryStore
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myfinancemanager.app.ui.theme.AxioLime
import com.myfinancemanager.app.ui.theme.AmountInput
import com.myfinancemanager.app.ui.theme.CardLight
import com.myfinancemanager.app.ui.theme.CategoryPalette
import com.myfinancemanager.app.ui.theme.ChartIndigo
import com.myfinancemanager.app.ui.theme.ChartIndigoSelected
import com.myfinancemanager.app.ui.theme.EyebrowPurple
import com.myfinancemanager.app.ui.theme.FeatureLilac
import com.myfinancemanager.app.ui.theme.FeatureMint
import com.myfinancemanager.app.ui.theme.HeroNumber
import com.myfinancemanager.app.ui.theme.Ink700
import com.myfinancemanager.app.ui.theme.Ink800
import com.myfinancemanager.app.ui.theme.InkOutlineStrong
import com.myfinancemanager.app.ui.theme.LocalMoneyColors
import com.myfinancemanager.app.ui.theme.OnCardLight
import com.myfinancemanager.app.ui.theme.PillShape
import com.myfinancemanager.app.ui.theme.ReceiptCardShape
import com.myfinancemanager.app.ui.theme.RingTrackDark
import com.myfinancemanager.app.ui.theme.StatValue
import com.myfinancemanager.app.ui.theme.TabularAmount
import com.myfinancemanager.app.ui.theme.TextMutedDark
import com.myfinancemanager.app.util.Money
import com.myfinancemanager.app.util.titleCase
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sin

private const val PI_F = 3.1415927f

// ---------------------------------------------------------------------------------------------
// Category colours and glyphs
// ---------------------------------------------------------------------------------------------

private val CategoryColorMap = mapOf(
    "SHOPPING" to Color(0xFF00B0D0),
    "CARD" to Color(0xFF00B0D0),
    "BILLS" to Color(0xFFF5A623),
    "UTILITIES" to Color(0xFFF5A623),
    "RENT" to Color(0xFF802040),
    "TRAVEL" to Color(0xFF9050D0),
    "TRANSPORT" to Color(0xFF9050D0),
    "FOOD" to Color(0xFF7AE04A),
    "GROCERY" to Color(0xFF7AE04A),
    "ENTERTAINMENT" to Color(0xFF4050E0),
    "SUBSCRIPTIONS" to Color(0xFF4050E0),
    "HEALTH" to Color(0xFFF06060),
    "EDUCATION" to Color(0xFF00B0D0),
    "OTHER" to Color(0xFF9E9E9E),
    "TRANSFER" to Color(0xFF9E9E9E),
    "SALARY" to Color(0xFFB4E300),
    "INCOME" to Color(0xFFB4E300),
    "FREELANCE" to Color(0xFFB4E300),
    "INTEREST" to Color(0xFF4CAF50),
    "RENTAL" to Color(0xFF7AE04A),
    "MUTUAL_FUND" to Color(0xFF4050E0),
    "STOCK" to Color(0xFF9050D0),
    "FD" to Color(0xFF4CAF50),
    "BOND" to Color(0xFF00B0D0),
    "GOLD" to Color(0xFFF5A623),
    "RETIREMENT" to Color(0xFF5B7CFA),
    "CRYPTO" to Color(0xFFFF9800)
)

/** Deterministic: the same category name keeps the same badge colour everywhere. */
fun categoryColorFor(name: String): Color {
    val key = name.trim().uppercase()
    return CategoryColorMap[key] ?: CategoryPalette[key.hashCode().absoluteValue % CategoryPalette.size]
}

fun categoryIconFor(name: String): ImageVector {
    return when (name.trim().uppercase()) {
        "SHOPPING" -> Icons.Rounded.ShoppingCart
        "CARD" -> Icons.AutoMirrored.Rounded.ReceiptLong
        "TRAVEL" -> Icons.Rounded.Flight
        "TRANSPORT" -> Icons.Rounded.DirectionsBus
        "FOOD" -> Icons.Rounded.Restaurant
        "GROCERY" -> Icons.Rounded.LocalGroceryStore
        "ENTERTAINMENT", "SUBSCRIPTIONS" -> Icons.Rounded.Movie
        "HEALTH" -> Icons.Rounded.MedicalServices
        "EDUCATION" -> Icons.Rounded.School
        "BILLS", "UTILITIES", "RENT" -> Icons.AutoMirrored.Rounded.ReceiptLong
        "SALARY", "INCOME", "FREELANCE", "INTEREST", "RENTAL" -> Icons.Rounded.Payments
        "MUTUAL_FUND", "STOCK", "FD", "BOND", "GOLD", "RETIREMENT", "CRYPTO" -> Icons.AutoMirrored.Rounded.TrendingUp
        "TRANSFER", "OTHER" -> Icons.Rounded.SwapHoriz
        else -> Icons.Rounded.Category
    }
}

// ---------------------------------------------------------------------------------------------
// Interaction
// ---------------------------------------------------------------------------------------------

/** A press that scales the container to 0.97 for 80ms without ever moving layout. */
@Composable
fun Modifier.axioClickable(enabled: Boolean = true, onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.97f else 1f,
        animationSpec = tween(durationMillis = if (pressed) 80 else 120),
        label = "press"
    )
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

// ---------------------------------------------------------------------------------------------
// Buttons
// ---------------------------------------------------------------------------------------------

enum class PillButtonVariant { Lime, Dark, Outlined, Text }

@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: PillButtonVariant = PillButtonVariant.Dark,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    loading: Boolean = false,
    compact: Boolean = false
) {
    val active = enabled && !loading
    val height = if (compact) 36.dp else 48.dp
    val contentPadding = PaddingValues(horizontal = if (compact) 16.dp else 24.dp)
    val spinnerColor = when (variant) {
        PillButtonVariant.Lime, PillButtonVariant.Dark -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onBackground
    }
    val label: @Composable () -> Unit = {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = spinnerColor
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Text(
                    text,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
    }
    when (variant) {
        PillButtonVariant.Lime -> Button(
            onClick = onClick,
            modifier = modifier.height(height),
            enabled = active,
            shape = PillShape,
            contentPadding = contentPadding,
            colors = ButtonDefaults.buttonColors(
                containerColor = AxioLime,
                contentColor = OnCardLight,
                disabledContainerColor = AxioLime.copy(alpha = 0.35f),
                disabledContentColor = OnCardLight.copy(alpha = 0.6f)
            ),
            content = { label() }
        )

        PillButtonVariant.Dark -> Button(
            onClick = onClick,
            modifier = modifier.height(height),
            enabled = active,
            shape = PillShape,
            contentPadding = contentPadding,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                contentColor = Color.White,
                disabledContainerColor = Color.Black.copy(alpha = 0.4f),
                disabledContentColor = Color.White.copy(alpha = 0.6f)
            ),
            content = { label() }
        )

        PillButtonVariant.Outlined -> OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(height),
            enabled = active,
            shape = PillShape,
            contentPadding = contentPadding,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground),
            content = { label() }
        )

        PillButtonVariant.Text -> TextButton(
            onClick = onClick,
            modifier = modifier.height(height),
            enabled = active,
            shape = PillShape,
            contentPadding = contentPadding,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
            content = { label() }
        )
    }
}

/** 40dp circular action button - axio's canonical row affordance. */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 18.dp,
    filled: Boolean = false,
    onLight: Boolean = false,
    tint: Color? = null
) {
    val background = when {
        onLight -> Color.White
        filled -> Ink800
        else -> Color.Transparent
    }
    val border = when {
        onLight -> null
        filled -> null
        else -> androidx.compose.foundation.BorderStroke(1.dp, InkOutlineStrong)
    }
    val resolvedTint = tint ?: if (onLight) OnCardLight else MaterialTheme.colorScheme.onBackground
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(if (border != null) Modifier.border(border, CircleShape) else Modifier)
            .background(background)
            .axioClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(iconSize), tint = resolvedTint)
    }
}

// ---------------------------------------------------------------------------------------------
// Cards and surfaces
// ---------------------------------------------------------------------------------------------

@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    color: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(color)
            .padding(contentPadding)
    ) { content() }
}

/** The light "receipt" card that jumps out of the dark canvas. */
@Composable
fun ReceiptCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = ReceiptCardShape,
        color = CardLight,
        contentColor = OnCardLight,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(Modifier.padding(contentPadding)) { content() }
    }
}

@Composable
fun StatTile(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    SurfaceCard(modifier = modifier, shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(14.dp)) {
        Column {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Spacer(Modifier.height(6.dp))
            Text(
                value,
                style = StatValue,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    SurfaceCard(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
        Column(Modifier.padding(vertical = 4.dp)) { content() }
    }
}

// ---------------------------------------------------------------------------------------------
// Inputs
// ---------------------------------------------------------------------------------------------

@Composable
fun FilledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    isError: Boolean = false,
    helperText: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    textAlign: TextAlign = TextAlign.Start,
    prefix: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Column(modifier) {
        if (label.isNotBlank()) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        TextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            readOnly = readOnly,
            singleLine = singleLine,
            isError = isError,
            textStyle = textStyle.copy(textAlign = textAlign),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            placeholder = placeholder?.let {
                { Text(it, color = TextMutedDark, style = textStyle.copy(textAlign = textAlign)) }
            },
            prefix = prefix?.let { { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = textStyle) } },
            trailingIcon = trailing,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Ink800,
                unfocusedContainerColor = Ink800,
                disabledContainerColor = Ink800,
                errorContainerColor = Ink800,
                focusedIndicatorColor = AxioLime,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = MaterialTheme.colorScheme.error,
                cursorColor = AxioLime,
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                disabledTextColor = MaterialTheme.colorScheme.onBackground,
                errorTextColor = MaterialTheme.colorScheme.onBackground
            )
        )
        if (helperText != null) {
            Text(
                helperText,
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

/** Amount input: 24sp semibold, currency prefix, numeric keyboard, right aligned. */
@Composable
fun MoneyField(
    value: String,
    label: String = "Amount",
    currencySymbol: String = "\u20B9",
    onValueChange: (String) -> Unit
) {
    FilledTextField(
        value = value,
        onValueChange = { raw -> onValueChange(raw.filter { it.isDigit() || it == '.' }) },
        label = label,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        textStyle = AmountInput,
        textAlign = TextAlign.End,
        prefix = currencySymbol
    )
}

@Composable
fun <T> EnumDropdown(
    label: String,
    selected: T,
    options: List<T>,
    modifier: Modifier = Modifier,
    labelOf: (T) -> String = { it.toString().titleCase() },
    onSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth()) {
        FilledTextField(
            value = labelOf(selected),
            onValueChange = {},
            label = label,
            enabled = false,
            trailing = {
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        )
        Box(Modifier.matchParentSize().axioClickable { expanded = true })
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Ink700
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(labelOf(option), color = MaterialTheme.colorScheme.onBackground) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Chips and tabs
// ---------------------------------------------------------------------------------------------

@Composable
fun AxioFilterChip(
    text: String,
    selected: Boolean,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    val background = if (selected) AxioLime else Ink800
    val contentColor = if (selected) OnCardLight else MaterialTheme.colorScheme.onBackground
    Row(
        modifier = Modifier
            .height(36.dp)
            .clip(PillShape)
            .background(background)
            .then(
                if (selected) Modifier
                else Modifier.border(1.dp, InkOutlineStrong, PillShape)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = contentColor)
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = contentColor)
    }
}

@Composable
fun FilterRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        options.forEach { option ->
            AxioFilterChip(text = option, selected = selected == option, onClick = { onSelect(option) })
        }
    }
}

@Composable
fun <T> SegmentedTabs(
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    iconOf: ((T) -> ImageVector?)? = null
) {
    Row(
        modifier = modifier
            .clip(PillShape)
            .background(Ink800)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            Row(
                modifier = Modifier
                    .clip(PillShape)
                    .background(if (isSelected) Color(0xFF2E2E2E) else Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                iconOf?.invoke(option)?.let {
                    Icon(it, contentDescription = null, modifier = Modifier.size(16.dp), tint = contentColor)
                }
                Text(labelOf(option), style = MaterialTheme.typography.labelLarge, color = contentColor)
            }
        }
    }
}

@Composable
fun OriginChip(origin: String, auto: Boolean) {
    val text = if (auto) "Auto-detected" else origin.titleCase()
    Row(
        modifier = Modifier
            .clip(PillShape)
            .background(if (auto) AxioLime.copy(alpha = 0.15f) else Ink800)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (auto) {
            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = AxioLime)
        }
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = if (auto) AxioLime else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Stats and lists
// ---------------------------------------------------------------------------------------------

@Composable
fun StatColumn(label: String, value: String, modifier: Modifier = Modifier, showEye: Boolean = false) {
    var masked by remember { mutableStateOf(showEye) }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (masked) "\u20B9***" else value,
                style = TabularAmount,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (showEye) {
                IconButton(onClick = { masked = !masked }, modifier = Modifier.size(24.dp)) {
                    Icon(
                        if (masked) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                        contentDescription = if (masked) "Show income" else "Hide income",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryBadge(
    category: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 20.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(categoryColorFor(category)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            categoryIconFor(category),
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = Color.White
        )
    }
}

/**
 * The axio list row: coloured category badge, title + meta, right-aligned amount and a circular
 * trailing action.
 */
@Composable
fun ListRow(
    title: String,
    meta: String,
    amount: String,
    amountColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeCategory: String = title,
    dateText: String? = null,
    trailingIcon: ImageVector = Icons.Rounded.ArrowOutward,
    onTrailingClick: (() -> Unit)? = null,
    onLight: Boolean = false
) {
    val titleColor = if (onLight) OnCardLight else MaterialTheme.colorScheme.onBackground
    val metaColor = if (onLight) TextMutedDark else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryBadge(badgeCategory)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = titleColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(meta, style = MaterialTheme.typography.bodySmall, color = metaColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            dateText?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = metaColor, maxLines = 1)
                Spacer(Modifier.height(2.dp))
            }
            Text(
                amount,
                style = TabularAmount,
                color = amountColor,
                maxLines = 1
            )
        }
        Spacer(Modifier.width(12.dp))
        CircleIconButton(
            icon = trailingIcon,
            contentDescription = null,
            onClick = onTrailingClick ?: onClick,
            onLight = onLight
        )
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Ink800),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Inbox,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 280.dp)
        )
        if (action != null) {
            Spacer(Modifier.height(16.dp))
            action()
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Charts
// ---------------------------------------------------------------------------------------------

@Composable
fun DonutRing(
    progress: Float,
    amountText: String,
    percentText: String,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    strokeWidth: Dp = 18.dp,
    progressColor: Color = ChartIndigo
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "ring"
    )
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(
                color = RingTrackDark,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
            val sweep = animated * 360f
            if (sweep > 0f) {
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
                val radius = (this.size.minDimension - stroke) / 2f
                val angle = Math.toRadians((-90.0 + sweep.toDouble()))
                val tip = Offset(
                    x = this.size.width / 2f + (radius * cos(angle)).toFloat(),
                    y = this.size.height / 2f + (radius * sin(angle)).toFloat()
                )
                drawCircle(AxioLime.copy(alpha = 0.25f), radius = 9.dp.toPx(), center = tip)
                drawCircle(AxioLime, radius = 5.dp.toPx(), center = tip)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Ink800),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.ArrowOutward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(amountText, style = HeroNumber, color = MaterialTheme.colorScheme.onBackground, maxLines = 1)
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .clip(PillShape)
                    .background(AxioLime.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(percentText, style = MaterialTheme.typography.labelMedium, color = AxioLime)
            }
        }
    }
}

/** Rounded capsule bars with a lime overlay line - the axio trend chart. */
@Composable
fun TrendChart(
    points: List<Pair<String, Double>>,
    modifier: Modifier = Modifier,
    highlightLast: Boolean = true
) {
    if (points.isEmpty()) {
        EmptyState("No trend yet", "Add a few months of records to see the shape of your spending.")
        return
    }
    val max = (points.maxOfOrNull { it.second } ?: 1.0).coerceAtLeast(1.0)
    val highlight = if (highlightLast) points.lastIndex else -1
    val growth by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "bars"
    )
    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            val count = points.size
            val slot = size.width / count.coerceAtLeast(1)
            val barWidth = (slot * 0.45f).coerceAtMost(16.dp.toPx())
            val radius = barWidth / 2f
            val baseline = size.height
            points.forEachIndexed { index, point ->
                val ratio = (point.second / max).toFloat()
                val barHeight = (baseline * ratio * growth).coerceAtLeast(barWidth)
                val cx = slot * index + slot / 2f
                val color = if (index == highlight) ChartIndigoSelected else ChartIndigo
                drawRoundRect(
                    color = color,
                    topLeft = Offset(cx - barWidth / 2f, baseline - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
                )
            }
            if (count > 1) {
                val path = Path()
                points.forEachIndexed { index, point ->
                    val ratio = (point.second / max).toFloat()
                    val barHeight = (baseline * ratio * growth).coerceAtLeast(barWidth)
                    val x = slot * index + slot / 2f
                    val y = baseline - barHeight
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, color = AxioLime, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                points.forEachIndexed { index, point ->
                    val ratio = (point.second / max).toFloat()
                    val barHeight = (baseline * ratio * growth).coerceAtLeast(barWidth)
                    val x = slot * index + slot / 2f
                    val y = baseline - barHeight
                    drawCircle(AxioLime, radius = 3.dp.toPx(), center = Offset(x, y))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            points.forEach { point ->
                Text(
                    point.first.substringBefore(' ').take(3),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** Per-category mini ring used in the budget breakdown. */
@Composable
fun CategoryRing(
    percent: Float,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    strokeWidth: Dp = 5.dp,
    centerText: String? = null
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(
                color = Ink800,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = (percent.coerceIn(0f, 1f)) * 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        if (centerText != null) {
            Text(
                centerText,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
fun BudgetBar(spent: Double, limit: Double, currency: String, category: String, modifier: Modifier = Modifier) {
    val over = limit > 0 && spent > limit
    val ratio = if (limit <= 0) 0f else (spent / limit).toFloat()
    val money = LocalMoneyColors.current
    val ringColor = if (over) money.negative else categoryColorFor(category)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryRing(
            percent = ratio,
            color = ringColor,
            centerText = if (limit > 0) "${(ratio * 100).toInt()}%" else "--"
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(category.titleCase(), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(2.dp))
            Text(
                "of ${Money.format(limit, currency)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            Money.format(spent, currency),
            style = TabularAmount,
            color = if (over) money.negative else MaterialTheme.colorScheme.onBackground
        )
    }
}

/** Decorative gradient card used for bills / due items. */
@Composable
fun GradientCard(
    eyebrow: String,
    title: String,
    amount: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(FeatureMint, FeatureLilac)))
            .padding(20.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                eyebrow.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = EyebrowPurple
            )
            Spacer(Modifier.height(6.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = OnCardLight)
            Spacer(Modifier.height(4.dp))
            Text(amount, style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"), color = OnCardLight)
            if (action != null) {
                Spacer(Modifier.height(12.dp))
                action()
            }
        }
    }
}
