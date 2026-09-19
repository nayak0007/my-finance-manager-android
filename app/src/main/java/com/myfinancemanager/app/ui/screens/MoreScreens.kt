package com.myfinancemanager.app.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.myfinancemanager.app.data.importing.ImportUploadResult
import com.myfinancemanager.app.data.local.entity.AutoCaptureEntity
import com.myfinancemanager.app.data.local.entity.BudgetEntity
import com.myfinancemanager.app.data.local.entity.ExpenseCategory
import com.myfinancemanager.app.data.local.entity.ExpenseEntity
import com.myfinancemanager.app.data.local.entity.ImportBatchEntity
import com.myfinancemanager.app.data.local.entity.InsightEntity
import com.myfinancemanager.app.data.local.entity.ParsedType
import com.myfinancemanager.app.data.local.entity.SenderRuleEntity
import com.myfinancemanager.app.data.prefs.AppPreferences
import com.myfinancemanager.app.data.remote.ApiConfig
import com.myfinancemanager.app.data.remote.NeonAuthConfig
import com.myfinancemanager.app.data.remote.RemoteImportedTransaction
import com.myfinancemanager.app.sms.InboxScanner
import com.myfinancemanager.app.ui.AppViewModel
import com.myfinancemanager.app.ui.SyncSnapshot
import com.myfinancemanager.app.ui.components.BudgetBar
import com.myfinancemanager.app.ui.components.CategoryBadge
import com.myfinancemanager.app.ui.components.CircleIconButton
import com.myfinancemanager.app.ui.components.EmptyState
import com.myfinancemanager.app.ui.components.EnumDropdown
import com.myfinancemanager.app.ui.components.FilledTextField
import com.myfinancemanager.app.ui.components.GradientCard
import com.myfinancemanager.app.ui.components.MoneyField
import com.myfinancemanager.app.ui.components.PillButton
import com.myfinancemanager.app.ui.components.PillButtonVariant
import com.myfinancemanager.app.ui.components.SectionTitle
import com.myfinancemanager.app.ui.components.SettingsCard
import com.myfinancemanager.app.ui.components.SurfaceCard
import com.myfinancemanager.app.ui.theme.AxioLime
import com.myfinancemanager.app.ui.theme.Ink900
import com.myfinancemanager.app.ui.theme.LocalMoneyColors
import com.myfinancemanager.app.ui.theme.TextSecondaryDark
import com.myfinancemanager.app.util.Dates
import com.myfinancemanager.app.util.Money
import com.myfinancemanager.app.util.titleCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.YearMonth

// ---------------------------------------------------------------------------------------------
// Shared chrome
// ---------------------------------------------------------------------------------------------

@Composable
private fun ScreenHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            CircleIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack, filled = true)
            Spacer(Modifier.width(8.dp))
        }
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        actions()
    }
}

@Composable
private fun AxioSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Ink900,
            checkedTrackColor = AxioLime,
            uncheckedThumbColor = TextSecondaryDark,
            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    )
}

@Composable
private fun SettingRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing?.invoke()
    }
}

@Composable
private fun CardDivider() {
    HorizontalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
        modifier = Modifier.padding(start = 16.dp)
    )
}

