package com.progettarsi.vibemusic.model

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import android.util.Log

object SongParser {

    // --- 1. PARSING HOME (Restituisce Canzoni e Album/Playlist) ---
    fun parseHomeContent(jsonObject: JsonObject): List<MusicItem> {
        val contents = mutableListOf<MusicItem>()
        try {
            traverseJson(jsonObject, contents)
        } catch (e: Exception) {
            Log.e("SongParser", "Errore Home: ${e.message}")
        }
        return contents
    }

    // --- 2. PARSING SEARCH (Restituisce solo Canzoni per ora) ---
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
                    musicShelf.getAsJsonArray("contents").forEach { item ->
                        // Riutilizziamo la logica di parsing esistente
                        val itemObj = item.asJsonObject.getAsJsonObject("musicResponsiveListItemRenderer")
                        if (itemObj != null) {
                            val parsedItem = parseResponsiveItem(itemObj)
                            // Se è una canzone, la aggiungiamo alla lista
                            if (parsedItem is Song) {
                                songs.add(parsedItem)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SongParser", "Errore Search: ${e.message}")
        }
        return songs
    }

    // --- LOGICA RICORSIVA (Deep Search) ---
    private fun traverseJson(element: JsonElement, results: MutableList<MusicItem>) {
        if (element.isJsonArray) {
            element.asJsonArray.forEach { traverseJson(it, results) }
        } else if (element.isJsonObject) {
            val obj = element.asJsonObject

            // CASO A: Elemento a riga (Canzoni)
            if (obj.has("musicResponsiveListItemRenderer")) {
                val item = parseResponsiveItem(obj.getAsJsonObject("musicResponsiveListItemRenderer"))
                if (item != null) results.add(item)
                return
            }

            // CASO B: Card quadrata (Album, Playlist, Mix)
            if (obj.has("musicTwoRowItemRenderer")) {
                val item = parseTwoRowItem(obj.getAsJsonObject("musicTwoRowItemRenderer"))
                if (item != null) results.add(item)
                return
            }

            // Continua a cercare...
            obj.entrySet().forEach { traverseJson(it.value, results) }
        }
    }

    // --- PARSING SPECIFICO ---

    private fun parseResponsiveItem(renderer: JsonObject): MusicItem? {
        val videoId = renderer.getAsJsonObject("playlistItemData")?.getAsJsonPrimitive("videoId")?.asString
            ?: findVideoIdInFlexColumns(renderer)

        val title = getTextFromFlexColumn(renderer, 0) ?: return null
        val subtitle = extractSubtitle(renderer, 1)
        val coverUrl = extractCover(renderer.getAsJsonObject("thumbnail"))

        if (videoId != null) {
            return Song(videoId, title, subtitle, coverUrl)
        }
        return null
    }

    private fun parseTwoRowItem(renderer: JsonObject): MusicItem? {
        val navEndpoint = renderer.getAsJsonObject("navigationEndpoint")
        val title = renderer.getAsJsonObject("title")?.getAsJsonArray("runs")?.get(0)?.asJsonObject?.get("text")?.asString ?: ""
        val subtitle = renderer.getAsJsonObject("subtitle")?.getAsJsonArray("runs")?.joinToString("") { it.asJsonObject.get("text").asString } ?: ""
        val coverUrl = extractCover(renderer.getAsJsonObject("thumbnailRenderer"))

        // 1. È un MIX o una CANZONE?
        if (navEndpoint?.has("watchEndpoint") == true) {
            val videoId = navEndpoint.getAsJsonObject("watchEndpoint").get("videoId").asString
            val playlistId = navEndpoint.getAsJsonObject("watchEndpoint").get("playlistId")?.asString

            if (playlistId != null) {
                return YTCollection(playlistId, title, subtitle, coverUrl, CollectionType.MIX)
            }
            return Song(videoId, title, subtitle, coverUrl)
        }

        // 2. È un ALBUM o PLAYLIST?
        if (navEndpoint?.has("browseEndpoint") == true) {
            val browseId = navEndpoint.getAsJsonObject("browseEndpoint").get("browseId").asString

            val type = when {
                browseId.startsWith("MPRE") -> CollectionType.ALBUM
                browseId.startsWith("VL") || browseId.startsWith("PL") -> CollectionType.PLAYLIST
                else -> CollectionType.UNKNOWN
            }
            return YTCollection(browseId, title, subtitle, coverUrl, type)
        }

        return null
    }

    // --- 4. PARSING RADIO (Up Next) ---
    fun parseNextContent(jsonObject: JsonObject): List<Song> {
        val songs = mutableListOf<Song>()
        try {
            // Percorso: contents -> twoColumnWatchNextResults -> playlist -> playlistPanelRenderer
            val playlistPanel = jsonObject.getAsJsonObject("contents")
                ?.getAsJsonObject("twoColumnWatchNextResults")
                ?.getAsJsonObject("playlist")
                ?.getAsJsonObject("playlistPanelRenderer")

            playlistPanel?.getAsJsonArray("contents")?.forEach { item ->
                val renderer = item.asJsonObject.getAsJsonObject("playlistPanelVideoRenderer")
                if (renderer != null) {
                    val videoId = renderer.get("videoId")?.asString ?: ""

                    // Titolo
                    val titleObj = renderer.getAsJsonObject("title")
                    val title = titleObj?.getAsJsonPrimitive("simpleText")?.asString
                        ?: titleObj?.getAsJsonArray("runs")?.get(0)?.asJsonObject?.get("text")?.asString
                        ?: ""

                    // Artista
                    val longByline = renderer.getAsJsonObject("longBylineText")
                    val artist = longByline?.getAsJsonArray("runs")?.get(0)?.asJsonObject?.get("text")?.asString
                        ?: ""

                    val coverUrl = extractCover(renderer.getAsJsonObject("thumbnail"))

                    if (videoId.isNotEmpty()) {
                        songs.add(Song(videoId, title, artist, coverUrl))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SongParser", "Errore Parsing Radio: ${e.message}")
        }
        return songs
    }

    fun parseCollectionContent(jsonObject: JsonObject): List<Song> {
        val songs = mutableListOf<Song>()
        try {
            // Cerchiamo la lista di canzoni.
            // Di solito è in: contents -> twoColumnBrowseResultsRenderer -> secondaryContents -> sectionListRenderer -> contents -> musicPlaylistShelfRenderer
            val secondaryContents = jsonObject.getAsJsonObject("contents")
                ?.getAsJsonObject("twoColumnBrowseResultsRenderer")
                ?.getAsJsonObject("secondaryContents")
                ?.getAsJsonObject("sectionListRenderer")
                ?.getAsJsonArray("contents")

            secondaryContents?.forEach { section ->
                val shelf = section.asJsonObject.getAsJsonObject("musicPlaylistShelfRenderer")
                if (shelf != null) {
                    shelf.getAsJsonArray("contents")?.forEach { item ->
                        val itemObj = item.asJsonObject.getAsJsonObject("musicResponsiveListItemRenderer")
                        if (itemObj != null) {
                            val parsedItem = parseResponsiveItem(itemObj)
                            if (parsedItem is Song) {
                                songs.add(parsedItem)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SongParser", "Errore Parsing Collection: ${e.message}")
        }
        return songs
    }

    // --- HELPER FUNZIONI ---
    private fun extractCover(thumbnailContainer: JsonObject?): String {
        val thumbnails = thumbnailContainer?.getAsJsonObject("musicThumbnailRenderer")
            ?.getAsJsonObject("thumbnail")?.getAsJsonArray("thumbnails")
        var url = thumbnails?.lastOrNull()?.asJsonObject?.getAsJsonPrimitive("url")?.asString ?: ""
        if (url.contains("w120")) url = url.replace("w120", "w544").replace("h120", "h544")
        return url
    }

    private fun findVideoIdInFlexColumns(renderer: JsonObject): String? {
        renderer.getAsJsonArray("flexColumns")?.forEach { col ->
            col.asJsonObject.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                ?.getAsJsonObject("text")?.getAsJsonArray("runs")?.forEach { run ->
                    val vid = run.asJsonObject.getAsJsonObject("navigationEndpoint")
                        ?.getAsJsonObject("watchEndpoint")?.getAsJsonPrimitive("videoId")?.asString
                    if (vid != null) return vid
                }
        }
        return null
    }

    private fun getTextFromFlexColumn(renderer: JsonObject, index: Int): String? {
        val flexColumns = renderer.getAsJsonArray("flexColumns") ?: return null
        if (index >= flexColumns.size()) return null
        return flexColumns.get(index)?.asJsonObject?.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
            ?.getAsJsonObject("text")?.getAsJsonArray("runs")?.get(0)?.asJsonObject?.get("text")?.asString
    }

    // In SongParser.kt

    private fun extractSubtitle(renderer: JsonObject, index: Int): String {
        val flexColumns = renderer.getAsJsonArray("flexColumns") ?: return ""
        if (index >= flexColumns.size()) return ""

        val runs = flexColumns.get(index)?.asJsonObject
            ?.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
            ?.getAsJsonObject("text")?.getAsJsonArray("runs")

        // --- MODIFICA QUI ---

        // PRIMA: Prendevi solo il primo elemento ("Brano")
        // return runs?.firstOrNull()?.asJsonObject?.get("text")?.asString ?: ""

        // ORA: Uniamo tutti i pezzi ("Brano • Artista • Album")
        val fullText = runs?.joinToString("") { it.asJsonObject.get("text").asString } ?: ""

        // (OPZIONALE) PULIZIA: Rimuoviamo "Brano • " per lasciare solo l'artista pulito
        return fullText
            .replace("Brano • ", "")
            .replace("Video • ", "")
            .replace("Song • ", "") // Caso inglese
    }
}