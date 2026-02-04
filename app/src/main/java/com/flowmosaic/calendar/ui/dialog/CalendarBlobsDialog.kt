package com.flowmosaic.calendar.ui.dialog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.flowmosaic.calendar.R
import com.flowmosaic.calendar.analytics.AgendaWidgetLogger
import com.flowmosaic.calendar.data.CalendarData
import com.flowmosaic.calendar.data.CalendarFetcher
import com.flowmosaic.calendar.prefs.AgendaWidgetPrefs
import com.flowmosaic.calendar.ui.getCommonEmoji

@Composable
fun ShowCalendarBlobsDialog(
    openDialog: MutableState<Boolean>,
    widgetId: String,
    logger: AgendaWidgetLogger
) {
    val context = LocalContext.current
    val prefs = AgendaWidgetPrefs(context)

    val calendarFetcher = CalendarFetcher()
    val calendarList = remember { mutableStateListOf<CalendarData>() }
    val indicatorStyle = remember { mutableStateOf(prefs.getIndicatorStyle(widgetId)) }

    LaunchedEffect(Unit) { calendarList.addAll(calendarFetcher.queryCalendarData(context)) }

    var colors =
        IntArray(calendarList.size) { index ->
            prefs.getCalendarColor(widgetId, calendarList[index].id).toArgb()
        }

    var emojis =
        Array(calendarList.size) { index ->
            prefs.getCalendarEmoji(widgetId, calendarList[index].id)
        }

    Dialog(
        onDismissRequest = { openDialog.value = false },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        content = {
            CalendarBlobsDialogContent(
                calendarList = calendarList,
                indicatorStyle = indicatorStyle,
                colors = colors,
                onColorChange = { index, colorArgb -> colors[index] = colorArgb },
                emojis = emojis,
                onEmojiChange = { index, emoji -> emojis[index] = emoji },
                onSaveClick = {
                    prefs.setIndicatorStyle(indicatorStyle.value, widgetId)
                    colors.forEachIndexed { index, colorArgb ->
                        val calendarId = calendarList[index].id
                        prefs.setCalendarColorArgb(colorArgb, widgetId, calendarId)
                    }
                    emojis.forEachIndexed { index, emoji ->
                        val calendarId = calendarList[index].id
                        prefs.setCalendarEmoji(emoji, widgetId, calendarId)
                    }
                    openDialog.value = false
                },
                onCancelClick = { openDialog.value = false },
                logger = logger
            )
        }
    )
}

