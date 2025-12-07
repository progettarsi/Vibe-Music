package com.progettarsi.openmusic.network

import android.util.Log
import com.google.gson.JsonObject
import com.progettarsi.openmusic.model.Song
import com.progettarsi.openmusic.model.SongParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class YouTubeRepository {

    suspend fun searchSongs(query: String): List<Song> {
        return withContext(Dispatchers.IO) {
            try {
                val body = YouTubeClient.createSearchBody(query)
                val response = YouTubeClient.api.search(YouTubeClient.API_KEY, body)
                SongParser.parseSearchResults(response)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    suspend fun getHomeContent(): JsonObject? {
        return withContext(Dispatchers.IO) {
            try {
                // FEwhat_to_listen è l'ID per la pagina "Home" personalizzata
                val body = YouTubeClient.createBrowseBody("FEwhat_to_listen")
                YouTubeClient.api.browse(YouTubeClient.API_KEY, body)
            } catch (e: Exception) {
                Log.e("YouTubeRepository", "Errore nel caricare la Home: ${e.message}")
                null
            }
        }
    }

    suspend fun getStreamUrl(videoId: String): String? {
        return withContext(Dispatchers.IO) {
            // Priorità iOS -> Android
            Log.d("YouTubeRepository", "Provo stream con client iOS...")
            var url = tryGetStream(videoId, isIos = true)

            if (url == null) {
                Log.w("YouTubeRepository", "iOS fallito, provo Android...")
                url = tryGetStream(videoId, isIos = false)
            }

            url
        }
    }

    private suspend fun tryGetStream(videoId: String, isIos: Boolean): String? {
        try {
            val body = if (isIos) YouTubeClient.createIosPlayerBody(videoId)
            else YouTubeClient.createAndroidPlayerBody(videoId)

            val response = YouTubeClient.api.player(YouTubeClient.API_KEY, body)

            val playability = response.getAsJsonObject("playabilityStatus")
            if (playability?.get("status")?.asString != "OK") {
                Log.e("YouTubeRepository", "Non riproducibile: ${playability?.get("status")?.asString}")
                return null
            }

            val streamingData = response.getAsJsonObject("streamingData") ?: return null
            val formats = streamingData.getAsJsonArray("adaptiveFormats") ?: return null

            for (element in formats) {
                val obj = element.asJsonObject
                if (obj.get("mimeType")?.asString?.contains("audio") == true && obj.has("url")) {
                    return obj.get("url").asString
                }
            }
        } catch (e: Exception) {
            Log.e("YouTubeRepository", "Errore stream: ${e.message}")
        }
        return null
    }
}