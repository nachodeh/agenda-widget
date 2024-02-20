package com.flowmosaic.calendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import com.flowmosaic.calendar.ui.screens.RequestPermissionsScreen
import com.flowmosaic.calendar.ui.theme.CalendarWidgetTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            CalendarWidgetTheme {
                enableEdgeToEdge(navigationBarStyle = SystemBarStyle.light(
                    MaterialTheme.colorScheme.background.toArgb(),
                    MaterialTheme.colorScheme.background.toArgb()
                ))
                Surface(
                    modifier = Modifier.fillMaxSize().safeDrawingPadding(),
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

