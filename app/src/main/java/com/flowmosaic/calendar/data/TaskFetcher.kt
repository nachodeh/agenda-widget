package com.flowmosaic.calendar.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.provider.CalendarContract
import com.flowmosaic.calendar.prefs.AgendaWidgetPrefs
import java.util.Calendar

/**
 * Fetches tasks from selected "task calendars". Many task apps (Google Tasks, Todoist, etc.) sync
 * their tasks to the Android calendar as events in dedicated calendars. This fetcher queries those
 * calendars and treats events as tasks, allowing them to be displayed with task UI (checkboxes)
 * and marked as complete.
 */
class TaskFetcher {

    fun readTaskData(context: Context, widgetId: String): List<TaskData> {
        val prefs = AgendaWidgetPrefs(context)
        if (!prefs.getShowTasks(widgetId)) {
            return emptyList()
        }
        return getTasks(context, widgetId, prefs)
    }

    private fun getTasks(context: Context, widgetId: String, prefs: AgendaWidgetPrefs): List<TaskData> {
        val tasks = arrayListOf<TaskData>()
        // Use task calendars, not regular selected calendars
        val taskCalendarIds = prefs.getTaskCalendars(widgetId)
        val showCompletedTasks = prefs.getShowCompletedTasks(widgetId)

        if (taskCalendarIds.isEmpty()) {
            return emptyList()
        }

        val (startTime, endTime) = getStartAndEndTime(prefs, widgetId)

        // Build selection for calendars
        val calendarSelection = taskCalendarIds.joinToString(",") { "?" }

        val projection =
            arrayOf(
                CalendarContract.Instances.CALENDAR_ID,
                CalendarContract.Instances._ID,
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.DESCRIPTION,
                CalendarContract.Instances.STATUS,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.ALL_DAY,
            )

        // Query ALL events from task calendars (not just all-day)
        val selection =
            "${CalendarContract.Instances.CALENDAR_ID} IN ($calendarSelection) AND " +
                "${CalendarContract.Instances.BEGIN} >= ? AND " +
                "${CalendarContract.Instances.BEGIN} <= ?"

        val selectionArgs =
            taskCalendarIds.toTypedArray() + arrayOf(startTime.toString(), endTime.toString())

        val sortOrder = "${CalendarContract.Instances.BEGIN} ASC"

        // Use Instances URI to handle recurring events properly
        val uri =
            CalendarContract.Instances.CONTENT_URI.buildUpon()
                .appendPath(startTime.toString())
                .appendPath(endTime.toString())
                .build()

        try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use {
                cursor ->
                while (cursor.moveToNext()) {
                    val task = parseTask(cursor, showCompletedTasks) ?: continue
                    tasks.add(task)
                }
            }
        } catch (e: Exception) {
            // Handle query errors gracefully
            return emptyList()
        }

        // Also query for tasks without due dates
        tasks.addAll(getUndatedTasks(context, taskCalendarIds, showCompletedTasks))

