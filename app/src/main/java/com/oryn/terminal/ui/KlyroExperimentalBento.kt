package com.oryn.terminal.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oryn.terminal.R

/**
 * Klyro-specific Settings concept. Debug APK only (feature resource flag).
 *
 * Unlike a generic identical-card grid, the app's working terminal is the
 * hero, its active sessions are a functional second tier, and privacy/about
 * become quiet secondary actions. All cards have meaningful hit targets.
 */
@Composable
internal fun KlyroExperimentalBento(
    tabsCount: Int,
    accentLabel: String,
    onOpen: (String) -> Unit
) {
    val c = LocalKlyroColors.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth().clickable(
                role = Role.Button,
                onClickLabel = "Appearance and terminal typography",
                onClick = { onOpen("appearance") }
            ),
            color = c.tile,
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, c.accent.copy(alpha = .18f))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("Terminal & appearance", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold, color = c.foreground)
                        Text("$accentLabel accent · tap to customize", color = c.muted,
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Box(
                        modifier = Modifier.size(44.dp)
                            .background(c.accent.copy(alpha = .14f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Aa", fontSize = 18.sp, color = c.accent)
                    }
                }
                // Klyro signature: a real monospace command-preview on the
                // same terminal token as the PTY, not a stock "settings" graphic.
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = c.terminal,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, c.accent.copy(alpha = .13f))
                ) {
                    Column(modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("klyro ~ $", fontFamily = FontFamily.Monospace, color = c.accent,
                            fontSize = 13.sp)
                        Text("echo \"make it yours\"", fontFamily = FontFamily.Monospace,
                            color = c.foreground, fontSize = 12.sp)
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BentoAction(
                title = "Sessions",
                subtitle = "$tabsCount running",
                icon = R.drawable.hugeicons_terminal,
                modifier = Modifier.weight(1.12f),
                emphasize = true
            ) { onOpen("sessions") }
            BentoAction(
                title = "Keyboard",
                subtitle = "Keys & modifiers",
                icon = R.drawable.hugeicons_keyboard,
                modifier = Modifier.weight(.88f)
            ) { onOpen("keyboard") }
        }
        BentoAction(
            title = "Files & storage",
            subtitle = "Private files · Android document picker",
            icon = R.drawable.hugeicons_folder_01,
            modifier = Modifier.fillMaxWidth(),
            horizontal = true
        ) { onOpen("files") }
        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BentoAction(
                title = "Privacy",
                subtitle = "Data & clipboard",
                icon = R.drawable.hugeicons_shield_01,
                modifier = Modifier.weight(1f),
                compact = true
            ) { onOpen("privacy") }
            BentoAction(
                title = "About",
                subtitle = "Version & licences",
                icon = R.drawable.hugeicons_information_square,
                modifier = Modifier.weight(1f),
                compact = true
            ) { onOpen("about") }
        }
        Text("Experimental Settings layout · available in test APK only",
            color = c.muted, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun BentoAction(
    title: String,
    subtitle: String,
    icon: Int,
    modifier: Modifier,
    emphasize: Boolean = false,
    horizontal: Boolean = false,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    val c = LocalKlyroColors.current
    val tint = if (emphasize) c.accent.copy(alpha = .12f) else c.tile
    Surface(
        modifier = modifier.clickable(
            role = Role.Button, onClickLabel = "Open $title", onClick = onClick
        ),
        color = tint,
        shape = RoundedCornerShape(if (compact) 20.dp else 24.dp)
    ) {
        if (horizontal) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(painterResource(icon), contentDescription = null, tint = c.accent,
                    modifier = Modifier.size(24.dp))
                Column {
                    Text(title, color = c.foreground, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, color = c.muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth()
                .heightIn(min = if (compact) 96.dp else 116.dp).padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween) {
                Icon(painterResource(icon), contentDescription = null, tint = c.accent,
                    modifier = Modifier.size(24.dp))
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(title, color = c.foreground, fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleSmall)
                    Text(subtitle, color = c.muted, style = MaterialTheme.typography.bodySmall,
                        maxLines = 2)
                }
            }
        }
    }
}
