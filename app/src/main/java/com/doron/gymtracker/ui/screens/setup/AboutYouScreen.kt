package com.doron.gymtracker.ui.screens.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doron.gymtracker.R
import com.doron.gymtracker.data.prefs.DistanceUnit
import com.doron.gymtracker.data.prefs.Sex
import com.doron.gymtracker.data.prefs.WeekStart
import com.doron.gymtracker.data.prefs.WeightUnit
import com.doron.gymtracker.ui.theme.Purple
import java.time.Year

/**
 * Units, week start and a few optional details. On first launch it comes after the permissions
 * and before "Your training" ([onBack] null, the button says Next); from Settings it saves and
 * goes back. Units and week start start out as the phone's region has them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutYouScreen(
    onDone: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: AboutYouViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {},
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
                    }
                }
            )
        }
    ) { padding ->
        val s = state ?: return@Scaffold
        val profile = s.profile
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Text(stringResource(R.string.setup_about_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.setup_about_text),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Choice(
                stringResource(R.string.setup_weights_in),
                listOf(WeightUnit.KG to R.string.unit_kg, WeightUnit.LB to R.string.unit_lb),
                profile.weightUnit,
                viewModel::setWeightUnit
            )
            Choice(
                stringResource(R.string.setup_distances_in),
                listOf(DistanceUnit.KM to R.string.unit_km, DistanceUnit.MI to R.string.unit_mi),
                profile.distanceUnit
            ) { unit -> viewModel.update { it.copy(profile = it.profile.copy(distanceUnit = unit)) } }
            Choice(
                stringResource(R.string.setup_week_starts),
                listOf(WeekStart.SUNDAY to R.string.day_sunday, WeekStart.MONDAY to R.string.day_monday),
                profile.weekStart
            ) { day -> viewModel.update { it.copy(profile = it.profile.copy(weekStart = day)) } }

            Spacer(Modifier.height(28.dp))
            Text(
                stringResource(R.string.setup_optional),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            NumberField(
                stringResource(if (profile.weightUnit == WeightUnit.LB) R.string.setup_body_weight_lb else R.string.setup_body_weight_kg),
                s.bodyWeight,
                decimal = true
            ) { text -> viewModel.update { it.copy(bodyWeight = text) } }
            NumberField(stringResource(R.string.setup_steps_goal), s.stepGoal) { text -> viewModel.update { it.copy(stepGoal = text) } }
            NumberField(stringResource(R.string.setup_birth_year), s.birthYear) { text -> viewModel.update { it.copy(birthYear = text.take(4)) } }
            Choice(
                stringResource(R.string.setup_sex),
                listOf(Sex.FEMALE to R.string.sex_female, Sex.MALE to R.string.sex_male),
                profile.sex,
                // Tapping the chosen one again takes the answer back
                onChoose = { sex -> viewModel.update { it.copy(profile = it.profile.copy(sex = sex.takeIf { it != profile.sex })) } }
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.setup_optional_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(28.dp))
            Button(
                onClick = { viewModel.save(Year.now().value, onDone) },
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Purple),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    stringResource(if (onBack == null) R.string.onboarding_next else R.string.save),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun <T> Choice(label: String, options: List<Pair<T, Int>>, selected: T?, onChoose: (T) -> Unit) {
    Spacer(Modifier.height(20.dp))
    Text(label, style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        options.forEach { (value, text) ->
            FilterChip(selected = value == selected, onClick = { onChoose(value) }, label = { Text(stringResource(text)) })
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, decimal: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { text -> onChange(text.filter { it.isDigit() || (decimal && (it == '.' || it == ',')) }.take(6)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    )
}
