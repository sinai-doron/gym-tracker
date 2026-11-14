package com.doron.gymtracker.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import javax.inject.Inject
import javax.inject.Singleton

private val Context.bodyDataStore: DataStore<Preferences> by preferencesDataStore(name = "body_tracking")

private val SHOW_WEIGHT = booleanPreferencesKey("show_weight_card")
private val SHOW_WATER = booleanPreferencesKey("show_water_card")
private val WEIGHT_REMINDER = stringPreferencesKey("weight_reminder")         // OFF, DAILY, WEEKLY
private val WEIGHT_REMINDER_DAY = stringPreferencesKey("weight_reminder_day")  // a DayOfWeek
private val WEIGHT_REMINDER_AT = intPreferencesKey("weight_reminder_minute")    // minutes after midnight
private val WATER_REMINDER = booleanPreferencesKey("water_reminder")
private val WATER_EVERY_HOURS = intPreferencesKey("water_every_hours")
private val WATER_FROM = intPreferencesKey("water_from_minute")
private val WATER_UNTIL = intPreferencesKey("water_until_minute")
private val WATER_SKIP_ON_TRACK = booleanPreferencesKey("water_skip_on_track")

/** How often to remind about a weigh-in. */
enum class WeighInReminder { OFF, DAILY, WEEKLY }

data class WeightReminder(
    val frequency: WeighInReminder = WeighInReminder.OFF,
    val day: DayOfWeek = DayOfWeek.SUNDAY,
    /** Minutes after midnight. */
    val atMinute: Int = 7 * 60 + 30
)

data class WaterReminder(
    val on: Boolean = false,
    val everyHours: Int = 2,
    val fromMinute: Int = 8 * 60,
    val untilMinute: Int = 22 * 60,
    /** No nudge while the day's drinking is ahead of its pace. */
    val skipWhenOnTrack: Boolean = true
)

/**
 * Weight and water on this phone: whether their cards show on Home, and their reminders. Kept here
 * and not synced: a reminder belongs to the phone that rings.
 */
@Singleton
class BodyPrefs @Inject constructor(@ApplicationContext private val context: Context) {

    val showWeightCard: Flow<Boolean> = context.bodyDataStore.data.map { it[SHOW_WEIGHT] ?: true }
    val showWaterCard: Flow<Boolean> = context.bodyDataStore.data.map { it[SHOW_WATER] ?: true }

    suspend fun setShowWeightCard(show: Boolean) = context.bodyDataStore.edit { it[SHOW_WEIGHT] = show }
    suspend fun setShowWaterCard(show: Boolean) = context.bodyDataStore.edit { it[SHOW_WATER] = show }

    val weightReminder: Flow<WeightReminder> = context.bodyDataStore.data.map { p ->
        val d = WeightReminder()
        WeightReminder(
            frequency = p[WEIGHT_REMINDER]?.let { runCatching { WeighInReminder.valueOf(it) }.getOrNull() } ?: d.frequency,
            day = p[WEIGHT_REMINDER_DAY]?.let { runCatching { DayOfWeek.valueOf(it) }.getOrNull() } ?: d.day,
            atMinute = p[WEIGHT_REMINDER_AT] ?: d.atMinute
        )
    }

    suspend fun setWeightReminder(r: WeightReminder) = context.bodyDataStore.edit {
        it[WEIGHT_REMINDER] = r.frequency.name
        it[WEIGHT_REMINDER_DAY] = r.day.name
        it[WEIGHT_REMINDER_AT] = r.atMinute
    }

    val waterReminder: Flow<WaterReminder> = context.bodyDataStore.data.map { p ->
        val d = WaterReminder()
        WaterReminder(
            on = p[WATER_REMINDER] ?: d.on,
            everyHours = p[WATER_EVERY_HOURS] ?: d.everyHours,
            fromMinute = p[WATER_FROM] ?: d.fromMinute,
            untilMinute = p[WATER_UNTIL] ?: d.untilMinute,
            skipWhenOnTrack = p[WATER_SKIP_ON_TRACK] ?: d.skipWhenOnTrack
        )
    }

    suspend fun setWaterReminder(r: WaterReminder) = context.bodyDataStore.edit {
        it[WATER_REMINDER] = r.on
        it[WATER_EVERY_HOURS] = r.everyHours
        it[WATER_FROM] = r.fromMinute
        it[WATER_UNTIL] = r.untilMinute
        it[WATER_SKIP_ON_TRACK] = r.skipWhenOnTrack
    }
}
