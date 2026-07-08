package dk.wromble.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import dk.wromble.app.data.Api
import dk.wromble.app.data.OrderStatus
import dk.wromble.app.ui.kr
import dk.wromble.app.ui.theme.WrombleRed
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(nav: NavController, orderId: Int) {
    var status by remember { mutableStateOf<OrderStatus?>(null) }
    var loading by remember { mutableStateOf(true) }

    // Poll live status every 15s (mirrors iOS)
    LaunchedEffect(orderId) {
        while (true) {
            try {
                status = Api.service.orderStatus(orderId)
            } catch (_: Exception) {}
            loading = false
            delay(15000)
        }
    }

    val steps = listOf("Modtaget", "Bekraeftet", "Paa vej", "Leveret")
    val stage = status?.stage ?: 0
    val rejected = stage < 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ordre #$orderId", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).background(MaterialTheme.colorScheme.background).padding(24.dp)
        ) {
            if (loading && status == null) {
                Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) { CircularProgressIndicator(color = WrombleRed) }
                return@Column
            }
            Text(status?.companyName ?: "", fontSize = 22.sp, fontWeight = FontWeight.Black)
            status?.total?.let { Text("Total: ${kr(it)}", color = Color(0xFF8A8A90)) }
            Spacer(Modifier.height(24.dp))

            if (rejected) {
                Text("❌ Ordren blev afvist", color = WrombleRed, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            } else {
                steps.forEachIndexed { i, label ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                        Box(
                            Modifier.size(40.dp).clip(CircleShape)
                                .background(if (i <= stage) WrombleRed else Color(0xFFE3E3E8)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (i < stage) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(22.dp))
                            else Text("${i + 1}", color = if (i <= stage) Color.White else Color(0xFF9A9AA2), fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(14.dp))
                        Text(label, fontSize = 16.sp,
                            fontWeight = if (i == stage) FontWeight.Bold else FontWeight.Normal,
                            color = if (i <= stage) MaterialTheme.colorScheme.onSurface else Color(0xFF9A9AA2))
                    }
                    if (i < steps.size - 1) {
                        Box(Modifier.padding(start = 19.dp).width(2.dp).height(18.dp)
                            .background(if (i < stage) WrombleRed else Color(0xFFE3E3E8)))
                    }
                }
            }
            status?.description?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(20.dp))
                Text(it, color = Color(0xFF6B6B72), fontSize = 15.sp)
            }
        }
    }
}
