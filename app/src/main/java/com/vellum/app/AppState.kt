package com.vellum.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject

enum class Conn { Idle, Connecting, Connected }

/** Сервер для экрана: outbound храним строкой JSON. */
data class Server(val flag: String, val name: String, val ping: Int?, val type: String = "", val outbound: String = "", val bad: Boolean = false)

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
    var fullCheck by mutableStateOf(Store.fullCheck(ctx))

    val alive get() = servers.size

    init {
        channels.addAll(Store.sources(ctx))
        servers.addAll(Store.servers(ctx))
        selected = Store.selected(ctx).coerceIn(0, (servers.size - 1).coerceAtLeast(0))
    }

    fun current(): Server? = servers.getOrNull(selected)

    fun changeFullCheck(v: Boolean) {
        fullCheck = v
        Store.saveFullCheck(ctx, v)
    }

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

    /** Проверяет все серверы: быстро (TCP) или полно (через sing-box), если включено и VPN выключен. */
    suspend fun checkAll() {
        val list = servers.toList()
        if (list.isEmpty()) return
        val full = fullCheck && VpnControl.conn == Conn.Idle
        val pings: List<Int?> = withContext(Dispatchers.IO) {
            if (full) list.map { probe(it, true) }
            else coroutineScope { list.map { async { probe(it, false) } }.awaitAll() }
        }
        pings.forEachIndexed { i, p ->
            if (i < servers.size) servers[i] = servers[i].copy(ping = p, bad = p == null)
        }
    }

    private fun probe(sv: Server, full: Boolean): Int? {
        val ob = runCatching { JSONObject(sv.outbound) }.getOrNull() ?: return null
        return if (full) FullCheck.run(ob) else Check.tcp(ob.getString("server"), ob.getInt("server_port"))
    }

    /** Загружает все подписки и ссылки, собирает список и проверяет серверы. Возвращает текст для пользователя. */
    suspend fun refresh(): String {
        if (refreshing) return "Уже обновляем"
        refreshing = true
        try {
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
            if (list.isEmpty()) return "Серверов не найдено"
            checkAll()
            return "Рабочих: ${servers.count { it.ping != null }} из ${servers.size}"
        } finally {
            refreshing = false
        }
    }
}
