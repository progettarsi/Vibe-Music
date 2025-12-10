package com.progettarsi.vibemusic.utils

import android.content.Context
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CookieManager(context: Context) {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    // Chiave per salvare il cookie di YouTube
    private val COOKIE_KEY = "youtube_auth_cookie"

    /**
     * Salva il cookie nella memoria persistente.
     */
    suspend fun saveCookie(cookie: String) = withContext(Dispatchers.IO) {
        prefs.edit().putString(COOKIE_KEY, cookie).apply()
    }

    /**
     * Carica il cookie salvato.
     */
    fun loadCookie(): String {
        return prefs.getString(COOKIE_KEY, "") ?: ""
    }

    /**
     * Rimuove il cookie (Log Out).
     */
    suspend fun clearCookie() = withContext(Dispatchers.IO) {
        prefs.edit().remove(COOKIE_KEY).apply()
    }
}