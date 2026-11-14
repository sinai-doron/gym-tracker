package com.doron.gymtracker.ui.screens.body

import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doron.gymtracker.R
import com.doron.gymtracker.data.of
import com.doron.gymtracker.data.prefs.WaterReminder
import com.doron.gymtracker.data.prefs.WeighInReminder
import com.doron.gymtracker.data.prefs.WeightReminder
import com.doron.gymtracker.data.prefs.WeightUnit
import com.doron.gymtracker.ui.components.LocalWeightUnit
import com.doron.gymtracker.ui.components.appLocale
import com.doron.gymtracker.ui.components.weightUnitLabel
import com.doron.gymtracker.ui.theme.AppColors
import com.doron.gymtracker.ui.theme.AppType
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

// The weight and water designs' measures
internal val BodyGutter = 16.dp
internal val BodyGap = 12.dp
internal val BodyCardRadius = 20.dp
internal val BodyInnerRadius = 14.dp

internal val bodyTitle = AppType.button
internal val bodyCaption = AppType.caption
internal val bodyBig = AppType.cardTitle.copy(fontSize = 52.sp, lineHeight = 52.sp, letterSpacing = (-1).sp)

/** [kg] in the person's unit to a tenth: "82.4", "181.7". */
internal fun WeightUnit.tenth(kg: Double, locale: Locale): String = "%.1f".format(locale, of(kg.toFloat()))

/** A change in kg, signed, in the person's unit: "−0.4", "+1.2", "0.0". */
internal fun WeightUnit.signed(kgChange: Double, locale: Locale): String {
    val v = of(kgChange.toFloat())
    val text = "%.1f".format(locale, abs(v))
    val number = when {
        v <= -0.05 -> "−$text"
        v >= 0.05 -> "+$text"
        else -> "%.1f".format(locale, 0.0)
    }
    // Kept left to right, so Hebrew shows "−2.7" and not "2.7−"
    return "\u2066$number\u2069"
}

@Composable
internal fun weightTenth(kg: Double): String = LocalWeightUnit.current.tenth(kg, appLocale())

/** "82.4 kg". */
@Composable
internal fun weightTenthText(kg: Double): String = stringResource(R.string.units_value, weightTenth(kg), weightUnitLabel())

/** Whole millilitres, grouped: "2,500". */
internal fun mlText(ml: Int, locale: Locale): String = "%,d".format(locale, ml)

/** "07:30", or "7:30 AM" where the phone is set to a 12-hour clock. */
@Composable
internal fun clockText(minuteOfDay: Int): String {
    val context = LocalContext.current
    val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
    return LocalTime.of(minuteOfDay / 60 % 24, minuteOfDay % 60).format(DateTimeFormatter.ofPattern(pattern, appLocale()))
}

/** The screen's own header: back (or close), the title over an optional line, an optional action. */
@Composable
internal fun BodyHeader(
    title: String,
    onBack: () -> Unit,
    subtitle: String? = null,
    close: Boolean = false,
    @DrawableRes action: Int? = null,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 4.dp, end = 8.dp, top = 12.dp, bottom = 8.dp)
    ) {
        RoundIcon(
            if (close) R.drawable.ic_close else R.drawable.ic_back,
            stringResource(if (close) R.string.body_nav_close else R.string.body_nav_back),
            onBack
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = AppType.headerTitle.copy(fontWeight = FontWeight.SemiBold), color = AppColors.textPrimary)
            subtitle?.let { Text(it, style = bodyCaption) }
        }
        if (action != null) RoundIcon(action, actionLabel.orEmpty(), onAction)
    }
}

@Composable
private fun RoundIcon(@DrawableRes icon: Int, label: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label }
    ) {
        // The back arrow is auto-mirrored, so it points the right way in Hebrew
        Icon(painterResource(icon), contentDescription = null, tint = AppColors.textPrimary, modifier = Modifier.size(22.dp))
    }
}

/** A white section card. */
@Composable
internal fun BodyCard(
    modifier: Modifier = Modifier,
    padding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(16.dp),
    gap: androidx.compose.ui.unit.Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(gap),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BodyCardRadius))
            .background(AppColors.surface)
            .padding(padding),
        content = content
    )
}

