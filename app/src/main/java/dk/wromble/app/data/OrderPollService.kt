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
 * Baggrunds-vagt for forretninger og chauffoerer.
 *
 * Problemet: den 5-sekunders opdatering der giver lyd + notifikation ved en ny ordre
 * koerer KUN mens app'en er aaben paa dashboard-skaermen. Er telefonen laast eller
 * app'en i baggrunden, poller ingenting -> ingen notifikation paa laast skaerm.
 *
 * Uden Firebase/push loeser vi det med en FORGRUNDS-tjeneste: en tjeneste med en
 * fast (lydloes) notifikation som Android holder i live selv naar app'en er lukket/laast.
 * Den poller serveren hvert ~12. sekund og udloeser SAMME hoeje alarm (Notifier.playAlarm,
 * USAGE_ALARM - lyder selv paa lydloes) + en heads-up notifikation naar en ny ordre
 * dukker op. En delvis wakelock holder CPU'en vaagen saa polling fortsaetter med
 * slukket skaerm.
 *
 * Dobbelt-alarm undgaas: er app'en i forgrunden, lader tjenesten den aabne skaerms
 * egen poller om lyden (WrombleApp.appInForeground) og noterer bare ordren som set.
 */
class OrderPollService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var loop: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundSafe()
        if (loop?.isActive != true) loop = scope.launch { pollLoop() }
        // START_STICKY: hvis systemet dræber tjenesten (fx hukommelsespres), genstartes den.
        return START_STICKY
    }

    private fun startForegroundSafe() {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n: Notification = NotificationCompat.Builder(this, WrombleApp.CH_SERVICE)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Wromble er klar")
            .setContentText("Holder øje med nye ordrer og leverancer")
            .setContentIntent(open)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(SERVICE_NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(SERVICE_NOTIF_ID, n)
            }
        }
        runCatching {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "wromble:orderpoll").apply {
                setReferenceCounted(false)
                acquire()
            }
        }
    }

    private suspend fun pollLoop() {
        val prefs = getSharedPreferences(SEEN_PREFS, Context.MODE_PRIVATE)
        var baselined = false
        // Set af ordre-id'er vi allerede har set/alarmeret for. Nulstilles ved hver ny start
        // saa vi ikke alarmerer for gamle ordrer, men baseline'r foerst (foerste poll = ingen lyd).
        val seen = HashSet<Int>()
        // Forretningens valgte alarm-varighed hentes fra serveren (samme som i appen). -1 = ikke hentet endnu.
        var companyAlarmSeconds = -1

        while (scope.isActive) {
            val s = Session.user
            // Kun forretning og chauffoer skal overvaages. Ingen bruger / kunde -> stop.
            val isCompany = s != null && s.type == "company" && s.companyId > 0
            val isDriver = s != null && (s.role.startsWith("chauff") || s.type == "rider")
            if (s == null || (!isCompany && !isDriver)) {
                stopSelfSafe(); return
            }
            if (isCompany && companyAlarmSeconds < 0) {
                companyAlarmSeconds = try { Api.service.companyAlarm(s.companyId).alarmSeconds } catch (_: Exception) { 10 }
            }

            val current: List<Pair<Int, String>> = try {
                if (isCompany) {
                    Api.service.companyOrders(s.companyId, "active").orders
                        .filter { it.isNew }                       // kun nye/afventende ordrer
                        .map { it.id to "Ny ordre #${it.id}" }
                } else {
                    val orders = Api.service.driverOrders(s.id, s.companyId).orders
                    // Send chaufføerens GPS med (ogsaa i baggrunden) saa kundens live-kort +
                    // ETA er praecise mens der er aktive leverancer. Uden dette opdateres
                    // positionen kun mens chauffoer-skaermen er aaben -> kunden saa en fast bil.
                    if (orders.isNotEmpty()) {
                        runCatching {
                            LocationProvider.lastKnown(this@OrderPollService)?.let { loc ->
                                Api.service.driverLocation(mapOf(
                                    "rider_id" to s.id, "latitude" to loc.latitude, "longitude" to loc.longitude))
                            }
                        }
                    }
                    orders.map { it.id to "Ny leverance #${it.id}" }
                }
            } catch (_: Exception) { emptyList() }

            if (current.isNotEmpty() || baselined) {
                val fresh = current.filter { it.first !in seen }
                if (baselined && fresh.isNotEmpty() && !WrombleApp.appInForeground) {
                    // App'en er lukket/i baggrunden -> tjenesten giver lyd + notifikation.
                    val seconds = if (isDriver) Settings.driverAlarmSeconds else companyAlarmSeconds.coerceAtLeast(5)
                    Notifier.playAlarm(this, seconds)
                    val title = if (isDriver) "Ny leverance!" else "Ny ordre!"
                    val body = if (fresh.size == 1) fresh.first().second
                               else "${fresh.size} nye ${if (isDriver) "leverancer" else "ordrer"}"
                    postAlert(title, body)
                }
                seen.clear(); seen.addAll(current.map { it.first })
                baselined = true
                prefs.edit().putInt("last_count", current.size).apply()
            }
            delay(POLL_MS)
        }
    }

    private fun postAlert(title: String, body: String) {
        if (!Settings.notificationsEnabled) return
        val open = PendingIntent.getActivity(
            this, 1, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(this, WrombleApp.CH_ORDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)   // vises paa laast skaerm
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(this).notify(ALERT_NOTIF_ID, n) }
    }

    private fun stopSelfSafe() {
        runCatching { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) stopForeground(STOP_FOREGROUND_REMOVE) }
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
        private const val SERVICE_NOTIF_ID = 4711
        private const val ALERT_NOTIF_ID = 4712
        private const val SEEN_PREFS = "wr_poll_seen"
        private const val POLL_MS = 12_000L

        // Startes naar en forretning/chauffoer er logget ind (fra deres dashboard).
        // Sikkert at kalde flere gange - tjenesten er en singleton.
        fun start(ctx: Context) {
            val s = Session.user ?: return
            val isCompany = s.type == "company" && s.companyId > 0
            val isDriver = s.role.startsWith("chauff") || s.type == "rider"
            if (!isCompany && !isDriver) return
            val i = Intent(ctx, OrderPollService::class.java)
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i)
                else ctx.startService(i)
            }
        }

        fun stop(ctx: Context) {
            runCatching { ctx.stopService(Intent(ctx, OrderPollService::class.java)) }
        }
    }
}
