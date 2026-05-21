package com.finploit.android.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    companion object {
        val KEY_CURRENCY_CODE = stringPreferencesKey("currency_code")
        val KEY_POSTAL_CODE = stringPreferencesKey("postal_code")
        val KEY_BUDGET_PRESET = stringPreferencesKey("budget_preset")
        val KEY_EATEN_MEALS = stringPreferencesKey("meal_eaten_meals")
        val KEY_MEAL_RATINGS = stringPreferencesKey("meal_ratings")
        val KEY_PREP_TIME_FILTER = stringPreferencesKey("prep_time_filter")
        val KEY_MEAL_PREP_MODE = stringPreferencesKey("meal_prep_mode")
    }

    val currencyCode: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_CURRENCY_CODE] ?: "BRL"
    }

    val postalCode: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_POSTAL_CODE] ?: "1000-001"
    }

    val budgetPreset: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_BUDGET_PRESET] ?: "BALANCED"
    }

    suspend fun setCurrencyCode(code: String) {
        dataStore.edit { it[KEY_CURRENCY_CODE] = code }
    }

    suspend fun setPostalCode(code: String) {
        dataStore.edit { it[KEY_POSTAL_CODE] = code }
    }

    suspend fun setBudgetPreset(preset: String) {
        dataStore.edit { it[KEY_BUDGET_PRESET] = preset }
    }

    val eatenMeals: Flow<String> = dataStore.data.map { it[KEY_EATEN_MEALS] ?: "[]" }
    val mealRatings: Flow<String> = dataStore.data.map { it[KEY_MEAL_RATINGS] ?: "{}" }
    val prepTimeFilter: Flow<String> = dataStore.data.map { it[KEY_PREP_TIME_FILTER] ?: "" }
    val mealPrepMode: Flow<Boolean> = dataStore.data.map { it[KEY_MEAL_PREP_MODE] == "true" }

    suspend fun setEatenMeals(json: String) { dataStore.edit { it[KEY_EATEN_MEALS] = json } }
    suspend fun setMealRatings(json: String) { dataStore.edit { it[KEY_MEAL_RATINGS] = json } }
    suspend fun setPrepTimeFilter(value: String) { dataStore.edit { it[KEY_PREP_TIME_FILTER] = value } }
    suspend fun setMealPrepMode(enabled: Boolean) { dataStore.edit { it[KEY_MEAL_PREP_MODE] = if (enabled) "true" else "false" } }
}
