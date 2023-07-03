package com.flowmosaic.calendar.data

import android.content.Context
import android.provider.CalendarContract
import com.flowmosaic.calendar.prefs.AgendaWidgetPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

data class CalendarData(val id: Long, val name: String)

data class CalendarEvent(
    val startTimeInMillis: Long,
    val endTimeInMillis: Long,
    val title: String,
    val location: String,
    val isAllDay: Boolean,
    val eventId: Long
)

sealed class CalendarViewItem {
    data class Day(val date: Date) : CalendarViewItem()
    data class Event(val event: CalendarEvent) : CalendarViewItem()
}

class CalendarFetcher {

    fun readCalendarData(context: Context): List<CalendarViewItem> {
        val events = getCalendarEvents(context)
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

    private fun getCalendarEvents(context: Context): List<CalendarEvent> {
        val events = arrayListOf<CalendarEvent>()
        val currentTime = Calendar.getInstance().timeInMillis
        val endTime =
            currentTime + TimeUnit.DAYS.toMillis(AgendaWidgetPrefs.getNumberOfDays(context).toLong())
        val selectedCalendarIds = AgendaWidgetPrefs.getSelectedCalendars(context, null)

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
                    "(${CalendarContract.Instances.ALL_DAY} = 1 AND ${CalendarContract.Instances.BEGIN} <= ? AND ${CalendarContract.Instances.END} IS NULL) OR " +
                    "(${CalendarContract.Instances.BEGIN} <= ? AND ${CalendarContract.Instances.END} >= ? AND ${CalendarContract.Instances.END} > ?)"

        val selectionArgs = arrayOf(
            endTime.toString(),
            currentTime.toString(),
            endTime.toString()
        )

        val sortOrder = "${CalendarContract.Instances.BEGIN} ASC"

        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(currentTime.toString())
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
                val calendarId = cursor.getLong(cursor.getColumnIndex(CalendarContract.Instances.CALENDAR_ID))
                val eventId =
                    cursor.getLong(cursor.getColumnIndex(CalendarContract.Instances.EVENT_ID))
                val title =
                    cursor.getString(cursor.getColumnIndex(CalendarContract.Instances.TITLE))
                val location =
                    cursor.getString(cursor.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION))
                val allDay =
                    (cursor.getInt(cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY)) != 0)
                val startDateTime =
                    cursor.getLong(cursor.getColumnIndex(CalendarContract.Instances.BEGIN))
                val endDateTime =
                    cursor.getLong(cursor.getColumnIndex(CalendarContract.Instances.END))

                if (calendarId.toString() in selectedCalendarIds) {
                    events.add(
                        CalendarEvent(
                            startDateTime,
                            endDateTime,
                            title,
                            location,
                            allDay,
                            eventId
                        )
                    )
                }
            }
        }

        return events
    }

    private fun parseAndTransformCalendarItems(parsedCalendarEvents: List<CalendarEvent>): List<CalendarViewItem> {
        return parsedCalendarEvents
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


}