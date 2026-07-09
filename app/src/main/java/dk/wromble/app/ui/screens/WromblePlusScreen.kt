package dk.wromble.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import dk.wromble.app.data.BASE_URL
import dk.wromble.app.ui.clickableNoRipple
import dk.wromble.app.ui.theme.WrombleRed

private val plusGradient = Brush.linearGradient(
    listOf(Color(0xFFE30F1E), Color(0xFFB00D17))
)

// Bruges paa forsiden – banner der linker til Wromble+
@Composable
fun WromblePlusBand(onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp)).background(plusGradient)
            .clickableNoRipple(onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(50.dp).clip(RoundedCornerShape(25.dp)).background(Color.White.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.Star, null, tint = Color.White, modifier = Modifier.size(30.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Wromble+", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(6.dp))
                Box(Modifier.clip(RoundedCornerShape(5.dp)).background(Color.White)
                    .padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text("NYT", color = WrombleRed, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
            Text("Gratis levering – hver gang", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("Kun 59,- pr. maaned", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
        }
        Spacer(Modifier.width(8.dp))
        Box(Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White)
            .padding(horizontal = 13.dp, vertical = 9.dp)) {
            Text("Kom i gang", color = WrombleRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WromblePlusScreen(nav: NavController) {
    val ctx = LocalContext.current
    val benefits = listOf(
        Triple(Icons.Filled.DirectionsBike, "Gratis levering", "Ingen leveringsgebyr paa dine ordrer – hver gang du bestiller."),
        Triple(Icons.Filled.LocalOffer, "Faste lave priser", "Adgang til Wromble+ tilbud og priser hos dine favoritter."),
        Triple(Icons.Filled.Bolt, "Nemt & enkelt", "Ingen binding. Opsig naar som helst – helt uden bovl.")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wromble+", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Tilbage")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                Modifier.fillMaxWidth().padding(20.dp).clip(RoundedCornerShape(24.dp))
                    .background(plusGradient).padding(vertical = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Filled.Star, null, tint = Color.White, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(8.dp))
                Text("Wromble+", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
                Text("Gratis levering hver gang", color = Color.White.copy(alpha = 0.9f), fontSize = 16.sp)
            }

            benefits.forEach { (icon, title, desc) -> BenefitRow(icon, title, desc) }

            Spacer(Modifier.height(12.dp))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Kun 59,- pr. maaned", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Ingen binding · opsig naar som helst", fontSize = 12.sp, color = Color(0xFF8A8A90))
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    runCatching {
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("$BASE_URL/wromble-plus/")))
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WrombleRed)
            ) { Text("Kom i gang", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun BenefitRow(icon: ImageVector, title: String, desc: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(WrombleRed.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = WrombleRed, modifier = Modifier.size(22.dp)) }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(desc, fontSize = 13.sp, color = Color(0xFF8A8A90))
        }
    }
}
