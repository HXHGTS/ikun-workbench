package ai.ikunattacker.komi

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat

class KomiService : Service() {
    companion object {
        const val NOTIF_ID = 1002
        private var wl: PowerManager.WakeLock? = null
        private var lastPri = "default"
        private var lastTitle = "ikun新媒体制作中心"
        private var lastText = ""
        private var lastProgress = -1

        fun createChannels(c: Context) {
            val nm = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val ids = arrayOf("ikun_gen_low", "ikun_gen_default", "ikun_gen_high", "ikun_result")
            val names = arrayOf("生成通知（低·静默常驻）", "生成通知（默认）", "生成通知（高·横幅提醒）", "生成结果")
            val imps = intArrayOf(
                NotificationManager.IMPORTANCE_LOW,
                NotificationManager.IMPORTANCE_DEFAULT,
                NotificationManager.IMPORTANCE_HIGH,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            for (i in ids.indices) {
                if (nm.getNotificationChannel(ids[i]) == null) {
                    val ch = NotificationChannel(ids[i], names[i], imps[i])
                    ch.setShowBadge(false)
                    nm.createNotificationChannel(ch)
                }
            }
        }

        private fun build(c: Context, pri: String, title: String, text: String, progress: Int): Notification {
            val launch = c.packageManager.getLaunchIntentForPackage(c.packageName)
            val pi = PendingIntent.getActivity(c, 0, launch,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            val b = NotificationCompat.Builder(c, "ikun_gen_$pri")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle(title).setContentText(text)
                .setOnlyAlertOnce(true).setOngoing(true)
                .setContentIntent(pi)
            if (progress >= 0) b.setProgress(100, progress, false) else b.setProgress(0, 0, true)
            return b.build()
        }

        fun start(c: Context, title: String, text: String, pri: String) {
            lastTitle = title; lastText = text; lastPri = pri; lastProgress = -1
            createChannels(c)
            val i = Intent(c, KomiService::class.java)
            try { c.startForegroundService(i) } catch (e: Exception) { }
        }

        fun notifyUpdate(c: Context, text: String, progress: Int) {
            if (text.isNotEmpty()) lastText = text
            lastProgress = progress
            createChannels(c)
            val nm = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIF_ID, build(c, lastPri, lastTitle, lastText, lastProgress))
        }

        fun notifyResult(c: Context, ok: Boolean, text: String) {
            createChannels(c)
            val nm = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val launch = c.packageManager.getLaunchIntentForPackage(c.packageName)
            val pi = PendingIntent.getActivity(c, 1, launch,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            val b = NotificationCompat.Builder(c, "ikun_result")
                .setSmallIcon(if (ok) android.R.drawable.ic_dialog_info else android.R.drawable.ic_dialog_alert)
                .setContentTitle(if (ok) "✅ 生成成功" else "❌ 生成失败")
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setAutoCancel(true).setContentIntent(pi)
            nm.notify(NOTIF_ID + 1, b.build())
        }

        fun stop(c: Context) {
            if (wl != null && wl!!.isHeld) wl!!.release()
            try { c.stopService(Intent(c, KomiService::class.java)) } catch (e: Exception) { }
            (c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(NOTIF_ID)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            it.getStringExtra("title")?.let { t -> lastTitle = t }
            it.getStringExtra("text")?.let { t -> lastText = t }
            it.getStringExtra("priority")?.let { p -> lastPri = p }
        }
        val n = build(this, lastPri, lastTitle, lastText, -1)
        if (Build.VERSION.SDK_INT >= 29)
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        else startForeground(NOTIF_ID, n)
        if (wl == null) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ikun:gen")
            wl!!.setReferenceCounted(false)
        }
        if (!wl!!.isHeld) wl!!.acquire()
        return START_NOT_STICKY
    }

    override fun onBind(i: Intent?): IBinder? = null

    override fun onDestroy() {
        if (wl != null && wl!!.isHeld) wl!!.release()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}
