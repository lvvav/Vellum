package com.vellum.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

enum class Conn { Idle, Connecting, Connected }

data class Server(val flag: String, val name: String, val ping: Int?)

/** Demo state. Later this is replaced by the real VPN service + channel parser. */
class AppState {
    var conn by mutableStateOf(Conn.Idle)
    var seconds by mutableIntStateOf(0)
    var selected by mutableIntStateOf(0)
    var refreshing by mutableStateOf(false)

    val servers = mutableStateListOf(
        Server("🇫🇮", "Финляндия", 41),
        Server("🇩🇪", "Германия", 48),
        Server("🇳🇱", "Нидерланды", 62),
        Server("🇸🇪", "Швеция", 77),
        Server("🇺🇸", "США", 164),
        Server("🇹🇷", "Турция", null),
    )
    val channels = mutableStateListOf("@my_channel_one", "@my_channel_two")

    var autoUpdate by mutableStateOf(true)
    var intervalHours by mutableIntStateOf(3)
    var killSwitch by mutableStateOf(true)
    var autoConnect by mutableStateOf(false)
    var splitTunnel by mutableStateOf(false)
    var smoothMotion by mutableStateOf(true)

    val alive get() = servers.count { it.ping != null }

    suspend fun toggle() {
        when (conn) {
            Conn.Idle -> {
                conn = Conn.Connecting
                delay(1800)
                seconds = 0
                conn = Conn.Connected
            }
            Conn.Connected -> conn = Conn.Idle
            Conn.Connecting -> Unit
        }
    }

    suspend fun refresh() {
        if (refreshing) return
        refreshing = true
        delay(1600)
        val fresh = servers.map { s -> s.copy(ping = if (s.ping == null) 90 else (s.ping + (-12..14).random()).coerceAtLeast(20)) }
        servers.clear()
        servers.addAll(fresh.sortedBy { it.ping ?: Int.MAX_VALUE })
        selected = 0
        refreshing = false
    }
}
