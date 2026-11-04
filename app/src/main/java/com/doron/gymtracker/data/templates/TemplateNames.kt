package com.doron.gymtracker.data.templates

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A programme or workout installed from a template is saved under its name in the language of
 * the day it was installed ("גוף מלא A"). This gives that name in the app's language now ("Full
 * body A"), from the templates' own translations. A name the user typed matches no template and
 * comes back as it is.
 */
@Singleton
class TemplateNames @Inject constructor(@ApplicationContext private val context: Context) {

    fun program(saved: String, lang: String): String = index(context).program(saved, lang)

    fun workout(saved: String, lang: String): String = index(context).workout(saved, lang)

    companion object {
        @Volatile private var loaded: TemplateNameIndex? = null

        /** Read once: four small files, the same for the app's lifetime. */
        private fun index(context: Context): TemplateNameIndex = loaded ?: synchronized(this) {
            loaded ?: TemplateNameIndex(
                context.assets.list(ProgramTemplate.DIR).orEmpty()
                    .filter { it.endsWith(".json") }
                    .map { file -> ProgramTemplate.parse(context.assets.open("${ProgramTemplate.DIR}/$file").bufferedReader().use { it.readText() }) }
            ).also { loaded = it }
        }
    }
}

/** Every template name, in any language, to its names in every language. */
class TemplateNameIndex(templates: List<ProgramTemplate>) {
    private val programs = templates.flatMap { t -> t.names.values.map { it to t.names } }.toMap()
    private val workouts = templates.flatMap { it.workouts }.flatMap { w -> w.names.values.map { it to w.names } }.toMap()

    fun program(saved: String, lang: String): String = programs[saved]?.get(lang) ?: saved

    fun workout(saved: String, lang: String): String = workouts[saved]?.get(lang) ?: saved
}
