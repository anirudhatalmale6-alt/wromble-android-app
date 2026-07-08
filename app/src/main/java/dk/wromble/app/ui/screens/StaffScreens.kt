package dk.wromble.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import dk.wromble.app.data.*
import dk.wromble.app.ui.Pill
import dk.wromble.app.ui.kr
import dk.wromble.app.ui.theme.WrombleRed
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverDashboardScreen(nav: NavController) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val session = Session.user
    val orders = remember { mutableStateListOf<DriverOrder>() }
    var loading by remember { mutableStateOf(true) }
    var toast by remember { mutableStateOf<String?>(null) }

    fun load() {
        val s = session ?: return
        loading = true
        scope.launch {
            try {
                val r = Api.service.driverOrders(s.id, s.companyId)
                orders.clear(); orders.addAll(r.orders)
            } catch (_: Exception) {} finally { loading = false }
        }
    }
    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = { StaffTopBar("Chauffoer · ${session?.name ?: ""}", nav, ctx) }
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad).background(MaterialTheme.colorScheme.background)) {
            when {
                loading && orders.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = WrombleRed) }
                orders.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Ingen aktive leverancer", color = Color(0xFF8A8A90)) }
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(orders) { o ->
                        Card(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                    Text("Ordre #${o.id}", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                    Text(kr(o.amount), fontWeight = FontWeight.Bold, color = WrombleRed)
                                }
                                Text(o.customer, fontSize = 15.sp)
                                Text(o.address, fontSize = 14.sp, color = Color(0xFF8A8A90))
                                Spacer(Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        val s = session ?: return@Button
                                        scope.launch {
                                            try {
                                                val res = Api.service.driverDeliver(mapOf(
                                                    "rider_id" to s.id, "company_id" to s.companyId, "order_id" to o.id))
                                                if (res.success) { orders.remove(o); toast = "Ordre #${o.id} leveret" }
                                                else toast = "Kunne ikke opdatere"
                                            } catch (_: Exception) { toast = "Netvaerksfejl" }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(46.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = WrombleRed)
                                ) { Text("Marker som leveret", fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                }
            }
            toast?.let {
                LaunchedEffect(it) { kotlinx.coroutines.delay(2500); toast = null }
                Snackbar(Modifier.align(Alignment.BottomCenter).padding(16.dp)) { Text(it) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyDashboardScreen(nav: NavController) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val session = Session.user
    val orders = remember { mutableStateListOf<CompanyOrder>() }
    var scope1 by remember { mutableStateOf("active") }
    var loading by remember { mutableStateOf(true) }
    var toast by remember { mutableStateOf<String?>(null) }

    fun load() {
        val s = session ?: return
        loading = true
        scope.launch {
            try {
                val r = Api.service.companyOrders(s.companyId, scope1)
                orders.clear(); orders.addAll(r.orders)
            } catch (_: Exception) {} finally { loading = false }
        }
    }
    LaunchedEffect(scope1) { load() }

    fun action(o: CompanyOrder, act: String) {
        val s = session ?: return
        scope.launch {
            try {
                val res = Api.service.companyOrderAction(mapOf(
                    "company_id" to s.companyId, "order_id" to o.id, "action" to act))
                if (res.success) {
                    if (act == "reject") { orders.remove(o); toast = "Ordre #${o.id} afvist" }
                    else { load(); toast = "Ordre #${o.id} accepteret" }
                } else toast = "Handlingen mislykkedes"
            } catch (_: Exception) { toast = "Netvaerksfejl" }
        }
    }

    Scaffold(topBar = { StaffTopBar("Forretning · ${session?.name ?: ""}", nav, ctx) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).background(MaterialTheme.colorScheme.background)) {
            TabRow(selectedTabIndex = if (scope1 == "active") 0 else 1,
                containerColor = MaterialTheme.colorScheme.surface, contentColor = WrombleRed) {
                Tab(selected = scope1 == "active", onClick = { scope1 = "active" },
                    text = { Text("Aktive") })
                Tab(selected = scope1 == "history", onClick = { scope1 = "history" },
                    text = { Text("Historik") })
            }
            Box(Modifier.fillMaxSize()) {
                when {
                    loading && orders.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = WrombleRed) }
                    orders.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Ingen ordrer", color = Color(0xFF8A8A90)) }
                    else -> LazyColumn(Modifier.fillMaxSize()) {
                        items(orders) { o ->
                            Card(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                        Text("Ordre #${o.id}", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                        if (o.isNew) Pill("NY", WrombleRed)
                                    }
                                    Text(o.customer, fontSize = 15.sp)
                                    if (o.phone.isNotBlank()) Text(o.phone, fontSize = 13.sp, color = Color(0xFF8A8A90))
                                    Text(if (o.delivery) "Levering: ${o.address}" else if (o.table) "Bord-bestilling" else "Afhentning",
                                        fontSize = 13.sp, color = Color(0xFF8A8A90))
                                    Spacer(Modifier.height(6.dp))
                                    o.items.forEach { i ->
                                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                            Text("${i.qty}x ${i.name}", fontSize = 14.sp, color = Color(0xFF6B6B72))
                                            Text(kr(i.price * i.qty), fontSize = 14.sp, color = Color(0xFF6B6B72))
                                        }
                                    }
                                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), Arrangement.End) {
                                        Text("Total: ${kr(o.amount)}", fontWeight = FontWeight.Bold, color = WrombleRed)
                                    }
                                    if (scope1 == "active" && !o.delivered) {
                                        Spacer(Modifier.height(10.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Button(onClick = { action(o, "accept") },
                                                modifier = Modifier.weight(1f).height(44.dp),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = WrombleRed)) {
                                                Text("Accepter", fontWeight = FontWeight.Bold)
                                            }
                                            OutlinedButton(onClick = { action(o, "reject") },
                                                modifier = Modifier.weight(1f).height(44.dp),
                                                shape = RoundedCornerShape(12.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, WrombleRed)) {
                                                Text("Afvis", color = WrombleRed, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                toast?.let {
                    LaunchedEffect(it) { kotlinx.coroutines.delay(2500); toast = null }
                    Snackbar(Modifier.align(Alignment.BottomCenter).padding(16.dp)) { Text(it) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StaffTopBar(title: String, nav: NavController, ctx: android.content.Context) {
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.Bold, maxLines = 1) },
        actions = {
            IconButton(onClick = {
                Session.clear(ctx)
                nav.navigate("login") { popUpTo(0) }
            }) { Icon(Icons.AutoMirrored.Filled.Logout, "Log ud", tint = WrombleRed) }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
    )
}
