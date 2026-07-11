package com.finploit.android.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.finploit.android.data.preferences.UserPreferencesRepository
import com.finploit.android.data.repository.BillsRepository
import com.finploit.android.data.repository.GoalRepository
import com.finploit.android.ui.theme.currencyConfigByCode
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val billsRepository: BillsRepository,
    private val goalRepository: GoalRepository,
    private val notificationHelper: NotificationHelper,
    private val preferencesRepository: UserPreferencesRepository,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        checkPendingBills()
        checkGoalProgress()
        return Result.success()
    }

    /**
     * Lembra das contas a pagar PENDENTES que vencem hoje/amanhã ou estão em atraso.
     * Usa as ocorrências (respeita status pago) e faz DEDUP via SharedPreferences:
     * cada conta é notificada UMA vez; ids de contas já pagas são limpos.
     */
    private suspend fun checkPendingBills() {
        val today = java.time.LocalDate.now()
        val month = "%04d-%02d".format(today.year, today.monthValue)
        val prefs = applicationContext.getSharedPreferences("bill_reminders", Context.MODE_PRIVATE)

        billsRepository.getBills(month).onSuccess { resp ->
            val pendingIds = resp.items.filter { it.status == "pending" }.map { it.id.toString() }.toSet()
            // Dedup: mantém só ids ainda pendentes (limpa pagos/descartados) e não re-notifica
            val notified = HashSet(prefs.getStringSet("notified", emptySet()) ?: emptySet())
            notified.retainAll(pendingIds)

            resp.items
                .filter { it.status == "pending" && it.type == "expense" }
                .forEach { item ->
                    val due = runCatching { java.time.LocalDate.parse(item.dueDate.take(10)) }.getOrNull()
                    val daysUntil = due?.let {
                        java.time.temporal.ChronoUnit.DAYS.between(today, it)
                    }
                    val inWindow = item.overdue || (daysUntil != null && daysUntil in 0L..1L)
                    val key = item.id.toString()
                    if (inWindow && !notified.contains(key)) {
                        val label = when {
                            item.overdue -> "em atraso"
                            daysUntil == 0L -> "vence hoje"
                            else -> "vence amanhã"
                        }
                        val value = currencyConfigByCode(item.currency).format(item.amount)
                        notificationHelper.sendNotification(
                            id = 10000 + item.id,
                            channelId = NotificationHelper.CHANNEL_RECURRING,
                            title = "Conta $label",
                            body = "${item.description}: $value",
                        )
                        notified.add(key)
                    }
                }
            prefs.edit().putStringSet("notified", notified).apply()
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
