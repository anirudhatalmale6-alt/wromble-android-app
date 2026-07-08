package dk.wromble.app.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dk.wromble.app.data.Favorites
import dk.wromble.app.data.Session
import dk.wromble.app.ui.screens.*

@Composable
fun AppRoot() {
    val nav = rememberNavController()
    val vm: MainViewModel = viewModel()
    val ctx = LocalContext.current

    LaunchedEffect(Unit) {
        Favorites.load(ctx)
        Session.load(ctx)
    }

    NavHost(navController = nav, startDestination = "splash") {
        composable("splash") { SplashScreen(nav) }
        composable("login") { LoginScreen(nav) }
        composable("main") { MainScaffold(nav, vm) }
        composable("restaurant/{id}") { entry ->
            val id = entry.arguments?.getString("id")?.toIntOrNull() ?: 0
            RestaurantScreen(nav, vm, id)
        }
        composable("cart") { CartScreen(nav, vm) }
        composable("orders") { OrdersScreen(nav, vm) }
        composable("tracking/{orderId}") { entry ->
            val oid = entry.arguments?.getString("orderId")?.toIntOrNull() ?: 0
            TrackingScreen(nav, oid)
        }
        composable("driver") { DriverDashboardScreen(nav) }
        composable("company") { CompanyDashboardScreen(nav) }
    }
}
