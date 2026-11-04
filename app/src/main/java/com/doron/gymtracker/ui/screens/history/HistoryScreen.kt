package com.doron.gymtracker.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doron.gymtracker.R
import com.doron.gymtracker.domain.model.TrainingType
import com.doron.gymtracker.ui.components.colorValue
import com.doron.gymtracker.ui.components.dayColors
import com.doron.gymtracker.ui.components.displayName
import com.doron.gymtracker.ui.components.drawDay
import com.doron.gymtracker.ui.theme.Purple
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val heLocale = Locale("he")

private val weekDayOrder = listOf(
    DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit = {},
    onSessionClick: (Long) -> Unit = {},
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val month by viewModel.currentMonth.collectAsState()
    val sessionDates by viewModel.sessionDates.collectAsState()
    val selectedDay by viewModel.selectedDaySessions.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.history_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            MonthHeader(month, onPrev = viewModel::prevMonth, onNext = viewModel::nextMonth)
            Spacer(Modifier.height(12.dp))
            DayHeaders()
            Spacer(Modifier.height(4.dp))
            CalendarGrid(month = month, sessionDates = sessionDates, onDayClick = { viewModel.loadDaySessions(it) })
            Spacer(Modifier.height(16.dp))
            Legend(sessionDates.filterKeys { YearMonth.from(it) == month }.values.flatten().distinctBy { it.id })
        }
    }

    if (selectedDay != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.clearSelectedDay() },
            sheetState = sheetState
        ) {
            DaySessionsSheet(
                state = selectedDay!!,
                onSessionClick = { sessionId ->
                    viewModel.clearSelectedDay()
                    onSessionClick(sessionId)
                },
                onDeleteSession = { sessionId -> viewModel.deleteSession(sessionId) }
            )
        }
    }
}

@Composable
private fun MonthHeader(month: YearMonth, onPrev: () -> Unit, onNext: () -> Unit) {
    // "← previous  month  next →" in every language, as months go left to right on a timeline
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPrev) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.history_prev_month))
            }
            val monthName = month.month.getDisplayName(TextStyle.FULL_STANDALONE, heLocale)
            Text(
                "$monthName ${month.year}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onNext) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.history_next_month))
            }
        }
    }
}

@Composable
private fun DayHeaders() {
    Row(modifier = Modifier.fillMaxWidth()) {
        stringArrayResource(R.array.history_day_headers).forEach { label ->
            Text(
                label,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    month: YearMonth,
    sessionDates: Map<LocalDate, List<TrainingType>>,
    onDayClick: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val firstDay = month.atDay(1)
    val firstDayOfWeekIndex = weekDayOrder.indexOf(firstDay.dayOfWeek)
    val totalDays = month.lengthOfMonth()
    val cells = firstDayOfWeekIndex + totalDays
    val rows = (cells + 6) / 7

    Column {
        repeat(rows) { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { col ->
                    val cellIndex = row * 7 + col
                    val dayNum = cellIndex - firstDayOfWeekIndex + 1
                    if (dayNum < 1 || dayNum > totalDays) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val date = month.atDay(dayNum)
                        val types = sessionDates[date].orEmpty()
                        val hasSession = types.isNotEmpty()
                        val isToday = date == today
                        DayCell(
                            day = dayNum,
                            colors = dayColors(types),
                            isToday = isToday,
                            modifier = Modifier.weight(1f),
                            onClick = if (hasSession) ({ onDayClick(date) }) else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    colors: List<Color>,
    isToday: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)?
) {
    val hasSession = colors.isNotEmpty()
    val surface = MaterialTheme.colorScheme.surface
    val textColor = when {
        hasSession -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }
    val borderMod = if (isToday && !hasSession)
        Modifier.border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
    else Modifier

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .drawBehind { drawDay(colors, surface) }
            .then(borderMod)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "$day",
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}

/** Which colour is which, under the calendar: the types done this month, one entry per colour. */
@Composable
private fun Legend(types: List<TrainingType>) {
    val entries = types.sortedBy { it.sortOrder }.groupBy { it.colorValue() }
    if (entries.isEmpty()) return
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(entries.entries.toList()) { (color, same) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).background(color, CircleShape))
                Spacer(Modifier.width(6.dp))
                Text(
                    same.map { it.displayName() }.joinToString(" / "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


@Composable
private fun DaySessionsSheet(
    state: DaySessionsUiState,
    onSessionClick: (Long) -> Unit,
    onDeleteSession: (Long) -> Unit
) {
    var deleteTarget by remember { mutableStateOf<SessionSummaryItem?>(null) }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            state.dateLabel,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            pluralStringResource(R.plurals.history_sessions_count, state.sessions.size, state.sessions.size),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(state.sessions, key = { it.sessionId }) { session ->
                SessionCard(
                    session = session,
                    onClick = { onSessionClick(session.sessionId) },
                    onDelete = { deleteTarget = session }
                )
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    deleteTarget?.let { session ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.history_delete_title)) },
            text = { Text(stringResource(R.string.history_delete_text, session.workoutNameRes?.let { stringResource(it) } ?: session.workoutName)) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteSession(session.sessionId)
                    deleteTarget = null
                }) {
                    Text(stringResource(R.string.history_delete_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun SessionCard(
    session: SessionSummaryItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f).padding(top = 8.dp, bottom = 8.dp)) {
                Text(
                    session.workoutNameRes?.let { stringResource(it) } ?: session.workoutName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    session.timeRange,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatChip(stringResource(R.string.history_minutes, session.durationMinutes))
                    if (session.sessionType == "GYM") {
                        StatChip(pluralStringResource(R.plurals.history_sets_count, session.totalSets, session.totalSets))
                        if (session.totalVolumeKg > 0) {
                            StatChip(stringResource(R.string.weight_value, "%,.0f".format(session.totalVolumeKg)))
                        }
                    } else {
                        session.distanceKm?.let { km ->
                            StatChip(stringResource(R.string.km_value, "%.2f".format(km)))
                        }
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.history_delete_session),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun StatChip(text: String) {
    Box(
        modifier = Modifier
            .background(
                Purple.copy(alpha = 0.1f),
                MaterialTheme.shapes.small
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = Purple,
            fontSize = 12.sp
        )
    }
}
