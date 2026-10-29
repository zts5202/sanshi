package com.zts.sanshi.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.zts.sanshi.MainActivity
import com.zts.sanshi.R
import com.zts.sanshi.data.taskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ensureChannel(context)
                val snap = taskRepository(context).observe().first()
                if (snap.reminder.enabled) {
                    ReminderScheduler.apply(context, snap.reminder)
                }
                val open = PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                val notif = NotificationCompat.Builder(context, ChannelId)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("三事")
                    .setContentText("今天最重要的三件事想好了吗？")
                    .setContentIntent(open)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .build()
                NotificationManagerCompat.from(context).notify(NotificationId, notif)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ChannelId = "sanshi_daily"
        const val NotificationId = 1001

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val mgr = context.getSystemService(NotificationManager::class.java) ?: return
            mgr.createNotificationChannel(
                NotificationChannel(ChannelId, "每日提醒", NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snap = taskRepository(context).observe().first()
                ReminderScheduler.apply(context, snap.reminder)
            } finally {
                pending.finish()
            }
        }
    }
}
