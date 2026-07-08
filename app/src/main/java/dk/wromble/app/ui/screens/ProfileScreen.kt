package dk.wromble.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import dk.wromble.app.data.Session
import dk.wromble.app.ui.brandGradient
import dk.wromble.app.ui.clickableNoRipple
import dk.wromble.app.ui.theme.WrombleRed

@Composable
fun ProfileScreen(nav: NavController) {
    val ctx = LocalContext.current
    val user = Session.user

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(
            Modifier.fillMaxWidth().background(brandGradient)
                .padding(top = 56.dp, bottom = 26.dp), contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(84.dp).clip(CircleShape).background(Color.White),
                    contentAlignment = Alignment.Center) {
                    Text(user?.name?.take(1)?.uppercase() ?: "?", color = WrombleRed,
                        fontSize = 36.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(12.dp))
                Text(user?.name ?: "Gaest", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(user?.email ?: "", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(Icons.Filled.Person, "Rediger profil")
        Row(Icons.Filled.LocationOn, "Mine adresser")
        Row(Icons.Filled.Favorite, "Favoritter")
        Row(Icons.Filled.SupportAgent, "Kundeservice")
        Row(Icons.Filled.Info, "Om Wromble")

        Spacer(Modifier.weight(1f))

        OutlinedButton(
            onClick = {
                Session.clear(ctx)
                nav.navigate("login") { popUpTo(0) }
            },
            modifier = Modifier.fillMaxWidth().padding(20.dp).height(50.dp),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, WrombleRed)
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, null, tint = WrombleRed)
            Spacer(Modifier.width(8.dp))
            Text("Log ud", color = WrombleRed, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Row(icon: ImageVector, label: String, onClick: () -> Unit = {}) {
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth().clickableNoRipple(onClick).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = WrombleRed)
        Spacer(Modifier.width(16.dp))
        Text(label, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Icon(Icons.Filled.ChevronRight, null, tint = Color(0xFFBFBFC6))
    }
}
