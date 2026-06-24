package ir.dbsgraphic.secondbrain.core.designsystem.util

/**
 * Short Persian relative time ("۵ دقیقه پیش"). A lightweight stand-in until the
 * Timeline brings proper Jalali date markers (Phase 3).
 */
fun relativeTimeFa(timestamp: Long, now: Long = System.currentTimeMillis()): String {
    val diff = now - timestamp
    val minute = 60_000L
    val hour = 60 * minute
    val day = 24 * hour
    return when {
        diff < minute -> "همین حالا"
        diff < hour -> "${(diff / minute).toString().toPersianDigits()} دقیقه پیش"
        diff < day -> "${(diff / hour).toString().toPersianDigits()} ساعت پیش"
        else -> "${(diff / day).toString().toPersianDigits()} روز پیش"
    }
}
