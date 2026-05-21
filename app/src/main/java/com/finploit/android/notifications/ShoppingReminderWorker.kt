package com.finploit.android.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.finploit.android.data.repository.MealPlannerRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.text.NumberFormat
import java.util.Locale

@HiltWorker
class ShoppingReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val mealPlannerRepository: MealPlannerRepository,
    private val notificationHelper: NotificationHelper,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val currency = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

        mealPlannerRepository.getActivePlan()
            .onSuccess { plan ->
                val items = plan?.shoppingList?.items ?: return@onSuccess
                val pending = items.filter { !it.purchased }
                if (pending.isEmpty()) return@onSuccess

                val total = pending.sumOf { it.estimatedPrice ?: 0.0 }
                val title = "🛒 Lista de compras da semana"
                val body = "${pending.size} ${if (pending.size == 1) "item pendente" else "itens pendentes"} — estimativa ${currency.format(total)}"

                notificationHelper.sendRecurringReminder(
                    id = 9999,
                    title = title,
                    body = body,
                )
            }

        return Result.success()
    }
}
