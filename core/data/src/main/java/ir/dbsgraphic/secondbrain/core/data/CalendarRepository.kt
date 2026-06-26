package ir.dbsgraphic.secondbrain.core.data

import ir.dbsgraphic.secondbrain.core.calendar.CalendarInfo
import ir.dbsgraphic.secondbrain.core.calendar.DeviceCalendarSync
import ir.dbsgraphic.secondbrain.core.calendar.DeviceEvent
import ir.dbsgraphic.secondbrain.core.database.dao.ItemDao
import javax.inject.Inject

/** Outcome of mirroring reminders into the device calendar. */
data class CalendarSyncResult(val created: Int, val updated: Int)

/**
 * Calendars, both ways (Phase 15):
 *  - **ICS** — a portable `.ics` of every item with a due date, and import back
 *    (no permission, fully offline).
 *  - **Device calendar** — mirror items-with-reminders into a calendar the user
 *    picks, read upcoming events, and pull an event into the Inbox.
 *
 * The item is the atom: a mirrored event's id is stored on the Item itself
 * ([Item.calendarEventId]), so re-syncing updates rather than duplicates.
 */
interface CalendarRepository {

    // ICS portability ────────────────────────────────────────────────────────
    suspend fun exportIcs(): String
    /** Imports events as Inbox items with reminders; returns how many were added. */
    suspend fun importIcs(text: String): Int

    // Device calendar ──────────────────────────────────────────────────────────
    fun canReadCalendar(): Boolean
    fun canWriteCalendar(): Boolean
    suspend fun writableCalendars(): List<CalendarInfo>
    suspend fun upcomingEvents(daysAhead: Int = 30): List<DeviceEvent>
    /** Push every item with a reminder into [calendarId] (create or update). */
    suspend fun mirrorReminders(calendarId: Long): CalendarSyncResult
    /** Pull a device event into the Inbox as a reminder item; returns the new id. */
    suspend fun importDeviceEvent(event: DeviceEvent): String
}

class CalendarRepositoryImpl @Inject constructor(
    private val itemDao: ItemDao,
    private val itemRepository: ItemRepository,
    private val deviceCalendar: DeviceCalendarSync,
    private val clock: Clock,
) : CalendarRepository {

    override suspend fun exportIcs(): String {
        val events = itemDao.getReminders().mapNotNull { item ->
            val start = item.reminderAt ?: return@mapNotNull null
            IcsEvent(
                uid = "${item.id}@secondbrain",
                summary = item.content.ifBlank { "(بدون عنوان)" },
                start = start,
            )
        }
        return IcsCodec.export(events)
    }

    override suspend fun importIcs(text: String): Int {
        val events = IcsCodec.parse(text)
        var added = 0
        for (event in events) {
            val id = itemRepository.captureShared(content = event.summary)
            itemRepository.setReminder(id, event.start)
            added++
        }
        return added
    }

    override fun canReadCalendar(): Boolean = deviceCalendar.canRead()

    override fun canWriteCalendar(): Boolean = deviceCalendar.canWrite()

    override suspend fun writableCalendars(): List<CalendarInfo> = deviceCalendar.writableCalendars()

    override suspend fun upcomingEvents(daysAhead: Int): List<DeviceEvent> =
        deviceCalendar.upcoming(daysAhead)

    override suspend fun mirrorReminders(calendarId: Long): CalendarSyncResult {
        if (!deviceCalendar.canWrite()) return CalendarSyncResult(0, 0)
        var created = 0
        var updated = 0
        for (item in itemDao.getReminders()) {
            val start = item.reminderAt ?: continue
            val title = item.content.ifBlank { "(بدون عنوان)" }
            val existing = item.calendarEventId
            if (existing == null) {
                val eventId = deviceCalendar.createEvent(calendarId, title, start)
                if (eventId != null) {
                    itemDao.update(item.copy(calendarEventId = eventId, updatedAt = clock.now()))
                    created++
                }
            } else {
                if (deviceCalendar.updateEvent(existing, title, start)) updated++
            }
        }
        return CalendarSyncResult(created = created, updated = updated)
    }

    override suspend fun importDeviceEvent(event: DeviceEvent): String {
        val id = itemRepository.captureShared(content = event.title.ifBlank { "(بدون عنوان)" })
        itemRepository.setReminder(id, event.begin)
        return id
    }
}
