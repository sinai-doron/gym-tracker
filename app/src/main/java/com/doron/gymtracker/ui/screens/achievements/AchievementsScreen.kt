package com.doron.gymtracker.ui.screens.achievements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doron.gymtracker.R
import com.doron.gymtracker.domain.model.AchievementFamily
import com.doron.gymtracker.ui.components.AchievementItem
import com.doron.gymtracker.ui.components.BadgesRow
import com.doron.gymtracker.ui.screens.home.BadgesViewModel
import com.doron.gymtracker.data.places.Place
import com.doron.gymtracker.domain.usecase.PlaceBadgeStatus
import com.doron.gymtracker.ui.components.PlaceBadgeImage
import com.doron.gymtracker.ui.components.appLanguage
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Every achievement, by family: the distance badges first, then the rest, each with its progress. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(onBack: () -> Unit, viewModel: BadgesViewModel = hiltViewModel()) {
    val badges by viewModel.badges.collectAsState()
    val achievements by viewModel.achievements.collectAsState()
    val places by viewModel.places.collectAsState()
    LaunchedEffect(Unit) { viewModel.refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.achievements_all)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (badges.isNotEmpty()) {
                Section(title = stringResource(R.string.home_badges_title), explain = stringResource(R.string.home_badges_explain)) { BadgesRow(badges) }
            }
            if (places.isNotEmpty()) {
                Section(title = stringResource(R.string.places_title), explain = stringResource(R.string.places_explain)) { Places(places) }
            }
            AchievementFamily.entries.forEach { family ->
                val inFamily = achievements.filter { it.kind.family == family }
                if (inFamily.isEmpty()) return@forEach
                Section(title = stringResource(family.title), explain = stringResource(family.explain)) {
                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        inFamily.forEach { AchievementItem(it, size = 88.dp) }
                    }
                }
            }
        }
    }
}

/** The countries, each with when it was earned; then the US states, the ones run in first. */
@Composable
private fun Places(places: List<PlaceBadgeStatus>) {
    val language = appLanguage()
    val dateFormat = remember { SimpleDateFormat("d.M.yy", Locale.getDefault()) }
    val countries = places.filter { it.place.kind == Place.Kind.COUNTRY }
    val states = places.filter { it.place.kind == Place.Kind.STATE }
        .sortedWith(compareBy({ !it.isEarned }, { it.earnedAt ?: 0L }, { it.place.nameIn(language) }))
    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
        countries.forEach { status ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PlaceBadgeImage(status.place, status.isEarned, size = 100.dp)
                Text(
                    status.earnedAt?.let { dateFormat.format(Date(it)) } ?: stringResource(R.string.achievement_not_yet),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    if (states.isNotEmpty()) {
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.places_us_states, states.count { it.isEarned }, states.size),
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(Modifier.height(8.dp))
        states.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                row.forEach { PlaceBadgeImage(it.place, it.isEarned, size = 100.dp) }
                // A short last row keeps the grid's columns
                repeat(3 - row.size) { Spacer(Modifier.size(100.dp)) }
            }
        }
    }
}

@Composable
private fun Section(title: String, explain: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(explain, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        content()
    }
}
