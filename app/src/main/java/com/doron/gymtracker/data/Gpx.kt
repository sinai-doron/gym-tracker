package com.doron.gymtracker.data

import com.doron.gymtracker.domain.model.RoutePoint

/** Reads the route out of a GPX file: its track points, or route points if it has no track. */
object Gpx {

    data class Parsed(val name: String?, val points: List<RoutePoint>)

    private val pointTag = Regex("""<(trkpt|rtept)\b([^>]*?)(/>|>(.*?)</\1>)""", RegexOption.DOT_MATCHES_ALL)
    private val latAttr = Regex("""\blat\s*=\s*["']([-+0-9.eE]+)["']""")
    private val lonAttr = Regex("""\blon\s*=\s*["']([-+0-9.eE]+)["']""")
    private val eleTag = Regex("""<ele>\s*([-+0-9.eE]+)\s*</ele>""")
    private val nameTag = Regex("""<name>\s*(?:<!\[CDATA\[)?(.*?)(?:]]>)?\s*</name>""", RegexOption.DOT_MATCHES_ALL)

    fun parse(xml: String): Parsed {
        val all = pointTag.findAll(xml).mapNotNull { m ->
            val attrs = m.groupValues[2]
            val lat = latAttr.find(attrs)?.groupValues?.get(1)?.toDoubleOrNull() ?: return@mapNotNull null
            val lon = lonAttr.find(attrs)?.groupValues?.get(1)?.toDoubleOrNull() ?: return@mapNotNull null
            val ele = eleTag.find(m.groupValues[4])?.groupValues?.get(1)?.toDoubleOrNull()
            m.groupValues[1] to RoutePoint(lat, lon, ele)
        }.toList()
        // Prefer the recorded track; a file with only a planned route uses that
        val track = all.filter { it.first == "trkpt" }.map { it.second }
        val points = track.ifEmpty { all.map { it.second } }
        // The track's own name if it has one, else the file's
        val trk = xml.indexOf("<trk")
        val name = (if (trk >= 0) nameTag.find(xml, trk) else null)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
            ?: nameTag.find(xml)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
        return Parsed(name, points)
    }

    /**
     * A recorded run as a GPX 1.1 track, for apps like Strava or Garmin: every fix with its
     * time and altitude. Fixes are only kept while recording, so pauses are gaps in time.
     */
    fun write(name: String, type: String, points: List<LocationPoint>): String = buildString {
        appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        appendLine("""<gpx version="1.1" creator="GymTracker" xmlns="http://www.topografix.com/GPX/1/1">""")
        appendLine("  <metadata><name>${escape(name)}</name>${points.firstOrNull()?.let { "<time>${time(it.timestamp)}</time>" } ?: ""}</metadata>")
        appendLine("  <trk>")
        appendLine("    <name>${escape(name)}</name>")
        // GPX's activity type, as Strava reads it
        appendLine("    <type>${if (type == "WALKING") "walking" else "running"}</type>")
        appendLine("    <trkseg>")
        points.forEach { p ->
            append("      <trkpt lat=\"${"%.7f".format(java.util.Locale.US, p.latitude)}\" lon=\"${"%.7f".format(java.util.Locale.US, p.longitude)}\">")
            p.altitude?.let { append("<ele>${"%.1f".format(java.util.Locale.US, it)}</ele>") }
            append("<time>${time(p.timestamp)}</time>")
            appendLine("</trkpt>")
        }
        appendLine("    </trkseg>")
        appendLine("  </trk>")
        appendLine("</gpx>")
    }

    private fun time(ms: Long): String = java.time.Instant.ofEpochMilli(ms).toString()

    private fun escape(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
