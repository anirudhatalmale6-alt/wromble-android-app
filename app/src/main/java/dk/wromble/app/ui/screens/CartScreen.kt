package dk.wromble.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import dk.wromble.app.data.*
import dk.wromble.app.ui.MainViewModel
import dk.wromble.app.ui.clickableNoRipple
import dk.wromble.app.ui.kr
import dk.wromble.app.ui.theme.WrombleRed
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(nav: NavController, vm: MainViewModel) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var isDelivery by remember { mutableStateOf(true) }
    var address by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var payment by remember { mutableStateOf("Kontant ved levering") }
    var ordering by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var placedOrderId by remember { mutableStateOf<Int?>(null) }

    fun placeOrder() {
        val user = Session.user
        if (user == null) { error = "Du skal vaere logget ind"; return }
        if (Cart.items.isEmpty()) return
        if (isDelivery && address.isBlank()) { error = "Indtast leveringsadresse"; return }
        error = ""; ordering = true
        scope.launch {
            try {
                val resp = Api.service.placeOrder(mapOf(
                    "user_id" to user.id,
                    "company_id" to Cart.restaurantId,
                    "total" to Cart.total,
                    "note" to note,
                    "delivery_check" to if (isDelivery) 1 else 0,
                    "payment_method" to payment,
                    "delivery_address" to address,
                    "items" to Cart.items.map { mapOf("id" to it.id, "quantity" to it.quantity) }
                ))
                ordering = false
                if (resp.error != null) { error = resp.error; return@launch }
                if (resp.orderId != null) {
                    placedOrderId = resp.orderId
                    Cart.clear()
                }
            } catch (e: Exception) { ordering = false; error = "Netvaerksfejl. Proev igen." }
        }
    }

    if (placedOrderId != null) {
        OrderConfirmation(placedOrderId!!) {
            nav.navigate("tracking/${placedOrderId}") { popUpTo("main") }
        }
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
                Surface(shadowElevation = 12.dp, color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Total", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(kr(Cart.total), fontSize = 18.sp, fontWeight = FontWeight.Black, color = WrombleRed)
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
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState())
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
                OutlinedTextField(address, { address = it }, label = { Text("Leveringsadresse") },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WrombleRed, focusedLabelColor = WrombleRed))
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(note, { note = it }, label = { Text("Bemaerkning (valgfri)") },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WrombleRed, focusedLabelColor = WrombleRed))

            Spacer(Modifier.height(16.dp))
            Text("Betaling", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Column(Modifier.padding(top = 8.dp)) {
                listOf("Kontant ved levering", "MobilePay", "Kort").forEach { m ->
                    Row(Modifier.fillMaxWidth().clickableNoRipple { payment = m }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = payment == m, onClick = { payment = m },
                            colors = RadioButtonDefaults.colors(selectedColor = WrombleRed))
                        Text(m)
                    }
                }
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
private fun QtyStepper(qty: Int, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FilledIconButton(onClick = onMinus, modifier = Modifier.size(34.dp),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFEDEDF0))) {
            Icon(Icons.Filled.Remove, "-", tint = Color.Black, modifier = Modifier.size(18.dp))
        }
        Text("$qty", Modifier.padding(horizontal = 12.dp), fontWeight = FontWeight.Bold)
        FilledIconButton(onClick = onPlus, modifier = Modifier.size(34.dp),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = WrombleRed)) {
            Icon(Icons.Filled.Add, "+", tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(12.dp))
            .background(if (selected) WrombleRed else Color(0xFFEDEDF0))
            .clickableNoRipple(onClick).padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(label, color = if (selected) Color.White else Color(0xFF444), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun OrderConfirmation(orderId: Int, onTrack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text("🎉", fontSize = 64.sp)
            Spacer(Modifier.height(12.dp))
            Text("Tak for din bestilling!", fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text("Ordre #$orderId er modtaget", color = Color(0xFF8A8A90), fontSize = 15.sp)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onTrack, shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WrombleRed),
                modifier = Modifier.height(52.dp)) {
                Text("Foelg din ordre", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp))
            }
        }
    }
}
