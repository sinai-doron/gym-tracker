package com.doron.gymtracker.ui.screens.body

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.doron.gymtracker.R
import com.doron.gymtracker.data.prefs.WeighInReminder
import com.doron.gymtracker.ui.theme.AppColors
import com.doron.gymtracker.ui.theme.AppType

/**
 * Settings' weight and water section: whether each shows on Home, and their reminders. A reminder
 * switched on asks for notifications, if not yet allowed.
 */
@Composable
fun BodySettingsSection(viewModel: BodySettingsViewModel = hiltViewModel()) {
    val showWeight by viewModel.showWeight.collectAsState()
    val showWater by viewModel.showWater.collectAsState()
    val weightReminder by viewModel.weightReminder.collectAsState()
    val waterReminder by viewModel.waterReminder.collectAsState()
    val context = LocalContext.current
    fun allowed() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    var notificationsAllowed by remember { mutableStateOf(allowed()) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notificationsAllowed = it }
    val askIfNeeded = {
        if (!allowed() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ask.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BodyCard {
            SwitchRow(stringResource(R.string.body_show_weight), showWeight, viewModel::setShowWeight)
            SwitchRow(stringResource(R.string.body_show_water), showWater, viewModel::setShowWater)
        }
        BodyCard {
            Text(stringResource(R.string.body_weight_reminder), style = bodyTitle.copy(color = AppColors.textPrimary))
            WeightReminderControls(weightReminder) { r ->
                if (r.frequency != WeighInReminder.OFF && weightReminder.frequency == WeighInReminder.OFF) askIfNeeded()
                viewModel.setWeightReminder(r)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(AppColors.border))
            WaterReminderControls(waterReminder) { r ->
                if (r.on && !waterReminder.on) askIfNeeded()
                viewModel.setWaterReminder(r)
            }
            if (!notificationsAllowed && (weightReminder.frequency != WeighInReminder.OFF || waterReminder.on)) {
                Text(stringResource(R.string.body_reminder_notifications), style = AppType.hint.copy(color = AppColors.warnText))
            }
        }
    }
}
