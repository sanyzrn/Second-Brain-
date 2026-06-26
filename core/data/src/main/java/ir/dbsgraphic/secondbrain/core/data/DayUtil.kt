package ir.dbsgraphic.secondbrain.core.data

import java.util.Calendar

/** Local start-of-day helpers used for habit streaks (one bucket per calendar day). */
object DayUtil {
    fun startOfDay(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun startOfToday(): Long = startOfDay(System.currentTimeMillis())

    /** The start-of-day before [dayStart]. */
    fun previousDay(dayStart: Long): Long = startOfDay(dayStart - 12 * 60 * 60 * 1000L)
}
