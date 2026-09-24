@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package io.github.mdshakib007.appwall.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.mdshakib007.appwall.Graph
import io.github.mdshakib007.appwall.core.BlockRules
import io.github.mdshakib007.appwall.core.Savings
import io.github.mdshakib007.appwall.data.db.BlockItem
import io.github.mdshakib007.appwall.data.db.BlockType
import io.github.mdshakib007.appwall.ui.common.AppIcon
import io.github.mdshakib007.appwall.ui.common.BigButton
import io.github.mdshakib007.appwall.ui.common.ChoiceChip
import io.github.mdshakib007.appwall.ui.common.Format
import io.github.mdshakib007.appwall.ui.common.IconTile
import io.github.mdshakib007.appwall.ui.common.Pill
import io.github.mdshakib007.appwall.ui.common.PillTone
import io.github.mdshakib007.appwall.ui.common.SiteIcon
import io.github.mdshakib007.appwall.ui.common.StatTile
import io.github.mdshakib007.appwall.ui.common.SurfaceCard
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

private class DurationChoice(val label: String, val ms: Long?) // null = forever, -1 = custom

private val durations = listOf(
    DurationChoice("Forever", null),
    DurationChoice("1 hour", 3600_000L),
    DurationChoice("3 hours", 3 * 3600_000L),
    DurationChoice("8 hours", 8 * 3600_000L),
    DurationChoice("1 day", 24 * 3600_000L),
    DurationChoice("3 days", 3 * 24 * 3600_000L),
    DurationChoice("1 week", 7 * 24 * 3600_000L),
    DurationChoice("1 month", 30 * 24 * 3600_000L),
    DurationChoice("3 months", 90 * 24 * 3600_000L),
    DurationChoice("1 year", 365 * 24 * 3600_000L),
    DurationChoice("Pick a date", -1L),
)

