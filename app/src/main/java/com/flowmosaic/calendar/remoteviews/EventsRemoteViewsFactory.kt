package com.flowmosaic.calendar.remoteviews

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.compose.ui.graphics.toArgb
import com.flowmosaic.calendar.widget.EXTRA_END_TIME
import com.flowmosaic.calendar.widget.EXTRA_EVENT_ID
import com.flowmosaic.calendar.widget.EXTRA_START_TIME
import com.flowmosaic.calendar.R
import com.flowmosaic.calendar.data.CalendarDateUtils
import com.flowmosaic.calendar.data.CalendarFetcher
import com.flowmosaic.calendar.data.CalendarViewItem
import com.flowmosaic.calendar.prefs.AgendaWidgetPrefs
import java.util.Locale

class EventsRemoteViewsFactory(private val context: Context, intent: Intent) :
    RemoteViewsService.RemoteViewsFactory {

    private var widgetId = ""

    init {
        if (intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) {
            widgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            ).toString()
        }
    }

    private val calendarFetcher = CalendarFetcher()
    private val events: MutableList<CalendarViewItem> = mutableListOf()

    override fun onCreate() {
        events.clear()
        events.addAll(getEvents())
    }

    override fun onDataSetChanged() {
        events.clear()
        events.addAll(getEvents())
    }

    override fun onDestroy() {
        events.clear()
    }

    override fun getCount(): Int {
        return events.size
    }

    private fun isColorLight(color: Int): Boolean {
        val red = Color.red(color) / 255.0
        val green = Color.green(color) / 255.0
        val blue = Color.blue(color) / 255.0

        val luminance = 0.2126 * red + 0.7152 * green + 0.0722 * blue
        return luminance > 0.5
    }

    override fun getViewAt(position: Int): RemoteViews {
        val item = events[position]

        val textColor = AgendaWidgetPrefs.getTextColor(context, widgetId).toArgb()
        val isColorLight = isColorLight(textColor)

        val layoutResId = when (item) {
            is CalendarViewItem.Day -> if (isColorLight) R.layout.item_date else R.layout.item_date_dark
            is CalendarViewItem.Event -> if (isColorLight) R.layout.item_event else R.layout.item_event_dark
        }

        return RemoteViews(context.packageName, layoutResId).apply {
            val defaultTextSizeSp = when (item) {
                is CalendarViewItem.Day -> 16f
                is CalendarViewItem.Event -> 14f
            }
            val textViewId = when (item) {
                is CalendarViewItem.Day -> if (isColorLight) R.id.item_date_text_view else R.id.item_date_text_view_dark
                is CalendarViewItem.Event -> if (isColorLight) R.id.item_event_text_view else R.id.item_event_text_view_dark
            }
            val text = when (item) {
                is CalendarViewItem.Day -> CalendarDateUtils.getFormattedDate(
                    context,
                    item.date.time
                )
                    .capitalize(Locale.getDefault())

                is CalendarViewItem.Event -> CalendarDateUtils.getCalendarEventText(
                    item.event,
                    context,
                    widgetId
                )
            }
            val fontSizeAdjustment = when (AgendaWidgetPrefs.getFontSize(context, widgetId)) {
                AgendaWidgetPrefs.FontSize.SMALL -> -2f
                AgendaWidgetPrefs.FontSize.MEDIUM -> 0f
                AgendaWidgetPrefs.FontSize.LARGE -> 2f
            }
            val textAlignment = when (AgendaWidgetPrefs.getTextAlignment(context, widgetId)) {
                AgendaWidgetPrefs.TextAlignment.LEFT -> Gravity.LEFT
                AgendaWidgetPrefs.TextAlignment.CENTER -> Gravity.CENTER
                AgendaWidgetPrefs.TextAlignment.RIGHT -> Gravity.RIGHT
            }
            val separatorVisibility =
                if (AgendaWidgetPrefs.getSeparatorVisible(
                        context,
                        widgetId
                    )
                ) View.VISIBLE else View.GONE
            setViewVisibility(R.id.date_separator, separatorVisibility)
            setInt(
                R.id.date_separator,
                "setBackgroundColor",
                textColor
            )
            setTextViewText(textViewId, text)
            setTextColor(textViewId, textColor)
            setTextViewTextSize(
                textViewId,
                TypedValue.COMPLEX_UNIT_SP,
                defaultTextSizeSp + fontSizeAdjustment
            )
            setInt(textViewId, "setGravity", textAlignment)
            setOnClickFillInIntent(textViewId, getFillInIntent(item))
        }
    }

    private fun getFillInIntent(item: CalendarViewItem): Intent {
        return when (item) {
            is CalendarViewItem.Day -> Intent().apply {
                putExtra(EXTRA_START_TIME, item.date.time)
            }

            is CalendarViewItem.Event -> Intent().apply {
                putExtra(EXTRA_EVENT_ID, item.event.eventId)
                putExtra(EXTRA_START_TIME, item.event.actualStartTime)
                putExtra(EXTRA_END_TIME, item.event.actualEndTime)
            }
        }
    }

    override fun getLoadingView(): RemoteViews {
        return RemoteViews(context.packageName, R.layout.item_date)
    }

    override fun getViewTypeCount(): Int {
        return 4
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    private fun getEvents(): List<CalendarViewItem> {
        return calendarFetcher.readCalendarData(context, widgetId)
    }

}