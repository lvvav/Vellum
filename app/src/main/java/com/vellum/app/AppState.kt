package com.vellum.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class Conn { Idle, Connecting, Connected }

/** Сервер для экрана: outbound храним строкой JSON. */
data class Server(val flag: String, val name: String, val ping: Int?, val type: String = "", val outbound: String = "")

class AppState(private val ctx: Context) {
    val conn: Conn get() = VpnControl.conn
    var seconds by mutableIntStateOf(0)
    var selected by mutableIntStateOf(0)
    var refreshing by mutableStateOf(false)

    val servers = mutableStateListOf<Server>()
    val channels = mutableStateListOf<String>()

    var autoUpdate by mutableStateOf(true)
    var intervalHours by mutableIntStateOf(3)
    var killSwitch by mutableStateOf(true)
    var autoConnect by mutableStateOf(false)
    var splitTunnel by mutableStateOf(false)
    var smoothMotion by mutableStateOf(true)

    val alive get() = servers.size

    init {
        channels.addAll(Store.sources(ctx))
        servers.addAll(Store.servers(ctx))
        selected = Store.selected(ctx).coerceIn(0, (servers.size - 1).coerceAtLeast(0))
    }

    fun current(): Server? = servers.getOrNull(selected)

    fun select(i: Int) {
        selected = i
        Store.saveSelected(ctx, i)
    }

    fun addSource(x: String) {
        val t = x.trim()
        if (t.isEmpty() || channels.contains(t)) return
        channels.add(t)
        Store.saveSources(ctx, channels.toList())
    }

    fun removeSource(i: Int) {
        channels.removeAt(i)
        Store.saveSources(ctx, channels.toList())
    }

    /** Загружает все подписки и ссылки, собирает список серверов. Возвращает текст для пользователя. */
    suspend fun refresh(): String {
        if (refreshing) return "Уже обновляем"
        refreshing = true
        return try {
            val sources = channels.toList()
            val nodes = withContext(Dispatchers.IO) {
                sources.flatMap { src ->
                    runCatching {
                        if (src.startsWith("http")) Links.fromSubscription(Links.fetch(src))
                        else listOfNotNull(Links.parse(src))
                    }.getOrDefault(emptyList())
                }
            }
            val list = nodes.map { Server("🌐", it.name, null, it.type, it.outbound.toString()) }
            servers.clear()
            servers.addAll(list)
            selected = 0
            Store.saveServers(ctx, list)
            Store.saveSelected(ctx, 0)
            if (list.isEmpty()) "Серверов не найдено" else "Серверов: ${list.size}"
        } finally {
            refreshing = false
        }
    }
}
