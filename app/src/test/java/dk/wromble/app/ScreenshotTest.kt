package dk.wromble.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import dk.wromble.app.ui.Pill
import dk.wromble.app.ui.theme.WrombleDarkRed
import dk.wromble.app.ui.theme.WrombleRed
import dk.wromble.app.ui.theme.WrombleTheme
import org.junit.Rule
import org.junit.Test

class ScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        renderingMode = SessionParams.RenderingMode.NORMAL,
        showSystemUi = false
    )

    private val grad = Brush.verticalGradient(listOf(WrombleRed, WrombleDarkRed))

    @Test fun splash() = paparazzi.snapshot { WrombleTheme { PreviewSplash() } }
    @Test fun login() = paparazzi.snapshot { WrombleTheme { PreviewLogin() } }
    @Test fun home() = paparazzi.snapshot { WrombleTheme { PreviewHome() } }
    @Test fun restaurant() = paparazzi.snapshot { WrombleTheme { PreviewRestaurant() } }
    @Test fun cart() = paparazzi.snapshot { WrombleTheme { PreviewCart() } }
    @Test fun tracking() = paparazzi.snapshot { WrombleTheme { PreviewTracking() } }
    @Test fun company() = paparazzi.snapshot { WrombleTheme { PreviewCompany() } }

    @Composable
    private fun PreviewSplash() {
        Box(Modifier.fillMaxSize().background(grad), Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("wromble", color = Color.White, fontSize = 58.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(10.dp))
                Text("Online Bestilling · Nemt & Enkelt", color = Color.White.copy(alpha = .9f), fontSize = 16.sp)
            }
        }
    }

    @Composable
    private fun PreviewLogin() {
        Column(Modifier.fillMaxSize().background(Color(0xFFF6F6F8))) {
            Box(Modifier.fillMaxWidth().background(grad).padding(top = 70.dp, bottom = 30.dp), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("wromble", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Black)
                    Text("Online Bestilling · Nemt & Enkelt", color = Color.White.copy(alpha = .9f), fontSize = 14.sp)
                }
            }
            Column(Modifier.padding(20.dp)) {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFEDEDF0)).padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Privat" to true, "Forretning" to false, "Chauffør" to false).forEach { (t, sel) ->
                        Box(Modifier.weight(1f).clip(RoundedCornerShape(11.dp))
                            .background(if (sel) Color.White else Color.Transparent).padding(vertical = 10.dp), Alignment.Center) {
                            Text(t, color = if (sel) WrombleRed else Color(0xFF6B6B72), fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium, fontSize = 14.sp)
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                PreviewField("E-mail", "testbruger@wromble.dk")
                Spacer(Modifier.height(12.dp))
                PreviewField("Adgangskode", "••••••••")
                Spacer(Modifier.height(20.dp))
                Button(onClick = {}, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WrombleRed)) {
                    Text("Log ind", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(16.dp))
                Text("Har du ikke en konto? Opret her", color = WrombleRed, fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }

    @Composable
    private fun PreviewField(label: String, value: String) {
        Column {
            OutlinedTextField(value = value, onValueChange = {}, label = { Text(label) }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WrombleRed))
        }
    }

    @Composable
    private fun FoodImg(res: Int, modifier: Modifier) {
        Image(painterResource(res), null, modifier, contentScale = ContentScale.Crop)
    }

    @Composable
    private fun PreviewHome() {
        LazyColumn(Modifier.fillMaxSize().background(Color(0xFFF6F6F8))) {
            item {
                Box(Modifier.fillMaxWidth().background(grad).padding(start = 20.dp, end = 20.dp, top = 60.dp, bottom = 22.dp)) {
                    Column {
                        Text("Hej Ronnie 👋", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
                        Text("Hvad har du lyst til i dag?", color = Color.White.copy(alpha = .92f), fontSize = 15.sp)
                    }
                }
            }
            item {
                Box(Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(20.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF1A0D0E), WrombleDarkRed))).padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(Color.White), Alignment.Center) {
                            Icon(Icons.Filled.QrCodeScanner, null, tint = WrombleRed, modifier = Modifier.size(30.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("Scan bordet", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Bestil direkte fra dit bord – spring koeen over", color = Color.White.copy(alpha = .85f), fontSize = 13.sp)
                        }
                    }
                }
            }
            item {
                Text("Kategorier", Modifier.padding(start = 20.dp, top = 4.dp, bottom = 8.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val cats = listOf(R.drawable.food_steak to "Varme retter", R.drawable.food_cake to "Kager", R.drawable.food_choc to "Slik & dessert")
                    items(cats.size) { i ->
                        Column(Modifier.width(96.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            FoodImg(cats[i].first, Modifier.size(96.dp).clip(RoundedCornerShape(20.dp)))
                            Text(cats[i].second, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
            }
            item { Text("Spisesteder i naerheden", Modifier.padding(start = 20.dp, top = 18.dp, bottom = 8.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            val rests = listOf(
                Triple(R.drawable.food_hero, "Cafe Alma", "Jaegersborggade 8, 2200 Koebenhavn N"),
                Triple(R.drawable.food_choc, "Citronen", "Frederiksborgvej 98, 2400 Kbh NV")
            )
            items(rests.size) { i -> PreviewRestCard(rests[i].first, rests[i].second, rests[i].third) }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }

    @Composable
    private fun PreviewRestCard(res: Int, name: String, address: String) {
        Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(3.dp)) {
            Column {
                Box {
                    FoodImg(res, Modifier.fillMaxWidth().height(160.dp))
                    Box(Modifier.padding(12.dp)) { Pill("Restaurant", WrombleRed) }
                    Box(Modifier.align(Alignment.TopEnd).padding(12.dp).size(38.dp).clip(RoundedCornerShape(19.dp))
                        .background(Color.White.copy(alpha = .92f)), Alignment.Center) {
                        Icon(Icons.Filled.FavoriteBorder, null, tint = WrombleRed, modifier = Modifier.size(22.dp))
                    }
                }
                Column(Modifier.padding(14.dp)) {
                    Text(name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(address, fontSize = 13.sp, color = Color(0xFF8A8A90), maxLines = 1)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill("📍 I naerheden", Color(0xFFF0F0F3), Color(0xFF444444))
                        Pill("Gratis levering", Color(0xFFE7F7EC), Color(0xFF16A34A))
                    }
                }
            }
        }
    }

    @Composable
    private fun PreviewRestaurant() {
        LazyColumn(Modifier.fillMaxSize().background(Color(0xFFF6F6F8))) {
            item { FoodImg(R.drawable.food_hero, Modifier.fillMaxWidth().height(220.dp)) }
            item {
                Column(Modifier.padding(20.dp)) {
                    Text("Cafe Alma", fontSize = 26.sp, fontWeight = FontWeight.Black)
                    Text("Jaegersborggade 8, 2200 Koebenhavn N", fontSize = 14.sp, color = Color(0xFF8A8A90))
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill("🕐 25-40 min", Color(0xFFF0F0F3), Color(0xFF444444))
                        Pill("Gratis levering", Color(0xFFE7F7EC), Color(0xFF16A34A))
                    }
                }
            }
            item { Text("Forretter", Modifier.padding(start = 20.dp, top = 8.dp, bottom = 4.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            val items = listOf(
                Triple(R.drawable.food_steak, "Hoensessuppe" to "Hjemmelavet med boller og groentsager", 69.0),
                Triple(R.drawable.food_choc, "Chokoladekage" to "Saftig kage med maerkbar chokolade", 49.0),
                Triple(R.drawable.food_cake, "Cheesecake" to "Klassisk med baer-coulis", 59.0)
            )
            items(items.size) { i -> PreviewMenuRow(items[i].first, items[i].second.first, items[i].second.second, items[i].third) }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }

    @Composable
    private fun PreviewMenuRow(res: Int, name: String, desc: String, price: Double) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(desc, fontSize = 13.sp, color = Color(0xFF8A8A90), maxLines = 2)
                Text("%.2f kr".format(price), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = WrombleRed, modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(Modifier.width(12.dp))
            FoodImg(res, Modifier.size(72.dp).clip(RoundedCornerShape(14.dp)))
            Spacer(Modifier.width(10.dp))
            FilledIconButton(onClick = {}, colors = IconButtonDefaults.filledIconButtonColors(containerColor = WrombleRed)) {
                Icon(Icons.Filled.Add, null, tint = Color.White)
            }
        }
    }

    @Composable
    private fun PreviewCart() {
        Column(Modifier.fillMaxSize().background(Color(0xFFF6F6F8)).padding(top = 40.dp)) {
            Text("Din kurv", Modifier.padding(20.dp), fontSize = 24.sp, fontWeight = FontWeight.Black)
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text("Cafe Alma", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                listOf("Hoensessuppe" to 69.0, "Cheesecake" to 59.0).forEach { (n, p) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(n, fontWeight = FontWeight.SemiBold)
                            Text("%.2f kr".format(p), color = Color(0xFF8A8A90), fontSize = 13.sp)
                        }
                        Text("1", Modifier.padding(horizontal = 12.dp), fontWeight = FontWeight.Bold)
                    }
                    Divider(color = Color(0xFFEDEDF0))
                }
                Spacer(Modifier.height(16.dp))
                Text("Levering", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.clip(RoundedCornerShape(12.dp)).background(WrombleRed).padding(horizontal = 20.dp, vertical = 10.dp)) {
                        Text("Levering", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                    Box(Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFFEDEDF0)).padding(horizontal = 20.dp, vertical = 10.dp)) {
                        Text("Afhentning", color = Color(0xFF444444), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Surface(shadowElevation = 12.dp, color = Color.White) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Total", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("128,00 kr", fontSize = 18.sp, fontWeight = FontWeight.Black, color = WrombleRed)
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = {}, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WrombleRed)) {
                        Text("Afgiv bestilling", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    @Composable
    private fun PreviewTracking() {
        Column(Modifier.fillMaxSize().background(Color(0xFFF6F6F8)).padding(top = 50.dp).padding(24.dp)) {
            Text("Cafe Alma", fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text("Total: 128,00 kr", color = Color(0xFF8A8A90))
            Spacer(Modifier.height(24.dp))
            val steps = listOf("Modtaget", "Bekraeftet", "Paa vej", "Leveret")
            val stage = 2
            steps.forEachIndexed { i, label ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(if (i <= stage) WrombleRed else Color(0xFFE3E3E8)), Alignment.Center) {
                        if (i < stage) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        else Text("${i + 1}", color = if (i <= stage) Color.White else Color(0xFF9A9AA2), fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(label, fontSize = 16.sp, fontWeight = if (i == stage) FontWeight.Bold else FontWeight.Normal,
                        color = if (i <= stage) Color(0xFF1A1A1E) else Color(0xFF9A9AA2))
                }
                if (i < steps.size - 1) Box(Modifier.padding(start = 19.dp).width(2.dp).height(18.dp).background(if (i < stage) WrombleRed else Color(0xFFE3E3E8)))
            }
            Spacer(Modifier.height(20.dp))
            Text("Chaufføren er på vej med din ordre 🚴", color = Color(0xFF6B6B72), fontSize = 15.sp)
        }
    }

    @Composable
    private fun PreviewCompany() {
        Column(Modifier.fillMaxSize().background(Color(0xFFF6F6F8))) {
            Box(Modifier.fillMaxWidth().background(Color.White).padding(top = 44.dp, bottom = 12.dp, start = 16.dp)) {
                Text("Forretning · Citronen", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            TabRow(selectedTabIndex = 0, containerColor = Color.White, contentColor = WrombleRed) {
                Tab(selected = true, onClick = {}, text = { Text("Aktive") })
                Tab(selected = false, onClick = {}, text = { Text("Historik") })
            }
            Card(Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("Ordre #1042", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Pill("NY", WrombleRed)
                    }
                    Text("Mette Hansen", fontSize = 15.sp)
                    Text("27 29 74 80", fontSize = 13.sp, color = Color(0xFF8A8A90))
                    Text("Levering: Noerrebrogade 42, 2200 Kbh N", fontSize = 13.sp, color = Color(0xFF8A8A90))
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("2x Wienerschnitzel", fontSize = 14.sp, color = Color(0xFF6B6B72)); Text("258,00 kr", fontSize = 14.sp, color = Color(0xFF6B6B72)) }
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), Arrangement.End) { Text("Total: 258,00 kr", fontWeight = FontWeight.Bold, color = WrombleRed) }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = {}, modifier = Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WrombleRed)) { Text("Accepter", fontWeight = FontWeight.Bold) }
                        OutlinedButton(onClick = {}, modifier = Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, WrombleRed)) { Text("Afvis", color = WrombleRed, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}
