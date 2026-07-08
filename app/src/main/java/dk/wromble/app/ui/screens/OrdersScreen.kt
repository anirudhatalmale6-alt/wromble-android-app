package dk.wromble.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import dk.wromble.app.data.Session
import dk.wromble.app.ui.*
import dk.wromble.app.ui.theme.WrombleRed

@Composable
fun OrdersScreen(nav: NavController, vm: MainViewModel) {
    LaunchedEffect(Unit) { Session.user?.let { vm.loadOrders(it.id) } }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Text("Mine ordrer", Modifier.padding(start = 20.dp, top = 56.dp, bottom = 8.dp),
            fontSize = 24.sp, fontWeight = FontWeight.Black)

        if (vm.loadingOrders && vm.orders.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = WrombleRed) }
        } else if (vm.orders.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("Du har ingen ordrer endnu", color = Color(0xFF8A8A90))
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(vm.orders) { order ->
                    Card(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                            .clickableNoRipple { nav.navigate("tracking/${order.id}") },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Text(order.companyName, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                val (label, color) = orderStatusLabel(order.status)
                                Pill(label, color)
                            }
                            if (order.date.isNotBlank())
                                Text(order.date, fontSize = 12.sp, color = Color(0xFF8A8A90))
                            Spacer(Modifier.height(8.dp))
                            order.items.forEach { i ->
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                    Text("${i.quantity}x ${i.name}", fontSize = 14.sp, color = Color(0xFF6B6B72))
                                    Text(kr(i.price * i.quantity), fontSize = 14.sp, color = Color(0xFF6B6B72))
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(Modifier.fillMaxWidth(), Arrangement.End) {
                                Text("Total: ${kr(order.total)}", fontWeight = FontWeight.Bold, color = WrombleRed)
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}
