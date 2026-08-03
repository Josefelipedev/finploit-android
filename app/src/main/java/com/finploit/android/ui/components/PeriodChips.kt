package com.finploit.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finploit.android.ui.theme.BackgroundDark
import com.finploit.android.ui.theme.CardElevated
import com.finploit.android.ui.theme.GreenPrimary
import com.finploit.android.ui.theme.TextSecondary
import com.finploit.android.util.Period

/**
 * O recorte de tempo, em botões.
 *
 * A web tem um seletor de duas datas; no telemóvel isso são dois calendários
 * para responder "os últimos 30 dias". Os recortes são os mesmos, e é o mesmo
 * componente no Dashboard e na Análise para os dois não divergirem outra vez.
 */
@Composable
fun PeriodChips(
    selected: Period,
    onSelect: (Period) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Period.entries.forEach { period ->
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
