package dk.wromble.app.data

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dk.wromble.app.MainActivity
import dk.wromble.app.WrombleApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Firebase Cloud Messaging: giver lyd + notifikation paa laast skaerm OGSAA naar app'en
 * er helt lukket - uden en forgrunds-tjeneste (og dermed uden Google Play's FGS-erklaering).
 *
 * Serveren sender DATA-beskeder (ikke "notification"-payload), saa onMessageReceived kaldes
 * ogsaa i baggrunden/lukket, og vi selv bygger notifikationen + spiller den rigtige lyd:
 *   type=new_order / new_delivery  -> forretning/chauffoer: hoej alarm (Notifier.playAlarm) + banner
 *   type=order_status              -> kunde: behagelig lyd + live ordre-banner (fremdrift + ETA)
 */
class WrombleMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        // Nyt FCM-token: registrér det hos serveren (hvis nogen er logget ind).
        PushTokens.registerToken(applicationContext, token)
    }

    override fun onMessageReceived(msg: RemoteMessage) {
        val d = msg.data
        val type = d["type"] ?: ""
        val title = d["title"] ?: "Wromble"
        val body = d["body"] ?: ""
        val ctx = applicationContext

        when (type) {
            "new_order", "new_delivery" -> {
                // Forretning/chauffoer: hoej alarm (lyder selv paa lydloes) + tydelig heads-up.
                if (Settings.notificationsEnabled) {
                    val seconds = d["alarm_seconds"]?.toIntOrNull()
                        ?: (if (type == "new_delivery") Settings.driverAlarmSeconds else 10)
                    Notifier.playAlarm(ctx, seconds.coerceAtLeast(3))
                    postAlert(ctx, WrombleApp.CH_ORDERS, 6001, title, body, ongoing = false, stage = -99)
                }
            }
            "order_status" -> {
                // Kunde: "kørende" ordre-notifikation med trin-tidslinje (Android-pendant til
                // iOS Live Activity - Android har ikke Live Activities, saa vi bruger en
                // opdaterende notifikation med fremdrifts-bjaelke + de 4 trin).
                val stage = d["stage"]?.toIntOrNull() ?: -99
                postOrderProgress(ctx, title, body, stage)
            }
            else -> {
                if (title.isNotBlank() || body.isNotBlank())
                    postAlert(ctx, WrombleApp.CH_STATUS, 6009, title, body, ongoing = false, stage = -99)
            }
        }
    }

    // Kundens "kørende" ordre-notifikation: én notifikation (fast id 5099) der OPDATERER
    // sig selv trin for trin med en fremdrifts-bjaelke og de fire trin (✅ udfoert, 🔴 nu,
    // ⚪ mangler). Lyder ved hvert trin (CH_ALERT). Bliver "ongoing" mens ordren koerer og
    // lukker naar den er leveret. Android-pendant til iOS' Live Activity-kort.
    private fun postOrderProgress(ctx: Context, title: String, body: String, stage: Int) {
        if (!Settings.notificationsEnabled) return
        val steps = listOf("Modtaget", "Tilberedes", "På vej", "Leveret")
        val lines = steps.mapIndexed { i, s ->
            val mark = when {
                i < stage -> "✅"
                i == stage -> "🔴"
                else -> "⚪"
            }
            "$mark  $s"
        }.joinToString("\n")
        val delivered = stage >= 3
        val open = PendingIntent.getActivity(
            ctx, 5099, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val b = NotificationCompat.Builder(ctx, WrombleApp.CH_ALERT)
            .setSmallIcon(android.R.drawable.ic_menu_directions)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body + "\n\n" + lines))
            .setSubText(if (delivered) "Leveret ✓" else "Følg din ordre")
            .setContentIntent(open)
            .setColor(0xFFE20F1E.toInt())
            .setOnlyAlertOnce(false)                                  // lyd/heads-up ved HVERT trin
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)      // vis paa laast skaerm
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        if (stage in 0..3) b.setProgress(3, stage.coerceIn(0, 3), false)
        if (delivered) { b.setOngoing(false); b.setAutoCancel(true) } else { b.setOngoing(true) }
        runCatching { NotificationManagerCompat.from(ctx).notify(5099, b.build()) }
    }

    private fun postAlert(ctx: Context, channel: String, id: Int, title: String, body: String, ongoing: Boolean, stage: Int) {
        val open = PendingIntent.getActivity(
            ctx, id, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val b = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(if (channel == WrombleApp.CH_TRACK) android.R.drawable.ic_menu_directions else android.R.drawable.ic_dialog_email)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setAutoCancel(!ongoing)
            .setOngoing(ongoing)
            .setOnlyAlertOnce(channel == WrombleApp.CH_TRACK)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)     // vis paa laast skaerm
            .setColor(0xFFE20F1E.toInt())
            .setPriority(if (channel == WrombleApp.CH_ORDERS) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
        if (channel == WrombleApp.CH_ORDERS) b.setCategory(NotificationCompat.CATEGORY_ALARM)
        // Live ordre-banner: fremdrifts-bjaelke over de 4 trin (0..3).
        if (stage in 0..3) b.setProgress(3, stage.coerceIn(0, 3), false)
        runCatching { NotificationManagerCompat.from(ctx).notify(id, b.build()) }
    }
}

/**
 * Henter det aktuelle FCM-token og registrerer det hos serveren for den bruger der er
 * logget ind (kunde/forretning/chauffoer udledes server-side af login-token'et).
 */
object PushTokens {
    fun register(ctx: Context) {
        runCatching {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                if (!token.isNullOrBlank()) registerToken(ctx, token)
            }
        }
    }

    fun registerToken(ctx: Context, token: String) {
        // Kun relevant naar nogen er logget ind (serveren binder token til den bruger).
        if (Session.user == null) return
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                Api.service.registerPushToken(mapOf("token" to token, "platform" to "android"))
            }
        }
    }
}
