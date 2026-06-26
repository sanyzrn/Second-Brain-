package ir.dbsgraphic.secondbrain.core.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class MedicineTest {

    @Test
    fun `codec round trips details`() {
        val details = MedicineDetails(
            dosage = "۱ قرص",
            times = listOf("08:00", "20:00"),
            stock = 12,
            refillAt = 4,
        )
        val decoded = MedicineCodec.decode(MedicineCodec.encode(details))
        assertEquals(details, decoded)
        assertEquals(2, decoded.dosesPerDay)
    }

    @Test
    fun `decode of blank yields defaults`() {
        val d = MedicineCodec.decode(null)
        assertEquals(1, d.dosesPerDay)
        assertEquals(0, d.stock)
    }

    @Test
    fun `refill flags fire at threshold and on empty`() {
        assertTrue(MedicineDetails(stock = 5, refillAt = 5).needsRefill)
        assertFalse(MedicineDetails(stock = 6, refillAt = 5).needsRefill)
        assertTrue(MedicineDetails(stock = 0).isOut)
        assertFalse(MedicineDetails(stock = 1).isOut)
    }

    @Test
    fun `default times match doses per day and clamp`() {
        assertEquals(listOf("09:00"), MedicineSchedule.defaultTimes(1))
        assertEquals(listOf("09:00", "21:00"), MedicineSchedule.defaultTimes(2))
        assertEquals(4, MedicineSchedule.defaultTimes(9).size) // clamped to 4
        assertEquals(1, MedicineSchedule.defaultTimes(0).size) // clamped to 1
    }

    @Test
    fun `next dose picks the soonest remaining slot today`() {
        val now = todayAt(hour = 10, minute = 0)
        val next = MedicineSchedule.nextDose(listOf("09:00", "21:00"), now)!!
        assertTrue(next > now)
        assertEquals(21, hourOf(next))
    }

    @Test
    fun `next dose rolls to tomorrow when all slots have passed`() {
        val now = todayAt(hour = 22, minute = 30)
        val next = MedicineSchedule.nextDose(listOf("09:00", "21:00"), now)!!
        assertTrue(next > now)
        assertEquals(9, hourOf(next))
    }

    @Test
    fun `next dose of empty schedule is null`() {
        assertNull(MedicineSchedule.nextDose(emptyList(), System.currentTimeMillis()))
    }

    private fun todayAt(hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun hourOf(millis: Long): Int =
        Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.HOUR_OF_DAY)
}
