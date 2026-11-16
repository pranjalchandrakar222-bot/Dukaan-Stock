package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.BusinessProfile
import com.example.data.model.BusinessTransaction
import com.example.data.model.CashBankAccount
import com.example.data.model.Party
import com.example.data.model.PaymentMode
import com.example.data.model.TxStatus
import com.example.data.model.TxType
import com.example.data.model.txTypeEnum
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.TaxInvoicePreviewCard
import com.example.ui.theme.LocalDukaanColors
import com.example.ui.util.TaxAndCommissionCalculator
import com.example.ui.util.formatCurrency

enum class TransactionHubMode(val title: String, val types: List<TxType>) {
  SALE(
    title = "Sale & Billing",
    types = listOf(
      TxType.SALE,
      TxType.PAYMENT_IN,
      TxType.ESTIMATE,
      TxType.SALE_ORDER,
      TxType.DELIVERY_CHALLAN,
      TxType.CREDIT_NOTE,
    ),
  ),
  PURCHASE_EXPENSE(
    title = "Purchase & Expense",
    types = listOf(
      TxType.PURCHASE,
      TxType.PAYMENT_OUT,
      TxType.EXPENSE,
      TxType.PURCHASE_ORDER,
      TxType.DEBIT_NOTE,
    ),
  ),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionsScreen(
  mode: TransactionHubMode,
  transactions: List<BusinessTransaction>,
  profile: BusinessProfile?,
  parties: List<Party>,
  accounts: List<CashBankAccount>,
  selectedTransactionForDetail: BusinessTransaction?,
  onSelectTransaction: (BusinessTransaction?) -> Unit,
  onOpenInvoiceBuilder: (TxType) -> Unit,
  onConvertToSale: (BusinessTransaction) -> Unit,
  onCreateCreditNote: (BusinessTransaction) -> Unit,
  onRecordQuickVoucher: (BusinessTransaction) -> Unit,
  onRecordSettlement: (BusinessTransaction, Double, String) -> Unit,
  onCancelTransaction: (String, String) -> Unit,
) {
  val sym = profile?.currencySymbol ?: "₹"
  val semantic = LocalDukaanColors.current

  var selectedSubType by remember(mode) { mutableStateOf<TxType?>(null) }
  var searchQuery by remember { mutableStateOf("") }
  var showQuickVoucherType by remember { mutableStateOf<TxType?>(null) }
  var settlementTarget by remember { mutableStateOf<BusinessTransaction?>(null) }
  var cancelConfirmTarget by remember { mutableStateOf<BusinessTransaction?>(null) }

  val activeParties = remember(parties) { parties.filter { it.isActive } }
  val activeAccounts = remember(accounts) { accounts.filter { it.isActive } }

  val modeTransactions =
    remember(transactions, mode) {
      val allowed = mode.types.map { it.name }.toSet()
      transactions.filter { it.txType in allowed }
    }

  val filteredTransactions =
    remember(modeTransactions, selectedSubType, searchQuery) {
      modeTransactions.filter { tx ->
        val matchesType = selectedSubType == null || tx.txType == selectedSubType!!.name
        val matchesSearch =
          searchQuery.isBlank() ||
            tx.refNumber.contains(searchQuery, ignoreCase = true) ||
            tx.partyName.contains(searchQuery, ignoreCase = true) ||
            tx.category.contains(searchQuery, ignoreCase = true)
        matchesType && matchesSearch
      }
    }

  val activeTxs = filteredTransactions.filter { it.status != TxStatus.CANCELLED.name }
  val totalAmt = activeTxs.sumOf { it.totalAmount }
  val paidAmt = activeTxs.sumOf { it.paidAmount }
  val balanceAmt = activeTxs.sumOf { it.balanceAmount }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(mode.title, style = MaterialTheme.typography.headlineSmall)
          Text(
            "${filteredTransactions.size} entries",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          if (mode == TransactionHubMode.SALE) {
            FilledTonalButton(
              onClick = { showQuickVoucherType = TxType.PAYMENT_IN },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.height(48.dp),
            ) {
              Text("+ Payment-In")
            }
            Button(
              onClick = { onOpenInvoiceBuilder(TxType.SALE) },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.height(48.dp).testTag("new_sale_tx_button"),
            ) {
              Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("New Sale")
            }
          } else {
            FilledTonalButton(
              onClick = { showQuickVoucherType = TxType.EXPENSE },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.height(48.dp).testTag("add_expense_button"),
            ) {
              Text("+ Expense")
            }
            FilledTonalButton(
              onClick = { showQuickVoucherType = TxType.PAYMENT_OUT },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.height(48.dp),
            ) {
              Text("+ Pay-Out")
            }
            Button(
              onClick = { onOpenInvoiceBuilder(TxType.PURCHASE) },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.height(48.dp).testTag("new_purchase_tx_button"),
            ) {
              Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Purchase")
            }
          }
        }
      }
    }

    item {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        KpiMetricCard(
          title = if (mode == TransactionHubMode.SALE) "Total Sales" else "Total Spend",
          value = formatCurrency(totalAmt, sym),
          subtitle = "${activeTxs.size} active entries",
          icon = Icons.AutoMirrored.Filled.ReceiptLong,
          accentColor = MaterialTheme.colorScheme.primary,
          containerColor = MaterialTheme.colorScheme.surface,
          cardColor = semantic.infoCardBg,
          cardBorderColor = semantic.infoCardBorder,
          valueColor = MaterialTheme.colorScheme.primary,
          modifier = Modifier.weight(1f),
        )
        KpiMetricCard(
          title = if (mode == TransactionHubMode.SALE) "Collected" else "Paid",
          value = formatCurrency(paidAmt, sym),
          subtitle = "Settled",
          icon = Icons.Filled.Payments,
          accentColor = semantic.positive,
          containerColor = MaterialTheme.colorScheme.surface,
          cardColor = semantic.positiveCardBg,
          cardBorderColor = semantic.positiveCardBorder,
          valueColor = semantic.positive,
          modifier = Modifier.weight(1f),
        )
        KpiMetricCard(
          title = "Unpaid Dues",
          value = formatCurrency(balanceAmt, sym),
          subtitle = "${activeTxs.count { it.balanceAmount > 0.01 }} open",
          icon = Icons.Filled.AccountBalanceWallet,
          accentColor = semantic.warning,
          containerColor = MaterialTheme.colorScheme.surface,
          cardColor = semantic.warningCardBg,
          cardBorderColor = semantic.warningCardBorder,
          valueColor = semantic.warning,
          modifier = Modifier.weight(1f),
        )
      }
    }

    item {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search by ref no., party, or category...") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
      )
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        FilterChip(
          selected = selectedSubType == null,
          onClick = { selectedSubType = null },
          label = { Text("All") },
        )
        mode.types.forEach { t ->
          FilterChip(
            selected = selectedSubType == t,
            onClick = { selectedSubType = t },
            label = { Text(t.shortLabel) },
          )
        }
      }
    }

    if (filteredTransactions.isEmpty()) {
      item {
        val emptyType = selectedSubType ?: mode.types.first()
        EmptyStateCard(
          icon = Icons.AutoMirrored.Filled.ReceiptLong,
          title = "No ${emptyType.label} records to show",
          subtitle = "You haven't recorded any ${emptyType.label.lowercase()} yet. Create one to keep your books updated.",
          actionLabel = "+ Add ${emptyType.shortLabel}",
          onAction = {
            when (emptyType) {
              TxType.EXPENSE, TxType.PAYMENT_IN, TxType.PAYMENT_OUT -> showQuickVoucherType = emptyType
              else -> onOpenInvoiceBuilder(emptyType)
            }
          },
        )
      }
    } else {
      items(filteredTransactions, key = { it.id }) { tx ->
        TransactionRowCard(
          tx = tx,
          currencySymbol = sym,
          onClick = { onSelectTransaction(tx) },
        )
      }
    }
  }

  // Transaction Preview & Action Dialog
  val detailTx = selectedTransactionForDetail
  if (detailTx != null) {
    val detailParty = parties.firstOrNull { it.id == detailTx.partyId }
    val canSettle =
      detailTx.status != TxStatus.CANCELLED.name &&
        detailTx.balanceAmount > 0.01 &&
        (detailTx.txType == TxType.SALE.name || detailTx.txType == TxType.PURCHASE.name)
    val canConvert =
      detailTx.status != TxStatus.CANCELLED.name &&
        detailTx.status != TxStatus.CONVERTED.name &&
        detailTx.txType in listOf(TxType.ESTIMATE.name, TxType.SALE_ORDER.name, TxType.DELIVERY_CHALLAN.name)
    val canIssueReturn =
      detailTx.status != TxStatus.CANCELLED.name && detailTx.txType == TxType.SALE.name

    AlertDialog(
      onDismissRequest = { onSelectTransaction(null) },
      title = { Text("${detailTx.txTypeEnum().label} • ${detailTx.refNumber}") },
      text = {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
          TaxInvoicePreviewCard(
            transaction = detailTx,
            profile = profile,
            party = detailParty,
            accentColor = MaterialTheme.colorScheme.primary,
          )
        }
      },
      confirmButton = {
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          if (detailTx.status != TxStatus.CANCELLED.name) {
            OutlinedButton(
              onClick = {
                cancelConfirmTarget = detailTx
                onSelectTransaction(null)
              },
              colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
              modifier = Modifier.testTag("cancel_transaction_button"),
            ) {
              Icon(Icons.Filled.Block, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Cancel")
            }
          }
          if (canConvert) {
            FilledTonalButton(
              onClick = {
                onSelectTransaction(null)
                onConvertToSale(detailTx)
              },
              modifier = Modifier.testTag("convert_to_sale_button"),
            ) {
              Icon(Icons.Filled.Transform, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Convert to Sale")
            }
          }
          if (canIssueReturn) {
            FilledTonalButton(
              onClick = {
                onSelectTransaction(null)
                onCreateCreditNote(detailTx)
              },
              modifier = Modifier.testTag("issue_credit_note_button"),
            ) {
              Icon(Icons.AutoMirrored.Filled.AssignmentReturn, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Issue Return")
            }
          }
          if (canSettle) {
            Button(
              onClick = {
                settlementTarget = detailTx
                onSelectTransaction(null)
              },
            ) {
              Icon(Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Settle Due")
            }
          } else {
            Button(onClick = { onSelectTransaction(null) }) {
              Text("Close")
            }
          }
        }
      },
      dismissButton = {
        if (canSettle) {
          TextButton(onClick = { onSelectTransaction(null) }) { Text("Close") }
        }
      },
    )
  }

  // Cancel Confirmation + Reason Dialog
  val cancelTx = cancelConfirmTarget
  if (cancelTx != null) {
    CancelTransactionDialog(
      tx = cancelTx,
      onDismiss = { cancelConfirmTarget = null },
      onConfirm = { reason ->
        onCancelTransaction(cancelTx.id, reason)
        cancelConfirmTarget = null
      },
    )
  }

  // Settlement Dialog
  val settleTx = settlementTarget
  if (settleTx != null) {
    SettleBalanceDialog(
      tx = settleTx,
      accounts = activeAccounts,
      currencySymbol = sym,
      onDismiss = { settlementTarget = null },
      onConfirm = { amount, accId ->
        onRecordSettlement(settleTx, amount, accId)
        settlementTarget = null
      },
    )
  }

  // Quick Voucher Dialog (Expense, Payment-In, Payment-Out)
  val quickType = showQuickVoucherType
  if (quickType != null) {
    QuickVoucherDialog(
      txType = quickType,
      parties = activeParties,
      accounts = activeAccounts,
      transactions = transactions,
      profile = profile,
      currencySymbol = sym,
      onDismiss = { showQuickVoucherType = null },
      onSave = { tx ->
        onRecordQuickVoucher(tx)
        showQuickVoucherType = null
      },
    )
  }
}

@Composable
fun CancelTransactionDialog(
  tx: BusinessTransaction,
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit,
) {
  var reason by remember { mutableStateOf("") }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Cancel ${tx.refNumber}?") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          "Cancelling this entry will automatically reverse all stock quantities, party receivable/payable balances, cash/bank balances, and any accrued agent commissions.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
          value = reason,
          onValueChange = { reason = it },
          label = { Text("Cancellation Reason") },
          placeholder = { Text("e.g. Customer returned order / Duplicate entry") },
          modifier = Modifier.fillMaxWidth().testTag("cancel_reason_input"),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(reason.trim().ifBlank { "Cancelled by user" }) },
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        modifier = Modifier.testTag("confirm_cancel_tx_button"),
      ) {
        Text("Confirm Cancel")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Keep Entry") }
    },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettleBalanceDialog(
  tx: BusinessTransaction,
  accounts: List<CashBankAccount>,
  currencySymbol: String,
  onDismiss: () -> Unit,
  onConfirm: (Double, String) -> Unit,
) {
  var amountText by remember { mutableStateOf(tx.balanceAmount.toString()) }
  var selectedAccountId by remember(accounts) { mutableStateOf(accounts.firstOrNull()?.id ?: "") }
  var accExpanded by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Record Settlement (${tx.refNumber})") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Pending Balance: ${formatCurrency(tx.balanceAmount, currencySymbol)}")
        OutlinedTextField(
          value = amountText,
          onValueChange = { amountText = it },
          label = { Text("Amount ($currencySymbol)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        if (accounts.isNotEmpty()) {
          val acc = accounts.firstOrNull { it.id == selectedAccountId } ?: accounts.first()
          ExposedDropdownMenuBox(expanded = accExpanded, onExpandedChange = { accExpanded = it }) {
            OutlinedTextField(
              value = acc.accountName,
              onValueChange = {},
              readOnly = true,
              label = { Text("Account") },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accExpanded) },
              modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = accExpanded, onDismissRequest = { accExpanded = false }) {
              accounts.forEach { a ->
                DropdownMenuItem(
                  text = { Text(a.accountName) },
                  onClick = {
                    selectedAccountId = a.id
                    accExpanded = false
                  },
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val amt = amountText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
          if (amt > 0) onConfirm(amt, selectedAccountId)
        },
        enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0,
      ) { Text("Record Payment") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickVoucherDialog(
  txType: TxType,
  initialParty: Party? = null,
  parties: List<Party>,
  accounts: List<CashBankAccount>,
  transactions: List<BusinessTransaction> = emptyList(),
  profile: BusinessProfile? = null,
  currencySymbol: String,
  onDismiss: () -> Unit,
  onSave: (BusinessTransaction) -> Unit,
) {
  var refNumber by remember {
    mutableStateOf(TaxAndCommissionCalculator.formatDocumentRef(txType, profile))
  }
  var selectedPartyId by remember { mutableStateOf(initialParty?.id ?: "") }
  var partyName by remember { mutableStateOf(initialParty?.name ?: "") }
  var partyExpanded by remember { mutableStateOf(false) }

  var linkedTxId by remember { mutableStateOf("") }
  var linkExpanded by remember { mutableStateOf(false) }

  var category by remember { mutableStateOf(if (txType == TxType.EXPENSE) "Rent" else "") }
  var amountText by remember { mutableStateOf("") }
  var paymentMode by remember { mutableStateOf(PaymentMode.CASH) }
  var selectedAccountId by remember(accounts) { mutableStateOf(accounts.firstOrNull()?.id ?: "") }
  var accExpanded by remember { mutableStateOf(false) }
  var notes by remember { mutableStateOf("") }

  val expenseCategories = listOf("Rent", "Electricity", "Transport", "Staff Salary", "Packaging", "Tea & Misc")

  val openInvoices = remember(transactions, selectedPartyId, txType) {
    if (txType != TxType.PAYMENT_IN) {
      emptyList()
    } else {
      transactions.filter {
        it.txType == TxType.SALE.name &&
          it.status != TxStatus.CANCELLED.name &&
          it.balanceAmount > 0.01 &&
          (selectedPartyId.isBlank() || it.partyId == selectedPartyId)
      }
    }
  }
  val linkedInv = openInvoices.firstOrNull { it.id == linkedTxId }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add ${txType.label}") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = refNumber,
          onValueChange = { refNumber = it },
          label = { Text("Voucher Ref No.") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        if (txType == TxType.EXPENSE) {
          Text("Category", style = MaterialTheme.typography.labelMedium)
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            expenseCategories.forEach { cat ->
              FilterChip(
                selected = category == cat,
                onClick = { category = cat },
                label = { Text(cat) },
              )
            }
          }
          OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            label = { Text("Expense Category") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
        } else {
          ExposedDropdownMenuBox(
            expanded = partyExpanded && parties.isNotEmpty(),
            onExpandedChange = { partyExpanded = it },
          ) {
            OutlinedTextField(
              value = partyName,
              onValueChange = {
                partyName = it
                selectedPartyId = ""
                linkedTxId = ""
              },
              label = { Text("Party Name *") },
              trailingIcon = {
                if (parties.isNotEmpty()) ExposedDropdownMenuDefaults.TrailingIcon(expanded = partyExpanded)
              },
              singleLine = true,
              modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth(),
            )
            ExposedDropdownMenu(
              expanded = partyExpanded && parties.isNotEmpty(),
              onDismissRequest = { partyExpanded = false },
            ) {
              parties.forEach { p ->
                DropdownMenuItem(
                  text = { Text(p.name) },
                  onClick = {
                    selectedPartyId = p.id
                    partyName = p.name
                    linkedTxId = ""
                    partyExpanded = false
                  },
                )
              }
            }
          }

          if (txType == TxType.PAYMENT_IN && openInvoices.isNotEmpty()) {
            ExposedDropdownMenuBox(expanded = linkExpanded, onExpandedChange = { linkExpanded = it }) {
              OutlinedTextField(
                value = linkedInv?.let { "${it.refNumber} (Due ${formatCurrency(it.balanceAmount, currencySymbol)})" }
                  ?: "General On-Account Payment",
                onValueChange = {},
                readOnly = true,
                label = { Text("Link Unpaid Sale Invoice") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = linkExpanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
              )
              ExposedDropdownMenu(expanded = linkExpanded, onDismissRequest = { linkExpanded = false }) {
                DropdownMenuItem(
                  text = { Text("General On-Account Payment") },
                  onClick = {
                    linkedTxId = ""
                    linkExpanded = false
                  },
                )
                openInvoices.forEach { inv ->
                  DropdownMenuItem(
                    text = {
                      Text("${inv.refNumber} • ${inv.partyName} • Due ${formatCurrency(inv.balanceAmount, currencySymbol)}")
                    },
                    onClick = {
                      linkedTxId = inv.id
                      if (inv.partyId.isNotBlank()) {
                        selectedPartyId = inv.partyId
                        partyName = inv.partyName
                      }
                      amountText = inv.balanceAmount.toString()
                      linkExpanded = false
                    },
                  )
                }
              }
            }
          }
        }

        OutlinedTextField(
          value = amountText,
          onValueChange = { amountText = it },
          label = { Text("Amount ($currencySymbol) *") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("voucher_amount_input"),
        )

        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          PaymentMode.entries.take(4).forEach { m ->
            FilterChip(
              selected = paymentMode == m,
              onClick = { paymentMode = m },
              label = { Text(m.label) },
            )
          }
        }

        if (accounts.isNotEmpty()) {
          val acc = accounts.firstOrNull { it.id == selectedAccountId } ?: accounts.first()
          ExposedDropdownMenuBox(expanded = accExpanded, onExpandedChange = { accExpanded = it }) {
            OutlinedTextField(
              value = acc.accountName,
              onValueChange = {},
              readOnly = true,
              label = { Text("Cash / Bank Account") },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accExpanded) },
              modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = accExpanded, onDismissRequest = { accExpanded = false }) {
              accounts.forEach { a ->
                DropdownMenuItem(
                  text = { Text(a.accountName) },
                  onClick = {
                    selectedAccountId = a.id
                    accExpanded = false
                  },
                )
              }
            }
          }
        }

        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Notes / Description") },
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      val amount = amountText.toDoubleOrNull() ?: 0.0
      val validTarget = if (txType == TxType.EXPENSE) category.isNotBlank() else partyName.isNotBlank()
      Button(
        onClick = {
          onSave(
            BusinessTransaction(
              txType = txType.name,
              refNumber = refNumber.trim(),
              dateMillis = System.currentTimeMillis(),
              partyId = selectedPartyId,
              partyName = partyName.trim().ifBlank { category.trim() },
              linkedTxId = linkedTxId,
              category = category.trim(),
              paymentMode = paymentMode.name,
              accountId = selectedAccountId,
              subtotal = amount,
              totalAmount = amount,
              paidAmount = amount,
              balanceAmount = 0.0,
              status = TxStatus.PAID.name,
              notes = notes.trim(),
            )
          )
        },
        enabled = amount > 0 && validTarget,
        modifier = Modifier.testTag("save_voucher_button"),
      ) { Text("Save") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}
