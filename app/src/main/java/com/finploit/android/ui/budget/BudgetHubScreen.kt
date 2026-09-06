package com.finploit.android.ui.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finploit.android.ui.planning.YearPlanPanel
import com.finploit.android.ui.rules.RulesScreen
import com.finploit.android.ui.theme.BackgroundDark
import com.finploit.android.ui.theme.GreenPrimary
import com.finploit.android.ui.theme.TextDisabled
import com.finploit.android.ui.theme.TextPrimary
import com.finploit.android.ui.theme.TextSecondary

/**
 * O Orçamento, nos três andares em que se decide gastar.
 *
 * **Mês** é o tecto de cada categoria. **Ano** é a mesma decisão com outro
 * horizonte — viviam em ecrãs diferentes (este e o Planeamento) e escondiam-se
 * quando se contradiziam. **Regra** está um andar acima dos dois: não diz
 * quanto se pode gastar em cada sítio, diz que proporção do que entra vai para
 * o que não se evita, para o que se escolhe e para o que fica. É a pergunta
 * que somar tectos nunca responde, porque nenhum tecto olha para o rendimento.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetHubScreen(
    onBack: () -> Unit,
    /**
     * Em que aba abrir. O cartão do Dashboard promete "A tua regra →" e tinha
     * de aterrar nela — abrir nos limites do mês era não cumprir o que o
     * próprio botão dizia.
     */
    initialTab: Int = 0,
) {
    var tabIndex by remember { mutableStateOf(initialTab) }

    Column(Modifier.fillMaxSize().background(BackgroundDark)) {
        TopAppBar(
            title = { Text("Orçamento", fontWeight = FontWeight.Bold, color = TextPrimary) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = TextSecondary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark),
        )

        val tabs = listOf("Mês", "Ano", "Regra")
        TabRow(
            selectedTabIndex = tabIndex,
            containerColor = BackgroundDark,
            contentColor = GreenPrimary,
            indicator = { positions ->
                SecondaryIndicator(Modifier.tabIndicatorOffset(positions[tabIndex]), color = GreenPrimary)
            },
        ) {
            tabs.forEachIndexed { index, label ->
                Tab(selected = tabIndex == index, onClick = { tabIndex = index }) {
                    Text(
                        label,
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = if (tabIndex == index) GreenPrimary else TextDisabled,
                        fontSize = 13.sp,
                        fontWeight = if (tabIndex == index) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }

        when (tabIndex) {
            0 -> BudgetLimitsScreen(viewModel = hiltViewModel(), onBack = onBack, embedded = true)
            1 -> YearPlanPanel(viewModel = hiltViewModel())
            else -> RulesScreen(viewModel = hiltViewModel())
        }
    }
}
