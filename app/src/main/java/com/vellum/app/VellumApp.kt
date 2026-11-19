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

    fun clear(ctx: Context) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().remove("last").apply()
    }
}
