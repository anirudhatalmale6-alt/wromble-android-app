package dk.wromble.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// Local app settings (mirrors iOS AppState UserDefaults flags)
object Settings {
    private const val PREFS = "wromble_settings"

    var notificationsEnabled by mutableStateOf(true)
    var locationEnabled by mutableStateOf(true)
    var biometricEnabled by mutableStateOf(false)
    var onboardingDone by mutableStateOf(false)

    fun load(ctx: Context) {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        notificationsEnabled = p.getBoolean("notifications", true)
        locationEnabled = p.getBoolean("location", true)
        biometricEnabled = p.getBoolean("biometric", false)
        onboardingDone = p.getBoolean("onboarding_done", false)
    }

    private fun edit(ctx: Context, block: android.content.SharedPreferences.Editor.() -> Unit) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply(block).apply()
    }

    fun setNotifications(ctx: Context, v: Boolean) { notificationsEnabled = v; edit(ctx) { putBoolean("notifications", v) } }
    fun setLocation(ctx: Context, v: Boolean) { locationEnabled = v; edit(ctx) { putBoolean("location", v) } }
    fun setBiometric(ctx: Context, v: Boolean) { biometricEnabled = v; edit(ctx) { putBoolean("biometric", v) } }
    fun setOnboardingDone(ctx: Context, v: Boolean) { onboardingDone = v; edit(ctx) { putBoolean("onboarding_done", v) } }
}
