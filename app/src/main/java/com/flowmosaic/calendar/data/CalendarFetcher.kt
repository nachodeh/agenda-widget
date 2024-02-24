package com.flowmosaic.calendar.data

import android.content.Context
import android.database.Cursor
import android.provider.CalendarContract
import com.flowmosaic.calendar.prefs.AgendaWidgetPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

data class CalendarData(val id: Long, val name: String)

data class CalendarEvent(
    val startTimeInMillis: Long,
    val endTimeInMillis: Long,
    val title: String,
    val location: String?,
    val isAllDay: Boolean,
    val eventId: Long,
    val actualStartTime: Long,
    val actualEndTime: Long,
)

sealed class CalendarViewItem {
    data class Day(val date: Date) : CalendarViewItem()
    data class Event(val event: CalendarEvent) : CalendarViewItem()
}

class CalendarFetcher {

    fun readCalendarData(context: Context, widgetId: String): List<CalendarViewItem> {
        val events = getCalendarEvents(context, widgetId)
        return parseAndTransformCalendarItems(events)
    }

    suspend fun queryCalendarData(context: Context): List<CalendarData> = withContext(
        Dispatchers.IO
    ) {
        val calendarList = mutableListOf<CalendarData>()

        val projection = arrayOf(
            CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
        )

        val cursor = context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI, projection, null, null, null
        )

