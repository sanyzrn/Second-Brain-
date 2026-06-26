package ir.dbsgraphic.secondbrain.core.calendar

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.TimeZone
import javax.inject.Inject

/** A writable calendar on the device. */
data class CalendarInfo(val id: Long, val displayName: String, val accountName: String)

/** A single event read from the device calendar. */
data class DeviceEvent(val id: Long, val title: String, val begin: Long, val end: Long)

/**
 * Two-way bridge to the device's calendar via [CalendarContract] (Phase 15). The
 * app never owns the user's calendar — it mirrors reminders into a calendar the
 * user chooses, and reads upcoming events back. Every call is guarded by runtime
 * permission and degrades to empty/null when not granted, so the app stays fully
 * usable with calendar access off (the §12 spirit: integrations only ever add).
 */
class DeviceCalendarSync @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun canRead(): Boolean = granted(Manifest.permission.READ_CALENDAR)
    fun canWrite(): Boolean = granted(Manifest.permission.WRITE_CALENDAR)

    private fun granted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    /** Calendars the user can write events to, account-grouped. */
    suspend fun writableCalendars(): List<CalendarInfo> = withContext(Dispatchers.IO) {
        if (!canRead()) return@withContext emptyList()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
        )
        val result = mutableListOf<CalendarInfo>()
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            null,
            null,
            "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC",
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val access = cursor.getInt(3)
                if (access >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR) {
                    result += CalendarInfo(
                        id = cursor.getLong(0),
                        displayName = cursor.getString(1) ?: "تقویم",
                        accountName = cursor.getString(2) ?: "",
                    )
                }
            }
        }
        result
    }

    /** Events between now and [daysAhead] days out, soonest first. */
    suspend fun upcoming(daysAhead: Int = 30): List<DeviceEvent> = withContext(Dispatchers.IO) {
        if (!canRead()) return@withContext emptyList()
        val now = System.currentTimeMillis()
        val until = now + daysAhead.toLong() * DAY_MS
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(now.toString())
            .appendPath(until.toString())
            .build()
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
        )
        val result = mutableListOf<DeviceEvent>()
        context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            "${CalendarContract.Instances.BEGIN} ASC",
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                result += DeviceEvent(
                    id = cursor.getLong(0),
                    title = cursor.getString(1) ?: "",
                    begin = cursor.getLong(2),
                    end = cursor.getLong(3),
                )
            }
        }
        result
    }

    /** Create an event; returns its id, or null on failure / no permission. */
    suspend fun createEvent(
        calendarId: Long,
        title: String,
        begin: Long,
        durationMinutes: Int = DEFAULT_DURATION_MIN,
    ): Long? = withContext(Dispatchers.IO) {
        if (!canWrite()) return@withContext null
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DTSTART, begin)
            put(CalendarContract.Events.DTEND, begin + durationMinutes.toLong() * 60_000)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        }
        runCatching {
            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            uri?.lastPathSegment?.toLongOrNull()
        }.getOrNull()
    }

    /** Update a mirrored event's title and time. Returns true if a row changed. */
    suspend fun updateEvent(
        eventId: Long,
        title: String,
        begin: Long,
        durationMinutes: Int = DEFAULT_DURATION_MIN,
    ): Boolean = withContext(Dispatchers.IO) {
        if (!canWrite()) return@withContext false
        val values = ContentValues().apply {
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DTSTART, begin)
            put(CalendarContract.Events.DTEND, begin + durationMinutes.toLong() * 60_000)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        }
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        runCatching { context.contentResolver.update(uri, values, null, null) > 0 }.getOrDefault(false)
    }

    /** Remove a mirrored event. Returns true if a row was deleted. */
    suspend fun deleteEvent(eventId: Long): Boolean = withContext(Dispatchers.IO) {
        if (!canWrite()) return@withContext false
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        runCatching { context.contentResolver.delete(uri, null, null) > 0 }.getOrDefault(false)
    }

    private companion object {
        const val DAY_MS = 24L * 60 * 60 * 1000
        const val DEFAULT_DURATION_MIN = 60
    }
}
