package dk.wromble.app

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.content.Context
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dk.wromble.app.data.Http
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.osmdroid.config.Configuration
import java.io.PrintWriter
import java.io.StringWriter

class WrombleApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()

        // Crash-rapportering FOERST, saa selv fejl under opstart fanges.
        installCrashReporter()
        sendPendingCrash()

        // OpenStreetMap (osmdroid) – app-private tile cache, no storage permission needed.
        val osm = Configuration.getInstance()
        osm.load(this, getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        osm.userAgentValue = packageName
        osm.osmdroidBasePath = cacheDir
        osm.osmdroidTileCache = java.io.File(cacheDir, "osm_tiles")

        createChannels()
    }

    // Coil uses the same bounded/retrying OkHttp client so image loads never flood the host.
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .okHttpClient(Http.client)
            .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.25).build() }
            .diskCache {
                DiskCache.Builder()
                    .directory(java.io.File(cacheDir, "image_cache"))
                    .maxSizeBytes(80L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            // Bitmap-downsampling / lav hukommelse (Google Plays "bitmap-effektivitet"-anbefaling):
            // allowRgb565 lader Coil bruge RGB_565 (halv hukommelse pr. billede) paa
            // hukommelses-begraensede enheder - ingen synlig forskel paa madfotos.
            // allowHardware holder billeder som hardware-bitmaps (mest effektive) hvor muligt.
            // Coil nedskalerer i forvejen hvert billede til den stoerrelse det vises i, saa
            // vi aldrig holder et fuldt-oploest bitmap i hukommelsen.
            .allowRgb565(true)
            .allowHardware(true)
            .respectCacheHeaders(false)
            .build()

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java) ?: return

        // Kanal til "ny ordre"-notifikationen. VIGTIGT: kanalen skal vaere LYDLOES og
        // uden vibration. Selve alarmen (den VALGTE melodi, i den VALGTE varighed) spilles
        // af Notifier.playAlarm() som en separat MediaPlayer - saa forretningen kan STOPPE
        // den praecist naar ordren accepteres/annulleres.
        // Tidligere havde kanalen OGSAA en lyd (systemets standard-alarm): den spillede
        // oveni melodien (to lyde paa én gang / "den gamle lyd henover"), i sin EGEN laengde
        // (passede ikke med 5/10/15 sek), og kunne IKKE stoppes ved accept (kun Notifier's
        // egen afspiller kan vi stoppe - systemets kanal-lyd koerte videre). Derfor: helt
        // lydloes kanal, kun et banner.
        // Kanal-indstillinger er UFORANDERLIGE efter oprettelse, saa den gamle (lydende)
        // kanal slettes og en ny oprettes under et nyt id (wromble_orders_v2).
        runCatching { nm.deleteNotificationChannel("wromble_orders") }
        val orders = NotificationChannel(
            CH_ORDERS, "Nye ordrer", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Besked ved nye ordrer og leverancer"
            enableVibration(false)
            setSound(null, null)
        }
        nm.createNotificationChannel(orders)

        // Customer order-status updates
        val status = NotificationChannel(
            CH_STATUS, "Ordrestatus", NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Opdateringer om dine bestillinger" }
        nm.createNotificationChannel(status)

        // Fast (lydloes) notifikation for baggrunds-vagten (OrderPollService). LAV vigtighed,
        // saa den ikke larmer - selve ordre-alarmen kommer via CH_ORDERS + Notifier.playAlarm.
        val svc = NotificationChannel(
            CH_SERVICE, "Baggrundsvagt", NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Holder øje med nye ordrer mens app'en er lukket"
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }
        nm.createNotificationChannel(svc)

        // Kundens LIVE ordre-banner paa laast skaerm (Android-pendant til iOS Live Activity).
        // LAV vigtighed (fast banner, ingen heads-up/lyd) - trin-lyden spilles af Notifier.
        val track = NotificationChannel(
            CH_TRACK, "Live ordre-status", NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Live-banner der følger din ordre fra bekræftet til leveret"
            setShowBadge(true)
            enableVibration(false)
            setSound(null, null)
        }
        nm.createNotificationChannel(track)

        // --- SYSTEM-LYD-KANALER (vises + lyder af sig selv, OGSAA naar app'en er HELT lukket) ---
        // Serveren sender nu et "notification"-payload med disse kanal-id'er. Så viser Android
        // selv beskeden paa laast skaerm MED lyd - uden at app-koden skal koere. Det er det der
        // gjorde beskederne upaalidelige foer (data-only kraevede at app'en selv byggede beskeden,
        // hvilket svigter naar telefonen har lukket app-processen).

        // Ny ordre (forretning/chauffoer): HOEJ alarm-lyd, spiller selv paa lydloes.
        val alarmAttr = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val ordersLoud = NotificationChannel(
            CH_ORDERS_LOUD, "Nye ordrer (alarm)", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Høj alarm ved nye ordrer – også når appen er lukket"
            setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), alarmAttr)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 300, 500, 300, 500)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setShowBadge(true)
        }
        nm.createNotificationChannel(ordersLoud)

        // Kunde-beskeder (ordre accepteret/paa vej/leveret): almindelig notifikations-lyd.
        val alert = NotificationChannel(
            CH_ALERT, "Ordre-beskeder", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Lyd og besked ved ordre-opdateringer – også når appen er lukket"
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setShowBadge(true)
        }
        nm.createNotificationChannel(alert)
    }

    // Gemmer enhver ukendt (uncaught) crash med enheds-info, saa den kan sendes
    // ved naeste opstart. Delegerer bagefter til systemets normale handler, saa
    // adfaerden ellers er uaendret.
    private fun installCrashReporter() {
        val prefs = getSharedPreferences("wr_crash", Context.MODE_PRIVATE)
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, ex ->
            try {
                val sw = StringWriter()
                ex.printStackTrace(PrintWriter(sw))
                val report = buildString {
                    append("device=").append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
                        .append(" (").append(Build.DEVICE).append(")\n")
                    append("android=").append(Build.VERSION.RELEASE)
                        .append(" SDK ").append(Build.VERSION.SDK_INT).append('\n')
                    append("app=").append(BuildConfig.VERSION_NAME)
                        .append(" (").append(BuildConfig.VERSION_CODE).append(")\n")
                    append("thread=").append(thread.name).append("\n\n")
                    append(sw.toString())
                }
                prefs.edit().putString("pending", report).commit()
            } catch (_: Throwable) { /* aldrig fejle i fejl-handleren */ }
            prev?.uncaughtException(thread, ex)
        }
    }

    // Sender en evt. gemt crash fra sidste koersel (baggrundstraad, best effort).
    private fun sendPendingCrash() {
        val prefs = getSharedPreferences("wr_crash", Context.MODE_PRIVATE)
        val pending = prefs.getString("pending", null) ?: return
        prefs.edit().remove("pending").apply()
        Thread {
            try {
                val body = org.json.JSONObject().put("report", pending).toString()
                    .toRequestBody("application/json".toMediaType())
                val req = Request.Builder()
                    .url("https://wromble.dk/api/app-crash-log.php").post(body).build()
                Http.client.newCall(req).execute().use { }
            } catch (_: Throwable) { /* diagnostik maa aldrig crashe appen */ }
        }.start()
    }

    companion object {
        const val CH_ORDERS = "wromble_orders_v2"   // v2 = lydloes kanal (lyden styres af Notifier.playAlarm)
        const val CH_STATUS = "wromble_status"
        const val CH_SERVICE = "wromble_service"     // fast notifikation for baggrunds-vagten
        const val CH_TRACK = "wromble_track"         // kundens live ordre-banner
        const val CH_ORDERS_LOUD = "wromble_orders_v3_loud"  // system-lyd (hoej alarm) OGSAA naar app lukket
        const val CH_ALERT = "wromble_alert"                 // system-lyd for kunde-beskeder OGSAA naar app lukket

        // Er en Activity synlig? Baggrunds-vagten (OrderPollService) bruger dette til at
        // undgaa dobbelt-alarm: er app'en aaben, staar skaermens egen poller for lyden.
        @Volatile
        @JvmStatic
        var appInForeground: Boolean = false
    }
}