/** The footer's main button, full width, over the navigation bar. */
@Composable
internal fun BodyFooter(text: String, onClick: () -> Unit, @DrawableRes icon: Int? = null, enabled: Boolean = true) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(AppColors.surface)
            .border(1.dp, AppColors.border)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = BodyGutter, end = BodyGutter, top = 12.dp, bottom = 16.dp)
    ) {
        BodyButton(text, onClick, icon, enabled)
    }
}

@Composable
internal fun BodyButton(text: String, onClick: () -> Unit, @DrawableRes icon: Int? = null, enabled: Boolean = true, color: Color = AppColors.accentStrength) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) color else color.copy(alpha = 0.4f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
    ) {
        icon?.let { Icon(painterResource(it), contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp)) }
        Text(text, style = AppType.button, color = Color.White)
    }
}

/** A round − or + beside a big number. */
@Composable
internal fun StepButton(plus: Boolean, label: String, onClick: () -> Unit, background: Color = AppColors.ground) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label }
    ) {
        Icon(
            painterResource(if (plus) R.drawable.ic_home_plus else R.drawable.ic_minus),
            contentDescription = null, tint = AppColors.textPrimary, modifier = Modifier.size(20.dp)
        )
    }
}

/** A row of choices on a tinted track, the chosen one raised in white. */
@Composable
internal fun <T> Segmented(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 40.dp,
    // Equal shares of a full-width track; false keeps each as wide as its label
    fill: Boolean = true
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AppColors.ground)
            .padding(3.dp)
    ) {
        options.forEach { (value, label) ->
            val on = value == selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .then(if (fill) Modifier.weight(1f) else Modifier)
                    .heightIn(min = height)
                    .then(if (on) Modifier.shadow(1.dp, RoundedCornerShape(9.dp)) else Modifier)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (on) AppColors.surface else Color.Transparent)
                    .clickable(role = Role.RadioButton) { onSelect(value) }
                    .padding(horizontal = 10.dp)
            ) {
                Text(
                    label, maxLines = 1,
                    style = AppType.smallButton.copy(fontWeight = FontWeight.Medium, fontSize = 13.sp),
                    color = if (on) AppColors.textPrimary else AppColors.textMuted
                )
            }
        }
    }
}

/** A label with a value, on a tinted block; tapping it changes the value. */
@Composable
internal fun ValueRow(label: String, value: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(BodyInnerRadius))
            .background(AppColors.ground)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp)
    ) {
        Text(label, style = AppType.menuItem.copy(fontWeight = FontWeight.Normal), modifier = Modifier.weight(1f))
        Text(value, style = AppType.menuItem.copy(fontWeight = FontWeight.SemiBold))
    }
}

/** A small label over a value, on a tinted block (From / Until). */
@Composable
internal fun RowScope.LabelledValue(label: String, value: String, onClick: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 60.dp)
            .clip(RoundedCornerShape(BodyInnerRadius))
            .background(AppColors.ground)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp)
    ) {
        Text(label, style = AppType.hint)
        Text(value, style = AppType.button.copy(color = AppColors.textPrimary))
    }
}

/** A title and an optional line under it, with a switch at the end. */
@Composable
internal fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit, hint: String? = null, bold: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Switch) { onChange(!checked) }
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = if (bold) bodyTitle.copy(color = AppColors.textPrimary) else AppType.menuItem)
            hint?.let { Text(it, style = AppType.hint) }
        }
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = AppColors.accentStrength)
        )
    }
}

/** Opens the phone's time picker at [minuteOfDay]. */
@Composable
internal fun rememberTimePicker(onPicked: (Int) -> Unit): (Int) -> Unit {
    val context = LocalContext.current
    return { minuteOfDay ->
        TimePickerDialog(
            context, { _, h, m -> onPicked(h * 60 + m) },
            minuteOfDay / 60, minuteOfDay % 60, DateFormat.is24HourFormat(context)
        ).show()
    }
}

