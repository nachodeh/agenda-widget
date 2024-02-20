package com.flowmosaic.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.simulateHotReload
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.flowmosaic.calendar.R
import com.flowmosaic.calendar.ui.theme.OnPrimary
import com.flowmosaic.calendar.ui.theme.Primary

@Composable
fun Header(subtitle: String = "") {
    val context = LocalContext.current
    val primary = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.inversePrimary else MaterialTheme.colorScheme.primary
    val onPrimary = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary
    Column (
        modifier = Modifier
            .fillMaxWidth()
            .background(primary)
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 20.dp),
    ) {
        Text(
            text = context.getString(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            color = onPrimary
        )
        if (subtitle.isNotEmpty()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.titleSmall,
                color = onPrimary,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 8.dp,
                    )
            )
        }
    }
}
