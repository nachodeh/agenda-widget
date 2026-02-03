package com.flowmosaic.calendar.activity

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

        // Detect system dark mode
        val isDarkMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

        // Enable edge-to-edge before setContent for SDK 35+ compatibility
        // Status bar: always use light icons (white) since primary color is always dark blue
        // Navigation bar: dark icons for light backgrounds, light icons for dark backgrounds
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = if (isDarkMode) {
                // Dark mode: light (white) icons for dark background
                SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT
                )
            } else {
                // Light mode: dark (black) icons for light background
                SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            }
        )

        setContent {
            CalendarWidgetTheme {
                val primaryColor = getPrimaryColor()
                val backgroundColor = MaterialTheme.colorScheme.background

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backgroundColor)
                ) {
                    // Status bar background - extends behind status bar with primary color
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsTopHeight(WindowInsets.statusBars)
                            .background(primaryColor)
                    )

                    // Navigation bar background - extends behind navigation bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsBottomHeight(WindowInsets.navigationBars)
                            .background(backgroundColor)
                            .align(Alignment.BottomCenter)
                    )

                    // Main content with safe drawing padding
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .safeDrawingPadding(),
                        color = backgroundColor
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
