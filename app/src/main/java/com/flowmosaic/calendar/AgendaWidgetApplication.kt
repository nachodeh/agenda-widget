package com.flowmosaic.calendar

import android.app.Application
import com.flowmosaic.calendar.analytics.AgendaWidgetLogger

class AgendaWidgetApplication: Application() {
    override fun onCreate() {
        super.onCreate()

        AgendaWidgetLogger.getAmplitudeInstance(applicationContext)
    }
}