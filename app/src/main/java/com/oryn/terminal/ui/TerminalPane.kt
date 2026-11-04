package com.oryn.terminal.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color as AndroidColor
import android.widget.EditText
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
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
import com.termux.terminal.TerminalSession
import com.termux.view.TerminalView
import java.nio.charset.StandardCharsets

private val TerminalBackground = Color(0xFF0B0F19)
private val TerminalSurface = Color(0xFF111827)
private val TerminalAccent = Color(0xFF00F0FF)
private val TerminalText = Color(0xFFE2E8F0)
private val TerminalMuted = Color(0xFF94A3B8)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TerminalPane(
    session: TerminalSession,
    title: String,
    tabs: List<Pair<Long, String>>,
    activeTabId: Long?,
    onSelectTab: (Long) -> Unit,
    onRenameTab: (Long, String) -> Unit,
    onCloseTab: (Long) -> Unit,
    onNewTab: () -> Unit,
    onImportFile: () -> Unit,
    onExportFile: () -> Unit,
    onSetupStorage: () -> Unit,
    onImportStorageFolder: () -> Unit,
    sessionClient: OrynTerminalSessionClient,
    viewClient: OrynTerminalViewClient,
    sessionFailureMessage: String?,
    onClose: () -> Unit,
    runtimeHasWarnings: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showToolsMenu by remember { mutableStateOf(false) }
    var showTerminalSettings by remember { mutableStateOf(false) }
    var pendingMultilinePaste by remember { mutableStateOf<String?>(null) }
    var activeTerminalView by remember { mutableStateOf<TerminalView?>(null) }
    LaunchedEffect(activeTabId, activeTerminalView) {
        activeTerminalView?.let { view ->
            view.isFocusableInTouchMode = true
            view.requestFocus()
        }
    }
    val context = LocalContext.current
    val fontPreferences = remember(context) {
        context.getSharedPreferences("klyro_terminal_preferences", Context.MODE_PRIVATE)
    }
    var terminalFontSp by remember {
        mutableStateOf(fontPreferences.getFloat("font_size_sp", 13.5f).coerceIn(9f, 24f))
    }
    val terminalTextSizePx = with(LocalDensity.current) { terminalFontSp.sp.roundToPx() }
    // Apply only when the preference changes: avoid resetting pinch-to-zoom on recomposition.
    LaunchedEffect(terminalTextSizePx, activeTerminalView) {
        activeTerminalView?.setTextSize(terminalTextSizePx)
    }
    fun changeTerminalFont(delta: Float) {
        terminalFontSp = (terminalFontSp + delta).coerceIn(9f, 24f)
        fontPreferences.edit().putFloat("font_size_sp", terminalFontSp).apply()
    }
    var showTranscriptSearch by remember { mutableStateOf(false) }
    var transcriptQuery by remember { mutableStateOf("") }
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
            val matches = TranscriptSearch.findRows(emulator.screen, emulator.mColumns, emulator.mRows, transcriptQuery)
            if (matches.isNotEmpty()) {
                view.setTopRow(TranscriptSearch.topRow(matches[index.coerceAtMost(matches.lastIndex)],
                    emulator.mRows, emulator.screen.activeTranscriptRows))
                view.invalidate()
            }
        }
        view.requestFocus()
        pendingSearch = null
    }
    var renameTarget by remember { mutableStateOf<Long?>(null) }
    var renameValue by remember { mutableStateOf("") }
    var closeTarget by remember { mutableStateOf<Long?>(null) }
    var controlActive by remember(viewClient) { mutableStateOf(viewClient.isControlDown()) }
    var altActive by remember(viewClient) { mutableStateOf(viewClient.isAltDown()) }
    var shiftActive by remember(viewClient) { mutableStateOf(false) }
    var fnActive by remember(viewClient) { mutableStateOf(false) }
    var showEndSessionConfirmation by remember { mutableStateOf(false) }
    var warningNoteDismissed by remember { mutableStateOf(false) }

    BackHandler(
        enabled = !showEndSessionConfirmation && !showToolsMenu &&
            !showTerminalSettings && !showTranscriptSearch &&
            pendingMultilinePaste == null && renameTarget == null && closeTarget == null
    ) {
        showEndSessionConfirmation = true
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val compact = maxHeight < 300.dp
    val toolsMenu: @Composable () -> Unit = {
            Box {
                TextButton(onClick = { showToolsMenu = true }) {
                    Text("⋮", color = TerminalAccent, fontSize = 22.sp)
                }
                DropdownMenu(expanded = showToolsMenu, onDismissRequest = { showToolsMenu = false }) {
                    DropdownMenuItem(text = { Text("Settings") }, onClick = {
                        showToolsMenu = false; showTerminalSettings = true
                    })
                    DropdownMenuItem(text = { Text("Set up storage folder") }, onClick = {
                        showToolsMenu = false; onSetupStorage()
                    })
                    DropdownMenuItem(text = { Text("Import from authorized folder") }, onClick = {
                        showToolsMenu = false; onImportStorageFolder()
                    })
                    DropdownMenuItem(text = { Text("Import file") }, onClick = {
                        showToolsMenu = false; onImportFile()
                    })
                    DropdownMenuItem(text = { Text("Export file") }, onClick = {
                        showToolsMenu = false; onExportFile()
                    })
                    DropdownMenuItem(text = { Text("End session") }, onClick = {
                        showToolsMenu = false; showEndSessionConfirmation = true
                    })
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
                    .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
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
                }

                toolsMenu()
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth()
                .background(TerminalSurface)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (compact) toolsMenu()
            tabs.forEach { (id, tabTitle) ->
                Surface(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .semantics {
                            contentDescription = "Session $tabTitle"
                            onLongClick("Renommer la session") {
                                renameTarget = id
                                renameValue = tabTitle
                                true
                            }
                        }
                        .combinedClickable(
                            onClick = { onSelectTab(id) },
                            onLongClick = {
                                renameTarget = id
                                renameValue = tabTitle
                            }
                        ),
                    color = if (id == activeTabId) TerminalAccent.copy(alpha = 0.2f)
                        else Color(0xFF1E293B),
                    contentColor = if (id == activeTabId) TerminalAccent else TerminalText,
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Row(
                        modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(tabTitle.take(18), fontSize = 12.sp)
                        TextButton(onClick = { closeTarget = id }) {
                            Text("×", fontSize = 16.sp)
                        }
                    }
                }
            }
            Button(
                onClick = onNewTab,
                enabled = tabs.size < 8,
                modifier = Modifier.heightIn(min = 44.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
            ) { Text("+", fontSize = 18.sp) }
        }

        if (sessionFailureMessage != null) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                color = Color(0xFF2A1B25),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        sessionFailureMessage,
                        color = Color(0xFFFFB4C0),
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onClose) {
                        Text("Back to setup", color = TerminalAccent, fontSize = 12.sp)
                    }
                }
            }
        }

        if (runtimeHasWarnings && sessionFailureMessage == null && !warningNoteDismissed) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                color = Color(0xFF2B2415),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "The environment check reported warnings. Messages above the prompt may relate to them.",
                        color = Color(0xFFFFC857),
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

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
        key(session) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            factory = { context ->
                TerminalView(context, null).apply {
                    setTerminalViewClient(viewClient)
                    setTextSize(terminalTextSizePx)
                    setBackgroundColor(AndroidColor.rgb(11, 15, 25))
                    isFocusable = true
                    isFocusableInTouchMode = true
                    sessionClient.attachView(this)
                    viewClient.attachView(this)
                    attachSession(session)
                    activeTerminalView = this
                }
            },
            update = { view ->
                if (activeTerminalView !== view) activeTerminalView = view
                sessionClient.attachView(view)
                viewClient.attachView(view)
                if (view.getCurrentSession() !== session) {
                    view.attachSession(session)
                }
            }
        )
        }
        }

        val secondaryKeys: @Composable () -> Unit = {
                TerminalExtraKey("HOME") { write(session, "\u001B[H") }
                TerminalExtraKey("DEL") { session.writeCodePoint(false, 127) }
                TerminalExtraKey("←", description = "Left arrow", labelSize = 18.sp) { write(session, "\u001B[D") }
                TerminalExtraKey("↓", description = "Down arrow", labelSize = 18.sp) { write(session, "\u001B[B") }
                TerminalExtraKey("→", description = "Right arrow", labelSize = 18.sp) { write(session, "\u001B[C") }
                TerminalExtraKey("PASTE", description = "Paste clipboard into terminal") {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val pasted = runCatching {
                        clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()
                    }.getOrNull()
                    if (!pasted.isNullOrEmpty()) {
                        if (pasted.contains('\n') || pasted.contains('\r')) {
                            pendingMultilinePaste = pasted
                        } else {
                            write(session, pasted)
                        }
                    }
                }
                TerminalExtraKey("ALL", description = "Copy all terminal text") {
                    val transcript = session.emulator?.screen?.transcriptText.orEmpty()
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    // Avoid placing unexpectedly large scrollback in Android's clipboard.
                    clipboard.setPrimaryClip(ClipData.newPlainText(
                        "Klyro terminal", transcript.takeLast(256 * 1024)
                    ))
                }
                TerminalExtraKey("FIND", description = "Search terminal transcript") {
                    transcriptQuery = ""
                    searchIndex = 0
                    showTranscriptSearch = true
                }
                TerminalExtraKey("CLEAR", description = "Clear terminal display", eraserIcon = true) {
                    session.writeCodePoint(false, 12) // Ctrl+L redraws Bash prompt
                }
                TerminalExtraKey("END") { write(session, "\u001B[F") }
        }
        Column(
            modifier = Modifier.fillMaxWidth().background(TerminalSurface)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TerminalExtraKey("ESC") { session.writeCodePoint(false, 27) }
                TerminalExtraKey("TAB") { session.writeCodePoint(false, 9) }
                TerminalExtraKey("CTRL", active = controlActive) {
                    controlActive = !controlActive
                    viewClient.setControlDown(controlActive)
                }
                TerminalExtraKey("↑", description = "Up arrow", labelSize = 18.sp) { write(session, "\u001B[A") }
                TerminalExtraKey("ALT", active = altActive) {
                    altActive = !altActive
                    viewClient.setAltDown(altActive)
                }
                TerminalExtraKey("SHIFT", active = shiftActive) {
                    shiftActive = !shiftActive
                    viewClient.setShiftDown(shiftActive)
                }
                TerminalExtraKey("FN", active = fnActive) {
                    fnActive = !fnActive
                    viewClient.setFnDown(fnActive)
                }
                if (compact) secondaryKeys()
            }
            if (!compact) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) { secondaryKeys() }
            }
        }
    }

    }

    if (showTerminalSettings) {
        AlertDialog(
            onDismissRequest = { showTerminalSettings = false },
            title = { Text("Terminal settings") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Font size", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${terminalFontSp.toInt()} sp",
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { changeTerminalFont(-1f) },
                            enabled = terminalFontSp > 9f
                        ) { Text("A−", modifier = Modifier.semantics { contentDescription = "Decrease font size" }) }
                        TextButton(
                            onClick = { changeTerminalFont(1f) },
                            enabled = terminalFontSp < 24f
                        ) { Text("A+", modifier = Modifier.semantics { contentDescription = "Increase font size" }) }
                    }
                    Text("Saved automatically", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { showTerminalSettings = false }) { Text("Done") }
            }
        )
    }

    if (pendingMultilinePaste != null) {
        AlertDialog(
            onDismissRequest = { pendingMultilinePaste = null },
            title = { Text("Paste multiple lines?") },
            text = { Text("Pasting multiple lines can execute commands in the terminal. Continue only if you trust the clipboard contents.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingMultilinePaste?.let { write(session, it) }
                    pendingMultilinePaste = null
                }) { Text("Paste") }
            },
            dismissButton = {
                TextButton(onClick = { pendingMultilinePaste = null }) { Text("Cancel") }
            }
        )
    }

    if (renameTarget != null) {
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename session") },
            text = {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it.take(32) },
                    label = { Text("Session name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    enabled = renameValue.isNotBlank(),
                    onClick = {
                        renameTarget?.let { onRenameTab(it, renameValue) }
                        renameTarget = null
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Cancel") }
            }
        )
    }

    if (closeTarget != null) {
        AlertDialog(
            onDismissRequest = { closeTarget = null },
            title = { Text("Close session?") },
            text = { Text("The shell process in this tab will be stopped.") },
            confirmButton = {
                TextButton(onClick = {
                    closeTarget?.let(onCloseTab)
                    closeTarget = null
                }) { Text("Close session") }
            },
            dismissButton = {
                TextButton(onClick = { closeTarget = null }) { Text("Cancel") }
            }
        )
    }

    if (showTranscriptSearch) {
        val emulator = session.emulator
        val screen = emulator?.screen
        // Re-read the mutable buffer; a screen object keeps its identity as output arrives.
        val matches = if (emulator == null || screen == null) emptyList<Int>()
            else TranscriptSearch.findRows(screen, emulator.mColumns, emulator.mRows, transcriptQuery)
        val position = if (matches.isEmpty()) 0 else searchIndex.coerceIn(0, matches.lastIndex)
        fun goToMatch(index: Int) {
            if (matches.isNotEmpty() && emulator != null) {
                activeTerminalView?.let { view ->
                    view.setTopRow(TranscriptSearch.topRow(matches[index], emulator.mRows, screen!!.activeTranscriptRows))
                    view.invalidate()
                }
            }
        }
        AlertDialog(
            onDismissRequest = { showTranscriptSearch = false },
            title = { Text("Search terminal transcript") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = transcriptQuery,
                        onValueChange = { transcriptQuery = it.take(128); searchIndex = 0 },
                        label = { Text("Find text") },
                        singleLine = true
                    )
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
                        val offset = line.indexOf(transcriptQuery, ignoreCase = true)
                        val highlightBackground = MaterialTheme.colorScheme.primary
                        val highlightForeground = MaterialTheme.colorScheme.onPrimary
                        val preview = buildAnnotatedString {
                            if (offset >= 0) {
                                append(line.substring(0, offset))
                                withStyle(SpanStyle(
                                    color = highlightForeground,
                                    background = highlightBackground,
                                    fontWeight = FontWeight.Bold
                                )) {
                                    append(line.substring(offset, offset + transcriptQuery.length))
                                }
                                append(line.substring(offset + transcriptQuery.length))
                            } else {
                                // Matches may span wrapped physical rows.
                                append(line)
                            }
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = preview,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
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
                TextButton(onClick = {
                    pendingSearch = position
                    keyboard?.hide()
                    showTranscriptSearch = false
                }, enabled = matches.isNotEmpty()) { Text("Go to line") }
            },
            dismissButton = {
                TextButton(onClick = { showTranscriptSearch = false }) { Text("Cancel") }
            }
        )
    }

    if (showEndSessionConfirmation) {
        AlertDialog(
            onDismissRequest = { showEndSessionConfirmation = false },
            title = { Text("End Bash session?") },
            text = { Text("This will stop the running shell and its child processes.") },
            confirmButton = {
                TextButton(onClick = {
                    showEndSessionConfirmation = false
                    onClose()
                }) { Text("End session") }
            },
            dismissButton = {
                TextButton(onClick = { showEndSessionConfirmation = false }) {
                    Text("Keep session")
                }
            }
        )
    }
}

