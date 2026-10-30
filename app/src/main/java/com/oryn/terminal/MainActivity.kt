package com.oryn.terminal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Obsidian = Color(0xFF0B0F19)
private val SurfaceDark = Color(0xFF111827)
private val SurfaceRaised = Color(0xFF182235)
private val Outline = Color(0xFF29364A)
private val TextPrimary = Color(0xFFF1F5F9)
private val TextMuted = Color(0xFFA3AFBF)
private val Cyan = Color(0xFF67DDE8)
private val Purple = Color(0xFFB69AF5)
private val Green = Color(0xFF8ED9AE)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Obsidian,
                    surface = SurfaceDark,
                    surfaceVariant = SurfaceRaised,
                    primary = Cyan,
                    secondary = Purple,
                    onBackground = TextPrimary,
                    onSurface = TextPrimary,
                    outline = Outline
                )
            ) {
                OrynTerminalApp()
            }
        }
    }
}

@Composable
private fun OrynTerminalApp() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Obsidian
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            Header()

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "A terminal that\nstays yours.",
                    modifier = Modifier.semantics { heading() },
                    color = TextPrimary,
                    fontSize = 34.sp,
                    lineHeight = 38.sp,
                    letterSpacing = (-0.8).sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "A focused Android command line, built to live alongside Termux — never replace it.",
                    color = TextMuted,
                    fontSize = 15.sp,
                    lineHeight = 23.sp
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionLabel("CURRENT MILESTONE")
                RuntimeCard()
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionLabel("BUILT AROUND YOU")
                InfoCard(
                    title = "Independent by design",
                    description = "Oryn has its own app identity and private storage. Your Termux installation stays untouched.",
                    accent = Cyan,
                    marker = "01"
                )
                InfoCard(
                    title = "A real Android runtime",
                    description = "The native PTY bridge is integrated. Connecting it to managed shell sessions comes next.",
                    accent = Purple,
                    marker = "02"
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "EARLY DEVELOPMENT  ·  ARM64 FIRST",
                    color = TextMuted.copy(alpha = 0.78f),
                    fontSize = 10.sp,
                    letterSpacing = 1.1.sp
                )
            }
        }
    }
}

@Composable
private fun Header() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceRaised)
                .border(1.dp, Outline, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "⌘",
                color = Cyan,
                fontSize = 23.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Column(
            modifier = Modifier.padding(start = 12.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = "Oryn",
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp
            )
            Text(
                text = "TERMINAL",
                color = TextMuted,
                fontSize = 10.sp,
                letterSpacing = 1.8.sp
            )
        }
        Spacer(Modifier.weight(1f))
        Surface(
            color = Green.copy(alpha = 0.10f),
            shape = CircleShape,
            border = BorderStroke(1.dp, Green.copy(alpha = 0.24f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Box(
                    Modifier
                        .size(6.dp)
                        .background(Green, CircleShape)
                )
                Text(
                    text = "FOUNDATION",
                    color = Green,
                    fontSize = 9.sp,
                    letterSpacing = 0.8.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(label: String) {
    Text(
        text = label,
        color = TextMuted,
        fontSize = 10.sp,
        letterSpacing = 1.35.sp,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun RuntimeCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Outline, RoundedCornerShape(22.dp)),
        color = SurfaceDark,
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NATIVE PTY BRIDGE",
                    color = Cyan,
                    fontSize = 10.sp,
                    letterSpacing = 1.1.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "INTEGRATED",
                    color = Green,
                    fontSize = 9.sp,
                    letterSpacing = 0.8.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Process layer connected",
                    color = TextPrimary,
                    fontSize = 19.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "The native PTY bridge is part of the Android app. The next step is wiring it to a managed shell session.",
                    color = TextMuted,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }

            Surface(
                color = Obsidian,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 13.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "oryn",
                        color = Cyan,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "  ·  shell session not connected",
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "—",
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    description: String,
    accent: Color,
    marker: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Outline.copy(alpha = 0.8f), RoundedCornerShape(18.dp)),
        color = SurfaceDark,
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = marker,
                color = accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    color = TextMuted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
