package dk.wromble.app.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.wromble.app.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    // Catalogue
    val restaurants = mutableStateListOf<Restaurant>()
    val categories = mutableStateListOf<ProductCat>()
    var loadingHome by mutableStateOf(false)
    var homeError by mutableStateOf<String?>(null)

    // User location (for distance sort)
    var userLat by mutableStateOf(0.0)
    var userLng by mutableStateOf(0.0)

    fun refreshLocation(ctx: Context) {
        val loc = LocationProvider.lastKnown(ctx) ?: return
        userLat = loc.latitude
        userLng = loc.longitude
    }

    fun restaurantsSorted(): List<Restaurant> {
        if (userLat == 0.0 && userLng == 0.0) return restaurants
        return restaurants.sortedBy { distanceKm(userLat, userLng, it.lat, it.lng) }
    }

    fun distanceTo(r: Restaurant): Double {
        if (userLat == 0.0 && userLng == 0.0) return Double.MAX_VALUE
        return distanceKm(userLat, userLng, r.lat, r.lng)
    }

    // Current menu
    val menuCategories = mutableStateListOf<MenuCategory>()
    var loadingMenu by mutableStateOf(false)

    // Orders
    val orders = mutableStateListOf<Order>()
    var loadingOrders by mutableStateOf(false)

    fun loadHome() {
        loadingHome = true
        homeError = null
        viewModelScope.launch {
            // Spisesteder (vigtigst): forsøg op til 3 gange ved en kortvarig
            // netvaerks-/serverhikke, saa forsiden ikke ender tom paa grund af
            // ét mislykket kald. Tidligere data bevares indtil et nyt hentes.
            var loaded = false
            var attempt = 0
            while (!loaded && attempt < 3) {
                try {
                    val r = Api.service.restaurants()
                    restaurants.clear(); restaurants.addAll(r.restaurants)
                    loaded = true
                } catch (_: Exception) {
                    attempt++
                    if (attempt < 3) delay(700L * attempt)
                }
            }
            homeError = if (loaded || restaurants.isNotEmpty()) null
                        else "Kunne ikke hente data. Tjek forbindelsen."

            // Kategorier (sekundaert): best effort – maa aldrig blokere spisestederne.
            try {
                val c = Api.service.homeCategories()
                categories.clear(); categories.addAll(c.categories)
            } catch (_: Exception) {}

            loadingHome = false
        }
    }

    fun loadMenu(companyId: Int) {
        loadingMenu = true
        menuCategories.clear()
        viewModelScope.launch {
            try {
                val m = Api.service.menu(companyId)
                menuCategories.addAll(m.categories)
            } catch (_: Exception) {
            } finally {
                loadingMenu = false
            }
        }
    }

    fun loadOrders(userId: Int) {
        loadingOrders = true
        viewModelScope.launch {
            try {
                val o = Api.service.orders(userId)
                orders.clear(); orders.addAll(o.orders)
            } catch (_: Exception) {
            } finally {
                loadingOrders = false
            }
        }
    }

    fun restaurantById(id: Int): Restaurant? = restaurants.firstOrNull { it.id == id }
}
