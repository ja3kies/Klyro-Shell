package com.oryn.terminal

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContracts
import com.oryn.terminal.core.OrynPaths
import java.io.File
import java.io.FileOutputStream
import java.io.FileInputStream
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.remember
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
    private var statusMessage by mutableStateOf("Checking Klyro's private environment…")
    private var technicalErrorDetails by mutableStateOf<String?>(null)
    private var showTechnicalDetails by mutableStateOf(false)
    private var bootstrapInstalled by mutableStateOf(false)
    private var runtimeInstalled by mutableStateOf(false)
    private var runtimeVerified by mutableStateOf(false)
    private var runtimeHasWarnings by mutableStateOf(false)
    private var bashStarted by mutableStateOf(false)
    private var isVerifyingRuntime by mutableStateOf(false)
    private val terminalState: TerminalSessionViewModel by lazy {
        ViewModelProvider(this)[TerminalSessionViewModel::class.java]
    }
    private val storageFolderLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri == null) return@registerForActivityResult
            try {
                contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                getSharedPreferences("klyro_storage", MODE_PRIVATE)
                    .edit().putString("tree_uri", uri.toString()).apply()
                Toast.makeText(this, "Storage folder authorized for Klyro", Toast.LENGTH_LONG).show()
            } catch (error: SecurityException) {
                Toast.makeText(this, "Storage permission denied: " + error.message, Toast.LENGTH_LONG).show()
            }
        }

    private fun importFromAuthorizedFolder() {
        val raw = getSharedPreferences("klyro_storage", MODE_PRIVATE).getString("tree_uri", null)
        if (raw == null) {
            Toast.makeText(this, "Choose a storage folder first", Toast.LENGTH_LONG).show()
            chooseStorageFolder()
            return
        }
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val tree = Uri.parse(raw)
                    val authorized = contentResolver.persistedUriPermissions.any { permission ->
                        permission.uri == tree && permission.isReadPermission
                    }
                    if (!authorized) {
                        throw SecurityException("Storage folder permission expired; authorize the folder again")
                    }
                    val documentId = DocumentsContract.getTreeDocumentId(tree)
                    val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, documentId)
                    val paths = OrynPaths(applicationContext.filesDir,
                        File(applicationContext.applicationInfo.nativeLibraryDir))
                    val destination = File(paths.home, "storage-imports")
                    if (!destination.isDirectory && !destination.mkdirs()) throw IOException("Cannot create destination")
                    var imported = 0
                    contentResolver.query(children, arrayOf(
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_MIME_TYPE,
                        DocumentsContract.Document.COLUMN_SIZE
                    ), null, null, null)?.use { cursor ->
                        while (cursor.moveToNext() && imported < 30) {
                            val mime = cursor.getString(2) ?: continue
                            if (mime == DocumentsContract.Document.MIME_TYPE_DIR) continue
                            val size = if (cursor.isNull(3)) -1L else cursor.getLong(3)
                            if (size > 32L * 1024 * 1024) continue
                            val name = (cursor.getString(1) ?: "file")
                                .replace(Regex("[^a-zA-Z0-9._-]"), "_").trimStart('.')
                                .take(80).ifBlank { "file" }
                            val target = generateSequence(0) { it + 1 }
                                .map { index -> File(destination, if (index == 0) name else "${index}_${name}") }
                                .first { !it.exists() }
                            val item = DocumentsContract.buildDocumentUriUsingTree(tree, cursor.getString(0))
                            val temporary = File.createTempFile(".klyro-", ".tmp", destination)
                            try {
                                contentResolver.openInputStream(item)?.use { input ->
                                    FileOutputStream(temporary).use { output ->
                                        val buffer = ByteArray(8192)
                                        var copied = 0L
                                        while (true) {
                                            val count = input.read(buffer)
                                            if (count < 0) break
                                            copied += count
                                            if (copied > 32L * 1024 * 1024) throw IOException("File exceeds 32 MiB")
                                            output.write(buffer, 0, count)
                                        }
                                    }
                                } ?: continue
                                if (!temporary.renameTo(target)) throw IOException("Cannot save file")
                                imported++
                            } finally {
                                if (temporary.exists()) temporary.delete()
                            }
                        }
                    } ?: throw IOException("Cannot read authorized folder")
                    imported
                }
            }
            Toast.makeText(this@MainActivity,
                result.fold({ "Imported $it files to ~/storage-imports" },
                    { "Storage import failed: " + (it.message ?: "unknown error") }),
                Toast.LENGTH_LONG).show()
        }
    }

    private fun chooseStorageFolder() {
        storageFolderLauncher.launch(null)
    }

    private val importFileLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@registerForActivityResult
            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val paths = OrynPaths(
                            applicationContext.filesDir,
                            File(applicationContext.applicationInfo.nativeLibraryDir)
                        )
                        val destinationDir = File(paths.home, "imports")
                        if (!destinationDir.isDirectory && !destinationDir.mkdirs()) {
                            throw IOException("Cannot create imports directory")
                        }
                        var sourceName = "imported-file"
                        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                sourceName = cursor.getString(0) ?: sourceName
                            }
                        }
                        val safeName = sourceName.substringAfterLast('/').substringAfterLast('\\')
                            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
                            .trimStart('.').take(80).ifBlank { "imported-file" }
                        val target = generateSequence(0) { it + 1 }
                            .map { n -> File(destinationDir, if (n == 0) safeName else n.toString() + "_" + safeName) }
                            .first { !it.exists() }
                        val temporary = File.createTempFile(".klyro-import-", ".tmp", destinationDir)
                        try {
                            contentResolver.openInputStream(uri)?.use { input ->
                                FileOutputStream(temporary).use { output ->
                                    val buffer = ByteArray(8192)
                                    var total = 0L
                                    while (true) {
                                        val count = input.read(buffer)
                                        if (count < 0) break
                                        total += count
                                        if (total > 32L * 1024 * 1024) {
                                            throw IOException("Import limited to 32 MiB")
                                        }
                                        output.write(buffer, 0, count)
                                    }
                                }
                            } ?: throw IOException("Cannot open selected file")
                            if (!temporary.renameTo(target)) throw IOException("Cannot finalize import")
                        } finally {
                            if (temporary.exists()) temporary.delete()
                        }
                        target.name
                    }
                }
                result.onSuccess {
                    Toast.makeText(this@MainActivity, "Imported: ~/imports/" + it, Toast.LENGTH_LONG).show()
                }.onFailure {
                    Toast.makeText(this@MainActivity, "Import failed: " + (it.message ?: "unknown error"), Toast.LENGTH_LONG).show()
                }
            }
        }

    private val exportFileLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
            if (uri == null) return@registerForActivityResult
            lifecycleScope.launch {
                val outcome = withContext(Dispatchers.IO) {
                    runCatching {
                        val root = OrynPaths(
                            applicationContext.filesDir,
                            File(applicationContext.applicationInfo.nativeLibraryDir)
                        )
                        val source = File(root.home, "imports/klyro-export.txt")
                        if (!source.isFile) throw IOException("Create ~/imports/klyro-export.txt first")
                        val stream = contentResolver.openOutputStream(uri, "w")
                            ?: throw IOException("Unable to open destination")
                        stream.use { output -> FileInputStream(source).use { it.copyTo(output) } }
                    }
                }
                val message = outcome.fold(
                    onSuccess = { "Export completed" },
                    onFailure = { "Export failed: " + (it.message ?: "unknown error") }
                )
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
            }
        }

    private fun chooseFileToExport() {
        exportFileLauncher.launch("klyro-export.txt")
    }

    private fun chooseFileToImport() {
        importFileLauncher.launch(arrayOf("*/*"))
    }

    private var installer: TermuxBootstrapInstaller? = null
    private var runtimeInstaller: ProotRuntimeInstaller? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        // The bars are transparent; draw a matching fallback behind system insets.
        // The Compose root paints the actual active-screen color edge to edge.
        window.decorView.setBackgroundColor(android.graphics.Color.rgb(24, 37, 40))

        try {
            installer = TermuxBootstrapInstaller(this)
            runtimeInstaller = ProotRuntimeInstaller(this)
            val manager = TerminalSessionManager(this)
            statusMessage = manager.getReadinessMessage()
            bootstrapInstalled = manager.getPaths().isBootstrapInstalled()
            runtimeInstalled = manager.isRuntimeInstalled()
            if (bootstrapInstalled) installer?.migrateShellProfile()
        } catch (error: IOException) {
            technicalErrorDetails = error.stackTraceToString()
            statusMessage = "Klyro couldn't verify its private environment."
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
                    runtimeHasWarnings = runtimeHasWarnings,
                    bashStarted = bashStarted,
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
                            clipboard.setPrimaryClip(ClipData.newPlainText("Klyro technical details", details))
                            Toast.makeText(this, "Technical details copied", Toast.LENGTH_SHORT).show()
                        }
                    },
                    bootstrapInstalled = bootstrapInstalled,
                    runtimeInstalled = runtimeInstalled,
                    session = terminalState.activeSession,
                    tabs = terminalState.sessions.map { it.id to (it.customTitle ?: it.title) },
                    activeTabId = terminalState.activeTabId,
                    onSelectTab = terminalState::selectTab,
                    onRenameTab = terminalState::renameTab,
                    onCloseTab = terminalState::closeTab,
                    onNewTab = ::launchBash,
                    onImportFile = ::chooseFileToImport,
                    onExportFile = ::chooseFileToExport,
                    onSetupStorage = ::chooseStorageFolder,
                    onImportStorageFolder = ::importFromAuthorizedFolder,
                    terminalTitle = terminalState.terminalTitle,
                    sessionClient = terminalState.sessionClient,
                    viewClient = terminalState.viewClient,
                    sessionFailureMessage = terminalState.sessionFailureMessage,
                    onInstall = ::handlePrimaryAction,
                    onLaunch = ::launchBash,
                    onClose = ::closeBash,
                    onRecheck = ::verifyRuntime
                )
            }
        }
        // Re-verify packaged hashes and refresh private SONAME copies after APK updates.
        if (bootstrapInstalled) installProotRuntime()
    }

    private fun handlePrimaryAction() {
        if (runtimeInstalled) verifyRuntime() else installEnvironment()
    }

    private fun verifyRuntime() {
        if (isVerifyingRuntime || !runtimeInstalled) return
        terminalState.clearSessionFailure()
        isVerifyingRuntime = true
        runtimeVerified = false
        runtimeHasWarnings = false
        bashStarted = false
        statusMessage = "Checking Bash, UID and package manager…"
        lifecycleScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    TerminalSessionManager(applicationContext).verifyBash()
                }
            }
            isVerifyingRuntime = false
            result.onSuccess { verification ->
                bashStarted = verification.bashStarted
                runtimeVerified = verification.isEnvironmentReady()
                // Termux bootstrap may emit a harmless mandoc database warning.
                // Suppress only this exact warning when all runtime checks passed.
                val warningLines = verification.diagnostics.lines()
                    .map { it.trim() }
                    .filter { it.startsWith("WARNING", ignoreCase = true) ||
                        it.startsWith("update-alternatives: warning:", ignoreCase = true) }
                val onlyMissingMandoc = warningLines.isNotEmpty() &&
                    warningLines.all { it.contains("makewhatis") && it.contains("mandoc") }
                runtimeHasWarnings = verification.hasWarnings &&
                    !(runtimeVerified && onlyMissingMandoc)
                val cleanStderr = !runtimeHasWarnings
                technicalErrorDetails = if (runtimeVerified && cleanStderr) null else verification.diagnostics
                showTechnicalDetails = false
                statusMessage = when {
                    runtimeVerified && cleanStderr -> "Environment ready · Bash, UID, APT and pkg verified."
                    runtimeVerified -> "Ready with warnings · Bash, UID, APT and pkg passed. Open the technical details to see the warnings."
                    bashStarted -> "Bash starts, but the environment is incomplete. Review diagnostics."
                    else -> "Bash startup failed. Review diagnostics."
                }
            }.onFailure { error ->
                runtimeVerified = false
                runtimeHasWarnings = false
                bashStarted = false
                technicalErrorDetails = error.stackTraceToString()
                showTechnicalDetails = false
                statusMessage = "Klyro couldn't complete the environment check. Review diagnostics."
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
            statusMessage = "Installer is unavailable. Restart Klyro and try again."
            return
        }
        technicalErrorDetails = null
        showTechnicalDetails = false
        bashStarted = false
        runtimeVerified = false
        runtimeHasWarnings = false
        isInstalling = true
        installProgress = 0
        statusMessage = "Preparing Klyro's private environment…"

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
            statusMessage = "Proot installer is unavailable. Restart Klyro and try again."
            return
        }
        technicalErrorDetails = null
        showTechnicalDetails = false
        bashStarted = false
        runtimeVerified = false
        runtimeHasWarnings = false
        isInstalling = true
        installProgress = 0
        statusMessage = "Installing Klyro's verified ARM64 PRoot runtime…"

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
                        runtimeInstalled = false
                        runtimeVerified = false
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
        if (terminalState.sessions.size >= 8) return
        try {
            OrynTerminalPalette.apply()
            val manager = TerminalSessionManager(this)
            val tabId = terminalState.reserveTabId()
            val newSessionClient = OrynTerminalSessionClient(applicationContext, terminalState.listenerFor(tabId))
            val newViewClient = OrynTerminalViewClient(applicationContext)
            val newSession = manager.startBash("Bash", newSessionClient)
            terminalState.attachSession(newSession, newSessionClient, newViewClient, tabId)
            statusMessage = "Starting private Bash session…"
        } catch (error: Exception) {
            runtimeVerified = false
            bashStarted = false
            technicalErrorDetails = error.stackTraceToString()
            showTechnicalDetails = false
            statusMessage = "Klyro couldn't start Bash. Check the technical details."
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
            runtimeHasWarnings = false
            bashStarted = false
            if (runtimeInstalled) {
                statusMessage = "Runtime installed · Bash startup has not been verified on this device."
            } else {
                statusMessage = manager.getReadinessMessage()
            }
        } catch (error: IOException) {
            runtimeInstalled = false
            technicalErrorDetails = error.stackTraceToString()
            showTechnicalDetails = false
            statusMessage = "Klyro couldn't validate its private environment."
        }
    }

}

