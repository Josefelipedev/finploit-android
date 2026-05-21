package com.finploit.android.ui.mealplanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finploit.android.data.api.EnrichItem
import com.finploit.android.data.dto.EnrichedShoppingItemDto
import com.finploit.android.data.dto.MealPlanDayDto
import com.finploit.android.data.dto.MealPlanDto
import com.finploit.android.data.dto.ScheduleItemDto
import com.finploit.android.data.dto.ShoppingItemImportDto
import com.finploit.android.data.dto.UserProfileRequest
import com.finploit.android.data.preferences.UserPreferencesRepository
import com.finploit.android.data.repository.GrocerySearchRepository
import com.finploit.android.data.repository.MealPlannerRepository
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
    val shoppingFilter: ShoppingFilter = ShoppingFilter.PENDING,
    val collapsedCategories: Set<String> = emptySet(),
    // Physical profile
    val profileHeight: String = "",
    val profileWeight: String = "",
    val profileActivityLevel: String = "moderate",
    val isSavingProfile: Boolean = false,
    // Dietary preferences & meal prep
    val dietaryPreferences: Set<String> = emptySet(),
    val mealPrepMode: Boolean = false,
    // Eaten meals diary: key = "${planId}_${dayOfWeek}_${mealType}"
    val eatenMeals: Set<String> = emptySet(),
    // Meal ratings: key = same, value 1=thumbsUp / -1=thumbsDown
    val mealRatings: Map<String, Int> = emptyMap(),
    // Prep time filter: null=all, 15=≤15min, 30=≤30min
    val prepTimeFilter: Int? = null,
)

enum class MealTab { PLAN, SHOPPING, SCHEDULE, HISTORY }
enum class ShoppingFilter { ALL, PENDING, PURCHASED }

@HiltViewModel
class MealPlannerViewModel @Inject constructor(
    private val repository: MealPlannerRepository,
    private val shoppingRepository: ShoppingRepository,
    private val grocerySearchRepository: GrocerySearchRepository,
    private val preferencesRepository: UserPreferencesRepository,
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
            val eatenSet: Set<String> = try {
                val type = object : TypeToken<List<String>>() {}.type
                gson.fromJson<List<String>>(eatenMealsJson, type)?.toSet() ?: emptySet()
            } catch (_: Exception) { emptySet() }
            val ratingsMap: Map<String, Int> = try {
                val type = object : TypeToken<Map<String, Int>>() {}.type
                gson.fromJson<Map<String, Int>>(ratingsJson, type) ?: emptyMap()
            } catch (_: Exception) { emptyMap() }
            val prepTimeFilter = prepTimeStr.toIntOrNull()
            _uiState.value = _uiState.value.copy(
                budget = preset,
                eatenMeals = eatenSet,
                mealRatings = ratingsMap,
                prepTimeFilter = prepTimeFilter,
                mealPrepMode = prepMode,
            )
        }
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val planDeferred = async { repository.getActivePlan() }
            val scheduleDeferred = async { repository.getSchedule() }
            val profileDeferred = async { repository.getProfile() }
            val planResult = planDeferred.await()
            val scheduleResult = scheduleDeferred.await()
            val profileResult = profileDeferred.await()
            val profile = profileResult.getOrNull()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                plan = planResult.getOrNull(),
                schedule = scheduleResult.getOrElse { emptyList() },
                error = planResult.exceptionOrNull()?.message,
                profileHeight = profile?.height?.let { "%.0f".format(it) } ?: "",
                profileWeight = profile?.weight?.let { "%.1f".format(it) } ?: "",
                profileActivityLevel = profile?.activityLevel ?: "moderate",
                dietaryPreferences = profile?.dietaryPreferences?.toSet() ?: emptySet(),
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

    fun generatePlan() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGenerating = true, error = null)
            val code = currencyCode.value
            val cfg = currencyConfigByCode(code)
            val amount = _uiState.value.budget.amountForCurrency(code)
            repository.generatePlan(
                budget = amount,
                currencyCode = code,
                currencySymbol = cfg.symbol,
                currencyLocale = "${cfg.locale.language}_${cfg.locale.country}",
                mealPrepMode = _uiState.value.mealPrepMode,
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
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        error = e.message ?: "Erro ao gerar plano",
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
                _uiState.value = _uiState.value.copy(plan = plan.copy(shoppingList = newList))
            }
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
                    preferencesRepository.setEatenMeals(gson.toJson(newEaten.toList()))
                    preferencesRepository.setMealRatings(gson.toJson(newRatings))
                    _uiState.value = _uiState.value.copy(
                        isDeletingPlan = null,
                        allPlans = _uiState.value.allPlans.filter { it.id != planId },
                        plan = if (_uiState.value.plan?.id == planId) null else _uiState.value.plan,
                        eatenMeals = newEaten,
                        mealRatings = newRatings,
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
                    _uiState.value = _uiState.value.copy(
                        isClearingHistory = false,
                        allPlans = emptyList(),
                        plan = null,
                        eatenMeals = emptySet(),
                        mealRatings = emptyMap(),
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

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}
