package dk.wromble.app.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dk.wromble.app.WrombleApp
import dk.wromble.app.data.Api
import dk.wromble.app.data.Favorites
import dk.wromble.app.data.Notifier
import dk.wromble.app.data.Session
import dk.wromble.app.data.Settings
import dk.wromble.app.ui.screens.*
import kotlinx.coroutines.delay

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

    // Global lyd-vagt: spiller alarmen ved nye ordrer for forretningen UANSET hvilken
    // firma-skaerm der vises (dashboard, menu, ordrer, ...) - saa lyden ikke kun virker
    // paa ordre-listen. Kun aktiv naar man er logget ind som forretning.
    CompanyOrderWatcher()

    // VIGTIGT: skaerm-overgange slaaet FRA. Material3's Scaffold-layout (1.2's
    // "MeasureFix") korrumperer Compose SlotTable naar en Scaffold maales inde i
    // en AnimatedContent-overgang (nav-animationen) -> ArrayIndexOutOfBounds-crash
    // ved navigation (bestil/foelg ordre). Uden overgangsanimation komponeres/maales
    // hver skaerm statisk EN gang, og den defekte maale-sti udloeses aldrig.
    NavHost(
        navController = nav,
        startDestination = "splash",
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
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

// App-global vagt der poller forretningens aktive ordrer og spiller alarmen ved nye
// ordrer - uafhaengigt af hvilken skaerm der vises. Loeber i ét langt loop og laeser
// Session.user hver runde, saa den selv starter/stopper ved login/logout uden at
// afhaenge af recomposition. seconds/melodi hentes fra serveren (firmaets valg).
@Composable
private fun CompanyOrderWatcher() {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) {
        var seen = emptySet<Int>()
        var firstLoad = true
        var watchedCompany = -1
        var alarmSeconds = 10
        while (true) {
            val u = Session.user
            val cid = u?.companyId ?: 0
            val isCompany = u != null && u.type == "company" && cid > 0
            if (isCompany) {
                // Ny/skiftet firma-session: nulstil baseline og hent lyd-indstilling.
                if (cid != watchedCompany) {
                    watchedCompany = cid; firstLoad = true; seen = emptySet()
                    runCatching { alarmSeconds = Api.service.companyAlarm(cid).alarmSeconds }
                }
                runCatching {
                    val r = Api.service.companyOrders(cid, "active")
                    val newOnes = r.orders.filter { it.id !in seen }
                    // Alarmér ved enhver ny ordre EFTER den foerste indlaesning (ogsaa den
                    // allerfoerste ordre naar listen var tom - den gamle "seen ikke tom"-
                    // betingelse sprang den over).
                    if (!firstLoad && newOnes.isNotEmpty() && alarmSeconds > 0) {
                        Notifier.playAlarm(ctx, alarmSeconds)
                        Notifier.notify(ctx, 3001, "Ny ordre!", "Du har ${newOnes.size} ny(e) ordre(r)", WrombleApp.CH_ORDERS)
                    }
                    seen = r.orders.map { it.id }.toSet()
                    firstLoad = false
                }
            } else {
                watchedCompany = -1
            }
            delay(6000)
        }
    }
}
