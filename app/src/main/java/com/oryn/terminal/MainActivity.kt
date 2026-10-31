package com.oryn.terminal

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oryn.terminal.session.OrynTerminalSessionClient
import com.oryn.terminal.session.TerminalSessionManager
import com.oryn.terminal.termux.ProotRuntimeInstaller
import com.oryn.terminal.termux.TermuxBootstrapInstaller
import com.oryn.terminal.ui.OrynTerminalPalette
import com.oryn.terminal.ui.OrynTerminalViewClient
import com.oryn.terminal.ui.TerminalPane
import com.termux.terminal.TerminalSession
import java.io.IOException

private val Obsidian = Color(0xFF0B0F19)
private val SurfaceDark = Color(0xFF111827)
private val Elevated = Color(0xFF1E293B)
private val TextPrimary = Color(0xFFE2E8F0)
private val TextMuted = Color(0xFF94A3B8)
private val Cyan = Color(0xFF00F0FF)
private val Purple = Color(0xFF8338EC)
private val Warning = Color(0xFFFFC857)
private val Success = Color(0xFF4DFF9A)

class MainActivity : ComponentActivity() {
    private var installProgress by mutableIntStateOf(0)
    private var isInstalling by mutableStateOf(false)
    private var statusMessage by mutableStateOf("Checking Oryn's private environment…")
    private var technicalErrorDetails by mutableStateOf<String?>(null)
    private var showTechnicalDetails by mutableStateOf(false)
    private var bootstrapInstalled by mutableStateOf(false)
    private var runtimeInstalled by mutableStateOf(false)
    private var runtimeVerified by mutableStateOf(false)
    private var isVerifyingRuntime by mutableStateOf(false)
    private val terminalState: TerminalSessionViewModel by lazy {
        ViewModelProvider(this)[TerminalSessionViewModel::class.java]
    }
    private var installer: TermuxBootstrapInstaller? = null
    private var runtimeInstaller: ProotRuntimeInstaller? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        try {
            installer = TermuxBootstrapInstaller(this)
            runtimeInstaller = ProotRuntimeInstaller(this)
            val manager = TerminalSessionManager(this)
            statusMessage = manager.getReadinessMessage()
            bootstrapInstalled = manager.getPaths().isBootstrapInstalled()
            runtimeInstalled = manager.isRuntimeInstalled()
        } catch (error: IOException) {
            technicalErrorDetails = error.stackTraceToString()
            statusMessage = "Oryn couldn't verify its private environment."
        }

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Obsidian,
                    surface = SurfaceDark,
                    primary = Cyan,
                    secondary = Purple,
                    onBackground = TextPrimary,
                    onSurface = TextPrimary
                )
            ) {
                OrynTerminalApp(
                    progress = installProgress,
                    installing = isInstalling,
                    status = terminalState.sessionFailureMessage ?: terminalState.sessionStatus.ifBlank { statusMessage },
                    runtimeVerified = runtimeVerified && terminalState.sessionFailureMessage == null,
                    verifyingRuntime = isVerifyingRuntime,
                    technicalErrorDetails = technicalErrorDetails,
                    showTechnicalDetails = showTechnicalDetails,
                    onToggleTechnicalDetails = {
                        showTechnicalDetails = !showTechnicalDetails
                    },
                    onCopyTechnicalDetails = {
                        val details = technicalErrorDetails
                        if (!details.isNullOrBlank()) {
                            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Oryn technical details", details))
                            Toast.makeText(this, "Technical details copied", Toast.LENGTH_SHORT).show()
                        }
                    },
                    bootstrapInstalled = bootstrapInstalled,
                    runtimeInstalled = runtimeInstalled,
                    session = terminalState.activeSession,
                    terminalTitle = terminalState.terminalTitle,
                    sessionClient = terminalState.sessionClient,
                    viewClient = terminalState.viewClient,
                    sessionFailureMessage = terminalState.sessionFailureMessage,
                    onInstall = ::handlePrimaryAction,
                    onLaunch = ::launchBash,
                    onClose = ::closeBash
                )
            }
        }
        // Re-verify packaged hashes and refresh private SONAME copies after APK updates.
        if (runtimeInstalled) installProotRuntime()
    }

    private fun handlePrimaryAction() {
        if (runtimeInstalled) verifyRuntime() else installEnvironment()
    }

    private fun verifyRuntime() {
        if (isVerifyingRuntime || !runtimeInstalled) return
        terminalState.clearSessionFailure()
        isVerifyingRuntime = true
        runtimeVerified = false
        statusMessage = "Checking Bash startup on this device…"
        lifecycleScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    TerminalSessionManager(applicationContext).verifyBash()
                }
            }
            isVerifyingRuntime = false
            result.onSuccess {
                runtimeVerified = true
                technicalErrorDetails = null
                showTechnicalDetails = false
                statusMessage = "Bash startup verified on this device."
            }.onFailure { error ->
                runtimeVerified = false
                technicalErrorDetails = error.stackTraceToString()
                showTechnicalDetails = false
                statusMessage = "Runtime is installed, but Bash couldn't start. Review the diagnostics and retry."
            }
        }
    }

    private fun installEnvironment() {
        if (isInstalling) return
        if (bootstrapInstalled) {
            installProotRuntime()
            return
        }

        val activeInstaller = installer ?: run {
            statusMessage = "Installer is unavailable. Restart Oryn and try again."
            return
        }
        technicalErrorDetails = null
        showTechnicalDetails = false
        isInstalling = true
        installProgress = 0
        statusMessage = "Preparing Oryn's private environment…"

        activeInstaller.install(object : TermuxBootstrapInstaller.Listener {
            override fun onProgress(stage: String, percent: Int) {
                runOnUiThread {
                    installProgress = percent.coerceIn(0, 100)
                    statusMessage = stage
                }
            }

            override fun onComplete(success: Boolean, message: String) {
                runOnUiThread {
                    bootstrapInstalled = success
                    statusMessage = message
                    if (success) {
                        installProotRuntime()
                    } else {
                        isInstalling = false
                        technicalErrorDetails = activeInstaller.getLastErrorDetails()
                        showTechnicalDetails = false
                        refreshReadiness()
                        statusMessage = message
                    }
                }
            }
        })
    }

    private fun installProotRuntime() {
        val activeInstaller = runtimeInstaller ?: run {
            isInstalling = false
            statusMessage = "Proot installer is unavailable. Restart Oryn and try again."
            return
        }
        technicalErrorDetails = null
        showTechnicalDetails = false
        isInstalling = true
        installProgress = 0
        statusMessage = "Installing Oryn's verified ARM64 proot runtime…"

        activeInstaller.install(object : ProotRuntimeInstaller.Listener {
            override fun onProgress(stage: String, percent: Int) {
                runOnUiThread {
                    installProgress = percent.coerceIn(0, 100)
                    statusMessage = stage
                }
            }

            override fun onComplete(success: Boolean, message: String) {
                runOnUiThread {
                    isInstalling = false
                    refreshReadiness()
                    if (!success) {
                        technicalErrorDetails = activeInstaller.getLastErrorDetails()
                        showTechnicalDetails = false
                        statusMessage = message
                    } else if (runtimeInstalled) {
                        verifyRuntime()
                    }
                }
            }
        })
    }

    private fun launchBash() {
        if (terminalState.activeSession != null) return
        try {
            OrynTerminalPalette.apply()
            val manager = TerminalSessionManager(this)
            val newSessionClient = OrynTerminalSessionClient(applicationContext, terminalState)
            val newViewClient = OrynTerminalViewClient(applicationContext)
            val newSession = manager.startBash("Bash", newSessionClient)
            terminalState.attachSession(newSession, newSessionClient, newViewClient)
            statusMessage = "Starting private Bash session…"
        } catch (error: Exception) {
            runtimeVerified = false
            technicalErrorDetails = error.stackTraceToString()
            showTechnicalDetails = false
            statusMessage = "Oryn couldn't start Bash. Check the technical details."
        }
    }

    private fun closeBash() {
        terminalState.closeSession()
        statusMessage = "Bash session closed."
    }

    private fun refreshReadiness() {
        try {
            val manager = TerminalSessionManager(this)
            bootstrapInstalled = manager.getPaths().isBootstrapInstalled()
            runtimeInstalled = manager.isRuntimeInstalled()
            runtimeVerified = false
            if (runtimeInstalled) {
                statusMessage = "Runtime installed · Bash startup has not been verified on this device."
            } else {
                statusMessage = manager.getReadinessMessage()
            }
        } catch (error: IOException) {
            runtimeInstalled = false
            technicalErrorDetails = error.stackTraceToString()
            showTechnicalDetails = false
            statusMessage = "Oryn couldn't validate its private environment."
        }
    }

}

