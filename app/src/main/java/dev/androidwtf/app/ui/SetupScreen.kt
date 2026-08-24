package dev.androidwtf.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/**
 * Setup, in the order things actually go wrong.
 *
 * Every command has a copy button, because retyping a curl one-liner on a phone
 * keyboard is its own failure mode.
 */
@Composable
fun SetupScreen(version: String, tier: Int?, deviceSummary: String?, onRunDoctor: () -> Unit) {
    val ctx = LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 96.dp)
    ) {
        Spacer(Modifier.height(20.dp))
        Text("Setup", style = MaterialTheme.typography.headlineMedium, color = Ink)
        Spacer(Modifier.height(6.dp))
        Text(
            "androidWTF $version",
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
            fontFamily = FontFamily.Monospace,
        )

        Spacer(Modifier.height(18.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .background(Surface2, RoundedCornerShape(16.dp))
                .border(1.dp, Line, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("THIS DEVICE", style = MaterialTheme.typography.labelSmall, color = Muted)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (tier != null) "Tier $tier" else "Not probed yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (tier != null) tierColour(tier) else Muted,
                    )
                    if (deviceSummary != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(deviceSummary, style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                }
                OutlinedButton(onClick = onRunDoctor, shape = RoundedCornerShape(12.dp)) {
                    Text("Run doctor", color = Accent, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Step(
            n = 1,
            title = "Install Termux",
            body = "From F-Droid or GitHub Releases. Never the Play Store — that build " +
                "was abandoned in 2020 and its package repositories no longer exist, " +
                "so nothing will install.",
        )

        Step(
            n = 2,
            title = "Install the engine",
            body = "Paste this into Termux. It clones the catalogue and puts the wtf " +
                "command on your PATH. Safe to re-run — it updates in place.",
            command = "curl -fsSL https://raw.githubusercontent.com/naturalstate/" +
                "androidWTF/main/platforms/android/bootstrap/install.sh | bash",
            ctx = ctx,
        )

        Step(
            n = 3,
            title = "Let this app talk to Termux",
            body = "Termux ignores commands from other apps until you turn this on. " +
                "The setting lives in a text file, and Termux ships that file with " +
                "every line commented out — a line starting with # is ignored, so " +
                "allow-external-apps can look present and still be switched off. " +
                "This command appends a live line and reloads, whatever the file " +
                "currently contains.",
            command = "mkdir -p ~/.termux && echo 'allow-external-apps = true' >> " +
                "~/.termux/termux.properties && termux-reload-settings",
            ctx = ctx,
        )

        Step(
            n = 4,
            title = "Check it took",
            body = "Prints the live setting. If nothing prints, the line is still " +
                "commented out or the file is somewhere else.",
            command = "grep -v '^#' ~/.termux/termux.properties | grep external",
            ctx = ctx,
        )

        Step(
            n = 5,
            title = "Let Termux come to the front",
            body = "Android Settings → Apps → Termux → Display over other apps. " +
                "Without it, commands still run but the terminal never appears, " +
                "which looks identical to nothing happening.",
        )

        Spacer(Modifier.height(26.dp))
        Notice(
            title = "New to the terminal?",
            body = "The Help tab covers all of this in detail: which keyboard you need " +
                "to get a Ctrl key at all, how to save in nano, why a config line can " +
                "be present and still switched off, and where each kind of tool " +
                "installs to.",
            accent = Tier1,
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Step(
    n: Int,
    title: String,
    body: String,
    command: String? = null,
    ctx: Context? = null,
) {
    Spacer(Modifier.height(24.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(24.dp)
                .background(Accent.copy(alpha = 0.16f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) { Text("$n", style = MaterialTheme.typography.labelSmall, color = Accent) }
        Spacer(Modifier.width(10.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = Ink)
    }
    Spacer(Modifier.height(8.dp))
    Text(body, style = MaterialTheme.typography.bodySmall, color = Muted)
    if (command != null && ctx != null) {
        Spacer(Modifier.height(10.dp))
        CommandBlock(command, ctx)
    }
}

@Composable
fun CommandBlock(command: String, ctx: Context) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Bg, RoundedCornerShape(12.dp))
            .border(1.dp, Line, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        SelectionContainer {
            Text(
                command,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = Ink.copy(alpha = 0.9f),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row {
            Spacer(Modifier.weight(1f))
            TextButton(onClick = {
                val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("androidWTF", command))
            }) { Text("Copy", color = Accent, style = MaterialTheme.typography.labelSmall) }
        }
    }
}
