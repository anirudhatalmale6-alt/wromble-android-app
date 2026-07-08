package dk.wromble.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
fun RestaurantScreen(nav: NavController, vm: MainViewModel, id: Int) {
    val ctx = LocalContext.current
    val restaurant = vm.restaurantById(id)
    LaunchedEffect(id) { vm.loadMenu(id) }

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
                            .size(40.dp).clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.9f))) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.Black)
                    }
                }
            }
            item {
                Column(Modifier.padding(20.dp)) {
                    Text(restaurant?.name ?: "", fontSize = 26.sp, fontWeight = FontWeight.Black)
                    Text(restaurant?.address ?: "", fontSize = 14.sp, color = Color(0xFF8A8A90))
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill("🕐 25-40 min", Color(0xFFF0F0F3), Color(0xFF444))
                        Pill("Gratis levering", Color(0xFFE7F7EC), Color(0xFF16A34A))
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
                    MenuItemRow(item) {
                        restaurant?.let { Cart.add(item, it.id, it.name) }
                    }
                }
            }
            item { Spacer(Modifier.height(30.dp)) }
        }
    }
}

@Composable
fun MenuItemRow(item: MenuItem, onAdd: () -> Unit) {
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
        FilledIconButton(onClick = onAdd,
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = WrombleRed)) {
            Icon(Icons.Filled.Add, "Tilfoej", tint = Color.White)
        }
    }
}
