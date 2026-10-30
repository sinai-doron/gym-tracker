package com.doron.gymtracker.ui.screens.routes

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.doron.gymtracker.data.LocationPoint
import com.doron.gymtracker.domain.model.SavedRoute
import com.doron.gymtracker.ui.components.RunMap
import com.doron.gymtracker.ui.theme.Purple
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Saved routes: each with a little map, renamed, deleted or run from here; GPX files are imported here. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutesScreen(
    viewModel: RoutesViewModel,
    onBack: () -> Unit,
    onRunRoute: () -> Unit
) {
    val routes by viewModel.routes.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val event by viewModel.event.collectAsState()
    val context = LocalContext.current
    var renaming by remember { mutableStateOf<SavedRoute?>(null) }
    var deleting by remember { mutableStateOf<SavedRoute?>(null) }
    var importing by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load() }

    // GPX files come with all sorts of types (application/gpx+xml, text/xml, octet-stream…)
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::import)
    }

    LaunchedEffect(event) {
        when (event) {
            RoutesViewModel.Event.Imported -> Toast.makeText(context, "המסלול נוסף", Toast.LENGTH_SHORT).show()
            RoutesViewModel.Event.NotARoute -> Toast.makeText(context, "לא נמצא מסלול בקובץ", Toast.LENGTH_LONG).show()
            null -> Unit
        }
        if (event != null) viewModel.eventHandled()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("מסלולים") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "חזרה")
                    }
                },
                actions = {
                    TextButton(onClick = { importing = true }, enabled = !busy) {
                        Text("ייבוא GPX", color = Purple)
                    }
                }
            )
        }
    ) { padding ->
        val list = routes
        when {
            list == null || busy -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Purple)
            }
            list.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding).padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    "אין עדיין מסלולים.\nשמרו ריצה כמסלול מההיסטוריה, או ייבאו קובץ GPX.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // A plain column: a handful of routes, each with its own map
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                list.forEach { route ->
                    RouteCard(
                        route,
                        onRename = { renaming = route },
                        onDelete = { deleting = route },
                        onRun = { viewModel.runRoute(route.id, onRunRoute) }
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    renaming?.let { route ->
        var name by remember(route.id) { mutableStateOf(route.name) }
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text("שם המסלול") },
            text = {
                OutlinedTextField(value = name, onValueChange = { name = it.take(40) }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = { viewModel.rename(route.id, name); renaming = null }, enabled = name.isNotBlank()) {
                    Text("שמור", color = Purple)
                }
            },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text("ביטול") } }
        )
    }

    // A step of our own before the system file picker, which has no cancel button
    if (importing) {
        ModalBottomSheet(onDismissRequest = { importing = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("ייבוא מסלול", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                Spacer(Modifier.height(8.dp))
                Text(
                    "בחרו קובץ GPX: מסלול שהורדתם, או ריצה שייצאתם מ-Strava, Garmin וכדומה.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        importing = false
                        importLauncher.launch(arrayOf("*/*"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Purple),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("📂 בחירת קובץ", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { importing = false },
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("ביטול", color = Purple)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "בבוחר הקבצים אין כפתור ביטול: כדי לחזור בלי לבחור, החליקו אחורה מקצה המסך.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    deleting?.let { route ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("למחוק את \"${route.name}\"?") },
            text = { Text("הריצות שנעשו במסלול נשארות בהיסטוריה.") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(route.id); deleting = null }) {
                    Text("מחק", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("ביטול") } }
        )
    }
}

@Composable
private fun RouteCard(route: SavedRoute, onRename: () -> Unit, onDelete: () -> Unit, onRun: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("d.M.yy", Locale.getDefault()) }
    val asRun = remember(route.id) { route.points.map { LocationPoint(it.latitude, it.longitude, null, null, null, 0) } }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        RunMap(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
            route = asRun,
            follow = false
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    route.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "%.1f ק״מ · נוסף %s".format(route.distanceMeters / 1000, dateFormat.format(Date(route.createdAt))),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onRename) { Icon(Icons.Default.Edit, contentDescription = "שינוי שם") }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "מחיקה", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Button(
            onClick = onRun,
            colors = ButtonDefaults.buttonColors(containerColor = Purple),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(48.dp)
        ) {
            Text("🏃 רוץ במסלול", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
        }
    }
}
