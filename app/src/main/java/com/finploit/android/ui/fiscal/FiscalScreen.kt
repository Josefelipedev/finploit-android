package com.finploit.android.ui.fiscal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finploit.android.data.dto.ChatMessageDto
import com.finploit.android.data.dto.FiscalDeadlineDto
import com.finploit.android.data.dto.FiscalObligationDto
import com.finploit.android.data.dto.FiscalProfileDto
import com.finploit.android.data.dto.FiscalProfileSettings
import com.finploit.android.ui.theme.BackgroundDark
import com.finploit.android.ui.theme.CardBackground
import com.finploit.android.ui.theme.CardElevated
import com.finploit.android.ui.theme.GreenPrimary
import com.finploit.android.ui.theme.SurfaceDark
import com.finploit.android.ui.theme.TextPrimary
import com.finploit.android.ui.theme.TextSecondary
import com.finploit.android.ui.theme.WarningAmber
import com.finploit.android.util.filterAmountInput
import com.finploit.android.util.parseAmountInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiscalScreen(
    viewModel: FiscalViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var editingProfile by remember { mutableStateOf(false) }

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
                title = { Text("Fiscal", fontWeight = FontWeight.Bold) },
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
                if (uiState.isConfigured && !editingProfile) {
                    ConfiguredPanel(uiState, onEdit = { editingProfile = true })
                    FiscalAssistant(
                        messages = uiState.messages,
                        isAsking = uiState.isAsking,
                        onAsk = { viewModel.ask(it) },
                    )
                } else {
                    SetupForm(
                        isSubmitting = uiState.isSubmitting,
                        initial = uiState.data?.profile,
                        isEditing = uiState.isConfigured,
                        onCancel = { editingProfile = false },
                        onSave = {
                            viewModel.saveProfile(it)
                            editingProfile = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SetupForm(
    isSubmitting: Boolean,
    initial: FiscalProfileDto?,
    isEditing: Boolean,
    onCancel: () -> Unit,
    onSave: (FiscalProfileSettings) -> Unit,
) {
    var activityStart by remember(initial) { mutableStateOf(initial?.activityStartDate.orEmpty()) }
    var fiscalNumber by remember(initial) { mutableStateOf(initial?.fiscalNumber.orEmpty()) }
    var activityCode by remember(initial) { mutableStateOf(initial?.activityCode.orEmpty()) }
    var annualRevenue by remember(initial) {
        mutableStateOf(initial?.annualRevenue?.takeIf { it > 0 }?.toString().orEmpty())
    }
    var accountingRegime by remember(initial) {
        mutableStateOf(initial?.accountingRegime ?: "simplified")
    }
    var vatRegime by remember(initial) { mutableStateOf(initial?.ivaStatus ?: "exempt_art53") }
    var withholdingMode by remember(initial) {
        mutableStateOf(initial?.withholdingMode ?: "exempt_art101b")
    }
    var socialSecurityStatus by remember(initial) {
        mutableStateOf(initial?.socialSecurityStatus ?: "auto")
    }
    var hasEuB2bClients by remember(initial) { mutableStateOf(initial?.hasEuB2bClients == true) }
    var hasNonEuClients by remember(initial) { mutableStateOf(initial?.hasNonEuClients == true) }
    var hasPaymentsOnAccount by remember(initial) { mutableStateOf(initial?.hasPaymentsOnAccount == true) }
    var hasWorkAccidentInsurance by remember(initial) {
        mutableStateOf(initial?.hasWorkAccidentInsurance == true)
    }
    var usesPortalInvoices by remember(initial) { mutableStateOf(initial?.usesPortalInvoices != false) }
    var hasEmployees by remember(initial) { mutableStateOf(initial?.hasEmployees == true) }
    val validDate = Regex("""\d{4}-\d{2}-\d{2}""").matches(activityStart.trim())

    SectionCard {
        Text(
            "Obrigações fiscais em Portugal",
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Indique o enquadramento que consta no comprovativo de atividade. O calendário adapta IVA, " +
                "IRS, retenção, Segurança Social e clientes internacionais ao seu caso.",
            color = TextSecondary,
            fontSize = 13.sp,
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = "Portugal",
            onValueChange = {},
            label = { Text("País") },
            enabled = false,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = TextPrimary,
                disabledBorderColor = TextSecondary.copy(alpha = 0.4f),
                disabledLabelColor = TextSecondary,
            ),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = activityStart,
            onValueChange = { activityStart = it },
            label = { Text("Data de início de atividade") },
            placeholder = { Text("AAAA-MM-DD") },
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
        OutlinedTextField(
            value = fiscalNumber,
            onValueChange = { fiscalNumber = it },
            label = { Text("NIF (opcional)") },
            placeholder = { Text("Ex.: 123456789") },
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
        OutlinedTextField(
            value = activityCode,
            onValueChange = { activityCode = it },
            label = { Text("CAE ou código CIRS principal") },
            placeholder = { Text("Ex.: CIRS 1519") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = fiscalFieldColors(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = annualRevenue,
            onValueChange = { value ->
                if (value.isEmpty() || value.matches(Regex("""\d*([.,]\d{0,2})?"""))) annualRevenue = value
            },
            label = { Text("Volume de negócios no ano (€)") },
            supportingText = {
                // C6: assim que houver categorias marcadas como faturação da
                // atividade, este campo deixa de ser usado — o volume passa a
                // ser somado dos lançamentos do ano.
                Text(
                    if (initial?.revenueSource == "ledger") {
                        "Ignorado: o volume vem dos seus lançamentos (${initial.revenueYear ?: ""})."
                    } else {
                        "Usado enquanto não marcar categorias de receita como \"Faturação da atividade\"."
                    },
                    fontSize = 11.sp,
                )
            },
            placeholder = { Text("0,00") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = fiscalFieldColors(),
        )

        Spacer(Modifier.height(16.dp))
        ChoiceSelector(
            title = "Regime de IRS/contabilidade",
            value = accountingRegime,
            options = listOf("simplified" to "Simplificado", "organized" to "Contabilidade organizada"),
            onChange = { accountingRegime = it },
        )
        ChoiceSelector(
            title = "Enquadramento de IVA",
            value = vatRegime,
            options = listOf(
                "exempt_art53" to "Isento — art. 53.º",
                "exempt_art9" to "Isento — art. 9.º",
                "normal_quarterly" to "Normal trimestral",
                "normal_monthly" to "Normal mensal",
            ),
            onChange = { vatRegime = it },
        )
        ChoiceSelector(
            title = "Retenção na fonte",
            value = withholdingMode,
            options = listOf(
                "exempt_art101b" to "Dispensa — art. 101.º-B",
                "withholding" to "Faço retenção",
                "not_applicable" to "Não aplicável",
            ),
            onChange = { withholdingMode = it },
        )
        ChoiceSelector(
            title = "Segurança Social",
            value = socialSecurityStatus,
            options = listOf(
                "auto" to "Calcular pelo início",
                "contributing" to "Estou a contribuir",
                "exempt_employment" to "Isento — trabalho dependente",
                "exempt_pension" to "Isento — pensão/incapacidade",
                "foreign_scheme" to "Regime de outro país",
                "professional_fund" to "Caixa profissional",
            ),
            onChange = { socialSecurityStatus = it },
        )

        Spacer(Modifier.height(8.dp))
        Text("Situações adicionais", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        FiscalCheckbox("Clientes empresariais na UE", hasEuB2bClients) { hasEuB2bClients = it }
        FiscalCheckbox("Clientes fora da UE", hasNonEuClients) { hasNonEuClients = it }
        FiscalCheckbox("A AT comunicou pagamentos por conta", hasPaymentsOnAccount) { hasPaymentsOnAccount = it }
        FiscalCheckbox("Tenho seguro de acidentes de trabalho", hasWorkAccidentInsurance) {
            hasWorkAccidentInsurance = it
        }
        FiscalCheckbox("Emito tudo no Portal das Finanças/ATGO", usesPortalInvoices) { usesPortalInvoices = it }
        FiscalCheckbox("Tenho trabalhadores contratados", hasEmployees) { hasEmployees = it }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                if (validDate) {
                    onSave(
                        FiscalProfileSettings(
                            activityStartDate = activityStart.trim(),
                            fiscalNumber = fiscalNumber.trim().ifBlank { null },
                            accountingRegime = accountingRegime,
                            vatRegime = vatRegime,
                            withholdingMode = withholdingMode,
                            socialSecurityStatus = socialSecurityStatus,
                            activityCode = activityCode.trim().ifBlank { null },
                            annualRevenue = parseAmountInput(annualRevenue) ?: 0.0,
                            hasEuB2bClients = hasEuB2bClients,
                            hasNonEuClients = hasNonEuClients,
                            hasPaymentsOnAccount = hasPaymentsOnAccount,
                            hasWorkAccidentInsurance = hasWorkAccidentInsurance,
                            usesPortalInvoices = usesPortalInvoices,
                            hasEmployees = hasEmployees,
                        )
                    )
                }
            },
            enabled = !isSubmitting && validDate,
            colors = ButtonDefaults.buttonColors(
                containerColor = GreenPrimary,
                contentColor = BackgroundDark,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (isSubmitting) "A guardar..." else "Guardar perfil fiscal",
                fontWeight = FontWeight.Bold,
            )
        }
        if (isEditing) {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CardElevated,
                    contentColor = TextPrimary,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Cancelar")
            }
        }
    }
}

@Composable
private fun fiscalFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = GreenPrimary,
    focusedLabelColor = GreenPrimary,
    unfocusedTextColor = TextPrimary,
    focusedTextColor = TextPrimary,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoiceSelector(
    title: String,
    value: String,
    options: List<Pair<String, String>>,
    onChange: (String) -> Unit,
) {
    Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    Spacer(Modifier.height(8.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (key, label) ->
            val selected = value == key
            Box(
                modifier = Modifier
                    .background(
                        if (selected) GreenPrimary.copy(alpha = 0.18f) else CardElevated,
                        RoundedCornerShape(10.dp),
                    )
                    .clickable { onChange(key) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    label,
                    color = if (selected) GreenPrimary else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
    Spacer(Modifier.height(14.dp))
}

@Composable
private fun FiscalCheckbox(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, color = TextSecondary, fontSize = 13.sp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConfiguredPanel(uiState: FiscalUiState, onEdit: () -> Unit) {
    val data = uiState.data ?: return
    val profile = data.profile
    val status = data.status

    // Profile chips
    SectionCard {
        Text("O seu perfil fiscal", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            profile?.regimeLabel?.let { Chip(it) }
            profile?.ivaLabel?.let { Chip(it) }
            profile?.withholdingLabel?.let { Chip(it) }
            profile?.socialSecurityLabel?.let { Chip(it) }
            if (status?.socialSecurityFirstYearExempt == true) {
                Chip("SS 1.º ano isento")
            }
        }
        if (status?.socialSecurityExemptUntil != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                "Segurança Social isenta até ${status.socialSecurityExemptUntil}.",
                color = TextSecondary,
                fontSize = 13.sp,
            )
        }
        profile?.activityStartDate?.let {
            Spacer(Modifier.height(6.dp))
            Text("Início de atividade: $it", color = TextSecondary, fontSize = 13.sp)
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onEdit,
            colors = ButtonDefaults.buttonColors(containerColor = CardElevated, contentColor = TextPrimary),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Editar perfil fiscal")
        }
    }

    if (data.warnings.isNotEmpty()) {
        SectionCard {
            Text("Pontos que precisam de atenção", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            data.warnings.forEach { warning ->
                Text(
                    warning.title ?: "Atenção",
                    color = if (warning.severity == "critical") Color(0xFFF87171) else WarningAmber,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
                warning.description?.let { Text(it, color = TextSecondary, fontSize = 12.sp) }
            }
        }
    }

    // Next deadline highlight
    data.nextDeadline?.let { next ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = GreenPrimary.copy(alpha = 0.12f)),
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("Próximo prazo", color = GreenPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                Text(next.title ?: "", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                next.description?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, color = TextSecondary, fontSize = 13.sp)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "${next.date ?: ""} · ${daysUntilLabel(next.daysUntil)}",
                    color = GreenPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
            }
        }
    }

    // Upcoming deadlines
    if (data.upcoming.isNotEmpty()) {
        SectionCard {
            Text("Próximos prazos", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            data.upcoming.forEach { DeadlineRow(it) }
        }
    }

    // Obligations
    if (data.obligations.isNotEmpty()) {
        SectionCard {
            Text("As suas obrigações", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            data.obligations.forEach { ObligationRow(it) }
        }
    }

    // Disclaimer
    data.disclaimer?.let {
        Text(
            it,
            color = TextSecondary,
            fontSize = 11.sp,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FiscalAssistant(
    messages: List<ChatMessageDto>,
    isAsking: Boolean,
    onAsk: (String) -> Unit,
) {
    var draft by remember { mutableStateOf("") }
    val suggestions = listOf(
        "Preciso de fazer algo agora?",
        "Como emito uma fatura-recibo?",
        "Quando entrego o IRS?",
        "E se passar dos 15.000 €?",
    )

    SectionCard {
        Text("🤖 Assistente Fiscal", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            "Tire as suas dúvidas fiscais em português. As respostas são orientações gerais.",
            color = TextSecondary,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(14.dp))

        if (messages.isEmpty() && !isAsking) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                suggestions.forEach { suggestion ->
                    SuggestionChip(suggestion) { onAsk(suggestion) }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                messages.forEach { ChatBubble(it) }
                if (isAsking) {
                    Text(
                        "a pensar…",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("Escreva a sua pergunta…") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenPrimary,
                    focusedLabelColor = GreenPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedTextColor = TextPrimary,
                ),
            )
            Spacer(Modifier.width(8.dp))
            val canSend = draft.isNotBlank() && !isAsking
            IconButton(
                onClick = {
                    if (canSend) {
                        onAsk(draft)
                        draft = ""
                    }
                },
                enabled = canSend,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Enviar",
                    tint = if (canSend) GreenPrimary else TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessageDto) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .background(
                    if (isUser) GreenPrimary.copy(alpha = 0.16f) else SurfaceDark,
                    RoundedCornerShape(14.dp),
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(
                message.content,
                color = TextPrimary,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun SuggestionChip(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(CardElevated, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(text, color = GreenPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DeadlineRow(deadline: FiscalDeadlineDto) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(deadline.title ?: "", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            deadline.description?.let {
                Text(it, color = TextSecondary, fontSize = 12.sp)
            }
            Text(
                "${deadline.date ?: ""}${deadline.endDate?.let { " – $it" } ?: ""}",
                color = TextSecondary,
                fontSize = 12.sp,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End) {
            deadline.tag?.let { Chip(it) }
            Spacer(Modifier.height(4.dp))
            Text(
                daysUntilLabel(deadline.daysUntil),
                color = if (deadline.daysUntil <= 7) WarningAmber else TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ObligationRow(obligation: FiscalObligationDto) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(obligation.title ?: "", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
            obligation.frequency?.let { Chip(it) }
        }
        obligation.description?.let {
            Spacer(Modifier.height(2.dp))
            Text(it, color = TextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun Chip(text: String) {
    Box(
        modifier = Modifier
            .background(CardElevated, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(text, color = GreenPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
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

private fun daysUntilLabel(days: Int): String = when {
    days <= 0 -> "hoje"
    days == 1 -> "em 1 dia"
    else -> "em $days dias"
}
