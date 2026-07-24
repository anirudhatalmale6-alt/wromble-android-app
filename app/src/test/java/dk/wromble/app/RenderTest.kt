package dk.wromble.app

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import androidx.navigation.compose.rememberNavController
import dk.wromble.app.ui.screens.LoginScreen
import dk.wromble.app.ui.screens.OnboardingScreen
import dk.wromble.app.ui.screens.WromblePlusScreen
import dk.wromble.app.ui.theme.WrombleTheme
import org.junit.Rule
import org.junit.Test

class RenderTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    @Test fun login() {
        paparazzi.snapshot { WrombleTheme { LoginScreen(rememberNavController()) } }
    }

    @Test fun onboarding() {
        paparazzi.snapshot { WrombleTheme { OnboardingScreen(rememberNavController()) } }
    }

    @Test fun wromblePlus() {
        paparazzi.snapshot { WrombleTheme { WromblePlusScreen(rememberNavController()) } }
    }
}