        cursor?.use {
            val idColumn = it.getColumnIndex(CalendarContract.Calendars._ID)
            val nameColumn = it.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val name = it.getString(nameColumn)
                calendarList.add(CalendarData(id, name))
            }
        }

        calendarList
    }

    private fun getStartAndEndTime(prefs: AgendaWidgetPrefs, widgetId: String): Pair<Long, Long> {
        val currentTime = Calendar.getInstance().timeInMillis
        val endTime =  Calendar.getInstance().apply {
            timeInMillis = currentTime
            add(Calendar.DAY_OF_MONTH, prefs.getNumberOfDays(widgetId))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return Pair(currentTime, endTime)
    }

    private fun getCalendarEvents(context: Context, widgetId: String): List<CalendarEvent> {
        val prefs = AgendaWidgetPrefs(context)
        val events = arrayListOf<CalendarEvent>()
        val (startTime, endTime) = getStartAndEndTime(prefs, widgetId)
        val selectedCalendarIds = prefs.getSelectedCalendars(null, widgetId)

        val projection = arrayOf(
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances._ID,
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
        )

        val selection =
            "(${CalendarContract.Instances.BEGIN} <= ? AND ${CalendarContract.Instances.END} >= ?) OR " +
                    "(${CalendarContract.Instances.ALL_DAY} = 1 AND ${CalendarContract.Instances.BEGIN} <= ? AND ${CalendarContract.Instances.END} IS NULL)"

        val selectionArgs = arrayOf(
            endTime.toString(),
            startTime.toString(),
            endTime.toString()
        )

        val sortOrder = "${CalendarContract.Instances.BEGIN} ASC"

        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(startTime.toString())
            .appendPath(endTime.toString())
            .build()

        val eventCursor = context.contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

        eventCursor?.use { cursor ->
            while (cursor.moveToNext()) {
                val calendarId =
                    cursor.safeGet(CalendarContract.Instances.CALENDAR_ID, Cursor::getLong)
                val eventId = cursor.safeGet(CalendarContract.Instances.EVENT_ID, Cursor::getLong)
                val title = cursor.safeGet(CalendarContract.Instances.TITLE, Cursor::getString)
                val location =
                    cursor.safeGet(CalendarContract.Instances.EVENT_LOCATION, Cursor::getString)
                val allDay = cursor.safeGet(CalendarContract.Instances.ALL_DAY, Cursor::getInt)
                    ?.let { it != 0 }
                val actualStartTime =
                    cursor.safeGet(CalendarContract.Instances.BEGIN, Cursor::getLong)
                val actualEndTime = cursor.safeGet(CalendarContract.Instances.END, Cursor::getLong)


                if (calendarId != null &&
                    eventId != null &&
                    title != null &&
                    location != null &&
                    allDay != null &&
                    actualStartTime != null &&
                    actualEndTime != null &&
                    calendarId.toString() in selectedCalendarIds
                ) {
                    var startDateTime = actualStartTime
                    var endDateTime = actualEndTime
                    var maxEndTime = endTime

                    if (allDay == true) {
                        val timeZone = TimeZone.getDefault()
                        val startOffsetMillis = timeZone.getOffset(startDateTime)
                        startDateTime -= startOffsetMillis

                        val endOffsetMillis = timeZone.getOffset(endDateTime)
                        endDateTime -= endOffsetMillis

                        val maxEndTimeOffsetMillis = timeZone.getOffset(maxEndTime)
                        maxEndTime -= maxEndTimeOffsetMillis
                    }
                    // Only add events happening in the future according to the timezone offset
                    if (endDateTime > System.currentTimeMillis()) {
                        val adjustedEndTime = if (endDateTime > maxEndTime) maxEndTime else endDateTime
                        events.add(
                            CalendarEvent(
                                startDateTime,
                                adjustedEndTime,
                                title,
                                location,
                                allDay,
                                eventId,
                                actualStartTime,
                                actualEndTime
                            )
                        )
                    }
                }
            }
        }

        return events
    }

    private fun <T> Cursor.safeGet(columnName: String, getter: Cursor.(Int) -> T): T? {
        val columnIndex = getColumnIndex(columnName)
        return if (columnIndex != -1) getter(columnIndex) else null
    }

    private fun parseAndTransformCalendarItems(parsedCalendarEvents: List<CalendarEvent>): List<CalendarViewItem> {
        val timeZone = TimeZone.getDefault()
        // Get the current date at the start of the day
        val currentDayStartTime = Calendar.getInstance(timeZone).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        return parsedCalendarEvents
            .flatMap { event ->
                var adjustedEvent = event

                // Adjust start time if it's earlier than today
                if (adjustedEvent.startTimeInMillis < currentDayStartTime) {
                    adjustedEvent = adjustedEvent.copy(startTimeInMillis = currentDayStartTime)
                }

                // If it's an all-day event and spans multiple days, create an event for each day
                if (adjustedEvent.isAllDay || spansMultipleDays(adjustedEvent)) {
                    val startCalendar = Calendar.getInstance()
                        .apply { timeInMillis = adjustedEvent.startTimeInMillis }
                    val endCalendar = Calendar.getInstance()
                        .apply { timeInMillis = adjustedEvent.endTimeInMillis }
                    val eventsList = mutableListOf<CalendarEvent>()

                    while (startCalendar.before(endCalendar)) {
                        val nextDayStartCalendar = startCalendar.clone() as Calendar
                        nextDayStartCalendar.set(Calendar.HOUR_OF_DAY, 24)
                        nextDayStartCalendar.set(Calendar.MINUTE, 0)
                        nextDayStartCalendar.set(Calendar.SECOND, 0)
                        nextDayStartCalendar.set(Calendar.MILLISECOND, 0)

                        val eventEndTime: Long = if (nextDayStartCalendar.before(endCalendar)) {
                            nextDayStartCalendar.timeInMillis
                        } else {
                            adjustedEvent.endTimeInMillis
                        }

                        val clonedEvent = adjustedEvent.copy(
                            startTimeInMillis = startCalendar.timeInMillis,
                            endTimeInMillis = eventEndTime
                        )
                        eventsList.add(clonedEvent)

                        // Move to next day 00:00
                        startCalendar.add(Calendar.DAY_OF_MONTH, 1)
                        startCalendar.set(Calendar.HOUR_OF_DAY, 0)
                        startCalendar.set(Calendar.MINUTE, 0)
                        startCalendar.set(Calendar.SECOND, 0)
                        startCalendar.set(Calendar.MILLISECOND, 0)
                    }
                    eventsList
                } else {
                    listOf(adjustedEvent)
                }

            }
            .groupBy { CalendarDateUtils.getDateFromTimestamp(it.startTimeInMillis) }
            .flatMap { (date, events) ->
                listOf(CalendarViewItem.Day(date)) + events.map { CalendarViewItem.Event(it) }
            }
            .sortedBy { item ->
                when (item) {
                    is CalendarViewItem.Day -> item.date.time
                    is CalendarViewItem.Event -> item.event.startTimeInMillis
                }
            }
    }

    private fun spansMultipleDays(event: CalendarEvent): Boolean {
        val startCal = Calendar.getInstance().apply { timeInMillis = event.startTimeInMillis }
        val endCal = Calendar.getInstance().apply { timeInMillis = event.endTimeInMillis }

        return startCal.get(Calendar.DAY_OF_YEAR) != endCal.get(Calendar.DAY_OF_YEAR) ||
                startCal.get(Calendar.YEAR) != endCal.get(Calendar.YEAR)
    }

}