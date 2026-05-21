package com.finploit.android.ui.mealplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finploit.android.data.api.EnrichItem
import com.finploit.android.ui.theme.BackgroundDark
import com.finploit.android.ui.theme.CardBackground
import com.finploit.android.ui.theme.GreenPrimary
import com.finploit.android.ui.theme.TextDisabled
import com.finploit.android.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPlannerScreen(
    viewModel: MealPlannerViewModel,
    onSearchPrices: (List<EnrichItem>) -> Unit = {},
    onPantryClick: () -> Unit = {},
) {
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

    if (state.selectedDay != null) {
        val planId = state.plan?.id ?: 0
        MealDayScreen(
            day = state.selectedDay!!,
            scheduleType = state.selectedDayScheduleType,
            lunchAtWork = state.selectedDayLunchAtWork,
            dinnerAtWork = state.selectedDayDinnerAtWork,
            planId = planId,
            eatenMeals = state.eatenMeals,
            mealRatings = state.mealRatings,
            onToggleEaten = viewModel::toggleEatenMeal,
            onRateMeal = viewModel::rateMeal,
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
                        prepTimeFilter = state.prepTimeFilter,
                        onSetPrepTimeFilter = viewModel::setPrepTimeFilter,
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
                        dietaryPreferences = state.dietaryPreferences,
                        mealPrepMode = state.mealPrepMode,
                        onToggleDietaryPref = viewModel::toggleDietaryPreference,
                        onSetMealPrepMode = viewModel::setMealPrepMode,
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