@Composable
fun DetailScreen(id: Long, onBack: () -> Unit) {
    val item by Graph.repo.item(id).collectAsState(initial = null)
    val state by Graph.engine.stateFlow.collectAsState()
    val attempts by Graph.repo.attemptCountFor(item?.type ?: BlockType.APP, item?.key ?: "").collectAsState(initial = 0)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val now = System.currentTimeMillis()
    val it = item

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(it?.displayName ?: "", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { inner ->
        if (it == null) { Box(Modifier.fillMaxSize().padding(inner)); return@Scaffold }
        val locked = state.focusActive
        val blockedNow = BlockRules.isBlocked(it, state.focus, now)

        // Editable draft
        var untilChoice by remember(it.id) { mutableIntStateOf(initialChoice(it, now)) }
        var customUntil by remember(it.id) { mutableStateOf(it.untilAt) }
        var allDay by remember(it.id) { mutableStateOf(it.allDay) }
        var daysMask by remember(it.id) { mutableIntStateOf(it.daysMask) }
        var start by remember(it.id) { mutableIntStateOf(it.startMinute) }
        var end by remember(it.id) { mutableIntStateOf(it.endMinute) }
        var showDate by remember { mutableStateOf(false) }
        var showStart by remember { mutableStateOf(false) }
        var showEnd by remember { mutableStateOf(false) }
        var confirmRemove by remember { mutableStateOf(false) }

        val computedUntil: Long? = when (val ms = durations[untilChoice].ms) {
            null -> null
            -1L -> customUntil
            else -> now + ms
        }
        val dirty = computedUntil?.let { u -> (it.untilAt ?: 0L) / 60_000 != u / 60_000 } ?: (it.untilAt != null) ||
            allDay != it.allDay || daysMask != it.daysMask || start != it.startMinute || end != it.endMinute

        Column(Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                if (it.type == BlockType.APP) AppIcon(it.key, 64.dp) else SiteIcon(64.dp)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(it.displayName, style = MaterialTheme.typography.headlineSmall)
                    Text(if (it.type == BlockType.APP) it.key else "Website · includes all subdomains", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Pill(
                        when {
                            locked -> "Locked by Focus Mode"
                            blockedNow -> "Blocked right now"
                            BlockRules.hasEnded(it, now) -> "Ended"
                            else -> "Not active right now"
                        },
                        tone = if (locked) PillTone.PRIMARY else if (blockedNow) PillTone.SUCCESS else PillTone.NEUTRAL,
                        icon = if (locked) Icons.Rounded.Lock else null,
                    )
                }
            }

            if (it.type == BlockType.WEBSITE) {
                Text(
                    "All subdomains are blocked too (m.${it.key}, www.${it.key}, …).",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            if (locked) {
                SurfaceCard(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconTile(Icons.Rounded.Lock, size = 36.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Focus Mode is on until ${Format.dateTime(state.focus!!.endAt)}. Schedules can't be changed and nothing can be removed until then.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Stats
            Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("$attempts", "times blocked", Modifier.weight(1f))
                StatTile(Format.duration(Savings.forItem(it, attempts, now), compact = true), "time you got back", Modifier.weight(1f), accent = true)
            }
            if (it.baselineDailyMs > 0) {
                Text(
                    "Before blocking, you used this about ${Format.duration(it.baselineDailyMs)} a day.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Duration
            Text("How long", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                durations.forEachIndexed { i, d ->
                    Chip(d.label, selected = untilChoice == i, enabled = !locked) {
                        if (d.ms == -1L) showDate = true else untilChoice = i
                    }
                }
            }
            computedUntil?.let { u ->
                Text(
                    "Ends ${Format.dateTime(u)}", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp),
                )
            }

            // Active hours
            Row(Modifier.padding(top = 24.dp, bottom = 4.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("All day", style = MaterialTheme.typography.titleMedium)
                    Text("Turn off to block only during certain hours", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = allDay, onCheckedChange = { allDay = it }, enabled = !locked)
            }
            if (!allDay) {
                Text("From – to", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TimeBox(Format.minuteOfDay(start), Modifier.weight(1f), enabled = !locked) { showStart = true }
                    TimeBox(Format.minuteOfDay(end), Modifier.weight(1f), enabled = !locked) { showEnd = true }
                }
                if (end <= start) Text(
                    "Ends the next day", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp),
                )
            }
            Text("On these days", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DayOfWeek.entries.forEach { day ->
                    val bit = 1 shl (day.value - 1)
                    val on = daysMask and bit != 0
                    Box(
                        Modifier.weight(1f).height(40.dp)
                            .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLowest, CircleShape)
                            .border(1.dp, if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            .clickable(enabled = !locked) { daysMask = daysMask xor bit },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            day.getDisplayName(TextStyle.NARROW, Locale.getDefault()), style = MaterialTheme.typography.labelLarge,
                            color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Every day", daysMask == BlockItem.ALL_DAYS, !locked) { daysMask = BlockItem.ALL_DAYS }
                Chip("Weekdays", daysMask == BlockItem.WEEKDAYS, !locked) { daysMask = BlockItem.WEEKDAYS }
                Chip("Weekends", daysMask == BlockItem.WEEKEND, !locked) { daysMask = BlockItem.WEEKEND }
            }

            Spacer(Modifier.height(28.dp))
            BigButton(
                "Save schedule", enabled = !locked && dirty && daysMask != 0,
                onClick = {
                    scope.launch {
                        runCatching {
                            Graph.repo.updateSchedule(it.copy(untilAt = computedUntil, allDay = allDay, daysMask = daysMask, startMinute = start, endMinute = end))
                        }.onSuccess { snackbar.showSnackbar("Saved") }.onFailure { e -> snackbar.showSnackbar(e.message ?: "Couldn't save") }
                    }
                },
            )
            TextButton(
                onClick = { confirmRemove = true }, enabled = !locked,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 32.dp),
            ) { Text("Remove from blocklist", color = if (locked) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.error) }
        }

        if (showDate) {
            val dp = rememberDatePickerState(initialSelectedDateMillis = customUntil ?: (now + 7 * 24 * 3600_000L))
            DatePickerDialog(
                onDismissRequest = { showDate = false },
                confirmButton = {
                    TextButton(onClick = {
                        dp.selectedDateMillis?.let { sel ->
                            // Picker returns UTC midnight; block until 23:59 local of that date.
                            val date = Instant.ofEpochMilli(sel).atZone(ZoneId.of("UTC")).toLocalDate()
                            customUntil = date.atTime(LocalTime.of(23, 59)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                            untilChoice = durations.lastIndex
                        }
                        showDate = false
                    }) { Text("OK") }
                },
                dismissButton = { TextButton(onClick = { showDate = false }) { Text("Cancel") } },
            ) { DatePicker(dp) }
        }
        if (showStart) TimeDialog(start, { start = it; showStart = false }, { showStart = false })
        if (showEnd) TimeDialog(end, { end = it; showEnd = false }, { showEnd = false })
        if (confirmRemove) {
            AlertDialog(
                onDismissRequest = { confirmRemove = false },
                title = { Text("Remove ${it.displayName}?") },
                text = { Text("It will no longer be blocked. You can always add it back.") },
                confirmButton = {
                    TextButton(onClick = {
                        confirmRemove = false
                        scope.launch { runCatching { Graph.repo.remove(it) }.onSuccess { onBack() } }
                    }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text("Keep") } },
            )
        }
    }
}

private fun initialChoice(item: BlockItem, now: Long): Int = if (item.untilAt == null) 0 else durations.lastIndex

@Composable
private fun Chip(text: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    ChoiceChip(text, selected, onClick, enabled = enabled)
}

@Composable
private fun TimeBox(text: String, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier.background(MaterialTheme.colorScheme.surfaceContainerLowest, MaterialTheme.shapes.medium)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
            .clickable(enabled = enabled, onClick = onClick).padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center) }
}

@Composable
private fun TimeDialog(minute: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val tp = rememberTimePickerState(initialHour = (minute / 60) % 24, initialMinute = minute % 60)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onPick(tp.hour * 60 + tp.minute) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = { TimePicker(tp) },
    )
}
