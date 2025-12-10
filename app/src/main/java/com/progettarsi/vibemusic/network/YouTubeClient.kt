package com.progettarsi.vibemusic.network

import com.google.gson.JsonObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object YouTubeClient {
    private const val BASE_URL = "https://music.youtube.com/"
    const val API_KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"

    // Versione del client WEB_REMIX stabile e funzionante
    private const val CLIENT_VERSION = "1.20230911.05.00"

    var currentCookie: String = ""

    private val headerInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.45 Safari/537.36")
            .addHeader("Referer", "https://music.youtube.com/")
            .addHeader("Origin", "https://music.youtube.com")
            .addHeader("Content-Type", "application/json")
            .apply {
                if (currentCookie.isNotBlank()) {
                    // FIX: Rimuoviamo Newline, Carriage Return e il carattere '…' (0x2026)
                    // Inoltre controlliamo che i caratteri siano ASCII standard per evitare crash di OkHttp
                    val cleanCookie = currentCookie
                        .trim()
                        .replace("\n", "")
                        .replace("\r", "")
                        .replace("…", "") // Rimuove i tre puntini se copiati per sbaglio

                    // Controllo di sicurezza: OkHttp accetta solo caratteri ASCII nei valori degli header
                    // Se c'è ancora spazzatura, usiamo un try-catch per non far crashare l'app
                    try {
                        addHeader("Cookie", cleanCookie)
                        addHeader("X-Goog-AuthUser", "0")
                    } catch (e: Exception) {
                        e.printStackTrace() // Loggiamo l'errore ma non crashiamo
                    }
                }
            }
            .build()
        chain.proceed(request)
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(headerInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: YouTubeMusicApi = retrofit.create(YouTubeMusicApi::class.java)

    fun createSearchBody(query: String): JsonObject {
        return createBody("WEB_REMIX", CLIENT_VERSION, null).apply {
            addProperty("query", query)
            addProperty("params", "Eg-KAQwI")
        }
    }

    fun createBrowseBody(browseId: String): JsonObject {
        return createBody("WEB_REMIX", CLIENT_VERSION, null).apply {
            addProperty("browseId", browseId)
        }
    }

    fun createIosPlayerBody(videoId: String): JsonObject {
        return createBody("IOS", "19.34.1", videoId)
    }

    fun createAndroidPlayerBody(videoId: String): JsonObject {
        return createBody("ANDROID", "19.34.35", videoId)
    }

    private fun createBody(name: String, version: String, videoId: String?): JsonObject {
        val json = JsonObject()
        val context = JsonObject()
        val client = JsonObject()
        client.addProperty("clientName", name)
        client.addProperty("clientVersion", version)
        client.addProperty("hl", "it")
        client.addProperty("gl", "IT")
        // FIX: YouTube richiede spesso l'offset orario
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