package dk.wromble.app.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dk.wromble.app.data.Favorites
import dk.wromble.app.data.Session
import dk.wromble.app.data.Settings
import dk.wromble.app.ui.screens.*

@Composable
fun AppRoot() {
    val nav = rememberNavController()
    val vm: MainViewModel = viewModel()
    val ctx = LocalContext.current

    LaunchedEffect(Unit) {
        Favorites.load(ctx)
        Session.load(ctx)
        Settings.load(ctx)
    }

    NavHost(navController = nav, startDestination = "splash") {
        composable("splash") { SplashScreen(nav) }
        composable("onboarding") { OnboardingScreen(nav) }
        composable("login") { LoginScreen(nav) }
        composable("main") { MainScaffold(nav, vm) }
        composable(
            "restaurant/{id}?table={table}",
            arguments = listOf(
                navArgument("id") { type = NavType.StringType },
                navArgument("table") { type = NavType.StringType; defaultValue = "" }
            )
        ) { entry ->
            val id = entry.arguments?.getString("id")?.toIntOrNull() ?: 0
            val table = entry.arguments?.getString("table")?.toIntOrNull()
            RestaurantScreen(nav, vm, id, table)
        }
        composable(
            "qrscan/{mode}",
            arguments = listOf(navArgument("mode") { type = NavType.StringType; defaultValue = "table" })
        ) { entry ->
            QrScannerScreen(nav, vm, entry.arguments?.getString("mode") ?: "table")
        }
        composable("cart") { CartScreen(nav, vm) }
        composable("orders") { OrdersScreen(nav, vm) }
        composable("tracking/{orderId}") { entry ->
            val oid = entry.arguments?.getString("orderId")?.toIntOrNull() ?: 0
            TrackingScreen(nav, oid)
        }
        composable("wromble-plus") { WromblePlusScreen(nav) }
        composable("profile/edit") { EditProfileScreen(nav) }
        composable("contact") { ContactScreen(nav) }
        composable("partner") { PartnerScreen(nav) }
        composable("jobs") { JobsScreen(nav) }
        composable("delete-account") { AccountDeletionScreen(nav) }
        composable("driver") { DriverDashboardScreen(nav) }
        composable("company") { CompanyDashboardScreen(nav) }
        composable("company/orders") { CompanyOrdersScreen(nav) }
        composable("company/menu") { CompanyMenuScreen(nav) }
        composable("company/hours") { CompanyHoursScreen(nav) }
        composable("company/profile") { CompanyProfileScreen(nav) }
        composable("earnings/{type}/{id}") { entry ->
            val type = entry.arguments?.getString("type") ?: "rider"
            val eid = entry.arguments?.getString("id")?.toIntOrNull() ?: 0
            EarningsScreen(nav, type, eid)
        }
    }
}
