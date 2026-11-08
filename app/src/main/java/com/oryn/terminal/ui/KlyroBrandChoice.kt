package com.oryn.terminal.ui

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/** Android launcher aliases are switched atomically on Android 13+. */
object KlyroBrandChoice {
    const val DEFAULT = "b"
    const val ALTERNATIVE = "a"
    private const val PREFS = "klyro_brand_v1"
    private const val KEY = "launcher_logo"

    fun selected(context: Context): String {
        val pm = context.packageManager
        val primary = ComponentName(context.packageName, context.packageName + ".LauncherB")
        val alternative = ComponentName(context.packageName, context.packageName + ".LauncherA")
        val a = pm.getComponentEnabledSetting(alternative)
        val b = pm.getComponentEnabledSetting(primary)
        return if (a == PackageManager.COMPONENT_ENABLED_STATE_ENABLED &&
            b != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) ALTERNATIVE else DEFAULT
    }

    fun select(context: Context, choice: String): Boolean {
        if (choice != DEFAULT && choice != ALTERNATIVE) return false
        val pm = context.packageManager
        val primary = ComponentName(context.packageName, context.packageName + ".LauncherB")
        val alternative = ComponentName(context.packageName, context.packageName + ".LauncherA")
        val on = PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        val off = PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        val ok = runCatching {
            if (Build.VERSION.SDK_INT >= 33) {
                pm.setComponentEnabledSettings(
                    listOf(
                        PackageManager.ComponentEnabledSetting(primary,
                            if (choice == DEFAULT) on else off,
                            PackageManager.DONT_KILL_APP),
                        PackageManager.ComponentEnabledSetting(alternative,
                            if (choice == ALTERNATIVE) on else off,
                            PackageManager.DONT_KILL_APP)
                    )
                )
            } else {
                // Preserve at least one launcher entry throughout the switch.
                if (choice == ALTERNATIVE) {
                    pm.setComponentEnabledSetting(alternative, on, PackageManager.DONT_KILL_APP)
                    pm.setComponentEnabledSetting(primary, off, PackageManager.DONT_KILL_APP)
                } else {
                    pm.setComponentEnabledSetting(primary, on, PackageManager.DONT_KILL_APP)
                    pm.setComponentEnabledSetting(alternative, off, PackageManager.DONT_KILL_APP)
                }
            }
        }.isSuccess
        val verified = ok && selected(context) == choice
        if (verified) context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, choice).apply()
        return verified
    }
}
