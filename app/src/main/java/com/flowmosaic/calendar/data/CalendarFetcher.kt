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

        context.contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
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

                if (calendarId == null ||
                    eventId == null ||
                    title == null ||
                    location == null ||
                    allDay == null ||
                    actualStartTime == null ||
                    actualEndTime == null ||
                    calendarId.toString() !in selectedCalendarIds
                ) {
                    continue
                }

                val startDateTime = adjustTimeForTimezone(actualStartTime, allDay)
                val endDateTime = adjustTimeForTimezone(actualEndTime, allDay)
                val maxEndTime = adjustTimeForTimezone(endTime, allDay)

                if (endDateTime < System.currentTimeMillis()) continue // Skip past events
                if (startDateTime >= endTime) continue // Skip events that will show as next day in current timezone.

                events.add(
                    CalendarEvent(
                        startDateTime,
                        endDateTime.coerceAtMost(maxEndTime),
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

        return events
    }

    private fun adjustTimeForTimezone(time: Long, allDay: Boolean): Long {
        if (!allDay) {
            return time
        }
        val timeZone = TimeZone.getDefault()
        return time - timeZone.getOffset(time)
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
                val adjustedEvent = if (event.startTimeInMillis < currentDayStartTime) {
                    event.copy(startTimeInMillis = currentDayStartTime)
                } else {
                    event
                }
                if (adjustedEvent.isAllDay || spansMultipleDays(adjustedEvent)) {
                    splitMultiDayEvent(adjustedEvent)
                } else {
                    listOf(adjustedEvent)
                }
            }
            .groupBy { CalendarDateUtils.getDateFromTimestamp(it.startTimeInMillis) }
            .toSortedMap()
            .flatMap { (date, events) ->
                val sortedEvents = events.sortedWith(
                    compareBy({ !it.isAllDay }, { it.startTimeInMillis })
                )
                listOf(CalendarViewItem.Day(date)) + sortedEvents.map { CalendarViewItem.Event(it) }
            }
    }

    private fun splitMultiDayEvent(event: CalendarEvent): List<CalendarEvent> {
        val startCalendar = Calendar.getInstance().apply { timeInMillis = event.startTimeInMillis }
        val endCalendar = Calendar.getInstance().apply { timeInMillis = event.endTimeInMillis }
        val eventsList = mutableListOf<CalendarEvent>()

        while (startCalendar.before(endCalendar)) {
            val nextDayStartCalendar = startCalendar.clone() as Calendar
            nextDayStartCalendar.set(Calendar.HOUR_OF_DAY, 24)
            nextDayStartCalendar.set(Calendar.MINUTE, 0)
            nextDayStartCalendar.set(Calendar.SECOND, 0)
            nextDayStartCalendar.set(Calendar.MILLISECOND, 0)

            val eventEndTime: Long =
                if (nextDayStartCalendar.before(endCalendar)) nextDayStartCalendar.timeInMillis else event.endTimeInMillis

            val clonedEvent = event.copy(
                startTimeInMillis = startCalendar.timeInMillis, endTimeInMillis = eventEndTime
            )
            eventsList.add(clonedEvent)

            // Move to next day 00:00
            startCalendar.add(Calendar.DAY_OF_MONTH, 1)
            startCalendar.set(Calendar.HOUR_OF_DAY, 0)
            startCalendar.set(Calendar.MINUTE, 0)
            startCalendar.set(Calendar.SECOND, 0)
            startCalendar.set(Calendar.MILLISECOND, 0)
        }
        return eventsList
    }

    private fun spansMultipleDays(event: CalendarEvent): Boolean {
        val startCal = Calendar.getInstance().apply { timeInMillis = event.startTimeInMillis }
        val endCal = Calendar.getInstance().apply { timeInMillis = event.endTimeInMillis }

        return startCal.get(Calendar.DAY_OF_YEAR) != endCal.get(Calendar.DAY_OF_YEAR) ||
                startCal.get(Calendar.YEAR) != endCal.get(Calendar.YEAR)
    }

}