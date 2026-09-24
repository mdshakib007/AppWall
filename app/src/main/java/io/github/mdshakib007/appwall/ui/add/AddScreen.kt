@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package io.github.mdshakib007.appwall.ui.add

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.mdshakib007.appwall.Graph
import io.github.mdshakib007.appwall.core.Domains
import io.github.mdshakib007.appwall.data.Catalog
import io.github.mdshakib007.appwall.data.InstalledApp
import io.github.mdshakib007.appwall.data.db.BlockType
import io.github.mdshakib007.appwall.ui.common.AppIcon
import io.github.mdshakib007.appwall.ui.common.BigButton
import io.github.mdshakib007.appwall.ui.common.ChoiceChip
import io.github.mdshakib007.appwall.ui.common.Pill
import io.github.mdshakib007.appwall.ui.common.PillTone
import io.github.mdshakib007.appwall.ui.common.SectionHeader
import io.github.mdshakib007.appwall.ui.common.SiteIcon
import kotlinx.coroutines.launch

@Composable
fun AddScreen(initialTab: Int, onBack: () -> Unit) {
    var tab by remember { mutableIntStateOf(initialTab.coerceIn(0, 1)) }
    val snackbar = remember { SnackbarHostState() }
    val state by Graph.engine.stateFlow.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Block something", fontWeight = FontWeight.Bold) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
                PrimaryTabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Websites") })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Apps") })
                }
            }
        },
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            if (state.focusActive) {
                Column {
                    Row(
                        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Lock, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Focus Mode is on: you can add more, but nothing can be removed.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    if (tab == 0) WebsitesTab(snackbar) else AppsTab(snackbar)
                }
            } else {
                if (tab == 0) WebsitesTab(snackbar) else AppsTab(snackbar)
            }
        }
    }
}

@Composable
private fun WebsitesTab(snackbar: SnackbarHostState) {
    val scope = rememberCoroutineScope()
    val state by Graph.engine.stateFlow.collectAsState()
    val blocked = state.itemByDomain.keys
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun add(raw: String) {
        val d = Domains.normalize(raw)
        if (d == null) { error = "That doesn't look like a website address."; return }
        error = null
        scope.launch {
            Graph.repo.add(BlockType.WEBSITE, d, d)
            snackbar.showSnackbar("Blocked $d and all its subdomains")
        }
        input = ""
    }

    fun toggle(s: Catalog.SiteSuggestion) {
        val all = s.domains.all { it in blocked }
        scope.launch {
            if (all) {
                if (state.focusActive) { snackbar.showSnackbar("Locked by Focus Mode"); return@launch }
                s.domains.forEach { d -> state.itemByDomain[d]?.let { runCatching { Graph.repo.remove(it) } } }
            } else {
                s.domains.forEach { d -> Graph.repo.add(BlockType.WEBSITE, d, d) }
            }
        }
    }

    LazyColumn(Modifier.fillMaxSize().imePadding(), contentPadding = PaddingValues(bottom = 40.dp)) {
        item {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                OutlinedTextField(
                    value = input, onValueChange = { input = it; error = null },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("facebook.com") },
                    label = { Text("Website to block") },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    trailingIcon = {
                        if (input.isNotBlank()) IconButton(onClick = { input = "" }) { Icon(Icons.Rounded.Close, "Clear") }
                    },
                    singleLine = true, isError = error != null,
                    supportingText = { Text(error ?: "Subdomains are included: blocking facebook.com also blocks m.facebook.com.") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done, capitalization = KeyboardCapitalization.None),
                    keyboardActions = KeyboardActions(onDone = { if (input.isNotBlank()) add(input) }),
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
                )
                AnimatedVisibility(input.isNotBlank()) {
                    BigButton("Block ${Domains.normalize(input) ?: input.trim()}", { add(input) }, Modifier.padding(top = 6.dp), icon = Icons.Rounded.Check)
                }
            }
        }
        Catalog.siteGroups.forEach { group ->
            item(key = group.title) {
                SectionHeader(group.title)
                FlowRow(
                    Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    group.sites.forEach { s ->
                        val on = s.domains.all { it in blocked }
                        SuggestionChip(text = "${s.emoji} ${s.name}", selected = on, onClick = { toggle(s) })
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SuggestionChip(text: String, selected: Boolean, onClick: () -> Unit) {
    ChoiceChip(text, selected, onClick, leading = if (selected) ({ Icon(Icons.Rounded.Check, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimary) }) else null)
}

@Composable
private fun AppsTab(snackbar: SnackbarHostState) {
    val scope = rememberCoroutineScope()
    val state by Graph.engine.stateFlow.collectAsState()
    var apps by remember { mutableStateOf<List<InstalledApp>?>(null) }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { apps = Graph.installedApps.all() }

    val list = apps
    if (list == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val byPkg = list.associateBy { it.packageName }
    val suggested = Catalog.timeKillerPackages.mapNotNull { byPkg[it] }
    val filtered = if (query.isBlank()) list else list.filter { it.label.contains(query, true) || it.packageName.contains(query, true) }

    fun toggle(app: InstalledApp) {
        val existing = state.itemByPackage[app.packageName]
        scope.launch {
            if (existing != null) {
                if (state.focusActive) { snackbar.showSnackbar("Locked by Focus Mode"); return@launch }
                Graph.repo.remove(existing)
            } else {
                Graph.repo.add(BlockType.APP, app.packageName, app.label)
            }
        }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 40.dp)) {
        item {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                placeholder = { Text("Search apps") }, singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                trailingIcon = { if (query.isNotBlank()) IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, "Clear") } },
                shape = MaterialTheme.shapes.medium,
            )
        }
        if (query.isBlank() && suggested.isNotEmpty()) {
            item { SectionHeader("Popular time-eaters on your phone", trailing = { Pill("${suggested.size}", tone = PillTone.PRIMARY) }) }
            items(suggested, key = { "s" + it.packageName }) { app ->
                AppRow(app, checked = app.packageName in state.itemByPackage, locked = state.focusActive) { toggle(app) }
            }
            item { SectionHeader("All apps") }
        }
        items(filtered, key = { it.packageName }) { app ->
            AppRow(app, checked = app.packageName in state.itemByPackage, locked = state.focusActive) { toggle(app) }
        }
        if (filtered.isEmpty()) item {
            Text("No apps match \"$query\".", Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AppRow(app: InstalledApp, checked: Boolean, locked: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app.packageName, 42.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(app.label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (app.isBrowser) Text("Browser", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (checked && locked) Icon(Icons.Rounded.Lock, null, Modifier.size(18.dp).padding(end = 2.dp), tint = MaterialTheme.colorScheme.primary)
        Checkbox(checked = checked, onCheckedChange = { onToggle() }, enabled = !(checked && locked))
    }
}
