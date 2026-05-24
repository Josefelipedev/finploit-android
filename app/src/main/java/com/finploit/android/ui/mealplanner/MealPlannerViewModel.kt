package com.finploit.android.ui.mealplanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finploit.android.data.api.EnrichItem
import com.finploit.android.data.dto.AddShoppingItemRequest
import com.finploit.android.data.dto.EnrichedShoppingItemDto
import com.finploit.android.data.dto.MealDetailDto
import com.finploit.android.data.dto.MealPlanDayDto
import com.finploit.android.data.dto.MealPlanDto
import com.finploit.android.data.dto.MealShoppingItemDto
import com.finploit.android.data.dto.ScheduleItemDto
import com.finploit.android.data.dto.ShoppingItemImportDto
import com.finploit.android.data.dto.UserProfileRequest
import com.finploit.android.data.preferences.UserPreferencesRepository
import com.finploit.android.data.repository.FinanceRepository
import com.finploit.android.data.repository.GrocerySearchRepository
import com.finploit.android.data.repository.MealPlannerRepository
import com.finploit.android.data.repository.PantryRepository
import com.finploit.android.data.repository.ShoppingRepository
import com.finploit.android.ui.theme.currencyConfigByCode
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class BudgetPreset(val label: String, val description: String) {
    ECONOMY("Econômico", "~20% menos"),
    BALANCED("Equilibrado", "recomendado"),
    PREMIUM("Premium", "sem restrições"),
    FREE("Sem limite", "IA decide"),
}

// Budget amounts per currency code — only currencies present in the Prisma CurrencyType enum
private val BUDGET_AMOUNTS = mapOf(
    "BRL" to mapOf(BudgetPreset.ECONOMY to 80.0, BudgetPreset.BALANCED to 150.0, BudgetPreset.PREMIUM to 280.0),
    "EUR" to mapOf(BudgetPreset.ECONOMY to 25.0, BudgetPreset.BALANCED to 50.0, BudgetPreset.PREMIUM to 90.0),
    "USD" to mapOf(BudgetPreset.ECONOMY to 30.0, BudgetPreset.BALANCED to 60.0, BudgetPreset.PREMIUM to 100.0),
    "GBP" to mapOf(BudgetPreset.ECONOMY to 25.0, BudgetPreset.BALANCED to 50.0, BudgetPreset.PREMIUM to 85.0),
    "JPY" to mapOf(BudgetPreset.ECONOMY to 4000.0, BudgetPreset.BALANCED to 8000.0, BudgetPreset.PREMIUM to 15000.0),
)

fun BudgetPreset.amountForCurrency(currencyCode: String): Double? =
    BUDGET_AMOUNTS[currencyCode]?.get(this) ?: BUDGET_AMOUNTS["BRL"]?.get(this)

