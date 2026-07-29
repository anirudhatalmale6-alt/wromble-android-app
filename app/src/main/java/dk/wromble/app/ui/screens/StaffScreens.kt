package dk.wromble.app.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import dk.wromble.app.WrombleApp
import dk.wromble.app.data.*
import dk.wromble.app.ui.Pill
import dk.wromble.app.ui.clickableNoRipple
import dk.wromble.app.ui.kr
import dk.wromble.app.ui.theme.WrombleRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---------------- Company hub ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyDashboardScreen(nav: NavController) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = Session.user
    var autoAccept by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var alarmSeconds by remember { mutableStateOf(5) }
    var melody by remember { mutableStateOf(Settings.alarmMelody) }

    DisposableEffect(Unit) { onDispose { Notifier.stopAlarm() } }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        val cid = s?.companyId ?: 0
        if (cid > 0) {
            try { autoAccept = Api.service.companyAutoAccept(cid).autoAccept == 1 } catch (_: Exception) {}
            try { busy = Api.service.companyBusy(cid).busy == 1 } catch (_: Exception) {}
            try { alarmSeconds = Api.service.companyAlarm(cid).alarmSeconds } catch (_: Exception) {}
        }
    }

    Scaffold(topBar = { StaffTopBar("Forretning · ${s?.name ?: ""}", nav, ctx) }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad).background(MaterialTheme.colorScheme.background)) {
            item {
                Card(Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Ordremodtagelse", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        StaffToggle("Accepter alle ordrer automatisk", autoAccept) {
                            autoAccept = it
                            scope.launch { runCatching { Api.service.setCompanyAutoAccept(mapOf("company_id" to (s?.companyId ?: 0), "auto_accept" to if (it) 1 else 0)) } }
                        }
                        Divider(Modifier.padding(vertical = 4.dp), color = Color(0xFFEDEDF0))
                        StaffToggle("Ekstraordinært travlt", busy) {
                            busy = it
                            scope.launch { runCatching { Api.service.setCompanyBusy(mapOf("company_id" to (s?.companyId ?: 0), "busy" to if (it) 1 else 0)) } }
                        }
                        Divider(Modifier.padding(vertical = 4.dp), color = Color(0xFFEDEDF0))
                        Text("Lyd ved ny ordre", modifier = Modifier.padding(top = 4.dp, bottom = 6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0 to "Fra", 5 to "5s", 10 to "10s", 15 to "15s").forEach { (v, label) ->
                                FilterChip(
                                    selected = alarmSeconds == v,
                                    onClick = {
                                        alarmSeconds = v
                                        if (v > 0) Notifier.playAlarm(ctx, 2) else Notifier.stopAlarm()  // kort forhaandsvisning
                                        scope.launch { runCatching { Api.service.setCompanyAlarm(mapOf("company_id" to (s?.companyId ?: 0), "alarm_seconds" to v)) } }
                                    },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = WrombleRed,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                        Text(
                            if (alarmSeconds == 0) "Der afspilles ingen lyd ved nye ordrer."
                            else "Alarmen spiller i $alarmSeconds sek. og stopper når du accepterer ordren.",
                            fontSize = 12.sp, color = Color(0xFF8A8A90), modifier = Modifier.padding(top = 6.dp)
                        )
                        if (alarmSeconds > 0) {
                            Text("Melodi", modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Notifier.melodyNames.forEachIndexed { idx, name ->
                                    FilterChip(
                                        selected = melody == idx,
                                        onClick = { melody = idx; Notifier.previewMelody(ctx, idx) },  // saetter + afspiller
                                        label = { Text(name) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = WrombleRed, selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item { HubRow(Icons.Filled.ReceiptLong, "Ordrer") { nav.navigate("company/orders") } }
            item { HubRow(Icons.Filled.RestaurantMenu, "Menukort") { nav.navigate("company/menu") } }
            item { HubRow(Icons.Filled.Schedule, "Åbningstider") { nav.navigate("company/hours") } }
            item { HubRow(Icons.Filled.Payments, "Drikkepenge & udbetaling") { nav.navigate("earnings/company/${s?.companyId ?: 0}") } }
            item { HubRow(Icons.Filled.Store, "Forretningsprofil") { nav.navigate("company/profile") } }
        }
    }
}

@Composable
private fun HubRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickableNoRipple(onClick).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = WrombleRed)
        Spacer(Modifier.width(16.dp))
        Text(label, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Icon(Icons.Filled.ChevronRight, null, tint = Color(0xFFBFBFC6))
    }
}

@Composable
private fun StaffToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = WrombleRed))
    }
}

// ---------------- Company orders ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyOrdersScreen(nav: NavController) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val session = Session.user
    val orders = remember { mutableStateListOf<CompanyOrder>() }
    var tab by remember { mutableStateOf("active") }
    var loading by remember { mutableStateOf(true) }
    var toast by remember { mutableStateOf<String?>(null) }
    val seen = remember { mutableStateOf(setOf<Int>()) }
    var alarmSeconds by remember { mutableStateOf(5) }   // firmaets valgte varighed (0 = fra)

    suspend fun load(alarmCheck: Boolean) {
        val cid = session?.companyId ?: return
        try {
            val r = Api.service.companyOrders(cid, tab)
            if (alarmCheck && tab == "active") {
                // Alarmér ved ENHVER ny ordre i aktiv-listen - ogsaa auto-accepterede
                // (auto_accept), som ikke laengere staar som "ny" (isNew=false).
                val newOnes = r.orders.filter { it.id !in seen.value }
                if (newOnes.isNotEmpty() && seen.value.isNotEmpty()) {
                    Notifier.playAlarm(ctx, alarmSeconds)   // spiller i firmaets valgte antal sek
                    Notifier.notify(ctx, 3001, "Ny ordre!", "Du har ${newOnes.size} ny(e) ordre(r)", WrombleApp.CH_ORDERS)
                }
            }
            seen.value = r.orders.map { it.id }.toSet()
            orders.clear(); orders.addAll(r.orders)
        } catch (_: Exception) {} finally { loading = false }
    }

    // Hent firmaets lyd-indstilling (0/5/10/15 sek), og stop alarmen naar skaermen forlades.
    LaunchedEffect(Unit) {
        val cid = session?.companyId ?: 0
        if (cid > 0) runCatching { alarmSeconds = Api.service.companyAlarm(cid).alarmSeconds }
    }
    DisposableEffect(Unit) { onDispose { Notifier.stopAlarm() } }

    LaunchedEffect(tab) { loading = true; load(false) }
    // auto-refresh every 5s with alarm detection
    LaunchedEffect(tab) {
        while (true) { delay(5000); load(true) }
    }

    fun action(o: CompanyOrder, act: String) {
        val cid = session?.companyId ?: return
        Notifier.stopAlarm()   // stop lyden med det samme naar forretningen reagerer paa ordren
        scope.launch {
            try {
                val res = Api.service.companyOrderAction(mapOf("company_id" to cid, "order_id" to o.id, "action" to act))
                if (res.success) {
                    if (act == "reject") { orders.remove(o); toast = "Ordre #${o.id} afvist" }
                    else { load(false); toast = "Ordre #${o.id} accepteret" }
                } else toast = "Handlingen mislykkedes"
            } catch (_: Exception) { toast = "Netværksfejl" }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Ordrer", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface))
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).background(MaterialTheme.colorScheme.background)) {
            TabRow(selectedTabIndex = if (tab == "active") 0 else 1,
                containerColor = MaterialTheme.colorScheme.surface, contentColor = WrombleRed) {
                Tab(selected = tab == "active", onClick = { tab = "active" }, text = { Text("Aktive") })
                Tab(selected = tab == "history", onClick = { tab = "history" }, text = { Text("Historik") })
            }
            Box(Modifier.fillMaxSize()) {
                when {
                    loading && orders.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = WrombleRed) }
                    orders.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Ingen ordrer", color = Color(0xFF8A8A90)) }
                    else -> LazyColumn(Modifier.fillMaxSize()) {
                        items(orders) { o -> CompanyOrderCard(o, tab == "active") { act -> action(o, act) } }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
                toast?.let {
                    LaunchedEffect(it) { delay(2500); toast = null }
                    Snackbar(Modifier.align(Alignment.BottomCenter).padding(16.dp)) { Text(it) }
                }
            }
        }
    }
}

