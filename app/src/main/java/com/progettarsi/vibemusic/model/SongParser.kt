package com.progettarsi.vibemusic.model

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import android.util.Log

object SongParser {

    // --- 1. PARSING HOME ---
    fun parseHomeContent(jsonObject: JsonObject): List<MusicItem> {
        val contents = mutableListOf<MusicItem>()
        try {
            traverseJson(jsonObject, contents)
        } catch (e: Exception) {
            Log.e("SongParser", "Errore Home: ${e.message}")
        }
        return contents
    }

    // --- 2. PARSING SEARCH ---
    fun parseSearchResults(response: JsonObject): List<Song> {
        val songs = mutableListOf<Song>()
        try {
            val contents = response.get("contents")?.asJsonObject
                ?.get("tabbedSearchResultsRenderer")?.asJsonObject
                ?.get("tabs")?.asJsonArray
                ?.get(0)?.asJsonObject
                ?.get("tabRenderer")?.asJsonObject
                ?.get("content")?.asJsonObject
                ?.get("sectionListRenderer")?.asJsonObject
                ?.get("contents")?.asJsonArray

            contents?.forEach { section ->
                val musicShelf = section.asJsonObject.get("musicShelfRenderer")?.asJsonObject
                musicShelf?.get("contents")?.asJsonArray?.forEach { item ->
                    val itemObj = item.asJsonObject.get("musicResponsiveListItemRenderer")?.asJsonObject
                    if (itemObj != null) {
                        val parsedItem = parseResponsiveItem(itemObj)
                        if (parsedItem is Song) songs.add(parsedItem)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SongParser", "Errore Search: ${e.message}")
        }
        return songs
    }

    // --- 4. PARSING RADIO / UP NEXT (MIGLIORATO) ---
    fun parseNextContent(jsonObject: JsonObject): List<Song> {
        val songs = mutableListOf<Song>()
        try {
            // Invece di cercare un percorso fisso, scansioniamo tutto il JSON
            // alla ricerca di brani validi.
            traverseRadioJson(jsonObject, songs)

            // LOG DI DEBUG (Cerca questo nel Logcat!)
            Log.d("SongParser", "Trovati ${songs.size} brani per la Radio")
        } catch (e: Exception) {
            Log.e("SongParser", "Errore Parsing Radio: ${e.message}")
        }
        return songs
    }

    // Funzione ricorsiva che scava in profondità nel JSON
    private fun traverseRadioJson(element: JsonElement, results: MutableList<Song>) {
        if (element.isJsonArray) {
            element.asJsonArray.forEach { traverseRadioJson(it, results) }
        } else if (element.isJsonObject) {
            val obj = element.asJsonObject

            // CASO A: Brano standard della Radio (playlistPanelVideoRenderer)
            if (obj.has("playlistPanelVideoRenderer")) {
                parseSingleRadioItem(obj.getAsJsonObject("playlistPanelVideoRenderer"))?.let {
                    results.add(it)
                }
                return
            }

            // CASO B: Brano correlato/Automix (compactVideoRenderer)
            // Nota: Raccogliamo questi solo se non sono "playlist", per evitare duplicati strani
            if (obj.has("compactVideoRenderer")) {
                parseSingleRadioItem(obj.getAsJsonObject("compactVideoRenderer"))?.let {
                    results.add(it)
                }
                return
            }

            // Continua a cercare...
            obj.entrySet().forEach { traverseRadioJson(it.value, results) }
        }
    }

    // Helper per parsare il singolo elemento della Radio
    private fun parseSingleRadioItem(renderer: JsonObject): Song? {
        try {
            val videoId = renderer.get("videoId")?.asString ?: return null

            // Titolo (Gestisce sia simpleText che runs)
            val titleObj = renderer.get("title")?.asJsonObject
            val title = titleObj?.get("simpleText")?.asString
                ?: titleObj?.get("runs")?.asJsonArray?.get(0)?.asJsonObject?.get("text")?.asString
                ?: ""

            // Artista (Gestisce shortBylineText o longBylineText)
            val byline = renderer.get("longBylineText")?.asJsonObject
                ?: renderer.get("shortBylineText")?.asJsonObject

            val artist = byline?.get("runs")?.asJsonArray?.get(0)?.asJsonObject?.get("text")?.asString
                ?: ""

            val coverUrl = extractCover(renderer.get("thumbnail")?.asJsonObject)

            return Song(videoId, title, artist, coverUrl)
        } catch (e: Exception) {
            return null
        }
    }

    // --- 3. PARSING ALBUM & PLAYLIST ---
    fun parseCollectionContent(jsonObject: JsonObject): List<Song> {
        val songs = mutableListOf<Song>()
        try {
            val secondaryContents = jsonObject.get("contents")?.asJsonObject
                ?.get("twoColumnBrowseResultsRenderer")?.asJsonObject
                ?.get("secondaryContents")?.asJsonObject
                ?.get("sectionListRenderer")?.asJsonObject
                ?.get("contents")?.asJsonArray

            secondaryContents?.forEach { section ->
                val shelf = section.asJsonObject.get("musicPlaylistShelfRenderer")?.asJsonObject
                shelf?.get("contents")?.asJsonArray?.forEach { item ->
                    val itemObj = item.asJsonObject.get("musicResponsiveListItemRenderer")?.asJsonObject
                    if (itemObj != null) {
                        val parsedItem = parseResponsiveItem(itemObj)
                        if (parsedItem is Song) songs.add(parsedItem)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SongParser", "Errore Parsing Collection: ${e.message}")
        }
        return songs
    }

    // --- HELPER DI NAVIGAZIONE E PARSING GENERICO ---

    private fun traverseJson(element: JsonElement, results: MutableList<MusicItem>) {
        if (element.isJsonArray) {
            element.asJsonArray.forEach { traverseJson(it, results) }
        } else if (element.isJsonObject) {
            val obj = element.asJsonObject
            if (obj.has("musicResponsiveListItemRenderer")) {
                parseResponsiveItem(obj.get("musicResponsiveListItemRenderer").asJsonObject)?.let { results.add(it) }
                return
            }
            if (obj.has("musicTwoRowItemRenderer")) {
                parseTwoRowItem(obj.get("musicTwoRowItemRenderer").asJsonObject)?.let { results.add(it) }
                return
            }
            obj.entrySet().forEach { traverseJson(it.value, results) }
        }
    }

    private fun parseResponsiveItem(renderer: JsonObject): MusicItem? {
        val videoId = renderer.get("playlistItemData")?.asJsonObject?.get("videoId")?.asString
            ?: findVideoIdInFlexColumns(renderer)

        val title = getTextFromFlexColumn(renderer, 0) ?: return null
        val subtitle = extractSubtitle(renderer, 1)
        val coverUrl = extractCover(renderer.get("thumbnail")?.asJsonObject)

        if (videoId != null) {
            return Song(videoId, title, subtitle, coverUrl)
        }
        return null
    }

    private fun parseTwoRowItem(renderer: JsonObject): MusicItem? {
        val navEndpoint = renderer.get("navigationEndpoint")?.asJsonObject
        val title = renderer.get("title")?.asJsonObject?.get("runs")?.asJsonArray?.get(0)?.asJsonObject?.get("text")?.asString ?: ""
        val subtitle = renderer.get("subtitle")?.asJsonObject?.get("runs")?.asJsonArray?.joinToString("") { it.asJsonObject.get("text").asString } ?: ""
        val coverUrl = extractCover(renderer.get("thumbnailRenderer")?.asJsonObject)

        if (navEndpoint?.has("watchEndpoint") == true) {
            val watch = navEndpoint.get("watchEndpoint").asJsonObject
            val videoId = watch.get("videoId").asString
            val playlistId = watch.get("playlistId")?.asString
            if (playlistId != null) return YTCollection(playlistId, title, subtitle, coverUrl, CollectionType.MIX)
            return Song(videoId, title, subtitle, coverUrl)
        }

        if (navEndpoint?.has("browseEndpoint") == true) {
            val browseId = navEndpoint.get("browseEndpoint").asJsonObject.get("browseId").asString
            val type = when {
                browseId.startsWith("MPRE") -> CollectionType.ALBUM
                browseId.startsWith("VL") || browseId.startsWith("PL") -> CollectionType.PLAYLIST
                else -> CollectionType.UNKNOWN
            }
            return YTCollection(browseId, title, subtitle, coverUrl, type)
        }
        return null
    }

    // --- UTILS DI SUPPORTO ---
    private fun extractCover(thumbnailContainer: JsonObject?): String {
        // TENTATIVO 1: Struttura Semplice (Tipica della Radio/Coda)
        // Cerca direttamente l'array "thumbnails" dentro l'oggetto passato
        var thumbnails = thumbnailContainer?.get("thumbnails")?.asJsonArray

        // TENTATIVO 2: Struttura Complessa (Tipica della Home/Search)
        // Se non lo trova, scava dentro musicThumbnailRenderer
        if (thumbnails == null) {
            thumbnails = thumbnailContainer?.get("musicThumbnailRenderer")?.asJsonObject
                ?.get("thumbnail")?.asJsonObject?.get("thumbnails")?.asJsonArray
        }

        // Prendi l'ultima immagine (la più grande disponibile)
        var url = thumbnails?.lastOrNull()?.asJsonObject?.get("url")?.asString ?: ""

        // Ottimizzazione qualità (Sostituisce le versioni piccole con quelle HD)
        if (url.isNotEmpty()) {
            if (url.contains("w120")) {
                url = url.replace("w120", "w544").replace("h120", "h544")
            } else if (url.contains("w60")) {
                url = url.replace("w60", "w544").replace("h60", "h544")
            }
        }

        return url
    }

    private fun findVideoIdInFlexColumns(renderer: JsonObject): String? {
        renderer.get("flexColumns")?.asJsonArray?.forEach { col ->
            col.asJsonObject.get("musicResponsiveListItemFlexColumnRenderer")?.asJsonObject
                ?.get("text")?.asJsonObject?.get("runs")?.asJsonArray?.forEach { run ->
                    val vid = run.asJsonObject.get("navigationEndpoint")?.asJsonObject
                        ?.get("watchEndpoint")?.asJsonObject?.get("videoId")?.asString
                    if (vid != null) return vid
                }
        }
        return null
    }

    private fun getTextFromFlexColumn(renderer: JsonObject, index: Int): String? {
        val flexColumns = renderer.get("flexColumns")?.asJsonArray ?: return null
        if (index >= flexColumns.size()) return null
        return flexColumns.get(index)?.asJsonObject?.get("musicResponsiveListItemFlexColumnRenderer")?.asJsonObject
            ?.get("text")?.asJsonObject?.get("runs")?.asJsonArray?.get(0)?.asJsonObject?.get("text")?.asString
    }

    private fun extractSubtitle(renderer: JsonObject, index: Int): String {
        val flexColumns = renderer.get("flexColumns")?.asJsonArray ?: return ""
        if (index >= flexColumns.size()) return ""
        val runs = flexColumns.get(index)?.asJsonObject
            ?.get("musicResponsiveListItemFlexColumnRenderer")?.asJsonObject
            ?.get("text")?.asJsonObject?.get("runs")?.asJsonArray

        val fullText = runs?.joinToString("") { it.asJsonObject.get("text").asString } ?: ""
        return fullText.replace("Brano • ", "").replace("Video • ", "").replace("Song • ", "")
    }
}