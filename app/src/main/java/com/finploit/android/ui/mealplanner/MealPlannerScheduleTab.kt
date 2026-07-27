package com.finploit.android.ui.mealplanner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finploit.android.data.dto.ScheduleItemDto
import com.finploit.android.ui.theme.BackgroundDark
import com.finploit.android.ui.theme.CardBackground
import com.finploit.android.ui.theme.CardElevated
import com.finploit.android.ui.theme.ExpenseRed
import com.finploit.android.ui.theme.GreenPrimary
import com.finploit.android.ui.theme.IncomeGreen
import com.finploit.android.ui.theme.TextDisabled
import com.finploit.android.ui.theme.TextPrimary
import com.finploit.android.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScheduleTab(
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
    dietaryPreferences: Set<String> = emptySet(),
    mealPrepMode: Boolean = false,
    onToggleDietaryPref: (String) -> Unit = {},
    onSetMealPrepMode: (Boolean) -> Unit = {},
    breakfastAtWork: Set<Int> = emptySet(),
    onToggleBreakfastAtWork: (Int) -> Unit = {},
) {
    val breakfastStates = remember(schedule, breakfastAtWork) {
        (0..6).map { day -> mutableStateOf(day in breakfastAtWork) }
    }
    val lunchAtWork = remember(schedule) {
        val map = schedule.associateBy { it.dayOfWeek }
        (0..6).map { day ->
            val item = map[day]
            val value = when {
                item == null -> false
                item.lunchAtWork -> true
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
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = profileHeight,
                            onValueChange = onHeightChange,
                            label = { Text("Altura (cm)", fontSize = 12.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
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
                            colors = OutlinedTextFieldDefaults.colors(
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
                        val tdee = computeTdee(h, w, profileActivityLevel)
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
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = CardBackground)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🥗", fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Preferências Alimentares", color = GreenPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("A IA nunca vai sugerir ingredientes que não podes comer.", color = TextDisabled, fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    val prefs = listOf(
                        "vegetarian" to "🥦 Vegetariano",
                        "vegan" to "🌱 Vegano",
                        "lactose_free" to "🥛 Sem lactose",
                        "gluten_free" to "🌾 Sem glúten",
                        "nut_free" to "🥜 Sem frutos secos",
                    )
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        prefs.forEach { (key, label) ->
                            val selected = key in dietaryPreferences
                            FilterChip(
                                selected = selected,
                                onClick = { onToggleDietaryPref(key) },
                                label = { Text(label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GreenPrimary.copy(alpha = 0.15f),
                                    selectedLabelColor = GreenPrimary,
                                    labelColor = TextSecondary,
                                ),
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (mealPrepMode) GreenPrimary.copy(alpha = 0.08f) else CardElevated)
                            .border(1.dp, if (mealPrepMode) GreenPrimary.copy(alpha = 0.4f) else TextDisabled.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                            .clickable { onSetMealPrepMode(!mealPrepMode) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("🍳", fontSize = 18.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Modo Meal Prep", color = if (mealPrepMode) GreenPrimary else TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Ao domingo cozinhar para a semana toda — menos tempo na cozinha", color = TextDisabled, fontSize = 11.sp, lineHeight = 15.sp)
                        }
                        Box(
                            modifier = Modifier.size(28.dp).clip(CircleShape)
                                .background(if (mealPrepMode) GreenPrimary.copy(alpha = 0.2f) else TextDisabled.copy(alpha = 0.1f))
                                .border(1.5.dp, if (mealPrepMode) GreenPrimary else TextDisabled.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (mealPrepMode) Text("✓", color = GreenPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
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
            val isBreakfast = breakfastStates[day]
            val isLunch = lunchAtWork[day]
            val isDinner = dinnerAtWork[day]
            val allOut = isBreakfast.value && isLunch.value && isDinner.value
            val allHome = !isBreakfast.value && !isLunch.value && !isDinner.value

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = CardBackground)) {
                Column(modifier = Modifier.padding(14.dp)) {
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
                                allOut -> "Todas as refeições fora 🏢"
                                isLunch.value && isDinner.value -> "Café em casa, almoço e jantar fora"
                                isLunch.value -> "Almoço fora, resto em casa"
                                isDinner.value -> "Jantar fora, resto em casa"
                                isBreakfast.value -> "Café fora, resto em casa"
                                else -> "Todas em casa 🏠"
                            }
                            Text(summary, color = if (allHome) GreenPrimary else Color(0xFF64B5F6), fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    MealLocationRow(
                        emoji = "☀️", label = "Café da manhã",
                        atWork = isBreakfast.value, locked = false,
                        onToggle = {
                            isBreakfast.value = !isBreakfast.value
                            onToggleBreakfastAtWork(day)
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                    MealLocationRow(emoji = "🌤", label = "Almoço", atWork = isLunch.value, locked = false, onToggle = { isLunch.value = !isLunch.value })
                    Spacer(Modifier.height(8.dp))
                    MealLocationRow(emoji = "🌙", label = "Jantar", atWork = isDinner.value, locked = false, onToggle = { isDinner.value = !isDinner.value })
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
internal fun MealLocationRow(
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
