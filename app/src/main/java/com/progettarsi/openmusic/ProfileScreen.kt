package com.progettarsi.openmusic

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
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
    var offsetY by remember { mutableFloatStateOf(0f) }

    val mockPlaylists = listOf("Trippin'", "musica!", "RADIO DE...", "Gym Phonk", "Late Night")
    val mockArtists = listOf("Kanye West", "Drake", "Travis Scott", "Rose Villain", "The Weeknd")

    val userName = if (musicViewModel.isLoggedIn) "SHAdow" else "Anonimo"
    val isLoggedIn = musicViewModel.isLoggedIn

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .offset(y = offsetY.dp)
            // GESTIONE SWIPE
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        if (offsetY > 100f) {
                            onClose()
                            // FIX 2: NON resettiamo offsetY a 0f qui.
                            // Lo lasciamo giù mentre il parent fa la transizione fade-out.
                        } else {
                            offsetY = 0f // Resetta solo se NON si chiude
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = offsetY + dragAmount.y
                        if (newOffset > 0) offsetY = newOffset
                    }
                )
            }
            .haze(state = profileHazeState)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {

            // --- HEADER IMMERSIVO ---
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(450.dp)
                ) {
                    if (isLoggedIn) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data("https://images.unsplash.com/photo-1529665253569-6d01c0eaf7b6?q=80&w=1000&auto=format&fit=crop")
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(SurfaceHighlight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = TextGrey.copy(alpha = 0.3f),
                                modifier = Modifier.scale(5f)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        DarkBackground.copy(alpha = 0.2f),
                                        DarkBackground.copy(alpha = 0.9f),
                                        DarkBackground
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = userName,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = (-1).sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            color = Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Icon(Icons.Default.HourglassEmpty, null, tint = TextGrey, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if(isLoggedIn) "10.000 hours played" else "Local stats only",
                                    color = TextGrey,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // --- PLAYLISTS ---
            item {
                Spacer(modifier = Modifier.height(40.dp))
                SectionHeader("Your Playlists")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { CreatePlaylistCard() }
                    items(mockPlaylists) { name -> ProfilePlaylistCard(name = name) }
                }
            }

            // --- TOP ARTISTS ---
            item {
                Spacer(modifier = Modifier.height(32.dp))
                SectionHeader("Top Artists")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(mockArtists) { index, artist ->
                        TopArtistCard(artist = artist, rank = index + 1)
                    }
                }
            }

            // --- TOP TRACKS ---
            item {
                Spacer(modifier = Modifier.height(32.dp))
                SectionHeader("Top Tracks")
            }

            itemsIndexed(mockArtists) { index, artist ->
                ProfileTrackItem(
                    number = index + 1,
                    title = "Hit Song ${index + 1}",
                    artist = artist
                )
            }

            // --- INSIGHTS ---
            item {
                Spacer(modifier = Modifier.height(32.dp))
                SectionHeader("Listening Insights")

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(SurfaceHighlight)
                        .padding(24.dp)
                ) {
                    Text("Top Genres", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    GenreBar(genre = "Hip Hop", percent = 0.7f, color = PurplePrimary)
                    Spacer(modifier = Modifier.height(12.dp))
                    GenreBar(genre = "R&B", percent = 0.45f, color = CyanAccent)
                    Spacer(modifier = Modifier.height(12.dp))
                    GenreBar(genre = "Alternative", percent = 0.3f, color = PinkAccent)
                }
            }

            // --- LOGOUT BUTTON ---
            if (isLoggedIn) {
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { musicViewModel.logout() }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Log Out", color = Color(0xFFFF4081), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }

        // 3. TOP BAR con Bottoni "Simulated Glass" (Stile Haze ma sicuro)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back Button
            SimulatedGlassButton(
                icon = Icons.Default.ArrowBack,
                onClick = onClose
            )

            // Settings Button
            SimulatedGlassButton(
                icon = if (isLoggedIn) Icons.Outlined.Settings else Icons.Default.Login,
                onClick = {
                    if (!isLoggedIn) showLoginDialog = true
                    else { /* TODO: Settings */ }
                }
            )
        }
    }

    if (showLoginDialog) {
        LoginDialog(
            onDismiss = { showLoginDialog = false },
            onLogin = { cookie ->
                musicViewModel.saveLoginCookie(cookie)
                showLoginDialog = false
            }
        )
    }
}

// --- COMPONENTI UI ---

@Composable
fun SimulatedGlassButton(
    icon: ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            // Colore Dock: 252530 con alpha 0.65f
            .background(Color(0xFF252530).copy(alpha = 0.65f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White)
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Color.White,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 24.dp, bottom = 12.dp)
    )
}

@Composable
fun GenreBar(genre: String, percent: Float, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(genre, color = TextGrey, fontSize = 12.sp, modifier = Modifier.width(70.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percent)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun CreatePlaylistCard() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(100.dp)) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(PurplePrimary.copy(alpha = 0.2f))
                .clickable { },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Add, "Create", tint = PurplePrimary, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text("Crea", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
fun ProfilePlaylistCard(name: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(100.dp)) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceHighlight)
        ) {
            AsyncImage(
                model = "https://source.unsplash.com/random/200x200/?abstract,$name",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().alpha(0.7f)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun TopArtistCard(artist: String, rank: Int) {
    Box(
        modifier = Modifier
            .width(160.dp)
            .height(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceHighlight)
    ) {
        AsyncImage(
            model = "https://source.unsplash.com/random/300x200/?singer,$artist",
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(0.5f)
        )
        Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color.Black.copy(0.7f), Color.Transparent))))
        Row(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$rank", color = Color.White.copy(alpha = 0.9f), fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.offset(y = 4.dp))
            Spacer(Modifier.width(12.dp))
            Text(artist, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun ProfileTrackItem(number: Int, title: String, artist: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = "https://source.unsplash.com/random/100x100/?music,$title",
            contentDescription = null,
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceHighlight),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(artist, color = TextGrey, fontSize = 14.sp, maxLines = 1)
        }
        Icon(Icons.Default.Favorite, "Like", tint = PurplePrimary, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun LoginDialog(
    onDismiss: () -> Unit,
    onLogin: (String) -> Unit
) {
    var cookieText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Inserisci il Cookie", color = Color.White) },
        text = {
            Column {
                Text("Incolla la stringa 'Cookie' completa dal browser per accedere a YouTube Music.", color = TextGrey, fontSize = 14.sp)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = cookieText,
                    onValueChange = { cookieText = it },
                    label = { Text("Stringa Cookie") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        focusedBorderColor = PurplePrimary,
                        unfocusedBorderColor = TextGrey.copy(0.3f),
                        focusedLabelColor = PurplePrimary,
                        unfocusedLabelColor = TextGrey,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onLogin(cookieText) },
                enabled = cookieText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
            ) {
                Text("Accedi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla", color = TextGrey) }
        },
        containerColor = SurfaceHighlight
    )
}