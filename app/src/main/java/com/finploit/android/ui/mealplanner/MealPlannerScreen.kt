package com.finploit.android.ui.mealplanner

import android.content.Intent
import androidx.compose.foundation.background
import com.finploit.android.data.api.EnrichItem
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finploit.android.data.dto.EnrichedShoppingItemDto
import com.finploit.android.data.dto.MealDetailDto
import com.finploit.android.data.dto.MealPlanDayDto
import com.finploit.android.data.dto.MealPlanDto
import com.finploit.android.data.dto.MealShoppingItemDto
import com.finploit.android.data.dto.ScheduleItemDto
import com.finploit.android.data.dto.SnackDetailDto
import com.finploit.android.ui.theme.BackgroundDark
import com.finploit.android.ui.theme.CardBackground
import com.finploit.android.ui.theme.CardElevated
import com.finploit.android.ui.theme.ExpenseRed
import com.finploit.android.ui.theme.GreenPrimary
import com.finploit.android.ui.theme.IncomeGreen
import com.finploit.android.ui.theme.TextDisabled
import com.finploit.android.ui.theme.TextPrimary
import com.finploit.android.ui.theme.TextSecondary
import com.finploit.android.ui.theme.LocalCurrencyConfig
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

private val gson = Gson()
private fun parseMeal(json: String?): MealDetailDto? = try {
    json?.let { gson.fromJson(it, MealDetailDto::class.java) }
} catch (e: JsonSyntaxException) { null }

private fun parseSnack(json: String?): SnackDetailDto? {
    if (json.isNullOrBlank()) return null
    return try {
        val parsed = gson.fromJson(json, SnackDetailDto::class.java)
        if (parsed?.name.isNullOrBlank()) null else parsed
    } catch (e: JsonSyntaxException) {
        // Legacy: snack stored as plain text string
        SnackDetailDto(name = json.trim())
    }
}

private val DAY_NAMES = listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb")
private val DAY_FULL_NAMES = listOf("Domingo", "Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado")
private val DAY_EMOJIS = listOf("🌅", "💼", "💼", "💼", "💼", "🎉", "😴")
private val DAY_COLORS = listOf(
    Color(0xFF69F0AE), Color(0xFF00C853), Color(0xFF00BFA5),
    Color(0xFF64B5F6), Color(0xFFFFD740), Color(0xFFFF8A65), Color(0xFFCE93D8),
)
private val DAY_TYPE_LABELS = mapOf("WORK" to "Trabalho", "OFF" to "Folga", "HALF_OFF" to "Meia-folga")
private val DAY_TYPE_COLORS = mapOf(
    "WORK" to Color(0xFF64B5F6), "OFF" to Color(0xFF69F0AE), "HALF_OFF" to Color(0xFFFFD740),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPlannerScreen(viewModel: MealPlannerViewModel, onSearchPrices: (List<EnrichItem>) -> Unit = {}, onPantryClick: () -> Unit = {}) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currencyCode by viewModel.currencyCode.collectAsStateWithLifecycle()
    val tabs = listOf("Cardápio", "Compras", "Agenda", "Histórico")
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    // Full-screen day overlay
    if (state.selectedDay != null) {
        MealDayScreen(
            day = state.selectedDay!!,
            scheduleType = state.selectedDayScheduleType,
            lunchAtWork = state.selectedDayLunchAtWork,
            dinnerAtWork = state.selectedDayDinnerAtWork,
            onBack = viewModel::clearSelectedDay,
        )
        return
    }

    Scaffold(
        containerColor = BackgroundDark,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(snackbarData = data, containerColor = CardBackground, contentColor = TextPrimary)
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).background(BackgroundDark),
        ) {
            TopAppBar(
                title = { Text("Alimentação", fontWeight = FontWeight.Bold, color = TextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark, titleContentColor = TextPrimary),
                actions = {
                    IconButton(onClick = onPantryClick) {
                        Text("🏠", fontSize = 18.sp)
                    }
                    IconButton(onClick = viewModel::load) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar", tint = GreenPrimary)
                    }
                }
            )

            TabRow(
                selectedTabIndex = state.tab.ordinal,
                containerColor = BackgroundDark,
                contentColor = GreenPrimary,
                indicator = { tabPositions ->
                    SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[state.tab.ordinal]),
                        color = GreenPrimary,
                    )
                },
            ) {
                MealTab.entries.forEachIndexed { index, tab ->
                    Tab(
                        selected = state.tab == tab,
                        onClick = { viewModel.setTab(tab) },
                        text = {
                            Text(
                                tabs[index],
                                color = if (state.tab == tab) GreenPrimary else TextDisabled,
                                fontWeight = if (state.tab == tab) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 12.sp,
                            )
                        },
                    )
                }
            }

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GreenPrimary)
                }
                else -> when (state.tab) {
                    MealTab.PLAN -> PlanTab(
                        plan = state.plan,
                        isGenerating = state.isGenerating,
                        error = state.error,
                        selectedBudget = state.budget,
                        currencyCode = currencyCode,
                        onSelectBudget = viewModel::selectBudget,
                        onGenerate = viewModel::generatePlan,
                        schedule = state.schedule,
                        onDayClick = { day -> viewModel.selectDay(day, state.schedule.find { it.dayOfWeek == day.dayOfWeek }) },
                    )
                    MealTab.SHOPPING -> ShoppingTab(
                        items = state.plan?.shoppingList?.items ?: emptyList(),
                        totalEstimate = state.plan?.shoppingList?.totalEstimate,
                        tips = state.plan?.tips,
                        onToggle = viewModel::toggleItem,
                        onSearchPrices = onSearchPrices,
                        onExportToShoppingList = viewModel::exportToShoppingList,
                        isExporting = state.isExportingToShoppingList,
                        enrichedItems = state.enrichedItems,
                        isEnrichingPrices = state.isEnrichingPrices,
                        onEnrichPrices = viewModel::enrichMealPrices,
                        shoppingFilter = state.shoppingFilter,
                        onFilterChange = viewModel::setShoppingFilter,
                        collapsedCategories = state.collapsedCategories,
                        onToggleCategory = viewModel::toggleCategoryCollapse,
                        onBuyAllInCategory = viewModel::buyAllInCategory,
                    )
                    MealTab.SCHEDULE -> ScheduleTab(
                        schedule = state.schedule,
                        isSaving = state.isSavingSchedule,
                        onSave = viewModel::saveSchedule,
                        profileHeight = state.profileHeight,
                        profileWeight = state.profileWeight,
                        profileActivityLevel = state.profileActivityLevel,
                        isSavingProfile = state.isSavingProfile,
                        onHeightChange = viewModel::setProfileHeight,
                        onWeightChange = viewModel::setProfileWeight,
                        onActivityChange = viewModel::setProfileActivityLevel,
                        onSaveProfile = viewModel::saveProfile,
                    )
                    MealTab.HISTORY -> HistoryTab(
                        plans = state.allPlans,
                        isLoading = state.isLoadingHistory,
                        isDeletingPlan = state.isDeletingPlan,
                        isClearingHistory = state.isClearingHistory,
                        onRefresh = viewModel::loadHistory,
                        onDeletePlan = viewModel::deletePlan,
                        onClearAll = viewModel::clearHistory,
                    )
                }
            }
        }
    }
}

