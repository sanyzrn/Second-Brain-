package ir.dbsgraphic.secondbrain.core.data

import ir.dbsgraphic.secondbrain.core.calendar.DeviceCalendarSync
import javax.inject.Inject

/**
 * Removes a mirrored device-calendar event. Abstracted (like [ReminderScheduler])
 * so [ItemRepositoryImpl] can clean up a mirrored event when an item is trashed
 * or deleted without depending on the platform calendar directly — and so the
 * repository stays unit-testable with [NoOpCalendarMirror].
 */
interface CalendarMirror {
    suspend fun remove(eventId: Long)
}

/** Default no-op (used in tests / when calendar mirroring isn't wired). */
object NoOpCalendarMirror : CalendarMirror {
    override suspend fun remove(eventId: Long) = Unit
}

/** Real implementation: deletes the event via the device calendar (best-effort). */
class DeviceCalendarMirror @Inject constructor(
    private val deviceCalendar: DeviceCalendarSync,
) : CalendarMirror {
    override suspend fun remove(eventId: Long) {
        runCatching { deviceCalendar.deleteEvent(eventId) }
    }
}
