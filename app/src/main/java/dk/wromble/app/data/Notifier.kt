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
            val mp = MediaPlayer()
            // USAGE_ALARM: forretnings-/chauffoer-alarmen skal lyde selv naar telefonen
            // er paa lydloes/vibration (spilles paa alarm-kanalen, ikke notifikation).
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            // Brug den valgte melodi (genereret WAV, samme toner som iOS). Falder tilbage
            // til systemets alarm-lyd hvis genereringen mod forventning fejler.
            var usedFile = false
            runCatching {
                val f = melodyWav(ctx, Settings.alarmMelody)
                if (f.exists() && f.length() > 0) { mp.setDataSource(f.absolutePath); usedFile = true }
            }
            if (!usedFile) {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                mp.setDataSource(ctx, uri)
            }
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

    // Navne paa de indbyggede melodier (samme raekkefoelge/lyd som iOS-appen).
    val melodyNames = listOf("Klassisk", "Stigende", "Hurtig")

    // Kort forhaandsvisning (2 sek) af en melodi.
    fun previewMelody(ctx: Context, melody: Int) {
        Settings.setAlarmMelody(ctx, melody)
        playAlarm(ctx, 2)
    }

    // Genererer (og cacher) en WAV-fil for melodien. Samme toner som iOS' WrombleAlarm.
    private fun melodyWav(ctx: Context, melody: Int): java.io.File {
        val f = java.io.File(ctx.cacheDir, "wr_alarm_$melody.wav")
        if (f.exists() && f.length() > 0) return f
        runCatching { f.writeBytes(makeWav(melody)) }
        return f
    }

    private fun makeWav(melody: Int): ByteArray {
        val sr = 44100
        val tones: DoubleArray; val beep: Double; val gap: Double; val count: Int
        when (melody) {
            1 -> { tones = doubleArrayOf(880.0, 1108.7, 1318.5); beep = 0.16; gap = 0.05; count = 9 }
            2 -> { tones = doubleArrayOf(1318.5);                beep = 0.09; gap = 0.05; count = 12 }
            else -> { tones = doubleArrayOf(1046.5, 784.0);      beep = 0.18; gap = 0.06; count = 8 }
        }
        val pcm = ArrayList<Short>()
        for (t in 0 until count) {
            val f = tones[t % tones.size]
            val bn = (sr * beep).toInt()
            for (k in 0 until bn) {
                val x = k.toDouble() / sr
                val env = minOf(1.0, minOf(k.toDouble(), (bn - k).toDouble()) / (sr * 0.005))
                val s = Math.sin(2.0 * Math.PI * f * x) * env * 0.9
                pcm.add((maxOf(-1.0, minOf(1.0, s)) * 32767).toInt().toShort())
            }
            repeat((sr * gap).toInt()) { pcm.add(0.toShort()) }
        }
        val dataSize = pcm.size * 2
        val buf = java.nio.ByteBuffer.allocate(44 + dataSize).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        buf.put("RIFF".toByteArray(Charsets.US_ASCII)); buf.putInt(36 + dataSize)
        buf.put("WAVE".toByteArray(Charsets.US_ASCII))
        buf.put("fmt ".toByteArray(Charsets.US_ASCII)); buf.putInt(16)
        buf.putShort(1.toShort()); buf.putShort(1.toShort())              // PCM, 1 kanal
        buf.putInt(sr); buf.putInt(sr * 2)                                // sample rate, byte rate
        buf.putShort(2.toShort()); buf.putShort(16.toShort())            // block align, bits
        buf.put("data".toByteArray(Charsets.US_ASCII)); buf.putInt(dataSize)
        for (s in pcm) buf.putShort(s)
        return buf.array()
    }
}
