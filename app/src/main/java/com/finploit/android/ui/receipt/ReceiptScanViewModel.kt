package com.finploit.android.ui.receipt

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finploit.android.data.repository.AnalysisRepository
import com.finploit.android.data.repository.FinanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReceiptScanState(
    val isAnalysing: Boolean = false,
    val capturedImageUri: Uri? = null,
    val extractedAmount: Double? = null,
    val extractedDescription: String? = null,
    val extractedCategory: String? = null,
    val error: String? = null,
    val success: Boolean = false,
    val isSaving: Boolean = false,
)

@HiltViewModel
class ReceiptScanViewModel @Inject constructor(
    private val analysisRepository: AnalysisRepository,
    private val financeRepository: FinanceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ReceiptScanState())
    val state: StateFlow<ReceiptScanState> = _state.asStateFlow()

    fun onImageCaptured(uri: Uri) {
        _state.update { it.copy(capturedImageUri = uri, isAnalysing = true, error = null) }
        analyseImage(uri)
    }

    private fun analyseImage(uri: Uri) {
        viewModelScope.launch {
            analysisRepository.analyseReceiptImage(uri)
                .onSuccess { result ->
                    _state.update { it.copy(
                        isAnalysing = false,
                        extractedAmount = result.amount,
                        extractedDescription = result.description,
                        extractedCategory = result.category,
                    )}
                }
                .onFailure { e ->
                    _state.update { it.copy(isAnalysing = false, error = e.message ?: "Erro ao analisar imagem") }
                }
        }
    }

    fun saveTransaction(amount: Double, description: String) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            financeRepository.createTransaction("expense", amount, description, null)
                .onSuccess { _state.update { it.copy(isSaving = false, success = true) } }
                .onFailure { e -> _state.update { it.copy(isSaving = false, error = e.message) } }
        }
    }

    fun reset() {
        _state.value = ReceiptScanState()
    }
}