        return tasks
    }

    private fun getUndatedTasks(
        context: Context,
        taskCalendarIds: Set<String>,
        showCompletedTasks: Boolean
    ): List<TaskData> {
        val tasks = arrayListOf<TaskData>()

        if (taskCalendarIds.isEmpty()) {
            return emptyList()
        }

        val calendarSelection = taskCalendarIds.joinToString(",") { "?" }

        val projection =
            arrayOf(
                CalendarContract.Events.CALENDAR_ID,
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DESCRIPTION,
                CalendarContract.Events.STATUS,
                CalendarContract.Events.DTSTART,
            )

        // Query for events with no start date (some task apps create these)
        val selection =
            "${CalendarContract.Events.CALENDAR_ID} IN ($calendarSelection) AND " +
                "(${CalendarContract.Events.DTSTART} IS NULL OR ${CalendarContract.Events.DTSTART} = 0)"

        val selectionArgs = taskCalendarIds.toTypedArray()

        try {
            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val calendarId =
                        cursor.safeGet(CalendarContract.Events.CALENDAR_ID, Cursor::getLong)
                    val eventId = cursor.safeGet(CalendarContract.Events._ID, Cursor::getLong)
                    val title = cursor.safeGet(CalendarContract.Events.TITLE, Cursor::getString)
                    val description =
                        cursor.safeGet(CalendarContract.Events.DESCRIPTION, Cursor::getString)
                    val status = cursor.safeGet(CalendarContract.Events.STATUS, Cursor::getInt)

                    if (calendarId == null || eventId == null || title.isNullOrBlank()) {
                        continue
                    }

                    val isCompleted = status == CalendarContract.Events.STATUS_CANCELED

                    // Skip completed tasks if preference is off
                    if (isCompleted && !showCompletedTasks) {
                        continue
                    }

                    tasks.add(
                        TaskData(
                            id = eventId,
                            title = title,
                            dueDate = null, // No due date
                            isCompleted = isCompleted,
                            calendarId = calendarId,
                            description = description
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Handle query errors gracefully
        }

        return tasks
    }

    private fun parseTask(cursor: Cursor, showCompletedTasks: Boolean): TaskData? {
        val calendarId = cursor.safeGet(CalendarContract.Instances.CALENDAR_ID, Cursor::getLong)
        val eventId = cursor.safeGet(CalendarContract.Instances.EVENT_ID, Cursor::getLong)
        val title = cursor.safeGet(CalendarContract.Instances.TITLE, Cursor::getString)
        val description = cursor.safeGet(CalendarContract.Instances.DESCRIPTION, Cursor::getString)
        val status = cursor.safeGet(CalendarContract.Instances.STATUS, Cursor::getInt)
        val beginTime = cursor.safeGet(CalendarContract.Instances.BEGIN, Cursor::getLong)

        if (calendarId == null || eventId == null || title.isNullOrBlank()) {
            return null
        }

        // Consider canceled events as completed tasks
        val isCompleted = status == CalendarContract.Events.STATUS_CANCELED

        // Skip completed tasks if preference is off
        if (isCompleted && !showCompletedTasks) {
            return null
        }

        return TaskData(
            id = eventId,
            title = title,
            dueDate = beginTime,
            isCompleted = isCompleted,
            calendarId = calendarId,
            description = description
        )
    }

    private fun getStartAndEndTime(prefs: AgendaWidgetPrefs, widgetId: String): Pair<Long, Long> {
        // Start from the beginning of today
        val startTime =
            Calendar.getInstance()
                .apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                .timeInMillis

        val endTime =
            Calendar.getInstance()
                .apply {
                    timeInMillis = startTime
                    add(Calendar.DAY_OF_MONTH, prefs.getNumberOfDays(widgetId))
                }
                .timeInMillis

        return Pair(startTime, endTime)
    }

    private fun <T> Cursor.safeGet(columnName: String, getter: Cursor.(Int) -> T): T? {
        val columnIndex = getColumnIndex(columnName)
        return if (columnIndex != -1 && !isNull(columnIndex)) getter(columnIndex) else null
    }

    companion object {
        /**
         * Marks a task as completed or not completed by updating the underlying calendar event.
         * Returns true if the update was successful.
         */
        fun setTaskCompleted(context: Context, taskId: Long, completed: Boolean): Boolean {
            return try {
                val values = ContentValues().apply {
                    put(
                        CalendarContract.Events.STATUS,
                        if (completed) CalendarContract.Events.STATUS_CANCELED
                        else CalendarContract.Events.STATUS_CONFIRMED
                    )
                }
                val uri = CalendarContract.Events.CONTENT_URI
                val selection = "${CalendarContract.Events._ID} = ?"
                val selectionArgs = arrayOf(taskId.toString())

                val rowsUpdated = context.contentResolver.update(uri, values, selection, selectionArgs)
                rowsUpdated > 0
            } catch (e: Exception) {
                false
            }
        }
    }
}
