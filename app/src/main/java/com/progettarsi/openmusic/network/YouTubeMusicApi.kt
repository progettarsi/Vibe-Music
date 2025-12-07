package com.progettarsi.openmusic.network

import com.google.gson.JsonObject
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface YouTubeMusicApi {
    @POST("youtubei/v1/search")
    suspend fun search(
        @Query("key") apiKey: String,
        @Body body: JsonObject
    ): JsonObject

    @POST("youtubei/v1/player")
    suspend fun player(
        @Query("key") apiKey: String,
        @Body body: JsonObject
    ): JsonObject

    // NUOVO ENDPOINT: Home Screen e Playlist
    @POST("youtubei/v1/browse")
    suspend fun browse(
        @Query("key") apiKey: String,
        @Body body: JsonObject
    ): JsonObject
}