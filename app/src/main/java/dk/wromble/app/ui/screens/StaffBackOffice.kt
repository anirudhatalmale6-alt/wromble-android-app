package dk.wromble.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import dk.wromble.app.data.*
import dk.wromble.app.ui.kr
import dk.wromble.app.ui.theme.WrombleRed
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackScaffold(title: String, nav: NavController, action: (@Composable () -> Unit)? = null, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = { action?.invoke() },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface))
        }, content = content
    )
}

@Composable
private fun BoTf(value: String, onChange: (String) -> Unit, label: String, keyboard: KeyboardType = KeyboardType.Text, lines: Int = 1) {
    OutlinedTextField(value, onChange, label = { Text(label) }, singleLine = lines == 1, minLines = lines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WrombleRed, focusedLabelColor = WrombleRed))
}

// ---------------- Menu CRUD ----------------
@Composable
fun CompanyMenuScreen(nav: NavController) {
    val scope = rememberCoroutineScope()
    val cid = Session.user?.companyId ?: 0
    val categories = remember { mutableStateListOf<MenuCategory>() }
    var loading by remember { mutableStateOf(true) }
    var addCat by remember { mutableStateOf(false) }
    var editItem by remember { mutableStateOf<Pair<Int, MenuItem?>?>(null) } // catId, item(null=new)

    fun reload() {
        scope.launch {
            try { val m = Api.service.menu(cid); categories.clear(); categories.addAll(m.categories) } catch (_: Exception) {}
            loading = false
        }
    }
    LaunchedEffect(Unit) { reload() }

    BackScaffold("Menukort", nav, action = {
        IconButton(onClick = { addCat = true }) { Icon(Icons.Filled.Add, "Ny kategori", tint = WrombleRed) }
    }) { pad ->
        Box(Modifier.fillMaxSize().padding(pad).background(MaterialTheme.colorScheme.background)) {
            if (loading) Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = WrombleRed) }
            else LazyColumn(Modifier.fillMaxSize()) {
                categories.forEach { cat ->
                    item {
                        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(cat.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            IconButton(onClick = { editItem = cat.id to null }) { Icon(Icons.Filled.Add, "Nyt produkt", tint = WrombleRed) }
                            IconButton(onClick = {
                                scope.launch { runCatching { Api.service.menuCategory(mapOf("company_id" to cid, "action" to "delete", "id" to cat.id)) }; reload() }
                            }) { Icon(Icons.Filled.Delete, "Slet", tint = Color(0xFF8A8A90)) }
                        }
                    }
                    items(cat.products) { item ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(item.name, fontWeight = FontWeight.SemiBold)
                                item.description?.takeIf { it.isNotBlank() }?.let { Text(it, fontSize = 13.sp, color = Color(0xFF8A8A90), maxLines = 1) }
                                Text(kr(item.price), color = WrombleRed, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            IconButton(onClick = { editItem = cat.id to item }) { Icon(Icons.Filled.Edit, "Rediger", tint = WrombleRed) }
                            IconButton(onClick = {
                                scope.launch { runCatching { Api.service.menuItem(mapOf("company_id" to cid, "action" to "delete", "id" to item.id)) }; reload() }
                            }) { Icon(Icons.Filled.Delete, "Slet", tint = Color(0xFF8A8A90)) }
                        }
                        Divider(color = Color(0xFFEDEDF0))
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    if (addCat) {
        var name by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { addCat = false },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { runCatching { Api.service.menuCategory(mapOf("company_id" to cid, "action" to "save", "name" to name)) }; addCat = false; reload() }
                }, enabled = name.isNotBlank()) { Text("Gem", color = WrombleRed) }
            },
            dismissButton = { TextButton(onClick = { addCat = false }) { Text("Annuller") } },
            title = { Text("Ny kategori") },
            text = { BoTf(name, { name = it }, "Kategorinavn") })
    }

    editItem?.let { (catId, item) ->
        var headline by remember { mutableStateOf(item?.name ?: "") }
        var desc by remember { mutableStateOf(item?.description ?: "") }
        var price by remember { mutableStateOf(item?.price?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "") }
        AlertDialog(onDismissRequest = { editItem = null },
            confirmButton = {
                TextButton(onClick = {
                    val body = mutableMapOf<String, Any>("company_id" to cid, "action" to "save", "cat_id" to catId,
                        "headline" to headline, "description" to desc, "price" to (price.toDoubleOrNull() ?: 0.0))
                    item?.let { body["id"] = it.id }
                    scope.launch { runCatching { Api.service.menuItem(body) }; editItem = null; reload() }
                }, enabled = headline.isNotBlank()) { Text("Gem", color = WrombleRed) }
            },
            dismissButton = { TextButton(onClick = { editItem = null }) { Text("Annuller") } },
            title = { Text(if (item == null) "Nyt produkt" else "Rediger produkt") },
            text = {
                Column {
                    BoTf(headline, { headline = it }, "Navn")
                    BoTf(desc, { desc = it }, "Beskrivelse", lines = 2)
                    BoTf(price, { price = it.filter { c -> c.isDigit() || c == '.' } }, "Pris (kr)", KeyboardType.Number)
                }
            })
    }
}

