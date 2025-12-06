package com.progettarsi.openmusic

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.progettarsi.openmusic.ui.theme.*

@Composable
fun SearchBarOverlay(
    searchState: SearchState,
    bottomPadding: Dp,
    onProfileClick: () -> Unit
) {
    // Definiamo lo stile "Glass" identico al MusicDock
    val glassColor = Color(0xFF252530).copy(alpha = 0.65f)
    val borderStroke = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = bottomPadding + 16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // --- BARRA RICERCA MODIFICATA ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Copertina Album (Sinistra)
                // Questa la lasciamo opaca o con il colore dell'album, ma aggiungiamo il bordo per stile
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(PurplePrimary) // Qui in futuro andrà l'immagine dell'album
                        .border(borderStroke, RoundedCornerShape(32.dp)), // Bordo vetro
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // 2. Barra di Testo (Centro) - ORA TRASPARENTE
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(glassColor) // <--- Colore Vetro (0.65 alpha)
                        .border(borderStroke, RoundedCornerShape(32.dp)), // <--- Bordo Vetro
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))

                        BasicTextField(
                            value = searchState.query,
                            onValueChange = { searchState.query = it },
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                            cursorBrush = SolidColor(PurpleLight),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                if (searchState.query.isEmpty()) {
                                    Text("Cerca...", color = TextGrey.copy(alpha = 0.7f))
                                }
                                innerTextField()
                            }
                        )

                        if (searchState.query.isNotEmpty()) {
                            IconButton(onClick = { searchState.query = "" }) {
                                Icon(Icons.Default.Close, null, tint = TextGrey)
                            }
                        }
                    }
                }

                // 3. Profilo (Destra) - ORA TRASPARENTE
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(glassColor) // <--- Colore Vetro
                        .border(borderStroke, CircleShape) // <--- Bordo Vetro
                        .clickable(onClick = onProfileClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- LISTA SUGGERIMENTI (Anche questa Glass) ---
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(glassColor) // <--- Vetro anche qui
                    .border(borderStroke, RoundedCornerShape(24.dp)) // <--- Bordo
            ) {
                items(searchSuggestions) { suggestion ->
                    SuggestionItem(text = suggestion)
                }
            }
        }
    }
}

@Composable
fun SuggestionItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Click suggerimento */ }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.8f))
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, color = Color.White.copy(alpha = 0.9f), fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}