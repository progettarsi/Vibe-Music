package com.progettarsi.vibemusic.network

import android.util.Log
import com.google.gson.JsonObject
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

object YouTubeClient {
    private const val BASE_URL = "https://music.youtube.com/"
    const val API_KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"

    // --- DEFINIZIONE DEI CLIENT ---
    enum class ClientType {
        WEB_REMIX, IOS, ANDROID
    }

    // Dati copiati da OuterTune
    private const val WEB_REMIX_ID = "67"
    private const val WEB_REMIX_VERSION = "1.20250310.01.00"
    private const val WEB_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0"

    private const val IOS_ID = "5"
    private const val IOS_VERSION = "19.29.1"
    private const val IOS_UA = "com.google.ios.youtube/19.29.1 (iPhone16,2; U; CPU iOS 17_5_1 like Mac OS X;)"

    private const val ANDROID_ID = "3"
    private const val ANDROID_VERSION = "19.29.35"
    private const val ANDROID_UA = "com.google.android.youtube/19.29.35 (Linux; U; Android 14) gzip"

    var currentCookie: String = ""

    // Genera gli header specifici per il tipo di client richiesto
    fun getClientHeaders(type: ClientType): Map<String, String> {
        val headers = mutableMapOf(
            "Content-Type" to "application/json",
            "X-Goog-Api-Format-Version" to "1",
            "X-Origin" to "https://music.youtube.com",
            "Origin" to "https://music.youtube.com",
            "Referer" to "https://music.youtube.com/"
        )

        when (type) {
            ClientType.WEB_REMIX -> {
                headers["User-Agent"] = WEB_UA
                headers["X-YouTube-Client-Name"] = WEB_REMIX_ID
                headers["X-YouTube-Client-Version"] = WEB_REMIX_VERSION
            }
            ClientType.IOS -> {
                headers["User-Agent"] = IOS_UA
                headers["X-YouTube-Client-Name"] = IOS_ID
                headers["X-YouTube-Client-Version"] = IOS_VERSION
            }
            ClientType.ANDROID -> {
                headers["User-Agent"] = ANDROID_UA
                headers["X-YouTube-Client-Name"] = ANDROID_ID
                headers["X-YouTube-Client-Version"] = ANDROID_VERSION
            }
        }
        return headers
    }

    // --- INTERCEPTOR: SOLO AUTH ---
    // Questo si occupa solo di aggiungere Cookie e Firma di sicurezza
    private val authInterceptor = Interceptor { chain ->
        val builder = chain.request().newBuilder()

        if (currentCookie.isNotBlank()) {
            val cleanCookie = currentCookie.trim().replace("\n", "").replace("\r", "")
            try {
                // 1. Cookie
                builder.addHeader("Cookie", cleanCookie)

                // 2. Authorization Hash (OuterTune Logic)
                val sapisid = extractSAPISID(cleanCookie)
                if (sapisid != null) {
                    val hash = getSAPISIDHASH(sapisid, "https://music.youtube.com")
                    builder.addHeader("Authorization", hash)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        chain.proceed(builder.build())
    }

    private fun extractSAPISID(cookie: String): String? =
        cookie.split("; ").find { it.startsWith("SAPISID=") }?.substringAfter("SAPISID=")

    private fun getSAPISIDHASH(sapisid: String, origin: String): String {
        val timestamp = System.currentTimeMillis() / 1000
        val input = "${timestamp} ${sapisid} ${origin}"
        val bytes = MessageDigest.getInstance("SHA-1").digest(input.toByteArray())
        val hash = bytes.joinToString("") { "%02x".format(it) }
        return "SAPISIDHASH ${timestamp}_${hash} SAPISID1PHASH ${timestamp}_${hash} SAPISID3PHASH ${timestamp}_${hash}"
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: YouTubeMusicApi = retrofit.create(YouTubeMusicApi::class.java)

    // --- BODY BUILDERS (Semplificati) ---

    fun createSearchBody(query: String) = createBody("WEB_REMIX", WEB_REMIX_VERSION).apply {
        addProperty("query", query)
        addProperty("params", "Eg-KAQwI")
    }

    fun createBrowseBody(browseId: String) = createBody("WEB_REMIX", WEB_REMIX_VERSION).apply {
        addProperty("browseId", browseId)
    }

    fun createIosPlayerBody(videoId: String) = createBody("IOS", IOS_VERSION, videoId)

    fun createAndroidPlayerBody(videoId: String) = createBody("ANDROID", ANDROID_VERSION, videoId)

    private fun createBody(name: String, version: String, videoId: String? = null): JsonObject {
        val json = JsonObject()
        val context = JsonObject()
        val client = JsonObject()

        client.addProperty("clientName", name)
        client.addProperty("clientVersion", version)
        client.addProperty("hl", "it")
        client.addProperty("gl", "IT")
        client.addProperty("utcOffsetMinutes", 0)

        if (name == "IOS") {
            client.addProperty("deviceMake", "Apple")
            client.addProperty("deviceModel", "iPhone14,5")
        }

        context.add("client", client)
        json.add("context", context)
        if (videoId != null) json.addProperty("videoId", videoId)

        return json
    }
}