package dk.wromble.app.data

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dk.wromble.app.MainActivity
import dk.wromble.app.WrombleApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Kundens LIVE ordre-banner paa laast skaerm (Android-pendant til iOS Live Activity).
 *
 * Viser en fast (ongoing) notifikation med forretningens navn, ordre-status og en
 * fremdrifts-bjaelke (Modtaget -> Bekraeftet -> Paa vej -> Leveret) + live ETA i minutter
 * naar chaufføeren er paa vej. Opdateres selv naar app'en er lukket/skaermen laast, fordi
 * det er en forgrunds-tjeneste (poller order-status hvert 15. sek). Ved hvert nyt trin
 * spilles en behagelig lyd. Naar ordren er leveret/afvist lukker banneret sig selv.
 */
class OrderTrackingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var loop: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var orderId = 0
    private var companyName = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val oid = intent?.getIntExtra(EXTRA_ORDER_ID, 0) ?: 0
        val name = intent?.getStringExtra(EXTRA_COMPANY) ?: ""
        if (oid <= 0) { stopSelfSafe(); return START_NOT_STICKY }
        orderId = oid
        if (name.isNotBlank()) companyName = name

        startForegroundSafe(companyName.ifBlank { "Din ordre" }, "Følger din ordre …", 0)
        if (loop?.isActive != true) loop = scope.launch { trackLoop() }
        return START_STICKY
    }

    private fun buildNotification(title: String, text: String, stage: Int, ongoing: Boolean): Notification {
        val open = PendingIntent.getActivity(
            this, 2, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val b = NotificationCompat.Builder(this, WrombleApp.CH_TRACK)
            .setSmallIcon(android.R.drawable.ic_menu_directions)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setOngoing(ongoing)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)   // vis paa laast skaerm
            .setColor(0xFFE20F1E.toInt())
        // Fremdrifts-bjaelke: 4 trin (0..3). Skjules ved afvist.
        if (stage in 0..3) b.setProgress(3, stage.coerceIn(0, 3), false)
        return b.build()
    }

    private fun startForegroundSafe(title: String, text: String, stage: Int) {
        val n = buildNotification(title, text, stage, ongoing = true)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(TRACK_NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(TRACK_NOTIF_ID, n)
            }
        }
        runCatching {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "wromble:ordertrack").apply {
                setReferenceCounted(false); acquire()
            }
        }
    }

    private suspend fun trackLoop() {
        var lastStage = Int.MIN_VALUE
        while (scope.isActive) {
            val st = try { Api.service.orderStatus(orderId) } catch (_: Exception) { null }
            if (st != null) {
                if (st.companyName.isNotBlank()) companyName = st.companyName
                val stage = st.stage
                val title = companyName.ifBlank { "Din ordre" }
                val line = statusLine(st)

                // Nyt trin: behagelig lyd + en tydelig besked (heads-up) til kunden.
                if (lastStage != Int.MIN_VALUE && stage > lastStage && stage in 0..3) {
                    if (Settings.notificationsEnabled) {
                        Notifier.playChime(this)
                        Notifier.notify(this, 5100 + orderId % 1000, title, line, WrombleApp.CH_STATUS)
                    }
                }
                lastStage = stage

                if (stage >= 3 || stage < 0) {
                    // Leveret eller afvist: vis en sidste (ikke-ongoing) besked og luk banneret.
                    val done = buildNotification(title, line, if (stage < 0) -1 else 3, ongoing = false)
                    runCatching { NotificationManagerCompat.from(this).notify(TRACK_NOTIF_ID, done) }
                    runCatching { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) stopForeground(STOP_FOREGROUND_DETACH) }
                    stopSelfSafe(); return
                } else {
                    runCatching {
                        NotificationManagerCompat.from(this).notify(TRACK_NOTIF_ID, buildNotification(title, line, stage, true))
                    }
                }
            }
            delay(POLL_MS)
        }
    }

    private fun statusLine(st: OrderStatus): String = when {
        st.stage >= 3 -> "Din ordre er leveret. Velbekomme!"
        st.stage < 0  -> "Ordren blev desværre afvist."
        st.stage == 2 -> when {
            st.etaMinutes > 0 -> "På vej – ca. ${st.etaMinutes} min. til dig"
            st.etaText.contains("fremme") -> "Chaufføren er næsten fremme"
            else -> "Din ordre er på vej til dig"
        }
        st.stage == 1 -> "${companyName.ifBlank { "Restauranten" }} er gået i gang med din ordre"
        else -> "Din ordre er modtaget"
    }

    private fun stopSelfSafe() {
        runCatching { wakeLock?.let { if (it.isHeld) it.release() } }; wakeLock = null
        runCatching { stopSelf() }
    }

    override fun onDestroy() {
        loop?.cancel()
        runCatching { scope.cancel() }
        runCatching { wakeLock?.let { if (it.isHeld) it.release() } }
        wakeLock = null
        super.onDestroy()
    }

    companion object {
        private const val TRACK_NOTIF_ID = 5099
        private const val EXTRA_ORDER_ID = "order_id"
        private const val EXTRA_COMPANY = "company"
        private const val POLL_MS = 15_000L

        // Startes naar kunden aabner sporings-skaermen for en aktiv ordre.
        fun start(ctx: Context, orderId: Int, companyName: String) {
            if (orderId <= 0) return
            val i = Intent(ctx, OrderTrackingService::class.java)
                .putExtra(EXTRA_ORDER_ID, orderId)
                .putExtra(EXTRA_COMPANY, companyName)
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i)
                else ctx.startService(i)
            }
        }

        fun stop(ctx: Context) {
            runCatching { ctx.stopService(Intent(ctx, OrderTrackingService::class.java)) }
        }
    }
}
