package dk.wromble.app

import android.app.Application
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
import org.osmdroid.config.Configuration

class WrombleApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()

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
            .respectCacheHeaders(false)
            .build()

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java) ?: return

        // High-priority merchant/driver alarm channel (loud, vibrate)
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val orders = NotificationChannel(
            CH_ORDERS, "Nye ordrer", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alarm ved nye ordrer og leverancer"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 250, 500, 250, 500)
            setSound(
                alarmSound,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }
        nm.createNotificationChannel(orders)

        // Customer order-status updates
        val status = NotificationChannel(
            CH_STATUS, "Ordrestatus", NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Opdateringer om dine bestillinger" }
        nm.createNotificationChannel(status)
    }

    companion object {
        const val CH_ORDERS = "wromble_orders"
        const val CH_STATUS = "wromble_status"
    }
}
