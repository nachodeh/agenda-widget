package com.flowmosaic.calendar.remoteviews

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.compose.ui.graphics.toArgb
import com.flowmosaic.calendar.EXTRA_END_TIME
import com.flowmosaic.calendar.EXTRA_START_TIME
import com.flowmosaic.calendar.EXTRA_EVENT_ID
import com.flowmosaic.calendar.R
import com.flowmosaic.calendar.data.CalendarDateUtils
import com.flowmosaic.calendar.data.CalendarFetcher
import com.flowmosaic.calendar.data.CalendarViewItem
import com.flowmosaic.calendar.prefs.AgendaWidgetPrefs

class EventsRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private val calendarFetcher = CalendarFetcher()
    private val events: MutableList<CalendarViewItem> = mutableListOf()

    override fun onCreate() {
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

    override fun getViewAt(position: Int): RemoteViews {
        val item = events[position]

        val layoutResId = when (item) {
            is CalendarViewItem.Day -> R.layout.item_date
            is CalendarViewItem.Event -> R.layout.item_event
        }

        return RemoteViews(context.packageName, layoutResId).apply {
            val textViewId = when (item) {
                is CalendarViewItem.Day -> R.id.item_date_text_view
                is CalendarViewItem.Event -> R.id.item_event_text_view
            }

            val text = when (item) {
                is CalendarViewItem.Day -> CalendarDateUtils.getFormattedDate(context, item.date.time)
                is CalendarViewItem.Event -> CalendarDateUtils.getCalendarEventText(item.event, context)
            }

            setTextViewText(textViewId, text)
            setTextColor(textViewId, AgendaWidgetPrefs.getTextColor(context).toArgb())
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
                putExtra(EXTRA_START_TIME, item.event.startTimeInMillis)
                putExtra(EXTRA_END_TIME, item.event.endTimeInMillis)
            }
        }
    }

    override fun getLoadingView(): RemoteViews {
        return RemoteViews(context.packageName, R.layout.item_date)
    }

    override fun getViewTypeCount(): Int {
        return 2
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    private fun getEvents(): List<CalendarViewItem> {
        return calendarFetcher.readCalendarData(context)
    }

}