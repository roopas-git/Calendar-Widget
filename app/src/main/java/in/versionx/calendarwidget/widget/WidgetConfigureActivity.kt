package `in`.versionx.calendarwidget.widget

import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import `in`.versionx.calendarwidget.R
import `in`.versionx.calendarwidget.data.CalendarEvent
import `in`.versionx.calendarwidget.data.CalendarInfo
import `in`.versionx.calendarwidget.data.CalendarRepository
import `in`.versionx.calendarwidget.data.LookaheadPreferences
import `in`.versionx.calendarwidget.ui.theme.Berry
import `in`.versionx.calendarwidget.ui.theme.Coral
import `in`.versionx.calendarwidget.ui.theme.HeaderGradient
import `in`.versionx.calendarwidget.ui.theme.SimpleCalendarWidgetTheme
import `in`.versionx.calendarwidget.ui.theme.Sky
import `in`.versionx.calendarwidget.ui.theme.Teal
import `in`.versionx.calendarwidget.work.WorkScheduler

/**
 * Per-widget-instance settings screen (the old app's "Calendar Accounts" +
 * "Background Color Setting" + Display section, see
 * docs/05-OLD-APP-REFERENCE.md), with a live preview pinned at the top and
 * one-tap color themes. Android launches this automatically after the widget is
 * placed on the home screen, since simple_calendar_widget_info.xml declares it as
 * the widget's `configure` activity; it's also reachable later via the home
 * screen's "Edit widget" action.
 */
class WidgetConfigureActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    private fun hasCalendarPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // If the user backs out without saving, the widget host should treat this as cancelled.
        setResult(Activity.RESULT_CANCELED)

        appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            SimpleCalendarWidgetTheme {
                var appearance by remember { mutableStateOf(WidgetAppearancePrefs.get(this, appWidgetId)) }
                var colorDialog by remember { mutableStateOf<ColorTarget?>(null) }
                var hasCalendarPerm by remember { mutableStateOf(hasCalendarPermission()) }
                var availableCalendars by remember { mutableStateOf<List<CalendarInfo>>(emptyList()) }
                var selectedCalendarIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
                var previewEvents by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }

                fun loadCalendars() {
                    val calendars = CalendarRepository(this).getAvailableCalendars()
                    availableCalendars = calendars
                    val stored = WidgetCalendarSelectionPrefs.get(this, appWidgetId)
                    selectedCalendarIds = stored ?: calendars.map { it.id }.toSet()
                }

                val calendarPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { granted ->
                    hasCalendarPerm = granted
                    if (granted) loadCalendars()
                }

                LaunchedEffect(hasCalendarPerm) {
                    if (hasCalendarPerm) loadCalendars()
                }

                // Keep the preview showing the user's real events for the calendars ticked right now.
                LaunchedEffect(hasCalendarPerm, selectedCalendarIds) {
                    previewEvents = if (hasCalendarPerm && selectedCalendarIds.isNotEmpty()) {
                        runCatching {
                            CalendarRepository(this@WidgetConfigureActivity)
                                .getUpcomingEvents(LookaheadPreferences.get(this@WidgetConfigureActivity), calendarIds = selectedCalendarIds)
                        }.getOrDefault(emptyList())
                    } else {
                        emptyList()
                    }
                }

                colorDialog?.let { target ->
                    ColorPickerDialog(
                        initialColor = target.get(appearance),
                        title = stringResource(target.titleRes),
                        onDismiss = { colorDialog = null },
                        onColorPicked = { picked ->
                            appearance = target.set(appearance, picked)
                            colorDialog = null
                        },
                    )
                }

                ConfigureScreen(
                    appearance = appearance,
                    previewEvents = previewEvents,
                    onAppearanceChange = { appearance = it },
                    hasCalendarPermission = hasCalendarPerm,
                    onGrantCalendarPermission = {
                        calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                    },
                    availableCalendars = availableCalendars,
                    selectedCalendarIds = selectedCalendarIds,
                    onToggleCalendar = { id, checked ->
                        selectedCalendarIds = if (checked) selectedCalendarIds + id else selectedCalendarIds - id
                    },
                    onPickColor = { colorDialog = it },
                    onSave = {
                        WidgetAppearancePrefs.set(this, appWidgetId, appearance)
                        if (availableCalendars.isNotEmpty() && selectedCalendarIds.size == availableCalendars.size) {
                            // Everything is selected: store "no filter" so newly-added calendars show up too.
                            WidgetCalendarSelectionPrefs.clear(this, appWidgetId)
                        } else {
                            WidgetCalendarSelectionPrefs.set(this, appWidgetId, selectedCalendarIds)
                        }
                        SimpleCalendarWidgetProvider.refreshWidget(this, appWidgetId)
                        // Reminders follow the widgets' calendars, so re-schedule them now.
                        WorkScheduler.scheduleDailyCheck(applicationContext)
                        WorkScheduler.runOnce(applicationContext)
                        val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        setResult(Activity.RESULT_OK, resultValue)
                        finish()
                    },
                )
            }
        }
    }
}