@Composable
private fun CompanyOrderCard(o: CompanyOrder, active: Boolean, onAction: (String) -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Ordre #${o.id}", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (o.overdue) Pill("FORSINKET", WrombleRed)
                    if (o.isNew) Pill("NY", WrombleRed)
                }
            }
            Text(o.customer, fontSize = 15.sp)
            if (o.phone.isNotBlank()) Text(o.phone, fontSize = 13.sp, color = Color(0xFF8A8A90))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                Pill(if (o.delivery) "Levering" else if (o.table) "Bord" else "Afhentning", Color(0xFFF0F0F3), Color(0xFF444444))
                Pill(if (o.payment == 1) "Online" else "Kontant", Color(0xFFF0F0F3), Color(0xFF444444))
            }
            if (o.delivery && o.address.isNotBlank()) Text(o.address, fontSize = 13.sp, color = Color(0xFF8A8A90), modifier = Modifier.padding(top = 4.dp))
            o.wantedTime?.takeIf { it.isNotBlank() }?.let { Text("Ønsket: $it", fontSize = 13.sp, color = Color(0xFF6B6B72)) }
            o.etaText?.let { Text("Chauffør: ${o.riderName ?: ""} · $it", fontSize = 13.sp, color = Color(0xFF6B6B72)) }
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
            if (active && !o.delivered && o.isNew) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { onAction("accept") }, modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = WrombleRed)) {
                        Text("Accepter", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(onClick = { onAction("reject") }, modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, WrombleRed)) {
                        Text("Afvis", color = WrombleRed, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ---------------- Driver ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverDashboardScreen(nav: NavController) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val session = Session.user
    val active = remember { mutableStateListOf<DriverOrder>() }
    val history = remember { mutableStateListOf<DriverOrder>() }
    var tab by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var toast by remember { mutableStateOf<String?>(null) }
    val seen = remember { mutableStateOf(setOf<Int>()) }
    var showAlarmSettings by remember { mutableStateOf(false) }

    DisposableEffect(Unit) { onDispose { Notifier.stopAlarm() } }
    if (showAlarmSettings) {
        AlarmSettingsDialog(showDuration = true) { showAlarmSettings = false }
    }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}
    LaunchedEffect(Unit) {
        val perms = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) perms.add(Manifest.permission.POST_NOTIFICATIONS)
        permLauncher.launch(perms.toTypedArray())
    }

    suspend fun loadActive(alarm: Boolean) {
        val s = session ?: return
        try {
            val r = Api.service.driverOrders(s.id, s.companyId)
            if (alarm) {
                val fresh = r.orders.filter { it.id !in seen.value }
                if (fresh.isNotEmpty() && seen.value.isNotEmpty()) {
                    Notifier.playAlarm(ctx, Settings.driverAlarmSeconds)   // chaufføerens valgte varighed
                    Notifier.notify(ctx, 3002, "Ny leverance!", "Du har en ny leverance", WrombleApp.CH_ORDERS)
                }
            }
            seen.value = r.orders.map { it.id }.toSet()
            active.clear(); active.addAll(r.orders)
        } catch (_: Exception) {} finally { loading = false }
    }
    fun loadHistory() {
        val s = session ?: return
        scope.launch { try { val r = Api.service.driverHistory(s.id, s.companyId); history.clear(); history.addAll(r.orders) } catch (_: Exception) {} }
    }

    LaunchedEffect(Unit) { loadActive(false) }
    LaunchedEffect(tab) { if (tab == 1) loadHistory() }
    // 5s refresh + live GPS post while there are active deliveries
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            loadActive(true)
            val s = session
            if (s != null && active.isNotEmpty()) {
                val loc = LocationProvider.lastKnown(ctx)
                if (loc != null) runCatching {
                    Api.service.driverLocation(mapOf("rider_id" to s.id, "latitude" to loc.latitude, "longitude" to loc.longitude))
                }
            }
        }
    }

    fun route(o: DriverOrder) {
        val uri = if (o.lat != 0.0 || o.lng != 0.0) Uri.parse("google.navigation:q=${o.lat},${o.lng}")
        else Uri.parse("geo:0,0?q=${Uri.encode(o.address)}")
        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
    }
    fun take(o: DriverOrder) {
        val s = session ?: return
        scope.launch {
            try {
                val r = Api.service.driverTake(mapOf("rider_id" to s.id, "company_id" to s.companyId, "order_id" to o.id))
                if (r.success) { loadActive(false); toast = "Levering #${o.id} startet" } else toast = "Kunne ikke starte"
            } catch (_: Exception) { toast = "Netværksfejl" }
        }
    }
    fun deliver(o: DriverOrder) {
        val s = session ?: return
        scope.launch {
            try {
                val r = Api.service.driverDeliver(mapOf("rider_id" to s.id, "company_id" to s.companyId, "order_id" to o.id))
                if (r.success) { active.remove(o); toast = "Ordre #${o.id} leveret" } else toast = "Kunne ikke opdatere"
            } catch (_: Exception) { toast = "Netværksfejl" }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Chauffør · ${session?.name ?: ""}", fontWeight = FontWeight.Bold, maxLines = 1) },
                actions = {
                    IconButton(onClick = { showAlarmSettings = true }) { Icon(Icons.Filled.Notifications, "Lyd", tint = WrombleRed) }
                    IconButton(onClick = { nav.navigate("earnings/rider/${session?.id ?: 0}") }) { Icon(Icons.Filled.Payments, "Drikkepenge", tint = WrombleRed) }
                    IconButton(onClick = { Session.clear(ctx); nav.navigate("login") { popUpTo(0) } }) { Icon(Icons.AutoMirrored.Filled.Logout, "Log ud", tint = WrombleRed) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface))
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).background(MaterialTheme.colorScheme.background)) {
            TabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.surface, contentColor = WrombleRed) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Aktive") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Historik") })
            }
            val list = if (tab == 0) active else history
            Box(Modifier.fillMaxSize()) {
                when {
                    loading && list.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = WrombleRed) }
                    list.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text(if (tab == 0) "Ingen aktive leverancer" else "Ingen historik", color = Color(0xFF8A8A90)) }
                    else -> LazyColumn(Modifier.fillMaxSize()) {
                        items(list) { o -> DriverOrderCard(o, tab == 0, onRoute = { route(o) }, onTake = { take(o) }, onDeliver = { deliver(o) }) }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
                toast?.let {
                    LaunchedEffect(it) { delay(2500); toast = null }
                    Snackbar(Modifier.align(Alignment.BottomCenter).padding(16.dp)) { Text(it) }
                }
            }
        }
    }
}

