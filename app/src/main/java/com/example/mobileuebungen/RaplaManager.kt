// add jsoup depenedency: implementation(libs.jsoup)

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

data class RaplaResult(val weeks: List<RaplaWeek>) {

    fun allEventTitles(): Set<String> {
        return weeks
            .flatMap { it.events }
            .map { it.title }
            .toSet()
    }
}

data class RaplaWeek(val weekNumber: RaplaWeekNumber, val events: List<RaplaEvent>)

data class RaplaWeekNumber(val number: Number) {

    constructor(calendarWeek: String):
            this(calendarWeek.filter { it.isDigit() }.toInt())

}

data class RaplaEvent(
    val title: String,
    val date: LocalDate,
    val startTime: String,
    val endTime: String,
    val dayOfWeek: String,
    val course: String?,
    val room: String?
)

data class RaplaDateHeader(val date: LocalDate, val dayName: String) {

    companion object {

        private val sdf = SimpleDateFormat("dd.MM.", Locale.GERMAN)
        private val now = LocalDate.now()

        private val dayNames = mapOf(
            "Mo" to "Montag",
            "Di" to "Dienstag",
            "Mi" to "Mittwoch",
            "Do" to "Donnerstag",
            "Fr" to "Freitag",
            "Sa" to "Samstag",
            "So" to "Sonntag"
        )

        fun from(header: String): RaplaDateHeader {
            val parts = header.split(" ")
            val dayAbbrev = parts.getOrNull(0) ?: ""
            val date = sdf.parse(parts.getOrNull(1) ?: "")
            val localDate: LocalDate =
                date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            val newDate = localDate.withYear(now.year)

            return RaplaDateHeader(newDate, dayNames[dayAbbrev] ?: dayAbbrev)
        }
    }
}

class RaplaParser {

    fun parse(htmlText: String): RaplaResult? {
        val doc: Document = Jsoup.parse(htmlText)
        val tables = doc.select("table.week_table")
        val weeks = tables.map { parseWeek(it) }
        return RaplaResult(weeks)
    }

    private fun parseWeek(table: Element): RaplaWeek {
        val weekNumberText = table.select("th.week_number").text().trim()
        val weekNumber = RaplaWeekNumber(weekNumberText)

        val dateHeaders = table.select("td.week_header")
            .map { it.text().trim() }
            .map { RaplaDateHeader.from(it) }

        val eventBlocks = table.select("td.week_block")
        val events = eventBlocks
            .mapNotNull { parseEvent(it, dateHeaders) }
            .sortedBy { it.date }

        return RaplaWeek(weekNumber, events)
    }

    private fun parseEvent(element: Element, dateHeaders: List<RaplaDateHeader>): RaplaEvent? {
        val columnIndex = getColumnIndex(element)

        return element.selectFirst("a")?.let { block ->
            val timeAndTitle = block.html().split("<br>")
            val timeRange = timeAndTitle[0].replace("&#160;", " ").trim()
            val title = timeAndTitle[1].trim()
            val times = timeRange.split("-")
                .map { it.replace("[^0-9:]".toRegex(), "") }
            val resources = element.select("span.resource")
            return dateHeaders.getOrNull(columnIndex / 3)?.let {
                RaplaEvent(
                    title = title,
                    startTime = times.getOrNull(0)?.trim() ?: "",
                    endTime = times.getOrNull(1)?.trim() ?: "",
                    date = it.date,
                    dayOfWeek = it.dayName,
                    course = resources.getOrNull(0)?.text()?.trim(),
                    room = resources.getOrNull(1)?.text()?.trim()
                )
            }
        }
    }

    private fun getColumnIndex(element: Element): Int {
        var index = 0
        var sibling = element.previousElementSibling()

        while (sibling != null) {
            val colspan = sibling.attr("colspan").toIntOrNull() ?: 1
            index += colspan
            sibling = sibling.previousElementSibling()
        }

        return index
    }
}
