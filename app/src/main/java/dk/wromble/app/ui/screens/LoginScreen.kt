package dk.wromble.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import dk.wromble.app.R
import dk.wromble.app.data.Api
import dk.wromble.app.data.AppleAuth
import dk.wromble.app.data.FacebookAuth
import dk.wromble.app.data.Session
import dk.wromble.app.data.UserProfile
import dk.wromble.app.ui.clickableNoRipple
import dk.wromble.app.ui.theme.WrombleRed
import kotlinx.coroutines.launch

// Wromble-login i lyst design: mad-collage i toppen + hvidt "ark" med login,
// saa det matcher resten af appens hvide baggrund. Privatkunder faar Apple /
// Facebook / Email + "Velkommen tilbage" naar de har logget ud. Forretning/
// chauffoer har deres eget portal-login (link nederst).
private enum class Role(val label: String, val mode: String) {
    Privat("Privat", "customer"),
    Forretning("Forretning", "company"),
    Chauffor("Chauffør", "rider")
}

// Lyst tema (hvid baggrund som resten af appen)
private val ScreenBg = Color.White
private val SheetBg = Color.White
private val FieldBg = Color(0xFFF4F5F7)
private val FieldBorder = Color(0xFFE2E4EA)
private val TextDark = Color(0xFF15161C)
private val TextMuted = Color(0xFF6C6D78)

