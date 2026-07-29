package dk.wromble.app.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dk.wromble.app.WrombleApp

object Notifier {

    private fun canPost(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun notify(ctx: Context, id: Int, title: String, text: String, channel: String = WrombleApp.CH_STATUS) {
        if (!Settings.notificationsEnabled || !canPost(ctx)) return
        val n = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(if (channel == WrombleApp.CH_ORDERS) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .build()
        runCatching { NotificationManagerCompat.from(ctx).notify(id, n) }
    }

    private var alarmPlayer: MediaPlayer? = null
    private var alarmVibrator: Vibrator? = null
    private val alarmHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var alarmStop: Runnable? = null

    // Loud alarm + vibration for merchants/drivers when a new order arrives.
    // Spiller (i loop) i [seconds] sekunder og stopper saa af sig selv. seconds<=0 = ingen lyd.
    // Kan afbrydes med stopAlarm() (fx naar forretningen accepterer ordren).
    fun playAlarm(ctx: Context, seconds: Int = 5) {
        if (seconds <= 0) return
        stopAlarm()
        runCatching {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val mp = MediaPlayer()
            // USAGE_ALARM: forretnings-/chauffoer-alarmen skal lyde selv naar telefonen
            // er paa lydloes/vibration (spilles paa alarm-kanalen, ikke notifikation).
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            mp.setDataSource(ctx, uri)
            mp.isLooping = true
            mp.prepare()
            mp.start()
            alarmPlayer = mp
        }
        runCatching {
            val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            alarmVibrator = vib
            val pattern = longArrayOf(0, 400, 300, 400, 300)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createWaveform(pattern, 0))  // 0 = gentag
            } else {
                @Suppress("DEPRECATION") vib.vibrate(pattern, 0)
            }
        }
        val stop = Runnable { stopAlarm() }
        alarmStop = stop
        alarmHandler.postDelayed(stop, seconds * 1000L)
    }

    // Stopper alarmen med det samme (lyd + vibration + planlagt stop).
    fun stopAlarm() {
        alarmStop?.let { alarmHandler.removeCallbacks(it) }; alarmStop = null
        runCatching { alarmPlayer?.stop(); alarmPlayer?.release() }; alarmPlayer = null
        runCatching { alarmVibrator?.cancel() }; alarmVibrator = null
    }
}
