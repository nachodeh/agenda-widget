package com.flowmosaic.calendar.ui.dialog

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun EmojiDialog(
    emojiList: List<String>,
    onDismiss: (() -> Unit),
    selectedEmoji: String,
    onEmojiSelected: ((String) -> Unit)
) {
    val gridState = rememberLazyGridState()
    // Add empty string as first option for "None"
    val optionsWithNone = listOf("") + emojiList

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        content = {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier =
                    Modifier.wrapContentSize()
                        .padding(16.dp)
                        .heightIn(min = 100.dp, max = 500.dp)
                        .widthIn(min = 100.dp, max = 500.dp),
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    state = gridState,
                    modifier = Modifier.padding(16.dp),
                ) {
                    items(optionsWithNone.size) { index ->
                        val emoji = optionsWithNone[index]

                        // Add a border around the selected emoji
                        val borderWidth = if (selectedEmoji == emoji) 2.dp else 0.dp

                        Surface(Modifier.aspectRatio(1.0f).padding(4.dp)) {
                            Surface(
                                Modifier.fillMaxWidth(1.0f)
                                    .fillMaxHeight(1.0f)
                                    .border(
                                        borderWidth,
                                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                                        RoundedCornerShape(5.dp)
                                    )
                                    .clickable {
                                        onEmojiSelected(emoji)
                                        onDismiss()
                                    }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxWidth().fillMaxHeight()
                                ) {
                                    if (emoji.isEmpty()) {
                                        // "None" option
                                        Text(
                                            text = "✕",
                                            fontSize = 20.sp,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        Text(
                                            text = emoji,
                                            fontSize = 24.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}
