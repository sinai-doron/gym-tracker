package com.doron.gymtracker.ui.components

import android.content.Context
import com.doron.gymtracker.AppLocale
import org.json.JSONArray
import org.json.JSONObject

/**
 * The run map's style (assets/map_style_run.json) with its labels in the app's language: the
 * OpenMapTiles name in [lang], else, outside Hebrew, the name in Latin letters (a street in
 * Tel Aviv reads "Herzl" in English), else the local name.
 */
fun runMapStyle(context: Context, lang: String = AppLocale.language()): JSONObject {
    val style = JSONObject(context.assets.open("map_style_run.json").bufferedReader().use { it.readText() })
    val names = listOfNotNull("name:$lang", "name:latin".takeIf { lang != "he" }, "name")
    val label = JSONArray().put("coalesce").apply { names.forEach { put(JSONArray().put("get").put(it)) } }
    val layers = style.getJSONArray("layers")
    for (i in 0 until layers.length()) {
        val layout = layers.getJSONObject(i).optJSONObject("layout") ?: continue
        if (layout.has("text-field")) layout.put("text-field", label)
    }
    return style
}
