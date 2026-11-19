package com.oryn.terminal.ui

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.oryn.terminal.plugins.KlyroPluginManager
import java.util.UUID
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PluginsRefinementUiTest {
    @get:Rule val ui = createAndroidComposeRule<ComponentActivity>()
    private lateinit var isolatedContext: Context
    private lateinit var preferences: SharedPreferences
    private val preferencePrefix = "plugin-ui-test-${UUID.randomUUID()}-"

    @Before fun isolateRegistry() {
        isolatedContext = object : ContextWrapper(ui.activity) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
                super.getSharedPreferences(preferencePrefix + name, mode)
        }
        preferences = isolatedContext.getSharedPreferences("klyro_plugins_v1", Context.MODE_PRIVATE)
    }

    @After fun cleanIsolatedRegistry() {
        ui.activity.deleteSharedPreferences(preferencePrefix + "klyro_plugins_v1")
    }

    @Test fun largeTextPlacesAddBelowPackInformationAndKeepsTouchTarget() {
        val plugin = KlyroPluginManager.bundled.first()
        var reviews = 0
        ui.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, 2f),
                LocalKlyroColors provides KlyroColors.create(true, "cyan")
            ) {
                MaterialTheme {
                    Box(Modifier.width(280.dp)) {
                        KlyroExplorePluginCard(plugin, false, true, onAdd = { reviews++ })
                    }
                }
            }
        }
        val add = ui.onNodeWithContentDescription("Review and add ${plugin.name}")
        add.assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        val descriptionBounds = ui.onNodeWithText(plugin.description).fetchSemanticsNode().boundsInRoot
        assertTrue(add.fetchSemanticsNode().boundsInRoot.top >= descriptionBounds.bottom)
        add.performClick()
        ui.runOnIdle { assertEquals(1, reviews) }
    }

    @Test fun installedPackCannotBeAddedAgain() {
        val plugin = KlyroPluginManager.bundled.first()
        ui.setContent {
            MaterialTheme { KlyroExplorePluginCard(plugin, true, true, onAdd = { error("Already installed") }) }
        }
        ui.onNodeWithContentDescription("${plugin.name} installed").assertIsNotEnabled()
    }

    @Test fun unavailableRegistryPreventsReviewEvenForAnUninstalledPack() {
        val plugin = KlyroPluginManager.bundled.first()
        ui.setContent {
            MaterialTheme { KlyroExplorePluginCard(plugin, false, false, onAdd = { error("Registry unavailable") }) }
        }
        ui.onNodeWithContentDescription("Review and add ${plugin.name}").assertIsNotEnabled()
    }

    @Test fun unreadableRegistryIsNotEmptyAndRecoversAfterRetryAndValidRefresh() {
        preferences.edit().putString("manifest", "{broken").commit()
        ui.setContent {
            CompositionLocalProvider(LocalContext provides isolatedContext) {
                MaterialTheme { KlyroPluginsPage(onBack = {}) }
            }
        }
        ui.waitUntil(5_000) { ui.onAllNodesWithText("Retry").fetchSemanticsNodes().isNotEmpty() }
        ui.onNodeWithText("Installed").performClick()
        ui.onNodeWithText("Nothing installed yet").assertDoesNotExist()
        ui.onNodeWithText("Retry").performClick()
        ui.waitUntil(5_000) {
            ui.onAllNodesWithText("Saved plugin registry is unreadable. Reload before making changes.")
                .fetchSemanticsNodes().isNotEmpty()
        }
        preferences.edit().putString("manifest", "[]").commit()
        ui.waitUntil(5_000) { ui.onAllNodesWithText("Installed (0)").fetchSemanticsNodes().isNotEmpty() }
        ui.onNodeWithText("Nothing installed yet").assertIsDisplayed()
        ui.onNodeWithText("Retry").assertDoesNotExist()
    }

    @Test fun unreadableAndPartiallyInvalidRegistriesCannotBeOverwritten() {
        val manager = KlyroPluginManager(isolatedContext)
        for (manifest in listOf("{broken", "[{}]")) {
            preferences.edit().putString("manifest", manifest).commit()
            assertTrue(runCatching { manager.installBundled(KlyroPluginManager.bundled.first().id) }
                .exceptionOrNull() is IllegalStateException)
            assertEquals(manifest, preferences.getString("manifest", null))
            assertTrue("Palette callers must distinguish an unreadable registry from no plugins",
                runCatching { manager.activeCommands() }.exceptionOrNull() is IllegalStateException)
        }
    }

    @Test fun commandPaletteShowsPluginRegistryFailureInsteadOfAnEmptyResult() {
        val message = "Saved plugin shortcuts are unavailable. Open Plugins to reload the registry."
        ui.setContent {
            CompositionLocalProvider(
                LocalContext provides isolatedContext,
                LocalKlyroColors provides KlyroColors.create(true, "cyan")
            ) {
                MaterialTheme {
                    KlyroCommandPalette(
                        installedPrefix = null,
                        pluginCommands = emptyList(),
                        pluginLoadError = message,
                        onInsert = { true },
                        onDismiss = {}
                    )
                }
            }
        }
        ui.onNodeWithText(message).assertIsDisplayed()
    }
}
