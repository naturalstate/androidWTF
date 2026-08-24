package dev.androidwtf.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.androidwtf.app.data.Catalogue
import dev.androidwtf.app.data.Selection
import dev.androidwtf.app.data.TIERS
import dev.androidwtf.app.data.Tool

@Composable
fun CatalogueScreen(cat: Catalogue, sel: Selection, deviceTier: Int?) {
    val query by sel.query
    val bundle by sel.bundle
    val maxTier by sel.maxTier
    val scriptableOnly by sel.scriptableOnly
    val shown = sel.filter(cat.tools)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { sel.query.value = it },
                placeholder = { Text("Search ${cat.tools.size} tools", color = Muted) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Muted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent, unfocusedBorderColor = Line,
                    focusedTextColor = Ink, unfocusedTextColor = Ink,
                ),
            )
        }

        // Tier filter — the axis that decides whether a tool works at all.
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    Chip("All tiers", maxTier == null) { sel.maxTier.value = null }
                }
                items(TIERS) { t ->
                    val label = if (deviceTier != null && t.n == deviceTier)
                        "T${t.n} ${t.label} · yours" else "T${t.n} ${t.label}"
                    Chip(label, maxTier == t.n) {
                        sel.maxTier.value = if (maxTier == t.n) null else t.n
                    }
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item { Chip("Auto-installable", scriptableOnly) { sel.scriptableOnly.value = !scriptableOnly } }
                item { Chip("All bundles", bundle == null) { sel.bundle.value = null } }
                items(cat.bundles) { b ->
                    Chip("${b.name} ${b.count}", bundle == b.name) {
                        sel.bundle.value = if (bundle == b.name) null else b.name
                    }
                }
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${shown.size} tools", color = Muted, style = MaterialTheme.typography.bodySmall)
                if (shown.isNotEmpty()) {
                    TextButton(onClick = { sel.addAll(shown.map { it.id }) }) {
                        Text("Select all", color = Accent, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        items(shown, key = { it.id }) { tool ->
            ToolRow(tool, sel.isPicked(tool.id), deviceTier) { sel.toggle(tool.id) }
        }

        if (shown.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    Text("Nothing matches these filters.", color = Muted)
                }
            }
        }
    }
}

@Composable
private fun ToolRow(tool: Tool, picked: Boolean, deviceTier: Int?, onToggle: () -> Unit) {
    val tooHigh = deviceTier != null && tool.tier > deviceTier
    val c = tierColour(tool.tier)
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (picked) Accent.copy(alpha = 0.09f) else Surface2, RoundedCornerShape(14.dp))
            .border(
                1.dp,
                if (picked) Accent.copy(alpha = 0.45f) else Line,
                RoundedCornerShape(14.dp),
            )
            .clickable { onToggle() }
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // The tier rail, carried over from the site as the one consistent signal.
        Box(Modifier.width(3.dp).height(40.dp).background(c, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TierBadge(tool.tier)
                Spacer(Modifier.width(8.dp))
                Text(
                    tool.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (tooHigh) Muted else Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(5.dp))
            Text(
                tool.desc,
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(7.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Label(tool.bundle, Muted)
                Label(tool.provider, if (tool.scriptable) Accent else Tier2)
                if (tool.essential) Label("essential", Accent)
                if (tooHigh) Label("needs T${tool.tier}", Danger)
            }
        }
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .size(28.dp)
                .background(if (picked) Accent else Color.Transparent, RoundedCornerShape(9.dp))
                .border(1.dp, if (picked) Accent else Line, RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (picked) Icon(Icons.Default.Check, null, tint = Color(0xFF06120B), modifier = Modifier.size(18.dp))
            else Text("+", color = Muted)
        }
    }
}

@Composable
private fun Label(text: String, colour: Color) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = colour,
        modifier = Modifier
            .background(colour.copy(alpha = 0.12f), RoundedCornerShape(5.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}
