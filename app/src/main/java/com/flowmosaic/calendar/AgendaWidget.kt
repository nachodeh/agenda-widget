package com.flowmosaic.calendar

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import android.widget.RemoteViews
import com.flowmosaic.calendar.remoteviews.EventsWidgetService

const val UPDATE_ACTION = "com.flowmosaic.calendar.broadcast.ACTION_UPDATE_WIDGET"
const val CLICK_ACTION = "com.flowmosaic.calendar.CLICK_ACTION"
const val EXTRA_DATE = "com.flowmosaic.calendar.DATE"
const val EXTRA_EVENT_ID = "com.flowmosaic.calendar.EVENT_ID"

/**
 * Implementation of App Widget functionality.
 */
class AgendaWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            UPDATE_ACTION -> {
                val widgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID
                )

                if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    updateWidget(context, AppWidgetManager.getInstance(context), widgetId)
                }
            }
            CLICK_ACTION -> {
                val time: Long = intent.getLongExtra(EXTRA_DATE, 0)
                val eventId: Long = intent.getLongExtra(EXTRA_EVENT_ID, 0)

                val builder: Uri.Builder = CalendarContract.CONTENT_URI.buildUpon()

                when {
                    time > 0 -> {
                        builder.appendPath("time")
                        ContentUris.appendId(builder, time)
                    }
                    eventId > 0 -> {
                        builder.appendPath("events")
                        ContentUris.appendId(builder, eventId)
                    }
                    else -> {
                        builder.appendPath("time")
                        ContentUris.appendId(builder, System.currentTimeMillis())
                    }
                }

                val viewIntent = Intent(Intent.ACTION_VIEW)
                    .setData(builder.build())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                context.startActivity(viewIntent)
            }
        }

        super.onReceive(context, intent)
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.agenda_widget)
        val intent = Intent(context, EventsWidgetService::class.java)
        views.setRemoteAdapter(R.id.events_list_view, intent)

        val toastPendingIntent: PendingIntent = Intent(
            context,
            AgendaWidget::class.java
        ).run {
            action = CLICK_ACTION
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))

            val flags = PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            PendingIntent.getBroadcast(context, 0, this, flags)
        }
        views.setPendingIntentTemplate(R.id.events_list_view, toastPendingIntent)

        appWidgetManager.notifyAppWidgetViewDataChanged(widgetId, R.id.events_list_view)
        appWidgetManager.updateAppWidget(widgetId, views)
    }

    fun forceWidgetUpdate(context: Context) {
        Log.d("nachodehlog", "forceWidgetUpdate")
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val widgetComponent = ComponentName(context, AgendaWidget::class.java)
        val widgetIds = appWidgetManager.getAppWidgetIds(widgetComponent)
        onUpdate(context, appWidgetManager, widgetIds)
    }

}
