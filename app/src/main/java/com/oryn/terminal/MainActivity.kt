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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Obsidian = Color(0xFF0B0F19)
private val SurfaceDark = Color(0xFF111827)
private val Elevated = Color(0xFF1E293B)
private val TextPrimary = Color(0xFFE2E8F0)
private val TextMuted = Color(0xFF94A3B8)
private val Cyan = Color(0xFF00F0FF)
private val Purple = Color(0xFF8338EC)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                    Text(
                        "ORYN",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        "TERMINAL",
                        color = TextMuted,
                        fontSize = 10.sp,
                        letterSpacing = 2.5.sp
                    )
                }
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .background(Cyan, CircleShape)
                )
                Text("FOUNDATION", color = TextMuted, fontSize = 10.sp, letterSpacing = 1.sp)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Your command line.\nYour own space.",
                    color = TextPrimary,
                    fontSize = 30.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "A standalone Android terminal, designed to live alongside Termux — never replace it.",
                    color = TextMuted,
                    fontSize = 14.sp,
                    lineHeight = 21.sp
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(3) { index ->
                            val dotColor = when (index) {
                                0 -> Color(0xFFFF0055)
                                1 -> Color(0xFFFBBF24)
                                else -> Color(0xFF00FF66)
                            }
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .background(dotColor, CircleShape)
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Text("SESSION 01", color = TextMuted, fontSize = 10.sp, letterSpacing = 1.sp)
                    }
                    Text(
                        "Native PTY bridge added — shell integration next.",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "The PTY process adapter is now part of the Android project. The next milestone is connecting it to a managed shell session. No simulated command output is shown here.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("oryn", color = Cyan, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                        Text("  ~/home  ", color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                        Text("›", color = Purple, fontFamily = FontFamily.Monospace, fontSize = 16.sp)
                        Text("  _", color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
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
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("ISOLATION FIRST", color = Cyan, fontSize = 10.sp, letterSpacing = 1.4.sp)
                    Text(
                        "Oryn has its own app identity and private storage. Your existing Termux installation is not modified.",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
            }

            Spacer(Modifier.weight(1f))
            Text(
                "NEON OBSIDIAN  ·  EARLY DEVELOPMENT",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = TextMuted,
                fontSize = 9.sp,
                letterSpacing = 1.2.sp
            )
        }
    }
}
