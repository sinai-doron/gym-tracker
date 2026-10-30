package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.data.ActiveClock
import com.doron.gymtracker.data.BestEffort
import com.doron.gymtracker.data.RouteDistance
import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.domain.model.PersonalRecord
import com.doron.gymtracker.domain.model.RecordKind
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

/** What one finished run counts towards the records. */
data class RunFacts(
    val sessionId: Long,
    val startedAt: Long,
    val distanceMeters: Double,
    /** Seconds for the fastest stretch of each distance the run covered. */
    val fastest: Map<RecordKind, Double>
)

/**
 * Personal records from the finished runs (walks don't count), worked out from their saved
 * routes, so runs from before records existed count too.
 */
class GetPersonalRecordsUseCase @Inject constructor(
    private val sessionRepo: SessionRepository
) {
    suspend operator fun invoke(): List<PersonalRecord> = records(facts())

    /** The records [sessionId] just broke: ones that existed before it, which it beat. */
    suspend fun newBy(sessionId: Long): List<PersonalRecord> {
        val facts = facts()
        val before = records(facts.filter { it.sessionId != sessionId }).associateBy { it.kind }
        val run = facts.firstOrNull { it.sessionId == sessionId } ?: return emptyList()
        return records(facts).filter { record ->
            val previous = before[record.kind] ?: return@filter false
            when (record.kind) {
                RecordKind.BEST_WEEK -> record.at == weekStart(run.startedAt) && record.value > previous.value
                else -> record.sessionId == sessionId && record.value != previous.value
            }
        }
    }

    private suspend fun facts(): List<RunFacts> =
        sessionRepo.getCompletedSince(0)
            .filter { it.type == "RUNNING" && it.finishedAt != null }
            .map { session ->
                val route = sessionRepo.getRouteForSession(session.id)
                val pauses = sessionRepo.getPauses(session.id)
                val marks = RouteDistance.marks(route, pauses)
                val clock = ActiveClock(session.startedAt, marks, pauses)
                val active = marks.map { clock.activeMs(it.timestamp) }
                RunFacts(
                    sessionId = session.id,
                    startedAt = session.startedAt,
                    distanceMeters = session.distanceMeters ?: (marks.lastOrNull()?.meters ?: 0.0),
                    fastest = RecordKind.entries.mapNotNull { kind ->
                        kind.meters?.let { m ->
                            BestEffort.fastestSecs(marks, active, m)
                                // Faster than this is a GPS jump, not running
                                ?.takeIf { it >= m / 1000 * FASTEST_SECS_PER_KM }
                                ?.let { kind to it }
                        }
                    }.toMap()
                )
            }

    companion object {
        private const val FASTEST_SECS_PER_KM = 150.0

        /** The best of each kind among [runs]; kinds no run has reached are left out. */
        fun records(runs: List<RunFacts>, zone: ZoneId = ZoneId.systemDefault()): List<PersonalRecord> {
            val fastest = RecordKind.entries.filter { it.isTime }.mapNotNull { kind ->
                runs.mapNotNull { r -> r.fastest[kind]?.let { r to it } }.minByOrNull { it.second }
                    ?.let { (r, secs) -> PersonalRecord(kind, secs, r.sessionId, r.startedAt) }
            }
            val longest = runs.maxByOrNull { it.distanceMeters }?.takeIf { it.distanceMeters > 0 }
                ?.let { PersonalRecord(RecordKind.LONGEST_RUN, it.distanceMeters, it.sessionId, it.startedAt) }
            val week = runs.groupBy { weekStart(it.startedAt, zone) }
                .mapValues { (_, rs) -> rs.sumOf { it.distanceMeters } }
                .maxByOrNull { it.value }?.takeIf { it.value > 0 }
                ?.let { (start, meters) -> PersonalRecord(RecordKind.BEST_WEEK, meters, null, start) }
            return fastest + listOfNotNull(longest, week)
        }

        /** The Sunday the week of [at] began, as epoch ms. */
        fun weekStart(at: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
            Instant.ofEpochMilli(at).atZone(zone).toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                .atStartOfDay(zone).toInstant().toEpochMilli()
    }
}
