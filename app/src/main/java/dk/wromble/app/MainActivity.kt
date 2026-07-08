package dk.wromble.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dk.wromble.app.ui.AppRoot
import dk.wromble.app.ui.theme.WrombleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WrombleTheme {
                AppRoot()
            }
        }
    }
}
