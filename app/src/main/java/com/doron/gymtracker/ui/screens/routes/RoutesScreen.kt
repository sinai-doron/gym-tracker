package com.doron.gymtracker.ui.screens.routes

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.doron.gymtracker.R
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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

/** Saved routes, each with a little map, opening its details; GPX files are imported here. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutesScreen(
    viewModel: RoutesViewModel,
    onBack: () -> Unit,
    onOpenRoute: (Long) -> Unit
) {
    val routes by viewModel.routes.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val event by viewModel.event.collectAsState()
    val context = LocalContext.current
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
                }
            )
        },
        floatingActionButton = {
            if (!busy && routes != null) {
                ExtendedFloatingActionButton(
                    onClick = { importing = true },
                    containerColor = Purple,
                    contentColor = Color.White,
                    icon = { Icon(painterResource(R.drawable.ic_import_route), contentDescription = null) },
                    text = { Text("ייבוא מסלול", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)) }
                )
            }
        }
    ) { padding ->
        val list = routes
        when {
            list == null || busy -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Purple)
            }
            list.isEmpty() -> Column(
                Modifier.fillMaxSize().padding(padding).padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ImportBadge(size = 96.dp)
                Spacer(Modifier.height(20.dp))
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
                    RouteCard(route, onClick = { onOpenRoute(route.id) })
                }
                // Room for the import button over the last card
                Spacer(Modifier.height(80.dp))
            }
        }
    }

    // A step of our own before the system file picker, which has no cancel button
    if (importing) {
        ModalBottomSheet(onDismissRequest = { importing = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ImportBadge(size = 72.dp)
                Spacer(Modifier.height(16.dp))
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
                    Icon(painterResource(R.drawable.ic_import_route), contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("בחירת קובץ GPX", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
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
}

/** The import icon in a soft purple circle. */
@Composable
private fun ImportBadge(size: Dp) {
    Box(
        modifier = Modifier.size(size).background(Purple.copy(alpha = 0.12f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painterResource(R.drawable.ic_import_route),
            contentDescription = null,
            tint = Purple,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

@Composable
private fun RouteCard(route: SavedRoute, onClick: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("d.M.yy", Locale.getDefault()) }
    val asRun = remember(route.id) { route.points.map { LocationPoint(it.latitude, it.longitude, null, null, null, 0) } }
    Card(
        onClick = onClick,
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
            modifier = Modifier.fillMaxWidth().padding(16.dp),
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
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
