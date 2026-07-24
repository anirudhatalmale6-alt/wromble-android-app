package dk.wromble.app.ui

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dk.wromble.app.ui.theme.WrombleRed

fun canUseBiometric(ctx: Context): Boolean {
    val m = BiometricManager.from(ctx)
    return m.canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    ) == BiometricManager.BIOMETRIC_SUCCESS
}

fun promptBiometric(activity: FragmentActivity, onSuccess: () -> Unit, onError: () -> Unit = {}) {
    val executor = ContextCompat.getMainExecutor(activity)
    val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onError()
    })
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Lås Wromble op")
        .setSubtitle("Bekræft din identitet")
        .setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        .build()
    prompt.authenticate(info)
}

@Composable
fun BiometricLockScreen(onUnlock: () -> Unit) {
    val ctx = LocalContext.current
    val activity = ctx as? FragmentActivity

    fun tryUnlock() {
        val a = activity ?: run { onUnlock(); return }
        promptBiometric(a, onSuccess = onUnlock)
    }
    LaunchedEffect(Unit) { tryUnlock() }

    Box(Modifier.fillMaxSize().background(brandGradient), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Fingerprint, null, tint = Color.White, modifier = Modifier.size(72.dp))
            Spacer(Modifier.height(16.dp))
            Text("Wromble er låst", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            Button(onClick = { tryUnlock() }, shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)) {
                Text("Lås op", color = WrombleRed, fontWeight = FontWeight.Bold)
            }
        }
    }
}
