package com.vellum.app

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.nekohasekai.libbox.CommandServer
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.libbox.SetupOptions
import io.nekohasekai.libbox.BridgeOptions
import io.nekohasekai.libbox.BridgeSession
import io.nekohasekai.libbox.CommandServerHandler
import io.nekohasekai.libbox.ConnectionOwner
import io.nekohasekai.libbox.InterfaceUpdateListener
import io.nekohasekai.libbox.LocalDNSTransport
import io.nekohasekai.libbox.NeighborUpdateListener
import io.nekohasekai.libbox.NetworkInterfaceIterator
import io.nekohasekai.libbox.PlatformUser
import io.nekohasekai.libbox.ShellSession
import io.nekohasekai.libbox.StringIterator
import io.nekohasekai.libbox.WIFIState
import io.nekohasekai.libbox.Notification
import io.nekohasekai.libbox.OverrideOptions
import io.nekohasekai.libbox.PlatformInterface
import io.nekohasekai.libbox.SystemProxyStatus
import io.nekohasekai.libbox.TunOptions
import org.json.JSONObject
import java.io.File

/** Состояние подключения: его читают экран и сервис. */
object VpnControl {
    var conn by mutableStateOf(Conn.Idle)
}

object Vpn {
    const val ACTION_STOP = "app.vellum.STOP"
    const val EXTRA_CONFIG = "config"

    fun start(ctx: Context, server: Server?) {
        if (server == null) {
            Toast.makeText(ctx, "Сначала обнови список серверов", Toast.LENGTH_LONG).show()
            return
        }
        VpnControl.conn = Conn.Connecting
        val config = Links.config(JSONObject(server.outbound))
        ctx.startService(Intent(ctx, VellumVpnService::class.java).putExtra(EXTRA_CONFIG, config))
    }

    fun stop(ctx: Context) {
        ctx.startService(Intent(ctx, VellumVpnService::class.java).setAction(ACTION_STOP))
    }
}

/** Системный VPN-сервис: поднимает туннель и запускает sing-box через libbox. */
class VellumVpnService : VpnService(), CommandServerHandler, PlatformInterface {
    private var box: CommandServer? = null
    private var tun: ParcelFileDescriptor? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    /** libbox должен знать, где можно писать служебные файлы (иначе падает на read-only). */
    private fun setupLibbox() {
        if (libboxReady) return
        val opts = SetupOptions()
        opts.basePath = filesDir.absolutePath
        opts.workingPath = File(filesDir, "work").apply { mkdirs() }.absolutePath
        opts.tempPath = cacheDir.absolutePath
        Libbox.setup(opts)
        libboxReady = true
    }

    companion object {
        @Volatile private var libboxReady = false
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == Vpn.ACTION_STOP) {
            shutdown()
            return START_NOT_STICKY
        }
        val config = intent?.getStringExtra(Vpn.EXTRA_CONFIG) ?: return START_NOT_STICKY
        closeBox()
        // Запуск ядра тяжёлый и блокирующий: делаем его не в главном потоке, интерфейс не должен висеть
        Thread {
            try {
                setupLibbox()
                val server = CommandServer(this, this)
                server.start()
                server.startOrReloadService(config, OverrideOptions())
                box = server
                mainHandler.post { VpnControl.conn = Conn.Connected }
            } catch (e: Throwable) {
                Diag.save(this, "Ошибка VPN:\n" + e.stackTraceToString())
                mainHandler.post {
                    Toast.makeText(this, "VPN не запустился: ${e.message}", Toast.LENGTH_LONG).show()
                    shutdown()
                }
            }
        }.start()
        return START_NOT_STICKY
    }

    override fun onRevoke() = shutdown()

    override fun onDestroy() {
        closeBox()
        super.onDestroy()
    }

    private fun closeBox() {
        runCatching { box?.closeService() }
        runCatching { box?.close() }
        box = null
        runCatching { tun?.close() }
        tun = null
    }

    private fun shutdown() {
        closeBox()
        VpnControl.conn = Conn.Idle
        stopSelf()
    }

    // CommandServerHandler
    override fun serviceStop() = shutdown()
    override fun serviceReload() {}
    override fun getSystemProxyStatus(): SystemProxyStatus? = null
    override fun setSystemProxyEnabled(isEnabled: Boolean) {}
    override fun triggerNativeCrash() {}
    override fun writeDebugMessage(message: String?) {}
    override fun connectSSHAgent(): Int = -1

    // PlatformInterface (сигнатуры из libbox 1.14.0)
    override fun autoDetectInterfaceControl(fd: Int) {
        protect(fd)
    }

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
    override fun readSystemSSHHostKey(): String = ""
    override fun readWIFIState(): WIFIState? = null
    override fun registerMyInterface(name: String) {}
    override fun sendNotification(notification: Notification) {}
    override fun startDefaultInterfaceMonitor(listener: InterfaceUpdateListener) {}
    override fun startNeighborMonitor(listener: NeighborUpdateListener) {}
    override fun tailscaleHostname(): String = ""
    override fun underNetworkExtension(): Boolean = false
    override fun usePlatformAutoDetectInterfaceControl(): Boolean = true
    override fun usePlatformBridge(): Boolean = false
    override fun usePlatformShell(): Boolean = false
    override fun useProcFS(): Boolean = false

    override fun openTun(options: TunOptions): Int {
        if (prepare(this) != null) error("нет разрешения на VPN")
        val pfd = Builder().setSession("Vellum").setMtu(options.mtu)
            .addAddress("172.19.0.1", 30)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("172.19.0.2")
            .establish() ?: error("VPN не установлен")
        tun = pfd
        return pfd.fd
    }
}
