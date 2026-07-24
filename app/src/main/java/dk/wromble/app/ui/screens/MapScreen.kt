package dk.wromble.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import dk.wromble.app.data.LocationProvider
import dk.wromble.app.data.Restaurant
import dk.wromble.app.ui.MainViewModel
import dk.wromble.app.ui.NetworkImage
import dk.wromble.app.ui.clickableNoRipple
import dk.wromble.app.ui.components.*
import dk.wromble.app.ui.theme.WrombleRed
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

@Composable
fun MapScreen(nav: NavController, vm: MainViewModel) {
    val ctx = LocalContext.current
    val mapView = remember { newMapView(ctx) }
    var selectedId by remember { mutableStateOf<Int?>(null) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { if (vm.restaurants.isEmpty()) vm.loadHome() }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose { mapView.onPause() }
    }

    val withCoords = vm.restaurants.filter { it.lat != 0.0 && it.lng != 0.0 }

    // (re)build pins whenever the list changes
    LaunchedEffect(withCoords.size) {
        mapView.overlays.clear()
        withCoords.forEach { r ->
            mapView.addPin(r.lat, r.lng, r.name, WrombleRedInt) {
                selectedId = r.id
            }
        }
        if (withCoords.isNotEmpty()) {
            val first = withCoords.first()
            mapView.controller.setCenter(GeoPoint(first.lat, first.lng))
        }
        mapView.invalidate()
    }

    // center map + scroll carousel on selection
    LaunchedEffect(selectedId) {
        val r = withCoords.firstOrNull { it.id == selectedId } ?: return@LaunchedEffect
        mapView.controller.animateTo(GeoPoint(r.lat, r.lng))
        val idx = withCoords.indexOf(r)
        if (idx >= 0) listState.animateScrollToItem(idx)
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        // my-location button
        FilledIconButton(
            onClick = {
                val loc = LocationProvider.lastKnown(ctx)
                if (loc != null) mapView.controller.animateTo(GeoPoint(loc.latitude, loc.longitude))
            },
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 56.dp, end = 16.dp).size(46.dp),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surface)
        ) { Icon(Icons.Filled.MyLocation, "Min placering", tint = WrombleRed) }

        Text(
            "Kort", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black,
            modifier = Modifier.align(Alignment.TopStart).padding(top = 60.dp, start = 20.dp)
                .clip(RoundedCornerShape(8.dp)).background(WrombleRed).padding(horizontal = 12.dp, vertical = 4.dp)
        )

        if (withCoords.isNotEmpty()) {
            LazyRow(
                state = listState,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(withCoords) { r ->
                    MapRestaurantCard(r, selected = r.id == selectedId,
                        onClick = { nav.navigate("restaurant/${r.id}") },
                        onSelect = { selectedId = r.id })
                }
            }
        }
    }
}

@Composable
private fun MapRestaurantCard(r: Restaurant, selected: Boolean, onClick: () -> Unit, onSelect: () -> Unit) {
    Card(
        modifier = Modifier.width(260.dp)
            .clickableNoRipple { onSelect(); onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) WrombleRed.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            NetworkImage(r.image, Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(r.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                Text(r.address, fontSize = 12.sp, color = Color(0xFF8A8A90), maxLines = 1)
                Spacer(Modifier.height(6.dp))
                Text("Se menu", color = WrombleRed, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .background(WrombleRed.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 4.dp))
            }
        }
    }
}
