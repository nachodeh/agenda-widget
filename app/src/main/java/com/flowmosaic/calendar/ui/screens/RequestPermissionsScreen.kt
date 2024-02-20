package com.flowmosaic.calendar.ui.screens

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.flowmosaic.calendar.R
import com.flowmosaic.calendar.analytics.AgendaWidgetLogger
import com.flowmosaic.calendar.ui.Header
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestPermissionsScreen(navHostController: NavHostController) {
    val context = LocalContext.current
    val calendarPermissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.WRITE_CALENDAR,
            Manifest.permission.READ_CALENDAR,
        )
    )

    if (calendarPermissionsState.allPermissionsGranted) {
        AgendaWidgetLogger.logScreenShownEvent(
            context,
            AgendaWidgetLogger.ScreenName.PREFS,
        )
        WidgetsListView(navHostController)
    } else {
        AgendaWidgetLogger.logScreenShownEvent(
            context,
            AgendaWidgetLogger.ScreenName.REQUEST_PERMISSIONS,
        )
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Header()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(id = R.string.calendar_access_info),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        calendarPermissionsState.launchMultiplePermissionRequest()
                        AgendaWidgetLogger.logSelectItemEvent(
                            context,
                            AgendaWidgetLogger.ScreenName.REQUEST_PERMISSIONS,
                            AgendaWidgetLogger.RequestPermissionsItemName.REQUEST_PERMISSIONS_BUTTON.itemName
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(8.dp)
                        ),
                ) {
                    Text(
                        text = stringResource(id = R.string.grant_permissions),
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}