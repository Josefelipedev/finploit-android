package com.finploit.android.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.hilt.navigation.compose.hiltViewModel
import com.finploit.android.ui.couple.CoupleScreen
import com.finploit.android.ui.fiscal.FiscalScreen
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.finploit.android.ui.theme.CardBackground
import com.finploit.android.ui.theme.CardElevated
import com.finploit.android.ui.theme.CURRENCY_OPTIONS
import com.finploit.android.ui.theme.ExpenseRed
import com.finploit.android.ui.theme.GreenPrimary
import com.finploit.android.ui.theme.Green80
import com.finploit.android.ui.theme.SurfaceDark
import com.finploit.android.ui.theme.TextDisabled
import com.finploit.android.ui.theme.TextPrimary
import com.finploit.android.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onLogout: () -> Unit,
    onBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedCurrencyCode by viewModel.currencyCode.collectAsStateWithLifecycle()
    var showCouple by remember { mutableStateOf(false) }
    var showFiscal by remember { mutableStateOf(false) }

    if (showCouple) {
        BackHandler { showCouple = false }
        CoupleScreen(viewModel = hiltViewModel(), onBack = { showCouple = false })
        return
    }

    if (showFiscal) {
        BackHandler { showFiscal = false }
        FiscalScreen(viewModel = hiltViewModel(), onBack = { showFiscal = false })
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark),
    ) {
        TopAppBar(
            title = { Text("Perfil", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = SurfaceDark,
                titleContentColor = Color.White,
            ),
        )

        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Green80)
            }
            uiState.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(uiState.error ?: "Erro", color = ExpenseRed)
            }
            uiState.user != null -> {
                val user = uiState.user!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    item { Spacer(Modifier.height(24.dp)) }

                    item {
                        if (!user.profilePicUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = user.profilePicUrl,
                                contentDescription = "Foto de perfil",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(90.dp).clip(CircleShape),
                            )
                        } else {
                            Box(
                                modifier = Modifier.size(90.dp).clip(CircleShape)
                                    .background(Green80.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = (user.displayName ?: user.name ?: user.email).take(1).uppercase(),
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Green80,
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            user.displayName ?: user.name ?: "Usuário",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(user.email, color = Color.Gray, fontSize = 14.sp)
                        Spacer(Modifier.height(20.dp))
                    }

                    if (!user.phone.isNullOrBlank() || !user.firstName.isNullOrBlank()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = CardBackground),
                            ) {
                                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    if (!user.phone.isNullOrBlank()) ProfileRow("Telefone", user.phone)
                                    if (!user.firstName.isNullOrBlank()) ProfileRow("Nome", "${user.firstName} ${user.lastName ?: ""}".trim())
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                        }
                    }

                    // Finanças do casal
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { showCouple = true },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                        ) {
                            Row(
                                modifier = Modifier.padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("💚", fontSize = 22.sp)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Finanças do Casal", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(
                                        "Convide seu par ou gerencie o vínculo",
                                        color = TextDisabled,
                                        fontSize = 12.sp,
                                    )
                                }
                                Text("›", color = TextSecondary, fontSize = 22.sp)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    // Obrigações fiscais (Portugal)
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { showFiscal = true },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                        ) {
                            Row(
                                modifier = Modifier.padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("🧾", fontSize = 22.sp)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Fiscal", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(
                                        "Prazos de IVA, IRS e Segurança Social",
                                        color = TextDisabled,
                                        fontSize = 12.sp,
                                    )
                                }
                                Text("›", color = TextSecondary, fontSize = 22.sp)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    // Currency picker
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text("💱 Moeda", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Escolha a moeda usada em toda a app",
                                    color = TextDisabled,
                                    fontSize = 12.sp,
                                )
                                Spacer(Modifier.height(14.dp))
                                CURRENCY_OPTIONS.forEach { cfg ->
                                    val selected = cfg.code == selectedCurrencyCode
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (selected) GreenPrimary.copy(alpha = 0.1f) else CardElevated)
                                            .border(
                                                if (selected) 1.5.dp else 1.dp,
                                                if (selected) GreenPrimary.copy(alpha = 0.5f) else TextDisabled.copy(alpha = 0.15f),
                                                RoundedCornerShape(12.dp),
                                            )
                                            .clickable { viewModel.setCurrency(cfg.code) }
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(cfg.flag, fontSize = 20.sp)
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(cfg.label, color = if (selected) GreenPrimary else TextPrimary, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, fontSize = 14.sp)
                                            Text("${cfg.code} · ${cfg.symbol}", color = TextDisabled, fontSize = 12.sp)
                                        }
                                        if (selected) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }

                    item {
                        Button(
                            onClick = { viewModel.logout(); onLogout() },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed.copy(alpha = 0.85f)),
                        ) {
                            Text("Sair da conta", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Column {
        Text(label, color = Color.Gray, fontSize = 12.sp)
        Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}
