package com.flowmosaic.calendar.broadcast

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import com.flowmosaic.calendar.AgendaWidget
import com.flowmosaic.calendar.UPDATE_ACTION

class WidgetUpdater {

    fun updateWidgets(context: Context) {
        Log.d("nachodehlog", "WidgetUpdater.updateWidgets")
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val widgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, AgendaWidget::class.java))

        for (widgetId in widgetIds) {
            Log.d("nachodehlog", "Sending widget update intent for id $widgetId")
            val intent = Intent(context, AgendaWidget::class.java)
            intent.action = UPDATE_ACTION
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            context.sendBroadcast(intent)
        }
    }

}