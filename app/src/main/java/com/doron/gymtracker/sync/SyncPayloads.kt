package com.doron.gymtracker.sync

import android.database.Cursor
import androidx.sqlite.db.SupportSQLiteDatabase
import com.doron.gymtracker.data.db.SyncMigration
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate
import java.util.UUID

/** One item as doron-backend's /api/gym takes it: kind, uuid, when, deleted, and its data. */
data class OutgoingChange(
    val entity: String,
    val id: String,
    val updatedAt: Long,
    val deleted: Boolean,
    val data: JsonObject?,
)

/**
 * Turns a `sync_changes` entry into what the server stores, from the row as it is now: local
 * numeric ids become the uuids the server knows, and the app's names become the server's
 * (RUNNING -> RUN). The shapes match the backend's gym.entities.ts, which refuses anything else.
 *
 * A row that is gone by the time it is read is sent as a deletion.
 */
class SyncPayloads(private val db: SupportSQLiteDatabase) {

    fun changes(entity: String, uuid: String, changedAt: Long, deleted: Boolean): List<OutgoingChange> {
        if (entity == TRACK) return trackChanges(uuid, changedAt, deleted)
        if (deleted) return listOf(OutgoingChange(entity, uuid, changedAt, true, null))
        val data = data(entity, uuid) ?: return listOf(OutgoingChange(entity, uuid, changedAt, true, null))
        return listOf(OutgoingChange(entity, uuid, changedAt, false, data))
    }

    private fun setParts(setId: Long): JsonArray = buildJsonArray {
        db.query("SELECT * FROM set_parts WHERE setId = ? ORDER BY seq", arrayOf<Any?>(setId)).use { c ->
            while (c.moveToNext()) add(
                buildJsonObject {
                    put("side", c.strOrNull("side"))
                    put("reps", c.intOrNull("reps"))
                    put("loadKg", c.doubleOrNull("loadKg"))
                    put("durationS", c.intOrNull("durationS"))
                    put("distanceM", c.intOrNull("distanceM"))
                    put("calories", c.intOrNull("calories"))
                    put("extra", c.strOrNull("extra")?.let { SyncJson.parseToJsonElement(it) } ?: JsonNull)
                }
            )
        }
    }

    private fun libraryNames(exerciseId: String): JsonArray = buildJsonArray {
        db.query("SELECT lang, name, isAlias FROM exercise_names WHERE exerciseId = ? ORDER BY lang, isAlias, name", arrayOf<Any?>(exerciseId))
            .use { c ->
                while (c.moveToNext()) add(
                    buildJsonObject { put("lang", c.str("lang")); put("name", c.str("name")); put("isAlias", c.bool("isAlias")) }
                )
            }
    }

    private fun libraryMuscles(exerciseId: String): JsonArray = buildJsonArray {
        db.query("SELECT muscle, isPrimary FROM exercise_muscles WHERE exerciseId = ? ORDER BY muscle", arrayOf<Any?>(exerciseId))
            .use { c ->
                while (c.moveToNext()) add(buildJsonObject { put("muscle", c.str("muscle")); put("isPrimary", c.bool("isPrimary")) })
            }
    }

