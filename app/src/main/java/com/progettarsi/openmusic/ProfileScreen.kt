package com.progettarsi.openmusic

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.progettarsi.openmusic.ui.theme.*
import com.progettarsi.openmusic.viewmodel.MusicViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

@Composable
fun ProfileScreenContent(
    onClose: () -> Unit,
    musicViewModel: MusicViewModel = viewModel(),
    hazeState: HazeState? = null
) {
    val profileHazeState = remember { HazeState() }
    var showLoginDialog by remember { mutableStateOf(false) }

    val mockPlaylists = listOf("Trippin'", "musica!", "RADIO DE...", "Gym Phonk", "Late Night")
    val mockArtists = listOf("Kanye West", "Drake", "Travis Scott", "Rose Villain", "The Weeknd")
    val userName = if (musicViewModel.isLoggedIn) "SHAdow" else "Anonimo"
    val isLoggedIn = musicViewModel.isLoggedIn

    // NESSUNA GESTURE QUI. Solo layout statico.
    Box(modifier = Modifier.fillMaxSize()) {

        // 1. SORGENTE SFOCATURA
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .haze(state = profileHazeState)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 120.dp)) {
                // Header (Immagine)
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(450.dp)) {
                        if (isLoggedIn) {
                            AsyncImage(model = ImageRequest.Builder(LocalContext.current).data("https://images.unsplash.com/photo-1529665253569-6d01c0eaf7b6?q=80&w=1000&auto=format&fit=crop").crossfade(true).build(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else {
                            Box(modifier = Modifier.fillMaxSize().background(SurfaceHighlight), contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, tint = TextGrey.copy(0.3f), modifier = Modifier.scale(5f)) }
                        }
                        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, DarkBackground.copy(0.2f), DarkBackground.copy(0.9f), DarkBackground))))
                        Column(modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(userName, fontSize = 42.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = (-1).sp)
                            Spacer(Modifier.height(8.dp))
                            Surface(color = Color.White.copy(0.15f), shape = RoundedCornerShape(50), modifier = Modifier.height(32.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp)) {
                                    Icon(Icons.Default.HourglassEmpty, null, tint = TextGrey, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(if(isLoggedIn) "10.000 hours played" else "Local stats only", color = TextGrey, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
                // Contenuto
                item { Spacer(Modifier.height(40.dp)); SectionHeader("Your Playlists"); LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) { item { CreatePlaylistCard() }; items(mockPlaylists) { ProfilePlaylistCard(it) } } }
                item { Spacer(Modifier.height(32.dp)); SectionHeader("Top Artists"); LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) { itemsIndexed(mockArtists) { i, artist -> TopArtistCard(artist, i + 1) } } }
                item { Spacer(Modifier.height(32.dp)); SectionHeader("Top Tracks") }; itemsIndexed(mockArtists) { i, artist -> ProfileTrackItem(i + 1, "Hit Song ${i + 1}", artist) }
                if (isLoggedIn) { item { Spacer(Modifier.height(32.dp)); Box(Modifier.fillMaxWidth().clickable { musicViewModel.logout() }.padding(vertical = 16.dp), contentAlignment = Alignment.Center) { Text("Log Out", color = PinkAccent, fontWeight = FontWeight.Bold, fontSize = 16.sp) } } }
            }
        }

        // 2. PULSANTI (EFFETTO VETRO)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tasto Indietro (Chiude il profilo)
            SimulatedGlassButton(Icons.Default.Close, profileHazeState, onClose)

            // Tasto Login/Settings
            SimulatedGlassButton(if (isLoggedIn) Icons.Outlined.Settings else Icons.Default.Login, profileHazeState) { if (!isLoggedIn) showLoginDialog = true }
        }
    }
    if (showLoginDialog) LoginDialog(onDismiss = { showLoginDialog = false }, onLogin = { musicViewModel.saveLoginCookie(it); showLoginDialog = false })
}

@Composable
fun SimulatedGlassButton(
    icon: ImageVector,
    hazeState: HazeState? = null,
    onClick: () -> Unit
) {
    val glassTint = Color(0xFF1E1E24).copy(alpha = 0.4f)
    Box(
        modifier = Modifier.size(48.dp).clip(CircleShape)
            .then(if (hazeState != null) Modifier.hazeChild(state = hazeState, shape = CircleShape, style = HazeStyle(backgroundColor = Color.Transparent, tint = HazeTint(glassTint), blurRadius = 30.dp)) else Modifier.background(glassTint))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White)
    }
}

// ... Componenti UI standard (SectionHeader, ecc.) rimangono uguali ...
@Composable fun SectionHeader(title: String) { Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 24.dp, bottom = 12.dp)) }
@Composable fun CreatePlaylistCard() { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(100.dp)) { Box(Modifier.size(100.dp).clip(RoundedCornerShape(16.dp)).background(PurplePrimary.copy(0.2f)).clickable{}, contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Add, null, tint = PurplePrimary, modifier = Modifier.size(40.dp)) }; Spacer(Modifier.height(8.dp)); Text("Crea", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium) } }
@Composable fun ProfilePlaylistCard(name: String) { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(100.dp)) { Box(Modifier.size(100.dp).clip(RoundedCornerShape(16.dp)).background(SurfaceHighlight)) { AsyncImage(model = "https://source.unsplash.com/random/200x200/?abstract,$name", null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().alpha(0.7f)) }; Spacer(Modifier.height(8.dp)); Text(name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis) } }
@Composable fun TopArtistCard(artist: String, rank: Int) { Box(Modifier.width(160.dp).height(100.dp).clip(RoundedCornerShape(16.dp)).background(SurfaceHighlight)) { AsyncImage(model = "https://source.unsplash.com/random/300x200/?singer,$artist", null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().alpha(0.5f)); Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color.Black.copy(0.7f), Color.Transparent)))); Row(Modifier.fillMaxSize().padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Text("$rank", color = Color.White.copy(0.9f), fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.offset(y=4.dp)); Spacer(Modifier.width(12.dp)); Text(artist, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis) } } }
@Composable fun ProfileTrackItem(number: Int, title: String, artist: String) { Row(Modifier.fillMaxWidth().clickable{}.padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { AsyncImage(model = "https://source.unsplash.com/random/100x100/?music,$title", null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceHighlight), contentScale = ContentScale.Crop); Spacer(Modifier.width(16.dp)); Column(Modifier.weight(1f)) { Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1); Text(artist, color = TextGrey, fontSize = 14.sp, maxLines = 1) }; Icon(Icons.Default.Favorite, null, tint = PurplePrimary, modifier = Modifier.size(20.dp)) } }
@Composable fun LoginDialog(onDismiss: () -> Unit, onLogin: (String) -> Unit) { var t by remember { mutableStateOf("") }; AlertDialog(onDismissRequest = onDismiss, title = { Text("Cookie", color = Color.White) }, text = { OutlinedTextField(value = t, onValueChange = { t = it }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)) }, confirmButton = { Button(onClick = { onLogin(t) }) { Text("OK") } }, containerColor = SurfaceHighlight) }