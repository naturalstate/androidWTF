package dev.androidwtf.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TierBadge(tier: Int, modifier: Modifier = Modifier) {
    val c = tierColour(tier)
    Box(
        modifier
            .background(c.copy(alpha = 0.16f), RoundedCornerShape(6.dp))
            .border(1.dp, c.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text("T$tier", color = c, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.bodySmall) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            labelColor = Muted,
            selectedContainerColor = Accent,
            selectedLabelColor = Color(0xFF06120B),
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true, selected = selected,
            borderColor = Line, selectedBorderColor = Accent,
        ),
    )
}

/** A callout for something the user must act on before anything will work. */
@Composable
fun Notice(
    title: String,
    body: String,
    accent: Color = Tier2,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(accent.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .border(1.dp, accent.copy(alpha = 0.30f), RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Text(title, color = accent, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(body, color = Ink.copy(alpha = 0.82f), style = MaterialTheme.typography.bodySmall)
        if (action != null && onAction != null) {
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color(0xFF06120B)),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
            ) { Text(action, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String? = null) {
    Column(Modifier.padding(top = 22.dp, bottom = 10.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = Ink)
        if (subtitle != null) {
            Spacer(Modifier.height(3.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Muted)
        }
    }
}

@Composable
fun StatPill(value: String, label: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = Ink)
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = Muted)
    }
}