    private fun data(entity: String, uuid: String): JsonObject? = when (entity) {
        // A training type, keyed by its id so a fresh install's built-ins match (version 17 on).
        "activity_type" -> row("SELECT * FROM activity_types WHERE uuid = ?", uuid) { c ->
            buildJsonObject {
                put("key", c.str("id"))
                put("name", c.strOrNull("name"))
                put("icon", c.str("icon"))
                put("color", c.str("color"))
                put("recorder", c.str("recorder"))
                put("healthConnectType", c.intOrNull("healthConnectType"))
                put("metValue", c.doubleOrNull("metValue"))
                put("builtin", c.bool("builtin"))
                put("sortOrder", c.int("sortOrder"))
                put("hiddenAt", c.longOrNull("hiddenAt"))
            }
        }
        // A user's own library exercise with its names and muscles, known by its id (version 17 on).
        SyncMigration.LIBRARY_EXERCISE -> row("SELECT * FROM exercises WHERE id = ? AND source = 'CUSTOM'", uuid) { c ->
            buildJsonObject {
                put("preset", c.str("preset"))
                put("load", c.str("load"))
                put("loadUnit", c.strOrNull("loadUnit"))
                put("sides", c.str("sides"))
                put("equipment", c.str("equipment"))
                put("mechanics", c.strOrNull("mechanics"))
                put("weightStepKg", c.doubleOrNull("weightStepKg"))
                put("archivedAt", c.longOrNull("archivedAt"))
                put("names", libraryNames(uuid))
                put("muscles", libraryMuscles(uuid))
            }
        }
        // A programme, keyed by its id like the plan's workouts (version 16 on).
        "program" -> row(
            "SELECT p.*, t.uuid AS type_uuid FROM programs p LEFT JOIN activity_types t ON t.id = p.activityTypeId WHERE p.uuid = ?",
            uuid,
        ) { c ->
            buildJsonObject {
                put("key", c.str("id"))
                put("name", c.strOrNull("name"))
                put("startDate", LocalDate.ofEpochDay(c.long("startEpochDay")).toString())
                put("deloadEveryWeeks", c.intOrNull("deloadEveryWeeks"))
                put("variantRule", c.str("variantRule"))
                put("isActive", c.bool("isActive"))
                put("archivedAt", c.longOrNull("archivedAt"))
                put("activityTypeId", c.strOrNull("type_uuid"))
                put("deloadLoadPercent", c.int("deloadLoadPercent"))
                put("deloadSets", c.int("deloadSets"))
                put("variantEveryWeeks", c.int("variantEveryWeeks"))
            }
        }
        "workout" -> row(
            "SELECT w.*, p.uuid AS program_uuid FROM workouts w LEFT JOIN programs p ON p.id = w.programId WHERE w.uuid = ?",
            uuid,
        ) { c ->
            buildJsonObject {
                put("key", c.str("id"))
                put("label", c.str("label"))
                put("programId", c.strOrNull("program_uuid"))
                put("sortOrder", c.int("sortOrder"))
                put("name", c.strOrNull("name"))
                put("weekday", c.intOrNull("weekday"))
                put("hiddenAt", c.longOrNull("hiddenAt"))
            }
        }
        "exercise" -> row(
            """SELECT e.*, w.uuid AS workout_uuid, v.uuid AS variant_uuid FROM exercise_definitions e
               JOIN workouts w ON w.id = e.workoutId
               LEFT JOIN exercise_definitions v ON v.id = e.variantOfId WHERE e.uuid = ?""",
            uuid,
        ) { c ->
            buildJsonObject {
                put("key", c.str("id"))
                put("workoutId", c.str("workout_uuid"))
                put("name", c.str("name"))
                put("type", c.str("type"))
                put("targetSets", c.int("targetSets"))
                put("restDurationSecs", c.int("restDurationSecs"))
                put("targetReps", c.intOrNull("targetReps"))
                put("targetDistanceM", c.intOrNull("targetDistanceM"))
                put("isOptional", c.bool("isOptional"))
                put("isPerSide", c.bool("isPerSide"))
                put("isBodyweight", c.bool("isBodyweight"))
                put("variantOfId", c.strOrNull("variant_uuid"))
                put("sortOrder", c.int("sortOrder"))
                put("isRemoved", c.bool("isRemoved"))
                put("targetRepsMin", c.intOrNull("targetRepsMin"))
                put("libraryExerciseId", c.strOrNull("libraryExerciseId"))
            }
        }
        "session" -> row(
            """SELECT s.*, w.uuid AS workout_uuid, t.uuid AS type_uuid FROM sessions s
               LEFT JOIN workouts w ON w.id = s.workoutId
               LEFT JOIN activity_types t ON t.id = s.activityTypeId WHERE s.uuid = ?""",
            uuid,
        ) { c ->
            buildJsonObject {
                // A run or walk names no workout; its workoutId is just "RUNNING" or "WALKING".
                put("workoutId", c.strOrNull("workout_uuid"))
                put("startedAt", c.long("startedAt"))
                put("finishedAt", c.longOrNull("finishedAt"))
                put("type", serverSessionType(c.str("type")))
                put("distanceMeters", c.doubleOrNull("distanceMeters"))
                put("activityTypeId", c.strOrNull("type_uuid"))
                put("loggedManually", c.bool("loggedManually"))
                put("durationS", c.intOrNull("durationS"))
                put("calories", c.intOrNull("calories"))
                put("note", c.strOrNull("note"))
            }
        }
        "entry" -> row(
            """SELECT s.uuid AS session_uuid, x.uuid AS exercise_uuid, e.libraryExerciseId FROM exercise_entries e
               JOIN sessions s ON s.id = e.sessionId
               JOIN exercise_definitions x ON x.id = e.exerciseId WHERE e.uuid = ?""",
            uuid,
        ) { c ->
            buildJsonObject {
                put("sessionId", c.str("session_uuid"))
                put("exerciseId", c.str("exercise_uuid"))
                put("libraryExerciseId", c.strOrNull("libraryExerciseId"))
            }
        }
        // A set with its parts, in order (version 15 on).
        "set" -> row(
            "SELECT s.*, e.uuid AS entry_uuid FROM sets s JOIN exercise_entries e ON e.id = s.entryId WHERE s.uuid = ?",
            uuid,
        ) { c ->
            buildJsonObject {
                put("entryId", c.str("entry_uuid"))
                put("seq", c.int("seq"))
                put("kind", c.str("kind"))
                put("rpe", c.doubleOrNull("rpe"))
                put("note", c.strOrNull("note"))
                put("completedAt", c.long("completedAt"))
                put("parts", setParts(c.long("id")))
            }
        }
        // Not the step counter's reading at the start: it means nothing on another device.
        "run" -> row(
            "SELECT r.*, s.uuid AS session_uuid FROM run_details r JOIN sessions s ON s.id = r.sessionId WHERE r.uuid = ?",
            uuid,
        ) { c ->
            buildJsonObject {
                put("sessionId", c.str("session_uuid"))
                put("targetType", c.str("targetType"))
                put("targetValue", c.doubleOrNull("targetValue"))
                put("lapMeters", c.int("lapMeters"))
                put("steps", c.intOrNull("steps"))
                put("elevationGainM", c.doubleOrNull("elevationGainM"))
                put("calories", c.intOrNull("calories"))
                put("weatherTempC", c.intOrNull("weatherTempC"))
                put("weatherCondition", c.strOrNull("weatherCondition"))
            }
        }
        "pause" -> row(
            "SELECT p.*, s.uuid AS session_uuid FROM run_pauses p JOIN sessions s ON s.id = p.sessionId WHERE p.uuid = ?",
            uuid,
        ) { c ->
            buildJsonObject {
                put("sessionId", c.str("session_uuid"))
                put("startedAt", c.long("startedAt"))
                put("endedAt", c.longOrNull("endedAt"))
            }
        }
        "route" -> row("SELECT * FROM routes WHERE uuid = ?", uuid) { c ->
            buildJsonObject {
                put("name", c.str("name"))
                put("distanceMeters", c.double("distanceMeters"))
                put("createdAt", c.long("createdAt"))
            }
        }
        // The route's points, keyed by the route's own uuid.
        "route_points" -> {
            val points = buildJsonArray {
                db.query(
                    "SELECT p.latitude, p.longitude, p.elevationM FROM route_points p JOIN routes r ON r.id = p.routeId " +
                        "WHERE r.uuid = ? ORDER BY p.seq",
                    arrayOf<Any?>(uuid),
                ).use { c ->
                    while (c.moveToNext()) add(
                        JsonArray(listOf(JsonPrimitive(c.getDouble(0)), JsonPrimitive(c.getDouble(1)), c.jsonDouble(2)))
                    )
                }
            }
            if (points.isEmpty()) null else buildJsonObject { put("routeId", uuid); put("points", points) }
        }
        // The music log: what played during a session (the now-playing bar).
        "song" -> row(
            "SELECT t.*, s.uuid AS session_uuid FROM track_log t JOIN sessions s ON s.id = t.sessionId WHERE t.uuid = ?",
            uuid,
        ) { c ->
            buildJsonObject {
                put("sessionId", c.str("session_uuid"))
                put("startedAt", c.long("startedAt"))
                put("title", c.str("title").take(500))
                put("artist", c.str("artist").take(500))
            }
        }
        else -> null
    }