/** The weigh-in reminder: how often, the day for a weekly one, and the time. Changes apply at once. */
@Composable
internal fun WeightReminderControls(r: WeightReminder, onChange: (WeightReminder) -> Unit) {
    val locale = appLocale()
    val pickTime = rememberTimePicker { onChange(r.copy(atMinute = it)) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Segmented(
            listOf(
                WeighInReminder.OFF to stringResource(R.string.body_reminder_off),
                WeighInReminder.DAILY to stringResource(R.string.body_reminder_daily),
                WeighInReminder.WEEKLY to stringResource(R.string.body_reminder_weekly)
            ),
            r.frequency, { onChange(r.copy(frequency = it)) }, Modifier.fillMaxWidth()
        )
        if (r.frequency == WeighInReminder.WEEKLY) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                weekDays(locale).forEach { day ->
                    val on = day == r.day
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (on) AppColors.accentStrength else AppColors.ground)
                            .clickable(role = Role.RadioButton) { onChange(r.copy(day = day)) }
                            .semantics { contentDescription = day.getDisplayName(TextStyle.FULL, locale) }
                    ) {
                        Text(
                            day.getDisplayName(TextStyle.NARROW, locale),
                            style = AppType.smallButton,
                            color = if (on) Color.White else AppColors.textPrimary
                        )
                    }
                }
            }
        }
        if (r.frequency != WeighInReminder.OFF) {
            ValueRow(stringResource(R.string.body_reminder_time), clockText(r.atMinute), { pickTime(r.atMinute) })
        }
    }
}

/** Water reminders: on or off, how often, between which hours, and whether to skip when on pace. */
@Composable
internal fun WaterReminderControls(r: WaterReminder, onChange: (WaterReminder) -> Unit) {
    val pickFrom = rememberTimePicker { m -> onChange(r.copy(fromMinute = m, untilMinute = maxOf(r.untilMinute, m + 60).coerceAtMost(24 * 60 - 1))) }
    val pickUntil = rememberTimePicker { m -> onChange(r.copy(untilMinute = m, fromMinute = minOf(r.fromMinute, m - 60).coerceAtLeast(0))) }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SwitchRow(stringResource(R.string.body_water_reminder), r.on, { onChange(r.copy(on = it)) }, bold = true)
        if (r.on) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.body_water_every), style = bodyCaption)
                Segmented(
                    listOf(1, 2, 3).map { it to pluralStringResource(R.plurals.body_hours, it, it) },
                    r.everyHours, { onChange(r.copy(everyHours = it)) }, Modifier.fillMaxWidth()
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LabelledValue(stringResource(R.string.body_from), clockText(r.fromMinute)) { pickFrom(r.fromMinute) }
                LabelledValue(stringResource(R.string.body_until), clockText(r.untilMinute)) { pickUntil(r.untilMinute) }
            }
            SwitchRow(
                stringResource(R.string.body_skip_on_track), r.skipWhenOnTrack, { onChange(r.copy(skipWhenOnTrack = it)) },
                hint = stringResource(R.string.body_skip_on_track_hint)
            )
        }
    }
}

/** The week's days from the locale's first. */
internal fun weekDays(locale: Locale): List<DayOfWeek> {
    val first = java.time.temporal.WeekFields.of(locale).firstDayOfWeek
    return (0L until 7L).map { first.plus(it) }
}

/** A big number with its unit after it, on one baseline. */
@Composable
internal fun BigNumber(value: String, unit: String, color: Color, size: Int = 52, unitSize: Int = 20, unitColor: Color = color) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            value,
            style = AppType.cardTitle.copy(fontSize = size.sp, lineHeight = size.sp, letterSpacing = (-1).sp, color = color),
            modifier = Modifier.alignByBaseline()
        )
        Text(
            unit,
            style = AppType.cardTitle.copy(fontSize = unitSize.sp, fontWeight = FontWeight.Medium, color = unitColor),
            modifier = Modifier.alignByBaseline()
        )
    }
}

/** A tinted pill with text, centred. */
@Composable
internal fun Pill(text: String, background: Color, color: Color, @DrawableRes icon: Int? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        icon?.let { Icon(painterResource(it), contentDescription = null, tint = color, modifier = Modifier.size(13.dp)) }
        Text(text, style = AppType.chip.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold), color = color, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun Gap(height: Int) = Spacer(Modifier.height(height.dp))

@Composable
internal fun HGap(width: Int) = Spacer(Modifier.width(width.dp))
