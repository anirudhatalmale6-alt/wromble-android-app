package dk.wromble.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Bro mellem MainActivity's deep link og login-skaermen for "Log ind med Apple".
 * Apple har ingen login-SDK til Android, saa login sker via et web-flow (Custom Tab).
 * Naar web-flowet er faerdigt, aabner det app'en igen via deep link
 * wromble://apple-login?code=... MainActivity laegger koden her, og LoginScreen
 * observerer den, bytter den til en bruger (app-apple-exchange.php) og logger ind.
 */
object AppleAuth {
    var pendingCode by mutableStateOf<String?>(null)
}
