package dk.wromble.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import dk.wromble.app.data.*
import dk.wromble.app.ui.*
import dk.wromble.app.ui.theme.WrombleDarkRed
import dk.wromble.app.ui.theme.WrombleRed

@Composable
fun HomeScreen(nav: NavController, vm: MainViewModel) {
    val ctx = LocalContext.current
    val favs = vm.restaurants.filter { Favorites.isFavorite(it.id) }

    LazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Header with logo/greeting
        item {
            Box(
                Modifier.fillMaxWidth().background(brandGradient)
                    .padding(start = 20.dp, end = 20.dp, top = 54.dp, bottom = 22.dp)
            ) {
                Column {
                    Text("Hej ${Session.user?.name?.substringBefore(" ") ?: "der"} 👋",
                        color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
                    Text("Hvad har du lyst til i dag?",
                        color = Color.White.copy(alpha = 0.92f), fontSize = 15.sp)
                }
            }
        }

        // Scan bordet banner
        item {
            Box(
                Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(20.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF1A0D0E), WrombleDarkRed)))
                    .padding(18.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(Color.White),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Filled.QrCodeScanner, null, tint = WrombleRed, modifier = Modifier.size(30.dp)) }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Scan bordet", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Bestil direkte fra dit bord – spring koeen over",
                            color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                    }
                }
            }
        }

        // Categories row
        if (vm.categories.isNotEmpty()) {
            item {
                Text("Kategorier", Modifier.padding(start = 20.dp, top = 4.dp, bottom = 8.dp),
                    fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(vm.categories) { cat -> CategoryChip(cat) }
                }
            }
        }

        // Favorites
        if (favs.isNotEmpty()) {
            item {
                Text("Dine favoritter", Modifier.padding(start = 20.dp, top = 18.dp, bottom = 8.dp),
                    fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(favs) { r ->
                        Column(Modifier.width(150.dp).clickableNoRipple { nav.navigate("restaurant/${r.id}") }) {
                            NetworkImage(r.image, Modifier.fillMaxWidth().height(96.dp).clip(RoundedCornerShape(16.dp)))
                            Text(r.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1,
                                modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
            }
        }

        item {
            Text("Spisesteder i naerheden", Modifier.padding(start = 20.dp, top = 18.dp, bottom = 8.dp),
                fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        if (vm.loadingHome && vm.restaurants.isEmpty()) {
            item { Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) { CircularProgressIndicator(color = WrombleRed) } }
        }
        vm.homeError?.let { err ->
            item {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(err, color = Color(0xFF8A8A90))
                    TextButton(onClick = { vm.loadHome() }) { Text("Proev igen", color = WrombleRed) }
                }
            }
        }

        items(vm.restaurants) { r ->
            RestaurantCard(r, onClick = { nav.navigate("restaurant/${r.id}") },
                onFav = { Favorites.toggle(ctx, r.id) },
                isFav = Favorites.isFavorite(r.id))
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
fun CategoryChip(cat: ProductCat) {
    Column(
        Modifier.width(96.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(96.dp).clip(RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            NetworkImage(cat.image, Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)), ContentScale.Crop)
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp))
                .background(Color.Black.copy(alpha = 0.28f)))
        }
        Text(cat.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 2,
            modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
fun RestaurantCard(r: Restaurant, onClick: () -> Unit, onFav: () -> Unit, isFav: Boolean) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
            .clickableNoRipple(onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column {
            Box {
                NetworkImage(r.image, Modifier.fillMaxWidth().height(160.dp))
                // type badge
                Box(Modifier.padding(12.dp)) { Pill(r.typeLabel.ifBlank { "Spisested" }, WrombleRed) }
                // favourite heart
                Box(Modifier.align(Alignment.TopEnd).padding(12.dp)
                    .size(38.dp).clip(RoundedCornerShape(19.dp)).background(Color.White.copy(alpha = 0.92f))
                    .clickableNoRipple(onFav), contentAlignment = Alignment.Center) {
                    Icon(if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        null, tint = WrombleRed, modifier = Modifier.size(22.dp))
                }
            }
            Column(Modifier.padding(14.dp)) {
                Text(r.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(r.address, fontSize = 13.sp, color = Color(0xFF8A8A90), maxLines = 1)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("📍 I naerheden", Color(0xFFF0F0F3), Color(0xFF444))
                    Pill("Gratis levering", Color(0xFFE7F7EC), Color(0xFF16A34A))
                }
            }
        }
    }
}
