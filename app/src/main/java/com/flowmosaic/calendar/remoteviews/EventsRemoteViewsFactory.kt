package com.flowmosaic.calendar.remoteviews

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.compose.ui.graphics.toArgb
import com.flowmosaic.calendar.R
import android.graphics.Paint
import com.flowmosaic.calendar.data.CalendarDateUtils
import com.flowmosaic.calendar.data.CalendarFetcher
import com.flowmosaic.calendar.data.CalendarViewItem
import com.flowmosaic.calendar.data.TaskData
import com.flowmosaic.calendar.data.TaskFetcher
import com.flowmosaic.calendar.prefs.AgendaWidgetPrefs
import com.flowmosaic.calendar.ui.UnitConverter
import com.flowmosaic.calendar.ui.isColorLight
import com.flowmosaic.calendar.widget.EXTRA_END_TIME
import com.flowmosaic.calendar.widget.EXTRA_EVENT_ID
import com.flowmosaic.calendar.widget.EXTRA_START_TIME
import com.flowmosaic.calendar.widget.EXTRA_TASK_ID
import com.flowmosaic.calendar.widget.EXTRA_TASK_COMPLETED
import java.util.Calendar
import java.util.Date

class EventsRemoteViewsFactory(private val context: Context, intent: Intent) :
    RemoteViewsService.RemoteViewsFactory {

    private val prefs by lazy { AgendaWidgetPrefs(context) }

    private var widgetId = ""

    init {
        if (intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) {
            widgetId =
                intent
                    .getIntExtra(
                        AppWidgetManager.EXTRA_APPWIDGET_ID,
                        AppWidgetManager.INVALID_APPWIDGET_ID
                    )
                    .toString()
        }
    }

    private val calendarFetcher = CalendarFetcher()
    private val taskFetcher = TaskFetcher()
    private val items: MutableList<CalendarViewItem> = mutableListOf()

    override fun onCreate() {
        items.clear()
        items.addAll(getItems())
    }

    override fun onDataSetChanged() {
        items.clear()
        items.addAll(getItems())
    }

    override fun onDestroy() {
        items.clear()
    }

    override fun getCount(): Int {
        return items.size
    }

    override fun getViewAt(position: Int): RemoteViews {
        if (position < 0 || position >= items.size) {
            // Return an empty view if out of bounds
            return RemoteViews(context.packageName, R.layout.empty_layout)
        }

        val item = items[position]
        val textColor = prefs.getTextColor(widgetId).toArgb()

        return RemoteViews(context.packageName, getLayoutId(item, textColor)).apply {
            val textViewId = getTextViewId(item, textColor)
            val text =
                when (item) {
                    is CalendarViewItem.Day ->
                        CalendarDateUtils.getFormattedDate(context, item.date.time)
                    is CalendarViewItem.Event ->
                        CalendarDateUtils.getCalendarEventText(
                            item.event,
                            context,
                            widgetId,
                            prefs.getShowLocation(widgetId)
                        )
                    is CalendarViewItem.Task -> item.task.title
                    is CalendarViewItem.NoDueDateHeader ->
                        context.getString(R.string.no_due_date)
                }

            setTextColor(textViewId, textColor)
            setUpSeparator(item, textColor)
            setUpFontSize(textViewId, item)
            setUpVerticalSpacing(context, textViewId, item)
            setUpFontAlignment(textViewId)

            if (item is CalendarViewItem.Event) {
                setUpCalendarBlob(textColor, item.event.calendarId)
            }

            if (item is CalendarViewItem.Task) {
                setUpTaskCheckbox(item.task, textColor, textViewId)
            }

            setTextViewText(textViewId, text)
            setOnClickFillInIntent(textViewId, getFillInIntent(item))
        }
    }

    private fun getLayoutId(calendarViewItem: CalendarViewItem, textColor: Int): Int {
        val isColorLight = isColorLight(textColor)
        return when (calendarViewItem) {
            is CalendarViewItem.Day ->
                if (isColorLight) R.layout.item_date else R.layout.item_date_dark
            is CalendarViewItem.Event ->
                if (isColorLight) R.layout.item_event else R.layout.item_event_dark
            is CalendarViewItem.Task ->
                if (isColorLight) R.layout.item_task else R.layout.item_task_dark
            is CalendarViewItem.NoDueDateHeader ->
                if (isColorLight) R.layout.item_no_due_date_header
                else R.layout.item_no_due_date_header_dark
        }
    }

    private fun getTextViewId(calendarViewItem: CalendarViewItem, textColor: Int): Int {
        val isColorLight = isColorLight(textColor)
        return when (calendarViewItem) {
            is CalendarViewItem.Day ->
                if (isColorLight) R.id.item_date_text_view else R.id.item_date_text_view_dark
            is CalendarViewItem.Event ->
                if (isColorLight) R.id.item_event_text_view else R.id.item_event_text_view_dark
            is CalendarViewItem.Task ->
                if (isColorLight) R.id.item_task_text_view else R.id.item_task_text_view_dark
            is CalendarViewItem.NoDueDateHeader ->
                if (isColorLight) R.id.item_no_due_date_text_view
                else R.id.item_no_due_date_text_view_dark
        }
    }

    private fun RemoteViews.setUpSeparator(item: CalendarViewItem, color: Int) {
        // Only Day and NoDueDateHeader have separators
        val (wrapperId, separatorId) = when (item) {
            is CalendarViewItem.Day -> R.id.date_separator_wrapper to R.id.date_separator
            is CalendarViewItem.NoDueDateHeader -> {
                val isColorLight = isColorLight(color)
                if (isColorLight)
                    R.id.no_due_date_separator_wrapper to R.id.no_due_date_separator
                else
                    R.id.no_due_date_separator_wrapper_dark to R.id.no_due_date_separator_dark
            }
            else -> return // No separator for events or tasks
        }

        // Set visibility
        val separatorVisibility =
            if (prefs.getSeparatorVisible(widgetId)) View.VISIBLE else View.GONE
        setViewVisibility(wrapperId, separatorVisibility)

        // Set background color
        setInt(separatorId, "setBackgroundColor", color)

        // Set spacing
        val verticalSpacing = prefs.getVerticalSpacing(widgetId)
        val bottomPadding =
            UnitConverter.dpToPx(
                when (verticalSpacing) {
                    AgendaWidgetPrefs.VerticalSpacing.SMALL -> 0f
                    AgendaWidgetPrefs.VerticalSpacing.LARGE -> 6f
                },
                context
            )
        setViewPadding(
            wrapperId,
            0,
            0,
            0,
            bottomPadding,
        )
    }

    private fun RemoteViews.setUpFontAlignment(textViewId: Int) {
        val textAlignment =
            when (prefs.getTextAlignment(widgetId)) {
                AgendaWidgetPrefs.TextAlignment.LEFT -> Gravity.START
                AgendaWidgetPrefs.TextAlignment.CENTER -> Gravity.CENTER
                AgendaWidgetPrefs.TextAlignment.RIGHT -> Gravity.END
            }
        setInt(textViewId, "setGravity", textAlignment)
    }

    private fun RemoteViews.setUpFontSize(textViewId: Int, calendarViewItem: CalendarViewItem) {
        val defaultTextSizeSp =
            when (calendarViewItem) {
                is CalendarViewItem.Day -> 16f
                is CalendarViewItem.Event -> 14f
                is CalendarViewItem.Task -> 14f
                is CalendarViewItem.NoDueDateHeader -> 16f
            }
        val fontSizeAdjustment =
            when (prefs.getFontSize(widgetId)) {
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
            is CalendarViewItem.Day, is CalendarViewItem.NoDueDateHeader -> {
                val topPadding =
                    UnitConverter.dpToPx(
                        when (verticalSpacing) {
                            AgendaWidgetPrefs.VerticalSpacing.SMALL -> 4f
                            AgendaWidgetPrefs.VerticalSpacing.LARGE -> 8f
                        },
                        context
                    )
                val bottomPadding =
                    UnitConverter.dpToPx(
                        when (verticalSpacing) {
                            AgendaWidgetPrefs.VerticalSpacing.SMALL -> 0f
                            AgendaWidgetPrefs.VerticalSpacing.LARGE -> 4f
                        },
                        context
                    )
                setViewPadding(
                    textViewId,
                    0,
                    topPadding,
                    0,
                    bottomPadding,
                )
            }
            is CalendarViewItem.Event, is CalendarViewItem.Task -> {
                val verticalPadding =
                    UnitConverter.dpToPx(
                        when (verticalSpacing) {
                            AgendaWidgetPrefs.VerticalSpacing.SMALL -> 0f
                            AgendaWidgetPrefs.VerticalSpacing.LARGE -> 5f
                        },
                        context
                    )
                setViewPadding(
                    textViewId,
                    0,
                    verticalPadding,
                    0,
                    verticalPadding,
                )
            }
        }
    }

    private fun getFillInIntent(item: CalendarViewItem): Intent {
        return when (item) {
            is CalendarViewItem.Day -> Intent().apply { putExtra(EXTRA_START_TIME, item.date.time) }
            is CalendarViewItem.Event ->
                Intent().apply {
                    putExtra(EXTRA_EVENT_ID, item.event.eventId)
                    putExtra(EXTRA_START_TIME, item.event.actualStartTime)
                    putExtra(EXTRA_END_TIME, item.event.actualEndTime)
                }
            is CalendarViewItem.Task ->
                Intent().apply {
                    putExtra(EXTRA_TASK_ID, item.task.id)
                    putExtra(EXTRA_TASK_COMPLETED, !item.task.isCompleted)
                }
            is CalendarViewItem.NoDueDateHeader -> Intent() // No action for header
        }
    }

    override fun getLoadingView(): RemoteViews {
        return RemoteViews(context.packageName, R.layout.item_date)
    }

    override fun getViewTypeCount(): Int {
        // 2 for day (light/dark), 2 for event (light/dark), 2 for task (light/dark),
        // 2 for no due date header (light/dark), 1 for empty
        return 9
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    private fun getItems(): List<CalendarViewItem> {
        val events = calendarFetcher.readCalendarData(context, widgetId)
        val tasks = taskFetcher.readTaskData(context, widgetId)

        if (tasks.isEmpty()) {
            return events
        }

        return mergeEventsAndTasks(events, tasks)
    }

    /**
     * Merges events and tasks into a single list. Tasks are organized as follows: - Undated tasks
     * appear at the very top under a "No due date" header - Dated tasks appear before events on
     * each day
     */
    private fun mergeEventsAndTasks(
        events: List<CalendarViewItem>,
        tasks: List<TaskData>
    ): List<CalendarViewItem> {
        val result = mutableListOf<CalendarViewItem>()

        // Separate tasks by whether they have a due date
        val (datedTasks, undatedTasks) = tasks.partition { it.dueDate != null }

        // Add undated tasks at the top with header
        if (undatedTasks.isNotEmpty()) {
            result.add(CalendarViewItem.NoDueDateHeader)
            undatedTasks.forEach { task -> result.add(CalendarViewItem.Task(task)) }
        }

        // Group dated tasks by date
        val tasksByDate =
            datedTasks.groupBy { task ->
                CalendarDateUtils.getDateFromTimestamp(task.dueDate!!)
            }

        // Process each day from the events list and insert tasks before events
        var currentDate: Date? = null
        for (item in events) {
            when (item) {
                is CalendarViewItem.Day -> {
                    // Add the day header
                    result.add(item)
                    currentDate = item.date

                    // Add tasks for this day (before events)
                    val tasksForDay = tasksByDate[currentDate]
                    tasksForDay?.forEach { task -> result.add(CalendarViewItem.Task(task)) }
                }
                is CalendarViewItem.Event -> {
                    result.add(item)
                }
                else -> {
                    result.add(item)
                }
            }
        }

        // Add any tasks for dates that don't have events
        val eventDates = events.filterIsInstance<CalendarViewItem.Day>().map { it.date }.toSet()
        val tasksOnNewDates =
            tasksByDate.filterKeys { date -> date !in eventDates }.toSortedMap()

        for ((date, tasksForDay) in tasksOnNewDates) {
            result.add(CalendarViewItem.Day(date))
            tasksForDay.forEach { task -> result.add(CalendarViewItem.Task(task)) }
        }

        return result
    }

    private fun RemoteViews.setUpTaskCheckbox(task: TaskData, textColor: Int, textViewId: Int) {
        val isColorLight = isColorLight(textColor)
        val checkboxId =
            if (isColorLight) R.id.item_task_checkbox else R.id.item_task_checkbox_dark

        // Set checkbox drawable based on completion state
        val checkboxDrawable =
            if (task.isCompleted) R.drawable.ic_checkbox_checked
            else R.drawable.ic_checkbox_unchecked

        setImageViewResource(checkboxId, checkboxDrawable)
        setInt(checkboxId, "setColorFilter", textColor)

        // Apply strikethrough for completed tasks
        if (task.isCompleted) {
            setInt(textViewId, "setPaintFlags", Paint.STRIKE_THRU_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG)
        } else {
            setInt(textViewId, "setPaintFlags", Paint.ANTI_ALIAS_FLAG)
        }
    }

    private fun RemoteViews.setUpCalendarBlob(textColor: Int, calendarId: Long) {
        var wrapperId = R.id.item_event_calendar_blob
        var backgroundId = R.id.item_event_calendar_blob_background
        var emojiId = R.id.item_event_calendar_blob_emoji
        if (!isColorLight(textColor)) {
            wrapperId = R.id.item_event_calendar_blob_dark
            backgroundId = R.id.item_event_calendar_blob_dark_background
            emojiId = R.id.item_event_calendar_blob_dark_emoji
        }

        // Check if blobs are enabled
        if (!prefs.getShowCalendarBlob(widgetId)) {
            setViewVisibility(wrapperId, View.GONE)
            return
        }

        val indicatorStyle = prefs.getIndicatorStyle(widgetId)
        val containerSizePx = UnitConverter.dpToPx(18f, context).toFloat()
        setViewLayoutWidth(wrapperId, containerSizePx, TypedValue.COMPLEX_UNIT_PX)
        setViewLayoutHeight(wrapperId, containerSizePx, TypedValue.COMPLEX_UNIT_PX)

        when (indicatorStyle) {
            AgendaWidgetPrefs.IndicatorStyle.COLORS -> {
                // Show color blob
                val calendarColor = prefs.getCalendarColor(widgetId, calendarId)
                setInt(backgroundId, "setColorFilter", calendarColor.toArgb())
                val blobPadding = UnitConverter.dpToPx(2f, context)
                setViewPadding(backgroundId, blobPadding, blobPadding, blobPadding, blobPadding)
                setViewVisibility(emojiId, View.GONE)
                setViewVisibility(backgroundId, View.VISIBLE)
                setViewVisibility(wrapperId, View.VISIBLE)
            }
            AgendaWidgetPrefs.IndicatorStyle.EMOJIS -> {
                val calendarEmoji = prefs.getCalendarEmoji(widgetId, calendarId)
                if (calendarEmoji.isNotEmpty()) {
                    // Show emoji
                    setTextViewText(emojiId, calendarEmoji)
                    setTextViewTextSize(emojiId, TypedValue.COMPLEX_UNIT_DIP, 14f)
                    setViewVisibility(emojiId, View.VISIBLE)
                    setViewVisibility(backgroundId, View.GONE)
                    setViewVisibility(wrapperId, View.VISIBLE)
                } else {
                    // No emoji set - hide the indicator
                    setViewVisibility(wrapperId, View.GONE)
                }
            }
        }
    }
}
