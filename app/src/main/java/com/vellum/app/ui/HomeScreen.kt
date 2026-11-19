package com.vellum.app.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vellum.app.AppState
import com.vellum.app.Conn
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(s: AppState, onToggle: () -> Unit) {
    LaunchedEffect(s.conn) {
        while (s.conn == Conn.Connected) {
            delay(1000)
            s.seconds++
        }
    }
    val server = s.servers.getOrNull(s.selected)

    Column(Modifier.fillMaxSize().padding(horizontal = 22.dp)) {
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Vellum", style = Type.Display, modifier = Modifier.weight(1f))
            Row(
                Modifier.glass(RoundedCornerShape(50), 1f).padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(if (s.alive > 0) Palette.Good else Palette.Bad))
                Text("${s.alive} серв.", style = Type.Label, modifier = Modifier.padding(start = 8.dp))
            }
        }

        Spacer(Modifier.weight(1f))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            PowerButton(s.conn, s.smoothMotion, onToggle)
        }
        Spacer(Modifier.height(18.dp))

        Crossfade(s.conn, animationSpec = tween(400), label = "status") { c ->
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    when (c) {
                        Conn.Idle -> "Отключено"
                        Conn.Connecting -> "Ищем сервер…"
                        Conn.Connected -> "Вы подключены"
                    },
                    style = Type.Status, textAlign = TextAlign.Center
                )
                Text(
                    when (c) {
                        Conn.Idle -> "Нажмите, чтобы подключиться"
                        Conn.Connecting -> "Проверяем, какие серверы живы"
                        Conn.Connected -> "Трафик идёт через ${server?.name ?: "сервер"}"
                    },
                    style = Type.Caption, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        Spacer(Modifier.weight(1f))

        GlassCard(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(46.dp).glass(CircleShape, 1.2f), contentAlignment = Alignment.Center) {
                    Text(server?.flag ?: "🌐", style = Type.Title)
                }
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(server?.name ?: "Нет серверов", style = Type.Title)
                    Text("Лучший по пингу", style = Type.Caption)
                }
                PingChip(server?.ping)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(Icons.Rounded.Timer, "В сети", timeText(s.seconds, s.conn), Modifier.weight(1f))
            StatCard(Icons.Rounded.Dns, "Живых серверов", "${s.alive} из ${s.servers.size}", Modifier.weight(1f))
        }
        Spacer(Modifier.height(112.dp))
    }
}

private fun timeText(sec: Int, c: Conn): String =
    if (c != Conn.Connected) "—" else "%d:%02d:%02d".format(sec / 3600, sec % 3600 / 60, sec % 60)

@Composable
private fun StatCard(icon: ImageVector, label: String, value: String, modifier: Modifier) {
    GlassCard(modifier) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, null, tint = Palette.Muted, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(10.dp))
            Text(value, style = Type.Number)
            Text(label, style = Type.Caption)
        }
    }
}

@Composable
fun PingChip(ping: Int?) {
    val color = when {
        ping == null -> Palette.Muted
        ping < 80 -> Palette.Good
        ping < 150 -> Palette.Warn
        else -> Palette.Bad
    }
    Row(
        Modifier.glass(RoundedCornerShape(50), 0.8f).padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Text(
            if (ping == null) "нет связи" else "$ping мс",
            style = Type.Label.copy(color = color),
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun PowerButton(state: Conn, motion: Boolean, onClick: () -> Unit) {
    val inf = rememberInfiniteTransition(label = "power")
    val pulse by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart), label = "pulse")
    val spin by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart), label = "spin")
    val accent by animateColorAsState(
        when (state) {
            Conn.Idle -> Palette.Text
            Conn.Connecting -> Palette.Warn
            Conn.Connected -> Palette.Good
        },
        tween(500), label = "accent"
    )

    Box(Modifier.size(250.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val maxR = size.minDimension / 2
            if (state != Conn.Idle && motion) {
                for (i in 0..1) {
                    val p = (pulse + i * 0.5f) % 1f
                    drawCircle(
                        color = accent.copy(alpha = (1f - p) * 0.40f),
                        radius = maxR * (0.64f + 0.36f * p),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )
                }
            }
            if (state == Conn.Connecting) {
                val d = size.minDimension * 0.68f
                drawArc(
                    color = accent, startAngle = spin, sweepAngle = 110f, useCenter = false,
                    topLeft = Offset((size.width - d) / 2f, (size.height - d) / 2f),
                    size = Size(d, d),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            if (state == Conn.Connected) {
                drawCircle(accent.copy(alpha = 0.16f), radius = maxR * 0.66f)
            }
        }
        Box(
            Modifier.size(156.dp).pressable(onClick).glass(CircleShape, 1.5f),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.PowerSettingsNew, "Подключить", tint = accent, modifier = Modifier.size(60.dp))
        }
    }
}
