package com.finploit.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finploit.android.data.dto.FinanceOriginDto

/**
 * De onde veio um lançamento que a app criou sozinha (T6.6).
 *
 * Cinco módulos emitem lançamentos — contas a pagar, metas, listas de compras,
 * cardápio, e a mão do utilizador — e na lista eram todos linhas iguais. Sem
 * isto não há como ver que a mesma compra foi contada duas vezes, uma pela
 * lista fechada e outra à mão. A app não adivinha duplicados: mostra a origem
 * e deixa a pessoa ver.
 *
 * O caso da conta a pagar tem crachá próprio (`BillChip`), que mostra também o
 * vencimento.
 */
@Composable
fun OriginChip(origin: FinanceOriginDto?, modifier: Modifier = Modifier) {
    if (origin == null || origin.kind == "bill") return

    Box(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            origin.chipLabel,
            color = Color.Gray,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