@Composable
private fun TerminalExtraKey(
    label: String,
    active: Boolean = false,
    description: String? = null,
    labelSize: androidx.compose.ui.unit.TextUnit = 12.sp,
    eraserIcon: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .width(54.dp)
            .semantics { contentDescription = description ?: label },
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (active) TerminalAccent.copy(alpha = 0.18f)
                else Color(0xFF1E293B),
            contentColor = if (active) TerminalAccent else TerminalText
        )
    ) {
        if (eraserIcon) {
            Canvas(modifier = Modifier.width(24.dp).height(24.dp)) {
                val outline = Path().apply {
                    moveTo(size.width * 0.15f, size.height * 0.62f)
                    lineTo(size.width * 0.57f, size.height * 0.15f)
                    lineTo(size.width * 0.88f, size.height * 0.44f)
                    lineTo(size.width * 0.47f, size.height * 0.88f)
                    lineTo(size.width * 0.31f, size.height * 0.88f)
                    close()
                }
                drawPath(outline, color = Color(0xFFE2E8F0), style = Stroke(width = 2.5.dp.toPx()))
                drawLine(
                    color = Color(0xFFE2E8F0),
                    start = androidx.compose.ui.geometry.Offset(size.width * 0.40f, size.height * 0.37f),
                    end = androidx.compose.ui.geometry.Offset(size.width * 0.70f, size.height * 0.67f),
                    strokeWidth = 2.dp.toPx()
                )
            }
        } else {
            Text(label, fontSize = labelSize, fontFamily = FontFamily.Monospace)
        }
    }
}

private fun write(session: TerminalSession, value: String) {
    val bytes = value.toByteArray(StandardCharsets.UTF_8)
    session.write(bytes, 0, bytes.size)
}
