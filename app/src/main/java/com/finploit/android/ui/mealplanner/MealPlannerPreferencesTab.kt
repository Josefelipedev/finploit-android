package com.finploit.android.ui.mealplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finploit.android.data.dto.PreferenceOptionDto
import com.finploit.android.ui.theme.CardBackground
import com.finploit.android.ui.theme.CardElevated
import com.finploit.android.ui.theme.ExpenseRed
import com.finploit.android.ui.theme.GreenPrimary
import com.finploit.android.ui.theme.TextDisabled
import com.finploit.android.ui.theme.TextPrimary

/** "2.5" instead of "2.5000000001"; whole numbers drop the decimal. */
fun formatServings(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

private val FAVORITE_SUGGESTIONS = listOf(
    "Frango", "Atum", "Ovos", "Bacalhau", "Arroz", "Massa",
    "Batata doce", "Feijão", "Brócolos", "Lasanha", "Feijoada", "Omelete",
)

private val DISLIKE_SUGGESTIONS = listOf(
    "Beringela", "Coentros", "Fígado", "Cogumelos", "Azeitonas", "Picante",
)

@Composable
private fun SectionCard(
    emoji: String,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(title, color = GreenPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(subtitle, color = TextDisabled, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun CounterRow(label: String, hint: String, value: Int, min: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(hint, color = TextDisabled, fontSize = 11.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StepButton("−", enabled = value > min) { onChange(value - 1) }
            Text(
                value.toString(),
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.width(24.dp),
            )
            StepButton("+", enabled = value < 12) { onChange(value + 1) }
        }
    }
}

@Composable
private fun StepButton(symbol: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(if (enabled) GreenPrimary.copy(alpha = 0.12f) else TextDisabled.copy(alpha = 0.06f))
            .border(1.dp, if (enabled) GreenPrimary.copy(alpha = 0.4f) else TextDisabled.copy(alpha = 0.2f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, color = if (enabled) GreenPrimary else TextDisabled, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun OptionRow(option: PreferenceOptionDto, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) GreenPrimary.copy(alpha = 0.08f) else CardElevated)
            .border(
                1.dp,
                if (selected) GreenPrimary.copy(alpha = 0.4f) else TextDisabled.copy(alpha = 0.15f),
                RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            option.label,
            color = if (selected) GreenPrimary else TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (selected) GreenPrimary.copy(alpha = 0.2f) else TextDisabled.copy(alpha = 0.08f))
                .border(1.5.dp, if (selected) GreenPrimary else TextDisabled.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Text("✓", color = GreenPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TagEditor(
    items: List<String>,
    suggestions: List<String>,
    placeholder: String,
    accent: androidx.compose.ui.graphics.Color,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    var draft by rememberSaveable { mutableStateOf("") }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            label = { Text(placeholder, fontSize = 11.sp) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accent.copy(alpha = 0.7f),
                unfocusedBorderColor = TextDisabled.copy(alpha = 0.4f),
                focusedLabelColor = accent.copy(alpha = 0.7f),
                unfocusedLabelColor = TextDisabled,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = accent,
            ),
        )
        IconButton(
            onClick = {
                if (draft.isNotBlank()) {
                    onAdd(draft.trim())
                    draft = ""
                }
            },
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(GreenPrimary.copy(alpha = 0.15f)),
        ) {
            Icon(Icons.Default.Add, contentDescription = "Adicionar", tint = GreenPrimary)
        }
    }

    if (items.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
        FlowChips(items) { item ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(alpha = 0.12f))
                    .clickable { onRemove(item) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(item, color = accent, fontSize = 12.sp)
                Spacer(Modifier.width(6.dp))
                Text("✕", color = accent.copy(alpha = 0.7f), fontSize = 11.sp)
            }
        }
    }

    val remaining = suggestions.filter { s -> items.none { it.equals(s, ignoreCase = true) } }
    if (remaining.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
        FlowChips(remaining.take(8)) { suggestion ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, TextDisabled.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .clickable { onAdd(suggestion) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text("+ $suggestion", color = TextDisabled, fontSize = 12.sp)
            }
        }
    }
}

/** Minimal wrap layout — avoids depending on the experimental FlowRow. */
@Composable
private fun <T> FlowChips(items: List<T>, chip: @Composable (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { chip(it) }
            }
        }
    }
}

@Composable
fun MealPlannerPreferencesTab(
    adults: Int,
    children: Int,
    servings: Double,
    cuisineStyle: String,
    dietGoal: String,
    cuisineOptions: List<PreferenceOptionDto>,
    dietGoalOptions: List<PreferenceOptionDto>,
    favoriteFoods: List<String>,
    dislikedFoods: List<String>,
    onSetHousehold: (Int, Int) -> Unit,
    onSetCuisineStyle: (String) -> Unit,
    onSetDietGoal: (String) -> Unit,
    onAddFavoriteFood: (String) -> Unit,
    onRemoveFavoriteFood: (String) -> Unit,
    onAddDislikedFood: (String) -> Unit,
    onRemoveDislikedFood: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 14.dp),
    ) {
        item {
            SectionCard("🏠", "Quem come em casa", "As receitas e as compras são multiplicadas para a casa toda.") {
                CounterRow("Adultos", "Porção inteira cada", adults, min = 1) { onSetHousehold(it, children) }
                CounterRow("Crianças", "Contam como meia porção", children, min = 0) { onSetHousehold(adults, it) }
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(GreenPrimary.copy(alpha = 0.08f))
                        .padding(12.dp),
                ) {
                    Text(
                        buildString {
                            append("Cada refeição rende ${formatServings(servings)} ")
                            append(if (servings == 1.0) "porção" else "porções")
                            if (children > 0) append(" — e o cardápio evita picante por causa das crianças")
                        },
                        color = GreenPrimary,
                        fontSize = 12.sp,
                    )
                }
            }
        }

        item {
            SectionCard("🍳", "Tipo de comida", "A tradição culinária do cardápio.") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    cuisineOptions.forEach { option ->
                        OptionRow(option, option.value == cuisineStyle) { onSetCuisineStyle(option.value) }
                    }
                }
            }
        }

        item {
            SectionCard("🎯", "Objetivo", "Combina com qualquer tipo de comida.") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    dietGoalOptions.forEach { option ->
                        OptionRow(option, option.value == dietGoal) { onSetDietGoal(option.value) }
                    }
                }
            }
        }

        item {
            SectionCard("⭐", "O que eu gosto", "Metade das refeições da semana vai usar algo daqui.") {
                TagEditor(
                    items = favoriteFoods,
                    suggestions = FAVORITE_SUGGESTIONS,
                    placeholder = "Ex: frango, lasanha, bacalhau...",
                    accent = GreenPrimary,
                    onAdd = onAddFavoriteFood,
                    onRemove = onRemoveFavoriteFood,
                )
            }
        }

        item {
            SectionCard("🚫", "Alimentos que não gosto", "A IA nunca inclui estes alimentos.") {
                TagEditor(
                    items = dislikedFoods,
                    suggestions = DISLIKE_SUGGESTIONS,
                    placeholder = "Ex: cebola, fígado, coentro...",
                    accent = ExpenseRed,
                    onAdd = onAddDislikedFood,
                    onRemove = onRemoveDislikedFood,
                )
            }
        }
    }
}
