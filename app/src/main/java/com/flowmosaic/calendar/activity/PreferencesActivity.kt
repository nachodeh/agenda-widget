package com.flowmosaic.calendar.activity

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import com.flowmosaic.calendar.analytics.AgendaWidgetLogger
import com.flowmosaic.calendar.ui.Header
import com.flowmosaic.calendar.ui.screens.PreferencesScreen
import com.flowmosaic.calendar.ui.theme.CalendarWidgetTheme
import com.flowmosaic.calendar.ui.theme.getPrimaryColor
import com.flowmosaic.calendar.widget.AgendaWidget

class PreferencesActivity : ComponentActivity() {

    private val logger by lazy { AgendaWidgetLogger(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CalendarWidgetTheme {
                val statusBarColor = getPrimaryColor().toArgb()
                enableEdgeToEdge(
                    navigationBarStyle = SystemBarStyle.light(
                        MaterialTheme.colorScheme.background.toArgb(),
                        MaterialTheme.colorScheme.background.toArgb()
                    ),
                    statusBarStyle = SystemBarStyle.dark(statusBarColor)
                )
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val appWidgetId = intent?.extras?.getInt(
                        AppWidgetManager.EXTRA_APPWIDGET_ID,
                        AppWidgetManager.INVALID_APPWIDGET_ID
                    ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

                    Column {
                        Header(
                            subtitle = "Preferences",
                        )
                        PreferencesScreen(
                            appWidgetId,
                            onCloseClick = { saveWidgetConfig(appWidgetId) })
                    }
                }
            }
        }
        logger.logActivityStartedEvent(AgendaWidgetLogger.Activity.PREFERENCES_ACTIVITY)
    }

    private fun saveWidgetConfig(appWidgetId: Int) {
        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(RESULT_OK, resultValue)
        finish()
    }

    override fun onPause() {
        val agendaWidgetProvider = AgendaWidget()
        agendaWidgetProvider.forceWidgetUpdate(applicationContext)

        super.onPause()
    }

}
