package com.progettarsi.vibemusic.network

import com.google.gson.JsonObject
import com.progettarsi.vibemusic.model.MusicItem
import com.progettarsi.vibemusic.model.Song
import com.progettarsi.vibemusic.model.SongParser
import com.progettarsi.vibemusic.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class YouTubeRepository {

    // --- 1. RICERCA (Usa client WEB_REMIX) ---
    suspend fun searchSongs(query: String): List<Song> {
        return withContext(Dispatchers.IO) {
            try {
                val headers = YouTubeClient.getClientHeaders(YouTubeClient.ClientType.WEB_REMIX)
                val body = YouTubeClient.createSearchBody(query)
                val response = YouTubeClient.api.search(YouTubeClient.API_KEY, headers, body)
                SongParser.parseSearchResults(response)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    // --- 2. HOME (Usa client WEB_REMIX) ---
    suspend fun getHomeContent(): Resource<List<MusicItem>> {
        return withContext(Dispatchers.IO) {
            try {
                val browseId = if (YouTubeClient.currentCookie.isNotEmpty()) "FEwhat_to_listen" else "FEmusic_new_releases"

                val headers = YouTubeClient.getClientHeaders(YouTubeClient.ClientType.WEB_REMIX)
                val body = YouTubeClient.createBrowseBody(browseId)

                val response = YouTubeClient.api.browse(YouTubeClient.API_KEY, headers, body)

                val parsedData = SongParser.parseHomeContent(response)

                if (parsedData.isNotEmpty()) {
                    Resource.Success(parsedData)
                } else {
                    Resource.Error("Nessun contenuto trovato")
                }
            } catch (e: IOException) {
                Resource.Error("Errore di connessione")
            } catch (e: Exception) {
                Resource.Error("Errore: ${e.message}")
            }
        }
    }

    // --- 5. RADIO / UP NEXT ---
    suspend fun getRadio(videoId: String): List<Song> {
        return withContext(Dispatchers.IO) {
            try {
                val headers = YouTubeClient.getClientHeaders(YouTubeClient.ClientType.WEB_REMIX)
                val body = YouTubeClient.createNextBody(videoId)
                val response = YouTubeClient.api.next(YouTubeClient.API_KEY, headers, body)
                SongParser.parseNextContent(response)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    suspend fun getPlaylistSongs(browseId: String): List<Song> {
        return withContext(Dispatchers.IO) {
            try {
                val headers = YouTubeClient.getClientHeaders(YouTubeClient.ClientType.WEB_REMIX)
                // Usiamo createBrowseBody che hai già in YouTubeClient
                val body = YouTubeClient.createBrowseBody(browseId)

                val response = YouTubeClient.api.browse(YouTubeClient.API_KEY, headers, body)

                // Usiamo il nuovo parser
                SongParser.parseCollectionContent(response)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    // --- 3. STREAM AUDIO (Usa client IOS/ANDROID) ---
    suspend fun getStreamUrl(videoId: String): String? {
        return withContext(Dispatchers.IO) {
            // CAMBIAMENTO QUI: Proviamo prima ANDROID (isIos = false)
            // È più affidabile per la riproduzione su dispositivi Android reali
            var url = tryGetStream(videoId, isIos = false)

            // Fallback su iOS se Android fallisce
            if (url == null) {
                url = tryGetStream(videoId, isIos = true)
            }
            url
        }
    }

    private suspend fun tryGetStream(videoId: String, isIos: Boolean): String? {
        try {
            val body: JsonObject
            // MODIFICA 1: Definiamo la variabile come MutableMap
            val headers: MutableMap<String, String>

            if (isIos) {
                body = YouTubeClient.createIosPlayerBody(videoId)
                // MODIFICA 2: Usiamo .toMutableMap() per renderla modificabile
                headers = YouTubeClient.getClientHeaders(YouTubeClient.ClientType.IOS).toMutableMap()
            } else {
                body = YouTubeClient.createAndroidPlayerBody(videoId)
                headers = YouTubeClient.getClientHeaders(YouTubeClient.ClientType.ANDROID).toMutableMap()
            }

            // MODIFICA 3: Ora questo funziona perché la mappa è mutabile
            headers["X-Anonymous"] = "true"

            val response = YouTubeClient.api.player(YouTubeClient.API_KEY, headers, body)

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
            e.printStackTrace()
        }
        return null
    }
}