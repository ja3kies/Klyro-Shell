package com.oryn.terminal.ui

import android.graphics.Color as AndroidColor
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
    var favoriteName by rememberSaveable { mutableStateOf("Custom") }
    var pendingFavoriteDeletion by remember { mutableStateOf<KlyroFavoriteColors.Favorite?>(null) }
    fun saveFavorites(value: List<KlyroFavoriteColors.Favorite>) {
        favorites = value
        prefs.edit().putString("custom_favorites", KlyroFavoriteColors.encode(value)).apply()
    }
    val initialHsv = remember(initialArgb) {
        FloatArray(3).also { AndroidColor.colorToHSV(initialArgb or -0x1000000, it) }
    }
    var hue by rememberSaveable(initialArgb) { mutableFloatStateOf(initialHsv[0]) }
    var saturation by rememberSaveable(initialArgb) { mutableFloatStateOf(initialHsv[1]) }
    var brightness by rememberSaveable(initialArgb) { mutableFloatStateOf(initialHsv[2]) }
    var alpha by rememberSaveable(initialArgb) {
        mutableFloatStateOf((initialArgb ushr 24) / 255f)
    }
    var showAdvanced by rememberSaveable { mutableStateOf(false) }
    // Preset buttons preview an intent. Only Apply changes the app theme.
    var pendingPreset by rememberSaveable(initialArgb) { mutableStateOf<String?>(null) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val smallWindow = LocalConfiguration.current.screenHeightDp < 700 ||
        density.fontScale > 1.15f || WindowInsets.ime.getBottom(density) > 0
    var hexInput by rememberSaveable(initialArgb) {
        mutableStateOf("#%06X".format(initialArgb and 0xFFFFFF))
    }
    fun rgb(): Int = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, brightness))
    fun argb(): Int =
        ((alpha * 255f).roundToInt().coerceIn(0, 255) shl 24) or (rgb() and 0xFFFFFF)
    fun position(x: Float, y: Float, w: Int, h: Int) {
        pendingPreset = null
        if (w <= 0 || h <= 0) return
        saturation = (x / w).coerceIn(0f, 1f)
        brightness = (1f - y / h).coerceIn(0f, 1f)
        hexInput = "#%06X".format(rgb() and 0xFFFFFF)
    }

    KlyroModalTheme {
        if (!showAdvanced && pendingFavoriteDeletion == null) AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = colors.sheet,
            shape = RoundedCornerShape(KlyroUiMetrics.modalCorner),
            title = { Text("Custom accent", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(
                    modifier = if (smallWindow) Modifier.fillMaxWidth()
                        .heightIn(max = 360.dp).verticalScroll(rememberScrollState())
                        else Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // 44dp tall, full-width input; shortened vertically, not horizontally.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f).heightIn(min = KlyroUiMetrics.minTouch),
                            shape = RoundedCornerShape(12.dp),
                            color = colors.tile,
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.muted)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("HEX", style = MaterialTheme.typography.labelSmall,
                                    color = colors.muted)
                                BasicTextField(
                                    value = hexInput,
                                    onValueChange = { input ->
                                        pendingPreset = null
                                        hexInput = KlyroHexInput.sanitize(input)
                                        if (KlyroHexInput.isComplete(hexInput)) {
                                            val hsv = FloatArray(3)
                                            AndroidColor.colorToHSV(
                                                hexInput.substring(1).toInt(16) or -0x1000000,
                                                hsv
                                            )
                                            hue = hsv[0]
                                            saturation = hsv[1]
                                            brightness = hsv[2]
                                        }
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Ascii,
                                        imeAction = ImeAction.Done
                                    ),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        color = colors.foreground
                                    ),
                                    modifier = Modifier.weight(1f)
                                        .semantics { contentDescription = "Hex color value" }
                                )
                            }
                        }
                        Surface(
                            modifier = Modifier.size(KlyroUiMetrics.minTouch),
                            color = androidx.compose.ui.graphics.lerp(
                                colors.sheet, Color(argb()).copy(alpha = 1f), alpha
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.muted)
                        ) {}
                    }
                    if (hexInput.length != 7) {
                        Text("Enter six hexadecimal digits (0–9, A–F).",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.danger,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    }
                    // Native HSV area: white/saturated along X, black/bright along Y.
                    Box(
                        modifier = Modifier.fillMaxWidth().height(112.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .semantics {
                                contentDescription = "Saturation and brightness area, drag to choose"
                                stateDescription =
                                    "Saturation ${(saturation * 100).roundToInt()} percent, " +
                                    "brightness ${(brightness * 100).roundToInt()} percent"
                                // Two-dimensional picker also works with TalkBack without touch dragging.
                                customActions = listOf(
                                    CustomAccessibilityAction("Increase saturation") {
                                        pendingPreset = null
                                        saturation = (saturation + .05f).coerceIn(0f, 1f)
                                        hexInput = "#%06X".format(rgb() and 0xFFFFFF)
                                        true
                                    },
                                    CustomAccessibilityAction("Decrease saturation") {
                                        pendingPreset = null
                                        saturation = (saturation - .05f).coerceIn(0f, 1f)
                                        hexInput = "#%06X".format(rgb() and 0xFFFFFF)
                                        true
                                    },
                                    CustomAccessibilityAction("Increase brightness") {
                                        pendingPreset = null
                                        brightness = (brightness + .05f).coerceIn(0f, 1f)
                                        hexInput = "#%06X".format(rgb() and 0xFFFFFF)
                                        true
                                    },
                                    CustomAccessibilityAction("Decrease brightness") {
                                        pendingPreset = null
                                        brightness = (brightness - .05f).coerceIn(0f, 1f)
                                        hexInput = "#%06X".format(rgb() and 0xFFFFFF)
                                        true
                                    }
                                )
                            }
                    ) {
                        Canvas(
                            modifier = Modifier.fillMaxWidth().height(112.dp)
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
                            // Smooth GPU gradients: white→hue horizontally,
                            // clear→black vertically. No visible 28×20 tile grid.
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    listOf(Color.White, Color(AndroidColor.HSVToColor(
                                        floatArrayOf(hue, 1f, 1f)
                                    )))
                                ),
                                size = size
                            )
                            drawRect(
                                brush = Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black)
                                ),
                                size = size
                            )
                            drawCircle(
                                color = Color.Black,
                                radius = 10.dp.toPx(),
                                center = Offset(saturation * size.width, (1f - brightness) * size.height)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 7.dp.toPx(),
                                center = Offset(saturation * size.width, (1f - brightness) * size.height)
                            )
                            drawCircle(
                                color = if (brightness > .55f) Color.Black else Color.White,
                                radius = 4.dp.toPx(),
                                center = Offset(saturation * size.width, (1f - brightness) * size.height)
                            )
                        }
                    }
                    Text("Hue · ${hue.roundToInt()}°", style = MaterialTheme.typography.labelMedium)
                    // Draw an actual rainbow under the native slider: the
                    // Material3 default accent track gives no hue feedback.
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Canvas(Modifier.fillMaxWidth().height(KlyroUiMetrics.minTouch)) {
                            val track = KlyroUiMetrics.iosSliderTrack.toPx()
                            drawRoundRect(
                                brush = Brush.horizontalGradient(
                                    listOf(
                                        Color.Red, Color.Yellow, Color.Green,
                                        Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                                    )
                                ),
                                topLeft = Offset(0f, (size.height - track) / 2f),
                                size = Size(size.width, track),
                                cornerRadius = CornerRadius(track / 2f)
                            )
                        }
                        KlyroThinSlider(
                            value = hue,
                            onValueChange = {
                                pendingPreset = null
                                hue = it
                                hexInput = "#%06X".format(rgb() and 0xFFFFFF)
                            },
                            valueRange = 0f..360f,
                            showTrack = false,
                            modifier = Modifier.semantics { contentDescription = "Hue in degrees" }
                        )
                    }
                    Text("Presets", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        KlyroColors.extended.forEach { preset ->
                            val swatch = KlyroColors.create(colors.dark, preset.id)
                            Button(
                                onClick = {
                                    val hsv = FloatArray(3)
                                    val source = AndroidColor.parseColor(preset.sourceHex)
                                    AndroidColor.colorToHSV(source or -0x1000000, hsv)
                                    hue = hsv[0]
                                    saturation = hsv[1]
                                    brightness = hsv[2]
                                    alpha = 1f
                                    hexInput = "#%06X".format(source and 0xFFFFFF)
                                    pendingPreset = preset.id
                                },
                                modifier = Modifier.weight(1f).heightIn(min = KlyroUiMetrics.minTouch)
                                    .semantics {
                                        selected = pendingPreset == preset.id
                                        contentDescription = "Preview ${preset.label} preset"
                                    },
                                shape = RoundedCornerShape(KlyroUiMetrics.controlCorner),
                                border = if (pendingPreset == preset.id)
                                    androidx.compose.foundation.BorderStroke(2.dp, colors.foreground)
                                else null,
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(3.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = swatch.accent,
                                    contentColor = swatch.onAccent
                                )
                            ) {
                                Text(preset.label, fontSize = 12.sp, maxLines = 1)
                            }
                        }
                    }
                    TextButton(
                        onClick = { showAdvanced = true },
                        modifier = Modifier.heightIn(min = KlyroUiMetrics.minTouch)
                    ) { Text("Opacity & saved colors") }
                }
            },
            confirmButton = {
                Button(
                    enabled = pendingPreset != null || KlyroHexInput.isComplete(hexInput),
                    onClick = { onApply(pendingPreset ?: KlyroColors.customId(argb())) },
                    shape = RoundedCornerShape(KlyroUiMetrics.controlCorner)
                ) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        )
        if (showAdvanced && pendingFavoriteDeletion == null) {
            AlertDialog(
                onDismissRequest = { showAdvanced = false },
                containerColor = colors.sheet,
                shape = RoundedCornerShape(KlyroUiMetrics.modalCorner),
                title = { Text("Opacity & saved colors") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Opacity · " + (alpha * 100f).roundToInt() + "%",
                            style = MaterialTheme.typography.labelMedium)
                        KlyroThinSlider(
                            value = alpha,
                            onValueChange = { alpha = it },
                            valueRange = 0f..1f,
                            modifier = Modifier.fillMaxWidth().semantics {
                                contentDescription = "Opacity"
                            }
                        )
                        Text("My colors", style = MaterialTheme.typography.titleSmall)
                        OutlinedTextField(
                            value = favoriteName,
                            onValueChange = { favoriteName = it.take(22) },
                            label = { Text("Color name") },
                            supportingText = { Text("${22 - favoriteName.length} characters remaining") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                saveFavorites(KlyroFavoriteColors.save(
                                    favorites, "#%08X".format(argb()), favoriteName
                                ))
                            },
                            enabled = KlyroHexInput.isComplete(hexInput),
                            modifier = Modifier.fillMaxWidth()
                                .heightIn(min = KlyroUiMetrics.minTouch)
                        ) { Text("Save current color") }
                        favorites.forEach { favorite ->
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .heightIn(min = KlyroUiMetrics.minTouch),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceSm)
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
                                TextButton(
                                    modifier = Modifier.heightIn(min = KlyroUiMetrics.minTouch),
                                    onClick = {
                                    favoriteName = favorite.label
                                    val favArgb = AndroidColor.parseColor(favorite.hex)
                                    val hsv = FloatArray(3)
                                    AndroidColor.colorToHSV(favArgb or -0x1000000, hsv)
                                    hue = hsv[0]
                                    saturation = hsv[1]
                                    brightness = hsv[2]
                                    alpha = (favArgb ushr 24) / 255f
                                    hexInput = "#%06X".format(favArgb and 0xFFFFFF)
                                    pendingPreset = null
                                    showAdvanced = false
                                }) { Text("Edit") }
                                TextButton(
                                    modifier = Modifier.heightIn(min = KlyroUiMetrics.minTouch),
                                    onClick = { pendingFavoriteDeletion = favorite }
                                ) { Text("Remove", color = colors.danger) }
                            }
                        }

                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAdvanced = false },
                        modifier = Modifier.heightIn(min = KlyroUiMetrics.minTouch)) {
                        Text("Done")
                    }
                }
            )
        }
        pendingFavoriteDeletion?.let { favorite ->
            AlertDialog(
                onDismissRequest = { pendingFavoriteDeletion = null },
                shape = RoundedCornerShape(KlyroUiMetrics.modalCorner),
                containerColor = colors.sheet,
                title = { Text("Remove saved color?") },
                text = { Text("Remove \"${favorite.label}\" from your saved colors?") },
                confirmButton = {
                    TextButton(onClick = {
                        saveFavorites(favorites.filterNot { it.hex == favorite.hex })
                        pendingFavoriteDeletion = null
                    }) { Text("Remove", color = colors.danger) }
                },
                dismissButton = {
                    TextButton(onClick = { pendingFavoriteDeletion = null }) { Text("Cancel") }
                }
            )
        }
    }
}
