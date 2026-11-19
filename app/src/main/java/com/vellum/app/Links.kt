package com.vellum.app

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder

/** Сервер из подписки или ссылки: имя, тип и готовый outbound для sing-box. */
data class Node(val name: String, val type: String, val outbound: JSONObject)

object Links {
    fun fetch(url: String): String {
        val c = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Vellum")
        }
        if (c.responseCode !in 200..299) throw IllegalStateException("Подписка не открылась (код ${c.responseCode})")
        return c.inputStream.bufferedReader().use { it.readText() }
    }

    /** Подписка: текст со ссылками или base64 от такого текста. */
    fun fromSubscription(body: String): List<Node> {
        val direct = parseAll(body)
        if (direct.isNotEmpty()) return direct
        val decoded = runCatching { b64(body) }.getOrNull() ?: return emptyList()
        return parseAll(decoded)
    }

    fun parseAll(text: String): List<Node> =
        text.lines().map { it.trim() }.filter { it.contains("://") }
            .mapNotNull { runCatching { parse(it) }.getOrNull() }

    fun parse(link: String): Node? {
        val body = link.substringAfter("://")
        return when (link.substringBefore("://").lowercase()) {
            "vless" -> vless(body)
            "trojan" -> trojan(body)
            "ss" -> ss(body)
            "vmess" -> vmess(body)
            else -> null
        }
    }

    private class Parts(val main: String, val query: Map<String, String>, val name: String)

    private fun split(body: String): Parts {
        val hash = body.indexOf('#')
        val name = if (hash >= 0) dec(body.substring(hash + 1)) else ""
        val rest = if (hash >= 0) body.substring(0, hash) else body
        val q = rest.indexOf('?')
        val main = if (q >= 0) rest.substring(0, q) else rest
        val query = if (q >= 0) {
            rest.substring(q + 1).split('&').filter { it.contains('=') }
                .associate { dec(it.substringBefore('=')) to dec(it.substringAfter('=')) }
        } else emptyMap()
        return Parts(main, query, name)
    }

    private fun dec(s: String): String = runCatching { URLDecoder.decode(s, "UTF-8") }.getOrDefault(s)

    private fun b64(s: String): String = String(Base64.decode(s.trim(), Base64.DEFAULT or Base64.URL_SAFE), Charsets.UTF_8)

    private fun hostPort(hp: String): Pair<String, Int> =
        hp.substringBeforeLast(':').removePrefix("[").removeSuffix("]") to hp.substringAfterLast(':').toInt()

    private fun vless(body: String): Node {
        val p = split(body)
        val (host, port) = hostPort(p.main.substringAfterLast('@'))
        val o = JSONObject().put("type", "vless").put("tag", "proxy")
            .put("server", host).put("server_port", port)
            .put("uuid", dec(p.main.substringBeforeLast('@')))
        p.query["flow"]?.takeIf { it.isNotBlank() }?.let { o.put("flow", it) }
        return finish(o, p.query, p.name, "vless", host, port, forced = false)
    }

    private fun trojan(body: String): Node {
        val p = split(body)
        val (host, port) = hostPort(p.main.substringAfterLast('@'))
        val o = JSONObject().put("type", "trojan").put("tag", "proxy")
            .put("server", host).put("server_port", port)
            .put("password", dec(p.main.substringBeforeLast('@')))
        return finish(o, p.query, p.name, "trojan", host, port, forced = true)
    }

    private fun ss(body: String): Node {
        val p = split(body)
        // Два формата: base64(метод:пароль)@хост:порт и base64(метод:пароль@хост:порт)
        val full = if (p.main.contains('@')) p.main else b64(p.main)
        val userPart = full.substringBeforeLast('@')
        val creds = if (userPart.contains(':')) userPart else b64(userPart)
        val (host, port) = hostPort(full.substringAfterLast('@'))
        val o = JSONObject().put("type", "shadowsocks").put("tag", "proxy")
            .put("method", creds.substringBefore(':'))
            .put("password", creds.substringAfter(':'))
            .put("server", host).put("server_port", port)
        return Node(p.name.ifBlank { "$host:$port" }, "ss", o)
    }

    private fun vmess(body: String): Node {
        val j = JSONObject(b64(body.substringBefore('#')))
        val host = j.optString("add")
        val port = j.opt("port").toString().toInt()
        val path = j.optString("path")
        val q = mapOf(
            "type" to j.optString("net", "tcp").ifBlank { "tcp" },
            "path" to path,
            "host" to j.optString("host"),
            "serviceName" to path,
            "security" to (if (j.optString("tls") == "tls") "tls" else "none"),
            "sni" to j.optString("sni").ifBlank { j.optString("host") },
        )
        val o = JSONObject().put("type", "vmess").put("tag", "proxy")
            .put("server", host).put("server_port", port)
            .put("uuid", j.getString("id"))
            .put("alter_id", j.optString("aid", "0").toIntOrNull() ?: 0)
            .put("security", "auto")
        return finish(o, q, j.optString("ps"), "vmess", host, port, forced = false)
    }

    private fun finish(o: JSONObject, q: Map<String, String>, name: String, type: String, host: String, port: Int, forced: Boolean): Node {
        tls(q, forced)?.let { o.put("tls", it) }
        transport(q)?.let { o.put("transport", it) }
        return Node(name.ifBlank { "$host:$port" }, type, o)
    }

    private fun tls(q: Map<String, String>, forced: Boolean): JSONObject? {
        val sec = q["security"]?.takeIf { it.isNotBlank() } ?: if (forced) "tls" else "none"
        if (sec == "none") return null
        val t = JSONObject().put("enabled", true)
        q["sni"]?.takeIf { it.isNotBlank() }?.let { t.put("server_name", it) }
        if (q["allowInsecure"] == "1" || q["insecure"] == "1") t.put("insecure", true)
        q["fp"]?.takeIf { it.isNotBlank() }?.let { t.put("utls", JSONObject().put("enabled", true).put("fingerprint", it)) }
        if (sec == "reality") {
            t.put("reality", JSONObject().put("enabled", true)
                .put("public_key", q["pbk"] ?: "").put("short_id", q["sid"] ?: ""))
        }
        return t
    }

    private fun transport(q: Map<String, String>): JSONObject? = when (q["type"] ?: "tcp") {
        "ws" -> JSONObject().put("type", "ws").put("path", q["path"]?.ifBlank { "/" } ?: "/").apply {
            q["host"]?.takeIf { it.isNotBlank() }?.let { put("headers", JSONObject().put("Host", it)) }
        }
        "grpc" -> JSONObject().put("type", "grpc").put("service_name", q["serviceName"] ?: q["path"] ?: "")
        else -> null
    }

    /** Полный конфиг sing-box: весь трафик через туннель, DNS через прокси. */
    fun config(outbound: JSONObject): String {
        val dns = JSONObject()
            .put("servers", JSONArray()
                .put(JSONObject().put("tag", "remote").put("type", "https").put("server", "1.1.1.1").put("detour", "proxy"))
                .put(JSONObject().put("tag", "local").put("type", "local")))
            .put("final", "remote")
        val tun = JSONObject().put("type", "tun").put("tag", "tun-in")
            .put("address", JSONArray().put("172.19.0.1/30"))
            .put("mtu", 1400).put("auto_route", true).put("strict_route", true).put("stack", "system")
        val route = JSONObject().put("auto_detect_interface", true).put("final", "proxy")
            .put("rules", JSONArray()
                .put(JSONObject().put("action", "sniff"))
                .put(JSONObject().put("protocol", "dns").put("action", "hijack-dns")))
        return JSONObject()
            .put("log", JSONObject().put("level", "warn"))
            .put("dns", dns)
            .put("inbounds", JSONArray().put(tun))
            .put("outbounds", JSONArray().put(outbound).put(JSONObject().put("type", "direct").put("tag", "direct")))
            .put("route", route)
            .toString()
    }
}
