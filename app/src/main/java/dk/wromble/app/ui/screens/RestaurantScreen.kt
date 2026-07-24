package dk.wromble.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Share
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
import dk.wromble.app.ui.*
import dk.wromble.app.ui.theme.WrombleRed

@Composable
fun RestaurantsListScreen(nav: NavController, vm: MainViewModel) {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { if (vm.restaurants.isEmpty()) vm.loadHome() }
    LazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        item {
            Text("Restauranter & butikker",
                Modifier.padding(start = 20.dp, top = 56.dp, bottom = 12.dp),
                fontSize = 24.sp, fontWeight = FontWeight.Black)
        }
        items(vm.restaurants) { r ->
            RestaurantCard(r, onClick = { nav.navigate("restaurant/${r.id}") },
                onFav = { Favorites.toggle(ctx, r.id) }, isFav = Favorites.isFavorite(r.id))
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantScreen(nav: NavController, vm: MainViewModel, id: Int, table: Int? = null) {
    val ctx = LocalContext.current
    val restaurant = vm.restaurantById(id)
    var openState by remember { mutableStateOf(ShopOpenState(true)) }
    var showClosed by remember { mutableStateOf(false) }
    var isFav by remember { mutableStateOf(Favorites.isFavorite(id)) }

    LaunchedEffect(id) {
        vm.loadMenu(id)
        try {
            val h = Api.service.companyHours(id)
            openState = wrombleShopOpenState(h.days, h.shopStatus)
        } catch (_: Exception) {}
    }

    fun tryAdd(item: MenuItem) {
        if (!openState.isOpen) { showClosed = true; return }
        restaurant?.let { Cart.add(item, it.id, it.name) }
    }

    fun shareRestaurant() {
        val url = "$BASE_URL/${restaurant?.alias?.ifBlank { "" } ?: ""}"
        runCatching {
            ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "${restaurant?.name} på Wromble: $url")
            }, "Del"))
        }
    }

    fun openInMaps() {
        val r = restaurant ?: return
        val uri = if (r.lat != 0.0 || r.lng != 0.0)
            Uri.parse("geo:${r.lat},${r.lng}?q=${r.lat},${r.lng}(${Uri.encode(r.name)})")
        else Uri.parse("geo:0,0?q=${Uri.encode(r.address)}")
        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
    }

    Scaffold(
        bottomBar = {
            if (Cart.itemCount > 0 && Cart.restaurantId == id) {
                Surface(shadowElevation = 12.dp, color = MaterialTheme.colorScheme.surface) {
                    Button(
                        onClick = { nav.navigate("cart") },
                        modifier = Modifier.fillMaxWidth().padding(16.dp).height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WrombleRed)
                    ) {
                        Text("Se kurv (${Cart.itemCount}) · ${kr(Cart.total)}",
                            fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(bottom = pad.calculateBottomPadding())
            .background(MaterialTheme.colorScheme.background)) {
            item {
                Box {
                    NetworkImage(restaurant?.image, Modifier.fillMaxWidth().height(220.dp))
                    IconButton(onClick = { nav.popBackStack() },
                        modifier = Modifier.padding(top = 40.dp, start = 12.dp)
                            .size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.9f))) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.Black)
                    }
                    Row(Modifier.align(Alignment.TopEnd).padding(top = 40.dp, end = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RoundIcon(if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder) {
                            Favorites.toggle(ctx, id); isFav = !isFav
                        }
                        RoundIcon(Icons.Filled.Share) { shareRestaurant() }
                        RoundIcon(Icons.Filled.Map) { openInMaps() }
                    }
                }
            }
            item {
                Column(Modifier.padding(20.dp)) {
                    Text(restaurant?.name ?: "", fontSize = 26.sp, fontWeight = FontWeight.Black)
                    Text(restaurant?.address ?: "", fontSize = 14.sp, color = Color(0xFF8A8A90))
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (openState.isOpen) Pill("● Åbent nu", Color(0xFFE7F7EC), Color(0xFF16A34A))
                        else Pill("● Lukket", Color(0xFFFBE7E7), WrombleRed)
                        Pill("Gratis levering", Color(0xFFF0F0F3), Color(0xFF444444))
                    }
                    if (!openState.isOpen) openState.reopenText?.let {
                        Text(it, fontSize = 13.sp, color = WrombleRed, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }

            // Table banner
            if (table != null) {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp)).background(WrombleRed.copy(alpha = 0.10f))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🍽️", fontSize = 22.sp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Bord $table", fontWeight = FontWeight.Bold, color = WrombleRed)
                            Text("Bestil her, så serverer vi ved dit bord", fontSize = 13.sp, color = Color(0xFF6B6B72))
                        }
                    }
                }
            }

            if (vm.loadingMenu) {
                item { Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) { CircularProgressIndicator(color = WrombleRed) } }
            }

            vm.menuCategories.forEach { cat ->
                item {
                    Text(cat.name, Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp),
                        fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                items(cat.products) { item ->
                    val qty = Cart.items.firstOrNull { it.id == item.id }?.quantity ?: 0
                    MenuItemRow(
                        item = item, quantity = if (Cart.restaurantId == id) qty else 0,
                        onAdd = { tryAdd(item) },
                        onMinus = { Cart.setQuantity(item.id, (Cart.items.firstOrNull { it.id == item.id }?.quantity ?: 1) - 1) }
                    )
                }
            }
            item { Spacer(Modifier.height(30.dp)) }
        }
    }

    if (showClosed) {
        AlertDialog(
            onDismissRequest = { showClosed = false },
            confirmButton = { TextButton(onClick = { showClosed = false }) { Text("OK", color = WrombleRed) } },
            title = { Text("Butikken er lukket") },
            text = { Text("Du kan ikke bestille lige nu. ${openState.reopenText ?: ""}") }
        )
    }
}

@Composable
private fun RoundIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.9f))
            .clickableNoRipple(onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = WrombleRed, modifier = Modifier.size(22.dp)) }
}

@Composable
fun MenuItemRow(item: MenuItem, quantity: Int, onAdd: () -> Unit, onMinus: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(item.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            item.description?.takeIf { it.isNotBlank() }?.let {
                Text(it, fontSize = 13.sp, color = Color(0xFF8A8A90), maxLines = 2)
            }
            Text(kr(item.price), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = WrombleRed,
                modifier = Modifier.padding(top = 4.dp))
        }
        Spacer(Modifier.width(12.dp))
        NetworkImage(item.image, Modifier.size(72.dp).clip(RoundedCornerShape(14.dp)))
        Spacer(Modifier.width(10.dp))
        if (quantity > 0) {
            QtyStepper(quantity, onMinus = onMinus, onPlus = onAdd)
        } else {
            FilledIconButton(onClick = onAdd,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = WrombleRed)) {
                Icon(Icons.Filled.Add, "Tilføj", tint = Color.White)
            }
        }
    }
}