@Composable
private fun CalendarBlobsDialogContent(
    calendarList: List<CalendarData>,
    indicatorStyle: MutableState<AgendaWidgetPrefs.IndicatorStyle>,
    colors: IntArray,
    onColorChange: (Int, Int) -> Unit,
    emojis: Array<String>,
    onEmojiChange: (Int, String) -> Unit,
    onSaveClick: () -> Unit,
    onCancelClick: () -> Unit,
    logger: AgendaWidgetLogger
) {
    Surface(
        modifier =
            Modifier.padding(16.dp)
                .wrapContentSize()
                .heightIn(min = 100.dp, max = 500.dp)
                .widthIn(min = 100.dp, max = 500.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.align(Alignment.TopStart).padding(bottom = 60.dp)) {
                // Indicator style selector
                Text(
                    text = stringResource(R.string.indicator_style),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                IndicatorStyleSelector(
                    selectedStyle = indicatorStyle.value,
                    onStyleSelected = { indicatorStyle.value = it }
                )

                // Calendar list
                LazyColumn(modifier = Modifier.padding(top = 16.dp)) {
                    itemsIndexed(calendarList) { index, calendar ->
                        if (index < colors.size && index < emojis.size) {
                            CalendarBlobRow(
                                calendarName = calendar.name,
                                indicatorStyle = indicatorStyle.value,
                                colorArgb = colors[index],
                                onColorChange = { colorArgb -> onColorChange(index, colorArgb) },
                                emoji = emojis[index],
                                onEmojiChange = { emoji -> onEmojiChange(index, emoji) },
                                logger = logger
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = onSaveClick) { Text(text = stringResource(id = R.string.save)) }
                Button(
                    onClick = onCancelClick,
                    colors = ButtonDefaults.textButtonColors(),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IndicatorStyleSelector(
    selectedStyle: AgendaWidgetPrefs.IndicatorStyle,
    onStyleSelected: (AgendaWidgetPrefs.IndicatorStyle) -> Unit
) {
    val options =
        listOf(
            AgendaWidgetPrefs.IndicatorStyle.COLORS to stringResource(R.string.style_colors),
            AgendaWidgetPrefs.IndicatorStyle.EMOJIS to stringResource(R.string.style_emojis)
        )

    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (style, label) ->
            SegmentedButton(
                selected = selectedStyle == style,
                onClick = { onStyleSelected(style) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
            ) {
                Text(label)
            }
        }
    }
}

@Composable
private fun CalendarBlobRow(
    calendarName: String,
    indicatorStyle: AgendaWidgetPrefs.IndicatorStyle,
    colorArgb: Int,
    onColorChange: (Int) -> Unit,
    emoji: String,
    onEmojiChange: (String) -> Unit,
    logger: AgendaWidgetLogger
) {
    val context = LocalContext.current
    val colorState = remember { mutableStateOf(Color(colorArgb)) }
    val emojiState = remember { mutableStateOf(emoji) }
    val showColorDialog = remember { mutableStateOf(false) }
    val showEmojiDialog = remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = calendarName, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))

        when (indicatorStyle) {
            AgendaWidgetPrefs.IndicatorStyle.COLORS -> {
                // Color circle that opens color picker
                Box(
                    modifier =
                        Modifier.size(28.dp)
                            .clip(CircleShape)
                            .background(colorState.value)
                            .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .clickable {
                                showColorDialog.value = true
                                logger.logUpdatePrefEvent(
                                    AgendaWidgetLogger.PrefsScreenItemName.CALENDAR_COLOR
                                )
                            }
                )
            }
            AgendaWidgetPrefs.IndicatorStyle.EMOJIS -> {
                // Emoji text or "None" that opens emoji picker
                Box(
                    modifier =
                        Modifier.clickable {
                                showEmojiDialog.value = true
                                logger.logUpdatePrefEvent(
                                    AgendaWidgetLogger.PrefsScreenItemName.CALENDAR_EMOJI
                                )
                            }
                            .padding(4.dp)
                ) {
                    if (emojiState.value.isEmpty()) {
                        Text(
                            text = context.getString(R.string.label_none),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Text(text = emojiState.value, style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
    }

    // Color picker dialog
    if (showColorDialog.value) {
        ColorDialog(
            colorList =
                listOf(
                    Color(0xFFEF5350),
                    Color(0xFFEC407A),
                    Color(0xFFAB47BC),
                    Color(0xFF7E57C2),
                    Color(0xFF5C6BC0),
                    Color(0xFF42A5F5),
                    Color(0xFF29B6F6),
                    Color(0xFF26C6DA),
                    Color(0xFF26A69A),
                    Color(0xFF66BB6A),
                    Color(0xFF9CCC65),
                    Color(0xFFD4E157),
                    Color(0xFFFFEE58),
                    Color(0xFFFFCA28),
                    Color(0xFFFFA726),
                    Color(0xFFFF7043),
                    Color(0xFF8D6E63),
                    Color(0xFF78909C),
                ),
            onDismiss = { showColorDialog.value = false },
            currentlySelected = colorState.value,
            onColorSelected = { newColor ->
                colorState.value = newColor
                onColorChange(newColor.toArgb())
            }
        )
    }

    // Emoji picker dialog
    if (showEmojiDialog.value) {
        EmojiDialog(
            emojiList = getCommonEmoji(),
            onDismiss = { showEmojiDialog.value = false },
            selectedEmoji = emojiState.value,
            onEmojiSelected = { newEmoji ->
                emojiState.value = newEmoji
                onEmojiChange(newEmoji)
            }
        )
    }
}
