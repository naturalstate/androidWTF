package dev.androidwtf.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.androidwtf.app.data.Kind
import dev.androidwtf.app.data.Tool
import dev.androidwtf.app.data.blurb
import dev.androidwtf.app.data.label

fun kindColour(k: Kind) = when (k) {
    Kind.Cli -> Tier1
    Kind.App -> Accent
    Kind.Guide -> Muted
}

/** What a tool is, where it lands, and how to run it afterwards. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolSheet(
    tool: Tool,
    picked: Boolean,
    deviceTier: Int?,
    onToggle: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Surface1,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Muted) },
    ) {
        SelectionContainer {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp)
                    .heightIn(max = 620.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TierBadge(tool.tier)
                    Spacer(Modifier.width(8.dp))
                    KindBadge(tool.kind)
                }
                Spacer(Modifier.height(10.dp))
                Text(tool.name, style = MaterialTheme.typography.titleLarge, color = Ink)
                Spacer(Modifier.height(8.dp))
                Text(tool.desc, style = MaterialTheme.typography.bodyMedium, color = Muted)

                Spacer(Modifier.height(16.dp))
                Text(tool.kind.blurb, style = MaterialTheme.typography.bodySmall, color = kindColour(tool.kind))

                if (deviceTier != null && tool.tier > deviceTier) {
                    Spacer(Modifier.height(16.dp))
                    Notice(
                        title = "Needs Tier ${tool.tier}",
                        body = "This device reports Tier $deviceTier, so this will not install here.",
                        accent = Danger,
                    )
                }

                if (tool.kind == Kind.App) {
                    Spacer(Modifier.height(16.dp))
                    Notice(
                        title = "Installed by hand",
                        body = "Phone apps cannot be installed by the engine — Android shows " +
                            "its own install dialog for every APK, and no app can skip it. " +
                            "Selecting this and pressing Install will not do anything.",
                        accent = Tier2,
                    )
                }

                Field("CATEGORY", tool.bundle)
                Field("SOURCE", tool.provider)
                if (tool.package_.isNotBlank()) Field("PACKAGE", tool.package_, mono = true)
                tool.installsTo?.let { Field("INSTALLS TO", it, mono = true) }
                tool.howToRun?.let { Field("RUN IT WITH", it, mono = true) }
                if (tool.notes.isNotBlank()) Field("NOTES", tool.notes)

                Spacer(Modifier.height(22.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (tool.kind != Kind.App && tool.kind != Kind.Guide) {
                        Button(
                            onClick = { onToggle(); onDismiss() },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (picked) Line else Accent,
                                contentColor = if (picked) Ink else Color(0xFF06120B),
                            ),
                        ) { Text(if (picked) "Remove from selection" else "Add to selection") }
                    }
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                        Text("Close", color = Muted)
                    }
                }
            }
        }
    }
}

@Composable
fun KindBadge(kind: Kind, modifier: Modifier = Modifier) {
    val c = kindColour(kind)
    Surface(
        modifier = modifier,
        color = c.copy(alpha = 0.16f),
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, c.copy(alpha = 0.35f)),
    ) {
        Text(
            kind.label,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = c,
        )
    }
}

@Composable
private fun Field(label: String, value: String, mono: Boolean = false) {
    Spacer(Modifier.height(16.dp))
    Text(label, style = MaterialTheme.typography.labelSmall, color = Muted)
    Spacer(Modifier.height(4.dp))
    Text(
        value,
        style = MaterialTheme.typography.bodySmall,
        color = Ink.copy(alpha = 0.88f),
        fontFamily = if (mono) FontFamily.Monospace else null,
    )
}
