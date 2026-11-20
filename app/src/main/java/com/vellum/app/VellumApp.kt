package com.vellum.app

import android.app.Application
import android.content.Context

/** Ловит необработанные падения и сохраняет их текст, чтобы показать в настройках. */
class VellumApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            Diag.save(this, "Падение в потоке ${t.name}:\n" + e.stackTraceToString())
            prev?.uncaughtException(t, e)
        }
    }
}

/** Последняя ошибка, которую приложение записало на диск. */
object Diag {
    private const val FILE = "diag"

    fun save(ctx: Context, text: String) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString("last", text.take(6000)).commit()
    }

    fun last(ctx: Context): String? = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString("last", null)

    /** Журнал шагов запуска VPN: пишется сразу на диск, поэтому виден даже после нативного падения. */
    fun step(ctx: Context, msg: String) {
        val p = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val old = p.getString("trace", "") ?: ""
        p.edit().putString("trace", (old + "\n" + msg).takeLast(3000)).commit()
    }

    fun resetTrace(ctx: Context) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString("trace", "").commit()
    }

    fun trace(ctx: Context): String = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString("trace", "") ?: ""

    fun clear(ctx: Context) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().remove("last").apply()
    }
}
