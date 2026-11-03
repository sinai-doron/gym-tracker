package com.doron.gymtracker.data.templates

import android.content.Context
import com.doron.gymtracker.data.repository.ExerciseLibrary
import com.doron.gymtracker.data.repository.WorkoutRepository
import com.doron.gymtracker.domain.model.Program
import com.doron.gymtracker.domain.model.VariantRule
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The programmes a user can start from, and writing one as their active programme. */
@Singleton
class ProgramTemplates @Inject constructor(
    @ApplicationContext private val context: Context,
    private val library: ExerciseLibrary,
    private val workouts: WorkoutRepository
) {
    suspend fun all(): List<ProgramTemplate> = withContext(Dispatchers.IO) {
        context.assets.list(ProgramTemplate.DIR).orEmpty()
            .filter { it.endsWith(".json") }
            .map { file -> ProgramTemplate.parse(context.assets.open("${ProgramTemplate.DIR}/$file").bufferedReader().use { it.readText() }) }
            .sortedBy { it.order }
    }

    /**
     * Writes [template] as a new programme starting [startDate], named and with its exercises
     * named in [lang], and makes it the active one; returns its id.
     */
    suspend fun install(template: ProgramTemplate, lang: String, startDate: LocalDate): String {
        val program = Program(
            id = UUID.randomUUID().toString(),
            name = template.name(lang),
            startDate = startDate,
            deloadEveryWeeks = null,
            variantRule = VariantRule.NONE
        )
        val exercises = library.all(lang).associateBy { it.id }
        workouts.installProgram(program, template.toNewWorkouts(exercises, lang) { "custom_${UUID.randomUUID()}" })
        return program.id
    }
}