    /**
     * A session's GPS points as chunks of at most [TRACK_CHUNK_POINTS], each its own item with a
     * stable id, so a request stays under the backend's 100 KB. Chunks no longer needed (fewer
     * points, or the session deleted) are sent as deletions, up to the count sent before.
     */
    private fun trackChanges(sessionUuid: String, changedAt: Long, deleted: Boolean): List<OutgoingChange> {
        val points = if (deleted) emptyList() else trackPoints(sessionUuid)
        val chunks = points.chunked(TRACK_CHUNK_POINTS)
        val sentBefore = sentChunks(sessionUuid)
        val changes = chunks.mapIndexed { i, chunk ->
            OutgoingChange(TRACK, trackChunkId(sessionUuid, i), changedAt, false, buildJsonObject {
                put("sessionId", sessionUuid)
                put("chunk", i)
                put("points", JsonArray(chunk))
            })
        }
        val gone = (chunks.size until sentBefore).map { i ->
            OutgoingChange(TRACK, trackChunkId(sessionUuid, i), changedAt, true, null)
        }
        return changes + gone
    }

    private fun trackPoints(sessionUuid: String): List<JsonElement> = buildList {
        db.query(
            "SELECT l.timestamp, l.latitude, l.longitude, l.speed, l.altitude, l.accuracy, l.bearing " +
                "FROM location_log l JOIN sessions s ON s.id = l.sessionId WHERE s.uuid = ? ORDER BY l.timestamp, l.id",
            arrayOf<Any?>(sessionUuid),
        ).use { c ->
            while (c.moveToNext()) add(
                JsonArray(
                    listOf(
                        JsonPrimitive(c.getLong(0)), JsonPrimitive(c.getDouble(1)), JsonPrimitive(c.getDouble(2)),
                        c.jsonDouble(3), c.jsonDouble(4), c.jsonDouble(5), c.jsonDouble(6),
                    )
                )
            )
        }
    }

