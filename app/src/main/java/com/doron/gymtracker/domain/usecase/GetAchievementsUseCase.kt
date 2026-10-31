package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.data.repository.WorkoutRepository
import com.doron.gymtracker.domain.model.AchievementFamily
import com.doron.gymtracker.domain.model.AchievementKind
import com.doron.gymtracker.domain.model.AchievementStatus
import com.doron.gymtracker.domain.model.RepSetRecord
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/** What one finished session counts towards the achievements. */
data class SessionFacts(
    val sessionId: Long,
    val type: String,
    val workoutId: String,
    val startedAt: Long,
    val distanceMeters: Double? = null,
    val elevationGainM: Double? = null,
)

/** One gym set's weight × reps, and when it was done. */
data class SetFacts(val sessionId: Long, val completedAt: Long, val volumeKg: Double)

/**
 * The achievements beyond the distance badges, worked out from history every time, so sessions
 * from before they existed count, and an edited or deleted session moves them back.
 */
class GetAchievementsUseCase @Inject constructor(
    private val sessionRepo: SessionRepository,
    private val workoutRepo: WorkoutRepository,
) {
    suspend operator fun invoke(): List<AchievementStatus> {
        val (sessions, sets) = facts()
        return compute(sessions, sets, workoutRepo.getAll().map { it.id }.toSet())
    }

    /** The achievements [sessionId] earned for the first time. */
    suspend fun earnedBy(sessionId: Long): List<AchievementStatus> = invoke().filter { it.sessionId == sessionId }

    private suspend fun facts(): Pair<List<SessionFacts>, List<SetFacts>> {
        val finished = sessionRepo.getCompletedSince(0).filter { it.finishedAt != null }
        val sessions = finished.map { s ->
            val cardio = s.type in CARDIO
            SessionFacts(
                sessionId = s.id,
                type = s.type,
                workoutId = s.workoutId,
                startedAt = s.startedAt,
                distanceMeters = s.distanceMeters,
                elevationGainM = if (cardio) sessionRepo.getRunDetails(s.id)?.elevationGainM else null,
            )
        }
        val sets = finished.filter { it.type == "GYM" }.flatMap { s ->
            sessionRepo.getEntriesForSession(s.id).flatMap { it.sets }.mapNotNull { set ->
                (set as? RepSetRecord)?.let { r ->
                    val kg = r.weightKg ?: return@mapNotNull null
                    SetFacts(s.id, r.completedAt, kg.toDouble() * r.reps)
                }
            }
        }
        return sessions to sets
    }

    companion object {
        private val CARDIO = setOf("RUNNING", "WALKING")
        const val WORKOUTS_A_WEEK = 3
        const val NIGHT_FROM_HOUR = 21
        const val EARLY_BEFORE_HOUR = 6

        fun compute(
            sessions: List<SessionFacts>,
            sets: List<SetFacts>,
            planWorkoutIds: Set<String>,
            zone: ZoneId = ZoneId.systemDefault(),
        ): List<AchievementStatus> {
            val bySession = sessions.sortedWith(compareBy({ it.startedAt }, { it.sessionId }))
            val cardio = bySession.filter { it.type in CARDIO }
            return AchievementKind.entries.map { kind ->
                when (kind.family) {
                    AchievementFamily.CLIMBER -> climber(kind, cardio)
                    AchievementFamily.MILEAGE -> mileage(kind, cardio)
                    AchievementFamily.IRON -> iron(kind, sets)
                    AchievementFamily.REGULAR -> regular(kind, bySession, zone)
                    AchievementFamily.SPECIAL -> when (kind) {
                        AchievementKind.ROTATION -> rotation(kind, bySession, planWorkoutIds, zone)
                        AchievementKind.NIGHT_OWL -> firstAt(kind, cardio, zone) { it >= NIGHT_FROM_HOUR }
                        else -> firstAt(kind, cardio, zone) { it < EARLY_BEFORE_HOUR }
                    }
                }
            }
        }

        /** The first run or walk that climbed the target in one go; progress is the best climb. */
        private fun climber(kind: AchievementKind, cardio: List<SessionFacts>): AchievementStatus {
            val first = cardio.firstOrNull { (it.elevationGainM ?: 0.0) >= kind.target }
            val best = cardio.maxOfOrNull { it.elevationGainM ?: 0.0 } ?: 0.0
            return AchievementStatus(kind, first?.startedAt, first?.sessionId, best)
        }

        /** Kilometers of every run and walk added up; earned by the one that crossed the target. */
        private fun mileage(kind: AchievementKind, cardio: List<SessionFacts>): AchievementStatus {
            var km = 0.0
            var earned: SessionFacts? = null
            for (s in cardio) {
                km += (s.distanceMeters ?: 0.0) / 1000
                if (earned == null && km >= kind.target) earned = s
            }
            return AchievementStatus(kind, earned?.startedAt, earned?.sessionId, km)
        }

        /** Tonnes lifted in all; earned by the set that crossed the target. */
        private fun iron(kind: AchievementKind, sets: List<SetFacts>): AchievementStatus {
            var tonnes = 0.0
            var earned: SetFacts? = null
            for (set in sets.sortedBy { it.completedAt }) {
                tonnes += set.volumeKg / 1000
                if (earned == null && tonnes >= kind.target) earned = set
            }
            return AchievementStatus(kind, earned?.completedAt, earned?.sessionId, tonnes)
        }

        /**
         * Weeks in a row (Sunday to Saturday) with at least [WORKOUTS_A_WEEK] sessions of any kind.
         * Earned by the session that made the streak long enough; progress is the longest streak.
         */
        private fun regular(kind: AchievementKind, sessions: List<SessionFacts>, zone: ZoneId): AchievementStatus {
            val weeks = sessions.groupBy { week(it.startedAt, zone) }.toSortedMap()
            var streak = 0
            var longest = 0
            var lastWeek: Long? = null
            var earned: SessionFacts? = null
            for ((weekNo, inWeek) in weeks) {
                if (inWeek.size < WORKOUTS_A_WEEK) { streak = 0; lastWeek = weekNo; continue }
                streak = if (lastWeek != null && weekNo == lastWeek + 1 && streak > 0) streak + 1 else 1
                lastWeek = weekNo
                longest = maxOf(longest, streak)
                if (earned == null && streak >= kind.target) earned = inWeek[WORKOUTS_A_WEEK - 1]
            }
            return AchievementStatus(kind, earned?.startedAt, earned?.sessionId, longest.toDouble())
        }

        /** Every workout in the plan (A, B and C) done within one week. */
        private fun rotation(
            kind: AchievementKind,
            sessions: List<SessionFacts>,
            plan: Set<String>,
            zone: ZoneId,
        ): AchievementStatus {
            if (plan.isEmpty()) return AchievementStatus(kind)
            for ((_, inWeek) in sessions.filter { it.type == "GYM" }.groupBy { week(it.startedAt, zone) }.toSortedMap()) {
                val seen = mutableSetOf<String>()
                for (s in inWeek) {
                    seen += s.workoutId
                    if (seen.containsAll(plan)) return AchievementStatus(kind, s.startedAt, s.sessionId, 1.0)
                }
            }
            return AchievementStatus(kind)
        }

        /** The first run or walk whose local start hour matches. */
        private fun firstAt(
            kind: AchievementKind,
            cardio: List<SessionFacts>,
            zone: ZoneId,
            hour: (Int) -> Boolean,
        ): AchievementStatus {
            val first = cardio.firstOrNull { hour(Instant.ofEpochMilli(it.startedAt).atZone(zone).hour) }
                ?: return AchievementStatus(kind)
            return AchievementStatus(kind, first.startedAt, first.sessionId, 1.0)
        }

        /** Weeks since the epoch, counted from Sunday, in the phone's zone. */
        private fun week(at: Long, zone: ZoneId): Long =
            GetPersonalRecordsUseCase.weekStart(at, zone).let { start ->
                java.time.Instant.ofEpochMilli(start).atZone(zone).toLocalDate().toEpochDay() / 7
            }
    }
}
