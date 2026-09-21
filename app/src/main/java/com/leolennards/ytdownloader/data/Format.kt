package com.leolennards.ytdownloader.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun formatDuration(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%d:%02d", m, s)
}

fun formatSize(bytes: Long): String {
    val mb = bytes / 1048576.0
    return if (mb >= 1) String.format(Locale.US, "%.1f MB", mb)
    else String.format(Locale.US, "%d KB", (bytes / 1024).coerceAtLeast(1))
}

private fun startOfDay(ms: Long): Long {
    val c = Calendar.getInstance()
    c.timeInMillis = ms
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
}

fun relativeDay(savedAtMs: Long, nowMs: Long = System.currentTimeMillis()): String {
    val days = ((startOfDay(nowMs) - startOfDay(savedAtMs)) / 86_400_000L).toInt()
    return when {
        days <= 0 -> "Today"
        days == 1 -> "Yesterday"
        days < 7 -> "This week"
        days < 14 -> "Last week"
        else -> SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(savedAtMs))
    }
}

private val musicNoise = Regex(
    """\s*[(\[]\s*(?:official\s+)?(?:music\s+|lyric\s+)?(?:audio|video|lyrics?|visuali[sz]er)\s*[)\]]""",
    RegexOption.IGNORE_CASE,
)

// "Drake - Not You Too (Audio) ft. Chris Brown" -> "Not You Too ft. Chris Brown"
fun cleanMusicTitle(raw: String): String {
    var title = raw.replace(musicNoise, "").replace(Regex("\\s+"), " ").trim()
    val dash = title.indexOf(" - ")
    if (dash > 0 && dash < title.length - 3) title = title.substring(dash + 3).trim()
    return title.ifBlank { raw }
}
