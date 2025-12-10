package com.progettarsi.vibemusic.utils

// Definiamo i campi 'data' e 'message' direttamente nella classe padre
sealed class Resource<T>(val data: T? = null, val message: String? = null) {

    // Success eredita T e passa 'data' al padre
    class Success<T>(data: T) : Resource<T>(data)

    // Error eredita T e passa 'message' (e opzionalmente 'data') al padre
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)

    // Loading eredita T
    class Loading<T>(data: T? = null) : Resource<T>(data)
}