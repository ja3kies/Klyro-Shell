package com.oryn.terminal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.layout.padding
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
private val Success = Color(0xFF00FF66)

class MainActivity : ComponentActivity() {
    private var installProgress by mutableIntStateOf(0)
    private var isInstalling by mutableStateOf(false)
    private var statusMessage by mutableStateOf("Checking Oryn's private environment…")
    private var technicalErrorDetails by mutableStateOf<String?>(null)
    private var showTechnicalDetails by mutableStateOf(false)
    private var bootstrapInstalled by mutableStateOf(false)
    private var shellReady by mutableStateOf(false)
    private var activeSession by mutableStateOf<TerminalSession?>(null)
    private var terminalTitle by mutableStateOf("Bash")
    private var installer: TermuxBootstrapInstaller? = null
    private var runtimeInstaller: ProotRuntimeInstaller? = null
    private var sessionClient: OrynTerminalSessionClient? = null
    private var viewClient: OrynTerminalViewClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            installer = TermuxBootstrapInstaller(this)
            runtimeInstaller = ProotRuntimeInstaller(this)
            val manager = TerminalSessionManager(this)
            statusMessage = manager.getReadinessMessage()
            bootstrapInstalled = manager.getPaths().isBootstrapInstalled()
            shellReady = manager.isReady()
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
                    status = statusMessage,
                    technicalErrorDetails = technicalErrorDetails,
                    showTechnicalDetails = showTechnicalDetails,
                    onToggleTechnicalDetails = {
                        showTechnicalDetails = !showTechnicalDetails
                    },
                    bootstrapInstalled = bootstrapInstalled,
                    shellReady = shellReady,
                    session = activeSession,
                    terminalTitle = terminalTitle,
                    sessionClient = sessionClient,
                    viewClient = viewClient,
                    onInstall = ::installEnvironment,
                    onLaunch = ::launchBash,
                    onClose = ::closeBash
                )
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
                    }
                }
            }
        })
    }

    private fun launchBash() {
        if (activeSession != null) return
        try {
            OrynTerminalPalette.apply()
            val manager = TerminalSessionManager(this)
            val newSessionClient = OrynTerminalSessionClient(
                this,
                object : OrynTerminalSessionClient.Listener {
                    override fun onTitleChanged(title: String?) {
                        runOnUiThread {
                            terminalTitle = title?.takeIf { it.isNotBlank() } ?: "Bash"
                        }
                    }

                    override fun onSessionFinished(exitStatus: Int) {
                        runOnUiThread {
                            statusMessage = "Bash exited with status $exitStatus."
                        }
                    }

                    override fun onShellPid(pid: Int) {
                        runOnUiThread {
                            statusMessage = "Bash session started (PID $pid)."
                        }
                    }
                }
            )
            val newViewClient = OrynTerminalViewClient(this)
            val newSession = manager.startBash("Bash", newSessionClient)
            sessionClient = newSessionClient
            viewClient = newViewClient
            terminalTitle = "Bash"
            activeSession = newSession
            statusMessage = "Starting private Bash session…"
        } catch (error: Exception) {
            technicalErrorDetails = error.stackTraceToString()
            showTechnicalDetails = false
            statusMessage = "Oryn couldn't start Bash. Check the technical details."
        }
    }

    private fun closeBash() {
        activeSession?.finishIfRunning()
        activeSession = null
        sessionClient?.attachView(null)
        viewClient?.attachView(null)
        sessionClient = null
        viewClient = null
        terminalTitle = "Bash"
        statusMessage = "Bash session closed."
    }

    private fun refreshReadiness() {
        try {
            val manager = TerminalSessionManager(this)
            bootstrapInstalled = manager.getPaths().isBootstrapInstalled()
            shellReady = manager.isReady()
            if (shellReady) {
                statusMessage = "Private proot runtime installed. Bash startup still requires on-device validation."
            } else {
                statusMessage = manager.getReadinessMessage()
            }
        } catch (error: IOException) {
            shellReady = false
            technicalErrorDetails = error.stackTraceToString()
            showTechnicalDetails = false
            statusMessage = "Oryn couldn't validate its private environment."
        }
    }

    override fun onDestroy() {
        if (isFinishing) closeBash()
        super.onDestroy()
    }
}

