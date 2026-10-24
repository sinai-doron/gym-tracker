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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doron.gymtracker.R
import com.doron.gymtracker.ui.components.SetLogRow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val heLocale = Locale("he")

// Sunday-first week order for Israeli calendar
private val weekDayOrder = listOf(
    DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
)
private val dayHeaders = listOf("א", "ב", "ג", "ד", "ה", "ו", "ש")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onBack: () -> Unit = {}, viewModel: HistoryViewModel = hiltViewModel()) {
    val month by viewModel.currentMonth.collectAsState()
    val sessionDates by viewModel.sessionDates.collectAsState()
    val selectedDay by viewModel.selectedDaySession.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "חזרה")
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
            CalendarGrid(month = month, sessionDates = sessionDates, onDayClick = { viewModel.loadDaySession(it) })
        }
    }

    if (selectedDay != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.clearSelectedDay() },
            sheetState = sheetState
        ) {
            DaySessionSheet(state = selectedDay!!)
        }
    }
}

@Composable
private fun MonthHeader(month: YearMonth, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        // In RTL: ArrowForward (→) is visually on the right = "start" = next month
        // ArrowBack (←) is visually on the left = "end" = previous month
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // In RTL layout, this button appears on the LEFT = goes to next month
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "חודש הבא")
        }
        val monthName = month.month.getDisplayName(TextStyle.FULL_STANDALONE, heLocale)
        Text(
            "$monthName ${month.year}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        // In RTL layout, this button appears on the RIGHT = goes to previous month
        IconButton(onClick = onPrev) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "חודש קודם")
        }
    }
}

@Composable
private fun DayHeaders() {
    Row(modifier = Modifier.fillMaxWidth()) {
        dayHeaders.forEach { label ->
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
    sessionDates: Set<LocalDate>,
    onDayClick: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val firstDay = month.atDay(1)
    // How many blank cells before day 1 (Sunday = 0)
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
                        val hasSession = date in sessionDates
                        val isToday = date == today
                        DayCell(
                            day = dayNum,
                            hasSession = hasSession,
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
    hasSession: Boolean,
    isToday: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)?
) {
    val bgColor = when {
        hasSession -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surface
    }
    val textColor = when {
        hasSession -> MaterialTheme.colorScheme.onPrimary
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
            .background(bgColor)
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

@Composable
private fun DaySessionSheet(state: LastSessionUiState) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            "אימון ${state.workoutLabel?.name ?: ""} — ${state.dateLabel ?: ""}",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(16.dp))
        if (state.summaries.isEmpty()) {
            Text(
                stringResource(R.string.no_sessions_yet),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(state.summaries) { _, summary ->
                    Column {
                        Text(
                            summary.exerciseName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        summary.sets.forEachIndexed { i, set ->
                            SetLogRow(setNumber = i + 1, record = set)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}
