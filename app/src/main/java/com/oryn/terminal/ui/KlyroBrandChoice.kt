package com.oryn.terminal.ui

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * Android launcher aliases are switched atomically on Android 13+.
 * Only OS-confirmed, exclusive alias states count as success; preferences
 * cannot prove which icon the launcher actually exposes.
 */
object KlyroBrandChoice {
    const val DEFAULT = "b"
    const val ALTERNATIVE = "a"
    private const val PREFS = "klyro_brand_v1"
    private const val KEY = "launcher_logo"

    private fun primary(context: Context) =
        ComponentName(context.packageName, context.packageName + ".LauncherB")
    private fun alternative(context: Context) =
        ComponentName(context.packageName, context.packageName + ".LauncherA")

    private fun aliasEnabled(pm: PackageManager, component: ComponentName, manifestDefault: Boolean): Boolean {
        return when (pm.getComponentEnabledSetting(component)) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED -> false
            else -> manifestDefault
        }
    }

    /** Null means an invalid state: both launcher entries on or both off. */
    private fun verifiedChoice(context: Context): String? {
        val pm = context.packageManager
        val b = aliasEnabled(pm, primary(context), true)
        val a = aliasEnabled(pm, alternative(context), false)
        return when {
            b && !a -> DEFAULT
            a && !b -> ALTERNATIVE
            else -> null
        }
    }

    /** Prefer the OS state to stale preferences; fall back only for display. */
    fun selected(context: Context): String =
        runCatching { verifiedChoice(context) }.getOrNull() ?: DEFAULT

    /** A visual fallback is not proof of the current Android launcher component state. */
    fun isVerifiedSelection(context: Context, choice: String): Boolean =
        (choice == DEFAULT || choice == ALTERNATIVE) &&
            runCatching { verifiedChoice(context) == choice }.getOrDefault(false)

    @Synchronized
    fun select(context: Context, choice: String): Boolean {
        if (choice != DEFAULT && choice != ALTERNATIVE) return false
        val pm = context.packageManager
        val b = primary(context)
        val a = alternative(context)
        val on = PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        val off = PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        val beforeB = runCatching { pm.getComponentEnabledSetting(b) }.getOrNull() ?: return false
        val beforeA = runCatching { pm.getComponentEnabledSetting(a) }.getOrNull() ?: return false

        val switched = runCatching {
            if (Build.VERSION.SDK_INT >= 33) {
                pm.setComponentEnabledSettings(
                    listOf(
                        PackageManager.ComponentEnabledSetting(b, if (choice == DEFAULT) on else off,
                            PackageManager.DONT_KILL_APP),
                        PackageManager.ComponentEnabledSetting(a, if (choice == ALTERNATIVE) on else off,
                            PackageManager.DONT_KILL_APP)
                    )
                )
            } else if (choice == ALTERNATIVE) {
                // Never disable the only launcher icon before enabling its replacement.
                pm.setComponentEnabledSetting(a, on, PackageManager.DONT_KILL_APP)
                pm.setComponentEnabledSetting(b, off, PackageManager.DONT_KILL_APP)
            } else {
                pm.setComponentEnabledSetting(b, on, PackageManager.DONT_KILL_APP)
                pm.setComponentEnabledSetting(a, off, PackageManager.DONT_KILL_APP)
            }
            verifiedChoice(context) == choice
        }.getOrDefault(false)

        if (!switched) {
            // An older Android build may fail half-way through a sequential switch.
            // Best effort rollback to the exact pre-switch OS component states.
            runCatching {
                if (Build.VERSION.SDK_INT >= 33) {
                    pm.setComponentEnabledSettings(
                        listOf(
                            PackageManager.ComponentEnabledSetting(b, beforeB, PackageManager.DONT_KILL_APP),
                            PackageManager.ComponentEnabledSetting(a, beforeA, PackageManager.DONT_KILL_APP)
                        )
                    )
                } else {
                    // Restore the prior active launcher before changing the other entry.
                    if (beforeB == on || beforeB == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {
                        pm.setComponentEnabledSetting(b, beforeB, PackageManager.DONT_KILL_APP)
                        pm.setComponentEnabledSetting(a, beforeA, PackageManager.DONT_KILL_APP)
                    } else {
                        pm.setComponentEnabledSetting(a, beforeA, PackageManager.DONT_KILL_APP)
                        pm.setComponentEnabledSetting(b, beforeB, PackageManager.DONT_KILL_APP)
                    }
                }
            }
            return false
        }

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, choice).apply()
        return true
    }
}
