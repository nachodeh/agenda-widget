package com.flowmosaic.calendar

import android.Manifest
import android.appwidget.AppWidgetManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.flowmosaic.calendar.analytics.FirebaseLogger
import com.flowmosaic.calendar.ui.Header
import com.flowmosaic.calendar.ui.PreferencesScreen
import com.flowmosaic.calendar.ui.theme.CalendarWidgetTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalendarWidgetTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val appWidgetId = intent?.extras?.getInt(
                        AppWidgetManager.EXTRA_APPWIDGET_ID,
                        AppWidgetManager.INVALID_APPWIDGET_ID
                    ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
                    RequestPermissionsScreen(appWidgetId)
                }
            }
        }
    }

    override fun onPause() {
        val agendaWidgetProvider = AgendaWidget()
        agendaWidgetProvider.forceWidgetUpdate(applicationContext)

        super.onPause()
    }

}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun RequestPermissionsScreen(appWidgetId: Int) {
    val context = LocalContext.current
    val calendarPermissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.WRITE_CALENDAR,
            Manifest.permission.READ_CALENDAR,
        )
    )

    if (calendarPermissionsState.allPermissionsGranted) {
        FirebaseLogger.logScreenShownEvent(
            context,
            FirebaseLogger.ScreenName.PREFS,
        )
        PreferencesScreen(appWidgetId)
    } else {
        FirebaseLogger.logScreenShownEvent(
            context,
            FirebaseLogger.ScreenName.REQUEST_PERMISSIONS,
        )
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Header()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(id = R.string.calendar_access_info),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        calendarPermissionsState.launchMultiplePermissionRequest()
                        FirebaseLogger.logSelectItemEvent(
                            context,
                            FirebaseLogger.ScreenName.REQUEST_PERMISSIONS,
                            FirebaseLogger.RequestPermissionsItemName.REQUEST_PERMISSIONS_BUTTON.itemName
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
