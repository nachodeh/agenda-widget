package com.flowmosaic.calendar

import android.Manifest
import android.R.attr
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.flowmosaic.calendar.analytics.FirebaseLogger
import com.flowmosaic.calendar.prefs.AgendaWidgetPrefs
import com.flowmosaic.calendar.remoteviews.EventsWidgetService
import com.google.firebase.ktx.Firebase


const val UPDATE_ACTION = "com.flowmosaic.calendar.broadcast.ACTION_UPDATE_WIDGET"
const val CREATE_ACTION = "com.flowmosaic.calendar.broadcast.ACTION_CREATE_EVENT"
const val CLICK_ACTION = "com.flowmosaic.calendar.CLICK_ACTION"
const val EXTRA_START_TIME = "com.flowmosaic.calendar.START_TIME"
const val EXTRA_END_TIME = "com.flowmosaic.calendar.END_TIME"
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
        if (AgendaWidgetPrefs.getShouldLogWidgetActivityEvent(context)) {
            FirebaseLogger.logWidgetLifecycleEvent(
                context, FirebaseLogger.WidgetStatus.ACTIVE, mapOf(
                    "number_of_widgets" to appWidgetIds.size.toString(),
                )
            )
            AgendaWidgetPrefs.setWidgetActivityEventLastLoggedTimestamp(context)
            FirebaseLogger.flushMixpanelEvents(context)
        }
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
                    Toast.makeText(context, R.string.agenda_widget_updated, Toast.LENGTH_SHORT).show()
                }

                FirebaseLogger.logActionButtonEvent(context, FirebaseLogger.ActionButton.REFRESH)
            }

            CREATE_ACTION -> {
                val intent = Intent(Intent.ACTION_INSERT).apply {
                    data = CalendarContract.Events.CONTENT_URI
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)

                FirebaseLogger.logActionButtonEvent(context, FirebaseLogger.ActionButton.ADD_EVENT)
            }

            CLICK_ACTION -> {
                val startTime: Long = intent.getLongExtra(EXTRA_START_TIME, 0)
                val endTime: Long = intent.getLongExtra(EXTRA_END_TIME, 0)
                val eventId: Long = intent.getLongExtra(EXTRA_EVENT_ID, 0)
                val builder: Uri.Builder = CalendarContract.CONTENT_URI.buildUpon()

                when {
                    eventId > 0 -> {
                        FirebaseLogger.logSelectItemEvent(
                            context,
                            FirebaseLogger.ScreenName.WIDGET,
                            FirebaseLogger.WidgetItemName.EVENT.itemName
                        )
                        builder.appendPath("events")
                        ContentUris.appendId(builder, eventId)
                    }

                    startTime > 0 -> {
                        FirebaseLogger.logSelectItemEvent(
                            context,
                            FirebaseLogger.ScreenName.WIDGET,
                            FirebaseLogger.WidgetItemName.DATE.itemName
                        )
                        builder.appendPath("time")
                        ContentUris.appendId(builder, startTime)
                    }

                    else -> {
                        builder.appendPath("time")
                        ContentUris.appendId(builder, System.currentTimeMillis())
                    }
                }

                val viewIntent = Intent(Intent.ACTION_VIEW)
                    .setData(builder.build())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTime)
                    .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime)

                context.startActivity(viewIntent)
            }
        }

        super.onReceive(context, intent)
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
        FirebaseLogger.logWidgetLifecycleEvent(context, FirebaseLogger.WidgetStatus.ENABLED)
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
        FirebaseLogger.logWidgetLifecycleEvent(context, FirebaseLogger.WidgetStatus.DISABLED)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        FirebaseLogger.logWidgetLifecycleEvent(context, FirebaseLogger.WidgetStatus.DELETED)
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
        if (hasCalendarPermission(context)) {
            renderCalendarWidget(context, appWidgetManager, widgetId)
        } else {
            showPermissionRequestView(context, appWidgetManager, widgetId)
        }
    }

    fun forceWidgetUpdate(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val widgetComponent = ComponentName(context, AgendaWidget::class.java)
        val widgetIds = appWidgetManager.getAppWidgetIds(widgetComponent)
        onUpdate(context, appWidgetManager, widgetIds)
    }


    private fun renderCalendarWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int
    ) {
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
//        val views = RemoteViews(context.packageName, R.layout.agenda_widget)
        val intent = Intent(context, EventsWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }

        val refreshIntent: PendingIntent = Intent(context, AgendaWidget::class.java).run {
            action = UPDATE_ACTION
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))

            val flags = PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            PendingIntent.getBroadcast(context, 0, this, flags)
        }

        val addEventIntent: PendingIntent = Intent(context, AgendaWidget::class.java).run {
            action = CREATE_ACTION
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))

            val flags = PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            PendingIntent.getBroadcast(context, 0, this, flags)
        }

        val views = RemoteViews(context.packageName, R.layout.agenda_widget).apply {
            setRemoteAdapter(R.id.events_list_view, intent)
            val backgroundColor = 0x000000
            val opacity = AgendaWidgetPrefs.getOpacity(context, widgetId.toString())
            val color = ColorUtils.setAlphaComponent(backgroundColor, (opacity * 255).toInt())
            setInt(R.id.main_view, "setBackgroundColor", color)
            setPendingIntentTemplate(R.id.events_list_view, toastPendingIntent)

            val actionButtonsVisible = if (AgendaWidgetPrefs.getShowActionButtons(context, widgetId.toString())) View.VISIBLE else View.GONE
            setViewVisibility(R.id.widget_action_buttons, actionButtonsVisible)

            val textColor = AgendaWidgetPrefs.getTextColor(context, widgetId.toString()).toArgb();

            setInt(R.id.refresh_button, "setColorFilter", textColor)
            setInt(R.id.add_button, "setColorFilter", textColor)

            setOnClickPendingIntent(R.id.refresh_button, refreshIntent)
            setOnClickPendingIntent(R.id.add_button, addEventIntent)

            setTextColor(R.id.empty_view, textColor)
            setEmptyView(R.id.events_list_view, R.id.empty_view)
        }

        appWidgetManager.notifyAppWidgetViewDataChanged(widgetId, R.id.events_list_view)
        appWidgetManager.updateAppWidget(widgetId, views)
    }


    private fun showPermissionRequestView(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.permission_request_widget)
        val appName = context.getString(R.string.app_name)
        val tapToSetupString = context.getString(R.string.tap_to_set_up, appName)
        views.setTextViewText(R.id.permission_request_text, tapToSetupString)

        val intent = Intent(context, MainActivity::class.java)
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, flags)
        views.setOnClickPendingIntent(R.id.permission_request_text, pendingIntent)

        appWidgetManager.updateAppWidget(widgetId, views)
    }

    private fun hasCalendarPermission(context: Context): Boolean {
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