@Composable
private fun OrynTerminalApp(
    progress: Int,
    installing: Boolean,
    status: String,
    runtimeVerified: Boolean,
    verifyingRuntime: Boolean,
    technicalErrorDetails: String?,
    showTechnicalDetails: Boolean,
    onToggleTechnicalDetails: () -> Unit,
    onCopyTechnicalDetails: () -> Unit,
    bootstrapInstalled: Boolean,
    runtimeInstalled: Boolean,
    session: TerminalSession?,
    terminalTitle: String,
    sessionClient: OrynTerminalSessionClient?,
    viewClient: OrynTerminalViewClient?,
    sessionFailureMessage: String?,
    onInstall: () -> Unit,
    onLaunch: () -> Unit,
    onClose: () -> Unit
) {
    val stateLabel = when {
        runtimeVerified -> "BASH VERIFIED"
        verifyingRuntime -> "CHECKING BASH"
        runtimeInstalled -> "NOT VERIFIED"
        bootstrapInstalled -> "BOOTSTRAP INSTALLED"
        else -> "SETUP"
    }
    val stateColor = when {
        runtimeVerified -> Success
        verifyingRuntime || runtimeInstalled || bootstrapInstalled -> Warning
        else -> Cyan
    }

    Surface(
        modifier = Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding(),
        color = Obsidian
    ) {
        if (session != null && sessionClient != null && viewClient != null) {
            TerminalPane(
                session = session,
                title = terminalTitle,
                sessionClient = sessionClient,
                viewClient = viewClient,
                sessionFailureMessage = sessionFailureMessage,
                onClose = onClose
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Elevated, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("\\$", color = Cyan, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("ORYN", color = TextPrimary, fontSize = 18.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Text("TERMINAL", color = TextMuted, fontSize = 10.sp,
                            letterSpacing = 2.5.sp)
                    }
                    Spacer(Modifier.weight(1f))
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Your command line.\nYour own space.",
                        color = TextPrimary, fontSize = 30.sp, lineHeight = 36.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "A standalone Android terminal that lives alongside Termux — without touching its data.",
                        color = TextMuted, fontSize = 14.sp, lineHeight = 21.sp
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SurfaceDark,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("PRIVATE ENVIRONMENT", color = Cyan, fontSize = 10.sp,
                                letterSpacing = 1.3.sp)
                            Spacer(Modifier.weight(1f))
                            Text(
                                stateLabel,
                                color = stateColor,
                                fontSize = 10.sp, letterSpacing = 0.8.sp
                            )
                        }
                        Text(
                            status,
                            color = if (technicalErrorDetails != null || sessionFailureMessage != null) Color(0xFFFF8AA5) else TextPrimary,
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )
                        if (technicalErrorDetails != null) {
                            TextButton(onClick = onToggleTechnicalDetails) {
                                Text(
                                    if (showTechnicalDetails) "Hide technical details"
                                    else "Show technical details",
                                    color = Cyan,
                                    fontSize = 12.sp
                                )
                            }
                            if (showTechnicalDetails) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth().height(132.dp),
                                    color = Obsidian,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(10.dp)
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        Text(
                                            technicalErrorDetails,
                                            color = TextMuted,
                                            fontSize = 10.sp,
                                            lineHeight = 14.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                                TextButton(onClick = onCopyTechnicalDetails) {
                                    Text("Copy technical details", color = TextMuted, fontSize = 12.sp)
                                }
                            }
                        }
                        if (installing) {
                            Box(
                                Modifier.fillMaxWidth().height(5.dp)
                                    .background(Elevated, CircleShape)
                            ) {
                                Box(
                                    Modifier.fillMaxWidth(progress / 100f).height(5.dp)
                                        .background(Cyan, CircleShape)
                                )
                            }
                            Text("$progress%", color = Cyan, fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace)
                        }
                        if (!runtimeVerified) {
                            Button(
                                onClick = onInstall,
                                enabled = !installing && !verifyingRuntime,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Cyan,
                                    contentColor = Obsidian,
                                    disabledContainerColor = Elevated,
                                    disabledContentColor = TextMuted
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    when {
                                        installing -> "Installing environment…"
                                        verifyingRuntime -> "Checking Bash…"
                                        runtimeInstalled -> "Verify Bash startup"
                                        technicalErrorDetails != null && !bootstrapInstalled -> "Clean & retry installation"
                                        technicalErrorDetails != null -> "Retry runtime setup"
                                        bootstrapInstalled -> "Verify packaged runtime"
                                        else -> "Install private environment"
                                    },
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        if (runtimeVerified) {
                            Button(
                                onClick = onLaunch,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Purple,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(13.dp)
                            ) {
                                Text("Launch Bash", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }


            }
        }
    }
}
