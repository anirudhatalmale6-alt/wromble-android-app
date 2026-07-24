package dk.wromble.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import dk.wromble.app.data.Favorites
import dk.wromble.app.data.Session
import dk.wromble.app.data.Settings
import dk.wromble.app.ui.AppRoot
import dk.wromble.app.ui.BiometricLockScreen
import dk.wromble.app.ui.canUseBiometric
import dk.wromble.app.ui.theme.WrombleTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load persisted state up-front so the lock decision is correct on cold start.
        Settings.load(this)
        Session.load(this)
        Favorites.load(this)

        val lockAtStart = Settings.biometricEnabled && Session.user != null && canUseBiometric(this)

        setContent {
            WrombleTheme {
                var locked by remember { mutableStateOf(lockAtStart) }
                if (locked) BiometricLockScreen(onUnlock = { locked = false })
                else AppRoot()
            }
        }
    }
}
