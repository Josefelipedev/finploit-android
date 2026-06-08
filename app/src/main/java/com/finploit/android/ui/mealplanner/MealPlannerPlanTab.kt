package com.finploit.android.ui.mealplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finploit.android.data.dto.MealPlanDayDto
import com.finploit.android.data.dto.MealPlanDto
import com.finploit.android.data.dto.ScheduleItemDto
import com.finploit.android.ui.theme.BackgroundDark
import com.finploit.android.ui.theme.CardBackground
import com.finploit.android.ui.theme.ExpenseRed
import com.finploit.android.ui.theme.GreenPrimary
import com.finploit.android.ui.theme.IncomeGreen
import com.finploit.android.ui.theme.LocalCurrencyConfig
import com.finploit.android.ui.theme.TextDisabled
import com.finploit.android.ui.theme.TextPrimary
import com.finploit.android.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlanTab(
    plan: MealPlanDto?,
    isGenerating: Boolean,
    error: String?,
    selectedBudget: BudgetPreset,
    currencyCode: String,
    onSelectBudget: (BudgetPreset) -> Unit,
    onGenerate: () -> Unit,
    onDismissError: () -> Unit = {},
    schedule: List<ScheduleItemDto>,
    prepTimeFilter: Int? = null,
    onSetPrepTimeFilter: (Int?) -> Unit = {},
    onDayClick: (MealPlanDayDto) -> Unit,
    dietMode: DietMode = DietMode.BALANCED,
    eatenMeals: Set<String> = emptySet(),
    planId: Int = 0,
    tips: String? = null,
    favoriteMeals: Set<String> = emptySet(),
    tdee: Int? = null,
    customBudgetText: String = "",
    onCustomBudgetChange: (String) -> Unit = {},
) {
    val scheduleByDay = schedule.associateBy { it.dayOfWeek }
    val currencyConfig = LocalCurrencyConfig.current
    var showRegenerateConfirm by remember { mutableStateOf(false) }

    if (showRegenerateConfirm) {
        AlertDialog(
            onDismissRequest = { showRegenerateConfirm = false },
            containerColor = CardBackground,
            title = { Text("Regenerar cardápio?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("O cardápio atual será substituído por um novo. Esta ação não pode ser desfeita.", color = TextSecondary, fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = { showRegenerateConfirm = false; onGenerate() },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                ) { Text("Regenerar", color = BackgroundDark, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showRegenerateConfirm = false }) {
                    Text("Cancelar", color = TextDisabled)
                }
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        if (plan != null) {
            item {
                val mealTypes = listOf("breakfast", "lunch", "dinner")
                val totalPlanned = plan.days.size * 3
                val eatenCount = plan.days.sumOf { day ->
                    mealTypes.count { type -> "${planId}_${day.dayOfWeek}_$type" in eatenMeals }
                }
                val adherencePct = if (totalPlanned > 0) (eatenCount * 100) / totalPlanned else 0
                // Consecutive days ending at today, going backward
                val streak = remember(plan.id, eatenMeals) {
                    val dowMap = plan.days.associateBy { it.dayOfWeek }
                    var count = 0
                    var checkDow = todayDowFromJava()
                    repeat(plan.days.size) {
                        val day = dowMap[checkDow] ?: return@remember count
                        if (mealTypes.none { type -> "${planId}_${day.dayOfWeek}_$type" in eatenMeals }) return@remember count
                        count++
                        checkDow = if (checkDow == 0) 6 else checkDow - 1
                    }
                    count
                }
                val favoriteCount = favoriteMeals.count { it.startsWith("${planId}_") }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$eatenCount/$totalPlanned", color = GreenPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("refeições", color = TextDisabled, fontSize = 10.sp)
                            Box(
                                modifier = Modifier.padding(top = 4.dp).clip(RoundedCornerShape(4.dp))
                                    .background(GreenPrimary.copy(alpha = 0.12f)).padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text("$adherencePct% aderência", color = GreenPrimary, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Box(modifier = Modifier.size(1.dp, 40.dp).background(TextDisabled.copy(alpha = 0.2f)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔥 $streak", color = Color(0xFFFF8A65), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("dias seguidos", color = TextDisabled, fontSize = 10.sp)
                        }
                        Box(modifier = Modifier.size(1.dp, 40.dp).background(TextDisabled.copy(alpha = 0.2f)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("⭐ $favoriteCount", color = Color(0xFFFFD740), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("favoritas", color = TextDisabled, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Orçamento semanal", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(BudgetPreset.entries) { preset ->
                        val isSelected = preset == selectedBudget
                        val color = when (preset) {
                            BudgetPreset.ECONOMY -> Color(0xFF69F0AE)
                            BudgetPreset.BALANCED -> GreenPrimary
                            BudgetPreset.PREMIUM -> Color(0xFFFFD740)
                            BudgetPreset.FREE -> TextSecondary
                            BudgetPreset.CUSTOM -> Color(0xFFCE93D8)
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
                if (selectedBudget == BudgetPreset.CUSTOM) {
                    OutlinedTextField(
                        value = customBudgetText,
                        onValueChange = onCustomBudgetChange,
                        label = { Text("Valor personalizado (${currencyConfig.symbol})", fontSize = 12.sp) },
                        placeholder = { Text("ex: 120", color = TextDisabled, fontSize = 13.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFCE93D8),
                            unfocusedBorderColor = TextDisabled.copy(alpha = 0.4f),
                            focusedLabelColor = Color(0xFFCE93D8),
                            unfocusedLabelColor = TextDisabled,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = Color(0xFFCE93D8),
                        ),
                    )
                    if (customBudgetText.isNotBlank()) {
                        Text(
                            "A IA vai tentar respeitar o limite de ${currencyConfig.symbol}${customBudgetText}/semana",
                            color = Color(0xFFCE93D8),
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        }
        if (error != null) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = ExpenseRed.copy(alpha = 0.12f))) {
                    Row(
                        modifier = Modifier.padding(start = 14.dp, top = 4.dp, bottom = 4.dp, end = 4.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            error,
                            color = ExpenseRed,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f).padding(vertical = 10.dp),
                        )
                        IconButton(onClick = onDismissError, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar erro", tint = ExpenseRed, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(GreenPrimary.copy(alpha = 0.12f))
                            .border(1.dp, GreenPrimary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Text(
                            "${dietMode.emoji} ${dietMode.label}",
                            color = GreenPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text("Modo activo — alterar em Agenda", color = TextDisabled, fontSize = 11.sp)
                }
                Box(
                    modifier = Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(14.dp))
                        .background(if (!isGenerating) Brush.horizontalGradient(listOf(GreenPrimary, IncomeGreen)) else Brush.horizontalGradient(listOf(TextDisabled, TextDisabled))),
                    contentAlignment = Alignment.Center,
                ) {
                    Button(
                        onClick = { if (plan != null) showRegenerateConfirm = true else onGenerate() },
                        enabled = !isGenerating,
                        modifier = Modifier.fillMaxSize(),
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
                val currentDow = remember { todayDowFromJava() }
                val daysByDow = plan.days.associateBy { it.dayOfWeek }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 4.dp)) {
                    items(7) { dow ->
                        val day = daysByDow[dow]
                        val dayColor = DAY_COLORS.getOrElse(dow) { GreenPrimary }
                        val isToday = dow == currentDow
                        Column(
                            modifier = Modifier
                                .width(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isToday) dayColor.copy(alpha = 0.2f) else CardBackground)
                                .border(if (isToday) 1.5.dp else 1.dp, if (isToday) dayColor else TextDisabled.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .clickable(enabled = day != null) { day?.let { onDayClick(it) } }
                                .padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(DAY_NAMES.getOrElse(dow) { "" }, color = if (isToday) dayColor else TextDisabled, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(2.dp))
                            Text(DAY_EMOJIS.getOrElse(dow) { "?" }, fontSize = 14.sp)
                            Spacer(Modifier.height(2.dp))
                            val cal = day?.calories
                            if (cal != null) Text("${cal}kcal", color = TextDisabled, fontSize = 8.sp)
                            else Text("—", color = TextDisabled.copy(alpha = 0.4f), fontSize = 9.sp)
                        }
                    }
                }
            }
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
            item {
                val allMeals = remember(plan.id) {
                    plan.days.flatMap { d ->
                        listOfNotNull(parseMeal(d.breakfast), parseMeal(d.lunch), parseMeal(d.dinner))
                    }
                }
                if (allMeals.isNotEmpty()) {
                    val totalCals = allMeals.sumOf { it.calories }
                    val totalProtein = allMeals.mapNotNull { it.protein }.sum()
                    val totalCarbs = allMeals.mapNotNull { it.carbs }.sum()
                    val totalFat = allMeals.mapNotNull { it.fat }.sum()
                    val avgCals = if (plan.days.isNotEmpty()) totalCals / plan.days.size else 0
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = CardBackground)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Resumo Nutricional Semanal", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(6.dp))
                            if (tdee != null) {
                                val diff = avgCals - tdee
                                val diffColor = when {
                                    diff > 200 -> ExpenseRed
                                    diff < -300 -> Color(0xFFFFD740)
                                    else -> GreenPrimary
                                }
                                val diffLabel = when {
                                    diff > 200 -> "▲ ${diff} kcal acima do TDEE"
                                    diff < -300 -> "▼ ${-diff} kcal abaixo do TDEE"
                                    else -> "✓ alinhado com o TDEE"
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column {
                                        Text("Média: ~$avgCals kcal/dia", color = GreenPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text("Target TDEE: $tdee kcal/dia", color = TextDisabled, fontSize = 11.sp)
                                    }
                                    Box(
                                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                            .background(diffColor.copy(alpha = 0.12f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                    ) {
                                        Text(diffLabel, color = diffColor, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            } else {
                                Text("Média: ~$avgCals kcal/dia", color = GreenPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Spacer(Modifier.height(10.dp))
                            val macroTargetKcal = tdee ?: (avgCals * plan.days.size)
                            val proteinTargetG: Int
                            val carbsTargetG: Int
                            val fatTargetG: Int
                            when (dietMode) {
                                DietMode.HIGH_PROTEIN -> { proteinTargetG = (macroTargetKcal * 0.40 / 4).toInt(); carbsTargetG = (macroTargetKcal * 0.35 / 4).toInt(); fatTargetG = (macroTargetKcal * 0.25 / 9).toInt() }
                                DietMode.LOW_CARB -> { proteinTargetG = (macroTargetKcal * 0.35 / 4).toInt(); carbsTargetG = (macroTargetKcal * 0.15 / 4).toInt(); fatTargetG = (macroTargetKcal * 0.50 / 9).toInt() }
                                DietMode.MEDITERRANEAN -> { proteinTargetG = (macroTargetKcal * 0.20 / 4).toInt(); carbsTargetG = (macroTargetKcal * 0.50 / 4).toInt(); fatTargetG = (macroTargetKcal * 0.30 / 9).toInt() }
                                DietMode.BUDGET_MAX, DietMode.BALANCED, DietMode.FAMILY -> { proteinTargetG = (macroTargetKcal * 0.25 / 4).toInt(); carbsTargetG = (macroTargetKcal * 0.50 / 4).toInt(); fatTargetG = (macroTargetKcal * 0.25 / 9).toInt() }
                                else -> { proteinTargetG = (macroTargetKcal * 0.30 / 4).toInt(); carbsTargetG = (macroTargetKcal * 0.40 / 4).toInt(); fatTargetG = (macroTargetKcal * 0.30 / 9).toInt() }
                            }
                            val weeklyProteinTarget = proteinTargetG * plan.days.size
                            val weeklyCarbsTarget = carbsTargetG * plan.days.size
                            val weeklyFatTarget = fatTargetG * plan.days.size
                            listOf(
                                Triple("Proteína", totalProtein.toInt() to weeklyProteinTarget, Color(0xFF64B5F6)),
                                Triple("Carbs", totalCarbs.toInt() to weeklyCarbsTarget, Color(0xFFFFD740)),
                                Triple("Gordura", totalFat.toInt() to weeklyFatTarget, Color(0xFFFF8A65)),
                            ).forEach { (label, valuePair, color) ->
                                val (value, target) = valuePair
                                val fraction = if (target > 0) (value.toFloat() / target.toFloat()).coerceIn(0f, 1f) else value.toFloat() / maxOf(weeklyProteinTarget, weeklyCarbsTarget, weeklyFatTarget).toFloat().coerceAtLeast(1f)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(label, color = TextDisabled, fontSize = 11.sp, modifier = Modifier.width(70.dp))
                                    Box(modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(TextDisabled.copy(alpha = 0.15f))) {
                                        Box(modifier = Modifier.fillMaxWidth(fraction).height(6.dp).clip(RoundedCornerShape(3.dp)).background(color))
                                    }
                                    Spacer(Modifier.width(6.dp))
                                    Text("${value}/${target}g", color = color, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(62.dp))
                                }
                                Spacer(Modifier.height(5.dp))
                            }
                        }
                    }
                }
            }
            if (plan.days.isNotEmpty()) {
                item {
                    WeeklyCalorieChart(
                        days = plan.days,
                        planId = planId,
                        eatenMeals = eatenMeals,
                    )
                }
            }
            item {
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(null to "Todos", 15 to "≤15 min", 30 to "≤30 min").forEach { (mins, label) ->
                        FilterChip(
                            selected = prepTimeFilter == mins,
                            onClick = { onSetPrepTimeFilter(mins) },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GreenPrimary.copy(alpha = 0.15f),
                                selectedLabelColor = GreenPrimary,
                                labelColor = TextSecondary,
                            ),
                        )
                    }
                }
            }
            // "Today" pinned card
            val todayDow2 = todayDowFromJava()
            val todayDay = plan.days.find { it.dayOfWeek == todayDow2 }
            if (todayDay != null) {
                item {
                    val bf = remember(todayDay.breakfast) { parseMeal(todayDay.breakfast) }
                    val lu = remember(todayDay.lunch) { parseMeal(todayDay.lunch) }
                    val di = remember(todayDay.dinner) { parseMeal(todayDay.dinner) }
                    val todayColor = DAY_COLORS.getOrElse(todayDow2) { GreenPrimary }
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onDayClick(todayDay) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = todayColor.copy(alpha = 0.08f)),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📅", fontSize = 16.sp)
                                Spacer(Modifier.width(8.dp))
                                Text("Hoje — ${DAY_FULL_NAMES.getOrElse(todayDow2) { "Hoje" }}", color = todayColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(Modifier.weight(1f))
                                todayDay.calories?.let { Text("🔥 $it kcal", color = todayColor, fontSize = 12.sp) }
                            }
                            Spacer(Modifier.height(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                bf?.let { Text("☀️ ${it.name}", color = TextPrimary, fontSize = 12.sp) }
                                lu?.let { Text("🌤 ${it.name}", color = TextSecondary, fontSize = 12.sp) }
                                    ?: Text("🌤 Almoço fora de casa", color = TextDisabled, fontSize = 12.sp)
                                di?.let { Text("🌙 ${it.name}", color = TextSecondary, fontSize = 12.sp) }
                                    ?: Text("🌙 Jantar fora de casa", color = TextDisabled, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            val costPerDay = (plan.shoppingList?.totalEstimate ?: 0.0).let { total ->
                if (plan.days.isNotEmpty()) total / plan.days.size else null
            }
            val filteredDays = plan.days.sortedBy { it.dayOfWeek }.filter { day ->
                if (prepTimeFilter == null) true
                else {
                    val meals = listOfNotNull(parseMeal(day.breakfast), parseMeal(day.lunch), parseMeal(day.dinner))
                    meals.isNotEmpty() && meals.all { meal ->
                        val mins = meal.prepTime?.replace(Regex("[^0-9]"), "")?.toIntOrNull()
                        mins == null || mins <= prepTimeFilter
                    }
                }
            }
            items(filteredDays) { day ->
                DayCard(day = day, scheduleType = scheduleByDay[day.dayOfWeek]?.dayType, costPerDay = costPerDay, planId = planId, eatenMeals = eatenMeals, onClick = { onDayClick(day) })
            }
        }
        if (!tips.isNullOrBlank() && plan != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = GreenPrimary.copy(alpha = 0.07f)),
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                        Text("💡", fontSize = 18.sp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Dica da IA", color = GreenPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(tips, color = TextSecondary, fontSize = 12.sp, lineHeight = 18.sp)
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
internal fun DayCard(day: MealPlanDayDto, scheduleType: String?, costPerDay: Double? = null, planId: Int = 0, eatenMeals: Set<String> = emptySet(), onClick: () -> Unit) {
    val breakfast = remember(day.breakfast) { parseMeal(day.breakfast) }
    val lunch = remember(day.lunch) { parseMeal(day.lunch) }
    val dinner = remember(day.dinner) { parseMeal(day.dinner) }
    val hasSnack = remember(day.snacks) { parseSnack(day.snacks) != null }
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
                    val currencyConfigForDay = LocalCurrencyConfig.current
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
                        costPerDay?.let {
                            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF64B5F6).copy(alpha = 0.1f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text("~${currencyConfigForDay.format(it)}/dia", color = Color(0xFF64B5F6), fontSize = 10.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        val bfEaten = "${planId}_${day.dayOfWeek}_breakfast" in eatenMeals
                        val luEaten = "${planId}_${day.dayOfWeek}_lunch" in eatenMeals
                        val diEaten = "${planId}_${day.dayOfWeek}_dinner" in eatenMeals
                        if (breakfast != null) {
                            val kcalStr = if (breakfast.calories > 0) " · ${breakfast.calories}k" else ""
                            MacroChip("${if (bfEaten) "✓ " else "☀️ "}${breakfast.name.take(14)}$kcalStr", if (bfEaten) GreenPrimary else dayColor)
                        }
                        if (lunch != null) {
                            val kcalStr = if (lunch.calories > 0) " · ${lunch.calories}k" else ""
                            MacroChip("${if (luEaten) "✓ " else "🌤 "}${lunch.name.take(14)}$kcalStr", if (luEaten) GreenPrimary else Color(0xFF64B5F6))
                        } else if (scheduleType == "WORK" || scheduleType == "HALF_OFF") MacroChip("🌤 Fora", TextDisabled)
                        if (dinner != null) {
                            val kcalStr = if (dinner.calories > 0) " · ${dinner.calories}k" else ""
                            MacroChip("${if (diEaten) "✓ " else "🌙 "}${dinner.name.take(14)}$kcalStr", if (diEaten) GreenPrimary else Color(0xFFCE93D8))
                        } else if (scheduleType == "WORK") MacroChip("🌙 Fora", TextDisabled)
                        if (hasSnack) MacroChip("🍎 Lanche", Color(0xFFFFD740))
                    }
                }
                Text("›", color = TextDisabled, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

@Composable
internal fun WeeklyCalorieChart(
    days: List<MealPlanDayDto>,
    planId: Int,
    eatenMeals: Set<String>,
) {
    if (days.isEmpty()) return
    val sortedDays = days.sortedBy { it.dayOfWeek }
    val maxCal = sortedDays.maxOfOrNull { it.calories ?: 0 }?.takeIf { it > 0 } ?: return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("📊 Calorias por dia", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().height(90.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom,
            ) {
                sortedDays.forEach { day ->
                    val totalCal = day.calories ?: 0
                    val mealTypes = listOf("breakfast", "lunch", "dinner")
                    val eatenCount = mealTypes.count { type -> "${planId}_${day.dayOfWeek}_$type" in eatenMeals }
                    val eatenFraction = (eatenCount.toFloat() / 3f).coerceIn(0f, 1f)
                    val barFrac = (totalCal.toFloat() / maxCal.toFloat()).coerceIn(0.04f, 1f)
                    val dayColor = DAY_COLORS.getOrElse(day.dayOfWeek) { GreenPrimary }

                    Column(
                        modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        if (totalCal > 0) {
                            Text(
                                "${totalCal}",
                                color = dayColor.copy(alpha = 0.65f),
                                fontSize = 7.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((72 * barFrac).dp)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(dayColor.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            if (eatenFraction > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(eatenFraction)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(dayColor),
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            // X axis labels
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                sortedDays.forEach { day ->
                    Text(
                        DAY_NAMES.getOrElse(day.dayOfWeek) { "?" },
                        color = TextDisabled,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            // Legend
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(GreenPrimary))
                    Text("Comido", color = TextDisabled, fontSize = 10.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(GreenPrimary.copy(alpha = 0.18f)))
                    Text("Planeado", color = TextDisabled, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
internal fun OnboardingStepRow(step: Int, title: String, subtitle: String, accentColor: Color) {
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
