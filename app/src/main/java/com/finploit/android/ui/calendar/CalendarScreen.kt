package com.finploit.android.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finploit.android.ui.theme.*
import java.util.Calendar

private val MONTH_NAMES = arrayOf("Janeiro","Fevereiro","Março","Abril","Maio","Junho",
    "Julho","Agosto","Setembro","Outubro","Novembro","Dezembro")
private val DAY_NAMES = listOf("Dom","Seg","Ter","Qua","Qui","Sex","Sáb")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        TopAppBar(
            title = { Text("Calendário", fontWeight = FontWeight.Bold, color = TextPrimary) },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = TextSecondary) }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark),
        )

        // Month navigation
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = viewModel::previousMonth) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Anterior", tint = TextSecondary)
            }
            Text(
                "${MONTH_NAMES[state.selectedMonth - 1]} ${state.selectedYear}",
                color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp,
            )
            IconButton(onClick = viewModel::nextMonth) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Próximo", tint = TextSecondary)
            }
        }

        // Day headers
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            DAY_NAMES.forEach { day ->
                Text(day, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = TextDisabled, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(4.dp))

        // Calendar grid
        val firstDayOfMonth = Calendar.getInstance().apply {
            set(state.selectedYear, state.selectedMonth - 1, 1)
        }.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sun
        val daysInMonth = Calendar.getInstance().apply {
            set(Calendar.YEAR, state.selectedYear)
            set(Calendar.MONTH, state.selectedMonth - 1)
        }.getActualMaximum(Calendar.DAY_OF_MONTH)

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).height(260.dp),
            userScrollEnabled = false,
        ) {
            items(firstDayOfMonth) { Box(Modifier.size(40.dp)) }
            items(daysInMonth) { dayIdx ->
                val day = dayIdx + 1
                val balance = state.dailyBalances[day]
                val hasTransactions = state.transactionsByDay.containsKey(day)
                val isSelected = state.selectedDay == day
                val dotColor = when {
                    balance == null -> Color.Transparent
                    balance >= 0 -> IncomeGreen
                    else -> ExpenseRed
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) GreenPrimary.copy(alpha = 0.3f) else Color.Transparent)
                        .border(if (isSelected) 1.dp else 0.dp, if (isSelected) GreenPrimary else Color.Transparent, CircleShape)
                        .clickable(enabled = hasTransactions) { viewModel.selectDay(day) },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(day.toString(), color = if (hasTransactions) TextPrimary else TextDisabled, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        if (hasTransactions) {
                            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(dotColor))
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = TextDisabled.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))

        // Selected day transactions
        val dayTransactions = state.selectedDay?.let { state.transactionsByDay[it] } ?: emptyList()
        if (state.selectedDay != null) {
            Text(
                "Transações — dia ${state.selectedDay}",
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        } else {
            Box(Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                Text("Toca num dia com ● para ver transações", color = TextDisabled, fontSize = 13.sp)
            }
        }
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(dayTransactions) { tx ->
                val isIncome = tx.type == "income"
                val color = if (isIncome) IncomeGreen else ExpenseRed
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isIncome) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = null, tint = color, modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(tx.description ?: tx.type ?: "", color = TextPrimary, modifier = Modifier.weight(1f), fontSize = 14.sp)
                        // Cada linha na moeda em que foi lançada — o "€" fixo
                        // dava um lançamento em reais como se fosse em euros.
                        Text(
                            currencyConfigByCode(tx.currency ?: LocalCurrencyConfig.current.code)
                                .format(tx.amount ?: 0.0),
                            color = color,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}
