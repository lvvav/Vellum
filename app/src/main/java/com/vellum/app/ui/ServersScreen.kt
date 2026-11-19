package com.vellum.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vellum.app.AppState

@Composable
fun ServersScreen(s: AppState, onRefresh: () -> Unit) {
    val spin by rememberInfiniteTransition(label = "refresh").animateFloat(
        0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart), label = "spin"
    )

    Column(Modifier.fillMaxSize().padding(horizontal = 22.dp)) {
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Серверы", style = Type.Display)
                Text(
                    if (s.refreshing) "Проверяем пинг…" else "Живых: ${s.alive} из ${s.servers.size}",
                    style = Type.Caption, modifier = Modifier.padding(top = 2.dp)
                )
            }
            Box(
                Modifier.size(48.dp).pressable(onRefresh).glass(CircleShape, 1.3f),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Refresh, "Обновить", tint = Palette.Text,
                    modifier = Modifier.size(24.dp).rotate(if (s.refreshing) spin else 0f)
                )
            }
        }
        Spacer(Modifier.height(18.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            itemsIndexed(s.servers, key = { _, it -> it.name }) { i, server ->
                val on = i == s.selected
                val tint by animateColorAsState(
                    if (on) Palette.Good.copy(alpha = 0.18f) else Color.Transparent, tween(300), label = "sel"
                )
                Row(
                    Modifier.fillMaxWidth()
                        .pressable { if (server.ping != null) s.selected = i }
                        .glass(RoundedCornerShape(22.dp), if (on) 1.5f else 1f)
                        .then(Modifier.padding(0.dp))
                        .let { it },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(44.dp).glass(CircleShape, 1.2f), contentAlignment = Alignment.Center) {
                            Text(server.flag, style = Type.Title)
                        }
                        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                            Text(server.name, style = Type.Title)
                            Text(
                                if (server.ping == null) "Не отвечает" else if (on) "Выбран" else "Доступен",
                                style = Type.Caption
                            )
                        }
                        if (on) {
                            Icon(Icons.Rounded.Check, null, tint = Palette.Good, modifier = Modifier.padding(end = 10.dp).size(22.dp))
                        }
                        PingChip(server.ping)
                    }
                }
            }
        }
    }
}
