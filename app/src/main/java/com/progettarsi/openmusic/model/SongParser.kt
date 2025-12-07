package com.progettarsi.openmusic.model

import com.google.gson.JsonObject
import android.util.Log
import com.google.gson.JsonElement

object SongParser {

    // Funzione esistente (Search)
    fun parseSearchResults(response: JsonObject): List<Song> {
        val songs = mutableListOf<Song>()
        try {
            val contents = response.getAsJsonObject("contents")
                ?.getAsJsonObject("tabbedSearchResultsRenderer")
                ?.getAsJsonArray("tabs")
                ?.get(0)?.asJsonObject
                ?.getAsJsonObject("tabRenderer")
                ?.getAsJsonObject("content")
                ?.getAsJsonObject("sectionListRenderer")
                ?.getAsJsonArray("contents")

            contents?.forEach { section ->
                section.asJsonObject.getAsJsonObject("musicShelfRenderer")?.let { musicShelf ->
                    musicShelf.getAsJsonArray("contents").forEach {
                        parseSingleSong(it.asJsonObject)?.let { song -> songs.add(song) }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SongParser", "Error parsing search results: ${e.message}")
        }
        return songs
    }

    private fun parseSingleSong(item: JsonObject): Song? {
        try {
            val renderer = item.getAsJsonObject("musicResponsiveListItemRenderer") ?: return null

            // ID
            val videoId = renderer.getAsJsonObject("playlistItemData")?.getAsJsonPrimitive("videoId")?.asString
                ?: renderer.getAsJsonArray("flexColumns")?.get(0)?.asJsonObject?.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")?.getAsJsonObject("text")?.getAsJsonObject("navigationEndpoint")?.getAsJsonObject("watchEndpoint")?.getAsJsonPrimitive("videoId")?.asString
                ?: return null

            // Titolo
            val title = renderer.getAsJsonArray("flexColumns")?.get(0)?.asJsonObject?.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")?.getAsJsonObject("text")?.getAsJsonArray("runs")?.get(0)?.asJsonObject?.getAsJsonPrimitive("text")?.asString ?: "Sconosciuto"

            // Artista (FIX: Salta "Video", "Song", "Brano", "•")
            val subtitleRuns = renderer.getAsJsonArray("flexColumns")?.get(1)?.asJsonObject?.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")?.getAsJsonObject("text")?.getAsJsonArray("runs")
            var artist = "Sconosciuto"
            if (subtitleRuns != null) {
                for (run in subtitleRuns) {
                    val text = run.asJsonObject.getAsJsonPrimitive("text")?.asString ?: ""
                    if (!listOf("Video", "Song", "Brano", "Single", " • ").contains(text) && text.isNotBlank()) {
                        artist = text
                        break
                    }
                }
            }

            // Cover
            val thumbnails = renderer.getAsJsonObject("thumbnail")?.getAsJsonObject("musicThumbnailRenderer")?.getAsJsonObject("thumbnail")?.getAsJsonArray("thumbnails")
            var coverUrl = ""
            if (thumbnails != null && thumbnails.size() > 0) {
                coverUrl = thumbnails.last().asJsonObject.getAsJsonPrimitive("url")?.asString ?: ""
                if (coverUrl.contains("w120")) coverUrl = coverUrl.replace("w120", "w544").replace("h120", "h544")
            }

            return Song(videoId, title, artist, coverUrl)
        } catch (e: Exception) {
            Log.e("SongParser", "Error parsing single song: ${e.message}")
            return null
        }
    }

    // Parsing della Home Screen
    fun parseHomeContent(jsonObject: JsonObject): List<Any> {
        val contents = mutableListOf<Any>()
        try {
            val sections = jsonObject.getAsJsonObject("contents")
                ?.getAsJsonObject("singleColumnBrowseResultsRenderer")
                ?.getAsJsonArray("tabs")
                ?.get(0)?.asJsonObject
                ?.getAsJsonObject("tabRenderer")
                ?.getAsJsonObject("content")
                ?.getAsJsonObject("sectionListRenderer")
                ?.getAsJsonArray("contents") ?: return emptyList()

            for (section in sections) {
                val shelf = section.asJsonObject.getAsJsonObject("musicShelfRenderer")
                    ?: section.asJsonObject.getAsJsonObject("itemSectionRenderer")
                        ?.getAsJsonArray("contents")?.get(0)?.asJsonObject
                        ?.getAsJsonObject("musicShelfRenderer")

                shelf?.let {
                    val items = it.getAsJsonArray("contents")
                    items?.forEach { item ->
                        item.asJsonObject.getAsJsonObject("musicTwoRowItemRenderer")?.let { card ->
                            parseTwoRowItem(card)?.let { parsedItem -> contents.add(parsedItem) }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SongParser", "Errore nel parsing della Home: ${e.message}")
        }
        return contents
    }

    // Parsa l'elemento generico (Song o Playlist)
    private fun parseTwoRowItem(jsonObject: JsonObject): Any? {
        val title = jsonObject.getAsJsonObject("title")
            ?.getAsJsonArray("runs")?.get(0)?.asJsonObject
            ?.get("text")?.asString ?: "Titolo Sconosciuto"

        // FIX CRITICO: Logica ultra-difensiva per evitare il mismatch Int/String
        val subtitleRuns = jsonObject.getAsJsonObject("subtitle")?.getAsJsonArray("runs")

        val subtitle = subtitleRuns?.mapNotNull { runElement ->
            if (runElement.isJsonObject) {
                val textElement: JsonElement? = runElement.asJsonObject.get("text")

                if (textElement != null && textElement.isJsonPrimitive) {
                    // FIX: Chiamiamo .toString() per forzare la conversione a stringa (sicuro anche per numeri)
                    // e poi rimuoviamo le virgolette se ci sono (solo sulle stringhe JSON)
                    val rawText = textElement.asJsonPrimitive.toString()
                    rawText.removePrefix("\"").removeSuffix("\"").takeIf { it.isNotBlank() }
                } else {
                    null
                }
            } else {
                null
            }
        }?.joinToString(" • ")

        val thumbnail = jsonObject.getAsJsonObject("thumbnailRenderer")
            ?.getAsJsonObject("musicThumbnailRenderer")
            ?.getAsJsonObject("thumbnail")
            ?.getAsJsonArray("thumbnails")?.last()?.asJsonObject
            ?.get("url")?.asString ?: ""

        val navigationEndpoint = jsonObject.getAsJsonObject("navigationEndpoint")

        if (navigationEndpoint?.has("watchEndpoint") == true) {
            // È una Canzone
            val videoId = navigationEndpoint.getAsJsonObject("watchEndpoint")
                ?.get("videoId")?.asString ?: return null

            return Song(
                title = title,
                artist = subtitle?.split(" • ")?.get(0) ?: "Artista Sconosciuto",
                videoId = videoId,
                coverUrl = thumbnail
            )
        } else if (navigationEndpoint?.has("browseEndpoint") == true) {
            // È una Playlist o Album
            val browseEndpoint = navigationEndpoint.getAsJsonObject("browseEndpoint")
            val playlistId = browseEndpoint.get("browseId")?.asString ?: return null

            return Playlist(
                title = title,
                subtitle = subtitle,
                playlistId = playlistId,
                thumbnails = thumbnail
            )
        }
        return null
    }
}