@Composable
private fun OrynTerminalApp(
    progress: Int,
    installing: Boolean,
    status: String,
    technicalErrorDetails: String?,
    showTechnicalDetails: Boolean,
    onToggleTechnicalDetails: () -> Unit,
    bootstrapInstalled: Boolean,
    shellReady: Boolean,
    session: TerminalSession?,
    terminalTitle: String,
    sessionClient: OrynTerminalSessionClient?,
    viewClient: OrynTerminalViewClient?,
    onInstall: () -> Unit,
    onLaunch: () -> Unit,
    onClose: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = Obsidian) {
        if (session != null && sessionClient != null && viewClient != null) {
            TerminalPane(
                session = session,
                title = terminalTitle,
                sessionClient = sessionClient,
                viewClient = viewClient,
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
                        Text("⌘", color = Cyan, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("ORYN", color = TextPrimary, fontSize = 18.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Text("TERMINAL", color = TextMuted, fontSize = 10.sp,
                            letterSpacing = 2.5.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier.size(9.dp).background(
                            if (shellReady) Success else if (bootstrapInstalled) Purple else Cyan,
                            CircleShape
                        )
                    )
                    Text(
                        if (shellReady) "RUNTIME READY" else "SETUP",
                        color = TextMuted, fontSize = 10.sp, letterSpacing = 1.sp
                    )
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
                                if (bootstrapInstalled) "BOOTSTRAP OK" else "NOT INSTALLED",
                                color = if (bootstrapInstalled) Success else TextMuted,
                                fontSize = 9.sp, letterSpacing = 0.8.sp
                            )
                        }
                        Text(
                            status,
                            color = if (technicalErrorDetails != null) Color(0xFFFF8AA5) else TextPrimary,
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )
                        if (technicalErrorDetails != null) {
                            TextButton(onClick = onToggleTechnicalDetails) {
                                Text(
                                    if (showTechnicalDetails) "Hide technical details"
                                    else "View technical details",
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
                        Button(
                            onClick = onInstall,
                            enabled = !installing && !shellReady,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Cyan,
                                contentColor = Obsidian,
                                disabledContainerColor = Elevated,
                                disabledContentColor = TextMuted
                            ),
                            shape = RoundedCornerShape(13.dp)
                        ) {
                            Text(
                                when {
                                    installing -> "Installing environment…"
                                    shellReady -> "Runtime installed"
                                    technicalErrorDetails != null -> "Retry installation"
                                    bootstrapInstalled -> "Install proot runtime"
                                    else -> "Install private environment"
                                },
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (shellReady) {
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

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Elevated.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("SHELL STATUS", color = Purple, fontSize = 10.sp,
                            letterSpacing = 1.4.sp)
                        Text(
                            if (shellReady) "Oryn's private proot runtime is installed. Launch Bash to validate the shell on this device."
                            else if (bootstrapInstalled) "Bootstrap is installed. Install Oryn's private ARM64 proot runtime to unlock Bash."
                            else "Your private Bash environment will be ready after setup.",
                            color = TextPrimary, fontSize = 13.sp, lineHeight = 19.sp
                        )
                        Text(
                            "Oryn keeps its shell and packages separate from your existing Termux installation.",
                            color = TextMuted, fontSize = 11.sp, lineHeight = 16.sp
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    "NEON OBSIDIAN  ·  EARLY DEVELOPMENT",
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = TextMuted, fontSize = 9.sp, letterSpacing = 1.2.sp
                )
            }
        }
    }
}
