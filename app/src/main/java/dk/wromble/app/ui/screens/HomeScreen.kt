package dk.wromble.app.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
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
    // Responsivt kolonneantal: tablets/brede skærme fylder hele bredden ud
    // (i stedet for 2 store, udtværede felter) – giver skarpere billeder.
    val screenW = LocalConfiguration.current.screenWidthDp
    val productCols = when {
        screenW >= 1000 -> 5
        screenW >= 820  -> 4
        screenW >= 600  -> 3
        else            -> 2
    }
    val restaurantCols = if (screenW >= 600) 2 else 1
    var query by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf<String?>(null) } // null = Alle

    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) vm.refreshLocation(ctx)
    }
    LaunchedEffect(Unit) {
        if (LocationProvider.hasPermission(ctx)) vm.refreshLocation(ctx)
        else if (Settings.locationEnabled) locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    val favs = vm.restaurants.filter { Favorites.isFavorite(it.id) }
    val q = query.trim().lowercase()

    val sortedRestaurants = remember(vm.restaurants.toList(), vm.userLat, vm.userLng, query) {
        vm.restaurantsSorted().filter {
            q.isBlank() || it.name.lowercase().contains(q) || it.address.lowercase().contains(q)
        }
    }
    val activeCat = vm.categories.firstOrNull { it.key == selectedCat }
    val catProducts = activeCat?.products.orEmpty().filter {
        q.isBlank() || it.name.lowercase().contains(q)
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // Header
            item {
                Box(
                    Modifier.fillMaxWidth().background(brandGradient)
                        .padding(start = 20.dp, end = 16.dp, top = 50.dp, bottom = 18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Hej ${Session.user?.name?.substringBefore(" ") ?: "der"} 👋",
                                color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black)
                            Text(if (vm.userLat != 0.0) "Steder i nærheden af dig" else "Hvad har du lyst til i dag?",
                                color = Color.White.copy(alpha = 0.92f), fontSize = 14.sp)
                        }
                        FilledIconButton(
                            onClick = { nav.navigate("qrscan/table") },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White)
                        ) { Icon(Icons.Filled.QrCodeScanner, "Scan", tint = WrombleRed) }
                    }
                }
            }

            // Search
            item {
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    placeholder = { Text("Søg efter mad eller sted") },
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = WrombleRed) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WrombleRed)
                )
            }

            // Categories row (with "Alle")
            if (vm.categories.isNotEmpty()) {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            CategoryTile("Alle", null, selectedCat == null, "✨") { selectedCat = null }
                        }
                        items(vm.categories) { cat ->
                            CategoryTile(cat.name, cat.image, selectedCat == cat.key, catEmoji(cat.key)) {
                                selectedCat = if (selectedCat == cat.key) null else cat.key
                            }
                        }
                    }
                }
            }

            // Promo band + scan banners (only when browsing, not searching)
            if (q.isBlank() && selectedCat == null) {
                item {
                    Spacer(Modifier.height(12.dp))
                    WromblePlusBand(onClick = { nav.navigate("wromble-plus") })
                    Spacer(Modifier.height(12.dp))
                    ScanBanner(
                        title = "Scan bordets QR-kode",
                        subtitle = "Sæt dig, scan og bestil direkte fra bordet",
                        icon = Icons.Filled.QrCodeScanner,
                        onClick = { nav.navigate("qrscan/table") }
                    )
                    Spacer(Modifier.height(10.dp))
                    ScanBanner(
                        title = "Scan i butikken",
                        subtitle = "Spring køen over – scan og bestil",
                        icon = Icons.Filled.Storefront,
                        onClick = { nav.navigate("qrscan/store") }
                    )
                }

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
            }

            // Product tiles when a category is selected, else restaurant cards
            if (activeCat != null) {
                item {
                    Text(activeCat.name, Modifier.padding(start = 20.dp, top = 18.dp, bottom = 8.dp),
                        fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                if (catProducts.isEmpty()) {
                    item { EmptyNote("Ingen produkter i denne kategori endnu") }
                } else {
                    items(catProducts.chunked(productCols)) { rowItems ->
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { p ->
                                Box(Modifier.weight(1f)) {
                                    ProductTileCard(p) {
                                        if (p.companyId != 0) nav.navigate("restaurant/${p.companyId}")
                                    }
                                }
                            }
                            // udfyld sidste række så felterne beholder samme bredde
                            repeat(productCols - rowItems.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            } else {
                item {
                    Text(if (q.isBlank()) "Spisesteder i nærheden" else "Resultater",
                        Modifier.padding(start = 20.dp, top = 18.dp, bottom = 8.dp),
                        fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                if (vm.loadingHome && vm.restaurants.isEmpty()) {
                    item { Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) { CircularProgressIndicator(color = WrombleRed) } }
                }
                vm.homeError?.let { err ->
                    item {
                        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(err, color = Color(0xFF8A8A90))
                            TextButton(onClick = { vm.loadHome() }) { Text("Prøv igen", color = WrombleRed) }
                        }
                    }
                }
                if (!vm.loadingHome && sortedRestaurants.isEmpty()) {
                    item { EmptyNote("Ingen steder fundet") }
                }
                items(sortedRestaurants.chunked(restaurantCols)) { rowRest ->
                    if (restaurantCols == 1) {
                        val r = rowRest[0]
                        RestaurantCard(r,
                            distance = distanceLabel(vm.distanceTo(r)),
                            onClick = { nav.navigate("restaurant/${r.id}") },
                            onFav = { Favorites.toggle(ctx, r.id) },
                            isFav = Favorites.isFavorite(r.id))
                    } else {
                        // tablet: to spisesteder side om side, fylder bredden ud
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                            rowRest.forEach { r ->
                                Box(Modifier.weight(1f)) {
                                    RestaurantCard(r,
                                        distance = distanceLabel(vm.distanceTo(r)),
                                        onClick = { nav.navigate("restaurant/${r.id}") },
                                        onFav = { Favorites.toggle(ctx, r.id) },
                                        isFav = Favorites.isFavorite(r.id))
                                }
                            }
                            repeat(restaurantCols - rowRest.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(90.dp)) }
        }

        // Floating cart bar
        if (Cart.itemCount > 0) {
            Surface(
                shadowElevation = 12.dp,
                color = WrombleRed,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
                    .fillMaxWidth().clickableNoRipple { nav.navigate("cart") }
            ) {
                Row(
                    Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${Cart.itemCount} i kurven", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("Se kurv · ${kr(Cart.total)}", color = Color.White, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

private fun catEmoji(key: String): String = when {
    key.contains("varme") -> "🍽️"
    key.contains("kolde") -> "🥗"
    key.contains("drikke") -> "🥤"
    key.contains("slik") -> "🍰"
    key.contains("dessert") -> "🍨"
    else -> "🍴"
}

@Composable
private fun EmptyNote(text: String) {
    Box(Modifier.fillMaxWidth().padding(30.dp), Alignment.Center) {
        Text(text, color = Color(0xFF8A8A90))
    }
}

@Composable
fun CategoryTile(name: String, image: String?, selected: Boolean, emoji: String, onClick: () -> Unit) {
    Column(
        Modifier.width(90.dp).clickableNoRipple(onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(84.dp).clip(RoundedCornerShape(20.dp))
                .background(if (selected) WrombleRed else Color(0xFFEDEDF0)),
            contentAlignment = Alignment.Center
        ) {
            if (image != null) {
                NetworkImage(image, Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)), ContentScale.Crop)
                Box(Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = if (selected) 0.30f else 0.22f)))
            } else {
                Text(emoji, fontSize = 34.sp)
            }
            if (selected) Text(emoji, fontSize = 30.sp)
        }
        Text(name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 2,
            color = if (selected) WrombleRed else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun ScanBanner(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF1A0D0E), WrombleDarkRed)))
            .clickableNoRipple(onClick).padding(16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Color.White),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = WrombleRed, modifier = Modifier.size(28.dp)) }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun ProductTileCard(p: CatProduct, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickableNoRipple(onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            NetworkImage(p.image, Modifier.fillMaxWidth().height(110.dp))
            Column(Modifier.padding(10.dp)) {
                Text(p.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                p.companyName?.let { Text(it, fontSize = 12.sp, color = Color(0xFF8A8A90), maxLines = 1) }
                p.price?.let {
                    Text(kr(it), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WrombleRed,
                        modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}

@Composable
fun RestaurantCard(r: Restaurant, distance: String = "", onClick: () -> Unit, onFav: () -> Unit, isFav: Boolean) {
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
                Box(Modifier.padding(12.dp)) { Pill(r.typeLabel.ifBlank { "Spisested" }, WrombleRed) }
                Box(Modifier.align(Alignment.TopEnd).padding(12.dp)
                    .size(38.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.92f))
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
                    Pill(if (distance.isNotBlank()) "📍 $distance" else "📍 I nærheden", Color(0xFFF0F0F3), Color(0xFF444444))
                    Pill("Gratis levering", Color(0xFFE7F7EC), Color(0xFF16A34A))
                }
            }
        }
    }
}
