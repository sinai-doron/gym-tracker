package com.doron.gymtracker.ui.screens.routes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doron.gymtracker.data.Slope
import com.doron.gymtracker.ui.components.ElevationChart
import com.doron.gymtracker.ui.components.RunMap
import com.doron.gymtracker.ui.components.SlopeColors
import com.doron.gymtracker.ui.theme.Purple
import kotlin.math.roundToInt

/** One route: its map by slope, distance, climb and expected time; run, renamed or deleted from here. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteDetailScreen(
    viewModel: RouteDetailViewModel,
    onBack: () -> Unit,
    onRunRoute: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val gone by viewModel.gone.collectAsState()
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }

    LaunchedEffect(gone) { if (gone) onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "חזרה")
                    }
                },
                actions = {
                    IconButton(onClick = { renaming = true }, enabled = state != null) {
                        Icon(Icons.Default.Edit, contentDescription = "שינוי שם")
                    }
                    IconButton(onClick = { deleting = true }, enabled = state != null) {
                        Icon(Icons.Default.Delete, contentDescription = "מחיקה")
                    }
                }
            )
        },
        bottomBar = {
            if (state != null) {
                Button(
                    onClick = { viewModel.runRoute(onRunRoute) },
                    colors = ButtonDefaults.buttonColors(containerColor = Purple),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(56.dp)
                ) {
                    Text("🏃 רוץ במסלול", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    ) { padding ->
        val s = state
        if (s == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Purple)
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🏃", fontSize = 28.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                s.route.name,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            streets(s)?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Box {
                        RunMap(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .clip(RoundedCornerShape(20.dp)),
                            plannedRoute = s.route.points,
                            follow = false,
                            interactive = true,
                            slopeColors = true,
                            showEnds = true
                        )
                        MapLegend(s, Modifier.align(Alignment.TopStart).padding(10.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    StatTile("מרחק", "%.2f".format(s.route.distanceMeters / 1000), "ק״מ", Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatTile("עלייה מצטברת", meters(s.climbM), "מ׳", Modifier.weight(1f))
                        StatTile("ירידה מצטברת", meters(s.descentM), "מ׳", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatTile("נקודה גבוהה", meters(s.highestM), "מ׳", Modifier.weight(1f))
                        StatTile("זמן משוער", formatTime(s.estimatedSecs), null, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (s.paceFromRuns) "הזמן המשוער לפי הקצב הממוצע שלך בריצות האחרונות (%s לק״מ)".format(formatPace(s.paceSecsPerKm))
                        else "הזמן המשוער לפי קצב של %s לק״מ".format(formatPace(s.paceSecsPerKm)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (s.profile.size >= 2) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("גובה", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Spacer(Modifier.height(8.dp))
                        ElevationChart(s.profile)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    state?.let { s ->
        if (renaming) {
            var name by remember(s.route.id) { mutableStateOf(s.route.name) }
            AlertDialog(
                onDismissRequest = { renaming = false },
                title = { Text("שם המסלול") },
                text = { OutlinedTextField(value = name, onValueChange = { name = it.take(40) }, singleLine = true) },
                confirmButton = {
                    TextButton(onClick = { viewModel.rename(name); renaming = false }, enabled = name.isNotBlank()) {
                        Text("שמור", color = Purple)
                    }
                },
                dismissButton = { TextButton(onClick = { renaming = false }) { Text("ביטול") } }
            )
        }
        if (deleting) {
            AlertDialog(
                onDismissRequest = { deleting = false },
                title = { Text("למחוק את \"${s.route.name}\"?") },
                text = { Text("הריצות שנעשו במסלול נשארות בהיסטוריה.") },
                confirmButton = {
                    TextButton(onClick = { deleting = false; viewModel.delete() }) {
                        Text("מחק", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = { TextButton(onClick = { deleting = false }) { Text("ביטול") } }
            )
        }
    }
}

/** "Berners Street ~ Oxford Street", or just the start for a loop; null until looked up. */
private fun streets(s: RouteDetailState): String? = when {
    s.startStreet == null -> null
    s.isLoop -> "מעגלי · ${s.startStreet}"
    s.finishStreet == null -> s.startStreet
    else -> "${s.startStreet} ~ ${s.finishStreet}"
}

@Composable
private fun MapLegend(s: RouteDetailState, modifier: Modifier = Modifier) {
    val entries = buildList {
        if (Slope.UP in s.slopes) add(SlopeColors.up to "עלייה")
        if (Slope.DOWN in s.slopes) add(SlopeColors.down to "ירידה")
        add(SlopeColors.start to "התחלה")
        if (!s.isLoop) add(SlopeColors.finish to "סיום")
    }
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        entries.forEach { (color, label) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(color, CircleShape))
                Spacer(Modifier.width(6.dp))
                Text(label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, unit: String?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(vertical = 14.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = if (value == "—") MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified
            )
            if (unit != null && value != "—") {
                Spacer(Modifier.width(4.dp))
                Text(
                    unit,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

private fun meters(m: Double?): String = m?.roundToInt()?.toString() ?: "—"

private fun formatPace(secsPerKm: Long): String = "%d:%02d".format(secsPerKm / 60, secsPerKm % 60)

private fun formatTime(totalSecs: Long): String {
    val h = totalSecs / 3600
    val m = (totalSecs % 3600) / 60
    val s = totalSecs % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
