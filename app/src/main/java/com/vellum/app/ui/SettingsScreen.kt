package com.vellum.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.vellum.app.AppState
import com.vellum.app.BuildConfig
import com.vellum.app.Updater
import androidx.compose.foundation.background
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(s: AppState) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp)
    ) {
        Spacer(Modifier.height(14.dp))
        Text("Настройки", style = Type.Display)
        Spacer(Modifier.height(18.dp))

        Group("Источники серверов") {
            s.channels.forEachIndexed { i, ch ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Link, null, tint = Palette.Muted, modifier = Modifier.size(20.dp))
                    Text(ch, style = Type.Body, modifier = Modifier.weight(1f).padding(start = 12.dp))
                    Box(Modifier.size(32.dp).pressable { s.channels.removeAt(i) }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Close, "Удалить", tint = Palette.Muted, modifier = Modifier.size(18.dp))
                    }
                }
                Divider()
            }
            AddChannel { s.channels.add(it) }
        }

        Group("Обновление") {
            SwitchRow(Icons.Rounded.Autorenew, "Обновлять автоматически", "Брать свежие серверы из каналов", s.autoUpdate) { s.autoUpdate = it }
            Divider()
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Каждые", style = Type.Body, modifier = Modifier.weight(1f))
                listOf(1, 3, 6, 12).forEach { h ->
                    Chip("$h ч", s.intervalHours == h) { s.intervalHours = h }
                    Spacer(Modifier.width(6.dp))
                }
            }
        }

        Group("Подключение") {
            SwitchRow(Icons.Rounded.Lock, "Блокировать без VPN", "Если туннель упал, интернет отключается", s.killSwitch) { s.killSwitch = it }
            Divider()
            SwitchRow(Icons.Rounded.Bolt, "Подключаться при запуске", "Сразу выбирать лучший сервер", s.autoConnect) { s.autoConnect = it }
            Divider()
            SwitchRow(Icons.Rounded.Apps, "Только выбранные приложения", "Остальные идут напрямую", s.splitTunnel) { s.splitTunnel = it }
        }

        Group("Приложение") {
            UpdateBlock()
        }

        Group("Вид") {
            SwitchRow(Icons.Rounded.AutoAwesome, "Плавное движение", "Анимация фона и кнопки", s.smoothMotion) { s.smoothMotion = it }
        }
        Spacer(Modifier.height(112.dp))
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Text(title, style = Type.Caption, modifier = Modifier.padding(start = 6.dp, bottom = 8.dp))
    GlassCard(Modifier.fillMaxWidth()) { content() }
    Spacer(Modifier.height(18.dp))
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(Color.White.copy(alpha = 0.08f)))
}

@Composable
private fun SwitchRow(icon: ImageVector, title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(
            interactionSource = remember { MutableInteractionSource() }, indication = null
        ) { onChange(!checked) }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(38.dp).glass(RoundedCornerShape(12.dp), 1.2f), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Palette.Text, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(title, style = Type.Label)
            Text(sub, style = Type.Caption)
        }
        GlassSwitch(checked, onChange)
    }
}

@Composable
fun GlassSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val x by animateDpAsState(if (checked) 22.dp else 0.dp, spring(dampingRatio = 0.6f, stiffness = 600f), label = "thumb")
    val track by animateColorAsState(if (checked) Palette.Good.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.18f), tween(250), label = "track")
    Box(
        Modifier.width(52.dp).height(30.dp).clip(CircleShape).background(track)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onChange(!checked) }
            .padding(3.dp)
    ) {
        Box(Modifier.offset(x = x).size(24.dp).clip(CircleShape).background(Color.White))
    }
}

@Composable
private fun Chip(text: String, on: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (on) Color.White.copy(alpha = 0.26f) else Color.White.copy(alpha = 0.07f), tween(250), label = "chip")
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(bg).pressable(onClick).padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(text, style = Type.Label.copy(color = if (on) Palette.Text else Palette.Muted))
    }
}

@Composable
private fun AddChannel(onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    fun submit() {
        val v = text.trim()
        if (v.isNotEmpty()) {
            onAdd(if (v.startsWith("@") || v.startsWith("http")) v else "@$v")
            text = ""
        }
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        BasicTextField(
            value = text, onValueChange = { text = it }, singleLine = true,
            textStyle = Type.Body, cursorBrush = SolidColor(Palette.Good),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box {
                    if (text.isEmpty()) Text("Добавить канал, например @name", style = Type.Caption)
                    inner()
                }
            }
        )
        Box(Modifier.size(34.dp).pressable { submit() }.glass(CircleShape, 1.4f), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Add, "Добавить", tint = Palette.Text, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun UpdateBlock() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("Версия ${BuildConfig.VERSION_NAME}") }
    var busy by remember { mutableStateOf(false) }
    var pct by remember { mutableIntStateOf(-1) }

    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text("Vellum ${BuildConfig.VERSION_NAME}", style = Type.Label)
        Text(status, style = Type.Caption)
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                .background(if (busy) Color.White.copy(alpha = 0.18f) else Palette.Good.copy(alpha = 0.85f))
                .clickable(enabled = !busy) {
                    busy = true
                    pct = -1
                    status = "Проверяю обновление…"
                    scope.launch {
                        try {
                            val latest = withContext(Dispatchers.IO) { Updater.check() }
                            if (latest == null) {
                                status = "Уже последняя версия"
                            } else {
                                status = "Скачиваю…"
                                val file = withContext(Dispatchers.IO) {
                                    Updater.download(ctx, latest) { p -> pct = p }
                                }
                                status = "Готово, открываю установку"
                                Updater.install(ctx, file)
                            }
                        } catch (e: Exception) {
                            status = e.message ?: "Не получилось обновить"
                        }
                        busy = false
                    }
                }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                when {
                    busy && pct >= 0 -> "Скачиваю… $pct%"
                    busy -> "Проверяю…"
                    else -> "Скачать и обновить"
                },
                style = Type.Label,
                color = Color.Black
            )
        }
    }
}