data class MealPlannerUiState(
    val isLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val isSavingSchedule: Boolean = false,
    val plan: MealPlanDto? = null,
    val allPlans: List<MealPlanDto> = emptyList(),
    val isLoadingHistory: Boolean = false,
    val schedule: List<ScheduleItemDto> = emptyList(),
    val error: String? = null,
    val tab: MealTab = MealTab.PLAN,
    val budget: BudgetPreset = BudgetPreset.BALANCED,
    val snackbarMessage: String? = null,
    val selectedDay: MealPlanDayDto? = null,
    val selectedDayScheduleType: String? = null,
    val selectedDayLunchAtWork: Boolean = false,
    val selectedDayDinnerAtWork: Boolean = false,
    val isExportingToShoppingList: Boolean = false,
    val isDeletingPlan: Int? = null,
    val isClearingHistory: Boolean = false,
    val enrichedItems: Map<String, EnrichedShoppingItemDto> = emptyMap(),
    val isEnrichingPrices: Boolean = false,
    val enrichedAt: Long? = null,
    val isResettingShopping: Boolean = false,
    val shoppingFilter: ShoppingFilter = ShoppingFilter.PENDING,
    val collapsedCategories: Set<String> = emptySet(),
    val pantryItemCount: Int = 0,
    // Physical profile
    val profileHeight: String = "",
    val profileWeight: String = "",
    val profileActivityLevel: String = "moderate",
    val isSavingProfile: Boolean = false,
    // Dietary preferences & meal prep
    val dietaryPreferences: Set<String> = emptySet(),
    val mealPrepMode: Boolean = false,
    val dietMode: DietMode = DietMode.BALANCED,
    // Eaten meals diary: key = "${planId}_${dayOfWeek}_${mealType}"
    val eatenMeals: Set<String> = emptySet(),
    // Meal ratings: key = same, value 1=thumbsUp / -1=thumbsDown
    val mealRatings: Map<String, Int> = emptyMap(),
    // Prep time filter: null=all, 15=≤15min, 30=≤30min
    val prepTimeFilter: Int? = null,
    // Pantry add suggestion shown when an item is marked purchased
    val pantryAddSuggestion: MealShoppingItemDto? = null,
    // Substitution in progress: key = "${dayId}_${mealType}"
    val isSubstituting: String? = null,
    // Offline indicator
    val isOffline: Boolean = false,
    // Favorite meals: key = "${planId}_${dayOfWeek}_${mealType}"
    val favoriteMeals: Set<String> = emptySet(),
    // Selected supermarket filter in shopping tab (null = show all)
    val shoppingSupFilter: String? = null,
    // Breakfast at work per day (set of dayOfWeek 0-6)
    val breakfastAtWork: Set<Int> = emptySet(),
    // Computed TDEE (kcal/day) from physical profile
    val tdee: Int? = null,
    // Melhoria #7 — locked (pinned) meals: key = "${planId}_${dayOfWeek}_${mealType}"
    val lockedMeals: Set<String> = emptySet(),
    // Melhoria #10 — free-text notes: key = same scheme, value = note text
    val mealNotes: Map<String, String> = emptyMap(),
)

enum class MealTab { PLAN, SHOPPING, SCHEDULE, HISTORY }
enum class ShoppingFilter { ALL, PENDING, PURCHASED }

enum class DietMode(val label: String, val emoji: String, val description: String) {
    BALANCED("Equilibrado", "🥗", "Variado e saudável"),
    HIGH_PROTEIN("Alto Proteico", "💪", "Ideal para musculação"),
    LOW_CARB("Low Carb", "🥑", "Reduz carboidratos"),
    MEDITERRANEAN("Mediterrânico", "🫒", "Peixe, oliveiras, vegetais"),
    QUICK_MEALS("Rápido", "⚡", "Refeições em ≤ 20 min"),
    FAMILY("Familiar", "👨‍👩‍👧", "Agrada crianças e adultos"),
    GOURMET("Gourmet", "👨‍🍳", "Receitas elaboradas"),
    BUDGET_MAX("Super Económico", "💰", "Custo mínimo possível"),
}

