package com.progettarsi.openmusic.network

import com.google.gson.JsonObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient

object YouTubeClient {
    private const val BASE_URL = "https://music.youtube.com/"

    // Chiave pubblica Browser (Standard per OuterTune/ViMusic)
    const val API_KEY = "AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8"

    private val headerInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            // User-Agent identico a quello messo in MusicService!
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.45 Safari/537.36")
            .addHeader("Referer", "https://music.youtube.com/")
            .addHeader("Content-Type", "application/json")
            // .addHeader("Cookie", "INCOLLA_QUI_IL_COOKIE_SE_VUOI_SBLOCCARE_TUTTO")
            .build()
        chain.proceed(request)
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(headerInterceptor)
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: YouTubeMusicApi = retrofit.create(YouTubeMusicApi::class.java)

    fun createSearchBody(query: String): JsonObject {
        return createBody("WEB_REMIX", "1.20230102.01.00", null)
            .apply {
                addProperty("query", query)
                addProperty("params", "Eg-KAQwI") // Filtro Songs
            }
    }

    // Client iOS (Spesso non criptato)
    fun createIosPlayerBody(videoId: String): JsonObject {
        return createBody("IOS", "19.29.1", videoId)
    }

    // Client Android (Alta qualità, fallback)
    fun createAndroidPlayerBody(videoId: String): JsonObject {
        return createBody("ANDROID", "17.34.35", videoId)
    }

    private fun createBody(name: String, version: String, videoId: String?): JsonObject {
        val json = JsonObject()
        val context = JsonObject()
        val client = JsonObject()
        client.addProperty("clientName", name)
        client.addProperty("clientVersion", version)
        client.addProperty("hl", "it")
        client.addProperty("gl", "IT")
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