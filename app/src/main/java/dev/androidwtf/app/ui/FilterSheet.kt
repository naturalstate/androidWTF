package dev.androidwtf.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.androidwtf.app.data.Bundle
import dev.androidwtf.app.data.Selection
import dev.androidwtf.app.data.TIERS

/**
 * All filtering in one place.
 *
 * The first version put two chip rows above the list and mixed their semantics:
 * "Auto-installable" was a toggle, "All bundles" was a reset, and twenty bundle
 * names scrolled off the right edge. Nothing said which were exclusive, which
 * combined, or what most of them meant. A sheet with labelled sections and a
 * count on the trigger is both smaller on screen and easier to reason about.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(
    sel: Selection,
    bundles: List<Bundle>,
    deviceTier: Int?,
    onDismiss: () -> Unit,
) {
    val maxTier by sel.maxTier
    val bundle by sel.bundle
    val scriptableOnly by sel.scriptableOnly

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Surface1,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Muted) },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .heightIn(max = 620.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Filters", style = MaterialTheme.typography.titleLarge, color = Ink)
                Spacer(Modifier.weight(1f))
                if (sel.activeFilterCount() > 0) {
                    TextButton(onClick = { sel.clearFilters() }) { Text("Reset", color = Accent) }
                }
            }

            FilterGroup(
                title = "What this phone can run",
                hint = "Hides tools that need more than the tier you pick.",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RadioRow("Everything", maxTier == null) { sel.maxTier.value = null }
                    TIERS.forEach { t ->
                        RadioRow(
                            label = "T${t.n} ${t.label}",
                            selected = maxTier == t.n,
                            note = if (deviceTier == t.n) "this device" else t.needs,
                            accent = tierColour(t.n),
                        ) { sel.maxTier.value = t.n }
                    }
                }
            }

            FilterGroup(
                title = "How it installs",
                hint = "Automatic tools are installed for you. The rest are Play Store, " +
                    "NetHunter Store or a documented workflow, and need a tap each.",
            ) {
                SwitchRow("Automatic only", scriptableOnly) { sel.scriptableOnly.value = it }
            }

            FilterGroup(title = "Category", hint = null) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RadioRow("All categories", bundle == null) { sel.bundle.value = null }
                    bundles.forEach { b ->
                        RadioRow(
                            label = b.name.replaceFirstChar { it.uppercase() },
                            selected = bundle == b.name,
                            note = "${b.count}",
                        ) { sel.bundle.value = b.name }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = androidx.compose.ui.graphics.Color(0xFF06120B),
                ),
            ) { Text("Show results") }
        }
    }
}

@Composable
private fun FilterGroup(title: String, hint: String?, content: @Composable () -> Unit) {
    Spacer(Modifier.height(22.dp))
    Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, color = Accent)
    if (hint != null) {
        Spacer(Modifier.height(4.dp))
        Text(hint, style = MaterialTheme.typography.bodySmall, color = Muted)
    }
    Spacer(Modifier.height(10.dp))
    content()
}

@Composable
private fun RadioRow(
    label: String,
    selected: Boolean,
    note: String? = null,
    accent: androidx.compose.ui.graphics.Color = Accent,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = accent, unselectedColor = Muted),
        )
        Text(label, style = MaterialTheme.typography.bodyMedium, color = if (selected) Ink else Muted)
        if (note != null) {
            Spacer(Modifier.weight(1f))
            Text(note, style = MaterialTheme.typography.bodySmall, color = Muted)
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Ink)
        Spacer(Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = androidx.compose.ui.graphics.Color(0xFF06120B),
                checkedTrackColor = Accent,
                uncheckedTrackColor = Line,
                uncheckedBorderColor = Line,
            ),
        )
    }
}