@Composable
fun LoginScreen(nav: NavController) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var role by remember { mutableStateOf(Role.Privat) }
    var isLogin by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var firstname by remember { mutableStateOf("") }
    var lastname by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    // Vis kunde-login (false) eller forretning/chauffoer-portal (true)
    var staffMode by remember { mutableStateOf(false) }
    // Fold email/adgangskode-formen ud (ellers vises kun de runde login-knapper)
    var showEmailForm by remember { mutableStateOf(false) }

    // "Velkommen tilbage": husket sidste kunde-login (navn, email, metode)
    val lastLogin = remember { Session.lastLogin(ctx) }
    var welcomeDismissed by remember { mutableStateOf(false) }
    val showWelcome = lastLogin != null && !welcomeDismissed && !staffMode

    fun onLoggedIn() {
        val u = Session.user ?: return
        val dest = when {
            u.type == "company" -> "company"
            u.role.startsWith("chauff") || u.type == "rider" -> "driver"
            else -> "main"
        }
        nav.navigate(dest) { popUpTo("login") { inclusive = true } }
    }

    // Gem bruger + (for kunder) husk login-metoden til "Velkommen tilbage", og gaa videre.
    fun finish(u: UserProfile, method: String) {
        Session.save(ctx, u)
        if (u.type == "customer" && u.email.isNotBlank()) {
            Session.saveLastLogin(ctx, u.name, u.email, method)
        }
        onLoggedIn()
    }

    // "Log ind med Apple": aabner Apple's web-login i en Custom Tab.
    fun startAppleLogin() {
        error = ""
        val url = android.net.Uri.parse("https://wromble.dk/app-apple-start.php")
        try {
            androidx.browser.customtabs.CustomTabsIntent.Builder().build().launchUrl(ctx, url)
        } catch (_: Exception) {
            try { ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, url)) }
            catch (_: Exception) { error = "Kunne ikke aabne Apple-login" }
        }
    }

    // "Log ind med Facebook": aabner Facebook's web-login i en Custom Tab.
    fun startFacebookLogin() {
        error = ""
        val url = android.net.Uri.parse("https://wromble.dk/app-facebook-start.php")
        try {
            androidx.browser.customtabs.CustomTabsIntent.Builder().build().launchUrl(ctx, url)
        } catch (_: Exception) {
            try { ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, url)) }
            catch (_: Exception) { error = "Kunne ikke aabne Facebook-login" }
        }
    }

    LaunchedEffect(AppleAuth.pendingCode) {
        val code = AppleAuth.pendingCode ?: return@LaunchedEffect
        AppleAuth.pendingCode = null
        error = ""; loading = true
        try {
            val resp = Api.service.appleExchange(mapOf("code" to code))
            loading = false
            if (resp.error != null) { error = resp.error; return@LaunchedEffect }
            resp.user?.let { finish(it, "apple") } ?: run { error = "Apple-login mislykkedes. Prøv igen." }
        } catch (e: Throwable) { loading = false; error = "Apple-login mislykkedes. Prøv igen." }
    }

    LaunchedEffect(FacebookAuth.pendingCode) {
        val code = FacebookAuth.pendingCode ?: return@LaunchedEffect
        FacebookAuth.pendingCode = null
        error = ""; loading = true
        try {
            val resp = Api.service.facebookExchange(mapOf("code" to code))
            loading = false
            if (resp.error != null) { error = resp.error; return@LaunchedEffect }
            resp.user?.let { finish(it, "facebook") } ?: run { error = "Facebook-login mislykkedes. Prøv igen." }
        } catch (e: Throwable) { loading = false; error = "Facebook-login mislykkedes. Prøv igen." }
    }

    fun submit() {
        error = ""; loading = true
        scope.launch {
            try {
                val resp = if (role == Role.Privat && !isLogin) {
                    if (firstname.isBlank()) { error = "Fornavn er påkrævet"; loading = false; return@launch }
                    Api.service.register(mapOf("firstname" to firstname, "lastname" to lastname,
                        "email" to email, "phone" to phone, "password" to password))
                } else {
                    Api.service.login(mapOf("email" to email, "password" to password, "mode" to role.mode))
                }
                loading = false
                if (resp.error != null) { error = resp.error; return@launch }
                resp.user?.let { finish(it, "email") } ?: run { error = "Netværksfejl. Prøv igen." }
            } catch (e: Exception) { loading = false; error = "Netværksfejl. Prøv igen." }
        }
    }

    // "Fortsæt som..." paa velkommen-tilbage: gentag den metode brugeren sidst brugte.
    fun continueLast() {
        when (lastLogin?.third) {
            "apple" -> startAppleLogin()
            "facebook" -> startFacebookLogin()
            else -> { welcomeDismissed = true; email = lastLogin?.second ?: ""; showEmailForm = true }
        }
    }

    // Fylder hele skaermen: mad-collage i toppen (fuld bredde, kant-til-kant) og
    // et hvidt ark der fylder resten hele vejen ned til bunden.
    Column(Modifier.fillMaxSize().background(ScreenBg)) {

        // ---------- Mad-collage (hero) der ruller langsomt ----------
        Box(Modifier.fillMaxWidth().height(288.dp).clipToBounds()) {
            Row(
                Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ScrollingFoodColumn(
                    listOf(R.drawable.login_pizza, R.drawable.login_dessert, R.drawable.login_burger),
                    up = true, durationMs = 26000, modifier = Modifier.weight(1f))
                ScrollingFoodColumn(
                    listOf(R.drawable.login_coffee, R.drawable.login_icecream, R.drawable.login_pizza),
                    up = false, durationMs = 33000, modifier = Modifier.weight(1f))
                ScrollingFoodColumn(
                    listOf(R.drawable.login_burger, R.drawable.login_coffee, R.drawable.login_dessert),
                    up = true, durationMs = 29000, modifier = Modifier.weight(1f))
            }
            // Blød overgang ned mod det hvide ark
            Box(Modifier.fillMaxWidth().height(96.dp).align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, ScreenBg))))
        }

        // ---------- Hvidt ark (fylder resten af skaermen) ----------
        Surface(
            color = SheetBg,
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
            modifier = Modifier.fillMaxWidth().weight(1f).offset(y = (-26).dp)
        ) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(start = 26.dp, end = 26.dp, top = 26.dp, bottom = 40.dp)
            ) {

                Text("wromble", color = TextDark, fontSize = 26.sp, fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())

                Spacer(Modifier.height(14.dp))

                    if (staffMode) {
                        StaffLogin(
                            role = role, onRole = { role = it; error = "" },
                            email = email, onEmail = { email = it },
                            password = password, onPassword = { password = it },
                            error = error, loading = loading,
                            onSubmit = { submit() }, onBack = { staffMode = false; error = ""; role = Role.Privat }
                        )
                    } else if (showWelcome && lastLogin != null) {
                        // ---------- Velkommen tilbage ----------
                        val first = lastLogin.first.trim().ifBlank { "igen" }.split(" ").first()
                        val methodLabel = when (lastLogin.third) {
                            "apple" -> "Apple"; "facebook" -> "Facebook"; else -> "email"
                        }
                        Text("Velkommen\ntilbage, $first", color = TextDark, fontSize = 27.sp,
                            fontWeight = FontWeight.ExtraBold, lineHeight = 32.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Du loggede sidst ind med $methodLabel", color = TextMuted, fontSize = 14.5.sp)
                        Spacer(Modifier.height(22.dp))
                        // Fortsæt som ...
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(WrombleRed)
                                .clickableNoRipple { continueLast() }.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(44.dp).clip(CircleShape).background(Color.White),
                                contentAlignment = Alignment.Center) {
                                Text(initials(lastLogin.first), color = WrombleRed, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text("Fortsæt som ${lastLogin.first.ifBlank { "dig" }}",
                                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(lastLogin.second, color = Color.White.copy(alpha = 0.85f), fontSize = 12.5.sp)
                            }
                        }
                        if (error.isNotEmpty()) { Spacer(Modifier.height(12.dp)); Text(error, color = WrombleRed, fontSize = 14.sp) }
                        DividerOr()
                        SecondaryButton("Log ind med email") { welcomeDismissed = true; showEmailForm = true }
                        Spacer(Modifier.height(16.dp))
                        Text("Brug en anden login-metode", color = WrombleRed, fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().clickableNoRipple { welcomeDismissed = true })
                    } else {
                        // ---------- Fuldt login / opret ----------
                        Text(if (isLogin) "Log ind eller\nopret dig" else "Opret din\nkonto",
                            color = TextDark, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 32.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Bestil mad fra dine lokale favoritter", color = TextMuted, fontSize = 14.5.sp)

                        Spacer(Modifier.height(24.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            SocialCircle("Apple", R.drawable.ic_apple, Color.Black) { startAppleLogin() }
                            SocialCircle("Facebook", R.drawable.ic_facebook_f, Color(0xFF1877F2)) { startFacebookLogin() }
                            SocialCircle("Email", R.drawable.ic_email, WrombleRed) { showEmailForm = !showEmailForm }
                        }

                        if (showEmailForm) {
                            DividerOr()
                            if (!isLogin) {
                                DarkField(firstname, { firstname = it }, "Fornavn")
                                Spacer(Modifier.height(12.dp))
                                DarkField(lastname, { lastname = it }, "Efternavn")
                                Spacer(Modifier.height(12.dp))
                                DarkField(phone, { phone = it }, "Telefon", KeyboardType.Phone)
                                Spacer(Modifier.height(12.dp))
                            }
                            DarkField(email, { email = it }, "E-mail", KeyboardType.Email)
                            Spacer(Modifier.height(12.dp))
                            DarkField(password, { password = it }, "Adgangskode", KeyboardType.Password, isPassword = true)
                            if (error.isNotEmpty()) { Spacer(Modifier.height(12.dp)); Text(error, color = WrombleRed, fontSize = 14.sp) }
                            Spacer(Modifier.height(18.dp))
                            PrimaryButton(if (isLogin) "Fortsæt" else "Opret konto",
                                enabled = email.isNotBlank() && password.isNotBlank() && !loading, loading = loading) { submit() }
                            Spacer(Modifier.height(16.dp))
                            Text(if (isLogin) "Har du ikke en konto? Opret her" else "Har du allerede en konto? Log ind",
                                color = WrombleRed, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().clickableNoRipple { isLogin = !isLogin; error = "" })
                        } else if (error.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp)); Text(error, color = WrombleRed, fontSize = 14.sp)
                        }

                        Spacer(Modifier.height(20.dp))
                        Text("Fortsæt uden login", color = TextMuted, fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().clickableNoRipple {
                                Session.user = UserProfile(id = 0, name = "Gæst", type = "guest")
                                nav.navigate("main") { popUpTo("login") { inclusive = true } }
                            })
                        Spacer(Modifier.height(18.dp))
                        Text("Log ind som forretning eller chauffør", color = TextMuted.copy(alpha = 0.8f),
                            fontSize = 13.sp, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().clickableNoRipple {
                                staffMode = true; role = Role.Forretning; error = ""; showEmailForm = false
                            })
                    }
            }
        }
    }
}

