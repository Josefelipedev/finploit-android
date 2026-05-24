package com.finploit.android.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.finploit.android.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val CHANNEL_RECURRING = "finploit_recurring"
        const val CHANNEL_GOALS = "finploit_goals"
        const val CHANNEL_BUDGET = "finploit_budget"
        const val CHANNEL_MEAL = "finploit_meal"
        // Keep old reference for backward compatibility
        const val CHANNEL_ID = CHANNEL_RECURRING
        const val CHANNEL_NAME = "Contas Recorrentes"
        const val CHANNEL_DESCRIPTION = "Lembretes de contas e entradas recorrentes"
    }

    fun createNotificationChannels() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        listOf(
            Triple(CHANNEL_RECURRING, "Contas Recorrentes", "Lembretes de contas e entradas recorrentes"),
            Triple(CHANNEL_GOALS, "Metas Financeiras", "Progresso e conclusão de metas"),
            Triple(CHANNEL_BUDGET, "Orçamento", "Alertas de limite de orçamento por categoria"),
            Triple(CHANNEL_MEAL, "Alimentação", "Lembretes de hora de refeição e meal prep"),
        ).forEach { (id, name, desc) ->
            manager.createNotificationChannel(
                NotificationChannel(id, name, NotificationManager.IMPORTANCE_DEFAULT).apply { description = desc }
            )
        }
    }

    // Legacy method kept for compatibility
    fun createNotificationChannel() = createNotificationChannels()

    fun sendNotification(id: Int, channelId: String = CHANNEL_RECURRING, title: String, body: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    // Legacy method
    fun sendRecurringReminder(id: Int, title: String, body: String) =
        sendNotification(id, CHANNEL_RECURRING, title, body)
}
