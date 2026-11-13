package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BusinessProfile
import com.example.data.model.BusinessTransaction
import com.example.data.model.CashBankAccount
import com.example.data.model.InventoryItem
import com.example.data.model.ItemType
import com.example.data.model.Party
import com.example.data.model.ThemeStyle
import com.example.data.model.TxStatus
import com.example.data.model.TxType
import com.example.data.model.isLowStock
import com.example.data.repository.DukaanRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

data class DashboardMetrics(
  val totalSales: Double = 0.0,
  val totalPurchases: Double = 0.0,
  val totalExpenses: Double = 0.0,
  val totalReceivable: Double = 0.0,
  val totalPayable: Double = 0.0,
  val cashAndBankTotal: Double = 0.0,
  val stockValue: Double = 0.0,
  val lowStockItems: List<InventoryItem> = emptyList(),
  val netProfit: Double = 0.0,
  val outputGst: Double = 0.0,
  val inputGst: Double = 0.0,
)

class DukaanViewModel(
  private val repository: DukaanRepository,
  val currentUserId: String,
  initialBusinessName: String,
  initialEmail: String,
) : ViewModel() {

  private val _uiMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
  val uiMessages: SharedFlow<String> = _uiMessages.asSharedFlow()

  private val _isBusy = MutableStateFlow(false)
  val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

  init {
    viewModelScope.launch {
      repository
        .ensureProfileInitialized(
          defaultBusinessName = initialBusinessName,
          defaultEmail = initialEmail,
        )
        .onFailure { err ->
          Log.w(TAG, "Profile initialization warning: ${err.message}", err)
        }
    }
  }

  private fun <T> Flow<T>.toUiState(label: String): StateFlow<UiState<T>> =
    this.map<T, UiState<T>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError =
          generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing $label", rootError)
        emit(UiState.Error(rootError.message ?: "Failed to load $label"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val profileState: StateFlow<UiState<BusinessProfile?>> =
    repository.observeBusinessProfile().toUiState("business profile")

  val partiesState: StateFlow<UiState<List<Party>>> =
    repository.observeParties().toUiState("parties")

  val itemsState: StateFlow<UiState<List<InventoryItem>>> =
    repository.observeItems().toUiState("items")

  val transactionsState: StateFlow<UiState<List<BusinessTransaction>>> =
    repository.observeTransactions().toUiState("transactions")

  val accountsState: StateFlow<UiState<List<CashBankAccount>>> =
    repository.observeAccounts().toUiState("accounts")

  val metricsState: StateFlow<DashboardMetrics> =
    combine(partiesState, itemsState, transactionsState, accountsState) {
        pState,
        iState,
        tState,
        aState ->
        val parties = (pState as? UiState.Success)?.data.orEmpty()
        val items = (iState as? UiState.Success)?.data.orEmpty()
        val txs =
          (tState as? UiState.Success)?.data.orEmpty().filter {
            it.status != TxStatus.CANCELLED.name
          }
        val accounts = (aState as? UiState.Success)?.data.orEmpty()

        val sales = txs.filter { it.txType == TxType.SALE.name }
        val purchases = txs.filter { it.txType == TxType.PURCHASE.name }
        val expenses = txs.filter { it.txType == TxType.EXPENSE.name }

        val totalSales = sales.sumOf { it.totalAmount }
        val totalPurchases = purchases.sumOf { it.totalAmount }
        val totalExpenses = expenses.sumOf { it.totalAmount }
        val outputGst = sales.sumOf { it.taxAmount }
        val inputGst = purchases.sumOf { it.taxAmount }

        val totalReceivable = parties.filter { it.currentBalance > 0 }.sumOf { it.currentBalance }
        val totalPayable = parties.filter { it.currentBalance < 0 }.sumOf { -it.currentBalance }

        val stockValue =
          items
            .filter { it.itemType == ItemType.PRODUCT.name && it.currentStock > 0 }
            .sumOf {
              val unitCost = if (it.purchasePrice > 0) it.purchasePrice else it.salePrice
              it.currentStock * unitCost
            }
        val lowStock = items.filter { it.isLowStock() }
        val cashBankTotal = accounts.sumOf { it.currentBalance }

        // Profit calculation based on recorded sales minus purchase cost & expenses
        val cogs =
          sales.sumOf { sale ->
            sale.lineItems.sumOf { line ->
              val item = items.firstOrNull { it.id == line.itemId }
              val cost = item?.purchasePrice ?: 0.0
              cost * line.quantity
            }
          }
        val netProfit = (totalSales - outputGst) - cogs - totalExpenses

        DashboardMetrics(
          totalSales = totalSales,
          totalPurchases = totalPurchases,
          totalExpenses = totalExpenses,
          totalReceivable = totalReceivable,
          totalPayable = totalPayable,
          cashAndBankTotal = cashBankTotal,
          stockValue = stockValue,
          lowStockItems = lowStock,
          netProfit = netProfit,
          outputGst = outputGst,
          inputGst = inputGst,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = DashboardMetrics(),
      )

  fun saveProfile(profile: BusinessProfile, onSaved: () -> Unit = {}) {
    viewModelScope.launch {
      _isBusy.value = true
      repository
        .saveBusinessProfile(profile)
        .onSuccess {
          _uiMessages.tryEmit("Business profile updated")
          onSaved()
        }
        .onFailure { _uiMessages.tryEmit(it.localizedMessage ?: "Failed to save profile") }
      _isBusy.value = false
    }
  }

  fun updateThemeStyle(themeStyle: ThemeStyle) {
    val current = (profileState.value as? UiState.Success)?.data ?: return
    saveProfile(current.copy(themeStyle = themeStyle.name))
  }

  fun saveParty(party: Party, onSaved: () -> Unit = {}) {
    viewModelScope.launch {
      _isBusy.value = true
      repository
        .saveParty(party)
        .onSuccess {
          _uiMessages.tryEmit(if (party.id.isBlank()) "Party added" else "Party updated")
          onSaved()
        }
        .onFailure { _uiMessages.tryEmit(it.localizedMessage ?: "Failed to save party") }
      _isBusy.value = false
    }
  }

  fun deleteParty(partyId: String) {
    viewModelScope.launch {
      repository
        .deleteParty(partyId)
        .onSuccess { _uiMessages.tryEmit("Party deleted") }
        .onFailure { _uiMessages.tryEmit(it.localizedMessage ?: "Failed to delete party") }
    }
  }

  fun saveItem(item: InventoryItem, onSaved: () -> Unit = {}) {
    viewModelScope.launch {
      _isBusy.value = true
      repository
        .saveItem(item)
        .onSuccess {
          _uiMessages.tryEmit(if (item.id.isBlank()) "Item added" else "Item updated")
          onSaved()
        }
        .onFailure { _uiMessages.tryEmit(it.localizedMessage ?: "Failed to save item") }
      _isBusy.value = false
    }
  }

  fun adjustStock(itemId: String, deltaQty: Double) {
    viewModelScope.launch {
      repository
        .adjustItemStock(itemId, deltaQty)
        .onSuccess { _uiMessages.tryEmit("Stock adjusted") }
        .onFailure { _uiMessages.tryEmit(it.localizedMessage ?: "Failed to adjust stock") }
    }
  }

  fun deleteItem(itemId: String) {
    viewModelScope.launch {
      repository
        .deleteItem(itemId)
        .onSuccess { _uiMessages.tryEmit("Item deleted") }
        .onFailure { _uiMessages.tryEmit(it.localizedMessage ?: "Failed to delete item") }
    }
  }

  fun saveAccount(account: CashBankAccount, onSaved: () -> Unit = {}) {
    viewModelScope.launch {
      _isBusy.value = true
      repository
        .saveAccount(account)
        .onSuccess {
          _uiMessages.tryEmit(if (account.id.isBlank()) "Account added" else "Account updated")
          onSaved()
        }
        .onFailure { _uiMessages.tryEmit(it.localizedMessage ?: "Failed to save account") }
      _isBusy.value = false
    }
  }

  fun adjustAccountBalance(accountId: String, delta: Double) {
    viewModelScope.launch {
      repository
        .adjustAccountBalance(accountId, delta)
        .onSuccess { _uiMessages.tryEmit("Balance updated") }
        .onFailure { _uiMessages.tryEmit(it.localizedMessage ?: "Failed to update balance") }
    }
  }

  fun recordTransaction(
    tx: BusinessTransaction,
    incrementInvoiceSequence: Boolean = false,
    onSaved: (String) -> Unit = {},
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      val knownItems = (itemsState.value as? UiState.Success)?.data.orEmpty()
      repository
        .recordTransaction(tx, knownItems, incrementInvoiceSequence)
        .onSuccess { id ->
          _uiMessages.tryEmit("${tx.refNumber} saved")
          onSaved(id)
        }
        .onFailure { _uiMessages.tryEmit(it.localizedMessage ?: "Failed to save transaction") }
      _isBusy.value = false
    }
  }

  fun recordSettlement(
    tx: BusinessTransaction,
    additionalPaid: Double,
    accountId: String,
  ) {
    if (additionalPaid <= 0) return
    viewModelScope.launch {
      val newPaid = (tx.paidAmount + additionalPaid).coerceAtMost(tx.totalAmount)
      val newBalance = (tx.totalAmount - newPaid).coerceAtLeast(0.0)
      val newStatus = if (newBalance <= 0.01) TxStatus.PAID else TxStatus.PARTIAL
      val type = TxType.entries.firstOrNull { it.name == tx.txType } ?: TxType.SALE
      repository
        .recordSettlementPayment(
          transactionId = tx.id,
          additionalPaid = additionalPaid,
          newTotalPaid = newPaid,
          newBalance = newBalance,
          newStatus = newStatus,
          partyId = tx.partyId,
          txType = type,
          accountId = accountId,
        )
        .onSuccess { _uiMessages.tryEmit("Payment recorded for ${tx.refNumber}") }
        .onFailure { _uiMessages.tryEmit(it.localizedMessage ?: "Failed to record payment") }
    }
  }

  fun cancelTransaction(txId: String) {
    viewModelScope.launch {
      repository
        .cancelTransaction(txId)
        .onSuccess { _uiMessages.tryEmit("Transaction cancelled") }
        .onFailure { _uiMessages.tryEmit(it.localizedMessage ?: "Failed to cancel transaction") }
    }
  }

  private companion object {
    const val TAG = "DukaanViewModel"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
