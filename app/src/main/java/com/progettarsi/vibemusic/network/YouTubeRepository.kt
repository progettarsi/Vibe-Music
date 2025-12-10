package com.progettarsi.vibemusic.network

import android.util.Log
import com.google.gson.JsonObject
import com.progettarsi.vibemusic.model.Song
import com.progettarsi.vibemusic.model.SongParser
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
                // --- LOGICA SMART ---
                // Se c'è il cookie -> Home Personale ("FEwhat_to_listen")
                // Se NON c'è cookie -> Classifiche ("FEmusic_charts")
                // NOTA: "FEmusic_explore" era vuoto perché usa layout a griglia non supportati dal parser.
                // "FEmusic_charts" invece usa le liste (Shelves) che il parser già riconosce!
                val browseId = if (YouTubeClient.currentCookie.isNotEmpty()) "FEwhat_to_listen" else "FEmusic_new_releases"

                Log.d("YouTubeRepository", "Caricamento Home con ID: $browseId")

                val body = YouTubeClient.createBrowseBody(browseId)
                YouTubeClient.api.browse(YouTubeClient.API_KEY, body)
            } catch (e: Exception) {
                Log.e("YouTubeRepository", "Errore nel caricare la Home: ${e.message}")
                null
            }
        }
    }

    suspend fun getStreamUrl(videoId: String): String? {
        return withContext(Dispatchers.IO) {
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
            // Ignora errore
        }
        return null
    }
}