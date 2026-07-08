package dk.wromble.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import dk.wromble.app.data.Cart
import dk.wromble.app.data.Session
import dk.wromble.app.ui.MainViewModel
import dk.wromble.app.ui.theme.WrombleRed

private data class Tab(val label: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(nav: NavController, vm: MainViewModel) {
    var selected by remember { mutableStateOf(0) }
    val tabs = listOf(
        Tab("Forside", Icons.Filled.Home),
        Tab("Restauranter", Icons.Filled.Restaurant),
        Tab("Ordrer", Icons.Filled.ReceiptLong),
        Tab("Profil", Icons.Filled.Person)
    )

    LaunchedEffect(Unit) { if (vm.restaurants.isEmpty()) vm.loadHome() }
    LaunchedEffect(selected) {
        if (selected == 2) Session.user?.let { vm.loadOrders(it.id) }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = selected == i,
                        onClick = { selected = i },
                        icon = {
                            BadgedBox(badge = {
                                if (i == 2 && Cart.itemCount > 0) Badge { Text("${Cart.itemCount}") }
                            }) { Icon(t.icon, contentDescription = t.label) }
                        },
                        label = { Text(t.label, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = WrombleRed,
                            selectedTextColor = WrombleRed,
                            indicatorColor = WrombleRed.copy(alpha = 0.12f)
                        )
                    )
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad)) {
            when (selected) {
                0 -> HomeScreen(nav, vm)
                1 -> RestaurantsListScreen(nav, vm)
                2 -> OrdersScreen(nav, vm)
                else -> ProfileScreen(nav)
            }
        }
    }
}
