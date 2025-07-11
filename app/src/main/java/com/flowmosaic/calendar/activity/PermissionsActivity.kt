package com.flowmosaic.calendar.activity

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.flowmosaic.calendar.analytics.AgendaWidgetLogger
import com.flowmosaic.calendar.data.CalendarFetcher
import com.flowmosaic.calendar.prefs.AgendaWidgetPrefs
import com.flowmosaic.calendar.widget.AgendaWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PermissionsActivity : ComponentActivity() {

    private val logger by lazy { AgendaWidgetLogger(applicationContext) }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allPermissionsGranted = permissions.entries.all { it.value }
            logger.logPermissionsResultEvent(allPermissionsGranted)
            updateWidgets()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestCalendarPermissions()

        setContent {
            enableEdgeToEdge()
        }

        logger.logActivityStartedEvent(AgendaWidgetLogger.Activity.PERMISSIONS_ACTIVITY)
    }

    private fun requestCalendarPermissions() {
        // Check if permissions are already granted
        val writeCalendarPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_CALENDAR
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val readCalendarPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_CALENDAR
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!writeCalendarPermission || !readCalendarPermission) {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.WRITE_CALENDAR,
                    Manifest.permission.READ_CALENDAR
                )
            )
        } else {
            finishAndRemoveTask()
        }
    }

    private fun updateWidgets() {
        lifecycleScope.launch(Dispatchers.Main) {
            // Update the calendars stored on prefs for first fetch
            AgendaWidgetPrefs(applicationContext).initSelectedCalendars(applicationContext)
            AgendaWidget().forceWidgetUpdate(applicationContext)
            finishAndRemoveTask()
        }
    }
}