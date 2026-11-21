package com.rfm.ms

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder

class MsService : Service() {
    private var wl: WifiManager.WifiLock? = null
    override fun onBind(i: Intent?): IBinder? = null

    private fun nb(ch: String): Notification.Builder =
        if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, ch) else Notification.Builder(this)

    private fun pi(open: String?): PendingIntent {
        val i = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (open != null) i.putExtra("open", open)
        return PendingIntent.getActivity(this, open?.hashCode() ?: 0, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onCreate() {
        super.onCreate()
        Core.init(applicationContext)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(NotificationChannel("run", "MS", NotificationManager.IMPORTANCE_MIN))
            nm.createNotificationChannel(NotificationChannel("msg", "رسائل MS", NotificationManager.IMPORTANCE_HIGH))
        }
        val n = nb("run").setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle("MS يعمل في الخلفية").setContentText("رقمك: ${Core.myNum}")
            .setContentIntent(pi(null)).setOngoing(true).build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC) else startForeground(1, n)
        wl = try {
            (applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager)
                .createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "ms").also { it.acquire() }
        } catch (e: Exception) { null }
        Core.onNotify = { num, m ->
            val prev = when (m.type) { 0 -> m.text; 1 -> "📷 صورة"; 2 -> "🎤 رسالة صوتية"; else -> "📁 ${m.text}" }
            nm.notify(num.hashCode(), nb("msg").setSmallIcon(android.R.drawable.stat_notify_chat)
                .setContentTitle(Core.nameOf(num)).setContentText(prev).setAutoCancel(true).setContentIntent(pi(num)).build())
        }
        Core.start()
    }

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int = START_STICKY

    override fun onDestroy() {
        try { wl?.release() } catch (e: Exception) {}
        Core.onNotify = null
        Core.stop()
        super.onDestroy()
    }
}
