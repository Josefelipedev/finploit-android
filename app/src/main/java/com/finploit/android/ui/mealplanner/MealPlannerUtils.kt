package com.finploit.android.ui.mealplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finploit.android.data.dto.MealDetailDto
import com.finploit.android.data.dto.SnackDetailDto
import com.finploit.android.ui.theme.GreenPrimary
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

internal val gson = Gson()

internal fun parseMeal(json: String?): MealDetailDto? = try {
    json?.let { gson.fromJson(it, MealDetailDto::class.java) }
} catch (e: Exception) { null }

internal fun parseSnack(json: String?): SnackDetailDto? {
    if (json.isNullOrBlank()) return null
    return try {
        val parsed = gson.fromJson(json, SnackDetailDto::class.java)
        if (parsed?.name.isNullOrBlank()) null else parsed
    } catch (e: JsonSyntaxException) {
        SnackDetailDto(name = json.trim())
    }
}

internal val SUPERMARKET_SECTION_ORDER = listOf(
    "Frutas e Legumes", "Fruta", "Legumes", "Verduras", "Vegetais",
    "Laticínios", "Lacticínios", "Iogurte", "Queijo",
    "Carnes e Peixe", "Proteína", "Carnes", "Peixe",
    "Padaria", "Cereais", "Grãos", "Grãos e Cereais",
    "Mercearia", "Conservas", "Condimentos",
    "Bebidas", "Bebida",
    "Outros",
)

internal fun categorySortKey(cat: String): Int {
    val idx = SUPERMARKET_SECTION_ORDER.indexOfFirst { it.equals(cat.trim(), ignoreCase = true) }
    return if (idx < 0) SUPERMARKET_SECTION_ORDER.size else idx
}

internal val DAY_NAMES = listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb")
internal val DAY_FULL_NAMES = listOf("Domingo", "Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado")
internal val DAY_EMOJIS = listOf("🌅", "💼", "💼", "💼", "💼", "🎉", "😴")
internal val DAY_COLORS = listOf(
    Color(0xFF69F0AE), Color(0xFF00C853), Color(0xFF00BFA5),
    Color(0xFF64B5F6), Color(0xFFFFD740), Color(0xFFFF8A65), Color(0xFFCE93D8),
)
internal val DAY_TYPE_LABELS = mapOf("WORK" to "Trabalho", "OFF" to "Folga", "HALF_OFF" to "Meia-folga")
internal val DAY_TYPE_COLORS = mapOf(
    "WORK" to Color(0xFF64B5F6), "OFF" to Color(0xFF69F0AE), "HALF_OFF" to Color(0xFFFFD740),
)

@Composable
internal fun MacroChip(text: String, color: Color = GreenPrimary) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
