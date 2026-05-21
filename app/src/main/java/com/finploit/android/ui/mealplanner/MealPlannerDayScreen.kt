package com.finploit.android.ui.mealplanner

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finploit.android.data.dto.MealDetailDto
import com.finploit.android.data.dto.MealPlanDayDto
import com.finploit.android.data.dto.SnackDetailDto
import com.finploit.android.ui.theme.BackgroundDark
import com.finploit.android.ui.theme.CardBackground
import com.finploit.android.ui.theme.CardElevated
import com.finploit.android.ui.theme.GreenPrimary
import com.finploit.android.ui.theme.IncomeGreen
import com.finploit.android.ui.theme.TextDisabled
import com.finploit.android.ui.theme.TextPrimary
import com.finploit.android.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MealDayScreen(
    day: MealPlanDayDto,
    scheduleType: String?,
    lunchAtWork: Boolean = false,
    dinnerAtWork: Boolean = false,
    planId: Int = 0,
    eatenMeals: Set<String> = emptySet(),
    mealRatings: Map<String, Int> = emptyMap(),
    onToggleEaten: (String) -> Unit = {},
    onRateMeal: (String, Int) -> Unit = { _, _ -> },
    onBack: () -> Unit,
) {
    val breakfast = parseMeal(day.breakfast)
    val lunch = parseMeal(day.lunch)
    val dinner = parseMeal(day.dinner)
    val dayColor = DAY_COLORS.getOrElse(day.dayOfWeek) { GreenPrimary }
    val typeColor = scheduleType?.let { DAY_TYPE_COLORS[it] } ?: TextDisabled
    val context = LocalContext.current

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
            actions = {
                IconButton(onClick = {
                    val text = buildString {
                        append("🥗 ${DAY_FULL_NAMES.getOrElse(day.dayOfWeek) { "Dia" }} — Plano Alimentar\n\n")
                        breakfast?.let { append("☀️ Café: ${it.name}\n${it.ingredients.joinToString(", ")}\n\n") }
                        lunch?.let { append("🌤 Almoço: ${it.name}\n${it.ingredients.joinToString(", ")}\n\n") }
                        dinner?.let { append("🌙 Jantar: ${it.name}\n${it.ingredients.joinToString(", ")}\n\n") }
                    }
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(Intent.createChooser(intent, "Partilhar receitas"))
                }) {
                    Icon(Icons.Default.Share, contentDescription = "Partilhar", tint = GreenPrimary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { Spacer(Modifier.height(4.dp)) }

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
                    val key = "${planId}_${day.dayOfWeek}_breakfast"
                    SectionCard(label = "☀️ Café da Manhã", meal = breakfast, accentColor = dayColor)
                    MealFeedbackRow(mealKey = key, eatenMeals = eatenMeals, mealRatings = mealRatings, onToggleEaten = onToggleEaten, onRateMeal = onRateMeal)
                }
            }
            if (lunch != null) {
                item {
                    val key = "${planId}_${day.dayOfWeek}_lunch"
                    SectionCard(label = "🌤 Almoço", meal = lunch, accentColor = Color(0xFF64B5F6))
                    MealFeedbackRow(mealKey = key, eatenMeals = eatenMeals, mealRatings = mealRatings, onToggleEaten = onToggleEaten, onRateMeal = onRateMeal)
                }
            } else if (lunchAtWork) {
                item { OutsideRow("🌤 Almoço") }
            }
            if (dinner != null) {
                item {
                    val key = "${planId}_${day.dayOfWeek}_dinner"
                    SectionCard(label = "🌙 Jantar", meal = dinner, accentColor = Color(0xFFCE93D8))
                    MealFeedbackRow(mealKey = key, eatenMeals = eatenMeals, mealRatings = mealRatings, onToggleEaten = onToggleEaten, onRateMeal = onRateMeal)
                }
            } else if (dinnerAtWork) {
                item { OutsideRow("🌙 Jantar") }
            }
            parseSnack(day.snacks)?.let { snack ->
                item { SnackCard(snack = snack, accentColor = Color(0xFFFFD740)) }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
internal fun MealFeedbackRow(
    mealKey: String,
    eatenMeals: Set<String>,
    mealRatings: Map<String, Int>,
    onToggleEaten: (String) -> Unit,
    onRateMeal: (String, Int) -> Unit,
) {
    val eaten = mealKey in eatenMeals
    val rating = mealRatings[mealKey]
    Spacer(Modifier.height(6.dp))
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (eaten) GreenPrimary.copy(alpha = 0.18f) else CardBackground)
                .border(1.dp, if (eaten) GreenPrimary else TextDisabled.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .clickable { onToggleEaten(mealKey) }
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text(if (eaten) "✓ Comi" else "Comi?", color = if (eaten) GreenPrimary else TextDisabled, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (rating == 1) Color(0xFF64B5F6).copy(alpha = 0.18f) else CardBackground)
                .border(1.dp, if (rating == 1) Color(0xFF64B5F6) else TextDisabled.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .clickable { onRateMeal(mealKey, if (rating == 1) 0 else 1) }
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text("👍", fontSize = 14.sp)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (rating == -1) Color(0xFFEF5350).copy(alpha = 0.18f) else CardBackground)
                .border(1.dp, if (rating == -1) Color(0xFFEF5350) else TextDisabled.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .clickable { onRateMeal(mealKey, if (rating == -1) 0 else -1) }
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text("👎", fontSize = 14.sp)
        }
    }
}

@Composable
internal fun OutsideRow(label: String) {
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
internal fun SnackCard(snack: SnackDetailDto, accentColor: Color = Color(0xFFFFD740)) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
internal fun SectionCard(label: String, meal: MealDetailDto, accentColor: Color) {
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
