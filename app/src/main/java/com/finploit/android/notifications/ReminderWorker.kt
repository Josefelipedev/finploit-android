package com.finploit.android.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.finploit.android.data.preferences.UserPreferencesRepository
import com.finploit.android.data.repository.GoalRepository
import com.finploit.android.data.repository.RecurringRepository
import com.finploit.android.ui.theme.currencyConfigByCode
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val recurringRepository: RecurringRepository,
    private val goalRepository: GoalRepository,
    private val notificationHelper: NotificationHelper,
    private val preferencesRepository: UserPreferencesRepository,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        checkRecurringBills()
        checkGoalProgress()
        return Result.success()
    }

    private suspend fun checkRecurringBills() {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        val tomorrow = if (today == Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH)) 1 else today + 1
        val currency = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

        recurringRepository.getAll()
            .onSuccess { transactions ->
                transactions
                    .filter { it.dueDay != null && (it.dueDay == today || it.dueDay == tomorrow) }
                    .forEachIndexed { index, tx ->
                        val dayLabel = if (tx.dueDay == today) "hoje" else "amanhã"
                        val typeLabel = if (tx.type == "expense") "Conta" else "Entrada"
                        notificationHelper.sendNotification(
                            id = 1000 + index,
                            channelId = NotificationHelper.CHANNEL_RECURRING,
                            title = "$typeLabel vence $dayLabel",
                            body = "${tx.description ?: tx.type}: ${currency.format(tx.amount)}",
                        )
                    }
            }
    }

    private suspend fun checkGoalProgress() {
        val currencyCode = preferencesRepository.currencyCode.first()
        val config = currencyConfigByCode(currencyCode)

        goalRepository.getGoals()
            .onSuccess { goals ->
                goals.forEachIndexed { index, goal ->
                    val current = goal.currentValue ?: 0.0
                    val target = goal.targetValue
                    val progress = if (target > 0) current / target else 0.0
                    when {
                        progress >= 1.0 -> notificationHelper.sendNotification(
                            id = 2000 + index,
                            channelId = NotificationHelper.CHANNEL_GOALS,
                            title = "🎯 Meta atingida!",
                            body = "Parabéns! Atingiste a meta \"${goal.name}\" de ${config.format(target)}",
                        )
                        progress >= 0.9 -> notificationHelper.sendNotification(
                            id = 2000 + index,
                            channelId = NotificationHelper.CHANNEL_GOALS,
                            title = "🎯 Quase lá!",
                            body = "A meta \"${goal.name}\" está a ${(progress * 100).toInt()}% — faltam ${config.format(target - current)}",
                        )
                    }
                }
            }
    }
}
