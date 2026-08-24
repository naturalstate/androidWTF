package dev.androidwtf.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.androidwtf.app.data.Catalogue
import dev.androidwtf.app.data.Profile
import dev.androidwtf.app.data.Selection
import dev.androidwtf.app.termux.TermuxState

@Composable
fun HomeScreen(
    cat: Catalogue,
    sel: Selection,
    termux: TermuxState,
    deviceTier: Int?,
    onFixTermux: () -> Unit,
    onGrantPermission: () -> Unit,
    onRunDoctor: () -> Unit,
    onOpenProfile: (Profile) -> Unit,
    onBrowse: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 96.dp)
    ) {
        Spacer(Modifier.height(20.dp))
        Text("Your old phone is a lab", style = MaterialTheme.typography.headlineMedium, color = Ink)
        Spacer(Modifier.height(8.dp))
        Text(
            "${cat.tools.size} curated tools, each labelled with what this phone " +
                "actually needs to run it.",
            style = MaterialTheme.typography.bodyMedium, color = Muted,
        )

        Spacer(Modifier.height(18.dp))

        // Prerequisites first. Everything below is inert until these are green.
        when (termux) {
            TermuxState.NotInstalled -> Notice(
                title = "Termux is not installed",
                body = "The engine runs inside Termux. Install it from F-Droid or GitHub " +
                    "Releases — not the Play Store, whose build was abandoned in 2020 and " +
                    "whose package repositories are dead.",
                accent = Danger,
                action = "Get Termux",
                onAction = onFixTermux,
            )
            TermuxState.NoPermission -> Notice(
                title = "Permission needed",
                body = "androidWTF needs the RUN_COMMAND permission to drive Termux. You " +
                    "also need allow-external-apps=true in ~/.termux/termux.properties — " +
                    "there is no way to detect that from here, so set it if installs do nothing.",
                accent = Tier2,
                action = "Grant",
                onAction = onGrantPermission,
            )
            TermuxState.Ready -> DeviceCard(deviceTier, onRunDoctor)
        }

        SectionHeader("Packs", "Curated sets. Tap to install the whole thing.")
        cat.profiles.forEach { p ->
            ProfileRow(p, deviceTier) { onOpenProfile(p) }
            Spacer(Modifier.height(8.dp))
        }

        SectionHeader("Build your own", "Filter the catalogue and pick exactly what you want.")
        Button(
            onClick = onBrowse,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = androidx.compose.ui.graphics.Color(0xFF06120B)),
            shape = RoundedCornerShape(14.dp),
        ) { Text("Browse ${cat.tools.size} tools") }

        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            StatPill("${cat.tools.size}", "tools")
            StatPill("${cat.tools.count { it.tier == 0 }}", "no root")
            StatPill("${cat.tools.count { it.scriptable }}", "automatic")
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DeviceCard(tier: Int?, onRunDoctor: () -> Unit) {
    val c = if (tier != null) tierColour(tier) else Muted
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
                    if (tier != null) "Tier $tier" else "Tier unknown",
                    style = MaterialTheme.typography.titleLarge, color = c,
                )
            }
            OutlinedButton(onClick = onRunDoctor, shape = RoundedCornerShape(12.dp)) {
                Text("Run doctor", color = Accent, style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            if (tier != null)
                "Tools above Tier $tier are shown but marked — they will not install here."
            else
                "Run doctor in Termux to detect the tier, then reopen this screen.",
            style = MaterialTheme.typography.bodySmall, color = Muted,
        )
    }
}

@Composable
private fun ProfileRow(p: Profile, deviceTier: Int?, onClick: () -> Unit) {
    val c = tierColour(p.requiresTier)
    val reachable = deviceTier == null || deviceTier >= p.requiresTier
    Column(
        Modifier
            .fillMaxWidth()
            .background(Surface2, RoundedCornerShape(14.dp))
            .border(1.dp, Line, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TierBadge(p.requiresTier)
            Spacer(Modifier.width(8.dp))
            Text(
                p.name.removePrefix("android-"),
                style = MaterialTheme.typography.titleMedium,
                color = if (reachable) Ink else Muted,
            )
            Spacer(Modifier.weight(1f))
            if (!reachable) Text("needs T${p.requiresTier}", color = c, style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.height(6.dp))
        Text(p.description, style = MaterialTheme.typography.bodySmall, color = Muted, maxLines = 3)
    }
}
