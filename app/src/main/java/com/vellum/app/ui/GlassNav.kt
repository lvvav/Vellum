package com.vellum.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val items = listOf(
    Icons.Rounded.Shield to "Главная",
    Icons.Rounded.Public to "Серверы",
    Icons.Rounded.Tune to "Настройки",
)

@Composable
fun GlassNav(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.glass(RoundedCornerShape(34.dp), 1.6f).padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { i, (icon, label) ->
            val on = i == selected
            val bg by animateColorAsState(if (on) Color.White.copy(alpha = 0.22f) else Color.Transparent, tween(300), label = "nav")
            Row(
                Modifier.clip(RoundedCornerShape(28.dp)).background(bg)
                    .pressable { onSelect(i) }
                    .padding(horizontal = 18.dp, vertical = 12.dp)
                    .animateContentSize(spring(dampingRatio = 0.8f, stiffness = 400f)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, label, tint = if (on) Palette.Text else Palette.Muted, modifier = Modifier.size(24.dp))
                AnimatedVisibility(on, enter = fadeIn() + expandHorizontally(), exit = fadeOut() + shrinkHorizontally()) {
                    Text(label, style = Type.Label, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}
