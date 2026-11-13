package com.oryn.terminal.diagnostics

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.oryn.terminal.BuildConfig
import com.oryn.terminal.MainActivity
import com.oryn.terminal.R
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Process singleton: Activity recreation never creates another watchdog. No PTY ownership. */
object FreezeRecorder {
    private val engine = FreezeWatchdog()
    val events = FreezeEvents()
    val openReports = MutableStateFlow(false)
    val notice = MutableStateFlow<String?>(null)
    @Volatile var lastError: String? = null
        private set
    @Volatile var lifecycle = "CREATED"
        private set
    @Volatile var terminal: com.termux.terminal.TerminalSession? = null
    @Volatile private var lastScreen = "UNKNOWN"
    @Volatile var activeSession = 0L
    @Volatile var sessionCount = 0
    @Volatile var serviceActive = false
    private var started = false
    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadScheduledExecutor { r -> Thread(r, "KlyroFreezeRecorder").apply { isDaemon = true } }
    private val notifications = java.util.concurrent.ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
        java.util.concurrent.ArrayBlockingQueue<Runnable>(2),
        { r -> Thread(r, "KlyroFreezeNotification").apply { isDaemon = true } },
        java.util.concurrent.RejectedExecutionHandler { _, _ -> lastError = "Diagnostics notification deferred; reports retained" })
    private lateinit var app: Context
    @Volatile private var store: FreezeReportStore? = null
    @Volatile private var pendingPing: Runnable? = null
    private val ready = java.util.concurrent.CountDownLatch(1)
    private var reportId = ""
    private var report = StringBuilder()
    private var warned = false
    fun now() = SystemClock.elapsedRealtime()
    fun event(code: FreezeEvents.Code, a: Long = 0, b: Long = 0) {
        when (code) {
            FreezeEvents.Code.HOME, FreezeEvents.Code.TERMINAL, FreezeEvents.Code.SETTINGS, FreezeEvents.Code.REPORTS -> lastScreen = code.name
            else -> Unit
        }
        events.add(code, now(), a, b)
    }

    @Synchronized fun start(context: Context) {
        if (started) return
        started = true
        app = context.applicationContext
        worker.execute {
            try { guard {
                store = FreezeReportStore(File(app.noBackupFilesDir, "freeze-reports"))
                // A report without RECOVERED survived an interrupted process; do not invent its cause.
                store!!.list().asReversed().firstOrNull { id ->
                    val body = store!!.read(id)
                    body.contains("UI_FREEZE_DETECTED") || body.contains("SYSTEM_ANR_CONFIRMED")
                }?.let { id ->
                    val body = store!!.read(id)
                    if (body.contains("UI_FREEZE_DETECTED") && !body.contains("UI_RECOVERED"))
                        store!!.write(id, body.removePrefix("KLYRO_FREEZE_V1\n") + "\nrecovery=NOT_OBSERVED_BEFORE_PROCESS_RESTART\n")
                    if (!File(app.noBackupFilesDir,"freeze-seen").readTextOrEmpty().contains(id)) notice.value = id
                }
            } } finally { ready.countDown() }
        }
        worker.scheduleWithFixedDelay({
            guard { process(engine.tick(now())) }
        }, 250, 250, TimeUnit.MILLISECONDS)
        // Historical system calls and trace reads cannot delay the heartbeat thread.
        notifications.execute { collectExits() }
    }
    fun transition(code: FreezeEvents.Code) {
        lifecycle = code.name
        event(code)
        pendingPing?.let(main::removeCallbacks)
        pendingPing = null
        val changes = engine.lifecycle(code == FreezeEvents.Code.RESUMED, now())
        if (changes.isNotEmpty()) worker.execute { guard { process(changes) } }
    }
    private fun process(changes: List<FreezeWatchdog.Event>) {
        for (change in changes) {
            when (change.kind) {
                FreezeWatchdog.Kind.PING -> {
                    val task = Runnable {
                        val recovery = engine.acknowledge(change.generation, now())
                        if (recovery.isNotEmpty()) worker.execute { guard { process(recovery) } }
                    }
                    pendingPing = task
                    main.post(task)
                }
                FreezeWatchdog.Kind.UI_STALL_DETECTED -> {
                    reportId = "${System.currentTimeMillis()}-${Process.myPid()}-${change.generation}-${change.started}"
                    report = StringBuilder("state=UI_STALL_DETECTED\nwall_ms=${System.currentTimeMillis()}\n")
                    warned = false
                    appendState(change)
                    persist()
                }
                FreezeWatchdog.Kind.UI_FREEZE_DETECTED, FreezeWatchdog.Kind.SECOND_SNAPSHOT -> {
                    report.append("state=UI_FREEZE_DETECTED heuristic=true\n")
                    appendState(change)
                    captureStacks()
                    persist()
                    if (!warned) {
                        warned = true
                        val id = reportId
                        notifications.execute { notifyFreeze(id) }
                    }
                }
                FreezeWatchdog.Kind.UI_RECOVERED -> {
                    report.append("state=UI_RECOVERED duration_ms=${change.duration}\n")
                    persist()
                    if (warned) notice.value = reportId
                }
            }
        }
    }
    private fun appendState(change: FreezeWatchdog.Event) {
        val runtime = Runtime.getRuntime()
        report.append("monotonic_ms=${now()} generation=${change.generation} blocked_ms=${change.duration}\n")
        report.append("pid=${Process.myPid()} version=${BuildConfig.VERSION_NAME} lifecycle=$lifecycle active_session=$activeSession sessions=$sessionCount service=$serviceActive\n")
        report.append("java_used=${runtime.totalMemory()-runtime.freeMemory()} java_max=${runtime.maxMemory()} native_bytes=${Debug.getNativeHeapAllocatedSize()}\n")
        val pty = terminal
        val read = pty?.diagnosticBytesRead ?: 0
        val parsed = pty?.diagnosticBytesParsed ?: 0
        report.append("last_screen=$lastScreen pty_read=$read pty_parsed=$parsed pty_backlog_approx=${(read-parsed).coerceAtLeast(0)}\n")
        report.append(events.snapshot())
    }
    private fun captureStacks() {
        try {
            val traces = Thread.getAllStackTraces()
            // Never store arbitrary thread names, which can contain user input.
            val selected = traces.entries.sortedBy { if (it.key === Looper.getMainLooper().thread) 0 else 1 }.take(24)
            for ((thread, stack) in selected) {
                report.append("thread_id=${thread.id} main=${thread === Looper.getMainLooper().thread} state=${thread.state}\n")
                stack.take(64).forEach { report.append("at ").append(it.toString().take(300)).append('\n') }
            }
        } catch (_: SecurityException) { report.append("STACKS_UNAVAILABLE_SECURITY\n") }
        report.append("JAVA_STACKS_DO_NOT_PROVE_NATIVE_ROOT_CAUSE\n")
    }
    private fun persist() {
        store?.write(reportId, report.toString()) ?: throw java.io.IOException("Report store unavailable")
    }
    private fun notifyFreeze(id: String) = guard {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(app,
                android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return@guard
        val manager = app.getSystemService(NotificationManager::class.java)
        if (!manager.areNotificationsEnabled()) return@guard
        manager.createNotificationChannel(NotificationChannel("klyro_freezes", "Freeze diagnostics", NotificationManager.IMPORTANCE_DEFAULT))
        val intent = Intent(app, MainActivity::class.java).putExtra("freeze_reports", true)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val action = PendingIntent.getActivity(app, 7501, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify(7501, NotificationCompat.Builder(app,"klyro_freezes")
            .setSmallIcon(R.drawable.ic_klyro_mark_monochrome).setContentTitle("Klyro stopped responding")
            .setContentText("A local diagnostic was recorded. Tap to review.")
            .setContentIntent(action).setOnlyAlertOnce(true).setAutoCancel(true).build())
    }
    private fun collectExits() = guard {
        if (Build.VERSION.SDK_INT < 30) return@guard
        val preferences = app.getSharedPreferences("freeze_history", Context.MODE_PRIVATE)
        val cursor = preferences.getLong("exit_cursor", 0)
        val manager = app.getSystemService(ActivityManager::class.java)
        val exits = manager.getHistoricalProcessExitReasons(app.packageName, 0, 16)
            .filter { it.processName == app.packageName && it.timestamp > cursor }.sortedBy { it.timestamp }
        ready.await()
        val historyStore = repository()
        for (exit in exits) {
            val state = if (exit.reason == ApplicationExitInfo.REASON_ANR) "SYSTEM_ANR_CONFIRMED" else "PROCESS_EXIT_OTHER"
            val id = "${exit.timestamp}-${exit.pid}-exit"
            val frames = try { exit.traceInputStream?.use { FreezeReportStore.traceFrames(it) }
                ?: "SYSTEM_TRACE_UNAVAILABLE\n" } catch (_: java.io.IOException) { "SYSTEM_TRACE_READ_FAILED\n" }
            historyStore.write(id,"state=$state\nwall_ms=${exit.timestamp} pid=${exit.pid} reason=${exit.reason}\n$frames")
            if (!preferences.edit().putLong("exit_cursor", exit.timestamp).commit()) throw java.io.IOException("Exit cursor persistence failed")
            if (state == "SYSTEM_ANR_CONFIRMED") notice.value = id
        }
    }
    fun recordStabilityRun(id: Long) {
        event(FreezeEvents.Code.STRESS_START, id, 1500)
        worker.execute { guard {
            repository().write("${System.currentTimeMillis()}-${Process.myPid()}-test", "state=STABILITY_TEST_STARTED\nmonotonic_ms=${now()} session=$id lines_requested=1500\nCompletion=NOT_VERIFIED inspect END marker\n" + events.snapshot())
        } }
    }
    fun repository(): FreezeReportStore = store ?: throw java.io.IOException("Diagnostics initializing; retry shortly")
    fun acknowledgeNotice(id: String) {
        notice.value = null
        worker.execute { guard { File(app.noBackupFilesDir,"freeze-seen").writeText(id) } }
    }
    private fun File.readTextOrEmpty() = if (exists()) readText() else ""
    private inline fun guard(action: () -> Unit) {
        try { action() } catch (e: Exception) {
            lastError = "Diagnostics operation failed: ${e.javaClass.simpleName}"
            android.util.Log.w("KlyroFreeze", lastError ?: "Diagnostics failure") // No exception message or user paths.
        }
    }
}