// ---------------- Opening hours ----------------
@Composable
fun CompanyHoursScreen(nav: NavController) {
    val scope = rememberCoroutineScope()
    val cid = Session.user?.companyId ?: 0
    val days = remember { mutableStateListOf<CompanyHourDay>() }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val r = Api.service.companyHours(cid)
            days.clear()
            if (r.days.isNotEmpty()) days.addAll(r.days)
            else days.addAll(listOf("Mandag","Tirsdag","Onsdag","Torsdag","Fredag","Lørdag","Søndag").map { CompanyHourDay(it) })
        } catch (_: Exception) {}
        loading = false
    }

    BackScaffold("Åbningstider", nav) { pad ->
        Box(Modifier.fillMaxSize().padding(pad).background(MaterialTheme.colorScheme.background)) {
            if (loading) Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = WrombleRed) }
            else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                days.forEachIndexed { i, d ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp), shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(d.weekday, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TimeField(d.storeOpen, "Åbner") { days[i] = days[i].copy(storeOpen = it) }
                                TimeField(d.storeClose, "Lukker") { days[i] = days[i].copy(storeClose = it) }
                            }
                            Text("Levering", fontSize = 12.sp, color = Color(0xFF8A8A90), modifier = Modifier.padding(top = 4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TimeField(d.bringOpen, "Fra") { days[i] = days[i].copy(bringOpen = it) }
                                TimeField(d.bringClose, "Til") { days[i] = days[i].copy(bringClose = it) }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = {
                    saving = true
                    scope.launch {
                        try {
                            val payload = days.map { mapOf("weekday" to it.weekday, "store_open" to it.storeOpen,
                                "store_close" to it.storeClose, "bring_open" to it.bringOpen, "bring_close" to it.bringClose) }
                            val r = Api.service.saveCompanyHours(mapOf("company_id" to cid, "days" to payload))
                            saving = false; toast = if (r.success) "Gemt" else "Kunne ikke gemme"
                        } catch (_: Exception) { saving = false; toast = "Netværksfejl" }
                    }
                }, enabled = !saving, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WrombleRed)) {
                    if (saving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    else Text("Gem åbningstider", fontWeight = FontWeight.Bold)
                }
                toast?.let { LaunchedEffect(it) { kotlinx.coroutines.delay(2000); toast = null }; Text(it, color = WrombleRed, modifier = Modifier.padding(top = 8.dp)) }
            }
        }
    }
}

@Composable
private fun RowScope.TimeField(value: String, label: String, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, label = { Text(label) }, singleLine = true,
        placeholder = { Text("HH:mm") }, modifier = Modifier.weight(1f).padding(top = 4.dp),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WrombleRed, focusedLabelColor = WrombleRed))
}

