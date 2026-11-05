package com.oryn.terminal.ui

import android.graphics.Color as AndroidColor
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Original native Compose translation of the Moduix React color-picker concept.
 * The picker stays INSIDE its dialog; no popup/portal can appear behind it.
 * Custom ARGB colors persist, with AA contrast enforced by KlyroColors.
 */
@Composable
internal fun KlyroColorPickerDialog(
    initialArgb: Int,
    onApply: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalKlyroColors.current
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences("klyro_ui_theme", Context.MODE_PRIVATE)
    }
    var favorites by remember(context) {
        mutableStateOf(KlyroFavoriteColors.decode(prefs.getString("custom_favorites", "")))
    }
    var favoriteName by remember { mutableStateOf("Custom") }
    var pendingFavoriteDeletion by remember { mutableStateOf<KlyroFavoriteColors.Favorite?>(null) }
    fun saveFavorites(value: List<KlyroFavoriteColors.Favorite>) {
        favorites = value
        prefs.edit().putString("custom_favorites", KlyroFavoriteColors.encode(value)).apply()
    }
    val initialHsv = remember(initialArgb) {
        FloatArray(3).also { AndroidColor.colorToHSV(initialArgb or -0x1000000, it) }
    }
    var hue by remember(initialArgb) { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember(initialArgb) { mutableFloatStateOf(initialHsv[1]) }
    var brightness by remember(initialArgb) { mutableFloatStateOf(initialHsv[2]) }
    var alpha by remember(initialArgb) {
        mutableFloatStateOf((initialArgb ushr 24) / 255f)
    }
    var showExtended by remember { mutableStateOf(false) }
    var hexInput by remember(initialArgb) {
        mutableStateOf("#%06X".format(initialArgb and 0xFFFFFF))
    }
    fun rgb(): Int = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, brightness))
    fun argb(): Int =
        ((alpha * 255f).roundToInt().coerceIn(0, 255) shl 24) or (rgb() and 0xFFFFFF)
    fun position(x: Float, y: Float, w: Int, h: Int) {
        if (w <= 0 || h <= 0) return
        saturation = (x / w).coerceIn(0f, 1f)
        brightness = (1f - y / h).coerceIn(0f, 1f)
        hexInput = "#%06X".format(rgb() and 0xFFFFFF)
    }

    KlyroModalTheme {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = colors.sheet,
            shape = RoundedCornerShape(28.dp),
            title = { Text("Choose a color", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .heightIn(max = 550.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Adjust hue, brightness, saturation and transparency. Changes apply after confirmation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted
                    )
                    // Native HSV area: white/saturated along X, black/bright along Y.
                    Box(
                        modifier = Modifier.fillMaxWidth().height(184.dp)
                            .semantics { contentDescription = "Saturation and brightness area, drag to choose" }
                    ) {
                        Canvas(
                            modifier = Modifier.fillMaxWidth().height(184.dp)
                                .pointerInput(hue) {
                                    detectTapGestures { offset ->
                                        position(offset.x, offset.y, size.width, size.height)
                                    }
                                }
                                .pointerInput(hue) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            position(offset.x, offset.y, size.width, size.height)
                                        },
                                        onDrag = { change, _ ->
                                            position(change.position.x, change.position.y, size.width, size.height)
                                            change.consume()
                                        }
                                    )
                                }
                        ) {
                            val cols = 28
                            val rows = 20
                            val tw = size.width / cols
                            val th = size.height / rows
                            for (iy in 0 until rows) {
                                for (ix in 0 until cols) {
                                    val sat = (ix + .5f) / cols
                                    val value = 1f - (iy + .5f) / rows
                                    drawRect(
                                        Color(AndroidColor.HSVToColor(floatArrayOf(hue, sat, value))),
                                        topLeft = Offset(ix * tw, iy * th),
                                        size = Size(tw + .5f, th + .5f)
                                    )
                                }
                            }
                            drawCircle(
                                color = Color.White,
                                radius = 8.dp.toPx(),
                                center = Offset(saturation * size.width, (1f - brightness) * size.height)
                            )
                            drawCircle(
                                color = Color.Black,
                                radius = 5.dp.toPx(),
                                center = Offset(saturation * size.width, (1f - brightness) * size.height)
                            )
                        }
                    }
                    Text("Hue · ${hue.roundToInt()}°", style = MaterialTheme.typography.labelMedium)
                    // Draw an actual rainbow under the native slider: the
                    // Material3 default accent track gives no hue feedback.
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Canvas(Modifier.fillMaxWidth().height(36.dp)) {
                            val steps = 60
                            val width = size.width / steps
                            for (i in 0 until steps) {
                                drawRect(
                                    color = Color(AndroidColor.HSVToColor(
                                        floatArrayOf(i * 360f / steps, 1f, 1f))),
                                    topLeft = Offset(i * width, size.height * .36f),
                                    size = Size(width + 1f, size.height * .28f)
                                )
                            }
                        }
                        Slider(
                            value = hue,
                            onValueChange = {
                                hue = it
                                hexInput = "#%06X".format(rgb() and 0xFFFFFF)
                            },
                            valueRange = 0f..360f,
                            modifier = Modifier.semantics { contentDescription = "Hue in degrees" }
                        )
                    }
                    Text("Saturation · ${(saturation * 100f).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = saturation,
                        onValueChange = {
                            saturation = it
                            hexInput = "#%06X".format(rgb() and 0xFFFFFF)
                        },
                        modifier = Modifier.semantics { contentDescription = "Saturation percentage" }
                    )
                    Text("Brightness · ${(brightness * 100f).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = brightness,
                        onValueChange = {
                            brightness = it
                            hexInput = "#%06X".format(rgb() and 0xFFFFFF)
                        },
                        modifier = Modifier.semantics { contentDescription = "Brightness percentage" }
                    )
                    /* Native Material slider is used for hue above.
                    Slider(
                        value = hue,
                        onValueChange = {
                            hue = it
                            hexInput = "#%06X".format(rgb() and 0xFFFFFF)
                        },
                        valueRange = 0f..360f,
                        modifier = Modifier.semantics { contentDescription = "Hue" }
                    )
                    */
                    Text("Opacity · " + (alpha * 100f).roundToInt() + "%",
                        style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = alpha,
                        onValueChange = { alpha = it },
                        valueRange = 0f..1f,
                        modifier = Modifier.semantics { contentDescription = "Opacity" }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = hexInput,
                            onValueChange = { input ->
                                val cleaned = input.removePrefix("#")
                                    .filter { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
                                    .take(6)
                                hexInput = "#" + cleaned.uppercase()
                                if (cleaned.length == 6) {
                                    val hsv = FloatArray(3)
                                    AndroidColor.colorToHSV(
                                        cleaned.toInt(16) or -0x1000000, hsv
                                    )
                                    hue = hsv[0]
                                    saturation = hsv[1]
                                    brightness = hsv[2]
                                }
                            },
                            label = { Text("Hex") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                        )
                        Surface(
                            modifier = Modifier.size(52.dp),
                            color = androidx.compose.ui.graphics.lerp(
                                colors.sheet,
                                Color(argb()).copy(alpha = 1f),
                                alpha
                            ),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.muted)
                        ) {}
                    }
                    Text(
                        "Selected button labels are adjusted to remain readable; opacity blends with the current theme.",
                        style = MaterialTheme.typography.bodySmall, color = colors.muted
                    )
                    TextButton(onClick = { showExtended = !showExtended }) {
                        Text(if (showExtended) "Hide extended presets" else "Show extended presets")
                    }
                    if (showExtended) {
                        Text("Extended colors", style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            KlyroColors.extended.forEach { preset ->
                                val swatch = KlyroColors.create(colors.dark, preset.id)
                                Button(
                                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                        .semantics { contentDescription = "Select " + preset.label },
                                    onClick = { onApply(preset.id) },
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(3.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = swatch.accent, contentColor = swatch.onAccent
                                    )
                                ) { Text(preset.label, fontSize = 11.sp, maxLines = 1) }
                            }
                        }
                        Text(
                            "#8D0826 · #EBEF7E · #DB0A71 (source colors)",
                            style = MaterialTheme.typography.bodySmall, color = colors.muted
                        )
                        Text("My colors", style = MaterialTheme.typography.titleSmall)
                        OutlinedTextField(
                            value = favoriteName,
                            onValueChange = { favoriteName = it.take(22) },
                            label = { Text("Color name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                saveFavorites(KlyroFavoriteColors.save(
                                    favorites, "#%08X".format(argb()), favoriteName
                                ))
                            },
                            enabled = hexInput.length == 7,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Save current color") }
                        favorites.forEach { favorite ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.size(24.dp),
                                    color = Color(AndroidColor.parseColor(favorite.hex)),
                                    shape = CircleShape
                                ) {}
                                Text(
                                    favorite.label,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                TextButton(onClick = {
                                    favoriteName = favorite.label
                                    val favArgb = AndroidColor.parseColor(favorite.hex)
                                    val hsv = FloatArray(3)
                                    AndroidColor.colorToHSV(favArgb or -0x1000000, hsv)
                                    hue = hsv[0]
                                    saturation = hsv[1]
                                    brightness = hsv[2]
                                    alpha = (favArgb ushr 24) / 255f
                                    hexInput = "#%06X".format(favArgb and 0xFFFFFF)
                                }) { Text("Edit") }
                                TextButton(onClick = {
                                    onApply("custom:" + favorite.hex)
                                }) { Text("Use") }
                                TextButton(onClick = {
                                    pendingFavoriteDeletion = favorite
                                }) { Text("Remove") }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = hexInput.length == 7,
                    onClick = { onApply(KlyroColors.customId(argb())) },
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        )
        pendingFavoriteDeletion?.let { favorite ->
            AlertDialog(
                onDismissRequest = { pendingFavoriteDeletion = null },
                title = { Text("Remove saved color?") },
                text = { Text("Remove \"${favorite.label}\" from your saved colors?") },
                confirmButton = {
                    TextButton(onClick = {
                        saveFavorites(favorites.filterNot { it.hex == favorite.hex })
                        pendingFavoriteDeletion = null
                    }) { Text("Remove") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingFavoriteDeletion = null }) { Text("Cancel") }
                }
            )
        }
    }
}
