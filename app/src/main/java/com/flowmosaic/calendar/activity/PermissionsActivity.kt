package com.flowmosaic.calendar.activity

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.flowmosaic.calendar.analytics.AgendaWidgetLogger
import com.flowmosaic.calendar.prefs.AgendaWidgetPrefs
import com.flowmosaic.calendar.widget.AgendaWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PermissionsActivity : ComponentActivity() {

    private val logger by lazy { AgendaWidgetLogger(applicationContext) }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            permissions ->
            val allPermissionsGranted = permissions.entries.all { it.value }
            logger.logPermissionsResultEvent(allPermissionsGranted)
            // After calendar permissions, request battery optimization exemption
            requestBatteryOptimizationExemption()
        }

    private val batteryOptimizationLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // Whether granted or not, proceed to update widgets
            updateWidgets()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        requestCalendarPermissions()

        logger.logActivityStartedEvent(AgendaWidgetLogger.Activity.PERMISSIONS_ACTIVITY)
    }

    private fun requestCalendarPermissions() {
        // Check if permissions are already granted
        val writeCalendarPermission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        val readCalendarPermission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!writeCalendarPermission || !readCalendarPermission) {
            requestPermissionLauncher.launch(
                arrayOf(Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR)
            )
        } else {
            // Calendar permissions already granted, check battery optimization
            requestBatteryOptimizationExemption()
        }
    }

    private fun requestBatteryOptimizationExemption() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            val intent =
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
            batteryOptimizationLauncher.launch(intent)
        } else {
            // Already exempted, proceed to update widgets
            updateWidgets()
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
