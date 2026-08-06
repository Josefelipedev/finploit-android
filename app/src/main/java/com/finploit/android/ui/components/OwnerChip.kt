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
import com.finploit.android.ui.theme.GreenPrimary
import com.finploit.android.ui.theme.LocalOwnerNaming
import com.finploit.android.ui.theme.TextSecondary

/**
 * Quem lançou o registo. Não desenha nada fora do workspace do casal.
 *
 * O do parceiro leva o acento lima e o próprio fica cinza: a pergunta que se
 * faz a uma lista partilhada é "o que aqui não fui eu que lancei", e é essa
 * que o olho tem de responder sem ler.
 */
@Composable
fun OwnerChip(userId: Int?, modifier: Modifier = Modifier) {
    val naming = LocalOwnerNaming.current
    val name = naming.nameOf(userId) ?: return
    val accent = if (naming.isMine(userId)) TextSecondary else GreenPrimary

    Box(
        modifier = modifier
            .background(accent.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(name, color = accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
