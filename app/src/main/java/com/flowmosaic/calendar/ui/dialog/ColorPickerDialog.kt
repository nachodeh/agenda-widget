package com.flowmosaic.calendar.ui.dialog

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ColorDialog(
    colorList: List<Color>,
    onDismiss: (() -> Unit),
    currentlySelected: Color,
    onColorSelected: ((Color) -> Unit)
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var currentColor by remember { mutableStateOf(currentlySelected) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        content = {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .wrapContentSize()
                    .padding(16.dp)
                    .widthIn(min = 280.dp, max = 400.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    TabRow(selectedTabIndex = selectedTabIndex) {
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 },
                            text = { Text("Presets") }
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 },
                            text = { Text("Spectrum") }
                        )
                        Tab(
                            selected = selectedTabIndex == 2,
                            onClick = { selectedTabIndex = 2 },
                            text = { Text("Hex") }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    when (selectedTabIndex) {
                        0 -> PresetColorsTab(
                            colorList = colorList,
                            currentlySelected = currentColor,
                            onColorSelected = { color ->
                                currentColor = color
                                onColorSelected(color)
                                onDismiss()
                            }
                        )
                        1 -> SpectrumTab(
                            currentColor = currentColor,
                            onColorChanged = { currentColor = it },
                            onColorSelected = {
                                onColorSelected(currentColor)
                                onDismiss()
                            }
                        )
                        2 -> HexInputTab(
                            currentColor = currentColor,
                            onColorChanged = { currentColor = it },
                            onColorSelected = {
                                onColorSelected(currentColor)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun PresetColorsTab(
    colorList: List<Color>,
    currentlySelected: Color,
    onColorSelected: (Color) -> Unit
) {
    val gridState = rememberLazyGridState()

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        state = gridState,
        modifier = Modifier.heightIn(min = 100.dp, max = 350.dp),
    ) {
        items(colorList.size) { index ->
            val color = colorList[index]
            var borderWidth = 0.dp
            if (currentlySelected == color) {
                borderWidth = 2.dp
            }

            Canvas(modifier = Modifier
                .padding(vertical = 16.dp, horizontal = 8.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(
                    borderWidth,
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                    RoundedCornerShape(20.dp)
                )
                .background(color)
                .requiredSize(70.dp)
                .clickable { onColorSelected(color) }
                .semantics {
                    contentDescription = "Color swatch: ${color.toArgb().toString(16)}"
                }
            ) {}
        }
    }
}

@Composable
private fun SpectrumTab(
    currentColor: Color,
    onColorChanged: (Color) -> Unit,
    onColorSelected: () -> Unit
) {
    var hue by remember { mutableFloatStateOf(getHueFromColor(currentColor)) }
    var saturation by remember { mutableFloatStateOf(getSaturationFromColor(currentColor)) }
    var brightness by remember { mutableFloatStateOf(getBrightnessFromColor(currentColor)) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Color preview
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(currentColor)
                .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Hue spectrum bar
        Text("Hue", style = MaterialTheme.typography.labelMedium)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.hsv(0f, 1f, 1f),
                            Color.hsv(60f, 1f, 1f),
                            Color.hsv(120f, 1f, 1f),
                            Color.hsv(180f, 1f, 1f),
                            Color.hsv(240f, 1f, 1f),
                            Color.hsv(300f, 1f, 1f),
                            Color.hsv(360f, 1f, 1f)
                        )
                    )
                )
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        hue = (offset.x / size.width * 360f).coerceIn(0f, 360f)
                        val newColor = Color.hsv(hue, saturation, brightness)
                        onColorChanged(newColor)
                    }
                }
        ) {
            // Indicator
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = ((hue / 360f) * 100).dp)
            )
        }
        Slider(
            value = hue,
            onValueChange = {
                hue = it
                onColorChanged(Color.hsv(hue, saturation, brightness))
            },
            valueRange = 0f..360f,
            modifier = Modifier.fillMaxWidth()
        )

        // Saturation slider
        Text("Saturation", style = MaterialTheme.typography.labelMedium)
        Slider(
            value = saturation,
            onValueChange = {
                saturation = it
                onColorChanged(Color.hsv(hue, saturation, brightness))
            },
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth()
        )

        // Brightness slider
        Text("Brightness", style = MaterialTheme.typography.labelMedium)
        Slider(
            value = brightness,
            onValueChange = {
                brightness = it
                onColorChanged(Color.hsv(hue, saturation, brightness))
            },
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onColorSelected,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Select")
        }
    }
}

@Composable
private fun HexInputTab(
    currentColor: Color,
    onColorChanged: (Color) -> Unit,
    onColorSelected: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var hexText by remember { mutableStateOf(colorToHex(currentColor)) }
    var isError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Color preview
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(currentColor)
                .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = hexText,
            onValueChange = { input ->
                val filtered = input.uppercase().filter { it in "0123456789ABCDEF#" }
                hexText = filtered
                parseHexColor(filtered)?.let { color ->
                    isError = false
                    onColorChanged(color)
                } ?: run {
                    isError = filtered.isNotEmpty() && !filtered.startsWith("#")
                }
            },
            label = { Text("Hex color (e.g. #FF5500)") },
            isError = isError,
            supportingText = if (isError) {{ Text("Invalid hex color") }} else null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    parseHexColor(hexText)?.let {
                        onColorSelected()
                    }
                }
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Quick hex presets
        Text("Quick presets:", style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("#FFFFFF", "#000000", "#FF0000", "#00FF00", "#0000FF").forEach { hex ->
                TextButton(onClick = {
                    hexText = hex
                    parseHexColor(hex)?.let { onColorChanged(it) }
                }) {
                    Text(hex, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onColorSelected,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isError && hexText.isNotEmpty()
        ) {
            Text("Select")
        }
    }
}

private fun colorToHex(color: Color): String {
    val argb = color.toArgb()
    return String.format("#%06X", argb and 0xFFFFFF)
}

private fun parseHexColor(hex: String): Color? {
    return try {
        val cleanHex = hex.removePrefix("#")
        when (cleanHex.length) {
            6 -> Color(android.graphics.Color.parseColor("#$cleanHex"))
            8 -> Color(android.graphics.Color.parseColor("#$cleanHex"))
            3 -> {
                // Short hex format (e.g., #FFF -> #FFFFFF)
                val expanded = cleanHex.map { "$it$it" }.joinToString("")
                Color(android.graphics.Color.parseColor("#$expanded"))
            }
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}

private fun getHueFromColor(color: Color): Float {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    return hsv[0]
}

private fun getSaturationFromColor(color: Color): Float {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    return hsv[1]
}

private fun getBrightnessFromColor(color: Color): Float {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    return hsv[2]
}