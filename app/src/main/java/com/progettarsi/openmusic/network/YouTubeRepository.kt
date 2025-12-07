package com.progettarsi.openmusic.network

import android.util.Log
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

    suspend fun getStreamUrl(videoId: String): String? {
        return withContext(Dispatchers.IO) {
            // STRATEGIA OUTERTUNE: Prova a cascata
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
            if (playability?.get("status")?.asString != "OK") return null

            val streamingData = response.getAsJsonObject("streamingData") ?: return null
            val formats = streamingData.getAsJsonArray("adaptiveFormats") ?: return null

            for (element in formats) {
                val obj = element.asJsonObject
                val mime = obj.get("mimeType")?.asString ?: ""
                // Cerchiamo audio con URL diretto (non cifrato)
                if (mime.contains("audio") && obj.has("url")) {
                    return obj.get("url").asString
                }
            }
        } catch (e: Exception) {
            Log.e("YouTubeRepository", "Errore: ${e.message}")
        }
        return null
    }
}