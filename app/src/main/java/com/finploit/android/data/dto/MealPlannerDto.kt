package com.finploit.android.data.dto

import com.google.gson.annotations.SerializedName

data class ScheduleItemDto(
    val id: Int = 0,
    val dayOfWeek: Int,
    val dayType: String = "WORK",
    val lunchAtWork: Boolean = false,
    val dinnerAtWork: Boolean = false,
)

data class SaveScheduleRequest(val schedule: List<ScheduleItemDto>)

data class MealDetailDto(
    val name: String,
    val description: String?,
    val ingredients: List<String>,
    val howToPrepare: List<String>,
    val calories: Int,
    val protein: Double?,
    val carbs: Double?,
    val fat: Double?,
    val fiber: Double?,
    val prepTime: String?,
    val mealType: String?,
    val tip: String?,
)

data class SnackDetailDto(
    val name: String,
    val description: String? = null,
    val ingredients: List<String> = emptyList(),
    val calories: Int? = null,
    val prepTime: String? = null,
    val cheaperAlternative: String? = null,
)

data class MealPlanDayDto(
    val id: Int,
    val dayOfWeek: Int,
    val date: String,
    val breakfast: String?,
    val lunch: String?,
    val dinner: String?,
    val snacks: String?,
    val calories: Int?,
)

data class MealShoppingItemDto(
    val id: Int,
    val name: String,
    val quantity: Double,
    val unit: String,
    val estimatedPrice: Double?,
    val actualPrice: Double? = null,
    val category: String?,
    val purchased: Boolean,
    val usedInDays: String? = null, // JSON string e.g. "[0,2,4]"
    val packageNote: String? = null,
)

data class MealShoppingListDto(
    val id: Int,
    val totalEstimate: Double?,
    val notified: Boolean,
    val items: List<MealShoppingItemDto> = emptyList(),
    /** Quando a lista foi dada por comprada e virou despesa (C4). */
    val closedAt: String? = null,
    /** A despesa que ela gerou. */
    val financeId: Int? = null,
    // History-only fields (items list is omitted in getAllPlans for performance)
    val totalItems: Int? = null,
    val purchasedCount: Int? = null,
    /**
     * O que vai comprado contra o orçamento da semana (F4). Enquanto a lista
     * está aberta conta só o que já foi marcado — é esse o número que responde
     * a "ainda posso pôr isto no carrinho?".
     */
    val budgetComparison: BudgetComparisonDto? = null,
)

/**
 * O orçado contra o real. `status` vale `no_budget`, `under`, `close` ou `over`.
 *
 * **`no_budget` não é orçamento zero.** O primeiro diz "ninguém combinou nada",
 * o segundo diz "não gastes nada" — e um ecrã que os confunde anuncia que se
 * estourou o orçamento a quem nunca definiu nenhum. Por isso `budget`, `delta`
 * e `usedPct` vêm nulos nesse caso.
 */
data class BudgetComparisonDto(
    val budget: Double? = null,
    val spent: Double = 0.0,
    val delta: Double? = null,
    val usedPct: Double? = null,
    val status: String = "no_budget",
)

fun MealShoppingItemDto.parsedUsedInDays(): List<Int> = try {
    com.google.gson.Gson().fromJson(usedInDays ?: "[]", Array<Int>::class.java)?.toList() ?: emptyList()
} catch (_: Exception) { emptyList() }

data class MealPlanDto(
    val id: Int,
    /**
     * Quem gerou o plano. O cardápio é do casal — um plano ativo de cada vez,
     * visto e usado pelos dois — mas cada um sai das preferências e da agenda
     * de quem carregou no botão.
     */
    val userId: Int? = null,
    val weekStart: String,
    /**
     * Moeda em que os preços deste plano foram GERADOS. Não é a da conta hoje:
     * mudar de moeda não converte um cardápio antigo, só lhe trocava o símbolo
     * — os mesmos números a dizer outra coisa. `null` em planos anteriores a
     * o campo existir.
     */
    val currency: String? = null,
    val active: Boolean,
    val days: List<MealPlanDayDto>,
    val shoppingList: MealShoppingListDto?,
    val tips: String? = null,
)

