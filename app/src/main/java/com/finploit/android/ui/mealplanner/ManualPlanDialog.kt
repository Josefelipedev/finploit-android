package com.finploit.android.ui.mealplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finploit.android.data.dto.ManualMealPlanDay
import com.finploit.android.data.dto.ManualShoppingItem
import com.finploit.android.ui.theme.CardBackground
import com.finploit.android.ui.theme.ExpenseRed
import com.finploit.android.ui.theme.GreenPrimary
import com.finploit.android.ui.theme.TextDisabled
import com.finploit.android.ui.theme.TextPrimary

private val DIAS = listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb")

/** Uma linha da lista, enquanto se está a escrever. */
private data class ItemEmEdicao(
    var nome: String = "",
    var quantidade: String = "",
    var unidade: String = "",
    var preco: String = "",
)

/**
 * Escrever o cardápio da semana à mão.
 *
 * Existe porque nem toda a semana precisa de IA: às vezes já se sabe o que se
 * vai comer, e gastar uma geração (e a espera) para escrever aquilo é absurdo.
 *
 * **Não é um caso à parte.** Passa pelo mesmo `savePlan` do servidor, portanto
 * tem lista de compras, fecha em despesa, desce o saldo da conta e conta para o
 * orçamento e para a sobra exactamente como um plano gerado. E os preços que
 * aqui se escrevem passam a ensinar a IA nas gerações seguintes.
 */
@Composable
fun ManualPlanDialog(
    isSaving: Boolean,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (List<ManualMealPlanDay>, List<ManualShoppingItem>, String?) -> Unit,
) {
    // Sete dias, cada um com três campos. Guardados por índice porque é assim
    // que o servidor os espera (`dayOfWeek` 0..6, domingo primeiro).
    val pequenos = remember { mutableStateListOf(*Array(7) { "" }) }
    val almocos = remember { mutableStateListOf(*Array(7) { "" }) }
    val jantares = remember { mutableStateListOf(*Array(7) { "" }) }
    val itens = remember { mutableStateListOf(ItemEmEdicao()) }
    var notas by remember { mutableStateOf("") }
    var diaAberto by remember { mutableStateOf(0) }

    val temAlgumaRefeicao = (0..6).any {
        pequenos[it].isNotBlank() || almocos[it].isNotBlank() || jantares[it].isNotBlank()
    }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        containerColor = CardBackground,
        title = {
            Column {
                Text("Escrever o cardápio", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(
                    "Sem IA. Preenche só o que quiseres — os dias vazios ficam vazios.",
                    color = TextDisabled,
                    fontSize = 11.sp,
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 460.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        DIAS.forEachIndexed { i, nome ->
                            val activo = i == diaAberto
                            val preenchido = pequenos[i].isNotBlank() ||
                                almocos[i].isNotBlank() || jantares[i].isNotBlank()
                            Text(
                                nome,
                                color = when {
                                    activo -> GreenPrimary
                                    preenchido -> TextPrimary
                                    else -> TextDisabled
                                },
                                fontWeight = if (activo) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (activo) GreenPrimary.copy(alpha = 0.15f)
                                        else TextDisabled.copy(alpha = 0.08f),
                                    )
                                    .clickable { diaAberto = i }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                            )
                        }
                    }
                }

                item {
                    CampoTexto("Pequeno-almoço", pequenos[diaAberto]) { pequenos[diaAberto] = it }
                    Spacer(Modifier.height(6.dp))
                    CampoTexto("Almoço", almocos[diaAberto]) { almocos[diaAberto] = it }
                    Spacer(Modifier.height(6.dp))
                    CampoTexto("Jantar", jantares[diaAberto]) { jantares[diaAberto] = it }
                }

                item {
                    Spacer(Modifier.height(6.dp))
                    Text("Lista de compras", color = GreenPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(
                        "Os preços que escreveres aqui ensinam a IA nas gerações seguintes.",
                        color = TextDisabled,
                        fontSize = 10.sp,
                    )
                }

                items(itens) { item ->
                    val indice = itens.indexOf(item)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(2.2f)) {
                            CampoTexto("Item", item.nome) {
                                itens[indice] = item.copy(nome = it)
                                // Uma linha nova assim que a última ganha nome:
                                // pedir "adicionar" antes de escrever é um passo
                                // a mais em cada item.
                                if (indice == itens.lastIndex && it.isNotBlank()) {
                                    itens.add(ItemEmEdicao())
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            CampoTexto("Qtd", item.quantidade, KeyboardType.Decimal) {
                                itens[indice] = item.copy(quantidade = it)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            CampoTexto("Un", item.unidade) { itens[indice] = item.copy(unidade = it) }
                        }
                        Column(modifier = Modifier.weight(1.2f)) {
                            CampoTexto(currencySymbol, item.preco, KeyboardType.Decimal) {
                                itens[indice] = item.copy(preco = it)
                            }
                        }
                        if (itens.size > 1) {
                            Text(
                                "✕",
                                color = ExpenseRed.copy(alpha = 0.7f),
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .clickable { itens.removeAt(indice) }
                                    .padding(4.dp),
                            )
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(6.dp))
                    CampoTexto("Notas (opcional)", notas) { notas = it }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSaving && temAlgumaRefeicao,
                onClick = {
                    val dias = (0..6).mapNotNull { d ->
                        val p = pequenos[d].trim()
                        val a = almocos[d].trim()
                        val j = jantares[d].trim()
                        if (p.isBlank() && a.isBlank() && j.isBlank()) {
                            null
                        } else {
                            ManualMealPlanDay(
                                dayOfWeek = d,
                                breakfast = p.ifBlank { null },
                                lunch = a.ifBlank { null },
                                dinner = j.ifBlank { null },
                            )
                        }
                    }
                    val lista = itens.mapNotNull { i ->
                        val nome = i.nome.trim()
                        if (nome.isBlank()) {
                            null
                        } else {
                            ManualShoppingItem(
                                name = nome,
                                quantity = i.quantidade.replace(',', '.').toDoubleOrNull() ?: 1.0,
                                unit = i.unidade.trim().ifBlank { "unidade" },
                                // Zero e "sem preço" são a mesma coisa aqui: o
                                // servidor recusa preços negativos e trata o 0
                                // como "ainda não sei quanto custa".
                                estimatedPrice = i.preco.replace(',', '.').toDoubleOrNull() ?: 0.0,
                            )
                        }
                    }
                    onConfirm(dias, lista, notas.trim().ifBlank { null })
                },
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(16.dp),
                        color = GreenPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Criar cardápio", color = if (temAlgumaRefeicao) GreenPrimary else TextDisabled)
                }
            }
        },
        dismissButton = {
            TextButton(enabled = !isSaving, onClick = onDismiss) {
                Text("Cancelar", color = TextDisabled)
            }
        },
    )
}

@Composable
private fun CampoTexto(
    rotulo: String,
    valor: String,
    tipo: KeyboardType = KeyboardType.Text,
    aoMudar: (String) -> Unit,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = aoMudar,
        label = { Text(rotulo, fontSize = 11.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = tipo),
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GreenPrimary,
            unfocusedBorderColor = TextDisabled.copy(alpha = 0.4f),
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
        ),
    )
}
