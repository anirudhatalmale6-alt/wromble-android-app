package dk.wromble.app

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
import androidx.core.content.ContextCompat
import dk.wromble.app.data.Api
import dk.wromble.app.data.LocationProvider
import dk.wromble.app.data.Notifier
import dk.wromble.app.data.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// Baggrunds-"vagt" for chaufføeren. Kører som forgrundstjeneste, saa den fortsaetter
// med at hente nye leverancer OG afspille alarmen selv naar appen er i baggrunden eller
// skaermen er slukket/laast. Uden denne tjeneste stopper poll-loopet (der er bundet til
// UI'et) naar skaermen slukkes, og chaufføeren faar ingen lyd - praecis den fejl kunden fandt.
class DriverAlarmService : Service() {

    companion object {
        const val EXTRA_RIDER = "rider_id"
        const val EXTRA_COMPANY = "company_id"
        private const val ONGOING_ID = 4001
        private const val NEW_ORDER_ID = 4002

        // Saa UI-poll-loopet ved at tjenesten allerede haandterer alarmen (undgaar dobbelt-lyd).
        @Volatile var running = false

        fun start(ctx: Context, riderId: Int, companyId: Int) {
            if (riderId <= 0) return
            val i = Intent(ctx, DriverAlarmService::class.java)
                .putExtra(EXTRA_RIDER, riderId)
                .putExtra(EXTRA_COMPANY, companyId)
            runCatching { ContextCompat.startForegroundService(ctx, i) }
        }

        fun stop(ctx: Context) {
            runCatching { ctx.stopService(Intent(ctx, DriverAlarmService::class.java)) }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val seen = HashSet<Int>()
    private var primed = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val rider = intent?.getIntExtra(EXTRA_RIDER, 0) ?: 0
        val company = intent?.getIntExtra(EXTRA_COMPANY, 0) ?: 0
        if (rider <= 0) { stopSelf(); return START_NOT_STICKY }
        startForegroundCompat()
        running = true
        acquireWake()
        if (job?.isActive != true) {
            job = scope.launch { pollLoop(rider, company) }
        }
        return START_STICKY
    }

    private fun startForegroundCompat() {
        val tap = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(this, WrombleApp.CH_STATUS)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Wromble - paa vagt")
            .setContentText("Du faar lyd ved nye leverancer, ogsaa naar skaermen er slukket.")
            .setOngoing(true)
            .setContentIntent(tap)
            .build()
        val canLoc = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && LocationProvider.hasPermission(this)
        val ok = runCatching {
            if (canLoc) startForeground(ONGOING_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            else startForeground(ONGOING_ID, n)
        }.isSuccess
        // startForeground SKAL kaldes indenfor faa sek. efter startForegroundService, ellers
        // dræber systemet appen - sidste udvej hvis den typede variant fejlede.
        if (!ok) runCatching { startForeground(ONGOING_ID, n) }
    }

    private fun acquireWake() {
        runCatching {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            val wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "wromble:driver-shift")
            wl.setReferenceCounted(false)
            wl.acquire(8 * 60 * 60 * 1000L) // maks 8 timer; frigives i onDestroy
            wakeLock = wl
        }
    }

    private suspend fun pollLoop(rider: Int, company: Int) {
        while (scope.isActive) {
            try {
                val r = Api.service.driverOrders(rider, company)
                val ids = r.orders.map { it.id }.toSet()
                if (primed) {
                    val fresh = ids - seen
                    if (fresh.isNotEmpty()) onNewOrder()
                }
                seen.clear(); seen.addAll(ids); primed = true
                // Live GPS mens der er aktive leverancer (samme som UI'et gjorde).
                if (r.orders.isNotEmpty()) {
                    LocationProvider.lastKnown(applicationContext)?.let { loc ->
                        runCatching {
                            Api.service.driverLocation(
                                mapOf("rider_id" to rider, "latitude" to loc.latitude, "longitude" to loc.longitude)
                            )
                        }
                    }
                }
            } catch (_: Exception) { /* netvaerksfejl maa ikke vaelte vagten */ }
            delay(5000)
        }
    }

    private fun onNewOrder() {
        // Den kraftige alarm (USAGE_ALARM + wake lock) - lyder ogsaa med slukket skaerm.
        Notifier.playAlarm(applicationContext, Settings.driverAlarmSeconds)
        // Fuldskaerms-notifikation for at vaekke skaermen. Falder blødt tilbage til en
        // almindelig heads-up hvis fuldskaerm ikke er tilladt (Android 14+).
        runCatching {
            val full = PendingIntent.getActivity(
                this, 1, Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val n = NotificationCompat.Builder(this, WrombleApp.CH_ORDERS)
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setContentTitle("Ny leverance!")
                .setContentText("Du har en ny leverance i Wromble.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setAutoCancel(true)
                .setFullScreenIntent(full, true)
                .build()
            NotificationManagerCompat.from(this).notify(NEW_ORDER_ID, n)
        }
    }

    override fun onDestroy() {
        running = false
        job?.cancel(); job = null
        runCatching { wakeLock?.release() }; wakeLock = null
        Notifier.stopAlarm()
        scope.cancel()
        super.onDestroy()
    }
}
