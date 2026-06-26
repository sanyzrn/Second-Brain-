package ir.dbsgraphic.secondbrain.core.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** A calendar event in transit, independent of Room or Android. */
data class IcsEvent(val uid: String?, val summary: String, val start: Long)

/**
 * A small, dependency-free iCalendar (RFC 5545) reader/writer — enough to carry
 * items with due dates in and out as a portable `.ics` file (Constitution §9:
 * the user can take their data anywhere). Times are written in UTC; the parser
 * accepts UTC (`…Z`), floating local, and all-day `DATE` values.
 */
object IcsCodec {

    private const val CRLF = "\r\n"

    private fun utcFormat(): SimpleDateFormat =
        SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

    // ── Export ──────────────────────────────────────────────────────────────

    fun export(events: List<IcsEvent>): String {
        val fmt = utcFormat()
        val stamp = fmt.format(Date())
        return buildString {
            append("BEGIN:VCALENDAR").append(CRLF)
            append("VERSION:2.0").append(CRLF)
            append("PRODID:-//Second Brain//SecondBrain//FA").append(CRLF)
            append("CALSCALE:GREGORIAN").append(CRLF)
            events.forEach { event ->
                append("BEGIN:VEVENT").append(CRLF)
                append("UID:").append(event.uid ?: "${System.nanoTime()}@secondbrain").append(CRLF)
                append("DTSTAMP:").append(stamp).append(CRLF)
                append("DTSTART:").append(fmt.format(Date(event.start))).append(CRLF)
                append("SUMMARY:").append(escape(event.summary)).append(CRLF)
                append("END:VEVENT").append(CRLF)
            }
            append("END:VCALENDAR").append(CRLF)
        }
    }

    private fun escape(text: String): String = text
        .replace("\\", "\\\\")
        .replace(";", "\\;")
        .replace(",", "\\,")
        .replace("\n", "\\n")

    private fun unescape(text: String): String {
        val sb = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c == '\\' && i + 1 < text.length) {
                when (val next = text[i + 1]) {
                    'n', 'N' -> sb.append('\n')
                    else -> sb.append(next)
                }
                i += 2
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }

    // ── Import ──────────────────────────────────────────────────────────────

    fun parse(text: String): List<IcsEvent> {
        val lines = unfold(text)
        val events = mutableListOf<IcsEvent>()
        var inEvent = false
        var uid: String? = null
        var summary: String? = null
        var start: Long? = null
        for (line in lines) {
            when {
                line.equals("BEGIN:VEVENT", ignoreCase = true) -> {
                    inEvent = true; uid = null; summary = null; start = null
                }
                line.equals("END:VEVENT", ignoreCase = true) -> {
                    if (inEvent && summary != null && start != null) {
                        events += IcsEvent(uid = uid, summary = summary!!, start = start!!)
                    }
                    inEvent = false
                }
                inEvent -> {
                    val colon = line.indexOf(':')
                    if (colon <= 0) continue
                    val name = line.substring(0, colon).substringBefore(';').uppercase(Locale.US)
                    val value = line.substring(colon + 1)
                    when (name) {
                        "UID" -> uid = value.trim()
                        "SUMMARY" -> summary = unescape(value)
                        "DTSTART" -> start = parseDate(value.trim())
                    }
                }
            }
        }
        return events
    }

    /** Join RFC 5545 folded lines (a leading space/tab continues the previous). */
    private fun unfold(text: String): List<String> {
        val raw = text.replace("\r\n", "\n").split("\n")
        val out = mutableListOf<String>()
        for (line in raw) {
            if (line.isNotEmpty() && (line[0] == ' ' || line[0] == '\t') && out.isNotEmpty()) {
                out[out.lastIndex] = out.last() + line.substring(1)
            } else {
                out += line
            }
        }
        return out
    }

    private fun parseDate(value: String): Long? {
        if (value.isEmpty()) return null
        val patterns = when {
            value.endsWith("Z") -> listOf("yyyyMMdd'T'HHmmss'Z'" to "UTC")
            value.contains("T") -> listOf("yyyyMMdd'T'HHmmss" to null)
            else -> listOf("yyyyMMdd" to null) // all-day DATE
        }
        for ((pattern, zone) in patterns) {
            val fmt = SimpleDateFormat(pattern, Locale.US).apply {
                if (zone != null) timeZone = TimeZone.getTimeZone(zone)
            }
            runCatching { return fmt.parse(value)?.time }
        }
        return null
    }
}