@Composable
private fun OrynTerminalApp(
    progress: Int,
    installing: Boolean,
    status: String,
    runtimeVerified: Boolean,
    runtimeHasWarnings: Boolean,
    bashStarted: Boolean,
    verifyingRuntime: Boolean,
    technicalErrorDetails: String?,
    showTechnicalDetails: Boolean,
    onToggleTechnicalDetails: () -> Unit,
    onCopyTechnicalDetails: () -> Unit,
    bootstrapInstalled: Boolean,
    runtimeInstalled: Boolean,
    session: TerminalSession?,
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
    terminalTitle: String,
    sessionClient: OrynTerminalSessionClient?,
    viewClient: OrynTerminalViewClient?,
    sessionFailureMessage: String?,
    onInstall: () -> Unit,
    onLaunch: () -> Unit,
    onClose: () -> Unit,
    onRecheck: () -> Unit
) {
    val warningLines = remember(technicalErrorDetails) {
        technicalErrorDetails.orEmpty().lines()
            .map { it.trim() }
            .filter { it.startsWith("WARNING", ignoreCase = true) }
            .distinct()
    }
    val stateLabel = when {
        runtimeVerified && runtimeHasWarnings -> "READY WITH WARNINGS"
        runtimeVerified -> "ENVIRONMENT READY"
        verifyingRuntime -> "CHECKING ENVIRONMENT"
        bashStarted -> "BASH STARTS"
        runtimeInstalled -> "NOT VERIFIED"
        bootstrapInstalled -> "RUNTIME INSTALLED"
        else -> "SETUP"
    }
    val stateColor = when {
        runtimeVerified && runtimeHasWarnings -> Warning
        runtimeVerified -> Success
        verifyingRuntime || bashStarted || runtimeInstalled || bootstrapInstalled -> Warning
        else -> Cyan
    }

    val isTerminalVisible = session != null && sessionClient != null && viewClient != null
    val screenChrome = if (isTerminalVisible) Color(0xFF182528) else Obsidian
    Surface(
        // Paint before insets; the transparent status bar must show Klyro, not
        // Android's default gray window background.
        modifier = Modifier.fillMaxSize()
            .background(screenChrome)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding(),
        color = screenChrome
    ) {
        if (session != null && sessionClient != null && viewClient != null) {
            TerminalPane(
                session = session,
                title = terminalTitle,
                tabs = tabs,
                activeTabId = activeTabId,
                onSelectTab = onSelectTab,
                onRenameTab = onRenameTab,
                onCloseTab = onCloseTab,
                onNewTab = onNewTab,
                onImportFile = onImportFile,
                onExportFile = onExportFile,
                onSetupStorage = onSetupStorage,
                onImportStorageFolder = onImportStorageFolder,
                sessionClient = sessionClient,
                viewClient = viewClient,
                sessionFailureMessage = sessionFailureMessage,
                runtimeHasWarnings = runtimeHasWarnings,
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
                        Text("KLYRO", color = TextPrimary, fontSize = 18.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Text("SHELL", color = TextMuted, fontSize = 11.sp,
                            letterSpacing = 2.5.sp)
                    }
                    Spacer(Modifier.weight(1f))
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Your shell.\nYour own space.",
                        color = TextPrimary, fontSize = 30.sp, lineHeight = 36.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "A standalone Android shell with its own private environment — separate from Termux.",
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
                            Text("PRIVATE ENVIRONMENT", color = Cyan, fontSize = 11.sp,
                                letterSpacing = 1.3.sp)
                            Spacer(Modifier.weight(1f))
                            Text(
                                stateLabel,
                                color = stateColor,
                                fontSize = 11.sp, letterSpacing = 0.8.sp
                            )
                        }
                        Text(
                            status,
                            color = if (runtimeHasWarnings) Warning else if (technicalErrorDetails != null || sessionFailureMessage != null) Color(0xFFFF8AA5) else TextPrimary,
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )
                        if (runtimeHasWarnings && warningLines.isNotEmpty()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = Warning.copy(alpha = 0.10f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        if (warningLines.size == 1) "1 distinct runtime warning"
                                        else "${warningLines.size} distinct runtime warnings",
                                        color = Warning, fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        warningLines.first(),
                                        color = TextMuted, fontSize = 12.sp, lineHeight = 17.sp,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 3
                                    )
                                }
                            }
                        }
                        if (technicalErrorDetails != null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                TextButton(
                                    onClick = onToggleTechnicalDetails,
                                    modifier = Modifier.heightIn(min = 48.dp),
                                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        if (showTechnicalDetails) "Hide technical details"
                                        else "Show technical details",
                                        color = Cyan,
                                        fontSize = 13.sp
                                    )
                                }
                                if (showTechnicalDetails) {
                                    TextButton(
                                        onClick = onCopyTechnicalDetails,
                                        modifier = Modifier.heightIn(min = 48.dp),
                                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)
                                    ) {
                                        Text("Copy", color = Cyan, fontSize = 13.sp)
                                    }
                                }
                            }
                            if (showTechnicalDetails) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                                    color = Obsidian,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(10.dp)
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        Text(
                                            technicalErrorDetails,
                                            color = TextMuted,
                                            fontSize = 12.sp,
                                            lineHeight = 17.sp,
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
                        if (!runtimeVerified) {
                            Button(
                                onClick = onInstall,
                                enabled = !installing && !verifyingRuntime,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
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
                                        runtimeInstalled -> "Recheck environment"
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
                                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Purple,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(13.dp)
                            ) {
                                Text("Launch Bash", fontWeight = FontWeight.SemiBold)
                            }
                            TextButton(
                                onClick = onRecheck,
                                enabled = !verifyingRuntime && !installing,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            ) {
                                Text("Recheck environment", color = Cyan, fontSize = 13.sp)
                            }
                        }
                    }
                }


            }
        }
    }
}
