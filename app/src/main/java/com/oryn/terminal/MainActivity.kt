package com.oryn.terminal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.oryn.terminal.session.TerminalSessionManager
import com.oryn.terminal.termux.ProotRuntimeInstaller
import com.oryn.terminal.termux.TermuxBootstrapInstaller
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
    private var bootstrapInstalled by mutableStateOf(false)
    private var shellReady by mutableStateOf(false)
    private var installer: TermuxBootstrapInstaller? = null
    private var runtimeInstaller: ProotRuntimeInstaller? = null

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
            statusMessage = "Environment check failed: ${error.message}"
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
                    bootstrapInstalled = bootstrapInstalled,
                    shellReady = shellReady,
                    onInstall = ::installEnvironment
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
                        refreshReadiness()
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
                    statusMessage = message
                    refreshReadiness()
                }
            }
        })
    }

    private fun refreshReadiness() {
        try {
            val manager = TerminalSessionManager(this)
            bootstrapInstalled = manager.getPaths().isBootstrapInstalled()
            shellReady = manager.isReady()
            if (shellReady) {
                statusMessage = "Private proot runtime installed. Bash startup still requires on-device validation."
            } else if (bootstrapInstalled && statusMessage.isBlank()) {
                statusMessage = manager.getReadinessMessage()
            }
        } catch (error: IOException) {
            shellReady = false
            statusMessage = "Environment validation failed: ${error.message}"
        }
    }
}

@Composable
private fun OrynTerminalApp(
    progress: Int,
    installing: Boolean,
    status: String,
    bootstrapInstalled: Boolean,
    shellReady: Boolean,
    onInstall: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = Obsidian) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                        color = TextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
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
                                bootstrapInstalled -> "Install proot runtime"
                                else -> "Install private environment"
                            },
                            fontWeight = FontWeight.SemiBold
                        )
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
                        if (shellReady) "Oryn's private proot runtime is installed. Bash startup still needs validation on an Android device."
                        else if (bootstrapInstalled) "Bootstrap is installed. Install Oryn's private ARM64 proot runtime to unlock Bash."
                        else "Bash is locked until Oryn's private bootstrap and proot runtime are installed.",
                        color = TextPrimary, fontSize = 13.sp, lineHeight = 19.sp
                    )
                    Text(
                        "No fallback to the system shell or your existing Termux installation is allowed.",
                        color = TextMuted, fontSize = 11.sp, lineHeight = 16.sp
                    )
                }
            }

            Spacer(Modifier.weight(1f))
            Text(
                "NEON OBSIDIAN  ·  EARLY DEVELOPMENT",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = TextMuted, fontSize = 9.sp, letterSpacing = 1.2.sp
            )
        }
    }
}
