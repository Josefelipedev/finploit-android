package com.finploit.android.ui.analysis

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finploit.android.data.dto.CategorySummaryDto
import com.finploit.android.ui.theme.BackgroundDark
import com.finploit.android.ui.theme.CardBackground
import com.finploit.android.ui.theme.CardElevated
import com.finploit.android.ui.theme.ExpenseRed
import com.finploit.android.ui.theme.GreenPrimary
import com.finploit.android.ui.theme.Green80
import com.finploit.android.ui.theme.IncomeGreen
import com.finploit.android.ui.theme.SurfaceDark
import com.finploit.android.ui.theme.TextDisabled
import com.finploit.android.ui.theme.TextPrimary
import com.finploit.android.ui.theme.TextSecondary
import com.finploit.android.ui.theme.LocalCurrencyConfig
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(viewModel: AnalysisViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
    ) {
        TopAppBar(
            title = { Text("Análise IA", fontWeight = FontWeight.Bold, color = TextPrimary) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = BackgroundDark,
                titleContentColor = TextPrimary,
            ),
            actions = {
                IconButton(onClick = viewModel::loadAll) {
                    Icon(Icons.Default.Refresh, contentDescription = "Atualizar", tint = GreenPrimary)
                }
            }
        )

        PeriodChips(
            selected = uiState.period,
            onSelect = viewModel::setPeriod,
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // Insight IA
            item {
                InsightCard(
                    insight = uiState.insight,
                    isLoading = uiState.isLoadingInsight,
                    error = uiState.insightError,
                    onRefresh = viewModel::loadInsight,
                )
            }

            if (uiState.isLoadingAnalysis) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator(color = Green80) }
                }
            } else if (uiState.analysis != null) {
                val analysis = uiState.analysis!!
                val monthly = analysis.Monthly

                // Gráfico de barras mensal
                if (monthly != null) {
                    item {
                        SectionTitle("Fluxo Mensal")
                        Spacer(Modifier.height(8.dp))
                        MonthlyBarChart(
                            income = monthly.summary.income,
                            expense = monthly.summary.expense,
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            SummaryPill("Receitas", monthly.summary.income, IncomeGreen)
                            SummaryPill("Despesas", monthly.summary.expense, ExpenseRed)
                            val saldo = monthly.summary.income - monthly.summary.expense
                            SummaryPill("Saldo", saldo, if (saldo >= 0) IncomeGreen else ExpenseRed)
                        }
                    }
                }

                // Categorias
                if (analysis.categorySummary.isNotEmpty()) {
                    item { SectionTitle("Gastos por Categoria") }
                    items(analysis.categorySummary.sortedByDescending { it.expense }) { cat ->
                        CategoryRow(cat)
                    }
                }

                // Metas
                if (analysis.goals.isNotEmpty()) {
                    item {
                        SectionTitle("Metas (${analysis.goals.size})")
                        Spacer(Modifier.height(4.dp))
                        val completed = analysis.goals.count { it.status == "COMPLETED" }
                        Text(
                            "$completed de ${analysis.goals.size} concluídas",
                            color = Color.Gray,
                            fontSize = 13.sp,
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

/**
 * O recorte que este ecrã está a ver.
 *
 * Enquanto a API só sabia devolver o histórico todo, não havia escolha a
 * oferecer. Agora há, e o recorte é o mesmo que a web tem no seletor de datas —
 * com "Tudo" à mão, que é o que este ecrã sempre mostrou.
 */
@Composable
private fun PeriodChips(
    selected: AnalysisPeriod,
    onSelect: (AnalysisPeriod) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AnalysisPeriod.entries.forEach { period ->
            val isSelected = period == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) GreenPrimary else CardElevated)
                    .clickable { onSelect(period) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    period.label,
                    color = if (isSelected) BackgroundDark else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun InsightCard(
    insight: String?,
    isLoading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFF2563EB)))
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    "Insight da IA",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 15.sp,
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onRefresh, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Novo insight", tint = TextDisabled, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            when {
                isLoading -> {
                    // Shimmer placeholder lines
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Analisando suas finanças...",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic,
                        )
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(if (it == 2) 0.6f else 1f)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CardElevated),
                            )
                        }
                    }
                }
                error != null -> Column {
                    Text(
                        "Analisando suas finanças...",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontStyle = FontStyle.Italic,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "O insight estará disponível em breve. Toque em atualizar para tentar novamente.",
                        color = TextDisabled,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                    )
                }
                insight != null -> Text(
                    insight,
                    color = TextPrimary.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                )
                else -> Text(
                    "Toque em atualizar para gerar um insight personalizado.",
                    color = TextDisabled,
                    fontSize = 13.sp,
                    fontStyle = FontStyle.Italic,
                )
            }
        }
    }
}

@Composable
private fun MonthlyBarChart(income: Double, expense: Double) {
    val maxValue = max(income, expense).takeIf { it > 0 } ?: 1.0
    var animated by remember { mutableStateOf(false) }
    val incomeAnim by animateFloatAsState(
        targetValue = if (animated) (income / maxValue).toFloat() else 0f,
        animationSpec = tween(900),
        label = "income",
    )
    val expenseAnim by animateFloatAsState(
        targetValue = if (animated) (expense / maxValue).toFloat() else 0f,
        animationSpec = tween(900, delayMillis = 150),
        label = "expense",
    )
    LaunchedEffect(Unit) { animated = true }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            val barWidth = size.width * 0.18f
            val gap = size.width * 0.12f
            val totalWidth = barWidth * 2 + gap
            val startX = (size.width - totalWidth) / 2

            drawBar(startX, barWidth, incomeAnim, IncomeGreen, "Receita")
            drawBar(startX + barWidth + gap, barWidth, expenseAnim, ExpenseRed, "Despesa")
        }
    }
}

private fun DrawScope.drawBar(x: Float, width: Float, ratio: Float, color: Color, label: String) {
    val barHeight = size.height * ratio
    val top = size.height - barHeight
    drawRoundRect(
        color = color,
        topLeft = Offset(x, top),
        size = Size(width, barHeight),
        cornerRadius = CornerRadius(8f, 8f),
    )
}

@Composable
private fun SummaryPill(label: String, value: Double, color: Color) {
    val currency = LocalCurrencyConfig.current
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label, color = TextSecondary, fontSize = 11.sp)
            Text(currency.format(value), color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun CategoryRow(cat: CategorySummaryDto) {
    val currency = LocalCurrencyConfig.current
    val maxVal = max(cat.income, cat.expense).takeIf { it > 0 } ?: 1.0
    val expenseRatio = (cat.expense / maxVal).toFloat().coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    cat.category,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f),
                )
                Column(horizontalAlignment = Alignment.End) {
                    if (cat.expense > 0) Text(currency.format(cat.expense), color = ExpenseRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    if (cat.income > 0) Text(currency.format(cat.income), color = IncomeGreen, fontSize = 12.sp)
                }
            }
            if (cat.expense > 0) {
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(TextDisabled.copy(alpha = 0.2f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(expenseRatio)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(ExpenseRed),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        color = TextPrimary,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
    )
}