    /** How many chunks of this session's track the server holds, so extra ones can be deleted. */
    fun sentChunks(sessionUuid: String): Int =
        db.query("SELECT chunks FROM sync_track_chunks WHERE session_uuid = ?", arrayOf<Any?>(sessionUuid))
            .use { if (it.moveToFirst()) it.getInt(0) else 0 }

    fun recordSentChunks(sessionUuid: String, chunks: Int) {
        if (chunks == 0) db.execSQL("DELETE FROM sync_track_chunks WHERE session_uuid = ?", arrayOf<Any?>(sessionUuid))
        else db.execSQL("INSERT OR REPLACE INTO sync_track_chunks VALUES (?, ?)", arrayOf<Any?>(sessionUuid, chunks))
    }

    private fun <T> row(sql: String, uuid: String, read: (Cursor) -> T): T? =
        db.query(sql, arrayOf<Any?>(uuid)).use { if (it.moveToFirst()) read(it) else null }

    companion object {
        const val TRACK = "track"
        const val TRACK_CHUNK_POINTS = 600
        val ROUTE_KINDS = setOf("route", "route_points", TRACK)
        val MUSIC_KINDS = setOf("song")

        /** The same chunk of the same session always gets the same id. */
        fun trackChunkId(sessionUuid: String, chunk: Int): String =
            UUID.nameUUIDFromBytes("gym-tracker:track:$sessionUuid:$chunk".toByteArray()).toString()

        fun serverSessionType(local: String): String = when (local) {
            "RUNNING" -> "RUN"
            "WALKING" -> "WALK"
            "OTHER" -> "OTHER"
            else -> "GYM"
        }
    }
}

private fun Cursor.idx(name: String) = getColumnIndexOrThrow(name)
private fun Cursor.str(name: String): String = getString(idx(name))
private fun Cursor.strOrNull(name: String): String? = idx(name).let { if (isNull(it)) null else getString(it) }
private fun Cursor.int(name: String): Int = getInt(idx(name))
private fun Cursor.intOrNull(name: String): Int? = idx(name).let { if (isNull(it)) null else getInt(it) }
private fun Cursor.long(name: String): Long = getLong(idx(name))
private fun Cursor.longOrNull(name: String): Long? = idx(name).let { if (isNull(it)) null else getLong(it) }
private fun Cursor.double(name: String): Double = getDouble(idx(name))
private fun Cursor.doubleOrNull(name: String): Double? = idx(name).let { if (isNull(it)) null else getDouble(it) }
private fun Cursor.jsonDouble(index: Int): JsonElement = if (isNull(index)) JsonNull else JsonPrimitive(getDouble(index))
private fun Cursor.bool(name: String): Boolean = getInt(idx(name)) != 0