// ─── Day full-screen overlay ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealDayScreen(
    day: MealPlanDayDto,
    scheduleType: String?,
    lunchAtWork: Boolean = false,
    dinnerAtWork: Boolean = false,
    onBack: () -> Unit,
) {
    val breakfast = parseMeal(day.breakfast)
    val lunch = parseMeal(day.lunch)
    val dinner = parseMeal(day.dinner)
    val dayColor = DAY_COLORS.getOrElse(day.dayOfWeek) { GreenPrimary }
    val typeColor = scheduleType?.let { DAY_TYPE_COLORS[it] } ?: TextDisabled

    Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        TopAppBar(
            title = {
                Column {
                    Text(DAY_FULL_NAMES.getOrElse(day.dayOfWeek) { "Dia" }, fontWeight = FontWeight.Bold, color = TextPrimary)
                    scheduleType?.let {
                        Text(DAY_TYPE_LABELS[it] ?: it, fontSize = 11.sp, color = typeColor)
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = TextPrimary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // Calories summary
            day.calories?.let { cal ->
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(dayColor.copy(alpha = 0.1f))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Total do dia", color = dayColor, fontWeight = FontWeight.SemiBold)
                        Text("🔥 $cal kcal", color = dayColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            if (breakfast != null) {
                item {
                    SectionCard(label = "☀️ Café da Manhã", meal = breakfast, accentColor = dayColor)
                }
            }
            if (lunch != null) {
                item {
                    SectionCard(label = "🌤 Almoço", meal = lunch, accentColor = Color(0xFF64B5F6))
                }
            } else if (lunchAtWork) {
                item {
                    OutsideRow("🌤 Almoço")
                }
            }
            if (dinner != null) {
                item {
                    SectionCard(label = "🌙 Jantar", meal = dinner, accentColor = Color(0xFFCE93D8))
                }
            } else if (dinnerAtWork) {
                item {
                    OutsideRow("🌙 Jantar")
                }
            }
            parseSnack(day.snacks)?.let { snack ->
                item { SnackCard(snack = snack, accentColor = Color(0xFFFFD740)) }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun OutsideRow(label: String) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TextDisabled.copy(alpha = 0.07f))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = TextDisabled, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Spacer(Modifier.width(8.dp))
        Text("— Refeição fora de casa", color = TextDisabled.copy(alpha = 0.6f), fontSize = 13.sp)
    }
}

@Composable
private fun SnackCard(snack: SnackDetailDto, accentColor: Color = Color(0xFFFFD740)) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🍎", fontSize = 18.sp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Lanche da tarde", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    if (snack.prepTime != null) {
                        Text("⏱ ${snack.prepTime}", color = TextDisabled, fontSize = 11.sp)
                    }
                }
                snack.calories?.let {
                    Spacer(Modifier.weight(1f))
                    MacroChip("🔥 $it kcal", IncomeGreen)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(snack.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            snack.description?.takeIf { it.isNotEmpty() }?.let {
                Text(it, color = TextDisabled, fontSize = 12.sp, fontStyle = FontStyle.Italic)
            }
            // Ingredients
            if (snack.ingredients.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(CardElevated).padding(10.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        snack.ingredients.forEach { ing ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.6f)))
                                Spacer(Modifier.width(8.dp))
                                Text(ing, color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            // Cheaper alternative
            snack.cheaperAlternative?.takeIf { it.isNotEmpty() }?.let { alt ->
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF2E7D32).copy(alpha = 0.12f))
                        .border(1.dp, GreenPrimary.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text("💸", fontSize = 14.sp)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Alternativa mais barata", color = GreenPrimary, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                        Spacer(Modifier.height(2.dp))
                        Text(alt, color = GreenPrimary.copy(alpha = 0.85f), fontSize = 12.sp, lineHeight = 17.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(label: String, meal: MealDetailDto, accentColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = accentColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (meal.mealType != null) {
                    Spacer(Modifier.width(8.dp))
                    MacroChip(meal.mealType, accentColor)
                }
                val prepTime = meal.prepTime
                if (!prepTime.isNullOrEmpty()) {
                    Spacer(Modifier.width(6.dp))
                    MacroChip("⏱ $prepTime", TextSecondary)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(meal.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            val desc = meal.description
            if (!desc.isNullOrEmpty()) {
                Text(desc, color = TextDisabled, fontSize = 12.sp, fontStyle = FontStyle.Italic)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MacroChip("🔥 ${meal.calories} kcal", IncomeGreen)
                if (meal.protein != null) MacroChip("P ${meal.protein.toInt()}g", Color(0xFF64B5F6))
                if (meal.carbs != null) MacroChip("C ${meal.carbs.toInt()}g", Color(0xFFFFD740))
                if (meal.fat != null) MacroChip("G ${meal.fat.toInt()}g", Color(0xFFFF8A65))
            }
            if (meal.ingredients.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("Ingredientes", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(CardElevated).padding(12.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        meal.ingredients.forEach { ing ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.6f)))
                                Spacer(Modifier.width(8.dp))
                                Text(ing, color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            if (meal.howToPrepare.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("Modo de preparo", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    meal.howToPrepare.forEachIndexed { i, step ->
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier.size(22.dp).clip(CircleShape)
                                    .background(accentColor.copy(alpha = 0.18f))
                                    .border(1.dp, accentColor.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("${i + 1}", color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(step, color = TextSecondary, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            meal.tip?.takeIf { it.isNotEmpty() }?.let {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.07f))
                        .border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text("💡", fontSize = 13.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(it, color = accentColor.copy(alpha = 0.9f), fontSize = 11.sp, lineHeight = 16.sp)
                }
            }
        }
    }
}

// ─── Plan Tab ────────────────────────────────────────────────────────────────

@Composable
private fun PlanTab(
    plan: MealPlanDto?,
    isGenerating: Boolean,
    error: String?,
    selectedBudget: BudgetPreset,
    currencyCode: String,
    onSelectBudget: (BudgetPreset) -> Unit,
    onGenerate: () -> Unit,
    schedule: List<ScheduleItemDto>,
    onDayClick: (MealPlanDayDto) -> Unit,
) {
    val scheduleByDay = schedule.associateBy { it.dayOfWeek }
    val currencyConfig = LocalCurrencyConfig.current

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        item {
            Column {
                Text("Orçamento semanal", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(BudgetPreset.entries) { preset ->
                        val isSelected = preset == selectedBudget
                        val color = when (preset) {
                            BudgetPreset.ECONOMY -> Color(0xFF69F0AE)
                            BudgetPreset.BALANCED -> GreenPrimary
                            BudgetPreset.PREMIUM -> Color(0xFFFFD740)
                            BudgetPreset.FREE -> TextSecondary
                        }
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) color.copy(alpha = 0.18f) else CardBackground)
                                .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) color else TextDisabled.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .clickable(enabled = !isGenerating) { onSelectBudget(preset) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(preset.label, color = if (isSelected) color else TextPrimary, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
                            val amount = preset.amountForCurrency(currencyCode)
                            val desc = amount?.let { "~${currencyConfig.symbol}${it.toInt()}/sem" } ?: preset.description
                            Text(desc, color = if (isSelected) color.copy(alpha = 0.8f) else TextDisabled, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
        if (error != null) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = ExpenseRed.copy(alpha = 0.12f))) {
                    Text(error, color = ExpenseRed, modifier = Modifier.padding(14.dp), fontSize = 13.sp)
                }
            }
        }
        item {
            Box(
                modifier = Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(14.dp))
                    .background(if (!isGenerating) Brush.horizontalGradient(listOf(GreenPrimary, IncomeGreen)) else Brush.horizontalGradient(listOf(TextDisabled, TextDisabled))),
                contentAlignment = Alignment.Center,
            ) {
                Button(
                    onClick = onGenerate, enabled = !isGenerating, modifier = Modifier.fillMaxSize(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent),
                    shape = RoundedCornerShape(14.dp), elevation = null,
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = BackgroundDark, strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Gerando cardápio com IA...", color = BackgroundDark, fontWeight = FontWeight.SemiBold)
                    } else {
                        Text(if (plan == null) "🥗 Gerar Cardápio com IA" else "🔄 Regenerar Cardápio", color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
        if (plan == null && !isGenerating) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("Como funciona", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 2.dp))
                    OnboardingStepRow(1, "Configure sua agenda", "Na aba Agenda defina os dias de trabalho e folga", GreenPrimary)
                    OnboardingStepRow(2, "Gere o cardápio com IA", "Escolha o orçamento e toque em Gerar Cardápio", Color(0xFF64B5F6))
                    OnboardingStepRow(3, "Compare preços", "Na aba Compras actualize os preços nos supermercados e economize", Color(0xFFFFD740))
                }
            }
        } else if (plan != null) {
            item {
                val planTotal = plan.shoppingList?.totalEstimate ?: 0.0
                val itemCount = plan.shoppingList?.items?.size ?: 0
                val dayCount = plan.days.size
                val budgetAmount = selectedBudget.amountForCurrency(currencyCode)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("Total estimado", color = TextDisabled, fontSize = 11.sp)
                            Text(currencyConfig.format(planTotal), color = GreenPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                            Text("$itemCount itens · $dayCount dias", color = TextDisabled, fontSize = 11.sp)
                        }
                        if (budgetAmount != null) {
                            val overBudget = planTotal > budgetAmount
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(10.dp))
                                    .background((if (overBudget) ExpenseRed else GreenPrimary).copy(alpha = 0.15f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    if (overBudget) "▲ acima do orçamento" else "✓ no orçamento",
                                    color = if (overBudget) ExpenseRed else GreenPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }
            items(plan.days.sortedBy { it.dayOfWeek }) { day ->
                DayCard(day = day, scheduleType = scheduleByDay[day.dayOfWeek]?.dayType, onClick = { onDayClick(day) })
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun DayCard(day: MealPlanDayDto, scheduleType: String?, onClick: () -> Unit) {
    val breakfast = parseMeal(day.breakfast)
    val lunch = parseMeal(day.lunch)
    val dinner = parseMeal(day.dinner)
    val hasSnack = parseSnack(day.snacks) != null
    val dayColor = DAY_COLORS.getOrElse(day.dayOfWeek) { GreenPrimary }
    val typeColor = scheduleType?.let { DAY_TYPE_COLORS[it] } ?: TextDisabled

    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).clickable(onClick = onClick)) {
        Box(modifier = Modifier.width(4.dp).fillMaxHeight().clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)).background(dayColor))
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 16.dp, bottomEnd = 16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
        ) {
            Row(modifier = Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(dayColor.copy(alpha = 0.15f)).border(1.dp, dayColor.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(DAY_EMOJIS.getOrElse(day.dayOfWeek) { "📅" }, fontSize = 14.sp)
                        Text(DAY_NAMES.getOrElse(day.dayOfWeek) { "?" }, color = dayColor, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    // Day name + schedule type badge
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(DAY_FULL_NAMES.getOrElse(day.dayOfWeek) { "?" }, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        scheduleType?.let {
                            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(typeColor.copy(alpha = 0.15f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text(DAY_TYPE_LABELS[it] ?: it, color = typeColor, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        day.calories?.let {
                            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(IncomeGreen.copy(alpha = 0.1f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text("🔥 $it kcal", color = IncomeGreen, fontSize = 10.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    // Meal chips — plain Row with horizontal scroll avoids SubcomposeLayout intrinsic crash
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        if (breakfast != null) MacroChip("☀️ ${breakfast.name.take(16)}", dayColor)
                        if (lunch != null) MacroChip("🌤 ${lunch.name.take(16)}", Color(0xFF64B5F6))
                        else if (scheduleType == "WORK" || scheduleType == "HALF_OFF") MacroChip("🌤 Fora", TextDisabled)
                        if (dinner != null) MacroChip("🌙 ${dinner.name.take(16)}", Color(0xFFCE93D8))
                        else if (scheduleType == "WORK") MacroChip("🌙 Fora", TextDisabled)
                        if (hasSnack) MacroChip("🍎 Lanche", Color(0xFFFFD740))
                    }
                }
                Text("›", color = TextDisabled, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

// ─── Shopping Tab ─────────────────────────────────────────────────────────

@Composable
private fun ShoppingTab(
    items: List<MealShoppingItemDto>,
    totalEstimate: Double?,
    tips: String?,
    onToggle: (Int) -> Unit,
    onSearchPrices: (List<EnrichItem>) -> Unit = {},
    onExportToShoppingList: () -> Unit = {},
    isExporting: Boolean = false,
    enrichedItems: Map<String, EnrichedShoppingItemDto> = emptyMap(),
    isEnrichingPrices: Boolean = false,
    onEnrichPrices: () -> Unit = {},
    shoppingFilter: ShoppingFilter = ShoppingFilter.PENDING,
    onFilterChange: (ShoppingFilter) -> Unit = {},
    collapsedCategories: Set<String> = emptySet(),
    onToggleCategory: (String) -> Unit = {},
    onBuyAllInCategory: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val currencyConfig = LocalCurrencyConfig.current

    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🛒", fontSize = 48.sp)
                Spacer(Modifier.height(12.dp))
                Text("Nenhuma lista de compras", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text("Gere um cardápio para criar sua lista", color = TextDisabled, fontSize = 13.sp)
            }
        }
        return
    }

    val purchased = items.count { it.purchased }
    val total = items.size
    val filteredItems = when (shoppingFilter) {
        ShoppingFilter.ALL -> items
        ShoppingFilter.PENDING -> items.filter { !it.purchased }
        ShoppingFilter.PURCHASED -> items.filter { it.purchased }
    }
    val grouped = filteredItems.groupBy { it.category ?: "Outros" }
    val remainingEstimate = items.filter { !it.purchased }.sumOf { i ->
        enrichedItems[i.name.trim().lowercase()]?.bestPrice ?: i.estimatedPrice ?: 0.0
    }

    val enrichItems = items.filter { !it.purchased }.map { i ->
        EnrichItem(name = i.name, quantity = i.quantity, unit = i.unit, estimatedPrice = i.estimatedPrice ?: 0.0)
    }
    val pendingCount = total - purchased

    val enrichedTotal = enrichedItems.values.sumOf { it.bestPrice ?: it.estimatedPrice }
    // Compare only over the same set of items that were enriched
    val aiTotal = enrichedItems.values.sumOf { it.estimatedPrice }
    val savings = aiTotal - enrichedTotal
    val hasEnriched = enrichedItems.isNotEmpty()

    val bestStore = if (hasEnriched) {
        enrichedItems.values
            .mapNotNull { it.bestSource }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
    } else null

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        // Filter chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ShoppingFilter.entries.forEach { f ->
                    val selected = shoppingFilter == f
                    val label = when (f) {
                        ShoppingFilter.ALL -> "Todos"
                        ShoppingFilter.PENDING -> "Pendentes"
                        ShoppingFilter.PURCHASED -> "Comprados"
                    }
                    val count = when (f) {
                        ShoppingFilter.ALL -> total
                        ShoppingFilter.PENDING -> total - purchased
                        ShoppingFilter.PURCHASED -> purchased
                    }
                    FilterChip(
                        selected = selected,
                        onClick = { onFilterChange(f) },
                        label = { Text("$label ($count)", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GreenPrimary.copy(alpha = 0.15f),
                            selectedLabelColor = GreenPrimary,
                        ),
                    )
                }
            }
        }
        // Progress card
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = CardBackground)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("$purchased de $total itens comprados", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(6.dp))
                            Box(modifier = Modifier.width(180.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(TextDisabled.copy(alpha = 0.2f))) {
                                if (total > 0) Box(modifier = Modifier.fillMaxWidth(purchased.toFloat() / total).height(6.dp).clip(RoundedCornerShape(3.dp)).background(Brush.horizontalGradient(listOf(GreenPrimary, IncomeGreen))))
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Restante", color = TextDisabled, fontSize = 11.sp)
                            Text("${LocalCurrencyConfig.current.format(remainingEstimate)}", color = GreenPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            totalEstimate?.let { Text("de ${LocalCurrencyConfig.current.format(it)}", color = TextDisabled, fontSize = 11.sp) }
                        }
                    }
                    if (purchased == total && total > 0) {
                        Spacer(Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(GreenPrimary.copy(alpha = 0.1f)).padding(8.dp), contentAlignment = Alignment.Center) {
                            Text("🎉 Todas as compras concluídas!", color = GreenPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        // Enrich + compare buttons (primary actions)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onEnrichPrices,
                    enabled = enrichItems.isNotEmpty() && !isEnrichingPrices,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32).copy(alpha = 0.85f),
                        disabledContainerColor = TextDisabled.copy(alpha = 0.2f),
                    ),
                ) {
                    if (isEnrichingPrices) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = TextPrimary, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("A buscar preços...", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    } else {
                        Text("💶", fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("Actualizar preços nos supermercados", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
                Button(
                    onClick = { onSearchPrices(enrichItems) },
                    enabled = enrichItems.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0).copy(alpha = 0.85f), disabledContainerColor = TextDisabled.copy(alpha = 0.2f)),
                ) {
                    Text("🛒", fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("Ver comparação detalhada", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }
        // No prices found banner
        if (hasEnriched && enrichedItems.values.all { it.bestPrice == null }) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFD740).copy(alpha = 0.08f))
                        .border(1.dp, Color(0xFFFFD740).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("⚠️", fontSize = 16.sp)
                    Column {
                        Text("Nenhum preço encontrado", color = Color(0xFFFFD740), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("O scraper pode estar a reiniciar. Tenta de novo em alguns segundos.", color = TextDisabled, fontSize = 11.sp, lineHeight = 16.sp)
                    }
                }
            }
        }
        // Savings summary (shown after enrichment)
        if (hasEnriched && savings > 0.50) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(GreenPrimary.copy(alpha = 0.10f))
                        .border(1.dp, GreenPrimary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text("💰 Poupança potencial", color = GreenPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("vs estimativa da IA", color = TextDisabled, fontSize = 11.sp)
                    }
                    Text(currencyConfig.format(savings), color = GreenPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
        // Best supermarket recommendation
        if (bestStore != null) {
            val storeEmoji = when (bestStore.lowercase()) {
                "continente" -> "🏪"
                "auchan" -> "🟠"
                "pingodoce" -> "🟡"
                "mercadona" -> "🟢"
                else -> "🏬"
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1565C0).copy(alpha = 0.08f))
                        .border(1.dp, Color(0xFF64B5F6).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text("$storeEmoji Melhor supermercado", color = Color(0xFF64B5F6), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("mais itens ao melhor preço", color = TextDisabled, fontSize = 11.sp)
                    }
                    Text(bestStore.replaceFirstChar { it.uppercase() }, color = Color(0xFF64B5F6), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
        // Share + export (secondary, side by side)
        item {
            val shareText = buildString {
                append("🛒 Lista de Compras — Plano Alimentar\n\n")
                grouped.forEach { (cat, catItems) ->
                    append("📦 $cat\n")
                    catItems.forEach { i ->
                        val qty = if (i.quantity == i.quantity.toLong().toDouble()) i.quantity.toLong().toString() else "%.1f".format(i.quantity)
                        append("  • ${i.name} — $qty ${i.unit}")
                        i.estimatedPrice?.let { append(" (~${currencyConfig.format(it)})") }
                        append("\n")
                    }
                    append("\n")
                }
                totalEstimate?.let { append("💰 Total estimado: ${currencyConfig.format(it)}") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, shareText) }
                        context.startActivity(Intent.createChooser(intent, "Compartilhar lista"))
                    },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary.copy(alpha = 0.12f)),
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Partilhar", color = GreenPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                if (pendingCount > 0) {
                    Button(
                        onClick = onExportToShoppingList,
                        enabled = !isExporting,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GreenPrimary.copy(alpha = 0.12f),
                            disabledContainerColor = TextDisabled.copy(alpha = 0.10f),
                        ),
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = GreenPrimary, strokeWidth = 2.dp)
                        } else {
                            Text("📋", fontSize = 13.sp)
                            Spacer(Modifier.width(4.dp))
                            Text("Exportar ($pendingCount)", color = GreenPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
        tips?.takeIf { it.isNotEmpty() }?.let { tip ->
            item {
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFFFD740).copy(alpha = 0.08f)).border(1.dp, Color(0xFFFFD740).copy(alpha = 0.3f), RoundedCornerShape(12.dp)).padding(12.dp), verticalAlignment = Alignment.Top) {
                    Text("💰", fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(tip, color = Color(0xFFFFD740).copy(alpha = 0.9f), fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        }
        // Empty states for filters
        if (shoppingFilter == ShoppingFilter.PURCHASED && purchased == 0) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🛒", fontSize = 32.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Nenhum item comprado ainda.", color = TextDisabled, fontSize = 13.sp)
                    }
                }
            }
        }
        if (shoppingFilter == ShoppingFilter.PENDING && purchased == total) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎉", fontSize = 32.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Todas as compras concluídas!", color = GreenPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        grouped.forEach { (category, categoryItems) ->
            val collapsed = category in collapsedCategories
            // All items in this category across full list (for purchased count)
            val allCategoryItems = items.filter { (it.category ?: "Outros") == category }
            val catPurchased = allCategoryItems.count { it.purchased }
            val catPending = allCategoryItems.count { !it.purchased }
            item(key = "header_$category") {
                val catEstimate = categoryItems.sumOf { enrichedItems[it.name.trim().lowercase()]?.bestPrice ?: it.estimatedPrice ?: 0.0 }
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onToggleCategory(category) }.padding(top = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(GreenPrimary))
                    Spacer(Modifier.width(8.dp))
                    Text(category, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(Modifier.width(6.dp))
                    Text("$catPurchased/${allCategoryItems.size}", color = if (catPurchased == allCategoryItems.size) GreenPrimary else TextDisabled, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    if (catPending > 0 && shoppingFilter != ShoppingFilter.PURCHASED) {
                        TextButton(
                            onClick = { onBuyAllInCategory(category) },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text("Comprar tudo", color = GreenPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Text(currencyConfig.format(catEstimate), color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(end = 4.dp))
                    }
                    Icon(
                        imageVector = if (collapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        tint = TextDisabled,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            if (!collapsed) {
                items(categoryItems, key = { it.id }) { item ->
                    ShoppingItemRow(
                        item = item,
                        enriched = enrichedItems[item.name.trim().lowercase()],
                        onToggle = { onToggle(item.id) },
                    )
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun ShoppingItemRow(
    item: MealShoppingItemDto,
    enriched: EnrichedShoppingItemDto? = null,
    onToggle: () -> Unit,
) {
    val qty = if (item.quantity == item.quantity.toLong().toDouble()) item.quantity.toLong().toString() else "%.1f".format(item.quantity)
    val currencyConfig = LocalCurrencyConfig.current
    val storeEmoji = when (enriched?.bestSource?.lowercase()) {
        "continente" -> "🏪"
        "auchan" -> "🟠"
        "pingodoce" -> "🟡"
        "mercadona" -> "🟢"
        else -> "🏷️"
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onToggle() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (item.purchased) CardBackground.copy(alpha = 0.5f) else CardBackground),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(26.dp).clip(CircleShape)
                    .background(if (item.purchased) Brush.radialGradient(listOf(GreenPrimary, IncomeGreen)) else Brush.radialGradient(listOf(Color.Transparent, Color.Transparent)))
                    .border(1.5.dp, if (item.purchased) GreenPrimary else TextDisabled.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (item.purchased) Icon(Icons.Default.Check, contentDescription = null, tint = BackgroundDark, modifier = Modifier.size(15.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, color = if (item.purchased) TextDisabled else TextPrimary, fontSize = 14.sp, fontWeight = if (!item.purchased) FontWeight.Medium else FontWeight.Normal, textDecoration = if (item.purchased) TextDecoration.LineThrough else TextDecoration.None)
                Text("$qty ${item.unit}", color = TextDisabled, fontSize = 12.sp)
                // usedInDays badge
                val usedCount = remember(item.usedInDays) {
                    try { gson.fromJson(item.usedInDays ?: "[]", Array<Int>::class.java)?.size ?: 0 } catch (e: Exception) { 0 }
                }
                if (!item.purchased && usedCount >= 2) {
                    Spacer(Modifier.height(3.dp))
                    Text("✦ $usedCount dias", color = Color(0xFF64B5F6), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
                // Package note
                if (!item.purchased && !item.packageNote.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFFD740).copy(alpha = 0.08f))
                            .border(1.dp, Color(0xFFFFD740).copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("📦 ", fontSize = 10.sp)
                        Text(item.packageNote, color = Color(0xFFFFD740).copy(alpha = 0.85f), fontSize = 10.sp, lineHeight = 14.sp)
                    }
                }
                // Inline scraped price badge
                if (!item.purchased && enriched?.bestPrice != null && enriched.bestSource != null) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(GreenPrimary.copy(alpha = 0.12f))
                                .border(1.dp, GreenPrimary.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                "$storeEmoji ${enriched.bestSource.replaceFirstChar { it.uppercase() }} ${currencyConfig.format(enriched.bestPrice)}",
                                color = GreenPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                item.estimatedPrice?.let {
                    Text(
                        currencyConfig.format(it),
                        color = if (item.purchased) TextDisabled else if (enriched?.bestPrice != null) TextDisabled else GreenPrimary,
                        fontSize = 13.sp,
                        fontWeight = if (!item.purchased) FontWeight.Bold else FontWeight.Normal,
                        textDecoration = if (item.purchased) TextDecoration.LineThrough else if (enriched?.bestPrice != null) TextDecoration.LineThrough else TextDecoration.None,
                    )
                }
                if (!item.purchased && enriched?.bestPrice != null) {
                    Text(
                        currencyConfig.format(enriched.bestPrice),
                        color = GreenPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

// ─── Schedule Tab ─────────────────────────────────────────────────────────

@Composable
private fun ScheduleTab(
    schedule: List<ScheduleItemDto>,
    isSaving: Boolean,
    onSave: (List<ScheduleItemDto>) -> Unit,
    profileHeight: String = "",
    profileWeight: String = "",
    profileActivityLevel: String = "moderate",
    isSavingProfile: Boolean = false,
    onHeightChange: (String) -> Unit = {},
    onWeightChange: (String) -> Unit = {},
    onActivityChange: (String) -> Unit = {},
    onSaveProfile: () -> Unit = {},
) {
    // Per-meal state: lunchAtWork[day] and dinnerAtWork[day]
    // Backward-compat: if the new boolean fields are false but dayType is WORK/HALF_OFF,
    // derive the correct value from dayType (old backend without the new columns).
    val lunchAtWork = remember(schedule) {
        val map = schedule.associateBy { it.dayOfWeek }
        (0..6).map { day ->
            val item = map[day]
            val value = when {
                item == null -> false
                item.lunchAtWork -> true
                // Old backend: WORK and HALF_OFF both had lunch outside
                item.dayType == "WORK" || item.dayType == "HALF_OFF" -> true
                else -> false
            }
            mutableStateOf(value)
        }
    }
    val dinnerAtWork = remember(schedule) {
        val map = schedule.associateBy { it.dayOfWeek }
        (0..6).map { day ->
            val item = map[day]
            val value = when {
                item == null -> false
                item.dinnerAtWork -> true
                // Old backend: only WORK had dinner outside (HALF_OFF had dinner at home)
                item.dayType == "WORK" -> true
                else -> false
            }
            mutableStateOf(value)
        }
    }

    val activityOptions = listOf(
        "sedentary" to "🪑 Sedentário",
        "light" to "🚶 Leve",
        "moderate" to "🏃 Moderado",
        "active" to "💪 Ativo",
        "very_active" to "🏋️ Muito Ativo",
    )

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Spacer(Modifier.height(8.dp)) }

        // Physical profile card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📊", fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Perfil Físico", color = GreenPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("A IA usa estes dados para calcular as calorias e porções certas para si.", color = TextDisabled, fontSize = 11.sp, lineHeight = 15.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    // Height + Weight side by side
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = profileHeight,
                            onValueChange = onHeightChange,
                            label = { Text("Altura (cm)", fontSize = 12.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GreenPrimary,
                                unfocusedBorderColor = TextDisabled.copy(alpha = 0.4f),
                                focusedLabelColor = GreenPrimary,
                                unfocusedLabelColor = TextDisabled,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = GreenPrimary,
                            ),
                        )
                        OutlinedTextField(
                            value = profileWeight,
                            onValueChange = onWeightChange,
                            label = { Text("Peso (kg)", fontSize = 12.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GreenPrimary,
                                unfocusedBorderColor = TextDisabled.copy(alpha = 0.4f),
                                focusedLabelColor = GreenPrimary,
                                unfocusedLabelColor = TextDisabled,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = GreenPrimary,
                            ),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    // BMI + TDEE preview
                    val h = profileHeight.toFloatOrNull()
                    val w = profileWeight.toFloatOrNull()
                    if (h != null && h > 0 && w != null && w > 0) {
                        val bmi = w / ((h / 100f) * (h / 100f))
                        val bmiLabel = when {
                            bmi < 18.5f -> "Abaixo do peso"
                            bmi < 25f -> "Peso normal"
                            bmi < 30f -> "Sobrepeso"
                            else -> "Obesidade"
                        }
                        val bmr = (10 * w + 6.25f * h - 155).toInt()
                        val mult = mapOf("sedentary" to 1.2f, "light" to 1.375f, "moderate" to 1.55f, "active" to 1.725f, "very_active" to 1.9f)
                        val tdee = (bmr * (mult[profileActivityLevel] ?: 1.55f)).toInt()
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                .background(GreenPrimary.copy(alpha = 0.08f)).padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("IMC", color = TextDisabled, fontSize = 10.sp)
                                Text("%.1f".format(bmi), color = GreenPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(bmiLabel, color = TextDisabled, fontSize = 9.sp)
                            }
                            Box(modifier = Modifier.width(1.dp).height(36.dp).background(TextDisabled.copy(alpha = 0.2f)))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("TDEE / dia", color = TextDisabled, fontSize = 10.sp)
                                Text("$tdee kcal", color = GreenPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("gasto estimado", color = TextDisabled, fontSize = 9.sp)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    // Activity level chips
                    Text("Nível de actividade", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        activityOptions.forEach { (key, label) ->
                            val selected = profileActivityLevel == key
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) GreenPrimary.copy(alpha = 0.18f) else CardElevated)
                                    .border(if (selected) 1.5.dp else 1.dp, if (selected) GreenPrimary else TextDisabled.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                    .clickable { onActivityChange(key) }
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                            ) {
                                Text(label, color = if (selected) GreenPrimary else TextSecondary, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onSaveProfile,
                        enabled = !isSavingProfile,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary.copy(alpha = 0.85f), disabledContainerColor = TextDisabled.copy(alpha = 0.3f)),
                    ) {
                        if (isSavingProfile) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = BackgroundDark, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("A guardar...", color = BackgroundDark)
                        } else {
                            Text("💾 Guardar Perfil", color = BackgroundDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = CardBackground)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("🗓️ Onde você come cada refeição?", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Marque as refeições que você faz fora de casa (trabalho, restaurante, etc). " +
                        "A IA só vai gerar receita e incluir ingredientes nas compras para o que você come em casa.",
                        color = TextDisabled, fontSize = 12.sp, lineHeight = 18.sp,
                    )
                }
            }
        }
        items(7) { day ->
            val dayColor = DAY_COLORS.getOrElse(day) { GreenPrimary }
            val isLunch = lunchAtWork[day]
            val isDinner = dinnerAtWork[day]
            val allOut = isLunch.value && isDinner.value
            val allHome = !isLunch.value && !isDinner.value

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = CardBackground)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Day header
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape)
                                .background(dayColor.copy(alpha = 0.15f))
                                .border(1.dp, dayColor.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(DAY_EMOJIS.getOrElse(day) { "📅" }, fontSize = 13.sp)
                                Text(DAY_NAMES.getOrElse(day) { "?" }, color = dayColor, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(DAY_FULL_NAMES[day], color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            val summary = when {
                                allHome -> "Todas as refeições em casa 🏠"
                                allOut -> "Almoço e jantar fora 🏢"
                                isLunch.value -> "Almoço fora, jantar em casa"
                                else -> "Almoço em casa, jantar fora"
                            }
                            Text(summary, color = if (allHome) GreenPrimary else Color(0xFF64B5F6), fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    // Café da manhã — always at home (info only)
                    MealLocationRow(
                        emoji = "☀️",
                        label = "Café da manhã",
                        atWork = false,
                        locked = true,
                        onToggle = {},
                    )
                    Spacer(Modifier.height(8.dp))

                    // Almoço toggle
                    MealLocationRow(
                        emoji = "🌤",
                        label = "Almoço",
                        atWork = isLunch.value,
                        locked = false,
                        onToggle = { isLunch.value = !isLunch.value },
                    )
                    Spacer(Modifier.height(8.dp))

                    // Jantar toggle
                    MealLocationRow(
                        emoji = "🌙",
                        label = "Jantar",
                        atWork = isDinner.value,
                        locked = false,
                        onToggle = { isDinner.value = !isDinner.value },
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (!isSaving) Brush.horizontalGradient(listOf(GreenPrimary, IncomeGreen)) else Brush.horizontalGradient(listOf(TextDisabled, TextDisabled))),
                contentAlignment = Alignment.Center,
            ) {
                Button(
                    onClick = {
                        onSave((0..6).map { day ->
                            val lunch = lunchAtWork[day].value
                            val dinner = dinnerAtWork[day].value
                            val dayType = when {
                                lunch && dinner -> "WORK"
                                lunch -> "HALF_OFF"
                                else -> "OFF"
                            }
                            ScheduleItemDto(dayOfWeek = day, dayType = dayType, lunchAtWork = lunch, dinnerAtWork = dinner)
                        })
                    },
                    enabled = !isSaving, modifier = Modifier.fillMaxSize(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent),
                    shape = RoundedCornerShape(12.dp), elevation = null,
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = BackgroundDark, strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Salvando...", color = BackgroundDark)
                    } else {
                        Text("✅ Salvar Agenda", color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun MealLocationRow(
    emoji: String,
    label: String,
    atWork: Boolean,
    locked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    locked -> GreenPrimary.copy(alpha = 0.06f)
                    atWork -> Color(0xFF1565C0).copy(alpha = 0.12f)
                    else -> GreenPrimary.copy(alpha = 0.06f)
                }
            )
            .border(
                1.dp,
                when {
                    locked -> GreenPrimary.copy(alpha = 0.2f)
                    atWork -> Color(0xFF64B5F6).copy(alpha = 0.5f)
                    else -> GreenPrimary.copy(alpha = 0.2f)
                },
                RoundedCornerShape(10.dp),
            )
            .clickable(enabled = !locked) { onToggle() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, fontSize = 16.sp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            Text(
                if (locked) "Sempre em casa ✓" else if (atWork) "Fora de casa 🏢" else "Em casa 🏠",
                color = if (atWork && !locked) Color(0xFF64B5F6) else GreenPrimary,
                fontSize = 11.sp,
            )
        }
        if (!locked) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (atWork) Color(0xFF64B5F6).copy(alpha = 0.2f) else GreenPrimary.copy(alpha = 0.15f))
                    .border(1.5.dp, if (atWork) Color(0xFF64B5F6) else GreenPrimary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (atWork) "🏢" else "🏠", fontSize = 13.sp)
            }
        }
    }
}

// ─── History Tab ──────────────────────────────────────────────────────────

@Composable
private fun HistoryTab(
    plans: List<MealPlanDto>,
    isLoading: Boolean,
    isDeletingPlan: Int?,
    isClearingHistory: Boolean,
    onRefresh: () -> Unit,
    onDeletePlan: (Int) -> Unit,
    onClearAll: () -> Unit,
) {
    var showClearConfirm by remember { mutableStateOf(false) }

    if (showClearConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            containerColor = CardBackground,
            title = { Text("Limpar histórico?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Todos os planos de alimentação serão apagados. Esta ação não pode ser desfeita.", color = TextSecondary, fontSize = 13.sp) },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = { showClearConfirm = false; onClearAll() },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
                ) { Text("Apagar tudo", color = Color.White) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancelar", color = TextDisabled)
                }
            },
        )
    }

    when {
        isLoading || isClearingHistory -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = GreenPrimary)
                if (isClearingHistory) {
                    Spacer(Modifier.height(8.dp))
                    Text("A limpar histórico…", color = TextDisabled, fontSize = 13.sp)
                }
            }
        }
        plans.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📋", fontSize = 48.sp)
                Spacer(Modifier.height(12.dp))
                Text("Nenhum plano anterior", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text("Os teus cardápios gerados aparecerão aqui", color = TextDisabled, fontSize = 13.sp)
            }
        }
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${plans.size} planos", color = TextDisabled, fontSize = 13.sp)
                    androidx.compose.material3.TextButton(
                        onClick = { showClearConfirm = true },
                        enabled = !isClearingHistory,
                    ) {
                        Text("🗑 Limpar tudo", color = Color(0xFFEF5350), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            items(plans, key = { it.id }) { plan ->
                HistoryPlanCard(
                    plan = plan,
                    isDeleting = isDeletingPlan == plan.id,
                    onDelete = { onDeletePlan(plan.id) },
                )
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun HistoryPlanCard(
    plan: MealPlanDto,
    isDeleting: Boolean,
    onDelete: () -> Unit,
) {
    var showConfirm by remember { mutableStateOf(false) }

    if (showConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showConfirm = false },
            containerColor = CardBackground,
            title = { Text("Apagar plano?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("O plano da semana de ${plan.weekStart.take(10)} será apagado permanentemente.", color = TextSecondary, fontSize = 13.sp) },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = { showConfirm = false; onDelete() },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
                ) { Text("Apagar", color = Color.White) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showConfirm = false }) {
                    Text("Cancelar", color = TextDisabled)
                }
            },
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Semana de ${plan.weekStart.take(10)}",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                    Text("${plan.days.size} dias planeados", color = TextDisabled, fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (plan.active) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(GreenPrimary.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                            Text("Ativo", color = GreenPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    if (isDeleting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFFEF5350), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = { showConfirm = true }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Apagar plano",
                                tint = Color(0xFFEF5350).copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
            plan.shoppingList?.let { list ->
                Spacer(Modifier.height(8.dp))
                Divider(color = TextDisabled.copy(alpha = 0.1f))
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val bought = list.items.count { it.purchased }
                    Text("🛒 $bought/${list.items.size} itens comprados", color = TextSecondary, fontSize = 12.sp)
                    list.totalEstimate?.let {
                        Text(LocalCurrencyConfig.current.format(it), color = GreenPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingStepRow(step: Int, title: String, subtitle: String, accentColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(accentColor.copy(alpha = 0.07f))
            .border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Text("$step", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Column {
            Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(subtitle, color = TextDisabled, fontSize = 11.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun MacroChip(text: String, color: Color = GreenPrimary) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.12f)).border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
