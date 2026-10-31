package com.oryn.terminal.ui

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
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

@Composable
fun TerminalPane(
    session: TerminalSession,
    title: String,
    sessionClient: OrynTerminalSessionClient,
    viewClient: OrynTerminalViewClient,
    sessionFailureMessage: String?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val terminalTextSizePx = with(LocalDensity.current) { 14.sp.roundToPx() }
    var controlActive by remember { mutableStateOf(viewClient.isControlDown()) }
    var altActive by remember { mutableStateOf(viewClient.isAltDown()) }
    var shiftActive by remember { mutableStateOf(false) }
    var fnActive by remember { mutableStateOf(false) }
    var showEndSessionConfirmation by remember { mutableStateOf(false) }

    BackHandler(enabled = !showEndSessionConfirmation) {
        showEndSessionConfirmation = true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBackground)
    ) {
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
                Text("ARM64", color = TerminalMuted, fontSize = 10.sp)
            }
            Button(
                onClick = { showEndSessionConfirmation = true },
                modifier = Modifier.heightIn(min = 48.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E293B),
                    contentColor = TerminalText
                )
            ) {
                Text("End session", fontSize = 12.sp)
            }
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

        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
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
                }
            },
            update = { view ->
                sessionClient.attachView(view)
                viewClient.attachView(view)
                if (view.getCurrentSession() !== session) {
                    view.attachSession(session)
                }
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TerminalSurface)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TerminalExtraKey("ESC") { session.writeCodePoint(false, 27) }
            TerminalExtraKey("TAB") { session.writeCodePoint(false, 9) }
            TerminalExtraKey("CTRL", active = controlActive) {
                controlActive = !controlActive
                viewClient.setControlDown(controlActive)
            }
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
            TerminalExtraKey("↑") { write(session, "\u001B[A") }
            TerminalExtraKey("↓") { write(session, "\u001B[B") }
            TerminalExtraKey("←") { write(session, "\u001B[D") }
            TerminalExtraKey("→") { write(session, "\u001B[C") }
            TerminalExtraKey("HOME") { write(session, "\u001B[H") }
            TerminalExtraKey("END") { write(session, "\u001B[F") }
            TerminalExtraKey("DEL") { session.writeCodePoint(false, 127) }
        }
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
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (active) TerminalAccent.copy(alpha = 0.18f)
                else Color(0xFF1E293B),
            contentColor = if (active) TerminalAccent else TerminalText
        )
    ) {
        Text(label, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    }
}

private fun write(session: TerminalSession, value: String) {
    val bytes = value.toByteArray(StandardCharsets.UTF_8)
    session.write(bytes, 0, bytes.size)
}
