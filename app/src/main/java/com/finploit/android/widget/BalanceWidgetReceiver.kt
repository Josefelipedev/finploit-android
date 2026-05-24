package com.finploit.android.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.finploit.android.BuildConfig
import com.finploit.android.R
import com.finploit.android.data.local.TokenStorage
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class BalanceWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { widgetId ->
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.balance_widget_layout)
        views.setTextViewText(R.id.widget_balance, "A carregar...")
        manager.updateAppWidget(widgetId, views)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = TokenStorage(context).getToken()
                if (token.isNullOrBlank()) {
                    withContext(Dispatchers.Main) {
                        views.setTextViewText(R.id.widget_balance, "Sem sessão")
                        manager.updateAppWidget(widgetId, views)
                    }
                    return@launch
                }
                val baseUrl = BuildConfig.API_BASE_URL
                val client = OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build()
                val request = Request.Builder()
                    .url("${baseUrl}finance/dashboard")
                    .addHeader("Authorization", "Bearer $token")
                    .build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                val data = json.optJSONObject("data") ?: json
                val balance = data.optDouble("totalBalance", 0.0)
                val formatted = "€%.2f".format(balance)
                withContext(Dispatchers.Main) {
                    views.setTextViewText(R.id.widget_balance, formatted)
                    manager.updateAppWidget(widgetId, views)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    views.setTextViewText(R.id.widget_balance, "Erro")
                    manager.updateAppWidget(widgetId, views)
                }
            }
        }
    }
}
