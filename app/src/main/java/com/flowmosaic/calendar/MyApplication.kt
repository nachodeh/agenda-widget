package com.flowmosaic.calendar

import android.app.Application

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Force a widget update
        val agendaWidgetProvider = AgendaWidget()
        agendaWidgetProvider.forceWidgetUpdate(applicationContext)
    }

}