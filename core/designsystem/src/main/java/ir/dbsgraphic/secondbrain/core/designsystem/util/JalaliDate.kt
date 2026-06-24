package ir.dbsgraphic.secondbrain.core.designsystem.util

import java.util.Calendar

/**
 * Gregorian → Jalali (Shamsi) conversion and Persian date formatting for the
 * Timeline. Pure Kotlin, offline, no dependency — the classic jalaali algorithm.
 */
object JalaliDate {

    private val monthNames = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند",
    )

    data class Ymd(val year: Int, val month: Int, val day: Int)

    private fun gregorianToJalali(gy: Int, gm: Int, gd: Int): Ymd {
        val gdm = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        val gy2 = if (gm > 2) gy + 1 else gy
        var days = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) +
            ((gy2 + 399) / 400) + gd + gdm[gm - 1]
        var jy = -1595 + (33 * (days / 12053))
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val jm: Int
        val jd: Int
        if (days < 186) {
            jm = 1 + (days / 31)
            jd = 1 + (days % 31)
        } else {
            jm = 7 + ((days - 186) / 30)
            jd = 1 + ((days - 186) % 30)
        }
        return Ymd(jy, jm, jd)
    }

    private fun toYmd(epochMillis: Long): Ymd {
        val cal = Calendar.getInstance().apply { timeInMillis = epochMillis }
        return gregorianToJalali(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
        )
    }

    /** Stable per-day key for grouping (e.g. Timeline day sections). */
    fun dayKey(epochMillis: Long): String = with(toYmd(epochMillis)) { "$year-$month-$day" }

    /** "۲۴ خرداد ۱۴۰۴" */
    fun formatDate(epochMillis: Long): String = with(toYmd(epochMillis)) {
        "${day.toString().toPersianDigits()} ${monthNames[month - 1]} ${year.toString().toPersianDigits()}"
    }

    /** "امروز" / "دیروز" / full date — the Timeline's day markers. */
    fun formatDayHeader(epochMillis: Long, now: Long = System.currentTimeMillis()): String {
        val target = dayKey(epochMillis)
        if (target == dayKey(now)) return "امروز"
        if (target == dayKey(now - DAY_MS)) return "دیروز"
        return formatDate(epochMillis)
    }

    /** "14:02" — data voice, Latin digits, paired with the mono style. */
    fun formatTime(epochMillis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = epochMillis }
        val h = cal.get(Calendar.HOUR_OF_DAY)
        val m = cal.get(Calendar.MINUTE)
        return "%02d:%02d".format(h, m)
    }

    private const val DAY_MS = 24L * 60 * 60 * 1000
}
