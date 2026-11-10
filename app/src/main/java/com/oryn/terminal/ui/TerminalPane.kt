package com.oryn.terminal.ui

import android.content.ClipData
import android.content.Intent
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.Typeface
import android.os.SystemClock
import android.widget.EditText
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import com.oryn.terminal.R
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.oryn.terminal.session.OrynTerminalSessionClient
import com.oryn.terminal.workspace.KlyroWorkspaceCatalog
import com.oryn.terminal.core.OrynPaths
import com.oryn.terminal.plugins.KlyroPluginManager
import java.io.File
import com.termux.terminal.TerminalSession
import com.termux.view.TerminalView
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** UI-only monotonic state; no command names, arguments or output are retained. */
private data class JobActivityUi(
    val sample: KlyroJobProbe.Sample,
    val detectedAtElapsedMs: Long
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TerminalPane(
    session: TerminalSession,
    darkChrome: Boolean,
    themeMode: String,
    accentId: String,
    onAccentIdChange: (String) -> Unit,
    onThemeModeChange: (String) -> Unit,
    title: String,
    tabs: List<Pair<Long, String>>,
    activeTabId: Long?,
    outputRevision: Long,
    workspaces: List<KlyroWorkspaceCatalog.Entry>,
    selectedWorkspaceId: String,
    tabWorkspaces: Map<Long, String>,
    tabsWithUnreadOutput: Set<Long>,
    jobSessions: Map<Long, TerminalSession>,
    lastOutputElapsedMs: Map<Long, Long>,
    onSelectWorkspace: (String) -> Unit,
    onCreateWorkspace: (String) -> Boolean,
    onRenameWorkspace: (String, String) -> Boolean,
    onDeleteWorkspace: (String) -> Unit,
    onAssignWorkspace: (String) -> Unit,
    diagnosticReport: String,
    onRecheck: () -> Unit,
    onSelectTab: (Long) -> Unit,
    onRenameTab: (Long, String) -> Unit,
    onCloseTab: (Long) -> Unit,
    onNewTab: () -> Unit,
    onReturnHome: () -> Unit,
    onImportFile: () -> Unit,
    onExportFile: () -> Unit,
    onSetupStorage: () -> Unit,
    onImportStorageFolder: () -> Unit,
    sessionClient: OrynTerminalSessionClient,
    viewClient: OrynTerminalViewClient,
    sessionFailureMessage: String?,
    nativePasteRequest: String?,
    onNativePasteConsumed: () -> Unit,
    onClose: () -> Unit,
    runtimeHasWarnings: Boolean = false,
    modifier: Modifier = Modifier
) {
    val ui = LocalKlyroColors.current
    // TerminalView stays dark for ANSI contrast; only surrounding chrome changes.
    val TerminalBackground = ui.background
    val TerminalSurface = ui.chrome
    val TerminalAccent = ui.accent
    val TerminalText = ui.foreground
    val TerminalMuted = ui.muted
    val inactiveTabSurface = ui.key
    var showToolsMenu by remember(session) { mutableStateOf(false) }
    var showWorkspaceSheet by remember(session) { mutableStateOf(false) }
    var showCommandPalette by remember(session) { mutableStateOf(false) }
    val currentProject = workspaces.firstOrNull { it.id == selectedWorkspaceId }?.name ?: "General"
    var showTerminalSettings by remember(session) { mutableStateOf(false) }
    var initialSettingsSection by remember(session) { mutableStateOf("home") }
    var sessionSheetId by remember(session) { mutableStateOf<Long?>(null) }
    var renameDialogId by remember(session) { mutableStateOf<Long?>(null) }
    var shellChooserSourceId by remember(session) { mutableStateOf<Long?>(null) }
    // A session can be closed while its sheet/rename/chooser is open.
    LaunchedEffect(tabs, sessionSheetId, renameDialogId, shellChooserSourceId) {
        val availableIds = tabs.map { it.first }.toSet()
        if (sessionSheetId != null && sessionSheetId !in availableIds) sessionSheetId = null
        if (renameDialogId != null && renameDialogId !in availableIds) renameDialogId = null
        if (shellChooserSourceId != null && shellChooserSourceId !in availableIds) {
            shellChooserSourceId = null
        }
    }
    var selectedOutput by remember(session) { mutableStateOf<String?>(null) }
    var selectedOutputTruncated by remember(session) { mutableStateOf(false) }
    var outputSelectionView by remember(session) { mutableStateOf<EditText?>(null) }
    val context = LocalContext.current
    val clipboardManager = remember(context) {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    }
    var clipboardAvailable by remember(session) {
        mutableStateOf(ClipboardTextReader.hasPlainText(clipboardManager))
    }
    val lifecycleOwner = context as? LifecycleOwner
    var tabJobActivity by remember { mutableStateOf<Map<Long, JobActivityUi>>(emptyMap()) }
    var activityElapsedMs by remember { mutableStateOf(SystemClock.elapsedRealtime()) }

    LaunchedEffect(jobSessions, lifecycleOwner) {
        while (true) {
            // No /proc polling while the app is backgrounded. Sampling uses the
            // IO dispatcher so terminal rendering and typing never block.
            val foreground = lifecycleOwner?.lifecycle?.currentState
                ?.isAtLeast(Lifecycle.State.RESUMED) != false
            if (foreground) {
                val measured = withContext(Dispatchers.IO) {
                    jobSessions.mapValues { (_, terminal) -> KlyroJobProbe.sample(terminal) }
                }
                val now = SystemClock.elapsedRealtime()
                val previous = tabJobActivity
                tabJobActivity = measured.mapValues { (tabId, sample) ->
                    val old = previous[tabId]
                    val sameJob = sample.hasForegroundJob() &&
                        old?.sample?.hasForegroundJob() == true &&
                        old.sample.shellPid == sample.shellPid &&
                        old.sample.foregroundGroup == sample.foregroundGroup
                    JobActivityUi(sample, if (sameJob) old!!.detectedAtElapsedMs else now)
                }
                activityElapsedMs = now
            }
            delay(if (foreground) 1_400L else 3_500L)
        }
    }
    fun recentOutput(id: Long): Boolean = KlyroJobProbe.recentOutput(
        lastOutputElapsedMs[id] ?: 0L, activityElapsedMs
    )
    fun activityLabel(id: Long): String {
        val uiState = tabJobActivity[id]
        val result = KlyroJobProbe.statusLabel(uiState?.sample, recentOutput(id))
        return if (uiState?.sample?.hasForegroundJob() == true) {
            "$result · " + KlyroJobProbe.elapsedLabel(
                uiState.detectedAtElapsedMs, activityElapsedMs
            )
        } else result
    }
    fun compactActivity(id: Long): String? = when {
        tabJobActivity[id]?.sample?.hasForegroundJob() == true -> "JOB"
        recentOutput(id) -> "OUT"
        else -> null
    }

    DisposableEffect(clipboardManager, session, lifecycleOwner) {
        val listener = ClipboardManager.OnPrimaryClipChangedListener {
            clipboardAvailable = ClipboardTextReader.hasPlainText(clipboardManager)
        }
        val observer = LifecycleEventObserver { _, event ->
            // Clipboard access is restricted in the background on modern
            // Android; refresh availability when the terminal regains focus.
            if (event == Lifecycle.Event.ON_RESUME) {
                clipboardAvailable = ClipboardTextReader.hasPlainText(clipboardManager)
            }
        }
        lifecycleOwner?.lifecycle?.addObserver(observer)
        runCatching { clipboardManager?.addPrimaryClipChangedListener(listener) }
        clipboardAvailable = ClipboardTextReader.hasPlainText(clipboardManager)
        onDispose {
            lifecycleOwner?.lifecycle?.removeObserver(observer)
            runCatching { clipboardManager?.removePrimaryClipChangedListener(listener) }
        }
    }
    var pendingMultilinePaste by remember(session) { mutableStateOf<String?>(null) }
    fun pasteClipboard() {
        val emulator = session.emulator
        if (!session.isRunning() || emulator == null) {
            Toast.makeText(context, "Terminal unavailable; start or reopen a session to paste",
                Toast.LENGTH_SHORT).show()
            return
        }
        val content = ClipboardTextReader.readPlainText(clipboardManager)
        if (!ClipboardPastePolicy.canPaste(content)) {
            Toast.makeText(context, "No supported clipboard text", Toast.LENGTH_SHORT).show()
        } else if (ClipboardPastePolicy.requiresConfirmation(content)) {
            pendingMultilinePaste = content
        } else {
            // Termux TerminalEmulator handles DECSET 2004 bracketed paste.
            emulator.paste(content!!)
        }
    }
    LaunchedEffect(nativePasteRequest, session) {
        nativePasteRequest?.let { incoming ->
            val emulator = session.emulator
            if (!session.isRunning() || emulator == null) {
                Toast.makeText(context, "Terminal unavailable; paste cancelled",
                    Toast.LENGTH_SHORT).show()
                onNativePasteConsumed()
                return@let
            }
            // Imported single-line text behaves like the clipboard action;
            // only lines capable of submitting shell commands need confirmation.
            if (!ClipboardPastePolicy.canPaste(incoming)) {
                Toast.makeText(context, "Unsupported or unsafe pasted text",
                    Toast.LENGTH_SHORT).show()
            } else if (ClipboardPastePolicy.requiresConfirmation(incoming)) {
                pendingMultilinePaste = incoming
            } else {
                emulator.paste(incoming)
            }
            onNativePasteConsumed()
        }
    }
    var activeTerminalView by remember(session) { mutableStateOf<TerminalView?>(null) }
    val fontPreferences = remember(context) {
        context.getSharedPreferences("klyro_terminal_preferences", Context.MODE_PRIVATE)
    }
    var terminalFontSp by remember {
        mutableStateOf(fontPreferences.getFloat("font_size_sp", 13.5f).coerceIn(9f, 24f))
    }
    var terminalFontId by remember {
        mutableStateOf(KlyroTerminalFonts.normalize(fontPreferences.getString("font_family", "system")))
    }
    val terminalTypeface = remember(context, terminalFontId) {
        KlyroTerminalFonts.load(context, terminalFontId)
    }
    val terminalTextSizePx = with(LocalDensity.current) { terminalFontSp.sp.roundToPx() }
    // Avoid resetting pinch-to-zoom during unrelated Compose recomposition.
    LaunchedEffect(terminalTextSizePx, activeTerminalView) {
        viewClient.setBaseTextSizePx(terminalTextSizePx)
        activeTerminalView?.setTextSize(terminalTextSizePx)
    }
    LaunchedEffect(terminalTypeface, activeTerminalView) {
        activeTerminalView?.setTypeface(terminalTypeface)
    }
    LaunchedEffect(ui.terminal, activeTerminalView) {
        activeTerminalView?.setBackgroundColor(ui.terminal.toArgb())
        activeTerminalView?.invalidate()
    }
    fun changeTerminalFont(delta: Float) {
        terminalFontSp = (terminalFontSp + delta).coerceIn(9f, 24f)
        fontPreferences.edit().putFloat("font_size_sp", terminalFontSp).apply()
    }
    var showTranscriptSearch by rememberSaveable(session) { mutableStateOf(false) }
    var transcriptQuery by rememberSaveable(session) { mutableStateOf("") }
    var findMatchCase by rememberSaveable(session) { mutableStateOf(false) }
    var searchIndex by rememberSaveable(session) { mutableStateOf(0) }
    var searchPage by rememberSaveable(session) { mutableStateOf(0) }
    var searchSnapshot by remember(session) {
        mutableStateOf<TranscriptSearch.Snapshot?>(null)
    }
    var searchingTranscript by remember(session) { mutableStateOf(false) }
    var pendingSearch by remember(session) { mutableStateOf<Int?>(null) }
    var pendingSearchOffset by remember(session) { mutableStateOf(0) }
    var pendingSearchPage by remember(session) { mutableStateOf(0) }
    var pendingSearchQuery by remember(session) { mutableStateOf("") }
    var pendingSearchCase by remember(session) { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val ime = WindowInsets.ime
    val density = LocalDensity.current
    // Query changes are debounced. The mutable PTY buffer is read on the UI
    // thread after the delay, and only the most recent 300 matches are kept.
    LaunchedEffect(
        showTranscriptSearch, transcriptQuery, findMatchCase, searchPage,
        outputRevision, session
    ) {
        if (!showTranscriptSearch || transcriptQuery.isBlank()) {
            searchSnapshot = null
            searchingTranscript = false
            return@LaunchedEffect
        }
        searchingTranscript = true
        delay(170)
        val emulator = session.emulator
        val snapshot = if (emulator == null) null else TranscriptSearch.scanPage(
            emulator.screen, emulator.mColumns, emulator.mRows,
            transcriptQuery, findMatchCase, searchPage
        )
        val previous = if (searchSnapshot?.pageFromNewest == searchPage) {
            searchSnapshot?.newest?.getOrNull(searchIndex)
        } else null
        searchIndex = when {
            snapshot == null -> 0
            searchIndex == Int.MAX_VALUE -> snapshot.newest.lastIndex.coerceAtLeast(0)
            previous != null -> TranscriptSearch.nearestIndex(
                snapshot.newest, previous.row, previous.logicalOffset
            )
            else -> 0
        }
        if (snapshot != null && snapshot.pageFromNewest != searchPage) {
            searchPage = snapshot.pageFromNewest
        }
        searchSnapshot = snapshot
        searchingTranscript = false
    }
    LaunchedEffect(pendingSearch, session, activeTerminalView) {
        val targetRow = pendingSearch ?: return@LaunchedEffect
        val targetOffset = pendingSearchOffset
        val requestedPage = pendingSearchPage
        val requestedQuery = pendingSearchQuery
        val requestedCase = pendingSearchCase
        val view = activeTerminalView ?: return@LaunchedEffect
        // Dialog dismissal and IME animation resize the PTY and reset TerminalView's top row.
        var stableFrames = 0
        var lastHeight = -1
        // Do not hang indefinitely if the keyboard remains open or the view keeps resizing.
        var framesWaited = 0
        while (stableFrames < 3 && framesWaited++ < 120 && view.isAttachedToWindow) {
            withFrameNanos { }
            stableFrames = if (ime.getBottom(density) == 0 && view.height == lastHeight) stableFrames + 1 else 0
            lastHeight = view.height
        }
        // A detached/closed PTY must never receive a deferred scroll or focus.
        if (view.isAttachedToWindow) {
            session.emulator?.let { emulator ->
                val result = TranscriptSearch.scanPage(
                    emulator.screen, emulator.mColumns, emulator.mRows,
                    requestedQuery, requestedCase, requestedPage
                )
                val selected = result.newest.getOrNull(
                    TranscriptSearch.nearestIndex(result.newest, targetRow, targetOffset)
                )
                if (selected != null) {
                    view.setTopRow(TranscriptSearch.topRow(
                        selected.row, emulator.mRows, emulator.screen.activeTranscriptRows
                    ))
                    view.invalidate()
                }
            }
        }
        // Focus restoration belongs to the modal-aware effect below. Never
        // steal focus from a new dialog opened while the IME was resizing.
        pendingSearch = null
    }
    fun openSessionSheet(id: Long, name: String) {
        if (tabs.any { it.first == id }) {
            sessionSheetId = id
        }
    }
    var controlActive by remember(viewClient) { mutableStateOf(viewClient.isControlDown()) }
    var altActive by remember(viewClient) { mutableStateOf(viewClient.isAltDown()) }
    var shiftActive by remember(viewClient) { mutableStateOf(viewClient.isShiftDown()) }
    var fnActive by remember(viewClient) { mutableStateOf(viewClient.isFnDown()) }
    var showEndSessionConfirmation by rememberSaveable(session) { mutableStateOf(false) }
    var warningNoteDismissed by rememberSaveable(session, runtimeHasWarnings) { mutableStateOf(false) }

    LaunchedEffect(
        activeTabId, activeTerminalView, sessionSheetId, renameDialogId,
        shellChooserSourceId, showWorkspaceSheet, showCommandPalette,
        showTerminalSettings, showTranscriptSearch, showEndSessionConfirmation,
        selectedOutput, pendingMultilinePaste
    ) {
        if (sessionSheetId == null && renameDialogId == null &&
            shellChooserSourceId == null && !showWorkspaceSheet &&
            !showCommandPalette && !showTerminalSettings &&
            !showTranscriptSearch && !showEndSessionConfirmation &&
            selectedOutput == null && pendingMultilinePaste == null
        ) {
            activeTerminalView?.let { view ->
                view.isFocusableInTouchMode = true
                view.requestFocus()
            }
        }
    }

    BackHandler(
        enabled = !showEndSessionConfirmation && !showToolsMenu &&
            !showTerminalSettings && !showTranscriptSearch &&
            !showWorkspaceSheet && !showCommandPalette &&
            pendingMultilinePaste == null && selectedOutput == null &&
            sessionSheetId == null && renameDialogId == null && shellChooserSourceId == null
    ) {
        showEndSessionConfirmation = true
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val compact = maxHeight < 300.dp
    fun selectTerminalOutput(expectedSelection: Boolean) {
        // Honor the action shown at touch-down, not a different action caused
        // by terminal output changing between composition and the tap.
        if (!expectedSelection) {
            pasteClipboard()
            return
        }
        val output = session.emulator?.screen?.transcriptText.orEmpty()
        if (TerminalActionPolicy.hasCopyableOutput(output)) {
            outputSelectionView = null
            selectedOutputTruncated = output.length > TerminalActionPolicy.MAX_SELECTION_UTF16_UNITS
            selectedOutput = TerminalActionPolicy.selectionTail(output)
        } else {
            Toast.makeText(context, "Terminal output changed; nothing to select",
                Toast.LENGTH_SHORT).show()
        }
    }
    // Never reduce key targets below Android's 48dp minimum: scroll narrow rows.
    val keyWidth = if (compact) 48.dp else ((maxWidth - 36.dp) / 7).coerceIn(48.dp, 56.dp)
    val canSendKeys = session.isRunning() && session.emulator != null
    LaunchedEffect(canSendKeys, viewClient, outputRevision) {
        // Clear sticky modifiers when a PTY exits while a key is held.
        if (!canSendKeys) {
            controlActive = false; altActive = false
            shiftActive = false; fnActive = false
            viewClient.setControlDown(false); viewClient.setAltDown(false)
            viewClient.setShiftDown(false); viewClient.setFnDown(false)
        }
    }
    val toolsMenu: @Composable () -> Unit = {
            Box {
                TextButton(
                    onClick = { showToolsMenu = true },
                    modifier = Modifier.size(KlyroUiMetrics.minTouch)
                ) {
                    Icon(painterResource(R.drawable.hugeicons_more_vertical),
                        contentDescription = "More actions", tint = TerminalMuted,
                        modifier = Modifier.size(KlyroUiMetrics.glyphMedium))
                }
                DropdownMenu(
                    expanded = showToolsMenu,
                    onDismissRequest = { showToolsMenu = false },
                    modifier = Modifier.width(KlyroUiMetrics.menuWidth),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(
                        KlyroUiMetrics.controlCorner
                    ),
                    containerColor = ui.sheet
                ) {
                    KlyroMenuItem("Command Palette", R.drawable.hugeicons_search_01) {
                        showToolsMenu = false
                        showCommandPalette = true
                    }
                    KlyroMenuItem("Workspaces", R.drawable.hugeicons_grid2x2) {
                        showToolsMenu = false
                        showWorkspaceSheet = true
                    }
                    KlyroMenuItem("Settings", R.drawable.hugeicons_settings_01) {
                        showToolsMenu = false
                        initialSettingsSection = "home"
                        showTerminalSettings = true
                    }
                    KlyroMenuItem("Home", R.drawable.hugeicons_home_03) {
                        showToolsMenu = false
                        onReturnHome()
                    }
                }
            }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBackground)
    ) {
        if (!compact) {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(TerminalSurface)
                    .heightIn(min = 52.dp)
                    .padding(horizontal = 10.dp)
            ) {
                Text(
                    "TERMINAL",
                    modifier = Modifier.align(Alignment.CenterStart),
                    color = TerminalMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
                Text(
                    currentProject,
                    modifier = Modifier.align(Alignment.Center).widthIn(max = 136.dp),
                    color = TerminalText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                    toolsMenu()
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth()
                .background(TerminalSurface)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (compact) toolsMenu()
            tabs.forEach { (id, tabTitle) ->
                val active = id == activeTabId
                val activityTag = compactActivity(id)
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .widthIn(min = KlyroUiMetrics.minTouch)
                        .combinedClickable(
                            onClickLabel = if (active) "Edit session" else "Switch session",
                            onLongClickLabel = "Edit session",
                            onClick = {
                                if (active) openSessionSheet(id, tabTitle) else onSelectTab(id)
                            },
                            onLongClick = { openSessionSheet(id, tabTitle) }
                        )
                        .semantics {
                            role = Role.Tab
                            selected = active
                            contentDescription = tabTitle.ifBlank { "Session" } +
                                ", project " + (workspaces.firstOrNull {
                                    it.id == (tabWorkspaces[id] ?: KlyroWorkspaceCatalog.GENERAL)
                                }?.name ?: "General")
                            stateDescription = buildString {
                                append(if (active) "Selected terminal" else "Open terminal")
                                append(", ")
                                append(activityLabel(id))
                                if (id in tabsWithUnreadOutput && !active) {
                                    append(", New terminal output")
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.height(36.dp).widthIn(max = 140.dp),
                        color = if (active) TerminalAccent else inactiveTabSurface,
                        contentColor = if (active) ui.onAccent else TerminalText,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(19.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (id in tabsWithUnreadOutput && !active) {
                                Box(Modifier.size(7.dp).background(
                                    TerminalAccent, androidx.compose.foundation.shape.CircleShape))
                            }
                            Text(
                                tabTitle.ifBlank { "Session" },
                                fontSize = 12.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            if (activityTag != null) {
                                Text(
                                    activityTag,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (active) ui.onAccent else if (activityTag == "JOB")
                                        ui.readyInk else ui.muted,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(
                        enabled = tabs.size < 8,
                        role = Role.Button,
                        onClickLabel = if (tabs.size < 8) "New terminal" else
                            "Maximum 8 terminal sessions",
                        onClick = onNewTab
                    )
                    .semantics {
                        contentDescription = if (tabs.size < 8) "New terminal" else
                            "Maximum 8 terminal sessions"
                    },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(38.dp),
                    // A translucent affordance, distinct from the active tab.
                    color = if (tabs.size < 8) TerminalAccent.copy(alpha = 0.14f) else inactiveTabSurface,
                    contentColor = if (tabs.size < 8) TerminalAccent else TerminalMuted,
                    shape = androidx.compose.foundation.shape.CircleShape,
                    border = if (tabs.size < 8) androidx.compose.foundation.BorderStroke(
                        1.dp, TerminalAccent.copy(alpha = 0.25f)
                    ) else null
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (tabs.size < 8) {
                            Icon(painterResource(R.drawable.hugeicons_plus_sign),
                                contentDescription = null,
                                modifier = Modifier.size(KlyroUiMetrics.glyphSmall))
                        } else {
                            Text("8/8", fontSize = 10.sp, color = TerminalMuted,
                                fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        if (sessionFailureMessage != null) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                color = ui.dangerContainer,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(KlyroUiMetrics.controlCorner)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        sessionFailureMessage,
                        color = ui.danger,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = onReturnHome,
                        modifier = Modifier.heightIn(min = KlyroUiMetrics.minTouch)
                    ) {
                        Text("Home", color = TerminalText, fontSize = 12.sp)
                    }
                }
            }
        }

        if (runtimeHasWarnings && sessionFailureMessage == null && !warningNoteDismissed) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                color = ui.warningContainer,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(KlyroUiMetrics.controlCorner)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Environment warnings detected. Review Diagnostics for details.",
                        color = ui.warningInk,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = { warningNoteDismissed = true },
                        modifier = Modifier.heightIn(min = KlyroUiMetrics.minTouch)
                    ) {
                        Text("Dismiss", color = TerminalText, fontSize = 12.sp)
                    }
                }
            }
        }

        Box(
            // A single consistently clipped surface replaces the previous
            // square AndroidView with misaligned decorative corner strips.
            modifier = Modifier.fillMaxWidth().weight(1f)
                .padding(horizontal = 8.dp, vertical = 2.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                .background(ui.terminal)
        ) {
        key(session) {
        AndroidView(
            // Inset real terminal glyphs inside the clipped rounded viewport.
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp),
            factory = { context ->
                TerminalView(context, null).apply {
                    // TerminalView (JitPack 0.118.0) is final. The upstream
                    // floating ActionMode's More calls showContextMenu().
                    // Register a real Android context-menu item; do not
                    // subclass the terminal or replace its selection engine.
                    setOnCreateContextMenuListener { menu, _, _ ->
                        menu.add("Select all").setOnMenuItemClickListener {
                            // Explicit native Select all must never activate clipboard Paste.
                            selectTerminalOutput(expectedSelection = true)
                            true
                        }
                    }
                    setTerminalViewClient(viewClient)
                    setTextSize(terminalTextSizePx)
                    setTypeface(terminalTypeface)
                    setBackgroundColor(ui.terminal.toArgb())
                    isFocusable = true
                    isFocusableInTouchMode = true
                    sessionClient.attachView(this)
                    viewClient.attachView(this)
                    attachSession(session)
                    activeTerminalView = this
                }
            },
            update = { view ->
                // Avoid redundant client attachments on every Compose recomposition.
                if (activeTerminalView !== view) {
                    activeTerminalView = view
                    sessionClient.attachView(view)
                    viewClient.attachView(view)
                }
                if (view.getCurrentSession() !== session) {
                    view.attachSession(session)
                }
            }
        )
        }
        }

        // Portrait: two fixed rows; compact height: one scrollable row to preserve PTY height.
        val firstRow: @Composable () -> Unit = {
            TerminalExtraKey("ESC", keyWidth = keyWidth, darkChrome = darkChrome,
                enabled = canSendKeys) { sendCodePointIfRunning(session, 27) }
            TerminalExtraKey("TAB", keyWidth = keyWidth, darkChrome = darkChrome,
                enabled = canSendKeys) { sendCodePointIfRunning(session, 9) }
            TerminalExtraKey("CTRL", active = controlActive, keyWidth = keyWidth, darkChrome = darkChrome,
                enabled = canSendKeys) {
                controlActive = !controlActive
                viewClient.setControlDown(controlActive)
            }
            TerminalExtraKey("ALT", active = altActive, keyWidth = keyWidth, darkChrome = darkChrome,
                enabled = canSendKeys) {
                altActive = !altActive
                viewClient.setAltDown(altActive)
            }
            TerminalExtraKey("SHIFT", active = shiftActive, keyWidth = keyWidth, darkChrome = darkChrome,
                enabled = canSendKeys) {
                shiftActive = !shiftActive
                viewClient.setShiftDown(shiftActive)
            }
            TerminalExtraKey("FN", active = fnActive, keyWidth = keyWidth, darkChrome = darkChrome,
                enabled = canSendKeys) {
                fnActive = !fnActive
                viewClient.setFnDown(fnActive)
            }
            val canSelect = remember(session, outputRevision) {
                TerminalActionPolicy.hasCopyableOutput(
                    session.emulator?.screen?.transcriptText.orEmpty()
                )
            }
            TerminalExtraKey(
                "ALL",
                description = if (canSelect) "Select all terminal output" else "Paste clipboard",
                glyph = if (canSelect) R.drawable.hugeicons_clipboard_list
                    else R.drawable.hugeicons_clipboard_paste,
                keyWidth = keyWidth, darkChrome = darkChrome,
                iconCaption = if (canSelect) "SELECT" else "PASTE",
                tone = if (canSelect) KlyroShortcutTone.SELECT else KlyroShortcutTone.PASTE,
                active = selectedOutput != null,
                // Allow copying a finished session's transcript; only pasting
                // requires a live PTY.
                enabled = canSelect || (session.isRunning() && session.emulator != null && clipboardAvailable)
            ) { selectTerminalOutput(canSelect) }
        }
        val secondRow: @Composable () -> Unit = {
            TerminalExtraKey("←", description = "Left arrow", arrow = "left", keyWidth = keyWidth, darkChrome = darkChrome, enabled = canSendKeys,
                longClickDescription = "Go to start of line",
                onLongClick = { write(session, TerminalNavigationPolicy.horizontalLongPress(false,
                    session.emulator?.isAlternateBufferActive == true, controlActive, altActive, shiftActive)) }
            ) { write(session, TerminalNavigationPolicy.arrow('D', controlActive, altActive, shiftActive)) }
            TerminalExtraKey("↑", description = "Up arrow", arrow = "up", keyWidth = keyWidth, darkChrome = darkChrome, enabled = canSendKeys) { write(session, TerminalNavigationPolicy.arrow('A', controlActive, altActive, shiftActive)) }
            TerminalExtraKey("↓", description = "Down arrow", arrow = "down", keyWidth = keyWidth, darkChrome = darkChrome, enabled = canSendKeys) { write(session, TerminalNavigationPolicy.arrow('B', controlActive, altActive, shiftActive)) }
            TerminalExtraKey("→", description = "Right arrow", arrow = "right", keyWidth = keyWidth, darkChrome = darkChrome, enabled = canSendKeys,
                longClickDescription = "Go to end of line",
                onLongClick = { write(session, TerminalNavigationPolicy.horizontalLongPress(true,
                    session.emulator?.isAlternateBufferActive == true, controlActive, altActive, shiftActive)) }
            ) { write(session, TerminalNavigationPolicy.arrow('C', controlActive, altActive, shiftActive)) }
            TerminalExtraKey("END", description = "Go to end of line",
                keyWidth = keyWidth, darkChrome = darkChrome, enabled = canSendKeys) {
                write(session, TerminalNavigationPolicy.horizontalLongPress(true,
                    session.emulator?.isAlternateBufferActive == true,
                    controlActive, altActive, shiftActive))
            }
            TerminalExtraKey("FIND", description = "Find in terminal output", keyWidth = keyWidth, darkChrome = darkChrome) {
                // Keep the previous query for successive searches in this session.
                searchIndex = 0
                showTranscriptSearch = true
            }
            TerminalExtraKey("CLEAR", description = "Clear terminal display", glyph = R.drawable.hugeicons_eraser,
                keyWidth = keyWidth, darkChrome = darkChrome, enabled = session.emulator != null) {
                // Never erase a partially typed Readline command or active program output.
                if (TerminalDisplayActions.clear(session)) {
                    activeTerminalView?.setTopRow(0)
                    activeTerminalView?.invalidate()
                } else {
                    Toast.makeText(context,
                        "Clear is available only at an idle Klyro prompt; input preserved",
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
        if (compact) {
            Row(
                modifier = Modifier.fillMaxWidth().background(TerminalSurface)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                firstRow()
                secondRow()
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().background(TerminalSurface)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) { firstRow() }
                Row(modifier = Modifier.fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) { secondRow() }
            }
        }
    }

    }

    // The native More item opens this in-app Select all action.
    // Selection is performed on a native read-only text surface, without
    // accidentally sending selected content to the running Bash session.
    selectedOutput?.let { output ->
        val truncated = selectedOutputTruncated
        val visibleOutput = output
        fun closeSelection() {
            outputSelectionView = null
            selectedOutput = null
            selectedOutputTruncated = false
        }
        KlyroModalTheme {
        AlertDialog(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(KlyroUiMetrics.modalCorner),
            containerColor = ui.sheet,
            onDismissRequest = { closeSelection() },
            title = { Text("Select terminal output") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (truncated) {
                        Text(
                            "Only the latest 2 million UTF-16 characters are selectable; older output is omitted for memory safety.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (visibleOutput.isEmpty()) {
                        Text("No output available to select.")
                    } else {
                        AndroidView(
                            modifier = Modifier.fillMaxWidth().height(244.dp),
                            factory = { activityContext ->
                                EditText(activityContext).apply {
                                    setText(visibleOutput)
                                    setTextIsSelectable(true)
                                    // Select and copy, but never edit the transcript preview.
                                    keyListener = null
                                    setShowSoftInputOnFocus(false)
                                    setHorizontallyScrolling(false)
                                    setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f)
                                    // Selection preview shares the current terminal ink,
                                    // including light mode; no isolated black subpanel.
                                    setTextColor(ui.terminalInk.toArgb())
                                    setBackgroundColor(ui.terminal.toArgb())
                                    typeface = android.graphics.Typeface.MONOSPACE
                                    setPadding(12, 8, 12, 8)
                                    outputSelectionView = this
                                    post {
                                        if (isAttachedToWindow) {
                                            requestFocus()
                                            selectAll()
                                        }
                                    }
                                }
                            },
                            update = { view ->
                                // React to theme changes without replacing selection or transcript.
                                view.setTextColor(ui.terminalInk.toArgb())
                                view.setBackgroundColor(ui.terminal.toArgb())
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = visibleOutput.isNotEmpty(),
                    onClick = {
                        val view = outputSelectionView
                        if (view != null) {
                            val value = view.text.toString()
                            val start = view.selectionStart.coerceIn(0, value.length)
                            val end = view.selectionEnd.coerceIn(0, value.length)
                            val left = minOf(start, end)
                            val right = maxOf(start, end)
                            val hasSelection = left < right
                            val selection = if (hasSelection) value.substring(left, right) else value
                            val copied = runCatching {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                                    as? ClipboardManager ?: error("Clipboard unavailable")
                                clipboard.setPrimaryClip(ClipData.newPlainText("Klyro terminal", selection))
                            }.isSuccess
                            Toast.makeText(
                                context,
                                if (copied) {
                                    if (hasSelection) "Selection copied" else "All visible output copied"
                                } else "Clipboard unavailable",
                                Toast.LENGTH_SHORT
                            ).show()
                            if (copied) closeSelection()
                        }
                    }
                ) { Text("Copy selection") }
            },
            dismissButton = {
                TextButton(onClick = { closeSelection() }) { Text("Close") }
            }
        )
        }
    }

    if (showTerminalSettings) {
        KlyroSettingsSheet(
            initialSection = initialSettingsSection,
            themeMode = themeMode,
            onThemeModeChange = onThemeModeChange,
            accentId = accentId,
            onAccentIdChange = onAccentIdChange,
            fontSizeSp = terminalFontSp,
            fontFamilyId = terminalFontId,
            onFontFamilyChange = { id ->
                terminalFontId = KlyroTerminalFonts.normalize(id)
                fontPreferences.edit().putString("font_family", terminalFontId).apply()
            },
            onFontChange = ::changeTerminalFont,
            onResetFont = {
                terminalFontSp = 13.5f
                fontPreferences.edit().remove("font_size_sp").apply()
            },
            onResetModifiers = {
                controlActive = false
                altActive = false
                shiftActive = false
                fnActive = false
                viewClient.setControlDown(false)
                viewClient.setAltDown(false)
                viewClient.setShiftDown(false)
                viewClient.setFnDown(false)
            },
            activeModifiers = buildSet {
                if (controlActive) add("Ctrl")
                if (altActive) add("Alt")
                if (shiftActive) add("Shift")
                if (fnActive) add("Fn")
            },
            onSetupStorage = onSetupStorage,
            onImportStorageFolder = onImportStorageFolder,
            onImportFile = onImportFile,
            onExportFile = onExportFile,
            diagnosticReport = diagnosticReport,
            onRecheckEnvironment = onRecheck,
            onCopyDiagnostic = {
                val copied = runCatching {
                    val clipboard = clipboardManager ?: error("Clipboard unavailable")
                    clipboard.setPrimaryClip(
                        ClipData.newPlainText("Klyro health snapshot", diagnosticReport)
                    )
                }.isSuccess
                Toast.makeText(context,
                    if (copied) "Report copied" else "Clipboard unavailable",
                    Toast.LENGTH_SHORT).show()
            },
            onShareDiagnostic = {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, diagnosticReport)
                }
                val shared = runCatching {
                    context.startActivity(Intent.createChooser(send, "Share Klyro report"))
                }.isSuccess
                if (!shared) {
                    Toast.makeText(context, "No sharing app available", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showTerminalSettings = false }
        )
    }

    if (pendingMultilinePaste != null) {
        KlyroModalTheme {
        AlertDialog(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(KlyroUiMetrics.modalCorner),
            containerColor = ui.sheet,
            onDismissRequest = { pendingMultilinePaste = null },
            title = { Text("Paste multiple lines?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Pasting multiple lines can execute commands. Review the clipboard content before continuing.")
                    Text(
                        pendingMultilinePaste.orEmpty(),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)
                            .verticalScroll(rememberScrollState()),
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text("Entire paste shown; scroll to review all lines before confirming.",
                        style = MaterialTheme.typography.labelSmall)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val text = pendingMultilinePaste
                    pendingMultilinePaste = null
                    val emulator = session.emulator
                    if (!session.isRunning() || emulator == null) {
                        Toast.makeText(context, "Terminal unavailable; paste cancelled",
                            Toast.LENGTH_SHORT).show()
                    } else if (ClipboardPastePolicy.canPaste(text)) {
                        emulator.paste(text!!)
                    } else {
                        Toast.makeText(context, "Clipboard text rejected", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Paste") }
            },
            dismissButton = {
                TextButton(onClick = { pendingMultilinePaste = null }) { Text("Cancel") }
            }
        )
        }
    }

    sessionSheetId?.let { id ->
        val name = tabs.firstOrNull { it.first == id }?.second
        if (name != null) {
            KlyroSessionSheet(
                sessionName = name,
                isActive = id == activeTabId,
                activityStatus = activityLabel(id),
                activityDetail = when {
                    tabJobActivity[id]?.sample?.hasForegroundJob() == true ->
                        "A different process group owns the foreground PTY. Elapsed time is approximate."
                    tabJobActivity[id]?.sample?.state == KlyroJobProbe.State.UNAVAILABLE ->
                        "Android did not provide a readable foreground process group."
                    else -> "Recent terminal output does not necessarily mean a job is running."
                },
                onDismiss = { sessionSheetId = null },
                onRename = {
                    sessionSheetId = null
                    renameDialogId = id
                },
                onChooseShell = {
                    sessionSheetId = null
                    shellChooserSourceId = id
                },
                canCreateSession = tabs.size < 8,
                onActivate = {
                    sessionSheetId = null
                    onSelectTab(id)
                },
                onCloseSession = {
                    sessionSheetId = null
                    onCloseTab(id)
                }
            )
        }
    }
    renameDialogId?.let { id ->
        val name = tabs.firstOrNull { it.first == id }?.second
        if (name != null) {
            KlyroRenameSessionDialog(
                name = name,
                onSave = { newName ->
                    onRenameTab(id, newName)
                    renameDialogId = null
                },
                onDismiss = { renameDialogId = null }
            )
        }
    }
    shellChooserSourceId?.let { sourceId ->
        KlyroChooseShellSheet(
            onBack = {
                shellChooserSourceId = null
                sessionSheetId = sourceId
            },
            onNewBash = {
                shellChooserSourceId = null
                onNewTab()
            },
            canStartBash = tabs.size < 8
        )
    }

    if (showTranscriptSearch) {
        val emulator = session.emulator
        val snapshot = searchSnapshot
        val matches = if (searchingTranscript) emptyList<TranscriptSearch.Occurrence>()
            else snapshot?.newest.orEmpty()
        val position = if (matches.isEmpty()) 0 else searchIndex.coerceIn(0, matches.lastIndex)
        val selectedMatch = matches.getOrNull(position)
        fun goToMatch(index: Int) {
            val match = matches.getOrNull(index) ?: return
            val currentEmulator = session.emulator ?: return
            activeTerminalView?.takeIf { it.isAttachedToWindow }?.let { view ->
                view.setTopRow(TranscriptSearch.topRow(
                    match.row, currentEmulator.mRows, currentEmulator.screen.activeTranscriptRows
                ))
                view.invalidate()
            }
        }
        KlyroModalTheme {
        AlertDialog(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(KlyroUiMetrics.modalCorner),
            containerColor = ui.sheet,
            onDismissRequest = { showTranscriptSearch = false },
            title = { Text("Find in terminal", style = MaterialTheme.typography.titleLarge, maxLines = 1) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = transcriptQuery,
                        onValueChange = {
                            transcriptQuery = it.take(128)
                            searchIndex = 0
                            searchPage = 0
                            searchSnapshot = null
                        },
                        label = { Text("Find text") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            if (transcriptQuery.isNotEmpty()) {
                                TextButton(onClick = {
                                    transcriptQuery = ""
                                    searchIndex = 0
                                    searchPage = 0
                                    searchSnapshot = null
                                }) {
                                    Text("Clear")
                                }
                            }
                        }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .heightIn(min = KlyroUiMetrics.minTouch)
                            .toggleable(value = findMatchCase, role = Role.Checkbox) {
                                findMatchCase = it
                                searchIndex = 0
                                searchPage = 0
                                searchSnapshot = null
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = findMatchCase, onCheckedChange = null)
                        Text("Match case", fontSize = 13.sp, color = ui.foreground)
                    }
                    Text(
                        when {
                            transcriptQuery.isBlank() -> "Enter text to search"
                            searchingTranscript -> "Searching terminal output…"
                            emulator == null -> "Terminal output unavailable"
                            snapshot == null || snapshot.totalCount == 0 -> "No matches"
                            else -> "Match ${snapshot.firstOrdinal + position + 1} of ${snapshot.totalCount}"
                        },
                        color = ui.foreground,
                        fontSize = 12.sp,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                    if (snapshot?.limited == true && !searchingTranscript) {
                        Text(
                            "Showing ${snapshot.firstOrdinal + 1}–${snapshot.firstOrdinal + matches.size} " +
                                "of ${snapshot.totalCount} · Page ${snapshot.pageFromNewest + 1}/${snapshot.pageCount} from newest",
                            color = ui.muted, style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (selectedMatch != null) {
                        val preview = buildAnnotatedString {
                            val context = selectedMatch.preview
                            if (selectedMatch.logicalOffset > selectedMatch.highlightStart) append("…")
                            append(context.substring(0, selectedMatch.highlightStart))
                            withStyle(SpanStyle(
                                color = ui.onAccent,
                                background = ui.accent,
                                fontWeight = FontWeight.Bold
                            )) {
                                append(context.substring(
                                    selectedMatch.highlightStart, selectedMatch.highlightEnd))
                            }
                            append(context.substring(selectedMatch.highlightEnd))
                        }
                        Surface(
                            color = ui.tile,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(KlyroUiMetrics.controlCorner)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    if (selectedMatch.row < 0) "History row ${-selectedMatch.row}"
                                    else "Visible screen row ${selectedMatch.row + 1}",
                                    color = ui.muted, style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    text = preview,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    maxLines = 3,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                TextButton(
                                    onClick = {
                                        val copied = runCatching {
                                            clipboardManager?.setPrimaryClip(
                                                ClipData.newPlainText("Klyro search excerpt", selectedMatch.preview))
                                            clipboardManager != null
                                        }.getOrDefault(false)
                                        Toast.makeText(context,
                                            if (copied) "Search excerpt copied" else "Clipboard unavailable",
                                            Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.heightIn(min = KlyroUiMetrics.minTouch)
                                ) { Text("Copy excerpt") }
                            }
                        }
                    }
                    if (matches.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TextButton(
                                modifier = Modifier.heightIn(min = KlyroUiMetrics.minTouch),
                                onClick = {
                                    searchIndex = TranscriptSearch.nextIndex(position, -1, matches.size)
                                    goToMatch(searchIndex)
                                }
                            ) { Text("Previous") }
                            TextButton(
                                modifier = Modifier.heightIn(min = KlyroUiMetrics.minTouch),
                                onClick = {
                                    searchIndex = TranscriptSearch.nextIndex(position, 1, matches.size)
                                    goToMatch(searchIndex)
                                }
                            ) { Text("Next") }
                            TextButton(
                                modifier = Modifier.heightIn(min = KlyroUiMetrics.minTouch),
                                onClick = {
                                    if (searchPage > 0) {
                                        searchPage = 0
                                        searchIndex = Int.MAX_VALUE
                                        searchSnapshot = null
                                    } else {
                                        searchIndex = matches.lastIndex
                                        goToMatch(searchIndex)
                                    }
                                }
                            ) { Text("Latest") }
                        }
                    }
                    if (snapshot != null && snapshot.pageCount > 1 &&
                        !searchingTranscript) {
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(
                                enabled = searchPage < snapshot.pageCount - 1,
                                modifier = Modifier.heightIn(min = KlyroUiMetrics.minTouch),
                                onClick = {
                                    searchPage += 1
                                    searchIndex = 0
                                    searchSnapshot = null
                                }
                            ) { Text("Older matches") }
                            TextButton(
                                enabled = searchPage > 0,
                                modifier = Modifier.heightIn(min = KlyroUiMetrics.minTouch),
                                onClick = {
                                    searchPage -= 1
                                    searchIndex = 0
                                    searchSnapshot = null
                                }
                            ) { Text("Newer matches") }
                        }
                    }
                    if (transcriptQuery.isNotBlank() && !searchingTranscript &&
                        snapshot?.totalCount == 0) {
                        TextButton(onClick = {
                            transcriptQuery = ""; searchIndex = 0
                            searchPage = 0; searchSnapshot = null
                        }) { Text("Clear search") }
                    }
                    if (transcriptQuery.length >= 128) {
                        Text("Search text limit reached (128 characters)",
                            color = ui.muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val chosen = selectedMatch ?: return@Button
                        // Snapshot the selected physical match before IME animation.
                        pendingSearchQuery = transcriptQuery
                        pendingSearchCase = findMatchCase
                        pendingSearchOffset = chosen.logicalOffset
                        pendingSearchPage = searchPage
                        pendingSearch = chosen.row
                        keyboard?.hide()
                        showTranscriptSearch = false
                    },
                    enabled = selectedMatch != null && !searchingTranscript,
                    modifier = Modifier.heightIn(min = KlyroUiMetrics.minTouch),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(KlyroUiMetrics.controlCorner)
                ) { Text("Go to line") }
            },
            dismissButton = {
                TextButton(onClick = { showTranscriptSearch = false }) { Text("Cancel") }
            }
        )
        }
    }

    if (showWorkspaceSheet) {
        KlyroWorkspaceSheet(
            entries = workspaces,
            selectedId = selectedWorkspaceId,
            tabs = tabs,
            tabWorkspaces = tabWorkspaces,
            activeTabId = activeTabId,
            onSelectProject = onSelectWorkspace,
            onCreateProject = onCreateWorkspace,
            onRenameProject = onRenameWorkspace,
            onDeleteProject = onDeleteWorkspace,
            onAssignCurrentTab = onAssignWorkspace,
            onSwitchTab = onSelectTab,
            onDismiss = { showWorkspaceSheet = false }
        )
    }

    if (showCommandPalette) {
        val privatePrefix = remember(context) {
            runCatching {
                OrynPaths(context.filesDir,
                    File(context.applicationInfo.nativeLibraryDir)).prefix
            }.getOrNull()
        }
        KlyroCommandPalette(
            installedPrefix = privatePrefix,
            pluginCommands = remember(context) { KlyroPluginManager(context).activeCommands() },
            onInsert = { snippet ->
                // A curated command is pasted, never submitted with Enter.
                // Full-screen applications retain their own keyboard focus.
                val emulator = session.emulator
                if (session.isRunning() && emulator != null &&
                    !emulator.isAlternateBufferActive) {
                    emulator.paste(snippet)
                    true
                } else {
                    Toast.makeText(context,
                        "Return to the Bash prompt before inserting a command.",
                        Toast.LENGTH_SHORT).show()
                    false
                }
            },
            onDismiss = { showCommandPalette = false }
        )
    }

    if (showEndSessionConfirmation) {
        KlyroModalTheme {
        AlertDialog(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(KlyroUiMetrics.modalCorner),
            containerColor = ui.sheet,
            onDismissRequest = { showEndSessionConfirmation = false },
            title = { Text("End Bash session?") },
            text = { Text("This will stop the running shell and its child processes.") },
            confirmButton = {
                TextButton(onClick = {
                    showEndSessionConfirmation = false
                    onClose()
                }) { Text("End session", color = ui.danger, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showEndSessionConfirmation = false }) {
                    Text("Keep session")
                }
            }
        )
        }
    }
}

internal enum class ShortcutState { ACTIVE, HOLD, INACTIVE, DEFAULT }

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TerminalExtraKey(
    label: String,
    active: Boolean = false,
    enabled: Boolean = true,
    description: String? = null,
    labelSize: androidx.compose.ui.unit.TextUnit = 12.sp,
    glyph: Int? = null,
    arrow: String? = null,
    keyWidth: androidx.compose.ui.unit.Dp = 52.dp,
    darkChrome: Boolean = true,
    longClickDescription: String? = null,
    iconCaption: String? = null,
    tone: KlyroShortcutTone = KlyroShortcutTone.NEUTRAL,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val colors = LocalKlyroColors.current
    val tooltipOffset = with(LocalDensity.current) { (-60).dp.roundToPx() }
    val interaction = remember { MutableInteractionSource() }
    val held by interaction.collectIsPressedAsState()
    val keyState = when {
        !enabled -> ShortcutState.INACTIVE
        held -> ShortcutState.HOLD
        active -> ShortcutState.ACTIVE
        else -> ShortcutState.DEFAULT
    }
    val spokenState = when (keyState) {
        ShortcutState.ACTIVE -> "Active"
        ShortcutState.HOLD -> "Pressed"
        ShortcutState.INACTIVE -> "Unavailable"
        ShortcutState.DEFAULT -> when (tone) {
            KlyroShortcutTone.SELECT -> "Selection available"
            KlyroShortcutTone.PASTE -> "Clipboard available"
            KlyroShortcutTone.NEUTRAL -> "Ready"
        }
    }
    // Neutral shortcuts by default. SELECT is a subtle accent, PASTE is a
    // neutral tile with its own outline: neither is a warning state.
    val visuals = KlyroShortcutTonePolicy.resolve(colors, tone, keyState)
    val stateInk = visuals.ink
    val stateBackground = visuals.background
    val stateBorder = if (tone == KlyroShortcutTone.PASTE)
        BorderStroke(1.dp, colors.muted.copy(alpha = .24f)) else null
    val keyContents: @Composable () -> Unit = {
        if (glyph != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Icon(
                    painter = painterResource(glyph),
                    contentDescription = null,
                    modifier = Modifier.size(KlyroUiMetrics.glyphSmall),
                    tint = stateInk
                )
                if (iconCaption != null) {
                    Text(iconCaption, fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1, color = stateInk)
                }
            }
        } else if (arrow != null) {
            // Thin chevrons, inspired by the reference terminal keyboard.
            Canvas(modifier = Modifier.width(22.dp).height(22.dp)) {
                val w = size.width
                val h = size.height
                val tip = when (arrow) {
                    "left" -> androidx.compose.ui.geometry.Offset(w * .32f, h * .50f)
                    "right" -> androidx.compose.ui.geometry.Offset(w * .68f, h * .50f)
                    "up" -> androidx.compose.ui.geometry.Offset(w * .50f, h * .32f)
                    else -> androidx.compose.ui.geometry.Offset(w * .50f, h * .68f)
                }
                val ends = when (arrow) {
                    "left" -> listOf(
                        androidx.compose.ui.geometry.Offset(w * .63f, h * .20f),
                        androidx.compose.ui.geometry.Offset(w * .63f, h * .80f)
                    )
                    "right" -> listOf(
                        androidx.compose.ui.geometry.Offset(w * .37f, h * .20f),
                        androidx.compose.ui.geometry.Offset(w * .37f, h * .80f)
                    )
                    "up" -> listOf(
                        androidx.compose.ui.geometry.Offset(w * .20f, h * .63f),
                        androidx.compose.ui.geometry.Offset(w * .80f, h * .63f)
                    )
                    else -> listOf(
                        androidx.compose.ui.geometry.Offset(w * .20f, h * .37f),
                        androidx.compose.ui.geometry.Offset(w * .80f, h * .37f)
                    )
                }
                ends.forEach { end ->
                    drawLine(
                        color = stateInk, start = end, end = tip,
                        strokeWidth = 1.75.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }
        } else {
            Text(label, fontSize = labelSize, fontFamily = FontFamily.Monospace)
        }
    }
    Box(contentAlignment = Alignment.Center) {
    if (onLongClick == null) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .height(50.dp)
                .width(keyWidth)
                .semantics {
                    contentDescription = description ?: label
                    stateDescription = spokenState
                },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(KlyroUiMetrics.controlCorner),
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
            interactionSource = interaction,
            border = stateBorder,
            colors = ButtonDefaults.buttonColors(
                containerColor = stateBackground,
                contentColor = stateInk,
                disabledContainerColor = stateBackground,
                disabledContentColor = stateInk
            )
        ) {
            keyContents()
        }
    } else {
        // Own the full gesture for long-press keys: Button's internal click handler
        // would otherwise double-send a short arrow after the long press.
        val shape = androidx.compose.foundation.shape.RoundedCornerShape(KlyroUiMetrics.controlCorner)
        Surface(
            modifier = Modifier.height(50.dp).width(keyWidth)
                .clip(shape)
                .combinedClickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClickLabel = description ?: label,
                    onLongClickLabel = longClickDescription ?: "Long press",
                    onLongClick = onLongClick,
                    onClick = onClick
                )
                .semantics {
                    contentDescription = description ?: label
                    stateDescription = spokenState
                },
            shape = shape,
            border = stateBorder,
            color = stateBackground,
            contentColor = stateInk
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                keyContents()
            }
        }
    }
    // A brief, visible action hint helps sighted users discover shortcuts.
    // Non-focusable popup does not interrupt the held touch gesture or PTY.
    if (held && enabled) {
        Popup(
            alignment = Alignment.TopCenter,
            offset = IntOffset(0, tooltipOffset),
            properties = PopupProperties(focusable = false)
        ) {
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(
                    KlyroUiMetrics.controlCorner),
                color = colors.sheet,
                contentColor = colors.foreground
            ) {
                Text(longClickDescription ?: description ?: label,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    fontSize = 12.sp, maxLines = 2)
            }
        }
    }
    }
}

private fun write(session: TerminalSession, value: String) {
    // Buttons may race PTY exit after the last Compose recomposition.
    if (value.isEmpty() || !session.isRunning() || session.emulator == null) return
    val bytes = value.toByteArray(StandardCharsets.UTF_8)
    session.write(bytes, 0, bytes.size)
}

private fun sendCodePointIfRunning(session: TerminalSession, codePoint: Int) {
    if (!session.isRunning() || session.emulator == null) return
    session.writeCodePoint(false, codePoint)
}
