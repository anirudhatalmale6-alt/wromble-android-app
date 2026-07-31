package dk.wromble.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Bro mellem MainActivity's deep link og login-skaermen for "Log ind med Facebook".
 * Vi bruger web-flowet (Custom Tab) og GENBRUGER hjemmesidens eksisterende Facebook-
 * opsaetning, saa der ikke skal saettes en native Facebook-SDK eller nye noegler op.
 * Naar web-flowet er faerdigt, aabner det app'en igen via deep link
 * wromble://fb-login?code=... MainActivity laegger koden her, og LoginScreen
 * observerer den, bytter den til en bruger (app-facebook-exchange.php) og logger ind.
 */
object FacebookAuth {
    var pendingCode by mutableStateOf<String?>(null)
}
