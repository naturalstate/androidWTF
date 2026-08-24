package dev.androidwtf.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.androidwtf.app.data.Catalogue
import dev.androidwtf.app.data.Selection
import dev.androidwtf.app.termux.Termux
import dev.androidwtf.app.termux.TermuxState
import dev.androidwtf.app.ui.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val catalogue = Catalogue.load(this)
        setContent { AndroidWtfTheme { App(catalogue) } }
    }
}

private enum class Tab { Home, Catalogue }

@Composable
private fun App(cat: Catalogue) {
    val ctx = LocalContext.current
    val sel = remember { Selection() }
    var tab by remember { mutableStateOf(Tab.Home) }
    var termux by remember { mutableStateOf(Termux.state(ctx)) }

    // The tier comes from the engine, which we cannot query synchronously from
    // here — RUN_COMMAND is fire-and-forget unless we register a PendingIntent
    // receiver. MVP: unknown until the user runs doctor. See the note below.
    val deviceTier: Int? = null

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { termux = Termux.state(ctx) }

    Scaffold(
        containerColor = Bg,
        bottomBar = {
            NavigationBar(containerColor = Surface1, tonalElevation = 0.dp) {
                NavigationBarItem(
                    selected = tab == Tab.Home,
                    onClick = { tab = Tab.Home },
                    icon = { Icon(Icons.Default.Home, null) },
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Accent, selectedTextColor = Accent,
                        unselectedIconColor = Muted, unselectedTextColor = Muted,
                        indicatorColor = Accent.copy(alpha = 0.14f),
                    ),
                )
                NavigationBarItem(
                    selected = tab == Tab.Catalogue,
                    onClick = { tab = Tab.Catalogue },
                    icon = { Icon(Icons.Default.Apps, null) },
                    label = { Text("Tools") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Accent, selectedTextColor = Accent,
                        unselectedIconColor = Muted, unselectedTextColor = Muted,
                        indicatorColor = Accent.copy(alpha = 0.14f),
                    ),
                )
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                Tab.Home -> HomeScreen(
                    cat = cat,
                    sel = sel,
                    termux = termux,
                    deviceTier = deviceTier,
                    onFixTermux = { Termux.openFDroidTermux(ctx) },
                    onGrantPermission = { permissionLauncher.launch(Termux.requiredPermission()) },
                    onRunDoctor = { Termux.doctor(ctx) },
                    onOpenProfile = { p -> Termux.installProfile(ctx, p.name, dryRun = true) },
                    onBrowse = { tab = Tab.Catalogue },
                )
                Tab.Catalogue -> CatalogueScreen(cat, sel, deviceTier)
            }

            if (sel.picked.isNotEmpty()) {
                InstallBar(
                    count = sel.picked.size,
                    enabled = termux == TermuxState.Ready,
                    onClear = { sel.clear() },
                    onPreview = { Termux.install(ctx, sel.picked.toList(), dryRun = true) },
                    onInstall = { Termux.install(ctx, sel.picked.toList()) },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

/** The selection tray. Preview runs --dry-run so nothing happens by accident. */
@Composable
private fun InstallBar(
    count: Int,
    enabled: Boolean,
    onClear: () -> Unit,
    onPreview: () -> Unit,
    onInstall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(12.dp),
        shape = RoundedCornerShape(18.dp),
        color = Surface2,
        border = androidx.compose.foundation.BorderStroke(1.dp, Line),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("$count selected", color = Ink, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onClear) { Text("Clear", color = Muted) }
            OutlinedButton(onClick = onPreview, enabled = enabled, shape = RoundedCornerShape(12.dp)) {
                Text("Preview", color = if (enabled) Accent else Muted)
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onInstall,
                enabled = enabled,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color(0xFF06120B)),
            ) { Text("Install") }
        }
    }
}
