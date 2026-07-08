package dk.wromble.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.composed
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import dk.wromble.app.data.imageUrl
import dk.wromble.app.ui.theme.WrombleDarkRed
import dk.wromble.app.ui.theme.WrombleRed
import java.util.Locale

fun kr(v: Double): String = String.format(Locale.GERMAN, "%.2f kr", v)

fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = composed {
    val source = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    this.clickable(
        interactionSource = source,
        indication = null
    ) { onClick() }
}

val brandGradient = Brush.verticalGradient(listOf(WrombleRed, WrombleDarkRed))

@Composable
fun NetworkImage(
    path: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val ctx = LocalContext.current
    val url = imageUrl(path)
    if (url == null) {
        Box(
            modifier.background(Color(0xFFEDEDF0)),
            contentAlignment = Alignment.Center
        ) { Text("🍽️", fontSize = 34.sp) }
    } else {
        AsyncImage(
            model = ImageRequest.Builder(ctx).data(url).crossfade(true).build(),
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}

@Composable
fun Pill(text: String, bg: Color, fg: Color = Color.White) {
    Box(
        Modifier.clip(RoundedCornerShape(20.dp)).background(bg)
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(text, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

fun orderStatusLabel(status: String): Pair<String, Color> = when (status.lowercase()) {
    "completed", "delivered" -> "Leveret" to Color(0xFF16A34A)
    "processing", "preparing" -> "Tilberedes" to Color(0xFFEA8A0C)
    "cancelled", "rejected" -> "Annulleret" to WrombleRed
    "on_the_way", "delivering" -> "Paa vej" to Color(0xFF2563EB)
    else -> "Afventer" to Color(0xFF2563EB)
}
