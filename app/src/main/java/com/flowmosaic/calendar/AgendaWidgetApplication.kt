package com.flowmosaic.calendar

import android.app.Application
import com.flowmosaic.calendar.analytics.FirebaseLogger

class AgendaWidgetApplication: Application() {
    override fun onCreate() {
        super.onCreate()

        FirebaseLogger.getAmplitudeInstance(applicationContext)
    }
}