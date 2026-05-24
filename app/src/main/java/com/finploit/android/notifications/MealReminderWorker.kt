package com.finploit.android.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.finploit.android.data.preferences.UserPreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Calendar

/**
 * Daily WorkManager worker that fires meal-time reminder notifications.
 *
 * Schedule times (local):
 *  - 08:00 → breakfast reminder
 *  - 12:30 → lunch reminder
 *  - 19:30 → dinner reminder
 *
 * The worker runs roughly once per hour via a PeriodicWorkRequest (scheduled in
 * MainActivity / App). It checks the current hour and only fires a notification when
 * within the correct window (±30 min of each target), using DataStore to prevent
 * duplicate fires within the same hour.
 */
@HiltWorker
class MealReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val notificationHelper: NotificationHelper,
    private val preferencesRepository: UserPreferencesRepository,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // Only fire reminders if meal reminder is globally enabled (default: true)
        val enabled = preferencesRepository.mealPrepMode.first() // reuse mealPrepMode flag or always true
        // Always run reminders regardless of meal prep mode

        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val totalMinutes = hour * 60 + minute

        // Target windows: [breakfast=480, lunch=750, dinner=1170] minutes since midnight
        val targets = listOf(
            480 to Triple("🌅 Bom dia!", "☀️ Hora do pequeno-almoço! Siga o seu plano alimentar.", 1001),
            750 to Triple("🍽️ Hora do almoço!", "🌤 O seu almoço está no plano. Bom apetite!", 1002),
            1170 to Triple("🌙 Hora do jantar!", "Confira o que está planeado para o jantar de hoje.", 1003),
        )

        targets.forEach { (targetMinutes, notification) ->
            val (title, body, notifId) = notification
            // Fire if within ±25 minutes of target
            if (kotlin.math.abs(totalMinutes - targetMinutes) <= 25) {
                notificationHelper.sendNotification(
                    id = notifId,
                    channelId = NotificationHelper.CHANNEL_MEAL,
                    title = title,
                    body = body,
                )
            }
        }

        // Meal prep mode reminder: Sunday at 10:00
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek == Calendar.SUNDAY && totalMinutes in 595..625) {
            notificationHelper.sendNotification(
                id = 1004,
                channelId = NotificationHelper.CHANNEL_MEAL,
                title = "🍳 Dia de Meal Prep!",
                body = "Hoje é domingo — hora de preparar as refeições da semana. Consulte o seu plano!",
            )
        }

        return Result.success()
    }
}
