package dk.wromble.app.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.location.Geocoder
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import dk.wromble.app.data.*
import dk.wromble.app.ui.ChoiceChip
import dk.wromble.app.ui.MainViewModel
import dk.wromble.app.ui.QtyStepper
import dk.wromble.app.ui.clickableNoRipple
import dk.wromble.app.ui.kr
import dk.wromble.app.ui.theme.WrombleRed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Omvendt geokodning: enhedens position -> strukturerede felter (vej, husnr, postnr, by).
private data class GeoAddr(val street: String, val husnr: String, val zip: String, val city: String)

private fun reverseGeocode(ctx: Context, lat: Double, lng: Double): GeoAddr? {
    if (lat == 0.0 && lng == 0.0) return null
    return try {
        @Suppress("DEPRECATION")
        val a = Geocoder(ctx, Locale("da", "DK")).getFromLocation(lat, lng, 1)?.firstOrNull()
            ?: return null
        val g = GeoAddr(
            street = a.thoroughfare.orEmpty().trim(),
            husnr = a.subThoroughfare.orEmpty().trim(),
            zip = a.postalCode.orEmpty().trim(),
            city = a.locality.orEmpty().trim()
        )
        if (g.street.isBlank() && g.zip.isBlank() && g.city.isBlank()) null else g
    } catch (_: Exception) { null }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(nav: NavController, vm: MainViewModel) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    var isDelivery by remember { mutableStateOf(true) }
    // Struktureret leveringsadresse: vej, husnr, etage/dør, postnr, by
    var address by remember { mutableStateOf("") }   // vejnavn (evt. inkl. nr fra placering)
    var husnr by remember { mutableStateOf("") }
    var floor by remember { mutableStateOf("") }
    var zip by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    // Adresse-forslag (DAWA) mens kunden skriver
    var addrSuggestions by remember { mutableStateOf<List<AddressSuggestion>>(emptyList()) }
    var addrFocused by remember { mutableStateOf(false) }
    var addrLoading by remember { mutableStateOf(false) }

    // Samlet adresse-streng der sendes til backend/chauffør
    fun composedAddress(): String {
        val line1 = listOf(address.trim(), husnr.trim()).filter { it.isNotEmpty() }.joinToString(" ")
        val cityLine = listOf(zip.trim(), city.trim()).filter { it.isNotEmpty() }.joinToString(" ")
        return listOf(line1, floor.trim(), cityLine).filter { it.isNotEmpty() }.joinToString(", ")
    }
    var note by remember { mutableStateOf("") }
    var payment by remember { mutableStateOf(2) } // 1 = online, 2 = kontanter
    var scheduleLater by remember { mutableStateOf(false) }
    var wantedTime by remember { mutableStateOf(Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) }) }
    var tip by remember { mutableStateOf(0) }
    var customTip by remember { mutableStateOf("") }
    var ordering by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var placedOrderId by remember { mutableStateOf<Int?>(null) }

    val effectiveTip = if (isDelivery) (customTip.toIntOrNull() ?: tip) else 0

    // Auto-udfyld leveringsadressen: brug brugerens gemte profiladresse,
    // ellers omvendt-geokod enhedens position. Kun hvis feltet er tomt.
    LaunchedEffect(Unit) {
        if (address.isNotBlank()) return@LaunchedEffect
        val uid = Session.user?.id ?: 0
        if (uid > 0) {
            try {
                val p = Api.service.userProfile(uid).profile
                if (p != null && address.isBlank()) {
                    address = p.adress.trim()
                    if (zip.isBlank()) zip = p.zipcode.trim()
                    if (city.isBlank()) city = p.city.trim()
                }
            } catch (_: Exception) {}
        }
        if (address.isBlank() && zip.isBlank()) {
            val geo = withContext(Dispatchers.IO) { reverseGeocode(ctx, vm.userLat, vm.userLng) }
            if (geo != null && address.isBlank()) {
                address = geo.street
                if (husnr.isBlank()) husnr = geo.husnr
                if (zip.isBlank()) zip = geo.zip
                if (city.isBlank()) city = geo.city
            }
        }
    }

    // Ryd kurven FOERST naar kvitteringsskaermen vises (placedOrderId sat). Goeres
    // i en effekt - ikke inline i bestillingen - saa kurvens bottomBar ikke aendres
    // samtidig med at skaermen skifter til kvitteringen.
    LaunchedEffect(placedOrderId) {
        if (placedOrderId != null) Cart.clear()
    }

    fun wantedLabel(): String {
        if (!scheduleLater) return "Hurtigst muligt (ca. 1 time)"
        val today = Calendar.getInstance()
        val same = today.get(Calendar.YEAR) == wantedTime.get(Calendar.YEAR) &&
            today.get(Calendar.DAY_OF_YEAR) == wantedTime.get(Calendar.DAY_OF_YEAR)
        val fmt = SimpleDateFormat(if (same) "'kl.' HH:mm" else "d/M 'kl.' HH:mm", Locale("da", "DK"))
        return fmt.format(wantedTime.time)
    }

    fun pickTime() {
        val base = wantedTime
        DatePickerDialog(ctx, { _, y, mo, d ->
            TimePickerDialog(ctx, { _, h, mi ->
                val c = Calendar.getInstance()
                c.set(y, mo, d, h, mi, 0)
                val min = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) }
                wantedTime = if (c.before(min)) min else c
                scheduleLater = true
            }, base.get(Calendar.HOUR_OF_DAY), base.get(Calendar.MINUTE), true).show()
        }, base.get(Calendar.YEAR), base.get(Calendar.MONTH), base.get(Calendar.DAY_OF_MONTH)).show()
    }

    fun placeOrder() {
        val user = Session.user
        if (user == null || user.id == 0) { error = "Du skal være logget ind for at bestille"; return }
        if (Cart.items.isEmpty()) return
        if (isDelivery && address.isBlank()) { error = "Indtast leveringsadresse"; return }
        val deliveryAddress = composedAddress()
        error = ""; ordering = true
        scope.launch {
            try {
                val resp = Api.service.placeOrder(mapOf(
                    "user_id" to user.id,
                    "company_id" to Cart.restaurantId,
                    // Drikkepenge laegges oven i totalen og betales via ordrens metode
                    // (kontant = kontant til chaufføren, online = samme sted som ordren).
                    "total" to Cart.total + effectiveTip,
                    "tip" to effectiveTip,
                    "note" to note,
                    "delivery_check" to if (isDelivery) 1 else 0,
                    "payment_method" to payment,
                    "delivery_address" to deliveryAddress,
                    "wanted_time" to wantedLabel(),
                    "wanted_ts" to if (scheduleLater) (wantedTime.timeInMillis / 1000).toInt() else 0,
                    "items" to Cart.items.map { mapOf("id" to it.id, "quantity" to it.quantity) }
                ))
                if (resp.error != null) { error = resp.error; return@launch }
                val oid = resp.orderId
                if (oid != null && oid > 0) {
                    // Notifikation maa ALDRIG kunne crashe bestillingen
                    runCatching {
                        Notifier.notify(ctx, oid, "Ordre modtaget", "Ordre #$oid er sendt til restauranten")
                    }
                    // Kurven ryddes IKKE her - det sker i en LaunchedEffect naar
                    // placedOrderId er sat, saa Scaffold'ens bottomBar ikke aendres
                    // i samme frame som skaermen skifter (undgaar Compose-crash).
                    placedOrderId = oid   // skifter til kvitteringsskaermen
                    // Ved online betaling: aabn Stripe Checkout (kort/MobilePay), saa kunden
                    // betaler nu. Wromble modtager beloebet og splitter via Connect server-side.
                    if (payment == 1) {
                        runCatching {
                            val co = Api.service.orderCheckout(mapOf("order_id" to oid))
                            val url = co.checkoutUrl
                            if (!url.isNullOrBlank()) {
                                ctx.startActivity(
                                    android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse(url)
                                    ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        }
                    }
                } else {
                    error = "Kunne ikke afgive bestillingen. Prøv igen."
                }
            } catch (e: Throwable) {
                // Alt (netvaerk, parsing, uventet) fanges saa appen aldrig lukker ned
                error = "Netværksfejl. Prøv igen."
            } finally {
                ordering = false
            }
        }
    }

    if (placedOrderId != null) {
        // Kunden skal IKKE selv trykke "Foelg ordre" - vi viser kvitteringen kort og
        // sender derefter automatisk videre til live-sporing. (Kan ogsaa trykkes straks.)
        val goTrack: () -> Unit = { nav.navigate("tracking/${placedOrderId}") { popUpTo("main") } }
        LaunchedEffect(placedOrderId) { delay(2500); goTrack() }
        OrderConfirmation(placedOrderId!!, tipAmount = effectiveTip, isCash = payment == 2,
            onTrack = goTrack)
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Din kurv", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            if (Cart.items.isNotEmpty()) {
                // imePadding: loeft "Afgiv bestilling"-knappen op over tastaturet
                // (appen er edge-to-edge, saa vinduet resizer ikke selv for IME)
                Surface(Modifier.imePadding(), shadowElevation = 12.dp, color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.padding(16.dp)) {
                        if (effectiveTip > 0) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Text("Drikkepenge", color = Color(0xFF8A8A90))
                                Text("+ ${effectiveTip},00 kr", color = Color(0xFF8A8A90))
                            }
                        }
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Total", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(kr(Cart.total + effectiveTip), fontSize = 18.sp, fontWeight = FontWeight.Black, color = WrombleRed)
                        }
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { placeOrder() },
                            enabled = !ordering,
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WrombleRed)
                        ) {
                            if (ordering) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            else Text("Afgiv bestilling", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { pad ->
        if (Cart.items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad), Alignment.Center) {
                Text("Din kurv er tom", color = Color(0xFF8A8A90), fontSize = 16.sp)
            }
            return@Scaffold
        }
        Column(
            Modifier.fillMaxSize().padding(pad)
                .pointerInput(Unit) { detectTapGestures(onTap = { focus.clearFocus() }) }
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background).padding(16.dp)
        ) {
            Text(Cart.restaurantName, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            Cart.items.forEach { item ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(item.name, fontWeight = FontWeight.SemiBold)
                        Text(kr(item.price), color = Color(0xFF8A8A90), fontSize = 13.sp)
                    }
                    QtyStepper(item.quantity,
                        onMinus = { Cart.setQuantity(item.id, item.quantity - 1) },
                        onPlus = { Cart.setQuantity(item.id, item.quantity + 1) })
                }
                Divider(color = Color(0xFFEDEDF0))
            }

            Spacer(Modifier.height(16.dp))
            Text("Levering", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChoiceChip("Levering", isDelivery) { isDelivery = true }
                ChoiceChip("Afhentning", !isDelivery) { isDelivery = false }
            }

            if (isDelivery) {
                Spacer(Modifier.height(12.dp))

                // Hent adresse-forslag fra DAWA, debounced, mens kunden taster i adressefeltet.
                // Koerer saa laenge feltet er i fokus - saa listen dukker paalideligt op,
                // uanset om feltet var forudfyldt med profiladressen.
                LaunchedEffect(address, addrFocused) {
                    if (!addrFocused) { addrLoading = false; return@LaunchedEffect }
                    val q = address.trim()
                    if (q.length < 2) { addrSuggestions = emptyList(); addrLoading = false; return@LaunchedEffect }
                    addrLoading = true
                    delay(250)
                    if (q == address.trim() && addrFocused) {
                        addrSuggestions = AddressAutocomplete.suggest(q)
                    }
                    addrLoading = false
                }
                // Auto-udfyld by naar postnr er 4 cifre og by-feltet er tomt
                LaunchedEffect(zip) {
                    if (zip.length == 4 && city.isBlank()) {
                        val c = AddressAutocomplete.cityForPostnr(zip)
                        if (c != null && city.isBlank()) city = c
                    }
                }

                // Vej – DAWA-forslag udfylder husnr, postnr og by automatisk
                OutlinedTextField(address, { address = it },
                    label = { Text("Søg adresse (vej + nr)") },
                    placeholder = { Text("fx Stationstorvet 1, Glostrup") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Next),
                    trailingIcon = {
                        if (addrLoading) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = WrombleRed)
                        } else if (address.isNotBlank()) {
                            IconButton(onClick = { address = ""; addrSuggestions = emptyList() }) {
                                Icon(Icons.Filled.Close, "Ryd")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                        .onFocusChanged { st ->
                            addrFocused = st.isFocused
                            if (!st.isFocused) addrSuggestions = emptyList()
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WrombleRed, focusedLabelColor = WrombleRed))

                // Forslags-liste (tryk for at udfylde alle felter)
                if (addrSuggestions.isNotEmpty()) {
                    Surface(
                        Modifier.fillMaxWidth().padding(top = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 3.dp
                    ) {
                        Column {
                            addrSuggestions.forEach { s ->
                                Row(
                                    Modifier.fillMaxWidth().clickableNoRipple {
                                        // Udfyld ALLE felter automatisk fra det valgte forslag
                                        address = s.vejnavn.ifBlank { s.text }
                                        if (s.husnr.isNotBlank()) husnr = s.husnr
                                        if (s.postnr.isNotBlank()) zip = s.postnr
                                        if (s.postnrnavn.isNotBlank()) city = s.postnrnavn
                                        addrFocused = false
                                        addrSuggestions = emptyList()
                                        focus.clearFocus()   // luk tastaturet naar adressen er valgt
                                    }.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.LocationOn, null, tint = WrombleRed,
                                        modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text(s.text, fontSize = 14.sp)
                                }
                                Divider(color = Color(0xFFEDEDF0))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                // Husnr + etage/dør
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(husnr, { husnr = it },
                        label = { Text("Husnr.") }, singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WrombleRed, focusedLabelColor = WrombleRed))
                    OutlinedTextField(floor, { floor = it },
                        label = { Text("Etage / dør (valgfri)") }, singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1.5f), shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WrombleRed, focusedLabelColor = WrombleRed))
                }

                Spacer(Modifier.height(8.dp))
                // Postnr + by
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(zip, { zip = it.filter { c -> c.isDigit() }.take(4) },
                        label = { Text("Postnr.") }, singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WrombleRed, focusedLabelColor = WrombleRed))
                    OutlinedTextField(city, { city = it },
                        label = { Text("By") }, singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { focus.clearFocus() }),
                        modifier = Modifier.weight(1.6f), shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WrombleRed, focusedLabelColor = WrombleRed))
                }

                TextButton(onClick = {
                    scope.launch {
                        val geo = withContext(Dispatchers.IO) { reverseGeocode(ctx, vm.userLat, vm.userLng) }
                        if (geo != null) {
                            address = geo.street; husnr = geo.husnr; zip = geo.zip; city = geo.city
                            addrFocused = false; addrSuggestions = emptyList()
                        } else error = "Kunne ikke finde din placering – tjek at placering er slået til"
                    }
                }, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                    Icon(Icons.Filled.MyLocation, null, tint = WrombleRed, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Brug min placering", color = WrombleRed, fontSize = 13.sp)
                }
            }

            // Time
            Spacer(Modifier.height(16.dp))
            Text("Tidspunkt", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ChoiceChip("Hurtigst muligt", !scheduleLater) { scheduleLater = false }
                ChoiceChip(if (scheduleLater) wantedLabel() else "Vælg tid", scheduleLater) { pickTime() }
            }

            // Tips (delivery only)
            if (isDelivery) {
                Spacer(Modifier.height(16.dp))
                Text("Drikkepenge til chaufføren", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 10, 20, 30).forEach { amount ->
                        ChoiceChip(if (amount == 0) "Ingen" else "$amount kr",
                            tip == amount && customTip.isBlank()) { tip = amount; customTip = "" }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(customTip, { customTip = it.filter { c -> c.isDigit() } },
                    label = { Text("Andet beløb (kr)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WrombleRed, focusedLabelColor = WrombleRed))
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(note, { note = it }, label = { Text("Bemærkning (valgfri)") },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WrombleRed, focusedLabelColor = WrombleRed))

            Spacer(Modifier.height(16.dp))
            Text("Betaling", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Column(Modifier.padding(top = 8.dp)) {
                PaymentRow("Kontanter", payment == 2) { payment = 2 }
                PaymentRow("Online betaling (Kort · MobilePay)", payment == 1) { payment = 1 }
            }

            if (error.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(error, color = WrombleRed)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun PaymentRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickableNoRipple(onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = WrombleRed))
        Text(label)
    }
}

@Composable
private fun OrderConfirmation(orderId: Int, tipAmount: Int, isCash: Boolean, onTrack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text("🎉", fontSize = 64.sp)
            Spacer(Modifier.height(12.dp))
            Text("Ordre modtaget!", fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text("Ordre #$orderId er modtaget", color = Color(0xFF8A8A90), fontSize = 15.sp)
            if (tipAmount >= 1) {
                Spacer(Modifier.height(6.dp))
                Text(
                    if (isCash) "Drikkepenge ($tipAmount kr) betales kontant til chaufføren"
                    else "Drikkepenge ($tipAmount kr) er lagt oven i din betaling",
                    color = Color(0xFF8A8A90), fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = onTrack, shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WrombleRed),
                modifier = Modifier.height(52.dp).fillMaxWidth()) {
                Text("Følg din ordre", fontWeight = FontWeight.Bold)
            }
        }
    }
}