@Composable
private fun DriverOrderCard(o: DriverOrder, active: Boolean, onRoute: () -> Unit, onTake: () -> Unit, onDeliver: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Ordre #${o.id}", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(kr(o.amount), fontWeight = FontWeight.Bold, color = WrombleRed)
            }
            if (o.company.isNotBlank()) Text(o.company, fontSize = 14.sp, color = Color(0xFF6B6B72))
            Text(o.customer, fontSize = 15.sp)
            Text(o.address, fontSize = 14.sp, color = Color(0xFF8A8A90))
            o.etaText?.let { Text("ETA: $it", fontSize = 13.sp, color = Color(0xFF6B6B72)) }
            if (o.dateLabel.isNotBlank()) Text(o.dateLabel, fontSize = 12.sp, color = Color(0xFF9A9AA2))
            if (active) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onRoute, modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, WrombleRed)) {
                        Icon(Icons.Filled.Directions, null, tint = WrombleRed, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp)); Text("Rute", color = WrombleRed, fontWeight = FontWeight.Bold)
                    }
                    if (o.mine) {
                        Button(onClick = onDeliver, modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = WrombleRed)) {
                            Text("Leveret", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(onClick = onTake, modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = WrombleRed)) {
                            Text("Start levering", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (o.failed) {
                Text("Ikke gennemført", color = WrombleRed, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffTopBar(title: String, nav: NavController, ctx: android.content.Context) {
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

// Delt lyd-indstillings-dialog (melodi + evt. varighed). Bruges af chaufføeren.
@Composable
fun AlarmSettingsDialog(showDuration: Boolean, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var seconds by remember { mutableStateOf(Settings.driverAlarmSeconds) }
    var melody by remember { mutableStateOf(Settings.alarmMelody) }
    AlertDialog(
        onDismissRequest = { Notifier.stopAlarm(); onDismiss() },
        confirmButton = { TextButton(onClick = { Notifier.stopAlarm(); onDismiss() }) { Text("Færdig") } },
        title = { Text("Lyd-indstillinger") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (showDuration) {
                    Text("Varighed", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0 to "Fra", 5 to "5s", 10 to "10s", 15 to "15s").forEach { (v, label) ->
                            FilterChip(
                                selected = seconds == v,
                                onClick = { seconds = v; Settings.setDriverAlarmSeconds(ctx, v) },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = WrombleRed, selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }
                Text("Melodi", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 4.dp))
                Notifier.melodyNames.forEachIndexed { idx, name ->
                    Row(
                        Modifier.fillMaxWidth().clickableNoRipple { melody = idx; Notifier.previewMelody(ctx, idx) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.MusicNote, null, tint = WrombleRed, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(name, modifier = Modifier.weight(1f))
                        if (melody == idx) Icon(Icons.Filled.Check, null, tint = WrombleRed)
                    }
                }
            }
        }
    )
}
