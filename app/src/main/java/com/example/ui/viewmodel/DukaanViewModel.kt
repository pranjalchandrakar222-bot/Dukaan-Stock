package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Agent
import com.example.data.model.BusinessProfile
import com.example.data.model.BusinessTransaction
import com.example.data.model.CashBankAccount
import com.example.data.model.InventoryItem
import com.example.data.model.ItemType
import com.example.data.model.Party
import com.example.data.model.PaymentMode
import com.example.data.model.ThemeStyle
import com.example.data.model.TxStatus
import com.example.data.model.TxType
import com.example.data.model.isLowStock
import com.example.data.repository.DukaanRepository
import com.example.ui.util.MoneyUtils
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
import java.util.Calendar

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

enum class DateRangeFilter(val label: String) {
  TODAY("Today"),
  THIS_WEEK("This Week"),
  THIS_MONTH("This Month"),
  THIS_FY("This FY"),
  ALL_TIME("All Time"),
}

data class DashboardMetrics(
  val dateRange: DateRangeFilter = DateRangeFilter.ALL_TIME,
  val totalSales: Double = 0.0,
  val creditNotesTotal: Double = 0.0,
  val netSales: Double = 0.0,
  val totalPurchases: Double = 0.0,
  val debitNotesTotal: Double = 0.0,
  val totalExpenses: Double = 0.0,
  val cogs: Double = 0.0,
  val totalReceivable: Double = 0.0,
  val totalPayable: Double = 0.0,
  val cashAndBankTotal: Double = 0.0,
  val stockValue: Double = 0.0,
  val lowStockItems: List<InventoryItem> = emptyList(),
  val commissionToPay: Double = 0.0,
  val commissionAccruedPeriod: Double = 0.0,
  val commissionPaidPeriod: Double = 0.0,
  val netProfit: Double = 0.0,
  val outputGst: Double = 0.0,
  val cgstOutput: Double = 0.0,
  val sgstOutput: Double = 0.0,
  val igstOutput: Double = 0.0,
  val inputGst: Double = 0.0,
  val cgstInput: Double = 0.0,
  val sgstInput: Double = 0.0,
  val igstInput: Double = 0.0,
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

  private val _dateRangeFilter = MutableStateFlow(DateRangeFilter.ALL_TIME)
  val dateRangeFilter: StateFlow<DateRangeFilter> = _dateRangeFilter.asStateFlow()

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

  fun setDateRangeFilter(filter: DateRangeFilter) {
    _dateRangeFilter.value = filter
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

  val agentsState: StateFlow<UiState<List<Agent>>> =
    repository.observeAgents().toUiState("agents")

  val hasPendingWritesState: StateFlow<Boolean> =
    repository.observeHasPendingWrites()
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = false,
      )

  val isOnlineNow: Boolean
    get() = repository.isNetworkAvailable()

  val metricsState: StateFlow<DashboardMetrics> =
    combine(
      partiesState,
      itemsState,
      transactionsState,
      accountsState,
      agentsState,
      _dateRangeFilter,
    ) { arr ->
      @Suppress("UNCHECKED_CAST")
      val pState = arr[0] as UiState<List<Party>>
      @Suppress("UNCHECKED_CAST")
      val iState = arr[1] as UiState<List<InventoryItem>>
      @Suppress("UNCHECKED_CAST")
      val tState = arr[2] as UiState<List<BusinessTransaction>>
      @Suppress("UNCHECKED_CAST")
      val aState = arr[3] as UiState<List<CashBankAccount>>
      @Suppress("UNCHECKED_CAST")
      val agState = arr[4] as UiState<List<Agent>>
      val range = arr[5] as DateRangeFilter

      val parties = (pState as? UiState.Success)?.data.orEmpty()
      val items = (iState as? UiState.Success)?.data.orEmpty()
      val allActiveTxs = (tState as? UiState.Success)?.data.orEmpty().filter {
        it.status != TxStatus.CANCELLED.name
      }
      val txs = filterTransactionsByRange(allActiveTxs, range)
      val accounts = (aState as? UiState.Success)?.data.orEmpty()
      val agents = (agState as? UiState.Success)?.data.orEmpty()

      val sales = txs.filter { it.txType == TxType.SALE.name }
      val creditNotes = txs.filter { it.txType == TxType.CREDIT_NOTE.name }
      val purchases = txs.filter { it.txType == TxType.PURCHASE.name }
      val debitNotes = txs.filter { it.txType == TxType.DEBIT_NOTE.name }
      val expenses = txs.filter { it.txType == TxType.EXPENSE.name }
      val payouts = txs.filter { it.txType == TxType.COMMISSION_PAYOUT.name }

      val totalSales = MoneyUtils.round2(sales.sumOf { it.totalAmount })
      val creditNotesTotal = MoneyUtils.round2(creditNotes.sumOf { it.totalAmount })
      val netSales = MoneyUtils.round2((totalSales - creditNotesTotal).coerceAtLeast(0.0))

      val totalPurchases = MoneyUtils.round2(purchases.sumOf { it.totalAmount })
      val debitNotesTotal = MoneyUtils.round2(debitNotes.sumOf { it.totalAmount })
      val totalExpenses = MoneyUtils.round2(expenses.sumOf { it.totalAmount })

      fun cgstOf(t: BusinessTransaction): Double = when {
        t.cgstAmount > 0.0 -> t.cgstAmount
        t.isInterState || t.igstAmount > 0.0 -> 0.0
        else -> MoneyUtils.round2(t.taxAmount / 2.0)
      }
      fun sgstOf(t: BusinessTransaction): Double = when {
        t.sgstAmount > 0.0 -> t.sgstAmount
        t.isInterState || t.igstAmount > 0.0 -> 0.0
        else -> MoneyUtils.round2(t.taxAmount - cgstOf(t))
      }
      fun igstOf(t: BusinessTransaction): Double = when {
        t.igstAmount > 0.0 -> t.igstAmount
        t.isInterState -> t.taxAmount
        else -> 0.0
      }

      val cgstOut = MoneyUtils.round2(sales.sumOf(::cgstOf) - creditNotes.sumOf(::cgstOf))
      val sgstOut = MoneyUtils.round2(sales.sumOf(::sgstOf) - creditNotes.sumOf(::sgstOf))
      val igstOut = MoneyUtils.round2(sales.sumOf(::igstOf) - creditNotes.sumOf(::igstOf))
      val outputGst = MoneyUtils.round2(sales.sumOf { it.taxAmount } - creditNotes.sumOf { it.taxAmount })

      val cgstIn = MoneyUtils.round2(purchases.sumOf(::cgstOf) - debitNotes.sumOf(::cgstOf))
      val sgstIn = MoneyUtils.round2(purchases.sumOf(::sgstOf) - debitNotes.sumOf(::sgstOf))
      val igstIn = MoneyUtils.round2(purchases.sumOf(::igstOf) - debitNotes.sumOf(::igstOf))
      val inputGst = MoneyUtils.round2(purchases.sumOf { it.taxAmount } - debitNotes.sumOf { it.taxAmount })

      val totalReceivable = MoneyUtils.round2(
        parties.filter { it.isActive && it.currentBalance > 0 }.sumOf { it.currentBalance }
      )
      val totalPayable = MoneyUtils.round2(
        parties.filter { it.isActive && it.currentBalance < 0 }.sumOf { -it.currentBalance }
      )

      val stockValue = MoneyUtils.round2(
        items
          .filter { it.isActive && it.itemType == ItemType.PRODUCT.name && it.currentStock > 0 }
          .sumOf {
            val unitCost = if (it.purchasePrice > 0) it.purchasePrice else it.salePrice
            it.currentStock * unitCost
          }
      )
      val lowStock = items.filter { it.isLowStock() }
      val cashBankTotal = MoneyUtils.round2(accounts.filter { it.isActive }.sumOf { it.currentBalance })

      val commissionToPay = MoneyUtils.round2(
        agents.filter { it.isActive && it.currentBalance > 0 }.sumOf { it.currentBalance }
      )
      val commissionAccruedPeriod = MoneyUtils.round2(
        sales.sumOf { it.commissionAccruedAmount } - creditNotes.sumOf { it.commissionAccruedAmount }
      )
      val commissionPaidPeriod = MoneyUtils.round2(payouts.sumOf { it.totalAmount })

      // Historical-cost COGS using LineItem.costPrice captured at invoice time (falling back to current purchasePrice)
      fun linesCost(list: List<BusinessTransaction>): Double = list.sumOf { tx ->
        tx.lineItems.sumOf { line ->
          val unitCost = if (line.costPrice > 0.0) line.costPrice else {
            items.firstOrNull { it.id == line.itemId }?.purchasePrice ?: 0.0
          }
          unitCost * line.quantity
        }
      }
      val cogs = MoneyUtils.round2((linesCost(sales) - linesCost(creditNotes)).coerceAtLeast(0.0))
      val netProfit = MoneyUtils.round2((netSales - outputGst) - cogs - totalExpenses - commissionAccruedPeriod)

      DashboardMetrics(
        dateRange = range,
        totalSales = totalSales,
        creditNotesTotal = creditNotesTotal,
        netSales = netSales,
        totalPurchases = totalPurchases,
        debitNotesTotal = debitNotesTotal,
        totalExpenses = totalExpenses,
        cogs = cogs,
        totalReceivable = totalReceivable,
        totalPayable = totalPayable,
        cashAndBankTotal = cashBankTotal,
        stockValue = stockValue,
        lowStockItems = lowStock,
        commissionToPay = commissionToPay,
        commissionAccruedPeriod = commissionAccruedPeriod,
        commissionPaidPeriod = commissionPaidPeriod,
        netProfit = netProfit,
        outputGst = outputGst,
        cgstOutput = cgstOut,
        sgstOutput = sgstOut,
        igstOutput = igstOut,
        inputGst = inputGst,
        cgstInput = cgstIn,
        sgstInput = sgstIn,
        igstInput = igstIn,
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = DashboardMetrics(),
    )

  fun filterTransactionsByRange(
    txs: List<BusinessTransaction>,
    range: DateRangeFilter,
  ): List<BusinessTransaction> {
    if (range == DateRangeFilter.ALL_TIME) return txs
    val startMillis = rangeStartMillis(range)
    return txs.filter { it.dateMillis >= startMillis }
  }

  private fun rangeStartMillis(range: DateRangeFilter): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    when (range) {
      DateRangeFilter.TODAY -> {}
      DateRangeFilter.THIS_WEEK -> {
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
      }
      DateRangeFilter.THIS_MONTH -> {
        cal.set(Calendar.DAY_OF_MONTH, 1)
      }
      DateRangeFilter.THIS_FY -> {
        val month = cal.get(Calendar.MONTH)
        val year = cal.get(Calendar.YEAR)
        val fyStartYear = if (month >= Calendar.APRIL) year else year - 1
        cal.set(fyStartYear, Calendar.APRIL, 1)
      }
      DateRangeFilter.ALL_TIME -> return 0L
    }
    return cal.timeInMillis
  }

  // ==================== PROFILE ====================

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

  // ==================== PARTIES ====================

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

  fun archiveOrDeleteParty(party: Party) {
    viewModelScope.launch {
      val txs = (transactionsState.value as? UiState.Success)?.data.orEmpty()
      val hasTxs = txs.any { it.partyId == party.id }
      repository
        .archiveOrDeleteParty(party, hasTxs)
        .onSuccess { deleted ->
          _uiMessages.tryEmit(if (deleted) "Party deleted" else "Party archived (ledger history preserved)")
        }
        .onFailure { _uiMessages.tryEmit(it.localizedMessage ?: "Failed to remove party") }
    }
  }

  // ==================== ITEMS ====================

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

  fun adjustStock(itemId: String, deltaQty: Double, reason: String = "Manual Stock Adjustment") {
    viewModelScope.launch {
      repository
        .adjustItemStock(itemId, deltaQty, reason)
        .onSuccess { _uiMessages.tryEmit("Stock adjusted") }
        .onFailure { _uiMessages.tryEmit(it.localizedMessage ?: "Failed to adjust stock") }
    }
  }

  fun archiveOrDeleteItem(item: InventoryItem) {
    viewModelScope.launch {
      val txs = (transactionsState.value as? UiState.Success)?.data.orEmpty()
      val hasTxs = txs.any { tx -> tx.lineItems.any { it.itemId == item.id } }
      repository
        .archiveOrDeleteItem(item, hasTxs)
        .onSuccess { deleted ->
          _uiMessages.tryEmit(if (deleted) "Item deleted" else "Item archived (invoice history preserved)")
        }
        .onFailure { _uiMessages.tryEmit(it.localizedMessage ?: "Failed to remove item") }
    }
  }

  // ==================== ACCOUNTS & TRANSFERS ====================

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

  fun adjustAccountBalance(accountId: String, delta: Double, reason: String = "Manual Account Adjustment") {
    viewModelScope.launch {
      repository
        .adjustAccountBalance(accountId, delta, reason)
        .onSuccess { _uiMessages.tryEmit("Account balance updated") }
        .onFailure { _uiMessages.tryEmit(it.localizedMessage ?: "Failed to update balance") }
    }
  }

  fun transferBetweenAccounts(
    fromAccountId: String,
    toAccountId: String,
    amount: Double,
    notes: String,
    onDone: () -> Unit = {},
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      repository
        .transferBetweenAccounts(fromAccountId, toAccountId, amount, notes)
        .onSuccess {
          _uiMessages.tryEmit("Transfer recorded")
          onDone()
        }
        .onFailure { _uiMessages.tryEmit(it.localizedMessage ?: "Transfer failed") }
      _isBusy.value = false
    }
  }

  fun archiveOrDeleteAccount(account: CashBankAccount) {
    viewModelScope.launch {
      val txs = (transactionsState.value as? UiState.Success)?.data.orEmpty()
      val hasTxs = txs.any { it.accountId == account.id || it.toAccountId == account.id }
      repository
        .archiveOrDeleteAccount(account, hasTxs)
        .onSuccess { deleted ->
          _uiMessages.tryEmit(if (deleted) "Account deleted" else "Account archived")
        }
        .onFailure { _uiMessages.tryEmit(it.localizedMessage ?: "Failed to remove account") }
    }
  }

  // ==================== AGENTS & COMMISSIONS ====================

  fun saveAgent(agent: Agent, onSaved: () -> Unit = {}) {
    viewModelScope.launch {
      _isBusy.value = true
      repository
        .saveAgent(agent)
        .onSuccess {
          _uiMessages.tryEmit(if (agent.id.isBlank()) "Agent / Mistri added" else "Agent updated")
          onSaved()
        }
        .onFailure { _uiMessages.tryEmit(it.localizedMessage ?: "Failed to save agent") }
      _isBusy.value = false
    }
  }

  fun archiveOrDeleteAgent(agent: Agent) {
    viewModelScope.launch {
      val txs = (transactionsState.value as? UiState.Success)?.data.orEmpty()
      val hasTxs = txs.any { tx ->
        tx.agentId == agent.id || tx.commissionSplits.any { it.agentId == agent.id }
      }
      repository
        .archiveOrDeleteAgent(agent, hasTxs)
        .onSuccess { deleted ->
          _uiMessages.tryEmit(if (deleted) "Agent deleted" else "Agent archived (commission ledger preserved)")
        }
        .onFailure { _uiMessages.tryEmit(it.localizedMessage ?: "Failed to remove agent") }
    }
  }

  fun recordCommissionPayout(
    agent: Agent,
    grossAmount: Double,
    tdsPercent: Double,
    accountId: String,
    paymentMode: PaymentMode,
    refNumber: String,
    notes: String,
    onDone: () -> Unit = {},
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      repository
        .recordCommissionPayout(
          agent = agent,
          grossPayoutAmount = grossAmount,
          tdsPercent = tdsPercent,
          accountId = accountId,
          paymentMode = paymentMode,
          refNumber = refNumber,
          notes = notes,
        )
        .onSuccess {
          _uiMessages.tryEmit("Commission payout recorded for ${agent.name}")
          onDone()
        }
        .onFailure { _uiMessages.tryEmit(it.localizedMessage ?: "Commission payout failed") }
      _isBusy.value = false
    }
  }

  // ==================== TRANSACTIONS ====================

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
      _isBusy.value = true
      repository
        .recordSettlementPayment(
          transactionId = tx.id,
          additionalPaid = additionalPaid,
          partyId = tx.partyId,
          txType = TxType.entries.firstOrNull { it.name == tx.txType } ?: TxType.SALE,
          accountId = accountId,
        )
        .onSuccess { _uiMessages.tryEmit("Payment recorded for ${tx.refNumber}") }
        .onFailure { _uiMessages.tryEmit(it.localizedMessage ?: "Failed to record payment") }
      _isBusy.value = false
    }
  }

  fun cancelTransaction(txId: String, reason: String = "Cancelled by user") {
    viewModelScope.launch {
      _isBusy.value = true
      repository
        .cancelTransaction(txId, reason)
        .onSuccess { _uiMessages.tryEmit("Transaction cancelled & balances reversed") }
        .onFailure { _uiMessages.tryEmit(it.localizedMessage ?: "Failed to cancel transaction") }
      _isBusy.value = false
    }
  }

  fun deleteAccountAndData(
    onSuccess: () -> Unit,
    onReauthRequired: () -> Unit,
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      repository
        .deleteAccountAndAllData()
        .onSuccess {
          _uiMessages.tryEmit("Account and cloud data deleted")
          onSuccess()
        }
        .onFailure { err ->
          val msg = err.localizedMessage.orEmpty()
          if (msg.contains("recent", ignoreCase = true) || msg.contains("CREDENTIAL", ignoreCase = true)) {
            onReauthRequired()
          } else {
            _uiMessages.tryEmit(msg.ifBlank { "Account deletion failed" })
          }
        }
      _isBusy.value = false
    }
  }

  private companion object {
    const val TAG = "DukaanViewModel"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