// ---- Byggeklodser ----

@Composable
private fun FoodTile(resId: Int, modifier: Modifier) {
    Image(
        painter = painterResource(resId), contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(RoundedCornerShape(20.dp))
    )
}

// En kolonne af mad-billeder der ruller uendeligt og langsomt (op eller ned).
// Der tegnes to saet under hinanden, saa loopet er soemloest.
@Composable
private fun ScrollingFoodColumn(images: List<Int>, up: Boolean, durationMs: Int, modifier: Modifier) {
    val infinite = rememberInfiniteTransition(label = "foodcol")
    val t by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMs, easing = LinearEasing), RepeatMode.Restart),
        label = "scroll"
    )
    val tile = 150.dp
    val gap = 10.dp
    val setPx = with(LocalDensity.current) { ((tile + gap) * images.size).toPx() }
    Column(
        modifier.graphicsLayer { translationY = if (up) -t * setPx else (t - 1f) * setPx }
    ) {
        repeat(2) {
            images.forEach { res ->
                FoodTile(res, Modifier.fillMaxWidth().height(tile))
                Spacer(Modifier.height(gap))
            }
        }
    }
}

@Composable
private fun SocialCircle(label: String, iconRes: Int, bg: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(64.dp).clip(CircleShape).background(bg).clickableNoRipple { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(painterResource(iconRes), contentDescription = label, tint = Color.White,
                modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(label, color = Color(0xFF3A3B44), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DividerOr() {
    Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Divider(Modifier.weight(1f), color = FieldBorder)
        Text("  eller  ", color = TextMuted, fontSize = 13.sp)
        Divider(Modifier.weight(1f), color = FieldBorder)
    }
}

@Composable
private fun PrimaryButton(text: String, enabled: Boolean, loading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(15.dp),
        colors = ButtonDefaults.buttonColors(containerColor = WrombleRed, disabledContainerColor = WrombleRed.copy(alpha = 0.4f))
    ) {
        if (loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
        else Text(text, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun SecondaryButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp))
            .background(FieldBg).clickableNoRipple { onClick() }.padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) { Text(text, color = TextDark, fontWeight = FontWeight.Bold, fontSize = 15.5.sp) }
}

@Composable
private fun DarkField(
    value: String, onChange: (String) -> Unit, label: String,
    keyboard: KeyboardType = KeyboardType.Text, isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = FieldBg, unfocusedContainerColor = FieldBg,
            focusedBorderColor = WrombleRed, unfocusedBorderColor = FieldBorder,
            focusedTextColor = TextDark, unfocusedTextColor = TextDark,
            focusedLabelColor = WrombleRed, unfocusedLabelColor = TextMuted,
            cursorColor = WrombleRed
        )
    )
}

