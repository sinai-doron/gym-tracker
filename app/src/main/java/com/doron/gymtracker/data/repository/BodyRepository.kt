package com.doron.gymtracker.data.repository

import com.doron.gymtracker.data.db.dao.WaterDao
import com.doron.gymtracker.data.db.dao.WeightDao
import com.doron.gymtracker.data.db.entities.WaterEntryEntity
import com.doron.gymtracker.data.db.entities.WaterGoalEntity
import com.doron.gymtracker.data.db.entities.WeightEntryEntity
import com.doron.gymtracker.data.db.entities.WeightGoalEntity
import com.doron.gymtracker.data.prefs.SettingsPrefs
import com.doron.gymtracker.domain.model.Drink
import com.doron.gymtracker.domain.model.WaterGoal
import com.doron.gymtracker.domain.model.WeighIn
import com.doron.gymtracker.domain.model.WeightGoal
import com.doron.gymtracker.domain.model.WeightStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Weigh-ins and the weight goal. The newest weigh-in is also the body weight calories are worked out from. */
@Singleton
class WeightRepository @Inject constructor(
    private val dao: WeightDao,
    private val settings: SettingsPrefs
) {
    /** Newest first. */
    val entries: Flow<List<WeighIn>> = dao.observeAll().map { rows -> rows.map { WeighIn(it.id, it.measuredAt, it.kg) } }

    val goal: Flow<WeightGoal?> = dao.observeGoal().map { g -> g?.let { WeightGoal(it.startKg, it.startAt, it.targetKg, it.paceKgPerWeek) } }

    suspend fun get(id: Long): WeighIn? = dao.get(id)?.let { WeighIn(it.id, it.measuredAt, it.kg) }

    suspend fun log(measuredAt: Long, kg: Double): Long =
        dao.insert(WeightEntryEntity(measuredAt = measuredAt, kg = clamp(kg))).also { syncBodyWeight() }

    suspend fun update(id: Long, measuredAt: Long, kg: Double) {
        val row = dao.get(id) ?: return
        dao.update(row.copy(measuredAt = measuredAt, kg = clamp(kg)))
        syncBodyWeight()
    }

    suspend fun delete(id: Long) {
        dao.delete(id)
        syncBodyWeight()
    }

    suspend fun setGoal(goal: WeightGoal) = dao.upsertGoal(
        WeightGoalEntity(startKg = clamp(goal.startKg), startAt = goal.startAt, targetKg = clamp(goal.targetKg), paceKgPerWeek = goal.paceKgPerWeek)
    )

    suspend fun weighedInBetween(from: Long, until: Long): Boolean = dao.countBetween(from, until) > 0

    suspend fun latestKg(): Double? = dao.latest()?.kg

    // Calories follow the latest weigh-in
    private suspend fun syncBodyWeight() {
        dao.latest()?.let { settings.setBodyWeightKg(it.kg.toFloat()) }
    }

    private fun clamp(kg: Double) = kg.coerceIn(WeightStats.MIN_KG, WeightStats.MAX_KG)
}

/** Drinks and the water goal. */
@Singleton
class WaterRepository @Inject constructor(private val dao: WaterDao) {

    /** Drinks since [from], newest first. */
    fun drinksSince(from: Long): Flow<List<Drink>> = dao.observeSince(from).map { rows -> rows.map { Drink(it.id, it.drankAt, it.ml) } }

    val goal: Flow<WaterGoal> = dao.observeGoal().map { g -> g?.let { WaterGoal(it.dailyMl, it.glassMl, it.bottleMl) } ?: WaterGoal() }

    suspend fun goalNow(): WaterGoal = dao.goal()?.let { WaterGoal(it.dailyMl, it.glassMl, it.bottleMl) } ?: WaterGoal()

    suspend fun add(drankAt: Long, ml: Int) = dao.insert(WaterEntryEntity(drankAt = drankAt, ml = ml.coerceIn(1, MAX_DRINK)))

    suspend fun delete(id: Long) = dao.delete(id)

    suspend fun totalBetween(from: Long, until: Long): Int = dao.totalBetween(from, until)

    suspend fun setGoal(goal: WaterGoal) = dao.upsertGoal(
        WaterGoalEntity(
            dailyMl = goal.dailyMl.coerceIn(WaterGoal.MIN_DAILY, WaterGoal.MAX_DAILY),
            glassMl = goal.glassMl.coerceIn(1, 2_000),
            bottleMl = goal.bottleMl.coerceIn(1, 3_000)
        )
    )

    companion object {
        const val MAX_DRINK = 5_000
    }
}
