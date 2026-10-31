package com.doron.gymtracker.ui.screens.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.health.connect.client.PermissionController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewModelScope
import com.doron.gymtracker.R
import com.doron.gymtracker.account.Account
import com.doron.gymtracker.data.HealthConnectManager
import com.doron.gymtracker.data.prefs.SettingsPrefs
import com.doron.gymtracker.media.MediaPermissionHelper
import com.doron.gymtracker.ui.theme.Purple
import com.doron.gymtracker.ui.theme.SuccessGreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One page of the wizard. The welcome and ready pages ask for nothing. */
enum class OnboardingStep(
    @DrawableRes val image: Int,
    @StringRes val title: Int,
    @StringRes val text: Int,
    @StringRes val button: Int
) {
    WELCOME(R.drawable.onboarding_welcome, R.string.onboarding_welcome_title, R.string.onboarding_welcome_text, R.string.onboarding_lets_go),
    NOTIFICATIONS(R.drawable.onboarding_notifications, R.string.onboarding_notifications_title, R.string.onboarding_notifications_text, R.string.onboarding_notifications_button),
    ACTIVITY(R.drawable.onboarding_activity, R.string.onboarding_activity_title, R.string.onboarding_activity_text, R.string.onboarding_activity_button),
    LOCATION(R.drawable.onboarding_location, R.string.onboarding_location_title, R.string.onboarding_location_text, R.string.onboarding_location_button),
    HEALTH(R.drawable.onboarding_health, R.string.onboarding_health_title, R.string.onboarding_health_text, R.string.onboarding_health_button),
    MUSIC(R.drawable.onboarding_music, R.string.onboarding_music_title, R.string.onboarding_music_text, R.string.onboarding_music_button),
    // Optional sign-in: opens the same consent screen as Settings, so nothing signs in without it.
    ACCOUNT(R.drawable.onboarding_account, R.string.onboarding_account_title, R.string.onboarding_account_text, R.string.account_sign_in),
    READY(R.drawable.onboarding_ready, R.string.onboarding_ready_title, R.string.onboarding_ready_text, R.string.onboarding_start);

    val asksPermission: Boolean get() = this != WELCOME && this != READY
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsPrefs: SettingsPrefs,
    val healthConnect: HealthConnectManager,
    private val account: Account
) : ViewModel() {

    suspend fun signedIn(): Boolean = account.user.first() != null

    /** The pages that apply to this phone: older Androids grant some permissions at install. */
    val steps: List<OnboardingStep> = OnboardingStep.entries.filter { step ->
        when (step) {
            OnboardingStep.NOTIFICATIONS -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            OnboardingStep.ACTIVITY -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            OnboardingStep.HEALTH -> healthConnect.isAvailable
            else -> true
        }
    }

    fun finish(onDone: () -> Unit) {
        viewModelScope.launch {
            settingsPrefs.setOnboardingDone()
            onDone()
        }
    }
}

private fun Context.granted(permission: String) =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    onSignIn: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val steps = viewModel.steps
    val pagerState = rememberPagerState { steps.size }
    val granted = remember { mutableStateMapOf<OnboardingStep, Boolean>() }
    // Bumped on resume so grants made in system settings show up
    var checks by remember { mutableIntStateOf(0) }

    fun advance() {
        scope.launch {
            if (pagerState.currentPage < steps.lastIndex) {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        }
    }

    fun finish() = viewModel.finish(onFinished)

    LaunchedEffect(checks) {
        granted[OnboardingStep.NOTIFICATIONS] =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || context.granted(Manifest.permission.POST_NOTIFICATIONS)
        granted[OnboardingStep.ACTIVITY] =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || context.granted(Manifest.permission.ACTIVITY_RECOGNITION)
        granted[OnboardingStep.LOCATION] = context.granted(Manifest.permission.ACCESS_FINE_LOCATION)
        granted[OnboardingStep.MUSIC] = MediaPermissionHelper.isNotificationListenerEnabled(context)
        granted[OnboardingStep.HEALTH] = viewModel.healthConnect.hasAllPermissions()
        granted[OnboardingStep.ACCOUNT] = viewModel.signedIn()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val wasMusic = granted[OnboardingStep.MUSIC] == true
        checks++
        // Back from the consent screen, signed in: move along
        if (steps.getOrNull(pagerState.currentPage) == OnboardingStep.ACCOUNT && granted[OnboardingStep.ACCOUNT] != true) {
            scope.launch { if (viewModel.signedIn()) { granted[OnboardingStep.ACCOUNT] = true; advance() } }
        }
        // Coming back from the listener settings with it switched on: move along
        if (!wasMusic && steps.getOrNull(pagerState.currentPage) == OnboardingStep.MUSIC &&
            MediaPermissionHelper.isNotificationListenerEnabled(context)
        ) advance()
    }

    // Whatever the answer, move on: the person decided, and Settings can change it later
    val singlePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        checks++; advance()
    }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        checks++; advance()
    }
    val healthPermission = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { checks++; advance() }

    fun request(step: OnboardingStep) {
        when (step) {
            OnboardingStep.WELCOME -> advance()
            OnboardingStep.NOTIFICATIONS -> singlePermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            OnboardingStep.ACTIVITY -> singlePermission.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            OnboardingStep.LOCATION -> locationPermission.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
            OnboardingStep.HEALTH -> healthPermission.launch(HealthConnectManager.ALL_PERMISSIONS)
            OnboardingStep.MUSIC -> MediaPermissionHelper.openNotificationListenerSettings(context)
            OnboardingStep.ACCOUNT -> onSignIn()
            OnboardingStep.READY -> finish()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Skip the whole wizard from any page but the last
        Box(Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 8.dp)) {
            if (pagerState.currentPage < steps.lastIndex) {
                TextButton(onClick = ::finish, modifier = Modifier.align(Alignment.CenterEnd)) {
                    Text(stringResource(R.string.onboarding_skip), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            val step = steps[page]
            OnboardingPage(
                step = step,
                isGranted = granted[step] == true,
                onPrimary = { if (step.asksPermission && granted[step] == true) advance() else request(step) },
                onSkip = ::advance
            )
        }

        PageDots(
            count = steps.size,
            current = pagerState.currentPage,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp, bottom = 28.dp)
        )
    }
}

@Composable
private fun OnboardingPage(
    step: OnboardingStep,
    isGranted: Boolean,
    onPrimary: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
    ) {
        Image(
            painter = painterResource(step.image),
            contentDescription = null,
            // The art is drawn on the page color, so it sits on the page without a frame
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .widthIn(max = 340.dp)
                .fillMaxWidth()
                .aspectRatio(1f)
        )
        Spacer(Modifier.height(32.dp))
        Text(
            stringResource(step.title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(step.text),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))

        val done = step.asksPermission && isGranted
        Button(
            onClick = onPrimary,
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (done) SuccessGreen else Purple),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(
                stringResource(if (done) R.string.onboarding_enabled else step.button),
                style = MaterialTheme.typography.titleMedium
            )
        }
        // Keeps the button in the same place on every page
        Box(Modifier.height(48.dp), contentAlignment = Alignment.Center) {
            if (step.asksPermission) {
                TextButton(onClick = onSkip) {
                    Text(
                        stringResource(if (done) R.string.onboarding_next else R.string.onboarding_skip),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PageDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        repeat(count) { index ->
            val selected = index == current
            val width by animateDpAsState(if (selected) 24.dp else 8.dp, label = "dot-width")
            val color by animateColorAsState(if (selected) Purple else Purple.copy(alpha = 0.25f), label = "dot-color")
            Box(
                Modifier
                    .size(width = width, height = 8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}