@Composable
private fun StaffLogin(
    role: Role, onRole: (Role) -> Unit,
    email: String, onEmail: (String) -> Unit,
    password: String, onPassword: (String) -> Unit,
    error: String, loading: Boolean,
    onSubmit: () -> Unit, onBack: () -> Unit
) {
    Text(if (role == Role.Forretning) "Forretnings-login" else "Chauffør-login",
        color = TextDark, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
    Spacer(Modifier.height(4.dp))
    Text(if (role == Role.Forretning) "Se og håndter indkomne ordrer" else "Se dine aktive leverancer",
        color = TextMuted, fontSize = 14.sp)
    Spacer(Modifier.height(18.dp))
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(FieldBg).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(Role.Forretning, Role.Chauffor).forEach { r ->
            val sel = r == role
            Box(Modifier.weight(1f).clip(RoundedCornerShape(9.dp))
                .background(if (sel) WrombleRed else Color.Transparent)
                .clickableNoRipple { onRole(r) }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                Text(r.label, color = if (sel) Color.White else TextMuted,
                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium, fontSize = 14.sp)
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    DarkField(email, onEmail, "E-mail", KeyboardType.Email)
    Spacer(Modifier.height(12.dp))
    DarkField(password, onPassword, "Adgangskode", KeyboardType.Password, isPassword = true)
    if (error.isNotEmpty()) { Spacer(Modifier.height(12.dp)); Text(error, color = WrombleRed, fontSize = 14.sp) }
    Spacer(Modifier.height(18.dp))
    PrimaryButton("Log ind", enabled = email.isNotBlank() && password.isNotBlank() && !loading, loading = loading) { onSubmit() }
    Spacer(Modifier.height(16.dp))
    Text("Tilbage til kunde-login", color = TextMuted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().clickableNoRipple { onBack() })
}

private fun initials(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(1).uppercase()
        else -> (parts[0].take(1) + parts.last().take(1)).uppercase()
    }
}