@HiltViewModel
class MealPlannerViewModel @Inject constructor(
    private val repository: MealPlannerRepository,
    private val shoppingRepository: ShoppingRepository,
    private val grocerySearchRepository: GrocerySearchRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val pantryRepository: PantryRepository,
    private val financeRepository: FinanceRepository,
) : ViewModel() {

    private val gson = Gson()

    val currencyCode: StateFlow<String> = preferencesRepository.currencyCode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "BRL")

    private val _uiState = MutableStateFlow(MealPlannerUiState(isLoading = true))
    val uiState: StateFlow<MealPlannerUiState> = _uiState.asStateFlow()

    init {
        // Restore cached enriched prices so they survive bottom-nav ViewModel recreation
        val cached = grocerySearchRepository.getCachedEnrichedItems()
        if (cached.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(enrichedItems = cached)
        }
        // Reads DataStore preferences (budget, diary, ratings) independently of load().
        // Fields touched here (budget, eatenMeals, mealRatings, prepTimeFilter, mealPrepMode) are
        // disjoint from those set by load() (plan, schedule, profile), so there is no race condition.
        viewModelScope.launch {
            val savedBudget = preferencesRepository.budgetPreset.first()
            val preset = BudgetPreset.entries.find { it.name == savedBudget } ?: BudgetPreset.BALANCED
            val eatenMealsJson = preferencesRepository.eatenMeals.first()
            val ratingsJson = preferencesRepository.mealRatings.first()
            val prepTimeStr = preferencesRepository.prepTimeFilter.first()
            val prepMode = preferencesRepository.mealPrepMode.first()
            val savedDietMode = preferencesRepository.dietMode.first()
            val favoriteMealsJson = preferencesRepository.favoriteMeals.first()
            val breakfastAtWorkJson = preferencesRepository.breakfastAtWork.first()
            val supFilter = preferencesRepository.shoppingSupFilter.first()
            val lockedMealsJson = preferencesRepository.lockedMeals.first()
            val mealNotesJson = preferencesRepository.mealNotes.first()
            val eatenSet: Set<String> = try {
                val type = object : TypeToken<List<String>>() {}.type
                gson.fromJson<List<String>>(eatenMealsJson, type)?.toSet() ?: emptySet()
            } catch (_: Exception) { emptySet() }
            val ratingsMap: Map<String, Int> = try {
                val type = object : TypeToken<Map<String, Int>>() {}.type
                gson.fromJson<Map<String, Int>>(ratingsJson, type) ?: emptyMap()
            } catch (_: Exception) { emptyMap() }
            val favoriteSet: Set<String> = try {
                val type = object : TypeToken<List<String>>() {}.type
                gson.fromJson<List<String>>(favoriteMealsJson, type)?.toSet() ?: emptySet()
            } catch (_: Exception) { emptySet() }
            val breakfastSet: Set<Int> = try {
                val type = object : TypeToken<List<Int>>() {}.type
                gson.fromJson<List<Int>>(breakfastAtWorkJson, type)?.toSet() ?: emptySet()
            } catch (_: Exception) { emptySet() }
            val lockedSet: Set<String> = try {
                val type = object : TypeToken<List<String>>() {}.type
                gson.fromJson<List<String>>(lockedMealsJson, type)?.toSet() ?: emptySet()
            } catch (_: Exception) { emptySet() }
            val notesMap: Map<String, String> = try {
                val type = object : TypeToken<Map<String, String>>() {}.type
                gson.fromJson<Map<String, String>>(mealNotesJson, type) ?: emptyMap()
            } catch (_: Exception) { emptyMap() }
            val prepTimeFilter = prepTimeStr.toIntOrNull()
            val dietModeEnum = DietMode.entries.find { it.name == savedDietMode.uppercase() } ?: DietMode.BALANCED
            _uiState.value = _uiState.value.copy(
                budget = preset,
                eatenMeals = eatenSet,
                mealRatings = ratingsMap,
                prepTimeFilter = prepTimeFilter,
                mealPrepMode = prepMode,
                dietMode = dietModeEnum,
                favoriteMeals = favoriteSet,
                breakfastAtWork = breakfastSet,
                shoppingSupFilter = supFilter.ifBlank { null },
                lockedMeals = lockedSet,
                mealNotes = notesMap,
            )
        }
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, isOffline = false)
            val planDeferred = async { repository.getActivePlan() }
            val scheduleDeferred = async { repository.getSchedule() }
            val profileDeferred = async { repository.getProfile() }
            val pantryDeferred = async { pantryRepository.getAll() }
            val planResult = planDeferred.await()
            val scheduleResult = scheduleDeferred.await()
            val profileResult = profileDeferred.await()
            val pantryResult = pantryDeferred.await()
            val profile = profileResult.getOrNull()
            // Recompute TDEE whenever profile loads
            val tdee = profile?.let { p ->
                val h = p.height?.toFloat() ?: 0f
                val w = p.weight?.toFloat() ?: 0f
                if (h > 0 && w > 0) {
                    val bmr = (10 * w + 6.25f * h - 155).toInt()
                    val mult = mapOf("sedentary" to 1.2f, "light" to 1.375f, "moderate" to 1.55f, "active" to 1.725f, "very_active" to 1.9f)
                    (bmr * (mult[p.activityLevel] ?: 1.55f)).toInt()
                } else null
            }

            val fetchedPlan = planResult.getOrNull()
            val activePlan: MealPlanDto?
            val isOffline: Boolean
            if (planResult.isSuccess) {
                activePlan = fetchedPlan
                isOffline = false
                // Cache the plan for offline use
                fetchedPlan?.let { plan ->
                    launch { preferencesRepository.cachePlan(gson.toJson(plan)) }
                }
            } else {
                // Network failed — try offline cache
                val cachedJson = preferencesRepository.cachedPlan.first()
                activePlan = cachedJson?.let {
                    try { gson.fromJson(it, MealPlanDto::class.java) } catch (_: Exception) { null }
                }
                isOffline = activePlan != null
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                plan = activePlan,
                schedule = scheduleResult.getOrElse { emptyList() },
                error = if (!planResult.isSuccess && activePlan == null) planResult.exceptionOrNull()?.message else null,
                isOffline = isOffline,
                profileHeight = profile?.height?.let { "%.0f".format(it) } ?: "",
                profileWeight = profile?.weight?.let { "%.1f".format(it) } ?: "",
                profileActivityLevel = profile?.activityLevel ?: "moderate",
                dietaryPreferences = profile?.dietaryPreferences?.toSet() ?: emptySet(),
                pantryItemCount = pantryResult.getOrNull()?.size ?: 0,
                tdee = tdee,
            )
        }
    }

    fun loadHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingHistory = true)
            repository.getAllPlans()
                .onSuccess { plans ->
                    _uiState.value = _uiState.value.copy(isLoadingHistory = false, allPlans = plans)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isLoadingHistory = false)
                }
        }
    }

    fun selectBudget(preset: BudgetPreset) {
        _uiState.value = _uiState.value.copy(budget = preset)
        viewModelScope.launch { preferencesRepository.setBudgetPreset(preset.name) }
    }

    private fun extractBadMealNames(): List<String> {
        val plan = _uiState.value.plan ?: return emptyList()
        val badKeys = _uiState.value.mealRatings.filter { it.value == -1 }.keys
        val result = mutableListOf<String>()
        for (key in badKeys) {
            val parts = key.split("_")
            if (parts.size < 3) continue
            val dow = parts[1].toIntOrNull() ?: continue
            val mealType = parts[2]
            val day = plan.days.find { it.dayOfWeek == dow } ?: continue
            val mealJson = when (mealType) {
                "breakfast" -> day.breakfast
                "lunch" -> day.lunch
                "dinner" -> day.dinner
                else -> null
            } ?: continue
            try {
                val detail = gson.fromJson(mealJson, MealDetailDto::class.java)
                detail?.name?.let { result.add(it) }
            } catch (_: Exception) {}
        }
        return result
    }

    fun generatePlan() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGenerating = true, error = null)
            val code = currencyCode.value
            val cfg = currencyConfigByCode(code)
            val amount = _uiState.value.budget.amountForCurrency(code)
            val badMeals = extractBadMealNames().takeIf { it.isNotEmpty() }
            val favoriteMeals = extractFavoriteMealNames().takeIf { it.isNotEmpty() }
            repository.generatePlan(
                budget = amount,
                currencyCode = code,
                currencySymbol = cfg.symbol,
                currencyLocale = "${cfg.locale.language}_${cfg.locale.country}",
                mealPrepMode = _uiState.value.mealPrepMode,
                dietMode = _uiState.value.dietMode.name.lowercase(),
                badMeals = badMeals,
                favoriteMeals = favoriteMeals,
            )
                .onSuccess { plan ->
                    grocerySearchRepository.clearCachedEnrichedItems()
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        plan = plan,
                        enrichedItems = emptyMap(),
                        snackbarMessage = "Cardápio gerado com sucesso! 🥗",
                    )
                }
                .onFailure { e ->
                    val friendlyMsg = when {
                        e is retrofit2.HttpException && e.code() == 503 ->
                            "Serviço de IA temporariamente indisponível. Tente novamente em alguns minutos."
                        e is retrofit2.HttpException && e.code() >= 500 ->
                            "Erro no servidor ao gerar plano. Tente novamente."
                        e is java.io.IOException ->
                            "Sem ligação à internet. Verifique a sua rede e tente novamente."
                        e.message?.contains("was null but response body") == true ->
                            "Erro ao processar resposta do servidor. Tente novamente."
                        else -> e.message ?: "Erro ao gerar plano"
                    }
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        error = friendlyMsg,
                    )
                }
        }
    }

    fun toggleItem(itemId: Int) {
        viewModelScope.launch {
            repository.toggleItem(itemId).onSuccess { updated ->
                val plan = _uiState.value.plan ?: return@onSuccess
                val newList = plan.shoppingList?.let { list ->
                    list.copy(items = list.items.map { if (it.id == itemId) updated else it })
                }
                val suggestion = if (updated.purchased) updated else null
                _uiState.value = _uiState.value.copy(
                    plan = plan.copy(shoppingList = newList),
                    pantryAddSuggestion = suggestion,
                )
                // Melhoria #9: auto-create finance transaction when item is purchased with actual price
                if (updated.purchased) {
                    val price = updated.actualPrice ?: updated.estimatedPrice
                    if (price != null && price > 0.0) {
                        launch {
                            financeRepository.createTransaction(
                                type = "expense",
                                amount = price,
                                description = "🛒 ${updated.name} — Plano Alimentar",
                                categoryId = null,
                            )
                        }
                    }
                }
            }
        }
    }

    fun dismissPantrySuggestion() {
        _uiState.value = _uiState.value.copy(pantryAddSuggestion = null)
    }

    fun addSuggestionToPantry() {
        val item = _uiState.value.pantryAddSuggestion ?: return
        _uiState.value = _uiState.value.copy(pantryAddSuggestion = null)
        viewModelScope.launch {
            pantryRepository.upsert(
                com.finploit.android.data.dto.UpsertPantryItemRequest(
                    name = item.name,
                    quantity = item.quantity,
                    unit = item.unit,
                    category = item.category,
                )
            )
            _uiState.value = _uiState.value.copy(
                snackbarMessage = "✅ ${item.name} adicionado à despensa!",
                pantryItemCount = _uiState.value.pantryItemCount + 1,
            )
        }
    }

    fun saveSchedule(schedule: List<ScheduleItemDto>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSavingSchedule = true)
            repository.saveSchedule(schedule)
                .onSuccess { saved ->
                    _uiState.value = _uiState.value.copy(
                        schedule = saved,
                        isSavingSchedule = false,
                        snackbarMessage = "Agenda salva com sucesso! ✅",
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isSavingSchedule = false,
                        snackbarMessage = "Erro ao salvar agenda. Tente novamente.",
                    )
                }
        }
    }

    fun setTab(tab: MealTab) {
        _uiState.value = _uiState.value.copy(tab = tab)
        if (tab == MealTab.HISTORY) loadHistory()
    }

    fun selectDay(day: MealPlanDayDto, scheduleItem: ScheduleItemDto?) {
        _uiState.value = _uiState.value.copy(
            selectedDay = day,
            selectedDayScheduleType = scheduleItem?.dayType,
            selectedDayLunchAtWork = scheduleItem?.lunchAtWork ?: false,
            selectedDayDinnerAtWork = scheduleItem?.dinnerAtWork ?: false,
        )
    }

    fun clearSelectedDay() {
        _uiState.value = _uiState.value.copy(
            selectedDay = null,
            selectedDayScheduleType = null,
            selectedDayLunchAtWork = false,
            selectedDayDinnerAtWork = false,
        )
    }

    fun exportToShoppingList() {
        val items = _uiState.value.plan?.shoppingList?.items
            ?.filter { !it.purchased }
            ?: return

        if (items.isEmpty()) {
            _uiState.value = _uiState.value.copy(snackbarMessage = "Todos os itens já foram comprados.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExportingToShoppingList = true)
            val weekStart = _uiState.value.plan?.weekStart?.take(10) ?: "semana"
            val listName = "🥗 Plano Alimentar - $weekStart"
            val importItems = items.map { i ->
                ShoppingItemImportDto(
                    name = i.name,
                    quantity = i.quantity,
                    unit = i.unit,
                    price = i.estimatedPrice,
                )
            }
            shoppingRepository.createListWithItems(listName, importItems)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isExportingToShoppingList = false,
                        snackbarMessage = "✅ ${items.size} itens exportados para a lista \"$listName\"!",
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isExportingToShoppingList = false,
                        snackbarMessage = "Erro ao exportar lista. Tente novamente.",
                    )
                }
        }
    }

    fun setProfileHeight(v: String) { _uiState.value = _uiState.value.copy(profileHeight = v.filter { it.isDigit() }.take(3)) }
    fun setProfileWeight(v: String) { _uiState.value = _uiState.value.copy(profileWeight = v.filter { it.isDigit() || it == '.' }.take(5)) }
    fun setProfileActivityLevel(v: String) { _uiState.value = _uiState.value.copy(profileActivityLevel = v) }

    fun saveProfile() {
        val s = _uiState.value
        val h = s.profileHeight.toFloatOrNull()?.takeIf { it > 0f }
        val w = s.profileWeight.toFloatOrNull()?.takeIf { it > 0f }
        if (h == null && w == null) {
            _uiState.value = s.copy(snackbarMessage = "Preencha pelo menos a altura ou o peso com valor válido.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSavingProfile = true)
            repository.saveProfile(UserProfileRequest(height = h, weight = w, activityLevel = s.profileActivityLevel, dietaryPreferences = s.dietaryPreferences.toList()))
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isSavingProfile = false,
                        snackbarMessage = "✅ Perfil guardado! Gere um novo cardápio para aplicar.",
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isSavingProfile = false,
                        snackbarMessage = "Erro ao guardar perfil. Tente novamente.",
                    )
                }
        }
    }

    fun toggleDietaryPreference(pref: String) {
        val current = _uiState.value.dietaryPreferences
        _uiState.value = _uiState.value.copy(
            dietaryPreferences = if (pref in current) current - pref else current + pref,
        )
    }

    fun setMealPrepMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(mealPrepMode = enabled)
        viewModelScope.launch { preferencesRepository.setMealPrepMode(enabled) }
    }

    fun setDietMode(mode: DietMode) {
        _uiState.value = _uiState.value.copy(dietMode = mode)
        viewModelScope.launch { preferencesRepository.setDietMode(mode.name.lowercase()) }
    }

    fun toggleEatenMeal(key: String) {
        val current = _uiState.value.eatenMeals
        val updated = if (key in current) current - key else current + key
        _uiState.value = _uiState.value.copy(eatenMeals = updated)
        viewModelScope.launch {
            preferencesRepository.setEatenMeals(gson.toJson(updated.toList()))
        }
    }

    fun rateMeal(key: String, rating: Int) {
        val updated = _uiState.value.mealRatings + (key to rating)
        _uiState.value = _uiState.value.copy(mealRatings = updated)
        viewModelScope.launch {
            preferencesRepository.setMealRatings(gson.toJson(updated))
        }
    }

    // Melhoria #3: toggle ⭐ favorite meal
    fun toggleFavoriteMeal(key: String) {
        val current = _uiState.value.favoriteMeals
        val updated = if (key in current) current - key else current + key
        _uiState.value = _uiState.value.copy(favoriteMeals = updated)
        viewModelScope.launch {
            preferencesRepository.setFavoriteMeals(gson.toJson(updated.toList()))
        }
    }

    private fun extractFavoriteMealNames(): List<String> {
        val plan = _uiState.value.plan ?: return emptyList()
        val favKeys = _uiState.value.favoriteMeals
        val result = mutableListOf<String>()
        for (key in favKeys) {
            val parts = key.split("_")
            if (parts.size < 3) continue
            val dow = parts[1].toIntOrNull() ?: continue
            val mealType = parts[2]
            val day = plan.days.find { it.dayOfWeek == dow } ?: continue
            val mealJson = when (mealType) {
                "breakfast" -> day.breakfast
                "lunch" -> day.lunch
                "dinner" -> day.dinner
                else -> null
            } ?: continue
            try {
                val detail = gson.fromJson(mealJson, MealDetailDto::class.java)
                detail?.name?.let { result.add(it) }
            } catch (_: Exception) {}
        }
        return result
    }

    // Melhoria #5: shopping supermarket filter
    fun setShoppingSupFilter(store: String?) {
        _uiState.value = _uiState.value.copy(shoppingSupFilter = store)
        viewModelScope.launch {
            preferencesRepository.setShoppingSupFilter(store ?: "")
        }
    }

    // Melhoria #7: breakfast at work per day
    fun toggleBreakfastAtWork(dayOfWeek: Int) {
        val current = _uiState.value.breakfastAtWork
        val updated = if (dayOfWeek in current) current - dayOfWeek else current + dayOfWeek
        _uiState.value = _uiState.value.copy(breakfastAtWork = updated)
        viewModelScope.launch {
            preferencesRepository.setBreakfastAtWork(gson.toJson(updated.toList()))
        }
    }

    fun setPrepTimeFilter(minutes: Int?) {
        _uiState.value = _uiState.value.copy(prepTimeFilter = minutes)
        viewModelScope.launch {
            preferencesRepository.setPrepTimeFilter(minutes?.toString() ?: "")
        }
    }

    fun deletePlan(planId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeletingPlan = planId)
            repository.deletePlan(planId)
                .onSuccess {
                    val prefix = "${planId}_"
                    val newEaten = _uiState.value.eatenMeals.filterNot { it.startsWith(prefix) }.toSet()
                    val newRatings = _uiState.value.mealRatings.filterKeys { !it.startsWith(prefix) }
                    val newFavorites = _uiState.value.favoriteMeals.filterNot { it.startsWith(prefix) }.toSet()
                    val newLocked = _uiState.value.lockedMeals.filterNot { it.startsWith(prefix) }.toSet()
                    val newNotes = _uiState.value.mealNotes.filterKeys { !it.startsWith(prefix) }
                    preferencesRepository.setEatenMeals(gson.toJson(newEaten.toList()))
                    preferencesRepository.setMealRatings(gson.toJson(newRatings))
                    preferencesRepository.setFavoriteMeals(gson.toJson(newFavorites.toList()))
                    preferencesRepository.setLockedMeals(gson.toJson(newLocked.toList()))
                    preferencesRepository.setMealNotes(gson.toJson(newNotes))
                    _uiState.value = _uiState.value.copy(
                        isDeletingPlan = null,
                        allPlans = _uiState.value.allPlans.filter { it.id != planId },
                        plan = if (_uiState.value.plan?.id == planId) null else _uiState.value.plan,
                        eatenMeals = newEaten,
                        mealRatings = newRatings,
                        favoriteMeals = newFavorites,
                        lockedMeals = newLocked,
                        mealNotes = newNotes,
                        snackbarMessage = "Plano apagado.",
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isDeletingPlan = null,
                        snackbarMessage = "Erro ao apagar plano.",
                    )
                }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isClearingHistory = true)
            repository.clearHistory()
                .onSuccess {
                    preferencesRepository.setEatenMeals("[]")
                    preferencesRepository.setMealRatings("{}")
                    preferencesRepository.setFavoriteMeals("[]")
                    preferencesRepository.setLockedMeals("[]")
                    preferencesRepository.setMealNotes("{}")
                    _uiState.value = _uiState.value.copy(
                        isClearingHistory = false,
                        allPlans = emptyList(),
                        plan = null,
                        eatenMeals = emptySet(),
                        mealRatings = emptyMap(),
                        favoriteMeals = emptySet(),
                        lockedMeals = emptySet(),
                        mealNotes = emptyMap(),
                        snackbarMessage = "Histórico apagado com sucesso.",
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isClearingHistory = false,
                        snackbarMessage = "Erro ao limpar histórico.",
                    )
                }
        }
    }

    fun enrichMealPrices() {
        val items = _uiState.value.plan?.shoppingList?.items
            ?.filter { !it.purchased }
            ?: return

        if (items.isEmpty()) {
            _uiState.value = _uiState.value.copy(snackbarMessage = "Todos os itens já foram comprados.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isEnrichingPrices = true)
            val postalCode = preferencesRepository.postalCode.first()
            val enrichItems = items.map {
                EnrichItem(it.name, it.quantity, it.unit, it.estimatedPrice ?: 0.0)
            }
            grocerySearchRepository.enrichItems(enrichItems, listOf("continente", "auchan", "pingodoce"), postalCode)
                .onSuccess { enriched ->
                    val map = enriched.associateBy { it.name.trim().lowercase() }
                    val found = enriched.count { it.bestPrice != null }
                    _uiState.value = _uiState.value.copy(
                        isEnrichingPrices = false,
                        enrichedItems = map,
                        enrichedAt = System.currentTimeMillis(),
                        snackbarMessage = if (found > 0) "💶 Preços actualizados para $found itens" else "Nenhum preço encontrado no scraper.",
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isEnrichingPrices = false,
                        snackbarMessage = "Erro ao buscar preços. Verifique a ligação.",
                    )
                }
        }
    }

    fun setShoppingFilter(filter: ShoppingFilter) {
        _uiState.value = _uiState.value.copy(shoppingFilter = filter)
    }

    fun toggleCategoryCollapse(category: String) {
        val current = _uiState.value.collapsedCategories
        _uiState.value = _uiState.value.copy(
            collapsedCategories = if (category in current) current - category else current + category,
        )
    }

    fun buyAllInCategory(category: String) {
        val ids = _uiState.value.plan?.shoppingList?.items
            ?.filter { !it.purchased && it.category == category }
            ?.map { it.id } ?: return
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.batchToggleItems(ids).onSuccess {
                val plan = _uiState.value.plan ?: return@onSuccess
                val newItems = plan.shoppingList?.items?.map { item ->
                    if (item.id in ids) item.copy(purchased = true) else item
                }
                val newList = plan.shoppingList?.copy(items = newItems ?: emptyList())
                _uiState.value = _uiState.value.copy(plan = plan.copy(shoppingList = newList))
            }
        }
    }

    fun resetShoppingList() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isResettingShopping = true)
            repository.resetShoppingList()
                .onSuccess { plan ->
                    _uiState.value = _uiState.value.copy(
                        isResettingShopping = false,
                        plan = plan,
                        enrichedItems = emptyMap(),
                        enrichedAt = null,
                        snackbarMessage = "Lista reiniciada — pronto para nova ida ao supermercado!",
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isResettingShopping = false)
                }
        }
    }

    fun addCustomShoppingItem(name: String, quantity: Double, unit: String, category: String?, estimatedPrice: Double?) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addShoppingItem(AddShoppingItemRequest(name.trim(), quantity, unit.ifBlank { "un" }, category?.ifBlank { null }, estimatedPrice))
                .onSuccess { newItem ->
                    val plan = _uiState.value.plan ?: return@onSuccess
                    val newList = plan.shoppingList?.let { list ->
                        list.copy(items = list.items + newItem)
                    }
                    _uiState.value = _uiState.value.copy(
                        plan = plan.copy(shoppingList = newList),
                        snackbarMessage = "✅ ${newItem.name} adicionado à lista!",
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(snackbarMessage = "Erro ao adicionar item.")
                }
        }
    }

    fun updateActualPrice(itemId: Int, price: Double?) {
        viewModelScope.launch {
            repository.updateActualPrice(itemId, price).onSuccess { updated ->
                val plan = _uiState.value.plan ?: return@onSuccess
                val newList = plan.shoppingList?.let { list ->
                    list.copy(items = list.items.map { if (it.id == itemId) updated else it })
                }
                _uiState.value = _uiState.value.copy(plan = plan.copy(shoppingList = newList))
            }
        }
    }

    // Melhoria #7 — toggle pin/lock on a meal
    fun toggleLockedMeal(key: String) {
        val current = _uiState.value.lockedMeals
        val updated = if (key in current) current - key else current + key
        _uiState.value = _uiState.value.copy(lockedMeals = updated)
        viewModelScope.launch { preferencesRepository.setLockedMeals(gson.toJson(updated.toList())) }
    }

    // Melhoria #10 — set or clear a free-text note for a meal
    fun setMealNote(key: String, note: String) {
        val current = _uiState.value.mealNotes
        val updated = if (note.isBlank()) current - key else current + (key to note.trim())
        _uiState.value = _uiState.value.copy(mealNotes = updated)
        viewModelScope.launch { preferencesRepository.setMealNotes(gson.toJson(updated)) }
    }

    // Melhoria #4 — substituteMeal now accepts optional user preferences
    fun substituteMeal(dayId: Int, mealType: String, preferences: String? = null) {
        val key = "${dayId}_${mealType}"
        _uiState.value = _uiState.value.copy(isSubstituting = key)
        viewModelScope.launch {
            repository.substituteMeal(dayId, mealType, preferences)
                .onSuccess { response ->
                    val plan = _uiState.value.plan ?: return@onSuccess
                    val newDays = plan.days.map { day ->
                        if (day.id == dayId) response.day else day
                    }
                    _uiState.value = _uiState.value.copy(
                        plan = plan.copy(days = newDays),
                        selectedDay = if (_uiState.value.selectedDay?.id == dayId) response.day else _uiState.value.selectedDay,
                        isSubstituting = null,
                        snackbarMessage = "🔄 Refeição substituída: ${response.meal.name}",
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isSubstituting = null,
                        snackbarMessage = "Erro ao substituir refeição. Tente novamente.",
                    )
                }
        }
    }

    fun loadPlanDaysForHistory(planId: Int) {
        viewModelScope.launch {
            repository.getPlanDays(planId).onSuccess { days ->
                val allPlans = _uiState.value.allPlans.map { p ->
                    if (p.id == planId) p.copy(days = days) else p
                }
                _uiState.value = _uiState.value.copy(allPlans = allPlans)
            }
        }
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