// ---------------- Company profile ----------------
@Composable
fun CompanyProfileScreen(nav: NavController) {
    val scope = rememberCoroutineScope()
    val cid = Session.user?.companyId ?: 0
    var p by remember { mutableStateOf(CompanyProfile()) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }
    var open by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val r = Api.service.companyProfile(cid).profile
            if (r != null) { p = r; open = !(r.shopStatus.equals("Lukket", true) || r.shopStatus.equals("closed", true)) }
        } catch (_: Exception) {}
        loading = false
    }

    BackScaffold("Forretningsprofil", nav) { pad ->
        Box(Modifier.fillMaxSize().padding(pad).background(MaterialTheme.colorScheme.background)) {
            if (loading) Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = WrombleRed) }
            else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Åben for bestillinger", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Switch(open, { open = it }, colors = SwitchDefaults.colors(checkedTrackColor = WrombleRed))
                }
                BoTf(p.companyname, { p = p.copy(companyname = it) }, "Forretningsnavn")
                BoTf(p.adress, { p = p.copy(adress = it) }, "Adresse")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f)) { BoTf(p.zipcode, { p = p.copy(zipcode = it) }, "Postnr.", KeyboardType.Number) }
                    Box(Modifier.weight(2f)) { BoTf(p.city, { p = p.copy(city = it) }, "By") }
                }
                BoTf(p.phoneMobile, { p = p.copy(phoneMobile = it) }, "Telefon", KeyboardType.Phone)
                BoTf(p.website, { p = p.copy(website = it) }, "Hjemmeside")
                BoTf(p.specialities, { p = p.copy(specialities = it) }, "Specialiteter")
                BoTf(p.description, { p = p.copy(description = it) }, "Beskrivelse", lines = 3)
                Text("Levering", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp, bottom = 4.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Tilbyd levering", modifier = Modifier.weight(1f))
                    Switch(p.comDelivery == 1, { p = p.copy(comDelivery = if (it) 1 else 0) },
                        colors = SwitchDefaults.colors(checkedTrackColor = WrombleRed))
                }
                if (p.comDelivery == 1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.weight(1f)) { BoTf(p.comDeliveryPrice, { p = p.copy(comDeliveryPrice = it) }, "Pris (kr)", KeyboardType.Number) }
                        Box(Modifier.weight(1f)) { BoTf(p.comDeliveryTime, { p = p.copy(comDeliveryTime = it) }, "Tid (min)", KeyboardType.Number) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    saving = true
                    scope.launch {
                        try {
                            val r = Api.service.saveCompanyProfile(mapOf("company_id" to cid, "companyname" to p.companyname,
                                "adress" to p.adress, "zipcode" to p.zipcode, "city" to p.city, "phone_mobile" to p.phoneMobile,
                                "description" to p.description, "specialities" to p.specialities, "website" to p.website,
                                "shop_status" to if (open) "Åben" else "Lukket", "com_delivery" to p.comDelivery,
                                "com_delivery_price" to p.comDeliveryPrice, "com_delivery_time" to p.comDeliveryTime))
                            saving = false; toast = if (r.success) "Gemt" else "Kunne ikke gemme"
                        } catch (_: Exception) { saving = false; toast = "Netværksfejl" }
                    }
                }, enabled = !saving, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WrombleRed)) {
                    if (saving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    else Text("Gem profil", fontWeight = FontWeight.Bold)
                }
                toast?.let { LaunchedEffect(it) { kotlinx.coroutines.delay(2000); toast = null }; Text(it, color = WrombleRed, modifier = Modifier.padding(top = 8.dp)) }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

// ---------------- Stripe earnings (driver + company) ----------------
@Composable
fun EarningsScreen(nav: NavController, type: String, id: Int) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var balance by remember { mutableStateOf(TipsBalanceResponse()) }
    var connect by remember { mutableStateOf(StripeConnectResponse()) }
    var loading by remember { mutableStateOf(true) }
    var toast by remember { mutableStateOf<String?>(null) }

    fun reload() {
        scope.launch {
            try { balance = Api.service.tipsBalance(type, id) } catch (_: Exception) {}
            try { connect = Api.service.stripeConnectStatus(type, id) } catch (_: Exception) {}
            loading = false
        }
    }
    LaunchedEffect(Unit) { reload() }

    BackScaffold("Drikkepenge", nav) { pad ->
        Box(Modifier.fillMaxSize().padding(pad).background(MaterialTheme.colorScheme.background)) {
            if (loading) Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = WrombleRed) }
            else LazyColumn(Modifier.fillMaxSize()) {
                item {
                    Card(Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp)) {
                        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Til udbetaling", color = Color(0xFF8A8A90))
                            Text(kr(balance.balance), fontSize = 34.sp, fontWeight = FontWeight.Black, color = WrombleRed)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Optjent", fontSize = 12.sp, color = Color(0xFF8A8A90)); Text(kr(balance.earned), fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Udbetalt", fontSize = 12.sp, color = Color(0xFF8A8A90)); Text(kr(balance.paidOut), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                item {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        if (connect.connectDisabled) {
                            Text("Stripe-udbetaling er ikke aktiveret for din konto endnu.", color = Color(0xFF8A8A90))
                        } else if (!connect.payoutsEnabled) {
                            Button(onClick = {
                                scope.launch {
                                    try {
                                        val r = Api.service.stripeConnect(mapOf("type" to type, "id" to id))
                                        r.onboardingUrl?.let { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
                                    } catch (_: Exception) {}
                                }
                            }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = WrombleRed)) {
                                Text("Opsæt Stripe-udbetaling", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(onClick = {
                                scope.launch {
                                    try {
                                        val r = Api.service.tipsPayout(mapOf("type" to type, "id" to id))
                                        toast = if (r.success) "Udbetaling på ${kr(r.amount)} igangsat" else (r.error ?: "Kunne ikke udbetale")
                                        reload()
                                    } catch (_: Exception) { toast = "Netværksfejl" }
                                }
                            }, enabled = balance.balance >= 1, modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = WrombleRed)) {
                                Text("Udbetal til bank", fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Seneste drikkepenge", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                items(balance.tips) { t ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), Arrangement.SpaceBetween) {
                        Column { Text("Ordre #${t.orderId}"); Text(t.date, fontSize = 12.sp, color = Color(0xFF8A8A90)) }
                        Text(kr(t.amount), fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                    }
                    Divider(color = Color(0xFFEDEDF0))
                }
                item {
                    toast?.let { LaunchedEffect(it) { kotlinx.coroutines.delay(2500); toast = null }; Text(it, color = WrombleRed, modifier = Modifier.padding(16.dp)) }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}
