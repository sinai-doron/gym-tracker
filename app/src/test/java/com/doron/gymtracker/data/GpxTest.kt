package com.doron.gymtracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GpxTest {

    @Test
    fun `a written run reads back the same`() {
        val points = listOf(
            LocationPoint(51.5174133, -0.1367549, null, 26.9, 5f, 1_761_114_471_000),
            LocationPoint(51.5175, -0.1369, null, null, 5f, 1_761_114_473_000)
        )
        val xml = Gpx.write("ריצה <בוקר>", "RUNNING", points)
        assertTrue(xml.contains("<time>2025-10-22T06:27:51Z</time>"))
        assertTrue(xml.contains("<type>running</type>"))
        assertTrue(xml.contains("ריצה &lt;בוקר&gt;"))
        val parsed = Gpx.parse(xml)
        assertEquals(2, parsed.points.size)
        assertEquals(51.5174133, parsed.points[0].latitude, 1e-7)
        assertEquals(26.9, parsed.points[0].elevationM!!, 0.05)
        assertEquals(null, parsed.points[1].elevationM)
    }
}
