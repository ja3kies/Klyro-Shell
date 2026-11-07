package com.oryn.terminal.ui

import android.content.ClipData
import android.content.Intent
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.Typeface
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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.oryn.terminal.session.OrynTerminalSessionClient
import com.oryn.terminal.workspace.KlyroWorkspaceCatalog
import com.termux.terminal.TerminalSession
import com.termux.view.TerminalView
import java.nio.charset.StandardCharsets

private val TerminalBackground = Color(0xFF101719)
private val TerminalSurface = Color(0xFF182528)
private val TerminalAccent = Color(0xFFB9DAD4)
private val TerminalText = Color(0xFFE3F1EA)
private val TerminalMuted = Color(0xFF98B0A9)

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
    workspaces: List<KlyroWorkspaceCatalog.Entry>,
    selectedWorkspaceId: String,
    tabWorkspaces: Map<Long, String>,
    tabsWithUnreadOutput: Set<Long>,
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
    var showDiagnosticsSheet by remember(session) { mutableStateOf(false) }
    val currentProject = workspaces.firstOrNull { it.id == selectedWorkspaceId }?.name ?: "General"
    var showTerminalSettings by remember(session) { mutableStateOf(false) }
    var initialSettingsSection by remember(session) { mutableStateOf("home") }
    var sessionSheetId by remember(session) { mutableStateOf<Long?>(null) }
    var renameDialogId by remember(session) { mutableStateOf<Long?>(null) }
    var shellChooserSourceId by remember(session) { mutableStateOf<Long?>(null) }
    var selectedOutput by remember(session) { mutableStateOf<String?>(null) }
    var outputSelectionView by remember(session) { mutableStateOf<EditText?>(null) }
    val context = LocalContext.current
    val clipboardManager = remember(context) {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    }
    var clipboardAvailable by remember(session) {
        mutableStateOf(ClipboardTextReader.hasPlainText(clipboardManager))
    }
    DisposableEffect(clipboardManager, session) {
        val listener = ClipboardManager.OnPrimaryClipChangedListener {
            clipboardAvailable = ClipboardTextReader.hasPlainText(clipboardManager)
        }
        clipboardManager?.addPrimaryClipChangedListener(listener)
        clipboardAvailable = ClipboardTextReader.hasPlainText(clipboardManager)
        onDispose { clipboardManager?.removePrimaryClipChangedListener(listener) }
    }
    var pendingMultilinePaste by remember(session) { mutableStateOf<String?>(null) }
    var hasCopyableOutput by remember(session) {
        mutableStateOf(TerminalActionPolicy.hasCopyableOutput(session.emulator?.screen?.transcriptText))
    }
    DisposableEffect(sessionClient, session) {
        val observer = Runnable {
            hasCopyableOutput = TerminalActionPolicy.hasCopyableOutput(session.emulator?.screen?.transcriptText)
        }
        sessionClient.setOutputObserver(observer)
        observer.run()
        onDispose { sessionClient.setOutputObserver(null) }
    }
    fun pasteClipboard() {
        val content = ClipboardTextReader.readPlainText(clipboardManager)
        if (!ClipboardPastePolicy.canPaste(content)) {
            Toast.makeText(context, "No supported clipboard text", Toast.LENGTH_SHORT).show()
        } else if (ClipboardPastePolicy.requiresConfirmation(content)) {
            pendingMultilinePaste = content
        } else {
            // Termux TerminalEmulator handles DECSET 2004 bracketed paste.
            session.emulator?.paste(content!!)
        }
    }
    LaunchedEffect(nativePasteRequest, session) {
        if (nativePasteRequest != null) {
            pendingMultilinePaste = nativePasteRequest
            onNativePasteConsumed()
        }
    }
    var activeTerminalView by remember(session) { mutableStateOf<TerminalView?>(null) }
    LaunchedEffect(activeTabId, activeTerminalView, sessionSheetId, renameDialogId, shellChooserSourceId) {
        if (sessionSheetId == null && renameDialogId == null && shellChooserSourceId == null) {
            activeTerminalView?.let { view ->
                view.isFocusableInTouchMode = true
                view.requestFocus()
            }
        }
    }
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
    var showTranscriptSearch by remember(session) { mutableStateOf(false) }
    var transcriptQuery by remember(session) { mutableStateOf("") }
    var findMatchCase by remember(session) { mutableStateOf(false) }
    var searchIndex by remember(session) { mutableStateOf(0) }
    var pendingSearch by remember(session) { mutableStateOf<Int?>(null) }
    val keyboard = LocalSoftwareKeyboardController.current
    val ime = WindowInsets.ime
    val density = LocalDensity.current
    LaunchedEffect(pendingSearch, session, activeTerminalView) {
        val index = pendingSearch ?: return@LaunchedEffect
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
        session.emulator?.let { emulator ->
            val matches = TranscriptSearch.findRows(emulator.screen, emulator.mColumns, emulator.mRows, transcriptQuery, findMatchCase)
            if (matches.isNotEmpty()) {
                view.setTopRow(TranscriptSearch.topRow(matches[index.coerceAtMost(matches.lastIndex)],
                    emulator.mRows, emulator.screen.activeTranscriptRows))
                view.invalidate()
            }
        }
        view.requestFocus()
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
    var showEndSessionConfirmation by remember(session) { mutableStateOf(false) }
    var warningNoteDismissed by remember(session) { mutableStateOf(false) }

    BackHandler(
        enabled = !showEndSessionConfirmation && !showToolsMenu &&
            !showTerminalSettings && !showTranscriptSearch &&
            !showWorkspaceSheet && !showCommandPalette && !showDiagnosticsSheet &&
            pendingMultilinePaste == null && selectedOutput == null &&
            sessionSheetId == null && renameDialogId == null && shellChooserSourceId == null
    ) {
        showEndSessionConfirmation = true
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val compact = maxHeight < 300.dp
    // Fit seven 48dp+ touch targets across a Pixel-width screen; scroll on narrower devices.
    val keyWidth = if (compact) 48.dp else ((maxWidth - 36.dp) / 7).coerceIn(40.dp, 56.dp)
    val toolsMenu: @Composable () -> Unit = {
            Box {
                TextButton(onClick = { showToolsMenu = true }) {
                    Text("⋮", color = TerminalAccent, fontSize = 22.sp,
                        modifier = Modifier.semantics { contentDescription = "More actions" })
                }
                DropdownMenu(
                    expanded = showToolsMenu,
                    onDismissRequest = { showToolsMenu = false },
                    modifier = Modifier.width(246.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    DropdownMenuItem(
                        text = { Text("Command Palette") },
                        onClick = { showToolsMenu = false; showCommandPalette = true }
                    )
                    DropdownMenuItem(
                        text = { Text("Workspaces") },
                        onClick = { showToolsMenu = false; showWorkspaceSheet = true }
                    )
                    DropdownMenuItem(
                        text = { Text("Diagnostics") },
                        onClick = { showToolsMenu = false; showDiagnosticsSheet = true }
                    )
                    DropdownMenuItem(
                        text = { Text("Settings", color = MaterialTheme.colorScheme.onSurface) },
                        modifier = Modifier.heightIn(min = 56.dp),
                        leadingIcon = {
                            Icon(painterResource(R.drawable.hugeicons_settings_01),
                                contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                        },
                        onClick = {
                            showToolsMenu = false
                            initialSettingsSection = "home"
                            showTerminalSettings = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Files & storage", color = MaterialTheme.colorScheme.onSurface) },
                        modifier = Modifier.heightIn(min = 56.dp),
                        leadingIcon = {
                            Icon(painterResource(R.drawable.hugeicons_folder_01),
                                contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                        },
                        onClick = {
                            showToolsMenu = false
                            initialSettingsSection = "files"
                            showTerminalSettings = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Home", color = MaterialTheme.colorScheme.onSurface) },
                        modifier = Modifier.heightIn(min = 56.dp),
                        leadingIcon = {
                            Icon(painterResource(R.drawable.hugeicons_terminal),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface)
                        },
                        onClick = {
                            showToolsMenu = false
                            onReturnHome()
                        }
                    )

                }
            }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBackground)
    ) {
        if (!compact) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TerminalSurface)
                    .padding(start = 14.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        title,
                        color = TerminalAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text("·", color = TerminalMuted)
                    Text("ARM64", color = TerminalMuted, fontSize = 11.sp)
                    Text("·", color = TerminalMuted)
                    Text(currentProject, color = TerminalMuted, fontSize = 11.sp,
                        maxLines = 1)
                    if (runtimeHasWarnings) {
                        Text("⚠", color = ui.warningInk, fontSize = 12.sp,
                            modifier = Modifier.semantics {
                                contentDescription = "Runtime warnings reported"
                            })
                    }
                }

                toolsMenu()
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
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .combinedClickable(
                            onClickLabel = if (active) "Edit session" else "Switch session",
                            onLongClickLabel = "Edit session",
                            onClick = {
                                if (active) openSessionSheet(id, tabTitle) else onSelectTab(id)
                            },
                            onLongClick = { openSessionSheet(id, tabTitle) }
                        )
                        .semantics {
                            contentDescription = tabTitle.ifBlank { "Session" } +
                                ", project " + (workspaces.firstOrNull {
                                    it.id == (tabWorkspaces[id] ?: KlyroWorkspaceCatalog.GENERAL)
                                }?.name ?: "General")
                            stateDescription = if (id in tabsWithUnreadOutput && !active)
                                "New terminal output" else if (active) "Selected terminal"
                                else "Open terminal"
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
                        onClickLabel = "New terminal",
                        onClick = onNewTab
                    )
                    .semantics { contentDescription = "New terminal" },
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
                    Box(contentAlignment = Alignment.Center) { Text("+", fontSize = 20.sp) }
                }
            }
        }

        if (sessionFailureMessage != null) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                color = ui.dangerContainer,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
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
                    TextButton(onClick = onReturnHome) {
                        Text("Home", color = TerminalAccent, fontSize = 12.sp)
                    }
                }
            }
        }

        if (runtimeHasWarnings && sessionFailureMessage == null && !warningNoteDismissed) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                color = ui.warningContainer,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "The environment check reported warnings. Messages above the prompt may relate to them.",
                        color = ui.warningInk,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = { warningNoteDismissed = true },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Dismiss", color = TerminalAccent, fontSize = 12.sp)
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
                            outputSelectionView = null
                            selectedOutput = session.emulator?.screen?.transcriptText.orEmpty()
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
            TerminalExtraKey("ESC", keyWidth = keyWidth, darkChrome = darkChrome) { session.writeCodePoint(false, 27) }
            TerminalExtraKey("TAB", keyWidth = keyWidth, darkChrome = darkChrome) { session.writeCodePoint(false, 9) }
            TerminalExtraKey("CTRL", active = controlActive, keyWidth = keyWidth, darkChrome = darkChrome) {
                controlActive = !controlActive
                viewClient.setControlDown(controlActive)
            }
            TerminalExtraKey("ALT", active = altActive, keyWidth = keyWidth, darkChrome = darkChrome) {
                altActive = !altActive
                viewClient.setAltDown(altActive)
            }
            TerminalExtraKey("SHIFT", active = shiftActive, keyWidth = keyWidth, darkChrome = darkChrome) {
                shiftActive = !shiftActive
                viewClient.setShiftDown(shiftActive)
            }
            TerminalExtraKey("FN", active = fnActive, keyWidth = keyWidth, darkChrome = darkChrome) {
                fnActive = !fnActive
                viewClient.setFnDown(fnActive)
            }
            TerminalExtraKey(
                if (hasCopyableOutput) "ALL" else "PASTE",
                description = if (hasCopyableOutput) "Select and copy terminal output" else "Paste clipboard text",
                glyph = if (hasCopyableOutput) R.drawable.hugeicons_clipboard_list else R.drawable.hugeicons_clipboard_paste,
                keyWidth = keyWidth, darkChrome = darkChrome,
                enabled = hasCopyableOutput || clipboardAvailable
            ) {
                val output = session.emulator?.screen?.transcriptText.orEmpty()
                if (TerminalActionPolicy.hasCopyableOutput(output)) {
                    outputSelectionView = null
                    selectedOutput = output
                } else pasteClipboard()
            }
        }
        val secondRow: @Composable () -> Unit = {
            TerminalExtraKey("←", description = "Left arrow", arrow = "left", keyWidth = keyWidth, darkChrome = darkChrome,
                longClickDescription = "Go to start of line",
                onLongClick = { write(session, TerminalNavigationPolicy.horizontalLongPress(false,
                    session.emulator?.isAlternateBufferActive == true, controlActive, altActive, shiftActive)) }
            ) { write(session, TerminalNavigationPolicy.arrow('D', controlActive, altActive, shiftActive)) }
            TerminalExtraKey("↑", description = "Up arrow", arrow = "up", keyWidth = keyWidth, darkChrome = darkChrome) { write(session, TerminalNavigationPolicy.arrow('A', controlActive, altActive, shiftActive)) }
            TerminalExtraKey("↓", description = "Down arrow", arrow = "down", keyWidth = keyWidth, darkChrome = darkChrome) { write(session, TerminalNavigationPolicy.arrow('B', controlActive, altActive, shiftActive)) }
            TerminalExtraKey("→", description = "Right arrow", arrow = "right", keyWidth = keyWidth, darkChrome = darkChrome,
                longClickDescription = "Go to end of line",
                onLongClick = { write(session, TerminalNavigationPolicy.horizontalLongPress(true,
                    session.emulator?.isAlternateBufferActive == true, controlActive, altActive, shiftActive)) }
            ) { write(session, TerminalNavigationPolicy.arrow('C', controlActive, altActive, shiftActive)) }
            TerminalExtraKey("FIND", description = "Find in terminal output", keyWidth = keyWidth, darkChrome = darkChrome) {
                transcriptQuery = ""
                searchIndex = 0
                showTranscriptSearch = true
            }
            TerminalExtraKey("END", description = "Move cursor to end of line", keyWidth = keyWidth, darkChrome = darkChrome) {
                write(session, TerminalNavigationPolicy.end(controlActive, altActive, shiftActive))
            }
            TerminalExtraKey("CLEAR", description = "Clear terminal display", glyph = R.drawable.hugeicons_eraser, keyWidth = keyWidth, darkChrome = darkChrome) {
                // Clear emulator display and scrollback without injecting input into Bash.
                TerminalDisplayActions.clear(session)
                activeTerminalView?.setTopRow(0)
                activeTerminalView?.invalidate()
                hasCopyableOutput = false
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
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) { firstRow() }
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) { secondRow() }
            }
        }
    }

    }

    // The native More item opens this in-app Select all action.
    // Selection is performed on a native read-only text surface, without
    // accidentally sending selected content to the running Bash session.
    selectedOutput?.let { output ->
        val selectionLimit = 2 * 1024 * 1024
        val truncated = output.length > selectionLimit
        val visibleOutput = output.takeLast(selectionLimit)
        fun closeSelection() {
            outputSelectionView = null
            selectedOutput = null
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
                            "Only the latest 2 MiB are selectable; older output is omitted for memory safety.",
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
                                    setShowSoftInputOnFocus(false)
                                    setHorizontallyScrolling(false)
                                    setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f)
                                    // Selection preview shares the current terminal ink,
                                    // including light mode; no isolated black subpanel.
                                    setTextColor(ui.foreground.toArgb())
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
                            val left = view.selectionStart.coerceIn(0, value.length)
                            val right = view.selectionEnd.coerceIn(0, value.length)
                            val selection = if (left < right) value.substring(left, right) else value
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val copied = runCatching {
                                clipboard.setPrimaryClip(ClipData.newPlainText("Klyro terminal", selection))
                            }.isSuccess
                            Toast.makeText(
                                context,
                                if (copied) "Selection copied" else "Clipboard unavailable",
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
            tabs = tabs,
            onEditSession = ::openSessionSheet,
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
            onSetupStorage = onSetupStorage,
            onImportStorageFolder = onImportStorageFolder,
            onImportFile = onImportFile,
            onExportFile = onExportFile,
            onSelectOutput = {
                outputSelectionView = null
                selectedOutput = session.emulator?.screen?.transcriptText.orEmpty()
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
                        pendingMultilinePaste.orEmpty().take(500),
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 8
                    )
                    if ((pendingMultilinePaste?.length ?: 0) > 500) {
                        Text("Preview truncated; cancel if the remaining content is unknown.")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val text = pendingMultilinePaste
                    pendingMultilinePaste = null
                    if (ClipboardPastePolicy.canPaste(text)) {
                        session.emulator?.paste(text!!)
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
                onDismiss = { sessionSheetId = null },
                onRename = {
                    sessionSheetId = null
                    renameDialogId = id
                },
                onChooseShell = {
                    sessionSheetId = null
                    shellChooserSourceId = id
                },
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
            onDismiss = { shellChooserSourceId = null },
            onBack = {
                shellChooserSourceId = null
                sessionSheetId = sourceId
            },
            onNewBash = {
                shellChooserSourceId = null
                onNewTab()
            }
        )
    }

    if (showTranscriptSearch) {
        val emulator = session.emulator
        val screen = emulator?.screen
        // Re-read the mutable buffer; a screen object keeps its identity as output arrives.
        val matches = if (emulator == null || screen == null) emptyList<Int>()
            else TranscriptSearch.findRows(screen, emulator.mColumns, emulator.mRows, transcriptQuery, findMatchCase)
        val position = if (matches.isEmpty()) 0 else searchIndex.coerceIn(0, matches.lastIndex)
        fun goToMatch(index: Int) {
            if (matches.isNotEmpty() && emulator != null) {
                activeTerminalView?.let { view ->
                    view.setTopRow(TranscriptSearch.topRow(matches[index], emulator.mRows, screen!!.activeTranscriptRows))
                    view.invalidate()
                }
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
                        onValueChange = { transcriptQuery = it.take(128); searchIndex = 0 },
                        label = { Text("Find text") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = findMatchCase, onCheckedChange = {
                            findMatchCase = it
                            searchIndex = 0
                        })
                        Text("Match case", fontSize = 13.sp)
                    }
                    Text(
                        if (transcriptQuery.isBlank()) "Enter text to search"
                        else if (matches.isEmpty()) "No matches"
                        else "Match ${position + 1} of ${matches.size}",
                        fontSize = 12.sp
                    )
                    if (matches.isNotEmpty() && screen != null && emulator != null) {
                        // Preview a physical transcript row without changing terminal output.
                        val matchRow = matches[position]
                        val line = screen.getSelectedText(
                            0, matchRow, emulator.mColumns - 1, matchRow, false, false
                        ).orEmpty()
                        val highlightBackground = MaterialTheme.colorScheme.primary
                        val highlightForeground = MaterialTheme.colorScheme.onPrimary
                        val preview = buildAnnotatedString {
                            var offset = 0
                            while (offset < line.length && transcriptQuery.isNotEmpty()) {
                                val found = line.indexOf(transcriptQuery, offset, ignoreCase = !findMatchCase)
                                if (found < 0) break
                                append(line.substring(offset, found))
                                withStyle(SpanStyle(
                                    color = highlightForeground,
                                    background = highlightBackground,
                                    fontWeight = FontWeight.Bold
                                )) { append(line.substring(found, found + transcriptQuery.length)) }
                                offset = found + transcriptQuery.length
                            }
                            // Matches can also span wrapped physical rows.
                            append(line.substring(offset))
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
                        ) {
                            Text(
                                text = preview,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth().padding(10.dp)
                            )
                        }
                    }
                    if (matches.isNotEmpty()) {
                        Row {
                            TextButton(onClick = {
                                searchIndex = TranscriptSearch.nextIndex(position, -1, matches.size)
                                goToMatch(searchIndex)
                            }) { Text("Previous") }
                            TextButton(onClick = {
                                searchIndex = TranscriptSearch.nextIndex(position, 1, matches.size)
                                goToMatch(searchIndex)
                            }) { Text("Next") }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingSearch = position
                        keyboard?.hide()
                        showTranscriptSearch = false
                    },
                    enabled = matches.isNotEmpty(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(KlyroUiMetrics.controlCorner)
                ) { Text("Go to line") }
            },
            dismissButton = {
                TextButton(onClick = { showTranscriptSearch = false }) { Text("Cancel") }
            }
        )
        }
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
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val colors = LocalKlyroColors.current
    val accentTint = colors.accent
    val inkTint = colors.foreground
    val backgroundTint = colors.key
    val keyContents: @Composable () -> Unit = {
        if (glyph != null) {
            Icon(
                painter = painterResource(glyph),
                contentDescription = null,
                modifier = Modifier.width(24.dp).height(24.dp),
                tint = if (active) accentTint else inkTint
            )
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
                        color = inkTint, start = end, end = tip,
                        strokeWidth = 1.75.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }
        } else {
            Text(label, fontSize = labelSize, fontFamily = FontFamily.Monospace)
        }
    }
    if (onLongClick == null) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .height(50.dp)
                .width(keyWidth)
                .semantics { contentDescription = description ?: label },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(KlyroUiMetrics.controlCorner),
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (active) accentTint.copy(alpha = 0.18f)
                    else backgroundTint,
                contentColor = if (active) accentTint else inkTint
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
                    enabled = enabled,
                    role = Role.Button,
                    onClickLabel = description ?: label,
                    onLongClickLabel = longClickDescription ?: "Long press",
                    onLongClick = onLongClick,
                    onClick = onClick
                )
                .semantics { contentDescription = description ?: label },
            shape = shape,
            color = if (active) accentTint.copy(alpha = 0.18f) else backgroundTint,
            contentColor = if (active) accentTint else inkTint
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                keyContents()
            }
        }
    }
}

private fun write(session: TerminalSession, value: String) {
    val bytes = value.toByteArray(StandardCharsets.UTF_8)
    session.write(bytes, 0, bytes.size)
}
