package com.flowmosaic.calendar

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
                    RequestPermissionsScreen()
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
private fun RequestPermissionsScreen() {
    val calendarPermissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.WRITE_CALENDAR,
            Manifest.permission.READ_CALENDAR,
        )
    )

    if (calendarPermissionsState.allPermissionsGranted) {
        PreferencesScreen()
    } else {
        Column {
            Text(text = "This widget requires calendar permissions")
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { calendarPermissionsState.launchMultiplePermissionRequest() }) {
                Text("Request Permissions")
            }
        }
    }
}
