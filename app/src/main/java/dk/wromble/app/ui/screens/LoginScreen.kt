package dk.wromble.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import dk.wromble.app.data.Api
import dk.wromble.app.data.Session
import dk.wromble.app.ui.brandGradient
import dk.wromble.app.ui.clickableNoRipple
import dk.wromble.app.ui.theme.WrombleRed
import kotlinx.coroutines.launch

private enum class Role(val label: String, val mode: String) {
    Privat("Privat", "customer"),
    Forretning("Forretning", "company"),
    Chauffor("Chauffør", "rider")
}

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

    fun onLoggedIn() {
        val u = Session.user ?: return
        val dest = when {
            u.type == "company" -> "company"
            u.role.startsWith("chauff") || u.type == "rider" -> "driver"
            else -> "main"
        }
        nav.navigate(dest) { popUpTo("login") { inclusive = true } }
    }

    fun submit() {
        error = ""
        loading = true
        scope.launch {
            try {
                val resp = if (role == Role.Privat && !isLogin) {
                    if (firstname.isBlank()) { error = "Fornavn er paakraevet"; loading = false; return@launch }
                    Api.service.register(
                        mapOf("firstname" to firstname, "lastname" to lastname,
                            "email" to email, "phone" to phone, "password" to password)
                    )
                } else {
                    Api.service.login(mapOf("email" to email, "password" to password, "mode" to role.mode))
                }
                loading = false
                if (resp.error != null) { error = resp.error; return@launch }
                val u = resp.user
                if (u != null) {
                    Session.save(ctx, u)
                    onLoggedIn()
                } else error = "Netvaerksfejl. Proev igen."
            } catch (e: Exception) {
                loading = false
                error = "Netvaerksfejl. Proev igen."
            }
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        ) {
            // Header
            Box(
                Modifier.fillMaxWidth().background(brandGradient).padding(top = 60.dp, bottom = 30.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("wromble", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Black)
                    Text("Online Bestilling · Nemt & Enkelt",
                        color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
                }
            }

            Column(Modifier.padding(20.dp)) {
                // Role selector
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFEDEDF0)).padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Role.entries.forEach { r ->
                        val sel = r == role
                        Box(
                            Modifier.weight(1f).clip(RoundedCornerShape(11.dp))
                                .background(if (sel) Color.White else Color.Transparent)
                                .clickableNoRipple { role = r; error = "" }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(r.label, fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                                color = if (sel) WrombleRed else Color(0xFF6B6B72), fontSize = 14.sp)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                if (role == Role.Privat && !isLogin) {
                    Field(firstname, { firstname = it }, "Fornavn")
                    Spacer(Modifier.height(12.dp))
                    Field(lastname, { lastname = it }, "Efternavn")
                    Spacer(Modifier.height(12.dp))
                    Field(phone, { phone = it }, "Telefon", KeyboardType.Phone)
                    Spacer(Modifier.height(12.dp))
                }

                Field(email, { email = it }, "E-mail", KeyboardType.Email)
                Spacer(Modifier.height(12.dp))
                Field(password, { password = it }, "Adgangskode", KeyboardType.Password, isPassword = true)

                if (error.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(error, color = WrombleRed, fontSize = 14.sp)
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = { submit() },
                    enabled = email.isNotBlank() && password.isNotBlank() && !loading,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WrombleRed)
                ) {
                    if (loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    else Text(if (role == Role.Privat && !isLogin) "Opret konto" else "Log ind",
                        fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }

                if (role == Role.Privat) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (isLogin) "Har du ikke en konto? Opret her" else "Har du allerede en konto? Log ind",
                        color = WrombleRed, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().clickableNoRipple { isLogin = !isLogin; error = "" }
                    )
                    Spacer(Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = {
                            Session.user = dk.wromble.app.data.UserProfile(id = 0, name = "Gæst", type = "guest")
                            nav.navigate("main") { popUpTo("login") { inclusive = true } }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("Fortsæt uden login", color = WrombleRed, fontWeight = FontWeight.SemiBold) }
                } else {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (role == Role.Forretning)
                            "Se og haandter indkomne ordrer for din forretning. Ogsaa for medarbejdere."
                        else "Se dine aktive leverancer og marker dem som leveret.",
                        color = Color(0xFF8A8A90), fontSize = 13.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun Field(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    keyboard: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = WrombleRed,
            focusedLabelColor = WrombleRed
        )
    )
}
