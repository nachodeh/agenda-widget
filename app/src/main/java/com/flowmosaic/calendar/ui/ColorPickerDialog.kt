package com.flowmosaic.calendar.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun ColorDialog(
    colorList: List<Color>,
    onDismiss: (() -> Unit),
    currentlySelected: Color,
    onColorSelected: ((Color) -> Unit)
) {
    val gridState = rememberLazyGridState()

    Dialog(
        onDismissRequest = onDismiss,
//        properties = DialogProperties(usePlatformDefaultWidth = false),
        content = {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.padding(8.dp).size(320.dp, 420.dp),
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    state = gridState,
                ) {
                    items(colorList.size) { index ->
                        val color = colorList[index]
                        // Add a border around the selected colour only
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
                            .clickable {
                                onColorSelected(color)
                                onDismiss()
                            }
                        ) {
                        }
                    }
                }
            }
        }
    )
}