private enum class ColorTarget(
    val titleRes: Int,
    val subtitleRes: Int,
    val get: (WidgetAppearance) -> Int,
    val set: (WidgetAppearance, Int) -> WidgetAppearance,
) {
    BACKGROUND(R.string.config_background_color, R.string.config_background_color_subtitle, { it.backgroundColor }, { a, c -> a.copy(backgroundColor = c) }),
    GRADIENT_END(R.string.config_gradient_end_color, R.string.config_gradient_end_color_subtitle, { it.gradientEndColor }, { a, c -> a.copy(gradientEndColor = c) }),
    TEXT(R.string.config_text_color, R.string.config_text_color_subtitle, { it.textColor }, { a, c -> a.copy(textColor = c) }),
    TODAY(R.string.config_today_color, R.string.config_today_color_subtitle, { it.todayColor }, { a, c -> a.copy(todayColor = c) }),
    TOMORROW(R.string.config_tomorrow_color, R.string.config_tomorrow_color_subtitle, { it.tomorrowColor }, { a, c -> a.copy(tomorrowColor = c) }),
    DATE(R.string.config_date_color, R.string.config_date_color_subtitle, { it.dateColor }, { a, c -> a.copy(dateColor = c) }),
}

@Composable
private fun ConfigureScreen(
    appearance: WidgetAppearance,
    previewEvents: List<CalendarEvent>,
    onAppearanceChange: (WidgetAppearance) -> Unit,
    hasCalendarPermission: Boolean,
    onGrantCalendarPermission: () -> Unit,
    availableCalendars: List<CalendarInfo>,
    selectedCalendarIds: Set<Long>,
    onToggleCalendar: (Long, Boolean) -> Unit,
    onPickColor: (ColorTarget) -> Unit,
    onSave: () -> Unit,
) {
    // Surface (not just a background) so text without an explicit color follows the
    // theme — light text in dark mode, dark text in light mode.
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header + live preview stay pinned while settings scroll underneath.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HeaderGradient)
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.config_heading),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                WidgetPreview(
                    appearance = appearance,
                    events = previewEvents,
                    maxRows = 4,
                    showSamplesWhenEmpty = !hasCalendarPermission,
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                SectionHeader(stringResource(R.string.config_section_themes), Coral)
                ThemePresetRow(appearance = appearance, onAppearanceChange = onAppearanceChange)

                SectionHeader(stringResource(R.string.config_section_calendar), Berry)
                CalendarAccountsSection(
                    hasCalendarPermission = hasCalendarPermission,
                    onGrantCalendarPermission = onGrantCalendarPermission,
                    availableCalendars = availableCalendars,
                    selectedCalendarIds = selectedCalendarIds,
                    onToggleCalendar = onToggleCalendar,
                )

                SectionHeader(stringResource(R.string.config_section_colors), Sky)
                ColorPreference(ColorTarget.BACKGROUND, appearance, onPickColor)
                SwitchPreference(
                    title = stringResource(R.string.config_gradient),
                    subtitle = stringResource(R.string.config_gradient_subtitle),
                    checked = appearance.gradientEnabled,
                    onCheckedChange = { onAppearanceChange(appearance.copy(gradientEnabled = it)) },
                )
                if (appearance.gradientEnabled) {
                    ColorPreference(ColorTarget.GRADIENT_END, appearance, onPickColor)
                }
                ColorPreference(ColorTarget.TEXT, appearance, onPickColor)
                ColorPreference(ColorTarget.TODAY, appearance, onPickColor)
                ColorPreference(ColorTarget.TOMORROW, appearance, onPickColor)
                ColorPreference(ColorTarget.DATE, appearance, onPickColor)

                SectionHeader(stringResource(R.string.config_section_display), Teal)
                SliderPreference(
                    title = stringResource(R.string.config_background_opacity),
                    valueLabel = "${appearance.backgroundOpacityPercent}%",
                    value = appearance.backgroundOpacityPercent,
                    range = WidgetAppearance.MIN_OPACITY_PERCENT..WidgetAppearance.MAX_OPACITY_PERCENT,
                    onValueChange = { onAppearanceChange(appearance.copy(backgroundOpacityPercent = it)) },
                )
                SliderPreference(
                    title = stringResource(R.string.config_corner_radius),
                    valueLabel = "${appearance.cornerRadiusDp} dp",
                    value = appearance.cornerRadiusDp,
                    range = WidgetAppearance.MIN_CORNER_RADIUS_DP..WidgetAppearance.MAX_CORNER_RADIUS_DP,
                    onValueChange = { onAppearanceChange(appearance.copy(cornerRadiusDp = it)) },
                )
                SliderPreference(
                    title = stringResource(R.string.config_text_size),
                    valueLabel = "${appearance.textSizeSp} sp",
                    value = appearance.textSizeSp,
                    range = WidgetAppearance.MIN_TEXT_SIZE_SP..WidgetAppearance.MAX_TEXT_SIZE_SP,
                    onValueChange = { onAppearanceChange(appearance.copy(textSizeSp = it)) },
                )
                SwitchPreference(
                    title = stringResource(R.string.config_show_date_block),
                    subtitle = stringResource(R.string.config_show_date_block_subtitle),
                    checked = appearance.showDateBlock,
                    onCheckedChange = { onAppearanceChange(appearance.copy(showDateBlock = it)) },
                )
                SwitchPreference(
                    title = stringResource(R.string.config_calendar_colors),
                    subtitle = stringResource(R.string.config_calendar_colors_subtitle),
                    checked = appearance.showCalendarColors,
                    onCheckedChange = { onAppearanceChange(appearance.copy(showCalendarColors = it)) },
                )
                Spacer(Modifier.height(16.dp))
            }

            Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                Button(
                    onClick = onSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Color.White),
                ) {
                    Text(text = stringResource(R.string.config_save), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .background(accent.copy(alpha = 0.12f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(width = 4.dp, height = 18.dp)
                .background(accent, RoundedCornerShape(2.dp)),
        )
        Spacer(Modifier.width(10.dp))
        Text(text = title, color = accent, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ThemePresetRow(appearance: WidgetAppearance, onAppearanceChange: (WidgetAppearance) -> Unit) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        WidgetThemePreset.entries.forEach { preset ->
            val selected = preset.matches(appearance)
            val end = preset.gradientEndColor ?: preset.backgroundColor
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onAppearanceChange(preset.applyTo(appearance)) }
                    .padding(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 72.dp, height = 52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(Color(preset.backgroundColor), Color(end))))
                        .border(
                            width = if (selected) 3.dp else 1.dp,
                            color = if (selected) Coral else Color.Gray.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(12.dp).background(Color(preset.dateColor), CircleShape))
                        Box(Modifier.size(12.dp).background(Color(preset.todayColor), CircleShape))
                        Box(Modifier.size(12.dp).background(Color(preset.tomorrowColor), CircleShape))
                        Box(Modifier.size(12.dp).background(Color(preset.textColor), CircleShape))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = preset.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun CalendarAccountsSection(
    hasCalendarPermission: Boolean,
    onGrantCalendarPermission: () -> Unit,
    availableCalendars: List<CalendarInfo>,
    selectedCalendarIds: Set<Long>,
    onToggleCalendar: (Long, Boolean) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(text = stringResource(R.string.config_calendar_accounts_heading), style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(R.string.config_calendar_accounts_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (!hasCalendarPermission) {
            Spacer(Modifier.height(8.dp))
            Text(text = stringResource(R.string.config_calendar_permission_rationale), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Button(onClick = onGrantCalendarPermission, colors = ButtonDefaults.buttonColors(containerColor = Berry)) {
                Text(text = stringResource(R.string.config_grant_calendar_permission))
            }
            return
        }

        if (availableCalendars.isEmpty()) {
            Text(text = stringResource(R.string.config_no_calendars_found), style = MaterialTheme.typography.bodyMedium)
            return
        }

        availableCalendars.forEach { calendar ->
            val checked = selectedCalendarIds.contains(calendar.id)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onToggleCalendar(calendar.id, !checked) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { onToggleCalendar(calendar.id, it) },
                    colors = CheckboxDefaults.colors(checkedColor = Color(calendar.color)),
                )
                Box(Modifier.size(10.dp).background(Color(calendar.color), CircleShape))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(text = calendar.displayName, style = MaterialTheme.typography.bodyLarge)
                    if (calendar.accountName.isNotBlank() && calendar.accountName != calendar.displayName) {
                        Text(
                            text = calendar.accountName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorPreference(target: ColorTarget, appearance: WidgetAppearance, onPickColor: (ColorTarget) -> Unit) {
    PreferenceRow(
        title = stringResource(target.titleRes),
        subtitle = stringResource(target.subtitleRes),
        onClick = { onPickColor(target) },
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color(target.get(appearance)), CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape),
        )
    }
}

@Composable
private fun SwitchPreference(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    PreferenceRow(title = title, subtitle = subtitle, onClick = { onCheckedChange(!checked) }) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Coral),
        )
    }
}

@Composable
private fun PreferenceRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        trailing()
    }
}

@Composable
private fun SliderPreference(
    title: String,
    valueLabel: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.labelLarge,
                color = Teal,
                fontWeight = FontWeight.Bold,
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            colors = SliderDefaults.colors(thumbColor = Teal, activeTrackColor = Teal),
        )
    }
}
