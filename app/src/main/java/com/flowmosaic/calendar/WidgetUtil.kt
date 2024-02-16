package com.flowmosaic.calendar

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context

class WidgetUtil {
    fun getAllWidgetIds(
        context: Context,
    ): IntArray {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, AgendaWidget::class.java)
        return appWidgetManager.getAppWidgetIds(componentName)
    }
}