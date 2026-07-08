package dk.wromble.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dk.wromble.app.data.*
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    // Catalogue
    val restaurants = mutableStateListOf<Restaurant>()
    val categories = mutableStateListOf<ProductCat>()
    var loadingHome by mutableStateOf(false)
    var homeError by mutableStateOf<String?>(null)

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
            try {
                val r = Api.service.restaurants()
                restaurants.clear(); restaurants.addAll(r.restaurants)
                val c = Api.service.homeCategories()
                categories.clear(); categories.addAll(c.categories)
            } catch (e: Exception) {
                homeError = "Kunne ikke hente data. Tjek forbindelsen."
            } finally {
                loadingHome = false
            }
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