data class GeneratePlanRequest(
    val budget: Double? = null,
    val currencyCode: String? = null,
    val currencySymbol: String? = null,
    val currencyLocale: String? = null,
    val mealPrepMode: Boolean? = null,
    val dietMode: String? = null,
    val badMeals: List<String>? = null,
    val favoriteMeals: List<String>? = null,
    val dislikedFoods: List<String>? = null,
)

data class AddShoppingItemRequest(
    val name: String,
    val quantity: Double,
    val unit: String,
    val category: String? = null,
    val estimatedPrice: Double? = null,
)

data class UpdateActualPriceRequest(val actualPrice: Double?)

data class SubstituteMealRequest(
    val mealType: String,
    val preferences: String? = null,
)

data class SubstituteMealResponse(
    val day: MealPlanDayDto,
    val meal: MealDetailDto,
)

data class UserProfileDto(
    val height: Float? = null,
    val weight: Float? = null,
    val activityLevel: String? = null,
    val dietaryPreferences: List<String>? = null,
)

data class UserProfileRequest(
    val height: Float? = null,
    val weight: Float? = null,
    val activityLevel: String? = null,
    val dietaryPreferences: List<String>? = null,
)

// ── Meal preferences (household / cuisine / goal) ────────────────────────────
// These used to live only in the local DataStore, so they were lost on
// reinstall and invisible to the web. The server is the source of truth now.

data class MealPreferencesDto(
    val adults: Int = 1,
    val children: Int = 0,
    val cuisineStyle: String = "varied",
    val dietGoal: String = "balanced",
    val favoriteFoods: List<String> = emptyList(),
    val dislikedFoods: List<String> = emptyList(),
    val mealPrepMode: Boolean = false,
    val onboarded: Boolean = false,
    /** Adult-equivalent portions, computed by the API from adults + children. */
    val servings: Double = 1.0,
    /**
     * A meta de comida DESTA pessoa, por mês. Nulo = ainda não respondeu, que
     * não é o mesmo que ter respondido zero.
     *
     * É pessoal e não da casa: num casal cada um tem o seu tecto, e a compra
     * consome o de quem a registou. Os dois nunca são somados.
     */
    val monthlyFoodBudget: Double? = null,
    val foodBudgetCurrency: String? = null,
    /** A mesma meta por semana — é como o cardápio pensa (`× 12 ÷ 52`). */
    val weeklyFoodBudget: Double? = null,
    /** As metas do casal, à vista e identificadas, mas nunca somadas. */
    val foodBudget: FoodBudgetOverviewDto? = null,
)

data class FoodBudgetPartDto(
    val userId: Int = 0,
    val name: String = "",
    /** Já convertido para a moeda de quem lê. Nulo enquanto não responder. */
    val amount: Double? = null,
    val nativeAmount: Double? = null,
    val nativeCurrency: String? = null,
    val answered: Boolean = false,
)

data class FoodBudgetOverviewDto(
    /** A meta de quem está autenticado. Nulo = ainda não respondeu. */
    val monthly: Double? = null,
    val weekly: Double? = null,
    val parts: List<FoodBudgetPartDto> = emptyList(),
    val partial: Boolean = false,
    val complete: Boolean = false,
    val currency: String? = null,
)

data class SavePreferencesRequest(
    val adults: Int? = null,
    val children: Int? = null,
    val cuisineStyle: String? = null,
    val dietGoal: String? = null,
    val favoriteFoods: List<String>? = null,
    val dislikedFoods: List<String>? = null,
    val mealPrepMode: Boolean? = null,
    val markOnboarded: Boolean? = null,
    /** A meta mensal desta pessoa. Omitir não mexe. */
    val monthlyFoodBudget: Double? = null,
    /**
     * Apagar a meta ("ainda não sei").
     *
     * Existe porque o Gson **omite os nulos**: mandar `monthlyFoodBudget = null`
     * daqui produz exactamente o mesmo corpo que não mandar nada, e o servidor
     * não teria como distinguir "apaga" de "não mexas". Sem esta bandeira, o
     * botão "Ainda não sei" existiria e não faria nada.
     */
    val clearFoodBudget: Boolean? = null,
)

data class PreferenceOptionDto(val value: String, val label: String)

data class PreferenceOptionsDto(
    val cuisineStyles: List<PreferenceOptionDto> = emptyList(),
    val dietGoals: List<PreferenceOptionDto> = emptyList(),
)
