package com.progettarsi.openmusic.model

import com.google.gson.JsonObject

object SongParser {
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
                val musicShelf = section.asJsonObject.getAsJsonObject("musicShelfRenderer")
                if (musicShelf != null) {
                    musicShelf.getAsJsonArray("contents").forEach {
                        parseSingleSong(it.asJsonObject)?.let { song -> songs.add(song) }
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
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
                    val text = run.asJsonObject.getAsJsonPrimitive("text").asString
                    if (!listOf("Video", "Song", "Brano", "Single", " • ").contains(text)) {
                        artist = text
                        break
                    }
                }
            }

            // Cover
            val thumbnails = renderer.getAsJsonObject("thumbnail")?.getAsJsonObject("musicThumbnailRenderer")?.getAsJsonObject("thumbnail")?.getAsJsonArray("thumbnails")
            var coverUrl = ""
            if (thumbnails != null && thumbnails.size() > 0) {
                coverUrl = thumbnails.last().asJsonObject.getAsJsonPrimitive("url").asString
                if (coverUrl.contains("w120")) coverUrl = coverUrl.replace("w120", "w544").replace("h120", "h544")
            }

            return Song(videoId, title, artist, coverUrl)
        } catch (e: Exception) { return null }
    }
}