package com.progettarsi.openmusic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.progettarsi.openmusic.ui.theme.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.Alignment

@Composable
fun HomeScreen(
    topPadding: Dp,
    bottomPadding: Dp
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // QUI LA MAGIA: Il padding è interno, così lo sfondo scorre sotto le barre
        contentPadding = PaddingValues(
            top = topPadding + 16.dp,        // Inizia sotto l'orologio (+ un po' di aria)
            bottom = bottomPadding, // Finisce ben sopra il dock e il gradiente
            start = 16.dp,
            end = 16.dp
        )
    ) {
        // Intestazione
        item {
            Text(
                text = "Welcome Back",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp) // Rimosso top padding manuale
            )
        }

        // Sezione Quick Picks
        item { picks(9) }

        item { MusicStack("Your Vibes", 4) }

        item { MusicStack("Chill", 5) }

        item { MusicStack("Mixes", 8) }

        item { MusicStack("Trending", 6) }

        // Spazio extra non serve più enorme qui, ci pensa il contentPadding bottom
    }
}

@Composable
fun picks(n: Int) {
    Column {
        SectionTitle("Quick Picks")
        LazyHorizontalGrid(
            rows = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .height(240.dp)
                .fillMaxWidth()
        ) {
            items(n) {
                MusicCard(title = "Canzone $it", subtitle = "Artista $it")
            }
        }
        spacing(16)
        // Indicatore di pagina (pallino viola)
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(60.dp)
                .height(5.dp)
                .background(PurplePrimary, shape = CircleShape)
        )
    }
}

@Composable
fun spacing(n: Int) {
    Spacer(modifier = Modifier.height(n.dp))
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        color = Color.White,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 16.dp)
    )
}

@Composable
fun MusicStack(title: String, n: Int) {
    Spacer(modifier = Modifier.height(16.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceHighlight)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(end = 16.dp)
        ) {
            Icon(Icons.Default.Star, contentDescription = "Play", tint = Color.White, modifier = Modifier
                .padding(end = 6.dp)
                .clickable(onClick = {/*nothing*/})
            )
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }


        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(n) {
                VibeSquare(title = "$title $it")
            }
        }
    }
}

// Card Rettangolare (stile Quick Picks)
@Composable
fun MusicCard(title: String, subtitle: String) {
    val cornerradius: Dp = 24.dp

    Box(
        modifier = Modifier
            .width(260.dp) // Leggermente più largo per far stare il testo
            .height(72.dp) // Altezza standard
            .clip(RoundedCornerShape(cornerradius))
            .background(SurfaceHighlight)
            .padding(8.dp) // Padding interno ridotto
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(cornerradius-8.dp))
                    .background(PurplePrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                Text(subtitle, color = Color.Gray, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

// Card Quadrata (stile Your Vibes)
@Composable
fun VibeSquare(title: String) {
    Column {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(PurplePrimary) // Uso il colore del tema
        )
        Text(
            text = title,
            color = Color.White,
            modifier = Modifier.padding(top = 8.dp),
            fontSize = 14.sp,
            maxLines = 1
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun PreviewHomeScreen() {
    // Passiamo padding finti per la preview
    HomeScreen(topPadding = 24.dp, bottomPadding = 48.dp)
}