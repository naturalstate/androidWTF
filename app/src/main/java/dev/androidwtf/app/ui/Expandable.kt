package dev.androidwtf.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/** A collapsed section. Help is long; nobody should scroll past what they don't need. */
@Composable
fun Expandable(
    title: String,
    subtitle: String? = null,
    accent: Color = Accent,
    startExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    var open by remember { mutableStateOf(startExpanded) }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .background(Surface2, RoundedCornerShape(14.dp))
            .border(1.dp, if (open) accent.copy(alpha = 0.35f) else Line, RoundedCornerShape(14.dp))
    ) {
        Row(
            Modifier.fillMaxWidth().clickable { open = !open }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = if (open) accent else Ink)
                if (subtitle != null) {
                    Spacer(Modifier.height(3.dp))
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Muted)
                }
            }
            Text(if (open) "–" else "+", style = MaterialTheme.typography.titleLarge, color = Muted)
        }
        AnimatedVisibility(open) {
            SelectionContainer {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), content = content)
            }
        }
    }
}

/** Body copy. */
@Composable
fun P(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = Muted)
    Spacer(Modifier.height(10.dp))
}

/** A labelled heading inside an expandable. */
@Composable
fun H(text: String) {
    Spacer(Modifier.height(6.dp))
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = Accent)
    Spacer(Modifier.height(6.dp))
}

/** Two-column reference row: keystroke / meaning. */
@Composable
fun KeyRow(keys: String, meaning: String) {
    Row(Modifier.fillMaxWidth().padding(bottom = 7.dp)) {
        Text(
            keys,
            Modifier.width(124.dp),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = Tier1,
        )
        Text(meaning, style = MaterialTheme.typography.bodySmall, color = Muted)
    }
}

/** Inline code, selectable via the enclosing SelectionContainer. */
@Composable
fun Code(text: String) {
    Text(
        text,
        Modifier
            .fillMaxWidth()
            .background(Bg, RoundedCornerShape(10.dp))
            .border(1.dp, Line, RoundedCornerShape(10.dp))
            .padding(11.dp),
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        color = Ink.copy(alpha = 0.9f),
    )
    Spacer(Modifier.height(10.dp))
}
