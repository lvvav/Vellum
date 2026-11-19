package com.vellum.app

import io.nekohasekai.libbox.BridgeOptions
import io.nekohasekai.libbox.BridgeSession
import io.nekohasekai.libbox.CommandServer
import io.nekohasekai.libbox.CommandServerHandler
import io.nekohasekai.libbox.ConnectionOwner
import io.nekohasekai.libbox.InterfaceUpdateListener
import io.nekohasekai.libbox.LocalDNSTransport
import io.nekohasekai.libbox.NeighborUpdateListener
import io.nekohasekai.libbox.NetworkInterfaceIterator
import io.nekohasekai.libbox.Notification
import io.nekohasekai.libbox.OverrideOptions
import io.nekohasekai.libbox.PlatformInterface
import io.nekohasekai.libbox.PlatformUser
import io.nekohasekai.libbox.ShellSession
import io.nekohasekai.libbox.StringIterator
import io.nekohasekai.libbox.SystemProxyStatus
import io.nekohasekai.libbox.TunOptions
import io.nekohasekai.libbox.WIFIState
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ServerSocket
import java.net.Socket
import java.net.URL

/** Быстрая проверка: открывается ли TCP-порт сервера, и за сколько мс. */
object Check {
    fun tcp(host: String, port: Int): Int? = runCatching {
        val t0 = System.nanoTime()
        Socket().use { it.connect(InetSocketAddress(host, port), 4000) }
        ((System.nanoTime() - t0) / 1_000_000).toInt()
    }.getOrNull()
}

/** Полная проверка: поднимаем sing-box только с этим сервером и пробуем открыть страницу через него. */
object FullCheck {
    fun run(outbound: JSONObject): Int? {
        val port = ServerSocket(0).use { it.localPort }
        val config = JSONObject()
            .put("log", JSONObject().put("level", "fatal"))
            .put("inbounds", JSONArray().put(
                JSONObject().put("type", "mixed").put("tag", "in").put("listen", "127.0.0.1").put("listen_port", port)
            ))
            .put("outbounds", JSONArray().put(outbound).put(JSONObject().put("type", "direct").put("tag", "direct")))
            .put("route", JSONObject().put("final", "proxy"))
            .toString()
        val server = CommandServer(Probe, Probe)
        return try {
            server.start()
            server.startOrReloadService(config, OverrideOptions())
            val t0 = System.nanoTime()
            val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", port))
            val c = (URL("http://www.gstatic.com/generate_204").openConnection(proxy) as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 8_000
            }
            val code = c.responseCode
            if (code == 204 || code == 200) ((System.nanoTime() - t0) / 1_000_000).toInt() else null
        } catch (e: Exception) {
            null
        } finally {
            runCatching { server.closeService() }
            runCatching { server.close() }
        }
    }
}

/** Заглушка платформы для проверки: TUN и системные функции не нужны. */
private object Probe : PlatformInterface, CommandServerHandler {
    override fun autoDetectInterfaceControl(fd: Int) {}
    override fun cancelNotification(identifier: String, typeID: Int) {}
    override fun checkPlatformShell() {}
    override fun clearDNSCache() {}
    override fun closeDefaultInterfaceMonitor(listener: InterfaceUpdateListener) {}
    override fun closeNeighborMonitor(listener: NeighborUpdateListener) {}
    override fun createBridge(options: BridgeOptions): BridgeSession? = null
    override fun findConnectionOwner(ipProtocol: Int, sourceAddress: String, sourcePort: Int, destinationAddress: String, destinationPort: Int): ConnectionOwner? = null
    override fun getInterfaces(): NetworkInterfaceIterator? = null
    override fun includeAllNetworks(): Boolean = false
    override fun localDNSTransport(): LocalDNSTransport? = null
    override fun lookupSFTPServer(): String = ""
    override fun lookupUser(name: String): PlatformUser? = null
    override fun openShellSession(user: PlatformUser?, command: String?, args: StringIterator?, dir: String?, cols: Int, rows: Int): ShellSession? = null
    override fun openTun(options: TunOptions): Int = error("TUN не нужен для проверки")
    override fun readSystemSSHHostKey(): String = ""
    override fun readWIFIState(): WIFIState? = null
    override fun registerMyInterface(name: String) {}
    override fun sendNotification(notification: Notification) {}
    override fun startDefaultInterfaceMonitor(listener: InterfaceUpdateListener) {}
    override fun startNeighborMonitor(listener: NeighborUpdateListener) {}
    override fun tailscaleHostname(): String = ""
    override fun underNetworkExtension(): Boolean = false
    override fun usePlatformAutoDetectInterfaceControl(): Boolean = false
    override fun usePlatformBridge(): Boolean = false
    override fun usePlatformShell(): Boolean = false
    override fun useProcFS(): Boolean = false
    override fun serviceStop() {}
    override fun serviceReload() {}
    override fun getSystemProxyStatus(): SystemProxyStatus? = null
    override fun setSystemProxyEnabled(isEnabled: Boolean) {}
    override fun triggerNativeCrash() {}
    override fun writeDebugMessage(message: String?) {}
    override fun connectSSHAgent(): Int = -1
}
