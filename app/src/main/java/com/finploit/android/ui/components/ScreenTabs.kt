package com.finploit.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finploit.android.ui.theme.BackgroundDark
import com.finploit.android.ui.theme.GreenPrimary
import com.finploit.android.ui.theme.TextDisabled

/**
 * A barra de abas de um ecrã.
 *
 * Nasceu de o Planeamento ter sido dissolvido: as abas dele foram para casa
 * (Metas, Orçamento, Análise) e a mesma `TabRow` ia ser copiada para três
 * sítios. Três cópias divergem — é assim que uma delas acaba com outra cor de
 * indicador e ninguém sabe qual está certa.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenTabs(
    tabs: List<String>,
    modifier: Modifier = Modifier,
    content: @Composable (Int) -> Unit,
) {
    var tabIndex by remember { mutableStateOf(0) }

    Column(modifier.fillMaxSize().background(BackgroundDark)) {
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
        content(tabIndex)
    }
}
