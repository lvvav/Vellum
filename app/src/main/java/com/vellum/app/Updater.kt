package com.vellum.app

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Обновление прямо из приложения: последняя сборка лежит в релизе vellum-latest. */
object Updater {
    private const val RELEASE_API = "https://api.github.com/repos/lvvav/Vellum/releases/tags/vellum-latest"
    private const val APK_NAME = "Vellum.apk"
    private const val VERSION_NAME = "version.txt"

    class Latest(val apkUrl: String, val commit: String)

    class Failure(message: String) : Exception(message)

    /** Возвращает последнюю сборку, если она отличается от установленной, иначе null. */
    fun check(): Latest? {
        val c = (URL(RELEASE_API).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("Accept", "application/vnd.github+json")
        }
        if (c.responseCode !in 200..299) throw Failure("Не удалось проверить обновление (код ${c.responseCode}).")
        val assets = JSONObject(c.inputStream.bufferedReader().use { it.readText() }).getJSONArray("assets")
        var apk: String? = null
        var version: String? = null
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            when (a.getString("name")) {
                APK_NAME -> apk = a.getString("browser_download_url")
                VERSION_NAME -> version = a.getString("browser_download_url")
            }
        }
        if (apk == null || version == null) throw Failure("В релизе нет файла обновления.")
        val remote = URL(version).openStream().bufferedReader().use { it.readText() }.trim()
        if (BuildConfig.COMMIT != "dev" && remote == BuildConfig.COMMIT) return null
        return Latest(apk, remote)
    }

    /** Скачивает APK в кэш приложения и сообщает проценты. */
    fun download(ctx: Context, latest: Latest, onProgress: (Int) -> Unit): File {
        val dir = File(ctx.cacheDir, "update").apply { mkdirs() }
        val file = File(dir, APK_NAME)
        val c = (URL(latest.apkUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 120_000
            instanceFollowRedirects = true
        }
        if (c.responseCode !in 200..299) throw Failure("Не удалось скачать обновление (код ${c.responseCode}).")
        val total = c.contentLengthLong
        var done = 0L
        c.inputStream.use { input ->
            file.outputStream().use { out ->
                val buf = ByteArray(16 * 1024)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n)
                    done += n
                    if (total > 0) onProgress((done * 100 / total).toInt())
                }
            }
        }
        return file
    }

    /** Открывает системный установщик. При первом запуске Android попросит разрешить установку. */
    fun install(ctx: Context, file: File) {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent)
    }
}
