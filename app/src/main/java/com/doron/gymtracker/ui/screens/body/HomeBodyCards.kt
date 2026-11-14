package com.doron.gymtracker.ui.screens.body

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doron.gymtracker.R
import com.doron.gymtracker.data.prefs.BodyPrefs
import com.doron.gymtracker.data.repository.WaterRepository
import com.doron.gymtracker.data.repository.WeightRepository
import com.doron.gymtracker.domain.model.Reminders
import com.doron.gymtracker.domain.model.WaterGoal
import com.doron.gymtracker.domain.model.WeighIn
import com.doron.gymtracker.domain.model.WeightStats
import com.doron.gymtracker.ui.components.LocalWeightUnit
import com.doron.gymtracker.ui.components.appLocale
import com.doron.gymtracker.ui.components.weightUnitLabel
import com.doron.gymtracker.ui.theme.AppColors
import com.doron.gymtracker.ui.theme.AppShapes
import com.doron.gymtracker.ui.theme.AppType
import com.doron.gymtracker.ui.theme.designShadow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** What Home's weight and water cards show; each only when switched on in Settings. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeBodyViewModel @Inject constructor(
    prefs: BodyPrefs,
    weight: WeightRepository,
    private val water: WaterRepository
) : ViewModel() {
    val showWeight: StateFlow<Boolean> = prefs.showWeightCard.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val showWater: StateFlow<Boolean> = prefs.showWaterCard.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val weighIns: StateFlow<List<WeighIn>?> = weight.entries.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val waterGoal: StateFlow<WaterGoal> = water.goal.stateIn(viewModelScope, SharingStarted.Eagerly, WaterGoal())

    /** Today's millilitres; the day turns over under an open Home. */
    val waterToday: StateFlow<Int> = flow {
        while (true) {
            emit(LocalDate.now())
            delay(60_000)
        }
    }.distinctUntilChanged()
        .flatMapLatest { day -> water.drinksSince(Reminders.startOfDay(day, ZoneId.systemDefault())) }
        .map { drinks -> drinks.sumOf { it.ml } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    fun addGlass() {
        viewModelScope.launch { water.add(System.currentTimeMillis(), waterGoal.value.glassMl) }
    }
}

/** Weight and water side by side (or the one switched on, full width); nothing with both off. */
@Composable
fun HomeBodyCards(onWeight: () -> Unit, onWater: () -> Unit, modifier: Modifier = Modifier, viewModel: HomeBodyViewModel = hiltViewModel()) {
    val showWeight by viewModel.showWeight.collectAsState()
    val showWater by viewModel.showWater.collectAsState()
    val weighIns by viewModel.weighIns.collectAsState()
    val goal by viewModel.waterGoal.collectAsState()
    val today by viewModel.waterToday.collectAsState()
    if (!showWeight && !showWater) return
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Max)) {
        if (showWeight) WeightTile(weighIns, onWeight)
        if (showWater) WaterTile(today, goal, onWater, viewModel::addGlass)
    }
}

@Composable
private fun RowScope.Tile(onClick: () -> Unit, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(AppShapes.cardRadius)
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .designShadow(AppShapes.cardShadow, AppShapes.cardRadius)
            .clip(shape)
            .background(AppColors.surface)
            .border(AppShapes.cardBorder, AppColors.border, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp)
    ) { content() }
}

@Composable
private fun TileHeader(@DrawableRes icon: Int, tile: Color, tint: Color, title: String, action: (@Composable () -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(AppShapes.smallIconTile)
                .background(tile, RoundedCornerShape(AppShapes.smallIconTileRadius))
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(AppShapes.smallIconTileIcon))
        }
        Text(title, style = AppType.label, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
private fun RowScope.WeightTile(weighIns: List<WeighIn>?, onClick: () -> Unit) {
    val unit = LocalWeightUnit.current
    val locale = appLocale()
    Tile(onClick) {
        TileHeader(R.drawable.ic_scale, AppColors.tintStrength, AppColors.accentStrength, stringResource(R.string.weight_title).uppercase(locale))
        val latest = weighIns?.firstOrNull()
        if (latest == null) {
            Text(stringResource(R.string.weight_log), style = AppType.button.copy(color = AppColors.accentStrength))
            return@Tile
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(unit.tenth(latest.kg, locale), style = AppType.cardTitle, modifier = Modifier.alignByBaseline())
            Text(weightUnitLabel(), style = AppType.caption.copy(fontWeight = FontWeight.Medium), modifier = Modifier.alignByBaseline())
        }
        val change = weighIns.let { WeightStats.weekChange(it, System.currentTimeMillis()) }
        Text(
            change?.let {
                stringResource(R.string.weight_card_change_week, stringResource(R.string.units_value, unit.signed(it, locale), weightUnitLabel()))
            } ?: stringResource(R.string.weight_current, dayLabel(latest.measuredAt)),
            style = AppType.hint.copy(color = if (change != null && change < -0.04) AppColors.goodText else AppColors.textMuted),
            maxLines = 1
        )
    }
}

@Composable
private fun dayLabel(at: Long): String {
    val day = java.time.Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).toLocalDate()
    return when (day) {
        LocalDate.now() -> stringResource(R.string.weight_today)
        LocalDate.now().minusDays(1) -> stringResource(R.string.weight_yesterday)
        else -> day.format(java.time.format.DateTimeFormatter.ofPattern("d MMM", appLocale()))
    }
}

@Composable
private fun RowScope.WaterTile(totalMl: Int, goal: WaterGoal, onClick: () -> Unit, onAddGlass: () -> Unit) {
    val locale = appLocale()
    val addLabel = stringResource(R.string.water_add_ml, mlText(goal.glassMl, locale))
    Tile(onClick) {
        TileHeader(R.drawable.ic_water_drop, AppColors.waterTint, AppColors.water, stringResource(R.string.water_title).uppercase(locale)) {
            // A glass in one tap, without opening the page
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(AppColors.water)
                    .clickable(role = Role.Button, onClick = onAddGlass)
                    .semantics { contentDescription = addLabel }
            ) {
                Icon(painterResource(R.drawable.ic_home_plus), contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(mlText(totalMl, locale), style = AppType.cardTitle, modifier = Modifier.alignByBaseline())
            Text(stringResource(R.string.units_ml), style = AppType.caption.copy(fontWeight = FontWeight.Medium), modifier = Modifier.alignByBaseline())
        }
        val fraction = (totalMl.toFloat() / goal.dailyMl).coerceIn(0f, 1f)
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(AppColors.waterTint)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(AppColors.water)
            )
        }
        Text(
            if (totalMl >= goal.dailyMl) stringResource(R.string.water_goal_reached)
            else stringResource(R.string.water_of_goal, mlText(goal.dailyMl, locale), (fraction * 100).toInt()),
            style = AppType.hint.copy(fontSize = 12.sp, color = if (totalMl >= goal.dailyMl) AppColors.goodText else AppColors.textMuted),
            maxLines = 1
        )
    }
}