// ---------------------------------------------------------------------------------------------
// Import
// ---------------------------------------------------------------------------------------------

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    viewModel: AppViewModel,
    batches: List<ImportBatchEntity>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // One import at a time. This is read from the batch list rather than kept in local state, so
    // the same import is still waiting when the user comes back to the screen, and a second
    // statement cannot be picked until this one is committed or cancelled.
    val active = batches.firstOrNull { it.status.lowercase() in ACTIVE_IMPORT_STATUSES }
    val parsing = active != null && active.status.lowercase() in PARSING_IMPORT_STATUSES
    val reviewBatch = active?.takeIf { it.status.equals("ready_for_review", ignoreCase = true) }

    var uploadInFlight by remember { mutableStateOf(false) }
    var committing by remember { mutableStateOf(false) }
    var cancelling by remember { mutableStateOf(false) }
    var statusLine by remember { mutableStateOf<String?>(null) }
    var rows by remember { mutableStateOf<List<RemoteImportedTransaction>>(emptyList()) }
    var rowsForBatch by remember { mutableStateOf<String?>(null) }
    val excluded = remember { mutableStateMapOf<String, Boolean>() } // staged row id -> excluded

    // Nothing else can be started while an import owns the screen.
    val locked = active != null || uploadInFlight || committing || cancelling

    fun startImport(block: suspend () -> ImportUploadResult) {
        statusLine = null
        uploadInFlight = true
        scope.launch {
            try {
                when (val result = block()) {
                    is ImportUploadResult.Failed -> statusLine = result.reason
                    is ImportUploadResult.Queued -> statusLine =
                        "Saved for upload — it will be parsed once your device is back online."
                    ImportUploadResult.Cancelled -> statusLine = "Import cancelled."
                    is ImportUploadResult.Ready -> Unit // the review follows from the batch state
                }
            } finally {
                uploadInFlight = false
            }
        }
    }

    fun cancelActive() {
        val batch = active ?: return
        statusLine = null
        cancelling = true
        scope.launch {
            val cancelled = viewModel.cancelImport(batch)
            cancelling = false
            if (!cancelled) {
                statusLine = "Could not cancel the import — check your connection and try again."
            }
        }
    }

    // The staged rows live on the server, so they are fetched once the parse reaches review —
    // including when the review is resumed rather than reached straight from an upload.
    LaunchedEffect(active?.id, active?.status) {
        val batch = active
        if (batch == null) {
            rowsForBatch = null
            rows = emptyList()
            excluded.clear()
        } else if (batch.status.equals("ready_for_review", ignoreCase = true) &&
            batch.remoteId != null && rowsForBatch != batch.id
        ) {
            rowsForBatch = batch.id
            excluded.clear()
            rows = runCatching { viewModel.stagedRows(batch).transactions }.getOrDefault(emptyList())
        }
    }

    // A parse that was still running when the screen was left has no coroutine watching it any
    // more, so watching resumes here: the spinner has to follow the server, not a stale local row.
    LaunchedEffect(active?.id, active?.status, uploadInFlight) {
        val batch = active ?: return@LaunchedEffect
        if (uploadInFlight || batch.remoteId == null ||
            batch.status.lowercase() !in PARSING_IMPORT_STATUSES
        ) {
            return@LaunchedEffect
        }
        while (true) {
            delay(RESUME_POLL_INTERVAL_MS)
            val refreshed = try {
                viewModel.refreshImport(batch)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            } ?: continue
            if (refreshed.status.lowercase() !in PARSING_IMPORT_STATUSES) break
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        // Resolve the display name ("statement.pdf") through the provider; the Uri's own path
        // is usually an opaque document id ("document:33") with no extension, and the backend
        // routes its parser by that extension.
        val name = queryDisplayName(context, uri) ?: uri.lastPathSegment?.substringAfterLast('/') ?: "statement.pdf"
        startImport { viewModel.uploadStatement(uri, name) }
    }
    Scaffold(
        containerColor = Ink900,
        topBar = { ScreenHeader("Smart Import", onBack) }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Upload a bank, card, or broker statement (PDF, CSV, or XLSX). It is parsed on the server; review every row before it is saved.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            PillButton(
                "Choose statement file",
                onClick = { picker.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth(),
                variant = PillButtonVariant.Lime,
                enabled = !locked
            )
            Spacer(Modifier.height(8.dp))
            PillButton(
                "Load sample statement",
                onClick = {
                    startImport {
                        val text = withContext(Dispatchers.IO) {
                            context.assets.open("sample_statement.csv").bufferedReader().readText()
                        }
                        viewModel.uploadStatementContent("sample_statement.csv", text)
                    }
                },
                enabled = !locked,
                modifier = Modifier.fillMaxWidth(),
                variant = PillButtonVariant.Outlined
            )

            // The spinner stays up for as long as the import is unfinished — there is no attempt
            // budget — and it can be ended from here at any point.
            if (parsing || uploadInFlight) {
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                    color = AxioLime,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (cancelling) "Cancelling the import…"
                    else "Uploading and parsing on the server. This can take a minute, and it keeps going until it finishes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                PillButton(
                    "Cancel import",
                    onClick = { cancelActive() },
                    modifier = Modifier.fillMaxWidth(),
                    variant = PillButtonVariant.Outlined,
                    enabled = !cancelling
                )
            }
            statusLine?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            // Review: the rows the server staged, with the user's include/exclude decisions.
            if (reviewBatch != null) {
                val batch = reviewBatch
                val includeCount = rows.count { excluded[it.id] != true }
                Spacer(Modifier.height(16.dp))
                Text(
                    "$includeCount of ${rows.size} rows will be imported. Uncheck duplicates or junk.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(8.dp))
                SettingsCard {
                    rows.forEachIndexed { index, row ->
                        val skip = excluded[row.id] == true
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = !skip,
                                onCheckedChange = { excluded[row.id] = !it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = AxioLime,
                                    uncheckedColor = TextSecondaryDark,
                                    checkmarkColor = Ink900
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${row.merchant ?: row.description ?: "Unknown"} · ${row.transactionType?.titleCase() ?: "Unknown type"}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    buildString {
                                        append(Money.format(row.amount))
                                        Dates.formatIso(row.transactionDate)?.let { append(" · ").append(it) }
                                        if (row.duplicate) append(" · duplicate")
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (index != rows.lastIndex) CardDivider()
                    }
                }
                Spacer(Modifier.height(16.dp))
                PillButton(
                    if (committing) "Importing…" else "Confirm import",
                    onClick = {
                        statusLine = null
                        committing = true
                        scope.launch {
                            val included = rows.filter { excluded[it.id] != true }.map { it.id }.toSet()
                            try {
                                viewModel.commitImport(batch, included)
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                statusLine = e.message ?: "Commit failed"
                            } finally {
                                committing = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    variant = PillButtonVariant.Lime,
                    enabled = includeCount > 0 && !committing && !cancelling
                )
                Spacer(Modifier.height(8.dp))
                PillButton(
                    "Cancel import",
                    onClick = { cancelActive() },
                    modifier = Modifier.fillMaxWidth(),
                    variant = PillButtonVariant.Outlined,
                    enabled = !committing && !cancelling
                )
            }

            SectionTitle("Recent imports")
            if (batches.isEmpty()) {
                Text("No imports yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            batches.take(8).forEach { batch ->
                val label = when {
                    batch.status.equals("failed", true) -> batch.errorMessage ?: "failed"
                    batch.status.equals("cancelled", true) -> "cancelled"
                    else -> "${batch.committed}/${batch.totalParsed}"
                }
                Text(
                    "${batch.sourceFile} · $label · ${Dates.format(batch.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * Batch statuses that mean "this import still owns the screen". Everything else — committed,
 * cancelled, failed — has finished, which is what frees the picker for the next statement.
 */
private val ACTIVE_IMPORT_STATUSES = setOf("queued", "processing", "ready_for_review")

/** The statuses during which the parse is still running and the spinner belongs on screen. */
private val PARSING_IMPORT_STATUSES = setOf("queued", "processing")

/** How often a resumed import screen re-checks a parse the server is still running. */
private const val RESUME_POLL_INTERVAL_MS = 2_000L

// ---------------------------------------------------------------------------------------------
// Review queue
// ---------------------------------------------------------------------------------------------

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    items: List<AutoCaptureEntity>,
    onBack: () -> Unit,
    onConfirm: (AutoCaptureEntity) -> Unit,
    onReject: (AutoCaptureEntity) -> Unit,
    onScanInbox: () -> Unit
) {
    val money = LocalMoneyColors.current
    Scaffold(
        containerColor = Ink900,
        topBar = {
            ScreenHeader("Review queue", onBack) {
                TextButton(onClick = onScanInbox) {
                    Text("Scan SMS", color = AxioLime, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    ) { padding ->
        if (items.isEmpty()) {
            EmptyState(
                "Queue is clear",
                "Auto-detected SMS and import drafts will show here for confirmation.",
                Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    SurfaceCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CategoryBadge(item.parsedCategory ?: item.parsedType.name, size = 40.dp, iconSize = 18.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        item.parsedParty ?: "Unknown",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        "${item.parsedType.name.titleCase()} · ${item.sender}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    item.parsedAmount?.let { Money.format(it) } ?: "-",
                                    style = com.myfinancemanager.app.ui.theme.TabularAmount,
                                    color = if (item.parsedType == ParsedType.INCOME) money.positive else MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                item.rawText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PillButton(
                                    "Confirm",
                                    onClick = { onConfirm(item) },
                                    modifier = Modifier.weight(1f),
                                    variant = PillButtonVariant.Lime,
                                    compact = true,
                                    icon = Icons.Rounded.Check
                                )
                                PillButton(
                                    "Reject",
                                    onClick = { onReject(item) },
                                    modifier = Modifier.weight(1f),
                                    variant = PillButtonVariant.Outlined,
                                    compact = true
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Insights
// ---------------------------------------------------------------------------------------------

@Composable
fun InsightsScreen(
    insights: List<InsightEntity>,
    onRefresh: () -> Unit,
    onDismiss: (InsightEntity) -> Unit,
    onSave: (InsightEntity) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Insights",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onRefresh) {
                Text("Refresh", color = AxioLime, style = MaterialTheme.typography.labelLarge)
            }
        }
        Text(
            "Suggestions are generated from your account and are informational only - not certified financial, tax, or investment advice.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        if (insights.isEmpty()) {
            EmptyState("No insights yet", "Add a few records, then refresh to generate observations.")
        } else {
            insights.forEach { item ->
                SurfaceCard(Modifier.fillMaxWidth().padding(bottom = 12.dp), shape = RoundedCornerShape(20.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(AxioLime.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp), tint = AxioLime)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    item.category.titleCase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AxioLime
                                )
                                item.title?.let {
                                    Text(it, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            item.insightText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PillButton(
                                if (item.saved) "Saved" else "Save",
                                onClick = { onSave(item) },
                                variant = if (item.saved) PillButtonVariant.Outlined else PillButtonVariant.Lime,
                                compact = true,
                                enabled = !item.saved,
                                icon = if (item.saved) Icons.Rounded.Check else null
                            )
                            PillButton("Dismiss", onClick = { onDismiss(item) }, variant = PillButtonVariant.Text, compact = true, icon = Icons.Rounded.Close)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/**
 * Resolves the picked document's real file name (e.g. "hdfc-statement.pdf") from the
 * ContentResolver. Returns null when the provider does not expose it, letting the caller
 * fall back to the Uri's path segment.
 */
private fun queryDisplayName(context: Context, uri: Uri): String? = runCatching {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) cursor.getString(index)?.takeIf { it.isNotBlank() } else null
    }
}.getOrNull()

// ---------------------------------------------------------------------------------------------
// Settings
// ---------------------------------------------------------------------------------------------

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    prefs: AppPreferences,
    email: String,
    sync: SyncSnapshot,
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onSenders: () -> Unit,
    onRequestSms: () -> Unit,
    onExport: () -> Unit
) {
    var display by remember { mutableStateOf(email.substringBefore("@")) }
    var deletePrompt by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = Ink900,
        topBar = { ScreenHeader("Settings", onBack) }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SectionTitle("Account")
            SettingsCard {
                SettingRow("Signed in as", subtitle = email)
                CardDivider()
                Column(Modifier.padding(16.dp)) {
                    FilledTextField(display, { display = it }, label = "Display name")
                    Spacer(Modifier.height(12.dp))
                    PillButton("Save profile", { viewModel.updateDisplayName(display) }, modifier = Modifier.fillMaxWidth(), variant = PillButtonVariant.Dark)
                    Spacer(Modifier.height(16.dp))
                    EnumDropdown("Currency", prefs.currencyCode, listOf("INR", "USD", "EUR", "GBP")) { viewModel.setCurrency(it) }
                }
            }

            SectionTitle("Capture")
            SettingsCard {
                SettingRow(
                    "SMS auto-capture",
                    subtitle = "Bank alerts are read on this device, then queued here for review."
                ) {
                    AxioSwitch(prefs.smsCaptureEnabled) { enabled ->
                        if (enabled) onRequestSms() else viewModel.setSmsEnabled(false)
                    }
                }
                CardDivider()
                SettingRow("Email auto-capture", subtitle = "Connect Gmail later via backend OAuth.") {
                    AxioSwitch(prefs.emailCaptureEnabled) { viewModel.setEmailEnabled(it) }
                }
                CardDivider()
                SettingRow("Sender allow / block lists", subtitle = "Choose which senders can create drafts.") {
                    TextButton(onClick = onSenders) { Text("Manage", color = AxioLime) }
                }
            }

            SectionTitle("Notifications")
            SettingsCard {
                SettingRow("Insight notifications", subtitle = "Get nudges when new insights are ready.") {
                    AxioSwitch(prefs.notificationsEnabled) { viewModel.setNotifications(it) }
                }
                CardDivider()
                Column(Modifier.padding(16.dp)) {
                    EnumDropdown("Insight frequency (days)", prefs.insightFrequencyDays, listOf(1, 3, 7, 14, 30)) {
                        viewModel.setInsightFrequency(it)
                    }
                }
            }

            SectionTitle("Sync")
            SettingsCard {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Income, expenses and investments are stored in your account, so they survive a reinstall and stay in step with any other device you sign in on. Confirming an alert asks the backend to record the transaction.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    sync.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                    }
                    Text(
                        sync.lastAt?.let { "Last synced ${Dates.formatDateTime(it)}" } ?: "Not synced yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Account sign-in: ${NeonAuthConfig.BASE_URL}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Data server: ${ApiConfig.BASE_URL}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    PillButton(
                        if (sync.running) "Syncing" else "Sync now",
                        onClick = { viewModel.syncNow() },
                        modifier = Modifier.fillMaxWidth(),
                        variant = PillButtonVariant.Outlined,
                        enabled = !sync.running,
                        loading = sync.running
                    )
                }
            }

            SectionTitle("Data")
            SettingsCard {
                SettingRow("Export", subtitle = "Download a CSV of every record in your account.") {
                    TextButton(onClick = onExport) { Text("Export", color = AxioLime) }
                }
                CardDivider()
                SettingRow("Log out", subtitle = "Sign out on this device.") {
                    TextButton(onClick = { viewModel.logout() }) { Text("Log out") }
                }
                CardDivider()
                SettingRow("Delete account", subtitle = "Removes every finance record the server holds.") {
                    TextButton(onClick = { deletePrompt = true }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (deletePrompt) {
        AlertDialog(
            onDismissRequest = { deletePrompt = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Delete account?") },
            text = {
                Text(
                    "This permanently deletes every record the server holds for this account, " +
                        "then clears this device. It cannot be undone.\n\n" +
                        "The sign-in itself (email and password) is held by Neon Auth, which does " +
                        "not expose account deletion to apps. Remove the user on the Auth page in " +
                        "the Neon Console if you want that gone as well."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAccount()
                        deletePrompt = false
                    }
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deletePrompt = false }) { Text("Cancel") }
            }
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Budgets
// ---------------------------------------------------------------------------------------------

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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
    val totalBudget = budgets.sumOf { it.monthlyLimit }
    val totalSpent = spent.values.sum()
    Scaffold(
        containerColor = Ink900,
        topBar = { ScreenHeader("Category budgets", onBack) }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (totalBudget > 0) {
                GradientCard(
                    eyebrow = "This month",
                    title = "Budget used",
                    amount = "${Money.format(totalSpent, currency)} of ${Money.format(totalBudget, currency)}"
                )
                Spacer(Modifier.height(16.dp))
            }
            SurfaceCard(Modifier.fillMaxWidth()) {
                Column {
                    EnumDropdown("Category", category, ExpenseCategory.entries.toList()) { category = it }
                    Spacer(Modifier.height(16.dp))
                    MoneyField(amount, "Monthly limit") { amount = it }
                    Spacer(Modifier.height(16.dp))
                    PillButton(
                        "Save budget",
                        onClick = { amount.toDoubleOrNull()?.let { onSave(category, it) } },
                        modifier = Modifier.fillMaxWidth(),
                        variant = PillButtonVariant.Lime,
                        enabled = amount.toDoubleOrNull() != null
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            if (budgets.isEmpty()) {
                EmptyState("No budgets", "Set a monthly cap per category to track your pace.")
            } else {
                budgets.forEach { b ->
                    BudgetBar(spent[b.category] ?: 0.0, b.monthlyLimit, currency, b.category.name)
                    Spacer(Modifier.height(10.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Senders
// ---------------------------------------------------------------------------------------------

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SenderScreen(
    rules: List<SenderRuleEntity>,
    onBack: () -> Unit,
    onSave: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit
) {
    var sender by remember { mutableStateOf("") }
    var allowed by remember { mutableStateOf(true) }
    Scaffold(
        containerColor = Ink900,
        topBar = { ScreenHeader("SMS senders", onBack) }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            SurfaceCard(Modifier.fillMaxWidth()) {
                Column {
                    FilledTextField(sender, { sender = it }, label = "Sender ID (e.g. VM-HDFCBK)")
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        com.myfinancemanager.app.ui.components.AxioFilterChip("Allow", allowed) { allowed = true }
                        com.myfinancemanager.app.ui.components.AxioFilterChip("Block", !allowed) { allowed = false }
                    }
                    Spacer(Modifier.height(16.dp))
                    PillButton(
                        "Save rule",
                        onClick = { if (sender.isNotBlank()) onSave(sender, allowed) },
                        modifier = Modifier.fillMaxWidth(),
                        variant = PillButtonVariant.Lime,
                        enabled = sender.isNotBlank()
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            SettingsCard {
                rules.forEachIndexed { index, rule ->
                    SettingRow(rule.sender, subtitle = if (rule.allowed) "Allowed" else "Blocked") {
                        TextButton(onClick = { onDelete(rule.id) }) {
                            Text("Remove", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    if (index != rules.lastIndex) CardDivider()
                }
            }
            Spacer(Modifier.height(24.dp))
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
