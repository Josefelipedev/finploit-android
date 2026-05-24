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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    planDays: List<MealPlanDayDto> = emptyList(),
    eatenMeals: Set<String> = emptySet(),
    mealRatings: Map<String, Int> = emptyMap(),
    favoriteMeals: Set<String> = emptySet(),
    lockedMeals: Set<String> = emptySet(),
    mealNotes: Map<String, String> = emptyMap(),
    isSubstituting: String? = null,
    onToggleEaten: (String) -> Unit = {},
    onRateMeal: (String, Int) -> Unit = { _, _ -> },
    onToggleFavorite: (String) -> Unit = {},
    onToggleLocked: (String) -> Unit = {},
    onSetNote: (String, String) -> Unit = { _, _ -> },
    // Melhoria #4: preferences param added
    onSubstituteMeal: (Int, String, String?) -> Unit = { _, _, _ -> },
    onBack: () -> Unit,
    onNavigateDay: (MealPlanDayDto) -> Unit = {},
) {
    val breakfast = parseMeal(day.breakfast)
    val lunch = parseMeal(day.lunch)
    val dinner = parseMeal(day.dinner)
    val dayColor = DAY_COLORS.getOrElse(day.dayOfWeek) { GreenPrimary }
    val typeColor = scheduleType?.let { DAY_TYPE_COLORS[it] } ?: TextDisabled
    val context = LocalContext.current

    // Caloric progress
    val bfKey = "${planId}_${day.dayOfWeek}_breakfast"
    val luKey = "${planId}_${day.dayOfWeek}_lunch"
    val diKey = "${planId}_${day.dayOfWeek}_dinner"
    val eatenCalories = (if (bfKey in eatenMeals) breakfast?.calories ?: 0 else 0) +
            (if (luKey in eatenMeals) lunch?.calories ?: 0 else 0) +
            (if (diKey in eatenMeals) dinner?.calories ?: 0 else 0)
    val totalCalories = day.calories ?: 0

    // Day navigation
    val sortedDays = planDays.sortedBy { it.dayOfWeek }
    val currentIndex = sortedDays.indexOfFirst { it.dayOfWeek == day.dayOfWeek }
    val prevDay = if (currentIndex > 0) sortedDays[currentIndex - 1] else null
    val nextDay = if (currentIndex < sortedDays.size - 1) sortedDays[currentIndex + 1] else null

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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = TextPrimary)
                }
            },
            actions = {
                prevDay?.let { prev ->
                    IconButton(onClick = { onNavigateDay(prev) }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = DAY_NAMES.getOrElse(prev.dayOfWeek) { "Anterior" }, tint = TextPrimary)
                    }
                }
                nextDay?.let { next ->
                    IconButton(onClick = { onNavigateDay(next) }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = DAY_NAMES.getOrElse(next.dayOfWeek) { "Próximo" }, tint = TextPrimary)
                    }
                }
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

            // Caloric progress bar
            if (totalCalories > 0) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(dayColor.copy(alpha = 0.08f))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("🔥 $eatenCalories / $totalCalories kcal", color = dayColor, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            val pct = if (totalCalories > 0) (eatenCalories * 100 / totalCalories) else 0
                            Text("$pct%", color = dayColor.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                        LinearProgressIndicator(
                            progress = { (eatenCalories.toFloat() / totalCalories.toFloat()).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = dayColor,
                            trackColor = dayColor.copy(alpha = 0.15f),
                        )
                    }
                }
            }

            if (breakfast != null) {
                item {
                    val key = "${planId}_${day.dayOfWeek}_breakfast"
                    val subKey = "${day.id}_breakfast"
                    SectionCard(label = "☀️ Café da Manhã", meal = breakfast, accentColor = dayColor)
                    MealFeedbackRow(
                        mealKey = key,
                        eatenMeals = eatenMeals,
                        mealRatings = mealRatings,
                        favoriteMeals = favoriteMeals,
                        lockedMeals = lockedMeals,
                        mealNotes = mealNotes,
                        onToggleEaten = onToggleEaten,
                        onRateMeal = onRateMeal,
                        onToggleFavorite = onToggleFavorite,
                        onToggleLocked = onToggleLocked,
                        onSetNote = onSetNote,
                        isSubstituting = isSubstituting == subKey,
                        onSubstitute = { prefs -> onSubstituteMeal(day.id, "breakfast", prefs) },
                    )
                }
            }
            if (lunch != null) {
                item {
                    val key = "${planId}_${day.dayOfWeek}_lunch"
                    val subKey = "${day.id}_lunch"
                    SectionCard(label = "🌤 Almoço", meal = lunch, accentColor = Color(0xFF64B5F6))
                    MealFeedbackRow(
                        mealKey = key,
                        eatenMeals = eatenMeals,
                        mealRatings = mealRatings,
                        favoriteMeals = favoriteMeals,
                        lockedMeals = lockedMeals,
                        mealNotes = mealNotes,
                        onToggleEaten = onToggleEaten,
                        onRateMeal = onRateMeal,
                        onToggleFavorite = onToggleFavorite,
                        onToggleLocked = onToggleLocked,
                        onSetNote = onSetNote,
                        isSubstituting = isSubstituting == subKey,
                        onSubstitute = { prefs -> onSubstituteMeal(day.id, "lunch", prefs) },
                    )
                }
            } else if (lunchAtWork) {
                item { OutsideRow("🌤 Almoço") }
            }
            if (dinner != null) {
                item {
                    val key = "${planId}_${day.dayOfWeek}_dinner"
                    val subKey = "${day.id}_dinner"
                    SectionCard(label = "🌙 Jantar", meal = dinner, accentColor = Color(0xFFCE93D8))
                    MealFeedbackRow(
                        mealKey = key,
                        eatenMeals = eatenMeals,
                        mealRatings = mealRatings,
                        favoriteMeals = favoriteMeals,
                        lockedMeals = lockedMeals,
                        mealNotes = mealNotes,
                        onToggleEaten = onToggleEaten,
                        onRateMeal = onRateMeal,
                        onToggleFavorite = onToggleFavorite,
                        onToggleLocked = onToggleLocked,
                        onSetNote = onSetNote,
                        isSubstituting = isSubstituting == subKey,
                        onSubstitute = { prefs -> onSubstituteMeal(day.id, "dinner", prefs) },
                    )
                }
            } else if (dinnerAtWork) {
                item { OutsideRow("🌙 Jantar") }
            }
            // Melhoria #5 — Snack feedback row
            parseSnack(day.snacks)?.let { snack ->
                item { SnackCard(snack = snack, accentColor = Color(0xFFFFD740)) }
                item {
                    val snackKey = "${planId}_${day.dayOfWeek}_snack"
                    MealFeedbackRow(
                        mealKey = snackKey,
                        eatenMeals = eatenMeals,
                        mealRatings = mealRatings,
                        favoriteMeals = favoriteMeals,
                        lockedMeals = lockedMeals,
                        mealNotes = mealNotes,
                        onToggleEaten = onToggleEaten,
                        onRateMeal = onRateMeal,
                        onToggleFavorite = onToggleFavorite,
                        onToggleLocked = onToggleLocked,
                        onSetNote = onSetNote,
                        isSubstituting = false,
                        onSubstitute = {},       // snacks can't be substituted
                        showSubstitute = false,  // hide Trocar button for snacks
                    )
                }
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
    favoriteMeals: Set<String> = emptySet(),
    lockedMeals: Set<String> = emptySet(),
    mealNotes: Map<String, String> = emptyMap(),
    onToggleEaten: (String) -> Unit,
    onRateMeal: (String, Int) -> Unit,
    onToggleFavorite: (String) -> Unit = {},
    onToggleLocked: (String) -> Unit = {},
    onSetNote: (String, String) -> Unit = { _, _ -> },
    isSubstituting: Boolean = false,
    onSubstitute: (String?) -> Unit = {},
    showSubstitute: Boolean = true,
) {
    val eaten = mealKey in eatenMeals
    val rating = mealRatings[mealKey]
    val isFavorite = mealKey in favoriteMeals
    val isLocked = mealKey in lockedMeals
    val note = mealNotes[mealKey]

    // Melhoria #4 — Substitution preferences dialog
    var showSubDialog by remember { mutableStateOf(false) }
    var subPrefs by remember { mutableStateOf("") }
    val quickSubPrefs = listOf("Mais barato", "Vegan", "Sem glúten", "Sem lacticínios", "Rápido")

    if (showSubDialog) {
        AlertDialog(
            onDismissRequest = { showSubDialog = false; subPrefs = "" },
            containerColor = CardBackground,
            title = { Text("Preferências para a troca", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("A IA vai considerar estas preferências ao sugerir uma nova refeição.", color = TextSecondary, fontSize = 12.sp)
                    // Quick chips
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        quickSubPrefs.take(3).forEach { pref ->
                            val sel = subPrefs.contains(pref)
                            FilterChip(
                                selected = sel,
                                onClick = {
                                    subPrefs = if (sel) subPrefs.replace(pref, "").trim().trimStart(',').trimEnd(',')
                                    else if (subPrefs.isBlank()) pref else "$subPrefs, $pref"
                                },
                                label = { Text(pref, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GreenPrimary.copy(alpha = 0.18f),
                                    selectedLabelColor = GreenPrimary,
                                    labelColor = TextSecondary,
                                ),
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        quickSubPrefs.drop(3).forEach { pref ->
                            val sel = subPrefs.contains(pref)
                            FilterChip(
                                selected = sel,
                                onClick = {
                                    subPrefs = if (sel) subPrefs.replace(pref, "").trim().trimStart(',').trimEnd(',')
                                    else if (subPrefs.isBlank()) pref else "$subPrefs, $pref"
                                },
                                label = { Text(pref, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GreenPrimary.copy(alpha = 0.18f),
                                    selectedLabelColor = GreenPrimary,
                                    labelColor = TextSecondary,
                                ),
                            )
                        }
                    }
                    OutlinedTextField(
                        value = subPrefs,
                        onValueChange = { subPrefs = it },
                        label = { Text("Outras preferências (opcional)", fontSize = 12.sp) },
                        placeholder = { Text("Ex: sem frutos do mar", fontSize = 12.sp, color = TextDisabled) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreenPrimary,
                            focusedLabelColor = GreenPrimary,
                            cursorColor = GreenPrimary,
                        ),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSubstitute(subPrefs.trim().ifBlank { null })
                        showSubDialog = false
                        subPrefs = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                ) { Text("Trocar", color = BackgroundDark, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showSubDialog = false; subPrefs = "" }) {
                    Text("Cancelar", color = TextDisabled)
                }
            },
        )
    }

    // Melhoria #10 — Note dialog
    var showNoteDialog by remember { mutableStateOf(false) }
    var noteInput by remember(note) { mutableStateOf(note ?: "") }

    if (showNoteDialog) {
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            containerColor = CardBackground,
            title = { Text("📝 Nota da refeição", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text("Nota pessoal", fontSize = 12.sp) },
                    placeholder = { Text("Ex: usei leite de aveia, adorei!", fontSize = 12.sp, color = TextDisabled) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenPrimary,
                        focusedLabelColor = GreenPrimary,
                        cursorColor = GreenPrimary,
                    ),
                )
            },
            confirmButton = {
                Button(
                    onClick = { onSetNote(mealKey, noteInput); showNoteDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                ) { Text("Guardar", color = BackgroundDark, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showNoteDialog = false }) { Text("Cancelar", color = TextDisabled) }
            },
        )
    }

    Spacer(Modifier.height(6.dp))
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // ✓ Comi
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
            // 🔄 Trocar (with preferences dialog)
            if (showSubstitute) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isLocked) CardBackground.copy(alpha = 0.5f) else CardBackground)
                        .border(1.dp, TextDisabled.copy(alpha = if (isLocked) 0.15f else 0.3f), RoundedCornerShape(8.dp))
                        .clickable(enabled = !isSubstituting && !isLocked) { showSubDialog = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    if (isSubstituting) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = GreenPrimary, strokeWidth = 2.dp)
                    } else {
                        Text(
                            if (isLocked) "🔒 Bloqueado" else "🔄 Trocar",
                            color = if (isLocked) TextDisabled.copy(alpha = 0.4f) else TextDisabled,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            // 📝 Nota
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (note != null) Color(0xFF90CAF9).copy(alpha = 0.12f) else CardBackground)
                    .border(1.dp, if (note != null) Color(0xFF90CAF9).copy(alpha = 0.4f) else TextDisabled.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .clickable { showNoteDialog = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text("📝", fontSize = 13.sp)
            }
            // 🔒 Lock / pin
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isLocked) Color(0xFFFF8A65).copy(alpha = 0.15f) else CardBackground)
                    .border(1.dp, if (isLocked) Color(0xFFFF8A65).copy(alpha = 0.5f) else TextDisabled.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .clickable { onToggleLocked(mealKey) }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
                Icon(
                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = if (isLocked) "Desbloquear" else "Bloquear",
                    tint = if (isLocked) Color(0xFFFF8A65) else TextDisabled,
                    modifier = Modifier.size(15.dp),
                )
            }
            // ⭐ Favorite
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isFavorite) Color(0xFFFFD740).copy(alpha = 0.18f) else CardBackground)
                    .border(1.dp, if (isFavorite) Color(0xFFFFD740) else TextDisabled.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .clickable { onToggleFavorite(mealKey) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(if (isFavorite) "⭐" else "☆", fontSize = 14.sp)
            }
            // 👍
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
            // 👎
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
        // Note preview line (tappable to edit)
        if (note != null) {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF90CAF9).copy(alpha = 0.07f))
                    .border(1.dp, Color(0xFF90CAF9).copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                    .clickable { showNoteDialog = true }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("📝", fontSize = 11.sp)
                Text(note.take(80) + if (note.length > 80) "…" else "", color = Color(0xFF90CAF9), fontSize = 11.sp, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun OutsideRow(label: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(4.dp).height(28.dp).clip(RoundedCornerShape(2.dp)).background(TextDisabled.copy(alpha = 0.3f)))
        Spacer(Modifier.width(10.dp))
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
            // Melhoria #1 — Fiber chip added alongside protein/carbs/fat
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MacroChip("🔥 ${meal.calories} kcal", IncomeGreen)
                if (meal.protein != null) MacroChip("P ${meal.protein.toInt()}g", Color(0xFF64B5F6))
                if (meal.carbs != null) MacroChip("C ${meal.carbs.toInt()}g", Color(0xFFFFD740))
                if (meal.fat != null) MacroChip("G ${meal.fat.toInt()}g", Color(0xFFFF8A65))
                if (meal.fiber != null) MacroChip("F ${meal.fiber.toInt()}g", Color(0xFF80CBC4))
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
                    Text("💡", fontSize = 14.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(it, color = accentColor.copy(alpha = 0.85f), fontSize = 12.sp, lineHeight = 17.sp)
                }
            }
        }
    }
}
