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

    // Crea il body per la richiesta "Radio/Next"
    // Crea il body per la richiesta "Radio/Next"
    fun createNextBody(videoId: String) = createBody("WEB_REMIX", WEB_REMIX_VERSION, videoId).apply {
        addProperty("enablePersistentPlaylistPanel", true)
        addProperty("isAudioOnly", true)

        // --- IL TRUCCO DI OUTERTUNE ---
        // Per avviare una "Radio", dobbiamo fingere di essere dentro la playlist speciale
        // generata per quel video. Il prefisso è sempre "RDAMVM".
        addProperty("playlistId", "RDAMVM$videoId")

        // Questo parametro (Automix) è utile per confermare l'intenzione, lasciamolo.
        addProperty("params", "wAEB")
    }

    // --- INTERCEPTOR: SOLO AUTH ---
    // Questo si occupa solo di aggiungere Cookie e Firma di sicurezza
    // --- INTERCEPTOR: SOLO AUTH ---
    // Questo si occupa di aggiungere Cookie e Firma di sicurezza,
    // MA li ignora se viene richiesto l'accesso anonimo (per lo stream audio).
    private val authInterceptor = Interceptor { chain ->
        val request = chain.request()
        val builder = request.newBuilder()

        // 1. CONTROLLO SPECIALE: Verifichiamo se è stato richiesto l'accesso anonimo
        // Questo flag viene aggiunto dal Repository quando chiediamo lo stream audio.
        val isAnonymous = request.header("X-Anonymous") != null

        if (isAnonymous) {
            // Rimuoviamo l'header "finto" prima di inviare la richiesta a Google,
            // altrimenti la richiesta potrebbe essere malformata.
            builder.removeHeader("X-Anonymous")
        }

        // 2. LOGICA COOKIE CONDIZIONALE
        // Aggiungiamo il cookie e la firma SOLO se NON siamo in modalità anonima
        // e se abbiamo effettivamente un cookie salvato.
        if (!isAnonymous && currentCookie.isNotBlank()) {
            val cleanCookie = currentCookie.trim().replace("\n", "").replace("\r", "")
            try {
                // A. Aggiungi il Cookie
                builder.addHeader("Cookie", cleanCookie)

                // B. Aggiungi Authorization Hash (Logica OuterTune)
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