package dk.wromble.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import dk.wromble.app.data.BASE_URL
import dk.wromble.app.data.Session
import dk.wromble.app.data.Settings
import dk.wromble.app.ui.brandGradient
import dk.wromble.app.ui.canUseBiometric
import dk.wromble.app.ui.clickableNoRipple
import dk.wromble.app.ui.theme.WrombleRed

@Composable
fun ProfileScreen(nav: NavController) {
    val ctx = LocalContext.current
    val user = Session.user
    val isGuest = user == null || user.id == 0

    fun openUrl(url: String) = runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    fun shareApp() = runCatching {
        ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Prøv Wromble – nem online bestilling: $BASE_URL")
        }, "Del Wromble"))
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .background(MaterialTheme.colorScheme.background)) {
        Box(
            Modifier.fillMaxWidth().background(brandGradient)
                .padding(top = 56.dp, bottom = 26.dp), contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(84.dp).clip(CircleShape).background(Color.White),
                    contentAlignment = Alignment.Center) {
                    Text(user?.name?.take(1)?.uppercase() ?: "?", color = WrombleRed,
                        fontSize = 36.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(12.dp))
                Text(if (isGuest) "Gæst" else (user?.name ?: ""), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(user?.email ?: "", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
            }
        }

        if (isGuest) {
            Button(onClick = { nav.navigate("login") { popUpTo(0) } },
                modifier = Modifier.fillMaxWidth().padding(16.dp).height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WrombleRed)) {
                Text("Log ind eller opret konto", fontWeight = FontWeight.Bold)
            }
        }

        var showSound by remember { mutableStateOf(false) }

        if (!isGuest) {
            Section("Min konto")
            SettingsGroup {
                NavRow(Icons.Filled.Person, "Rediger profil") { nav.navigate("profile/edit") }
            }
        }

        Section("Indstillinger")
        SettingsGroup {
            ToggleRow(Icons.Filled.Notifications, "Notifikationer", Settings.notificationsEnabled) {
                Settings.setNotifications(ctx, it)
            }
            RowDivider()
            NavRow(Icons.Filled.MusicNote, "Lyd ved ordre-opdatering") { showSound = true }
            RowDivider()
            ToggleRow(Icons.Filled.LocationOn, "Placering", Settings.locationEnabled) {
                Settings.setLocation(ctx, it)
            }
            if (canUseBiometric(ctx)) {
                RowDivider()
                ToggleRow(Icons.Filled.Fingerprint, "Lås app med biometri", Settings.biometricEnabled) {
                    Settings.setBiometric(ctx, it)
                }
            }
        }
        if (showSound) AlarmSettingsDialog(showDuration = false) { showSound = false }

        Section("Wromble")
        SettingsGroup {
            NavRow(Icons.Filled.MailOutline, "Kontakt os") { nav.navigate("contact") }
            RowDivider()
            NavRow(Icons.Filled.Handshake, "Bliv partner") { nav.navigate("partner") }
            RowDivider()
            NavRow(Icons.Filled.Work, "Job hos Wromble") { nav.navigate("jobs") }
        }

        Section("Del & support")
        SettingsGroup {
            NavRow(Icons.Filled.Share, "Del appen") { shareApp() }
            RowDivider()
            NavRow(Icons.Filled.PrivacyTip, "Privatlivspolitik") { openUrl("$BASE_URL/privacy-policy/app.php") }
            RowDivider()
            NavRow(Icons.Filled.Description, "Vilkår") { openUrl("$BASE_URL/terms/app.php") }
        }

        Spacer(Modifier.height(20.dp))
        Text("Wromble ${dk.wromble.app.BuildConfig.VERSION_NAME} (${dk.wromble.app.BuildConfig.VERSION_CODE})",
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            color = Color(0xFFB0B0B6), fontSize = 12.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)

        Spacer(Modifier.height(8.dp))
        if (!isGuest) {
            OutlinedButton(
                onClick = { Session.clear(ctx); nav.navigate("login") { popUpTo(0) } },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(50.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, WrombleRed)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, null, tint = WrombleRed)
                Spacer(Modifier.width(8.dp))
                Text("Log ud", color = WrombleRed, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = { nav.navigate("delete-account") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                Text("Slet konto", color = Color(0xFF8A8A90))
            }
        }
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun Section(title: String) {
    Text(title, Modifier.padding(start = 24.dp, top = 22.dp, bottom = 8.dp),
        fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8A8A90))
}

// Grupperer rækker i ét afrundet kort (iOS-agtig "grouped list") så siden
// virker mere rolig og overskuelig – rent visuelt, ingen ændret funktion.
@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) { Column(content = content) }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 56.dp),
        thickness = 0.7.dp, color = Color(0x14000000)
    )
}

@Composable
private fun NavRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickableNoRipple(onClick).padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = WrombleRed)
        Spacer(Modifier.width(16.dp))
        Text(label, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Icon(Icons.Filled.ChevronRight, null, tint = Color(0xFFBFBFC6))
    }
}

@Composable
private fun ToggleRow(icon: ImageVector, label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = WrombleRed)
        Spacer(Modifier.width(16.dp))
        Text(label, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = WrombleRed))
    }
}
