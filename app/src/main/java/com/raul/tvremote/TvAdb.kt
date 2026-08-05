package com.raul.tvremote

import android.content.Context
import dadb.AdbKeyPair
import dadb.Dadb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket

object TvAdb {

    @Volatile private var dadb: Dadb? = null

    private fun keys(ctx: Context): AdbKeyPair {
        val priv = File(ctx.filesDir, "adbkey")
        val pub = File(ctx.filesDir, "adbkey.pub")
        if (!priv.exists() || !pub.exists()) AdbKeyPair.generate(priv, pub)
        return AdbKeyPair.read(priv, pub)
    }

    fun lastIp(ctx: Context): String =
        ctx.getSharedPreferences("tv", Context.MODE_PRIVATE).getString("ip", "") ?: ""

    suspend fun connect(ctx: Context, ip: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            runCatching { dadb?.close() }
            val d = Dadb.create(ip, 5555, keys(ctx))
            val model = d.shell("getprop ro.product.model").output.trim()
            dadb = d
            ctx.getSharedPreferences("tv", Context.MODE_PRIVATE)
                .edit().putString("ip", ip).apply()
            if (model.isBlank()) "Android TV ($ip)" else "$model ($ip)"
        }
    }

    private suspend fun shell(cmd: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val d = dadb ?: error("Sin TV conectada")
            d.shell(cmd).output
        }
    }

    suspend fun key(code: Int, long: Boolean = false): Result<String> =
        shell("input keyevent " + (if (long) "--longpress " else "") + code)

    suspend fun text(t: String): Result<String> =
        shell("input text '" + t.replace("'", "").replace(" ", "%s") + "'")

    suspend fun apps(): List<Pair<String, String>> {
        val out = shell("cmd package list packages -3").getOrElse { return emptyList() }
        return out.lines()
            .map { it.trim().removePrefix("package:").trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .map { it to prettyName(it) }
            .sortedWith(compareBy({ if (NAMES.containsKey(it.first)) 0 else 1 }, { it.second }))
    }

    suspend fun launch(pkg: String): Result<String> {
        val r = shell("monkey -p $pkg -c android.intent.category.LEANBACK_LAUNCHER 1")
        val txt = r.getOrNull() ?: return r
        return if (txt.contains("No activities"))
            shell("monkey -p $pkg -c android.intent.category.LAUNCHER 1") else r
    }

    suspend fun scan(myIp: String): List<String> = coroutineScope {
        val base = myIp.substringBeforeLast('.')
        (1..254).map { i ->
            async(Dispatchers.IO) {
                val ip = "$base.$i"
                runCatching {
                    Socket().use { it.connect(InetSocketAddress(ip, 5555), 500) }
                    ip
                }.getOrNull()
            }
        }.mapNotNull { it.await() }
    }

    private val NAMES = mapOf(
        "com.netflix.ninja" to "Netflix",
        "com.google.android.youtube.tv" to "YouTube",
        "com.amazon.amazonvideo.livingroom" to "Prime Video",
        "com.disney.disneyplus" to "Disney+",
        "com.spotify.tv.android" to "Spotify",
        "com.wbd.stream" to "Max",
        "com.plexapp.android" to "Plex",
        "org.videolan.vlc" to "VLC",
        "com.apple.atve.androidtv.appletv" to "Apple TV",
        "tv.pluto.android" to "Pluto TV",
        "com.twitch.android.app" to "Twitch"
    )

    private fun prettyName(pkg: String): String =
        NAMES[pkg] ?: pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }
}

object K {
    const val POWER = 26
    const val VOL_UP = 24
    const val VOL_DOWN = 25
    const val MUTE = 164
    const val UP = 19
    const val DOWN = 20
    const val LEFT = 21
    const val RIGHT = 22
    const val OK = 23
    const val BACK = 4
    const val HOME = 3
    const val MENU = 82
    const val SETTINGS = 176
    const val PLAY_PAUSE = 85
    const val STOP = 86
    const val REW = 89
    const val FFW = 90
    const val NEXT = 87
    const val CH_UP = 166
    const val CH_DOWN = 167
    const val SOURCE = 178
    const val GUIDE = 172
    const val SEARCH = 84
    const val ASSIST = 219
    const val CAPTIONS = 175
    const val INFO = 165
    const val RECENTS = 187
    const val ENTER = 66
    const val DEL = 67
    fun num(n: Int) = 7 + n
}
