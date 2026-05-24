package com.finploit.android.ui.receipt

import android.Manifest
import android.net.Uri
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.*
import com.finploit.android.ui.theme.*
import java.io.File
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ReceiptScanScreen(
    viewModel: ReceiptScanViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    var amountText by remember(state.extractedAmount) { mutableStateOf(state.extractedAmount?.toString() ?: "") }
    var descriptionText by remember(state.extractedDescription) { mutableStateOf(state.extractedDescription ?: "") }

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    LaunchedEffect(state.success) {
        if (state.success) onBack()
    }

    Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        TopAppBar(
            title = { Text("Scan de Recibo", fontWeight = FontWeight.Bold, color = TextPrimary) },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = TextSecondary) }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark),
        )

        when {
            !cameraPermission.status.isGranted -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(24.dp)) {
                        Text("📷", fontSize = 52.sp)
                        Text("Permissão de câmara necessária", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Text("Para digitalizar recibos, precisa de acesso à câmara.", color = TextDisabled, fontSize = 13.sp)
                        Button(onClick = { cameraPermission.launchPermissionRequest() }, colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)) {
                            Text("Conceder permissão", color = BackgroundDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            state.extractedAmount != null || state.extractedDescription != null -> {
                // Show extracted data for confirmation
                Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("✅ Recibo analisado", color = GreenPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Confirma ou ajusta os valores extraídos:", color = TextSecondary, fontSize = 14.sp)
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Valor (€)", color = TextDisabled) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreenPrimary,
                            unfocusedBorderColor = TextDisabled.copy(0.4f),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = GreenPrimary,
                        ),
                    )
                    OutlinedTextField(
                        value = descriptionText,
                        onValueChange = { descriptionText = it },
                        label = { Text("Descrição", color = TextDisabled) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreenPrimary,
                            unfocusedBorderColor = TextDisabled.copy(0.4f),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = GreenPrimary,
                        ),
                    )
                    if (state.error != null) Text(state.error!!, color = ExpenseRed, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = viewModel::reset, modifier = Modifier.weight(1f)) {
                            Text("Novo scan", color = TextSecondary)
                        }
                        Button(
                            onClick = {
                                val amount = amountText.toDoubleOrNull() ?: return@Button
                                viewModel.saveTransaction(amount, descriptionText)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                            enabled = !state.isSaving,
                        ) {
                            if (state.isSaving) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = BackgroundDark, strokeWidth = 2.dp)
                            else Text("Guardar", color = BackgroundDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            state.isAnalysing -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        CircularProgressIndicator(color = GreenPrimary, modifier = Modifier.size(48.dp))
                        Text("A analisar recibo...", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Text("A IA está a extrair os valores", color = TextDisabled, fontSize = 13.sp)
                    }
                }
            }
            else -> {
                // Camera preview
                Box(modifier = Modifier.weight(1f)) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }
                                val capture = ImageCapture.Builder()
                                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                    .build()
                                imageCapture = capture
                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                                } catch (e: Exception) { /* ignore */ }
                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                    // Capture button
                    Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp)) {
                        Box(
                            modifier = Modifier.size(72.dp).clip(CircleShape).background(GreenPrimary),
                            contentAlignment = Alignment.Center,
                        ) {
                            IconButton(onClick = {
                                val ic = imageCapture ?: return@IconButton
                                val file = File(context.cacheDir, "receipt_${System.currentTimeMillis()}.jpg")
                                val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
                                ic.takePicture(outputOptions, Executors.newSingleThreadExecutor(),
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                            viewModel.onImageCaptured(Uri.fromFile(file))
                                        }
                                        override fun onError(exc: ImageCaptureException) {
                                            /* ignore */
                                        }
                                    }
                                )
                            }) {
                                Icon(Icons.Default.CameraAlt, contentDescription = "Capturar", tint = BackgroundDark, modifier = Modifier.size(36.dp))
                            }
                        }
                    }
                    if (state.error != null) {
                        Text(state.error!!, color = ExpenseRed, modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp), fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
