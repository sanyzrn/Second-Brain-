package ir.dbsgraphic.secondbrain.core.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IcsCodecTest {

    @Test
    fun `export wraps events in a VCALENDAR with VEVENTs`() {
        val ics = IcsCodec.export(listOf(IcsEvent(uid = "a@sb", summary = "خرید نان", start = 0L)))
        assertTrue(ics.contains("BEGIN:VCALENDAR"))
        assertTrue(ics.contains("END:VCALENDAR"))
        assertTrue(ics.contains("BEGIN:VEVENT"))
        assertTrue(ics.contains("UID:a@sb"))
        assertTrue(ics.contains("SUMMARY:خرید نان"))
        // epoch 0 is 1970-01-01T00:00:00Z
        assertTrue(ics.contains("DTSTART:19700101T000000Z"))
    }

    @Test
    fun `summary special characters are escaped and unescaped on round trip`() {
        val original = "جلسه: کار, مهم; نکته\nخط دوم"
        val ics = IcsCodec.export(listOf(IcsEvent(uid = "x", summary = original, start = 0L)))
        // commas, semicolons and newlines are escaped in the wire form
        assertTrue(ics.contains("\\,"))
        assertTrue(ics.contains("\\;"))
        assertTrue(ics.contains("\\n"))
        val parsed = IcsCodec.parse(ics)
        assertEquals(1, parsed.size)
        assertEquals(original, parsed.first().summary)
    }

    @Test
    fun `parse reads back exported UTC start time`() {
        val start = 1_700_000_000_000L
        val ics = IcsCodec.export(listOf(IcsEvent(uid = "t", summary = "سررسید", start = start)))
        val parsed = IcsCodec.parse(ics)
        // second precision: equal once truncated to whole seconds
        assertEquals(start / 1000, parsed.first().start / 1000)
    }

    @Test
    fun `parse tolerates folded lines and DTSTART parameters`() {
        val ics = buildString {
            append("BEGIN:VCALENDAR\r\n")
            append("BEGIN:VEVENT\r\n")
            append("UID:folded@sb\r\n")
            append("DTSTART;TZID=Asia/Tehran:20260101T120000\r\n")
            append("SUMMARY:یک عنوان خیلی\r\n")
            append(" بلند که تا شده\r\n")
            append("END:VEVENT\r\n")
            append("END:VCALENDAR\r\n")
        }
        val parsed = IcsCodec.parse(ics)
        assertEquals(1, parsed.size)
        assertEquals("یک عنوان خیلی بلند که تا شده", parsed.first().summary)
        assertTrue(parsed.first().start > 0)
    }

    @Test
    fun `parse of non-calendar text yields nothing`() {
        assertEquals(emptyList<IcsEvent>(), IcsCodec.parse("just some text"))
        assertEquals(emptyList<IcsEvent>(), IcsCodec.parse(""))
    }
}
