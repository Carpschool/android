package ca.carpschool.data

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

val DAYS = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
fun dirLabel(d: String) = if (d == "to-school") "To school" else "Home"
fun shortId(s: String) = s.removePrefix("user_").takeLast(5).uppercase()

fun schedule(c: Commute): String {
    if (c.days.isNotEmpty()) {
        val d = c.days.sorted()
        if (d == listOf(1, 2, 3, 4, 5)) return "Weekdays"
        return "Every " + d.joinToString(", ") { DAYS[it] }
    }
    val f = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
    return c.dates.joinToString(", ") { runCatching { LocalDate.parse(it).format(f) }.getOrDefault(it) }
}

fun todayISO(): String = LocalDate.now().toString()
fun runsToday(c: Commute): Boolean = c.days.contains(LocalDate.now().dayOfWeek.value % 7) || c.dates.contains(todayISO())

fun clock(iso: String): String = runCatching {
    OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
}.getOrDefault("")

fun greeting(): String {
    val h = java.time.LocalTime.now().hour
    return if (h < 12) "Good morning" else if (h < 18) "Good afternoon" else "Good evening"
}

fun errText(e: Throwable) = e.message ?: "Something went wrong"
