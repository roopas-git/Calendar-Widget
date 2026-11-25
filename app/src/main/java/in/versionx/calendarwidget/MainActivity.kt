package `in`.versionx.calendarwidget

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import `in`.versionx.calendarwidget.data.CalendarEvent
import `in`.versionx.calendarwidget.data.CalendarRepository
import `in`.versionx.calendarwidget.data.LookaheadMode
import `in`.versionx.calendarwidget.data.LookaheadPreferences
import `in`.versionx.calendarwidget.data.LookaheadSettings
import `in`.versionx.calendarwidget.ui.theme.Amber
import `in`.versionx.calendarwidget.ui.theme.Berry
import `in`.versionx.calendarwidget.ui.theme.Coral
import `in`.versionx.calendarwidget.ui.theme.HeaderGradient
import `in`.versionx.calendarwidget.ui.theme.SimpleCalendarWidgetTheme
import `in`.versionx.calendarwidget.ui.theme.Sky
import `in`.versionx.calendarwidget.widget.EventLineFormatter
import `in`.versionx.calendarwidget.widget.WidgetCalendarSelectionPrefs
import `in`.versionx.calendarwidget.work.WorkScheduler
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : ComponentActivity() {

    private fun hasCalendarPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SimpleCalendarWidgetTheme {
                var hasCalendarPerm by remember { mutableStateOf(hasCalendarPermission()) }
                var hasNotifPerm by remember { mutableStateOf(hasNotificationPermission()) }
                var events by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }
                var lookaheadSettings by remember { mutableStateOf(LookaheadPreferences.get(this)) }

                var calendarFilter by remember { mutableStateOf<Set<Long>?>(null) }

                fun refreshEvents() {
                    if (hasCalendarPerm) {
                        // Same calendars as the user's widgets (all calendars if none placed).
                        calendarFilter = WidgetCalendarSelectionPrefs.appCalendarFilter(this)
                        events = CalendarRepository(this).getUpcomingEvents(lookaheadSettings, calendarIds = calendarFilter)
                        WorkScheduler.scheduleDailyCheck(this)
                    }
                }

                val notifLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { granted ->
                    hasNotifPerm = granted
                }

                val calendarLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { granted ->
                    hasCalendarPerm = granted
                    if (granted) {
                        refreshEvents()
                        if (!hasNotifPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                ) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding).navigationBarsPadding(),
                        hasCalendarPermission = hasCalendarPerm,
                        events = events,
                        filteredByWidgets = calendarFilter != null,
                        lookaheadSettings = lookaheadSettings,
                        onLookaheadSettingsChange = { newSettings ->
                            lookaheadSettings = newSettings
                            LookaheadPreferences.set(this, newSettings)
                            refreshEvents()
                            WorkScheduler.runOnce(this)
                        },
                        onGrantPermissions = {
                            calendarLauncher.launch(Manifest.permission.READ_CALENDAR)
                        },
                        onCheckNow = {
                            refreshEvents()
                            WorkScheduler.runOnce(this)
                        },
                    )
                }

                // Pick up events as soon as permission is already granted on launch.
                LaunchedEffect(hasCalendarPerm) {
                    if (hasCalendarPerm) refreshEvents()
                }
            }
        }
    }

    companion object {
        fun newIntent(context: Context): Intent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    hasCalendarPermission: Boolean,
    events: List<CalendarEvent>,
    filteredByWidgets: Boolean,
    lookaheadSettings: LookaheadSettings,
    onLookaheadSettingsChange: (LookaheadSettings) -> Unit,
    onGrantPermissions: () -> Unit,
    onCheckNow: () -> Unit,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { TodayHeader() }

        if (!hasCalendarPermission) {
            item {
                ColorCard(accent = Berry) {
                    Text(text = stringResource(R.string.permission_rationale_calendar), style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onGrantPermissions, colors = ButtonDefaults.buttonColors(containerColor = Berry)) {
                        Text(text = stringResource(R.string.grant_permissions))
                    }
                }
            }
            return@LazyColumn
        }

        item {
            ColorCard(accent = Coral) {
                LookaheadSettingsPanel(settings = lookaheadSettings, onSettingsChange = onLookaheadSettingsChange)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onCheckNow,
                    colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Color.White),
                ) {
                    Text(text = stringResource(R.string.check_now), fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            ColorCard(accent = Sky) {
                Text(text = stringResource(R.string.add_widget_hint_title), style = MaterialTheme.typography.titleMedium, color = Sky)
                Text(
                    text = stringResource(R.string.add_widget_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Text(
                text = stringResource(R.string.upcoming_events_heading),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            if (filteredByWidgets) {
                Text(
                    text = stringResource(R.string.events_filtered_by_widgets),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        if (events.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.widget_no_upcoming_events),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(events, key = { it.instanceKey }) { event -> EventCard(event) }
        }
    }
}

/** Big amber date block over the header gradient, echoing the widget's date block. */
@Composable
private fun TodayHeader() {
    val today = remember { Calendar.getInstance() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderGradient)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(SimpleDateFormat("MMM", Locale.getDefault()).format(today.time).uppercase(), color = Amber, fontWeight = FontWeight.Bold)
            Text(today.get(Calendar.DAY_OF_MONTH).toString(), color = Amber, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 38.sp)
            Text(SimpleDateFormat("EEE", Locale.getDefault()).format(today.time).uppercase(), color = Amber, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                text = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(today.time),
                color = Color.White.copy(alpha = 0.9f),
            )
        }
    }
}

@Composable
private fun ColorCard(accent: Color, content: @Composable ColumnScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(accent.copy(alpha = 0.10f))
            .height(IntrinsicSize.Min),
    ) {
        Box(
            Modifier
                .width(6.dp)
                .fillMaxHeight()
                .background(accent),
        )
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun EventCard(event: CalendarEvent) {
    val parts = remember(event) { EventLineFormatter.parts(event) }
    val isToday = remember(event) { EventLineFormatter.isToday(event) }
    val eventColor = Color(event.color)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, eventColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(6.dp)
                .fillMaxHeight()
                .background(eventColor),
        )
        Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                text = parts.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = event.calendarDisplayName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = parts.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (isToday) Color.Black else eventColor,
            modifier = Modifier
                .padding(end = 12.dp)
                .clip(RoundedCornerShape(50))
                .background(if (isToday) Amber else eventColor.copy(alpha = 0.15f))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun LookaheadSettingsPanel(
    settings: LookaheadSettings,
    onSettingsChange: (LookaheadSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.lookahead_heading),
            style = MaterialTheme.typography.titleMedium,
            color = Coral,
            fontWeight = FontWeight.Bold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = settings.mode == LookaheadMode.DAYS,
                onClick = { onSettingsChange(settings.copy(mode = LookaheadMode.DAYS)) },
                label = { Text(stringResource(R.string.lookahead_mode_days)) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Coral, selectedLabelColor = Color.White),
            )
            FilterChip(
                selected = settings.mode == LookaheadMode.COUNT,
                onClick = { onSettingsChange(settings.copy(mode = LookaheadMode.COUNT)) },
                label = { Text(stringResource(R.string.lookahead_mode_count)) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Coral, selectedLabelColor = Color.White),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedButton(onClick = {
                val newValue = (settings.value - 1).coerceAtLeast(LookaheadSettings.MIN_VALUE)
                onSettingsChange(settings.copy(value = newValue))
            }) {
                Text(text = "−")
            }
            val valueLabel = when (settings.mode) {
                LookaheadMode.DAYS -> stringResource(R.string.lookahead_days_value, settings.value)
                LookaheadMode.COUNT -> stringResource(R.string.lookahead_count_value, settings.value)
            }
            Text(text = valueLabel, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            OutlinedButton(onClick = {
                val newValue = (settings.value + 1).coerceAtMost(LookaheadSettings.MAX_VALUE)
                onSettingsChange(settings.copy(value = newValue))
            }) {
                Text(text = "+")
            }
        }
    }
}
