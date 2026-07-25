package com.devpro58.hnem06.moneysnap.presentation.expense.add

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.devpro58.hnem06.moneysnap.domain.model.AddExpenseInput
import com.devpro58.hnem06.moneysnap.domain.model.ExpenseCategory
import com.devpro58.hnem06.moneysnap.domain.model.PaymentMethod
import com.devpro58.hnem06.moneysnap.domain.usecase.expense.AddExpenseUseCase
import com.devpro58.hnem06.moneysnap.domain.usecase.payment.AddPaymentMethodUseCase
import com.devpro58.hnem06.moneysnap.domain.usecase.payment.ObservePaymentMethodsUseCase
import com.devpro58.hnem06.moneysnap.domain.usecase.receipt.ScanReceiptUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val addExpense: AddExpenseUseCase,
    observePaymentMethods: ObservePaymentMethodsUseCase,
    private val addPaymentMethodUseCase: AddPaymentMethodUseCase,
    private val scanReceiptUseCase: ScanReceiptUseCase
) : ViewModel() {

    private val _uiState = MutableLiveData<AddExpenseUiState>(AddExpenseUiState.Idle)
    val uiState: LiveData<AddExpenseUiState> = _uiState

    val paymentMethods: LiveData<List<PaymentMethod>> =
        observePaymentMethods().asLiveData()

    private val _paymentMethodResult = MutableLiveData<Result<PaymentMethod>?>()
    val paymentMethodResult: LiveData<Result<PaymentMethod>?> = _paymentMethodResult

    private val _receiptScanState = MutableLiveData<ReceiptScanUiState>(ReceiptScanUiState.Idle)
    val receiptScanState: LiveData<ReceiptScanUiState> = _receiptScanState

    fun scanReceipt(imageUri: String) {
        if (_receiptScanState.value == ReceiptScanUiState.Scanning) return
        _receiptScanState.value = ReceiptScanUiState.Scanning
        viewModelScope.launch {
            _receiptScanState.value = runCatching { scanReceiptUseCase(imageUri) }.fold(
                onSuccess = { draft ->
                    if (draft.totalAmount == null && draft.spentAtMillis == null && draft.suggestedCategory == null) {
                        ReceiptScanUiState.NoUsefulData
                    } else {
                        ReceiptScanUiState.Recognized(draft)
                    }
                },
                onFailure = { ReceiptScanUiState.Error }
            )
        }
    }

    fun consumeReceiptScanState() {
        _receiptScanState.value = ReceiptScanUiState.Idle
    }

    fun saveExpense(
        amount: Long,
        title: String,
        category: ExpenseCategory,
        paymentMethod: String?,
        note: String?,
        receiptSourceUri: String?,
        spentAtMillis: Long
    ) {
        _uiState.value = AddExpenseUiState.Saving
        viewModelScope.launch {
            runCatching {
                addExpense(
                    AddExpenseInput(
                        amount = amount,
                        title = title,
                        category = category,
                        paymentMethod = paymentMethod?.takeIf { it.isNotBlank() },
                        note = note?.takeIf { it.isNotBlank() },
                        receiptSourceUri = receiptSourceUri,
                        spentAtMillis = spentAtMillis
                    )
                )
            }.onSuccess {
                _uiState.value = AddExpenseUiState.Saved
            }.onFailure {
                _uiState.value = AddExpenseUiState.Error("")
            }
        }
    }

    fun resetState() {
        _uiState.value = AddExpenseUiState.Idle
    }

    fun addPaymentMethod(name: String) {
        viewModelScope.launch {
            _paymentMethodResult.value = runCatching {
                addPaymentMethodUseCase(name)
            }
        }
    }

    fun consumePaymentMethodResult() {
        _paymentMethodResult.value = null
    }
}
