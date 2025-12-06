package com.progettarsi.openmusic.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Definiamo lo schema scuro usando la TUA palette
private val DarkColorScheme = darkColorScheme(
    primary = PurplePrimary,          // Il tuo viola principale
    onPrimary = Color.White,          // Testo sopra il viola

    background = BlackVoid,           // Sfondo nero assoluto dell'app
    onBackground = TextWhite,         // Testo su sfondo nero

    surface = SurfaceDark,            // Colore delle card normali
    onSurface = TextWhite,            // Testo sulle card

    surfaceVariant = SurfaceHighlight, // Colore per il Dock e i blocchi in evidenza
    onSurfaceVariant = TextGrey,       // Testo secondario (artisti)

    secondary = CyanAccent,           // Accento ciano (facoltativo)
    tertiary = PinkAccent             // Accento rosa (cuori/like)
)

@Composable
fun OpenMusicTheme(
    darkTheme: Boolean = true, // Forziamo il tema scuro di default
    // Disattiviamo i colori dinamici (Dynamic Color) per mantenere la tua identità viola
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme

    // Questo codice serve a colorare la barra di stato (dove c'è l'orologio)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Colora la status bar dello stesso colore dello sfondo
            window.statusBarColor = BlackVoid.toArgb()
            // Dice al sistema che le icone della status bar devono essere chiare
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}