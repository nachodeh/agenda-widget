package com.flowmosaic.calendar.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object CalendarPermissionsChecker {

    fun hasCalendarPermission(context: Context): Boolean {
        val readCalendarPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        )
        val writeCalendarPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CALENDAR
        )
        return readCalendarPermission == PackageManager.PERMISSION_GRANTED &&
                writeCalendarPermission == PackageManager.PERMISSION_GRANTED
    }

}