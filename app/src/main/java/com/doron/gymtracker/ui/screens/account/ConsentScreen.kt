package com.doron.gymtracker.ui.screens.account

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doron.gymtracker.R

/**
 * Shown before Google's account picker: what syncs, what never does, why, who sees it and how to
 * stop, with run routes as their own choice. Signing in happens only after "agree"; "not now"
 * leaves the app as it was. A prominent disclosure in Play's sense, since routes are location.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsentScreen(onDone: () -> Unit, viewModel: ConsentViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var routes by rememberSaveable { mutableStateOf(false) }
    val activity = LocalContext.current.findActivity()

    LaunchedEffect(state) { if (state is ConsentViewModel.State.Done) onDone() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.consent_title)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.consent_not_now))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(stringResource(R.string.consent_intro), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(16.dp))
            Point(R.string.consent_synced_title, R.string.consent_synced)
            Point(R.string.consent_never_title, R.string.consent_never)
            Point(R.string.consent_why_title, R.string.consent_why)
            Point(R.string.consent_who_title, R.string.consent_who)
            Point(R.string.consent_stop_title, R.string.consent_stop)

            Spacer(Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.consent_routes_title), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.consent_routes_text),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(checked = routes, onCheckedChange = { routes = it })
                }
            }

            Spacer(Modifier.height(24.dp))
            (state as? ConsentViewModel.State.Failed)?.let { failed ->
                Text(
                    stringResource(
                        if (failed.notAllowed) R.string.account_not_allowed else R.string.account_sign_in_failed
                    ),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
            }
            val busy = state is ConsentViewModel.State.Busy
            Button(
                onClick = { activity?.let { viewModel.agreeAndSignIn(it, routes) } },
                enabled = !busy && activity != null,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (busy) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                else Text(stringResource(R.string.consent_agree))
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDone, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.consent_not_now))
            }
        }
    }
}

@Composable
private fun Point(title: Int, text: Int) {
    Column(modifier = Modifier.padding(bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Text(stringResource(text), style = MaterialTheme.typography.bodyMedium)
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
