package com.flowmosaic.calendar

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
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
        WidgetsListView()
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

@Composable
private fun WidgetsListView() {
    val context = LocalContext.current

    val widgetIds = remember {
        mutableStateOf(
            AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, AgendaWidget::class.java))
        )
    }

    LifecycleResumeEffect(Unit) {
        widgetIds.value = AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, AgendaWidget::class.java))

        onPauseOrDispose {}
    }

    if (widgetIds.value.isEmpty()) {
        PreferencesScreen(appWidgetId = 0)
    } else {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Header(subtitle = context.getString(R.string.active_widgets))
            Spacer(modifier = Modifier.height(8.dp))
            widgetIds.value.forEachIndexed { index, id ->
                val idx = index + 1;
                LaunchWidgetConfigButton(id = id, text = "Widget $idx")
            }
            LaunchWidgetConfigButton(id = 0, text = context.getString(R.string.default_configuration))
        }
    }
}

@Composable
private fun LaunchWidgetConfigButton(id: Int, text: String) {
    val context = LocalContext.current
    Button(
        onClick = {
            val intent = Intent(context, PreferencesActivity::class.java)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            context.startActivity(intent)
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)

    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp)
        )
    }
}

