package dk.wromble.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import dk.wromble.app.data.OrderPollService
import dk.wromble.app.data.Session
import dk.wromble.app.data.Settings
import dk.wromble.app.ui.brandGradient
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(nav: NavController) {
    val ctx = LocalContext.current
    var visible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.7f,
        animationSpec = tween(700, easing = EaseOutBack),
        label = "scale"
    )

    LaunchedEffect(Unit) {
        visible = true
        delay(1400)
        // Route by saved session
        val u = Session.user
        val dest = when {
            !Settings.onboardingDone -> "onboarding"
            u == null -> "login"
            u.type == "company" -> "company"
            u.role == "chauffør" || u.role == "chauffoer" || u.type == "rider" -> "driver"
            else -> "main"
        }
        // Allerede logget ind som forretning/chauffoer paa cold start: start baggrunds-vagten.
        if (dest == "company" || dest == "driver") OrderPollService.start(ctx)
        nav.navigate(dest) { popUpTo("splash") { inclusive = true } }
    }

    Box(
        Modifier.fillMaxSize().background(brandGradient),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "wromble",
                color = Color.White,
                fontSize = 58.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.scale(scale)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Online Bestilling · Nemt & Enkelt",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}
