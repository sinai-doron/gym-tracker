package com.doron.gymtracker.ui.screens.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.doron.gymtracker.R
import com.doron.gymtracker.account.Consent
import com.doron.gymtracker.account.SignedInUser
import com.doron.gymtracker.sync.SyncStatus
import android.text.format.DateUtils

/**
 * Settings' account section. Signed out: what signing in is for, and that it is optional.
 * Signed in: the Google picture, name and email, the run-routes switch, and sign-out.
 */
@Composable
fun AccountCard(
    user: SignedInUser?,
    consent: Consent?,
    syncStatus: SyncStatus,
    onSyncNow: () -> Unit,
    onSignIn: () -> Unit,
    onRoutesChange: (Boolean) -> Unit,
    onMusicChange: (Boolean) -> Unit,
    onSignOut: () -> Unit,
    onDeleteServerData: () -> Unit,
    deleteFailed: Boolean,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (user == null) {
                Text(
                    stringResource(R.string.account_signed_out_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = onSignIn, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.account_sign_in))
                }
                return@Column
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(user)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        user.displayName ?: user.email,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        user.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.consent_routes_title), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.account_routes_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(checked = consent?.routes == true, onCheckedChange = onRoutesChange)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.consent_music_title), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.account_music_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(checked = consent?.music == true, onCheckedChange = onMusicChange)
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    syncStatusText(syncStatus),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (syncStatus.lastError != null) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onSyncNow) { Text(stringResource(R.string.account_sync_now)) }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.account_sign_out))
            }
            var confirmDelete by remember { mutableStateOf(false) }
            TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.account_delete_server_data), color = MaterialTheme.colorScheme.error)
            }
            if (deleteFailed) {
                Text(
                    stringResource(R.string.account_delete_failed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (confirmDelete) {
                AlertDialog(
                    onDismissRequest = { confirmDelete = false },
                    title = { Text(stringResource(R.string.account_delete_server_data)) },
                    text = { Text(stringResource(R.string.account_delete_confirm_text)) },
                    confirmButton = {
                        TextButton(onClick = { confirmDelete = false; onDeleteServerData() }) {
                            Text(stringResource(R.string.account_delete_confirm), color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
                    }
                )
            }
        }
    }
}

@Composable
private fun syncStatusText(status: SyncStatus): String {
    val last = status.lastSuccess?.let {
        DateUtils.getRelativeTimeSpanString(it, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
    }
    return when {
        status.lastError != null && last != null -> stringResource(R.string.account_sync_pending_since, last)
        status.lastError != null -> stringResource(R.string.account_sync_pending)
        last != null -> stringResource(R.string.account_synced_at, last)
        else -> stringResource(R.string.account_sync_never)
    }
}

@Composable
private fun Avatar(user: SignedInUser) {
    val modifier = Modifier.size(48.dp).clip(CircleShape)
    if (user.pictureUrl != null) {
        AsyncImage(
            model = user.pictureUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Box(modifier.background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Text(
                (user.displayName ?: user.email).take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
