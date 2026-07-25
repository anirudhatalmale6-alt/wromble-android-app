package dk.wromble.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.BarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import dk.wromble.app.data.Restaurant
import dk.wromble.app.ui.MainViewModel
import dk.wromble.app.ui.theme.WrombleRed

// Parse a scanned Wromble code -> (restaurant, table number or null)
private fun matchScan(raw: String, restaurants: List<Restaurant>): Pair<Restaurant, Int?>? {
    val text = raw.trim()
    val uri = runCatching { Uri.parse(text) }.getOrNull()

    // table number from common query keys
    var table: Int? = null
    var companyId: Int? = null
    var alias: String? = null
    if (uri != null && uri.isHierarchical) {
        for (key in listOf("bord", "table", "t")) {
            uri.getQueryParameter(key)?.toIntOrNull()?.let { table = it }
        }
        for (key in listOf("company_id", "company", "c", "id")) {
            uri.getQueryParameter(key)?.toIntOrNull()?.let { companyId = it }
        }
        // first non-empty path segment often is the restaurant alias
        alias = uri.pathSegments?.firstOrNull { it.isNotBlank() }
    }
    // bare table number in the string, e.g. "BORD-12"
    if (table == null) {
        Regex("(?i)bord[^0-9]*(\\d+)").find(text)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { table = it }
    }

    val match = when {
        companyId != null -> restaurants.firstOrNull { it.id == companyId }
        !alias.isNullOrBlank() -> restaurants.firstOrNull { it.alias.equals(alias, ignoreCase = true) }
        else -> null
    }
    return match?.let { it to table }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScannerScreen(nav: androidx.navigation.NavController, vm: MainViewModel, mode: String) {
    val ctx = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var error by remember { mutableStateOf<String?>(null) }
    val handled = remember { mutableStateOf(false) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
    }
    LaunchedEffect(Unit) { if (!hasPermission) permLauncher.launch(Manifest.permission.CAMERA) }

    LaunchedEffect(vm.restaurants.size) { if (vm.restaurants.isEmpty()) vm.loadHome() }

    fun onScanned(raw: String) {
        if (handled.value) return
        val result = matchScan(raw, vm.restaurants)
        if (result == null) {
            error = "Ukendt QR-kode. Prøv igen."
            return
        }
        handled.value = true
        val (r, table) = result
        val route = if (table != null) "restaurant/${r.id}?table=$table" else "restaurant/${r.id}"
        nav.navigate(route) { popUpTo("main") }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (hasPermission) {
            // ZXing-baseret scanner (ren Java, ingen native kode) – erstatter tidligere
            // ML Kit + CameraX, saa app-bundlen ikke laengere indeholder native biblioteker
            // (fjerner Play-advarslen om manglende fejlretningssymboler).
            val barcodeView = remember {
                BarcodeView(ctx).apply {
                    decoderFactory = DefaultDecoderFactory(listOf(BarcodeFormat.QR_CODE))
                }
            }
            DisposableEffect(Unit) {
                barcodeView.decodeContinuous(object : BarcodeCallback {
                    override fun barcodeResult(result: BarcodeResult) {
                        result.text?.let { onScanned(it) }
                    }
                    override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>) {}
                })
                barcodeView.resume()
                onDispose { barcodeView.pause() }
            }
            AndroidView(modifier = Modifier.fillMaxSize(), factory = { barcodeView })
            // dimmed scan window
            Box(
                Modifier.align(Alignment.Center).size(240.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.06f))
            )
        } else {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Kameraadgang kræves for at scanne", color = Color.White, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { permLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = WrombleRed)
                ) { Text("Giv adgang") }
            }
        }

        // header
        Column(Modifier.align(Alignment.TopCenter).padding(top = 60.dp, start = 24.dp, end = 24.dp)) {
            Text(
                if (mode == "table") "Scan bordets QR-kode" else "Scan i butikken",
                color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
            )
            Text(
                if (mode == "table") "Sæt dig ved bordet og scan for at bestille"
                else "Spring køen over – scan og bestil",
                color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )
        }

        error?.let {
            LaunchedEffect(it) { kotlinx.coroutines.delay(2200); error = null }
            Box(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp)
                    .clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) { Text(it, color = Color.White) }
        }

        IconButton(
            onClick = { nav.popBackStack() },
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 48.dp, end = 12.dp)
                .size(44.dp).clip(RoundedCornerShape(22.dp)).background(Color.Black.copy(alpha = 0.5f))
        ) { Icon(Icons.Filled.Close, "Luk", tint = Color.White) }
    }
}
