package com.finploit.android.ui.couple

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finploit.android.ui.theme.BackgroundDark
import com.finploit.android.ui.theme.CardBackground
import com.finploit.android.ui.theme.ExpenseRed
import com.finploit.android.ui.theme.GreenPrimary
import com.finploit.android.ui.theme.SurfaceDark
import com.finploit.android.ui.theme.TextPrimary
import com.finploit.android.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoupleScreen(
    viewModel: CoupleViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var phone by remember { mutableStateOf("") }

    LaunchedEffect(uiState.message, uiState.error) {
        val text = uiState.message ?: uiState.error
        if (text != null) {
            snackbarHostState.showSnackbar(text)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        containerColor = SurfaceDark,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Casal", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.White,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark,
                    titleContentColor = Color.White,
                ),
            )
        },
    ) { padding ->
        when {
            uiState.isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = GreenPrimary)
            }

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val received = uiState.receivedInvite
                val sent = uiState.sentInvite

                when {
                    uiState.isMarried -> {
                        val spouseName = uiState.profile?.spouse?.name ?: "seu par"
                        SectionCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Avatar(initialsOf(uiState.profile?.displayName ?: uiState.profile?.name))
                                Spacer(Modifier.width((-10).dp))
                                Avatar(initialsOf(spouseName), background = CardBackground, textColor = TextPrimary)
                                Spacer(Modifier.width(14.dp))
                                Column {
                                    Text(
                                        "Você & $spouseName",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                    )
                                    Text(
                                        "Workspace compartilhado ativo",
                                        color = GreenPrimary,
                                        fontSize = 13.sp,
                                    )
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            Text(
                                "Transações, orçamento, metas, listas e despensa valem para os dois — cada um vendo os totais na própria moeda.",
                                color = TextSecondary,
                                fontSize = 13.sp,
                            )
                        }

                        SectionCard {
                            Text("Desfazer o vínculo", color = TextPrimary, fontWeight = FontWeight.Bold)
                            Text(
                                "Cada um volta a ter um workspace separado. Nenhum dado é apagado.",
                                color = TextSecondary,
                                fontSize = 13.sp,
                            )
                            Spacer(Modifier.height(10.dp))
                            var confirm by remember { mutableStateOf(false) }
                            OutlinedButton(
                                onClick = { if (confirm) viewModel.unlink() else confirm = true },
                                enabled = !uiState.isSubmitting,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed),
                            ) {
                                Text(if (confirm) "Confirmar desvínculo" else "Desvincular")
                            }
                        }
                    }

                    received != null -> SectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Avatar(initialsOf(received.inviter.displayName ?: received.inviter.name))
                            Spacer(Modifier.width(14.dp))
                            Text(
                                "${received.inviter.name ?: "Alguém"} te convidou para compartilhar as finanças",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Ao aceitar, transações, orçamento, metas, listas e despensa passam a ser dos dois.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { viewModel.accept(received.id) },
                                enabled = !uiState.isSubmitting,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GreenPrimary,
                                    contentColor = BackgroundDark,
                                ),
                                modifier = Modifier.weight(1f),
                            ) { Text("Aceitar", fontWeight = FontWeight.Bold) }
                            OutlinedButton(
                                onClick = { viewModel.reject(received.id) },
                                enabled = !uiState.isSubmitting,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            ) { Text("Recusar") }
                        }
                    }

                    sent != null -> SectionCard {
                        Text(
                            "Aguardando resposta de ${sent.invitee.name ?: "seu par"}",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Seu par aceita (ou recusa) o convite na tela Casal do app dele(a). O convite expira em 7 dias.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                        )
                        Spacer(Modifier.height(12.dp))
                        TextButton(
                            onClick = { viewModel.cancel(sent.id) },
                            enabled = !uiState.isSubmitting,
                        ) {
                            Text("Cancelar convite", color = ExpenseRed)
                        }
                    }

                    else -> SectionCard {
                        Text(
                            "Finanças a dois, sem planilha",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Convide seu par pelo telefone cadastrado na conta dele(a). O vínculo só acontece quando o convite for aceito.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                        )
                        Spacer(Modifier.height(14.dp))
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Telefone do seu par") },
                            placeholder = { Text("Ex.: +351 912 345 678") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GreenPrimary,
                                focusedLabelColor = GreenPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedTextColor = TextPrimary,
                            ),
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { if (phone.isNotBlank()) viewModel.sendInvite(phone.trim()) },
                            enabled = !uiState.isSubmitting && phone.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GreenPrimary,
                                contentColor = BackgroundDark,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                if (uiState.isSubmitting) "Enviando..." else "Enviar convite",
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
    ) {
        Column(Modifier.padding(18.dp)) { content() }
    }
}

@Composable
private fun Avatar(
    initials: String,
    background: Color = GreenPrimary,
    textColor: Color = BackgroundDark,
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .background(background, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials, color = textColor, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

private fun initialsOf(name: String?): String =
    name?.trim()?.split(Regex("\\s+"))?.take(2)?.mapNotNull { it.firstOrNull()?.uppercase() }
        ?.joinToString("") ?: "?"
