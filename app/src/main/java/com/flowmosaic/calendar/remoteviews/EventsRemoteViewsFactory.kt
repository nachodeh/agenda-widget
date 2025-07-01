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
import com.flowmosaic.calendar.R
import com.flowmosaic.calendar.data.CalendarDateUtils
import com.flowmosaic.calendar.data.CalendarFetcher
import com.flowmosaic.calendar.data.CalendarViewItem
import com.flowmosaic.calendar.prefs.AgendaWidgetPrefs
import com.flowmosaic.calendar.ui.UnitConverter
import com.flowmosaic.calendar.widget.EXTRA_END_TIME
import com.flowmosaic.calendar.widget.EXTRA_EVENT_ID
import com.flowmosaic.calendar.widget.EXTRA_START_TIME

class EventsRemoteViewsFactory(private val context: Context, intent: Intent) :
    RemoteViewsService.RemoteViewsFactory {

    private val prefs by lazy { AgendaWidgetPrefs(context) }

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

    override fun getViewAt(position: Int): RemoteViews {
        if (position < 0 || position >= events.size) {
            // Return an empty view if out of bounds
            return RemoteViews(context.packageName, R.layout.empty_layout)
        }

        val item = events[position]
        val textColor = prefs.getTextColor(widgetId).toArgb()

        return RemoteViews(context.packageName, getLayoutId(item, textColor)).apply {
            val textViewId = getTextViewId(item, textColor)
            val text = when (item) {
                is CalendarViewItem.Day -> CalendarDateUtils.getFormattedDate(
                    context,
                    item.date.time
                )

                is CalendarViewItem.Event -> CalendarDateUtils.getCalendarEventText(
                    item.event,
                    context,
                    widgetId
                )
            }

            setTextColor(textViewId, textColor)
            setUpSeparator(textColor)
            setUpFontSize(textViewId, item)
            setUpVerticalSpacing(context, textViewId, item)
            setUpFontAlignment(textViewId)

            setTextViewText(textViewId, text)
            setOnClickFillInIntent(textViewId, getFillInIntent(item))
        }
    }

    private fun getLayoutId(calendarViewItem: CalendarViewItem, textColor: Int): Int {
        val isColorLight = isColorLight(textColor)
        return when (calendarViewItem) {
            is CalendarViewItem.Day -> if (isColorLight) R.layout.item_date else R.layout.item_date_dark
            is CalendarViewItem.Event -> if (isColorLight) R.layout.item_event else R.layout.item_event_dark
        }
    }

    private fun getTextViewId(calendarViewItem: CalendarViewItem, textColor: Int): Int {
        val isColorLight = isColorLight(textColor)
        return when (calendarViewItem) {
            is CalendarViewItem.Day -> if (isColorLight) R.id.item_date_text_view else R.id.item_date_text_view_dark
            is CalendarViewItem.Event -> if (isColorLight) R.id.item_event_text_view else R.id.item_event_text_view_dark
        }
    }

    private fun RemoteViews.setUpSeparator(color: Int) {
        // Set visibility
        val separatorVisibility =
            if (prefs.getSeparatorVisible(widgetId)) View.VISIBLE else View.GONE
        setViewVisibility(R.id.date_separator_wrapper, separatorVisibility)

        // Set background color
        setInt(
            R.id.date_separator,
            "setBackgroundColor",
            color
        )

        // Set spacing
        val verticalSpacing = prefs.getVerticalSpacing(widgetId)
        val bottomPadding = UnitConverter.dpToPx(
            when (verticalSpacing) {
                AgendaWidgetPrefs.VerticalSpacing.SMALL -> 0f
                AgendaWidgetPrefs.VerticalSpacing.LARGE -> 6f
            }, context
        )
        setViewPadding(
            R.id.date_separator_wrapper,
            0, 0, 0, bottomPadding,
        )
    }

    private fun RemoteViews.setUpFontAlignment(textViewId: Int) {
        val textAlignment = when (prefs.getTextAlignment(widgetId)) {
            AgendaWidgetPrefs.TextAlignment.LEFT -> Gravity.START
            AgendaWidgetPrefs.TextAlignment.CENTER -> Gravity.CENTER
            AgendaWidgetPrefs.TextAlignment.RIGHT -> Gravity.END
        }
        setInt(textViewId, "setGravity", textAlignment)
    }

    private fun RemoteViews.setUpFontSize(textViewId: Int, calendarViewItem: CalendarViewItem) {
        val defaultTextSizeSp = when (calendarViewItem) {
            is CalendarViewItem.Day -> 16f
            is CalendarViewItem.Event -> 14f
        }
        val fontSizeAdjustment = when (prefs.getFontSize(widgetId)) {
            AgendaWidgetPrefs.FontSize.SMALL -> -2f
            AgendaWidgetPrefs.FontSize.MEDIUM -> 0f
            AgendaWidgetPrefs.FontSize.LARGE -> 2f
        }
        setTextViewTextSize(
            textViewId,
            TypedValue.COMPLEX_UNIT_SP,
            defaultTextSizeSp + fontSizeAdjustment
        )
    }

    private fun RemoteViews.setUpVerticalSpacing(
        context: Context,
        textViewId: Int,
        calendarViewItem: CalendarViewItem,
    ) {
        val verticalSpacing = prefs.getVerticalSpacing(widgetId)
        when (calendarViewItem) {
            is CalendarViewItem.Day -> {
                val topPadding = UnitConverter.dpToPx(
                    when (verticalSpacing) {
                        AgendaWidgetPrefs.VerticalSpacing.SMALL -> 4f
                        AgendaWidgetPrefs.VerticalSpacing.LARGE -> 8f
                    }, context
                )
                val bottomPadding = UnitConverter.dpToPx(
                    when (verticalSpacing) {
                        AgendaWidgetPrefs.VerticalSpacing.SMALL -> 0f
                        AgendaWidgetPrefs.VerticalSpacing.LARGE -> 4f
                    }, context
                )
                setViewPadding(
                    textViewId,
                    0, topPadding, 0, bottomPadding,
                )
            }

            is CalendarViewItem.Event -> {
                val verticalPadding = UnitConverter.dpToPx(
                    when (verticalSpacing) {
                        AgendaWidgetPrefs.VerticalSpacing.SMALL -> 0f
                        AgendaWidgetPrefs.VerticalSpacing.LARGE -> 5f
                    }, context
                )
                setViewPadding(
                    textViewId,
                    0, verticalPadding, 0, verticalPadding,
                )
            }
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
        return 5
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

    private fun isColorLight(color: Int): Boolean {
        val red = Color.red(color) / 255.0
        val green = Color.green(color) / 255.0
        val blue = Color.blue(color) / 255.0

        val luminance = 0.2126 * red + 0.7152 * green + 0.0722 * blue
        return luminance > 0.5
    }

}