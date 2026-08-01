package dk.wromble.app

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dk.wromble.app.data.AppleAuth
import dk.wromble.app.data.FacebookAuth
import dk.wromble.app.data.Favorites
import dk.wromble.app.data.Session
import dk.wromble.app.data.Settings
import dk.wromble.app.ui.AppRoot
import dk.wromble.app.ui.BiometricLockScreen
import dk.wromble.app.ui.canUseBiometric
import dk.wromble.app.ui.theme.WrombleTheme

class MainActivity : FragmentActivity() {

    // Runtime-tilladelse til notifikationer. Uden denne viser Android 13+ (API 33+)
    // INGEN notifikationer, selv om de er deklareret i manifestet - derfor kom der
    // ingen notifikationer paa nyere Android-enheder (fx tablet'en i restauranten).
    private val notifPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load persisted state up-front so the lock decision is correct on cold start.
        Settings.load(this)
        Session.load(this)
        Favorites.load(this)

        // Bed om notifikations-tilladelsen paa Android 13+ hvis den ikke allerede er givet.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            runCatching { notifPermLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS) }
        }

        val lockAtStart = Settings.biometricEnabled && Session.user != null && canUseBiometric(this)

        setContent {
            WrombleTheme {
                var locked by remember { mutableStateOf(lockAtStart) }
                if (locked) BiometricLockScreen(onUnlock = { locked = false })
                else AppRoot()
            }
        }

        // "Log ind med Apple" kan have startet os via deep link paa cold start
        handleDeepLink(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    // Forgrunds-flag til baggrunds-vagten: er app'en aaben, staar skaermens egen
    // poller for lyden, saa OrderPollService springer sin alarm over (ingen dobbelt-lyd).
    override fun onResume() {
        super.onResume()
        WrombleApp.appInForeground = true
    }

    override fun onStop() {
        super.onStop()
        WrombleApp.appInForeground = false
    }

    // Fanger wromble://apple-login?code=... fra Apple web-flowet (Custom Tab) og
    // lader LoginScreen bytte koden til en bruger.
    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "wromble" && data.host == "apple-login") {
            data.getQueryParameter("code")?.takeIf { it.isNotBlank() }?.let {
                AppleAuth.pendingCode = it
            }
        } else if (data.scheme == "wromble" && data.host == "fb-login") {
            data.getQueryParameter("code")?.takeIf { it.isNotBlank() }?.let {
                FacebookAuth.pendingCode = it
            }
        }
    }
}
