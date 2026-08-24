package dev.androidwtf.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.androidwtf.app.termux.RunResult

/**
 * What happened. Shown after every command, including the ones that fail before
 * Termux runs anything — those used to be invisible.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultSheet(result: RunResult, onDismiss: () -> Unit, onOpenTermux: () -> Unit) {
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
                .heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
        ) {
            val accent = when {
                result.pending -> Tier1
                result.ok -> Accent
                else -> Danger
            }
            Text(
                when {
                    result.pending -> "Running ${result.label}…"
                    result.ok -> "${result.label.replaceFirstChar { it.uppercase() }} finished"
                    else -> "${result.label.replaceFirstChar { it.uppercase() }} failed"
                },
                style = MaterialTheme.typography.titleLarge,
                color = accent,
            )
            Spacer(Modifier.height(8.dp))
            Text(result.diagnosis, style = MaterialTheme.typography.bodyMedium, color = Ink.copy(alpha = 0.85f))

            if (result.pending) {
                Spacer(Modifier.height(18.dp))
                LinearProgressIndicator(Modifier.fillMaxWidth(), color = Accent, trackColor = Line)
            }

            // A failure is exactly when the prerequisites are worth restating.
            if (!result.pending && !result.ok) {
                Spacer(Modifier.height(20.dp))
                SetupChecklist()
            }

            val body = listOf(result.stdout, result.stderr).filter { it.isNotBlank() }.joinToString("\n")
            if (body.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                Text("OUTPUT", style = MaterialTheme.typography.labelSmall, color = Muted)
                Spacer(Modifier.height(6.dp))
                Surface(
                    color = Bg,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Line),
                ) {
                    Text(
                        body.take(4000),
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = Muted,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onOpenTermux, shape = RoundedCornerShape(12.dp)) {
                    Text("Open Termux", color = Accent)
                }
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Accent,
                        contentColor = androidx.compose.ui.graphics.Color(0xFF06120B),
                    ),
                ) { Text("Done") }
            }
        }
    }
}

/** Prerequisites the app cannot detect, listed where a failure sends you. */
@Composable
fun SetupChecklist() {
    Column {
        Text("IF NOTHING HAPPENS", style = MaterialTheme.typography.labelSmall, color = Muted)
        Spacer(Modifier.height(8.dp))
        listOf(
            "1  Run the bootstrap in Termux so the engine exists",
            "2  Set allow-external-apps = true in ~/.termux/termux.properties, " +
                "then termux-reload-settings",
            "3  Grant Termux \"Display over other apps\" in Android Settings, or " +
                "commands run without the terminal ever appearing",
        ).forEach {
            Text(it, style = MaterialTheme.typography.bodySmall, color = Muted)
            Spacer(Modifier.height(6.dp))
        }
    }
}
