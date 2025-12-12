package com.progettarsi.vibemusic.network

import com.google.gson.JsonObject
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import retrofit2.http.Query

interface YouTubeMusicApi {
    @POST("youtubei/v1/search")
    suspend fun search(
        @Query("key") apiKey: String,
        @HeaderMap headers: Map<String, String>, // <--- AGGIUNTO
        @Body body: JsonObject
    ): JsonObject

    @POST("youtubei/v1/player")
    suspend fun player(
        @Query("key") apiKey: String,
        @HeaderMap headers: Map<String, String>, // <--- AGGIUNTO
        @Body body: JsonObject
    ): JsonObject

    @POST("youtubei/v1/browse")
    suspend fun browse(
        @Query("key") apiKey: String,
        @HeaderMap headers: Map<String, String>, // <--- AGGIUNTO
        @Body body: JsonObject
    ): JsonObject
    @POST("youtubei/v1/next")
    suspend fun next(
        @Query("key") apiKey: String,
        @HeaderMap headers: Map<String, String>,
        @Body body: JsonObject
    ): JsonObject
}