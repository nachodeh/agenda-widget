package com.flowmosaic.calendar.ui.dialog

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun IconDialog(
    iconList: List<Int>,
    onDismiss: (() -> Unit),
    selectedIcon: Int,
    onIconSelected: ((Int) -> Unit)
) {
    val gridState = rememberLazyGridState()

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
                    .heightIn(min = 100.dp, max = 500.dp)
                    .widthIn(min = 100.dp, max = 500.dp),
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    state = gridState,
                    modifier = Modifier.padding(16.dp),
                ) {
                    items(iconList.size) { index ->
                        val icon = iconList[index]

                        // Add a border around the selected icon
                        var borderWidth = 0.dp
                        if (selectedIcon == index) {
                            borderWidth = 2.dp
                        }

                        Surface(Modifier
                            .aspectRatio(1.0f)
                            .padding(8.dp)) {
                            Surface(Modifier
                                .fillMaxWidth(1.0f)
                                .fillMaxHeight(1.0f)
                                .border(
                                    borderWidth,
                                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                                    RoundedCornerShape(5.dp)
                                )) {
                                // @todo change this to a calendar icon which does the colour for us and used in the view
                                Icon(
                                    imageVector = ImageVector.vectorResource(icon),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .fillMaxWidth(1.0f)
                                        .fillMaxHeight(1.0f)
                                        .padding(8.dp)
                                        .clickable {
                                            onIconSelected(index)
                                            onDismiss()
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}