package com.doron.gymtracker.ui.screens.types

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doron.gymtracker.R
import com.doron.gymtracker.data.db.ActivityTypeMigration
import com.doron.gymtracker.data.repository.TrainingTypes
import com.doron.gymtracker.domain.model.TrainingType
import com.doron.gymtracker.ui.components.TrainingTypeBadge
import com.doron.gymtracker.ui.components.TrainingTypeIcons
import com.doron.gymtracker.ui.components.displayName
import com.doron.gymtracker.ui.components.recorderLabel
import com.doron.gymtracker.ui.theme.Purple
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class TrainingTypesViewModel @Inject constructor(
    private val types: TrainingTypes
) : ViewModel() {

    private val _all = MutableStateFlow<List<TrainingType>?>(null)
    val all: StateFlow<List<TrainingType>?> = _all.asStateFlow()

    init { reload() }

    private fun reload() {
        viewModelScope.launch { _all.value = types.all() }
    }

    private fun change(block: suspend () -> Unit) {
        viewModelScope.launch {
            block()
            _all.value = types.all()
        }
    }

    fun setShown(id: String, shown: Boolean) = change { types.setShown(id, shown) }
    fun create(name: String, icon: String) = change { types.create(name, icon) }
    fun edit(id: String, name: String, icon: String) = change { types.edit(id, name, icon) }
    fun remove(id: String) = change { types.remove(id) }
}

/**
 * Settings → Training types: the built-ins, shown or hidden, and the user's own. A new type is
 * recorded with a timer; home offers every shown one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingTypesScreen(onBack: () -> Unit, viewModel: TrainingTypesViewModel = hiltViewModel()) {
    val all by viewModel.all.collectAsState()
    var editing by remember { mutableStateOf<TrainingType?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.types_title)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
                }
            )
        }
    ) { padding ->
        val types = all ?: return@Scaffold
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .imePadding(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "intro") {
                Text(
                    stringResource(R.string.types_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            items(types, key = { it.id }) { type ->
                TypeRow(
                    type = type,
                    onShown = { viewModel.setShown(type.id, it) },
                    onEdit = if (type.builtin) null else ({ editing = type })
                )
            }
            item(key = "new") { NewType(onAdd = viewModel::create) }
        }
    }

    editing?.let { type ->
        EditDialog(
            type = type,
            onDismiss = { editing = null },
            onSave = { name, icon -> viewModel.edit(type.id, name, icon); editing = null },
            onRemove = { viewModel.remove(type.id); editing = null }
        )
    }
}

@Composable
private fun TypeRow(type: TrainingType, onShown: (Boolean) -> Unit, onEdit: (() -> Unit)?) {
    val name = type.displayName()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .then(if (onEdit != null) Modifier.clickable(onClickLabel = stringResource(R.string.types_edit, name), onClick = onEdit) else Modifier)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        TrainingTypeBadge(type)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(
                type.recorderLabel() + if (type.builtin) " · " + stringResource(R.string.types_builtin) else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // Strength is what programmes are: always there
        if (type.id != ActivityTypeMigration.STRENGTH) {
            Switch(checked = !type.hidden, onCheckedChange = onShown)
        }
    }
}

@Composable
private fun NewType(onAdd: (name: String, icon: String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf(TrainingTypeIcons.CHOICES.first()) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Text(stringResource(R.string.types_new), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        TypeFields(name, { name = it }, icon, { icon = it })
        Text(
            stringResource(R.string.types_new_timer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Button(
            onClick = { onAdd(name, icon); name = "" },
            enabled = name.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = Purple),
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.types_add)) }
    }
}

@Composable
private fun TypeFields(name: String, onName: (String) -> Unit, icon: String, onIcon: (String) -> Unit) {
    OutlinedTextField(
        value = name,
        onValueChange = { onName(it.take(40)) },
        label = { Text(stringResource(R.string.types_name)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(TrainingTypeIcons.CHOICES, key = { it }) { slug ->
            Text(
                TrainingTypeIcons.emoji(slug),
                fontSize = 22.sp,
                modifier = Modifier
                    .size(44.dp)
                    .background(if (slug == icon) Purple.copy(alpha = 0.18f) else MaterialTheme.colorScheme.background, CircleShape)
                    .clickable { onIcon(slug) }
                    .padding(9.dp)
            )
        }
    }
}

@Composable
private fun EditDialog(type: TrainingType, onDismiss: () -> Unit, onSave: (String, String) -> Unit, onRemove: () -> Unit) {
    var name by remember { mutableStateOf(type.name.orEmpty()) }
    var icon by remember { mutableStateOf(type.icon) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.types_edit, type.name.orEmpty())) },
        text = { Column { TypeFields(name, { name = it }, icon, { icon = it }) } },
        confirmButton = {
            TextButton(onClick = { onSave(name, icon) }, enabled = name.isNotBlank()) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onRemove) { Text(stringResource(R.string.remove), color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        }
    )
}
