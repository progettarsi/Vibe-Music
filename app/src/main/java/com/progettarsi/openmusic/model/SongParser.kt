package com.progettarsi.openmusic.model

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import android.util.Log

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

            // 1. VIDEO ID
            // Tentativo principale: dal playlistItemData (più affidabile)
            // Fallback: dal navigation endpoint dell'immagine o del testo
            val videoId = renderer.getAsJsonObject("playlistItemData")?.getAsJsonPrimitive("videoId")?.asString
                ?: findVideoIdInFlexColumns(renderer)
                ?: return null

            // 2. TITOLO
            // Prende il primo run di testo della prima colonna
            val title = getTextFromFlexColumn(renderer, 0) ?: "Titolo Sconosciuto"

            // 3. ARTISTA (Logica Migliorata)
            // Cerca nella seconda colonna (sottotitoli).
            // La logica è: l'artista è solitamente il primo elemento che ha un 'navigationEndpoint' (cliccabile)
            // oppure, se nessuno è cliccabile, prendiamo il primo elemento che non è un separatore (•).
            val artist = extractArtistFromFlexColumn(renderer, 1)

            // 4. COVER
            val thumbnails = renderer.getAsJsonObject("thumbnail")?.getAsJsonObject("musicThumbnailRenderer")?.getAsJsonObject("thumbnail")?.getAsJsonArray("thumbnails")
            var coverUrl = ""
            if (thumbnails != null && thumbnails.size() > 0) {
                // Prendiamo l'ultima immagine (risoluzione più alta)
                coverUrl = thumbnails.last().asJsonObject.getAsJsonPrimitive("url")?.asString ?: ""
                // Hack per forzare alta risoluzione se YouTube ci dà quella bassa
                if (coverUrl.contains("w120")) {
                    coverUrl = coverUrl.replace("w120", "w544").replace("h120", "h544")
                } else if (coverUrl.contains("w60")) {
                    coverUrl = coverUrl.replace("w60", "w544").replace("h60", "h544")
                }
            }

            return Song(videoId, title, artist, coverUrl)
        } catch (e: Exception) {
            Log.e("SongParser", "Error parsing single song: ${e.message}")
            return null
        }
    }

    // --- HELPER DI PARSING ---

    private fun findVideoIdInFlexColumns(renderer: JsonObject): String? {
        val flexColumns = renderer.getAsJsonArray("flexColumns") ?: return null
        // Cerca in tutte le colonne un watchEndpoint
        flexColumns.forEach { col ->
            val runs = col.asJsonObject.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
                ?.getAsJsonObject("text")?.getAsJsonArray("runs")

            runs?.forEach { run ->
                val videoId = run.asJsonObject.getAsJsonObject("navigationEndpoint")
                    ?.getAsJsonObject("watchEndpoint")?.getAsJsonPrimitive("videoId")?.asString
                if (videoId != null) return videoId
            }
        }
        return null
    }

    private fun getTextFromFlexColumn(renderer: JsonObject, index: Int): String? {
        return renderer.getAsJsonArray("flexColumns")
            ?.get(index)?.asJsonObject
            ?.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
            ?.getAsJsonObject("text")
            ?.getAsJsonArray("runs")
            ?.get(0)?.asJsonObject
            ?.getAsJsonPrimitive("text")?.asString
    }

    private fun extractArtistFromFlexColumn(renderer: JsonObject, index: Int): String {
        val runs = renderer.getAsJsonArray("flexColumns")
            ?.get(index)?.asJsonObject
            ?.getAsJsonObject("musicResponsiveListItemFlexColumnRenderer")
            ?.getAsJsonObject("text")
            ?.getAsJsonArray("runs") ?: return "Sconosciuto"

        // Strategia 1: Cerca il primo elemento che ha un link di navigazione di tipo "BROWSE" (di solito porta al canale artista)
        for (run in runs) {
            val obj = run.asJsonObject
            val hasBrowseEndpoint = obj.getAsJsonObject("navigationEndpoint")?.has("browseEndpoint") == true
            val text = obj.getAsJsonPrimitive("text")?.asString ?: ""

            // Se ha un link ed è un testo valido, è quasi certamente l'artista
            if (hasBrowseEndpoint && text.isNotBlank()) {
                return text
            }
        }

        // Strategia 2 (Fallback): Se nessuno ha link, prendi il primo testo che non è un punto o una parola chiave di struttura
        for (run in runs) {
            val text = run.asJsonObject.getAsJsonPrimitive("text")?.asString ?: ""
            // Ignoriamo i separatori e le etichette di durata (es. "3:45")
            if (text.isNotBlank() && text != " • " && !text.contains(":")) {
                // Filtro extra per evitare "Song" o "Video" solo se non abbiamo trovato nulla di meglio
                if (text != "Song" && text != "Video" && text != "Brano") {
                    return text
                }
            }
        }

        return "Artista Sconosciuto"
    }

    // --- PARSING HOME (Invariato o quasi) ---
    fun parseHomeContent(jsonObject: JsonObject): List<Any> {
        val contents = mutableListOf<Any>()
        try {
            val tabs = jsonObject.getAsJsonObject("contents")?.getAsJsonObject("singleColumnBrowseResultsRenderer")?.getAsJsonArray("tabs")
            val sectionList = tabs?.get(0)?.asJsonObject?.getAsJsonObject("tabRenderer")?.getAsJsonObject("content")?.getAsJsonObject("sectionListRenderer")?.getAsJsonArray("contents")

            if (sectionList == null) return emptyList()

            for (section in sectionList) {
                val shelf = section.asJsonObject.getAsJsonObject("musicShelfRenderer")
                    ?: section.asJsonObject.getAsJsonObject("itemSectionRenderer")
                        ?.getAsJsonArray("contents")?.get(0)?.asJsonObject
                        ?.getAsJsonObject("musicShelfRenderer")

                shelf?.let {
                    it.getAsJsonArray("contents")?.forEach { item ->
                        // Supporto per vari tipi di card
                        val twoRow = item.asJsonObject.getAsJsonObject("musicTwoRowItemRenderer")
                        val responsive = item.asJsonObject.getAsJsonObject("musicResponsiveListItemRenderer")

                        if (twoRow != null) {
                            parseTwoRowItem(twoRow)?.let { parsed -> contents.add(parsed) }
                        } else if (responsive != null) {
                            // A volte nella home ci sono liste responsive (come nella ricerca)
                            parseSingleSong(item.asJsonObject)?.let { parsed -> contents.add(parsed) }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SongParser", "Errore Parsing Home: ${e.message}")
        }
        return contents
    }

    private fun parseTwoRowItem(jsonObject: JsonObject): Any? {
        val title = jsonObject.getAsJsonObject("title")?.getAsJsonArray("runs")?.get(0)?.asJsonObject?.get("text")?.asString ?: "Titolo"

        // Estrarre sottotitolo in modo sicuro
        val subtitle = jsonObject.getAsJsonObject("subtitle")?.getAsJsonArray("runs")?.joinToString("") {
            it.asJsonObject.get("text").asString
        } ?: ""

        val thumbnail = jsonObject.getAsJsonObject("thumbnailRenderer")?.getAsJsonObject("musicThumbnailRenderer")?.getAsJsonObject("thumbnail")?.getAsJsonArray("thumbnails")?.last()?.asJsonObject?.get("url")?.asString ?: ""

        val navEndpoint = jsonObject.getAsJsonObject("navigationEndpoint")

        if (navEndpoint?.has("watchEndpoint") == true) {
            val videoId = navEndpoint.getAsJsonObject("watchEndpoint").get("videoId").asString
            // Puliamo il sottotitolo per estrarre solo l'artista (prendiamo la parte prima del •)
            val artist = subtitle.split(" • ").firstOrNull() ?: subtitle
            return Song(videoId, title, artist, thumbnail)
        } else if (navEndpoint?.has("browseEndpoint") == true) {
            val id = navEndpoint.getAsJsonObject("browseEndpoint").get("browseId").asString
            return Playlist(title, subtitle, id, thumbnail)
        }
        return null
    }
}