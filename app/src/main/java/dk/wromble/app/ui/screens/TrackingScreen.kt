package dk.wromble.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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

            // Leverings-illustration (restaurant + bil paa roed rute + hus). Ren, professionel
            // visning i stedet for et rigtigt kort - bilen flytter sig efter ordrens status.
            if (!rejected && status != null) {
                Spacer(Modifier.height(22.dp))
                DeliveryRouteIllustration(
                    stage = stage.coerceIn(0, 3),
                    isDelivery = status?.isDelivery ?: true,
                    companyName = status?.companyName ?: ""
                )
            }

            // Adresse-info + "Vis på kort". Vi åbner systemets kort-app (Google Maps/kort)
            // via en geo:-intent i stedet for et indlejret kort. Det er 100% stabilt – der
            // er intet indlejret kort-view der kan crashe sporingsskaermen, og kunden lander
            // direkte i deres kort-app med ruten.
            val s = status
            if (s != null && s.companyAddress.isNotBlank()) {
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Place, null, tint = WrombleRed, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Afhentes hos", color = Color(0xFF8A8A90), fontSize = 12.sp)
                        Text(s.companyName.ifBlank { "Restaurant" }, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(s.companyAddress, color = Color(0xFF6B6B72), fontSize = 13.sp)
                    }
                }
            }
            if (s != null && (s.companyLat != 0.0 || s.customerLat != 0.0)) {
                Spacer(Modifier.height(14.dp))
                OutlinedButton(
                    onClick = {
                        runCatching {
                            val lat: Double; val lng: Double; val label: String
                            if (s.isDelivery && (s.customerLat != 0.0 || s.customerLng != 0.0)) {
                                lat = s.customerLat; lng = s.customerLng; label = "Leveringsadresse"
                            } else {
                                lat = s.companyLat; lng = s.companyLng; label = s.companyName.ifBlank { "Restaurant" }
                            }
                            val enc = Uri.encode(label)
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$lat,$lng($enc)"))
                            ctx.startActivity(intent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.LocationOn, null, tint = WrombleRed, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (s.isDelivery) "Vis leveringsadresse på kort" else "Vis restaurant på kort",
                        color = WrombleRed, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// Professionel leverings-illustration: restaurant til venstre, kundens hus til hoejre,
// en roed rute imellem, og en bil/bud der bevaeger sig langs ruten efter ordrens status.
@Composable
private fun DeliveryRouteIllustration(stage: Int, isDelivery: Boolean, companyName: String) {
    val target = when {
        stage <= 0 -> 0.04f
        stage == 1 -> 0.24f
        stage == 2 -> 0.62f
        else -> 1f
    }
    val anim by animateFloatAsState(targetValue = target, animationSpec = tween(600), label = "route")
    val density = LocalDensity.current
    BoxWithConstraints(
        Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFFEAF2FF), Color(0xFFF1F8F0))))
    ) {
        val wPx = with(density) { maxWidth.toPx() }
        val hPx = with(density) { maxHeight.toPx() }
        val y = hPx * 0.60f
        val startX = wPx * 0.16f
        val endX = wPx * 0.84f
        val carX = startX + (endX - startX) * anim

        Canvas(Modifier.fillMaxSize()) {
            drawLine(
                Color(0x559E9E9E), Offset(startX, y), Offset(endX, y),
                strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 18f))
            )
            drawLine(
                WrombleRed, Offset(startX, y), Offset(carX, y),
                strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round
            )
        }

        fun toDp(px: Float) = with(density) { px.toDp() }
        RouteMarker(Icons.Filled.Restaurant, WrombleRed,
            Modifier.offset(x = toDp(startX) - 17.dp, y = toDp(y) - 17.dp))
        RouteMarker(if (isDelivery) Icons.Filled.Home else Icons.Filled.ShoppingBag, Color(0xFF2E6FF2),
            Modifier.offset(x = toDp(endX) - 17.dp, y = toDp(y) - 17.dp))
        Box(
            Modifier.offset(x = toDp(carX) - 21.dp, y = toDp(y) - 21.dp - 22.dp)
                .size(42.dp).clip(CircleShape).background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(if (isDelivery) Icons.Filled.DirectionsCar else Icons.Filled.ShoppingBag,
                null, tint = WrombleRed, modifier = Modifier.size(20.dp))
        }
        Row(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(companyName.ifBlank { "Restaurant" }, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, modifier = Modifier.weight(1f))
            Text(if (isDelivery) "Dig" else "Afhentning", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RouteMarker(icon: ImageVector, tint: Color, modifier: Modifier) {
    Box(modifier.size(34.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
    }
}
