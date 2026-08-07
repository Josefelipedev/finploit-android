package com.finploit.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finploit.android.data.dto.LinkedBillDto
import com.finploit.android.ui.theme.GreenPrimary

/**
 * Diz que este lançamento **quita uma conta** (B5).
 *
 * O caminho inverso já existia — a conta mostra que veio de uma recorrente.
 * Deste lado não havia nada: em Transações, a renda de 500 € era uma linha
 * igual às outras, e apagá-la reabria a conta em silêncio.
 */
@Composable
fun BillChip(bill: LinkedBillDto?, modifier: Modifier = Modifier) {
    if (bill == null) return

    Box(
        modifier = modifier
            .background(GreenPrimary.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            "Conta ${bill.dueLabel}",
            color = GreenPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
