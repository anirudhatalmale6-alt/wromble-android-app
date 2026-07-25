package dk.wromble.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import dk.wromble.app.data.Api
import dk.wromble.app.data.OrderStatus
import dk.wromble.app.ui.components.*
import dk.wromble.app.ui.kr
import dk.wromble.app.ui.theme.WrombleRed
import kotlinx.coroutines.delay
import org.osmdroid.util.GeoPoint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(nav: NavController, orderId: Int) {
    val ctx = LocalContext.current
    var status by remember { mutableStateOf<OrderStatus?>(null) }
    var loading by remember { mutableStateOf(true) }

    // Poll live status every 12s (matches iOS)
    LaunchedEffect(orderId) {
        while (true) {
            try { status = Api.service.orderStatus(orderId) } catch (_: Exception) {}
            loading = false
            delay(12000)
        }
    }

    val steps = listOf("Modtaget", "Bekræftet", "På vej", "Leveret")
    val stage = status?.stage ?: 0
    val rejected = stage < 0
    val progress by animateFloatAsState(
        targetValue = if (rejected) 0f else (stage.coerceIn(0, 3)) / 3f,
        animationSpec = tween(600), label = "prog"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ordre #$orderId", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 24.dp)
        ) {
            if (loading && status == null) {
                Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) { CircularProgressIndicator(color = WrombleRed) }
                return@Column
            }

            Spacer(Modifier.height(16.dp))
            // Progress ring
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(180.dp)) {
                    val stroke = 16.dp.toPx()
                    val d = size.minDimension - stroke
                    val topLeft = androidx.compose.ui.geometry.Offset((size.width - d) / 2, (size.height - d) / 2)
                    drawArc(
                        color = Color(0xFFE9E9EE), startAngle = -90f, sweepAngle = 360f, useCenter = false,
                        topLeft = topLeft, size = Size(d, d), style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                    if (!rejected) drawArc(
                        color = WrombleRed, startAngle = -90f, sweepAngle = 360f * progress, useCenter = false,
                        topLeft = topLeft, size = Size(d, d), style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (rejected) {
                        Text("❌", fontSize = 40.sp)
                        Text("Afvist", fontWeight = FontWeight.Black, fontSize = 20.sp, color = WrombleRed)
                    } else {
                        Text(status?.label?.ifBlank { steps.getOrElse(stage) { "" } } ?: steps.getOrElse(stage) { "" },
                            fontWeight = FontWeight.Black, fontSize = 20.sp)
                        Text("Trin ${stage + 1} af 4", color = Color(0xFF8A8A90), fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(status?.companyName ?: "", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            status?.total?.let { Text("Total: ${kr(it)}", color = Color(0xFF8A8A90)) }
            status?.description?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, color = Color(0xFF6B6B72), fontSize = 15.sp)
            }

            // Step chips row
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                steps.forEachIndexed { i, label ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Box(
                            Modifier.size(28.dp).clip(RoundedCornerShape(14.dp))
                                .background(if (!rejected && i <= stage) WrombleRed else Color(0xFFE3E3E8)),
                            contentAlignment = Alignment.Center
                        ) { Text("${i + 1}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                        Text(label, fontSize = 11.sp, maxLines = 1,
                            color = if (!rejected && i <= stage) MaterialTheme.colorScheme.onSurface else Color(0xFF9A9AA2),
                            modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            // Map with pins. Kortet er "nice-to-have" – hvis osmdroid af en eller anden
            // grund ikke kan initialiseres, springer vi kortet over i stedet for at crashe
            // hele sporingsskaermen.
            val s = status
            val mapView = remember { runCatching { newMapView(ctx) }.getOrNull() }
            if (mapView != null && s != null && (s.companyLat != 0.0 || s.customerLat != 0.0)) {
                Spacer(Modifier.height(16.dp))
                DisposableEffect(Unit) { mapView.onResume(); onDispose { mapView.onPause() } }
                LaunchedEffect(s.companyLat, s.customerLat) {
                    runCatching {
                        mapView.overlays.clear()
                        mapView.addPin(s.companyLat, s.companyLng, s.companyName, WrombleRedInt)
                        if (s.isDelivery) mapView.addPin(s.customerLat, s.customerLng, "Din adresse", BlueInt)
                        val center = if (s.companyLat != 0.0) GeoPoint(s.companyLat, s.companyLng)
                        else GeoPoint(s.customerLat, s.customerLng)
                        mapView.controller.setCenter(center)
                        mapView.invalidate()
                    }
                }
                AndroidView(factory = { mapView },
                    modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(16.dp)))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
