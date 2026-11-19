package com.vellum.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Сохранённые подписки, ссылки и список серверов между запусками. */
object Store {
    private const val FILE = "vellum"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun sources(ctx: Context): List<String> = runCatching {
        val a = JSONArray(prefs(ctx).getString("sources", "[]"))
        (0 until a.length()).map { a.getString(it) }
    }.getOrDefault(emptyList())

    fun saveSources(ctx: Context, list: List<String>) {
        prefs(ctx).edit().putString("sources", JSONArray(list).toString()).apply()
    }

    fun servers(ctx: Context): List<Server> = runCatching {
        val a = JSONArray(prefs(ctx).getString("servers", "[]"))
        (0 until a.length()).map {
            val o = a.getJSONObject(it)
            Server("🌐", o.getString("name"), null, o.getString("type"), o.getString("outbound"))
        }
    }.getOrDefault(emptyList())

    fun saveServers(ctx: Context, list: List<Server>) {
        val a = JSONArray()
        list.forEach { a.put(JSONObject().put("name", it.name).put("type", it.type).put("outbound", it.outbound)) }
        prefs(ctx).edit().putString("servers", a.toString()).apply()
    }

    fun fullCheck(ctx: Context): Boolean = prefs(ctx).getBoolean("fullCheck", false)

    fun saveFullCheck(ctx: Context, v: Boolean) {
        prefs(ctx).edit().putBoolean("fullCheck", v).apply()
    }

    fun selected(ctx: Context): Int = prefs(ctx).getInt("selected", 0)

    fun saveSelected(ctx: Context, i: Int) {
        prefs(ctx).edit().putInt("selected", i).apply()
    }
}
