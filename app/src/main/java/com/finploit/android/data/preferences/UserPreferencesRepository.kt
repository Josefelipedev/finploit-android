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
        val KEY_DIET_MODE = stringPreferencesKey("diet_mode")
        val KEY_CACHED_PLAN = stringPreferencesKey("cached_active_plan")
        val KEY_SELECTED_SUPERMARKETS = stringPreferencesKey("selected_supermarkets")
        val KEY_HIDDEN_SUPERMARKETS = stringPreferencesKey("hidden_supermarkets")
        val KEY_FAVORITE_MEALS = stringPreferencesKey("meal_favorite_meals")
        val KEY_BREAKFAST_AT_WORK = stringPreferencesKey("meal_breakfast_at_work")
        val KEY_SHOPPING_SUPERMARKET_FILTER = stringPreferencesKey("meal_shopping_supermarket_filter")
        // Melhoria #7 — locked (pinned) meals: JSON list of keys "${planId}_${dow}_${type}"
        val KEY_LOCKED_MEALS = stringPreferencesKey("meal_locked_meals")
        // Melhoria #10 — free-text notes per meal: JSON object {key: note}
        val KEY_MEAL_NOTES = stringPreferencesKey("meal_notes")
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

    val dietMode: Flow<String> = dataStore.data.map { it[KEY_DIET_MODE] ?: "balanced" }
    suspend fun setDietMode(mode: String) { dataStore.edit { it[KEY_DIET_MODE] = mode } }

    suspend fun cachePlan(json: String) { dataStore.edit { it[KEY_CACHED_PLAN] = json } }
    suspend fun clearCachedPlan() { dataStore.edit { it.remove(KEY_CACHED_PLAN) } }
    val cachedPlan: Flow<String?> = dataStore.data.map { it[KEY_CACHED_PLAN] }

    // Grocery supermarket preferences
    val selectedSupermarkets: Flow<Set<String>> = dataStore.data.map { prefs ->
        prefs[KEY_SELECTED_SUPERMARKETS]
            ?.split(",")?.filter { it.isNotBlank() }?.toSet()
            ?: setOf("continente", "auchan", "pingodoce")
    }
    val hiddenSupermarkets: Flow<Set<String>> = dataStore.data.map { prefs ->
        prefs[KEY_HIDDEN_SUPERMARKETS]
            ?.split(",")?.filter { it.isNotBlank() }?.toSet()
            ?: emptySet()
    }
    suspend fun setSelectedSupermarkets(chains: Set<String>) {
        dataStore.edit { it[KEY_SELECTED_SUPERMARKETS] = chains.joinToString(",") }
    }
    suspend fun setHiddenSupermarkets(chains: Set<String>) {
        dataStore.edit { it[KEY_HIDDEN_SUPERMARKETS] = chains.joinToString(",") }
    }

    // Favorite meals diary: JSON list of keys "${planId}_${dayOfWeek}_${mealType}"
    val favoriteMeals: Flow<String> = dataStore.data.map { it[KEY_FAVORITE_MEALS] ?: "[]" }
    suspend fun setFavoriteMeals(json: String) { dataStore.edit { it[KEY_FAVORITE_MEALS] = json } }

    // Breakfast at work: JSON list of dayOfWeek integers
    val breakfastAtWork: Flow<String> = dataStore.data.map { it[KEY_BREAKFAST_AT_WORK] ?: "[]" }
    suspend fun setBreakfastAtWork(json: String) { dataStore.edit { it[KEY_BREAKFAST_AT_WORK] = json } }

    // Shopping supermarket filter: store name or empty = all
    val shoppingSupFilter: Flow<String> = dataStore.data.map { it[KEY_SHOPPING_SUPERMARKET_FILTER] ?: "" }
    suspend fun setShoppingSupFilter(store: String) { dataStore.edit { it[KEY_SHOPPING_SUPERMARKET_FILTER] = store } }

    // Locked (pinned) meals: JSON list of keys "${planId}_${dow}_${type}"
    val lockedMeals: Flow<String> = dataStore.data.map { it[KEY_LOCKED_MEALS] ?: "[]" }
    suspend fun setLockedMeals(json: String) { dataStore.edit { it[KEY_LOCKED_MEALS] = json } }

    // Free-text notes per meal: JSON object {key: note}
    val mealNotes: Flow<String> = dataStore.data.map { it[KEY_MEAL_NOTES] ?: "{}" }
    suspend fun setMealNotes(json: String) { dataStore.edit { it[KEY_MEAL_NOTES] = json } }
}
