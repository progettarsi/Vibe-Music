package com.progettarsi.openmusic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.progettarsi.openmusic.ui.theme.*

@Composable
fun ProfileScreenContent(
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White.copy(0.1f), CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, "Indietro", tint = Color.White)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(84.dp),
                    shape = CircleShape,
                    color = PurplePrimary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("A", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.width(20.dp))

                Column {
                    Text("Andrea", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("Premium Plan", color = PurplePrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text("ACCOUNT", color = TextGrey, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(16.dp))

            MenuOptionItem(Icons.Default.Person, "Edit Profile")
            MenuOptionItem(Icons.Default.History, "Listening History")
            MenuOptionItem(Icons.Default.Settings, "Settings")

            Spacer(modifier = Modifier.height(32.dp))

            Text("OTHER", color = TextGrey, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(16.dp))

            MenuOptionItem(Icons.Default.Logout, "Log Out", isDestructive = true)
        }
    }
}

@Composable
private fun MenuOptionItem(
    icon: ImageVector,
    label: String,
    isDestructive: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if(isDestructive) Color(0xFFFF4081) else Color.White.copy(0.7f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            color = if(isDestructive) Color(0xFFFF4081) else Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        if(!isDestructive) {
            Icon(Icons.Outlined.ChevronRight, null, tint = TextGrey.copy(0.5f))
        }
    }
    HorizontalDivider(color = Color.White.copy(0.05f))
}