package ir.dbsgraphic.secondbrain.core.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Calendar

/**
 * The medicine payload carried in an Item's [Item.details] JSON. A medicine is an
 * ordinary Item (type=medicine): its dosage and daily schedule live here, the
 * *next* dose reuses [Item.reminderAt] (so it notifies for free), and adherence
 * is recorded in the shared `habit_checkins` table (the Phase 13 cadence engine).
 *
 * [times] are local daily dose times as `HH:mm`; their count is doses-per-day.
 * [stock] is doses on hand; a refill is due once it falls to [refillAt] or below.
 */
@Serializable
data class MedicineDetails(
    val dosage: String = "",
    val times: List<String> = listOf("09:00"),
    val stock: Int = 0,
    val refillAt: Int = 5,
) {
    val dosesPerDay: Int get() = times.size.coerceAtLeast(1)
    val isOut: Boolean get() = stock <= 0
    val needsRefill: Boolean get() = stock <= refillAt
}

/** Reads/writes [MedicineDetails] tolerantly so older items never break. */
object MedicineCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun decode(details: String?): MedicineDetails =
        if (details.isNullOrBlank()) MedicineDetails()
        else runCatching { json.decodeFromString(MedicineDetails.serializer(), details) }
            .getOrDefault(MedicineDetails())

    fun encode(details: MedicineDetails): String =
        json.encodeToString(MedicineDetails.serializer(), details)
}

/** Schedule maths for medicines — kept apart from Room/Android types. */
object MedicineSchedule {

    /** Sensible default dose times for a given doses-per-day (1..4, else clamped). */
    fun defaultTimes(dosesPerDay: Int): List<String> = when (dosesPerDay.coerceIn(1, 4)) {
        1 -> listOf("09:00")
        2 -> listOf("09:00", "21:00")
        3 -> listOf("08:00", "14:00", "20:00")
        else -> listOf("08:00", "13:00", "18:00", "22:00")
    }

    /**
     * The epoch-millis of the next dose at or after [now]: the soonest of today's
     * remaining [times], or the earliest time tomorrow if none are left today.
     * Returns null when [times] is empty.
     */
    fun nextDose(times: List<String>, now: Long): Long? {
        val parsed = times.mapNotNull(::parseHm).sortedWith(compareBy({ it.first }, { it.second }))
        if (parsed.isEmpty()) return null
        val today = parsed.map { (h, m) -> atTime(now, h, m, dayOffset = 0) }
        today.filter { it > now }.minOrNull()?.let { return it }
        // All of today's doses have passed — first dose tomorrow.
        val (h, m) = parsed.first()
        return atTime(now, h, m, dayOffset = 1)
    }

    private fun parseHm(value: String): Pair<Int, Int>? {
        val parts = value.trim().split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h to m
    }

    private fun atTime(reference: Long, hour: Int, minute: Int, dayOffset: Int): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = reference
            add(Calendar.DAY_OF_YEAR, dayOffset)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
