package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AccountType
import com.example.data.model.BusinessProfile
import com.example.data.model.BusinessTransaction
import com.example.data.model.CashBankAccount
import com.example.data.model.TxStatus
import com.example.data.model.TxType
import com.example.data.model.accountTypeEnum
import com.example.data.model.txTypeEnum
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.StatusBadgeChip
import com.example.ui.theme.LocalDukaanColors
import com.example.ui.util.formatCurrency
import com.example.ui.util.formatDate

@Composable
fun CashBankScreen(
  accounts: List<CashBankAccount>,
  transactions: List<BusinessTransaction>,
  profile: BusinessProfile?,
  onSaveAccount: (CashBankAccount, () -> Unit) -> Unit,
  onAdjustBalance: (String, Double, String) -> Unit,
  onTransferBetweenAccounts: (String, String, Double, String, () -> Unit) -> Unit,
  onArchiveOrDeleteAccount: (CashBankAccount) -> Unit,
) {
  val sym = profile?.currencySymbol ?: "₹"
  val semantic = LocalDukaanColors.current

  var showArchived by remember { mutableStateOf(false) }
  var showEditorDialog by remember { mutableStateOf(false) }
  var editingAccount by remember { mutableStateOf<CashBankAccount?>(null) }
  var adjustAccount by remember { mutableStateOf<CashBankAccount?>(null) }
  var showTransferDialog by remember { mutableStateOf(false) }
  var confirmDeleteAccount by remember { mutableStateOf<CashBankAccount?>(null) }

  val visibleAccounts = remember(accounts, showArchived) {
    if (showArchived) accounts else accounts.filter { it.isActive }
  }
  val activeAccounts = remember(accounts) { accounts.filter { it.isActive } }

  val totalLiquid = activeAccounts.sumOf { it.currentBalance }
  val cashTotal = activeAccounts.filter { it.accountType == AccountType.CASH.name }.sumOf { it.currentBalance }
  val bankTotal = activeAccounts.filter { it.accountType != AccountType.CASH.name }.sumOf { it.currentBalance }
  val cashMovements = remember(transactions) {
    transactions.filter { tx ->
      tx.status != TxStatus.CANCELLED.name && (
        tx.txType in setOf(
          TxType.CASH_TRANSFER.name,
          TxType.CASH_ADJUSTMENT.name,
          TxType.PAYMENT_IN.name,
          TxType.PAYMENT_OUT.name,
          TxType.EXPENSE.name,
          TxType.COMMISSION_PAYOUT.name,
        ) || tx.paidAmount > 0.01
      )
    }.take(12)
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("Cash & Bank", style = MaterialTheme.typography.headlineSmall)
          Text(
            "Manage cash registers, bank accounts, UPI wallets & transfers",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          if (activeAccounts.size >= 2) {
            FilledTonalButton(
              onClick = { showTransferDialog = true },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.height(48.dp).testTag("transfer_cash_bank_button"),
            ) {
              Icon(Icons.Filled.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Transfer")
            }
          }
          Button(
            onClick = {
              editingAccount = null
              showEditorDialog = true
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(48.dp).testTag("add_bank_account_button"),
          ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add")
          }
        }
      }
    }

    item {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        KpiMetricCard(
          title = "Total Liquidity",
          value = formatCurrency(totalLiquid, sym),
          subtitle = "${activeAccounts.size} active accounts",
          icon = Icons.Filled.AccountBalanceWallet,
          accentColor = MaterialTheme.colorScheme.primary,
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          cardColor = semantic.infoCardBg,
          cardBorderColor = semantic.infoCardBorder,
          valueColor = MaterialTheme.colorScheme.primary,
          modifier = Modifier.weight(1f),
        )
        KpiMetricCard(
          title = "Cash in Hand",
          value = formatCurrency(cashTotal, sym),
          subtitle = "Bank & UPI: ${formatCurrency(bankTotal, sym)}",
          icon = Icons.Filled.AccountBalance,
          accentColor = semantic.positive,
          containerColor = semantic.positiveContainer,
          cardColor = semantic.positiveCardBg,
          cardBorderColor = semantic.positiveCardBorder,
          valueColor = semantic.positive,
          modifier = Modifier.weight(1f),
        )
      }
    }

    if (accounts.any { !it.isActive }) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FilterChip(
            selected = !showArchived,
            onClick = { showArchived = false },
            label = { Text("Active (${activeAccounts.size})") },
          )
          FilterChip(
            selected = showArchived,
            onClick = { showArchived = true },
            label = { Text("Show Archived (${accounts.count { !it.isActive }})") },
          )
        }
      }
    }

    if (visibleAccounts.isEmpty()) {
      item {
        EmptyStateCard(
          icon = Icons.Filled.AccountBalance,
          title = "No bank or cash accounts yet",
          subtitle = "Add your Cash register, Bank account, or UPI handle to track incoming and outgoing money.",
          actionLabel = "Add Account",
          onAction = {
            editingAccount = null
            showEditorDialog = true
          },
        )
      }
    } else {
      items(visibleAccounts, key = { it.id }) { acc ->
        val type = acc.accountTypeEnum()
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector =
                  when (type) {
                    AccountType.CASH -> Icons.Filled.AccountBalanceWallet
                    AccountType.BANK -> Icons.Filled.AccountBalance
                    AccountType.UPI -> Icons.Filled.QrCode2
                  },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(30.dp),
              )
              Spacer(modifier = Modifier.width(14.dp))
              Column(modifier = Modifier.weight(1f)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Text(acc.accountName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                  if (!acc.isActive) {
                    StatusBadgeChip(
                      label = "Archived",
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    )
                  }
                }
                Text(
                  listOfNotNull(
                    type.label,
                    acc.bankName.takeIf { it.isNotBlank() },
                    acc.accountNumber.takeIf { it.isNotBlank() }?.let { "A/C ••${it.takeLast(4)}" },
                    acc.upiId.takeIf { it.isNotBlank() },
                  ).joinToString(" • "),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Text(
                text = formatCurrency(acc.currentBalance, sym),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (acc.currentBalance >= 0) semantic.positive else semantic.negative,
              )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.End,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              if (!acc.isActive) {
                TextButton(
                  onClick = { onSaveAccount(acc.copy(isActive = true)) {} },
                ) {
                  Text("Restore")
                }
              } else {
                OutlinedButton(
                  onClick = { adjustAccount = acc },
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.testTag("adjust_account_${acc.id}"),
                ) {
                  Text("Adjust Balance")
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                  onClick = {
                    editingAccount = acc
                    showEditorDialog = true
                  },
                ) {
                  Icon(Icons.Filled.Edit, contentDescription = "Edit Account")
                }
                IconButton(
                  onClick = { confirmDeleteAccount = acc },
                ) {
                  Icon(
                    Icons.Filled.DeleteOutline,
                    contentDescription = "Delete or Archive Account",
                    tint = MaterialTheme.colorScheme.error,
                  )
                }
              }
            }
          }
        }
      }
    }

    // Recent Money Movement (Account Transfers, Adjustments, Receipts, Payouts, Expenses)
    if (cashMovements.isNotEmpty()) {
      item {
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("Recent Money Movement", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          Text(
            "${cashMovements.size} entries",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      items(cashMovements, key = { "move_${it.id}" }) { tx ->
        val accName = accounts.firstOrNull { it.id == tx.accountId }?.accountName ?: "Cash / Bank"
        val toAccName = accounts.firstOrNull { it.id == tx.toAccountId }?.accountName.orEmpty()
        val movementAmount = when (tx.txTypeEnum()) {
          TxType.SALE, TxType.PURCHASE, TxType.CREDIT_NOTE, TxType.DEBIT_NOTE -> tx.paidAmount
          else -> tx.totalAmount
        }
        val isCredit = when (tx.txTypeEnum()) {
          TxType.SALE, TxType.PAYMENT_IN, TxType.DEBIT_NOTE -> true
          TxType.CASH_ADJUSTMENT -> tx.paidAmount >= 0.0
          else -> false
        }
        val isTransfer = tx.txType == TxType.CASH_TRANSFER.name
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = if (isTransfer && toAccName.isNotBlank()) {
                  "${tx.refNumber} • $accName → $toAccName"
                } else {
                  "${tx.refNumber} • ${tx.partyName.ifBlank { tx.category.ifBlank { tx.txTypeEnum().label } }}"
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
              )
              Text(
                text = "${tx.txTypeEnum().shortLabel} • $accName • ${formatDate(tx.dateMillis)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = when {
                isTransfer -> formatCurrency(movementAmount, sym)
                isCredit -> "+${formatCurrency(movementAmount, sym)}"
                else -> "-${formatCurrency(movementAmount, sym)}"
              },
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = when {
                isTransfer -> MaterialTheme.colorScheme.primary
                isCredit -> semantic.positive
                else -> semantic.negative
              },
            )
          }
        }
      }
    }
  }

  if (showEditorDialog) {
    AddEditAccountDialog(
      existing = editingAccount,
      currencySymbol = sym,
      onDismiss = { showEditorDialog = false },
      onSave = { acc ->
        onSaveAccount(acc) { showEditorDialog = false }
      },
    )
  }

  val target = adjustAccount
  if (target != null) {
    AdjustAccountBalanceDialog(
      account = target,
      currencySymbol = sym,
      onDismiss = { adjustAccount = null },
      onApply = { delta, reason ->
        onAdjustBalance(target.id, delta, reason)
        adjustAccount = null
      },
    )
  }

  if (showTransferDialog) {
    TransferBetweenAccountsDialog(
      accounts = activeAccounts,
      currencySymbol = sym,
      onDismiss = { showTransferDialog = false },
      onConfirm = { fromId, toId, amount, notes ->
        onTransferBetweenAccounts(fromId, toId, amount, notes) {
          showTransferDialog = false
        }
      },
    )
  }

  val deleteTarget = confirmDeleteAccount
  if (deleteTarget != null) {
    val hasTxs = transactions.any { it.accountId == deleteTarget.id || it.toAccountId == deleteTarget.id }
    AlertDialog(
      onDismissRequest = { confirmDeleteAccount = null },
      title = { Text(if (hasTxs) "Archive ${deleteTarget.accountName}?" else "Delete ${deleteTarget.accountName}?") },
      text = {
        Text(
          if (hasTxs) {
            "This account has recorded transactions. Archiving hides it from active billing while keeping historical cash/bank ledgers intact."
          } else {
            "Are you sure you want to permanently delete this account?"
          }
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onArchiveOrDeleteAccount(deleteTarget)
            confirmDeleteAccount = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) {
          Text(if (hasTxs) "Archive" else "Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { confirmDeleteAccount = null }) { Text("Cancel") }
      },
    )
  }
}

@Composable
private fun AddEditAccountDialog(
  existing: CashBankAccount?,
  currencySymbol: String,
  onDismiss: () -> Unit,
  onSave: (CashBankAccount) -> Unit,
) {
  var name by remember(existing) { mutableStateOf(existing?.accountName ?: "") }
  var type by remember(existing) { mutableStateOf(existing?.accountTypeEnum() ?: AccountType.BANK) }
  var bankName by remember(existing) { mutableStateOf(existing?.bankName ?: "") }
  var accNumber by remember(existing) { mutableStateOf(existing?.accountNumber ?: "") }
  var ifsc by remember(existing) { mutableStateOf(existing?.ifscCode ?: "") }
  var upiId by remember(existing) { mutableStateOf(existing?.upiId ?: "") }
  var openingText by remember(existing) { mutableStateOf(existing?.openingBalance?.toString() ?: "0") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (existing == null) "Add Cash / Bank Account" else "Edit Account") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          AccountType.entries.forEach { t ->
            FilterChip(
              selected = type == t,
              onClick = { type = t },
              label = { Text(t.name) },
            )
          }
        }
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Account Display Name *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        if (type == AccountType.BANK) {
          OutlinedTextField(
            value = bankName,
            onValueChange = { bankName = it },
            label = { Text("Bank Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = accNumber,
            onValueChange = { accNumber = it },
            label = { Text("Account Number") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = ifsc,
            onValueChange = { ifsc = it.uppercase() },
            label = { Text("IFSC Code") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
        }
        if (type == AccountType.UPI || type == AccountType.BANK) {
          OutlinedTextField(
            value = upiId,
            onValueChange = { upiId = it },
            label = { Text("UPI ID (Optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
        }
        if (existing == null) {
          OutlinedTextField(
            value = openingText,
            onValueChange = { openingText = it },
            label = { Text("Opening Balance ($currencySymbol)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val openBal = openingText.toDoubleOrNull() ?: 0.0
          val updated = (existing ?: CashBankAccount(openingBalance = openBal, currentBalance = openBal)).copy(
            accountName = name.trim(),
            accountType = type.name,
            bankName = bankName.trim(),
            accountNumber = accNumber.trim(),
            ifscCode = ifsc.trim().uppercase(),
            upiId = upiId.trim(),
          )
          onSave(updated)
        },
        enabled = name.trim().isNotEmpty(),
      ) { Text("Save") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
private fun AdjustAccountBalanceDialog(
  account: CashBankAccount,
  currencySymbol: String,
  onDismiss: () -> Unit,
  onApply: (Double, String) -> Unit,
) {
  var isDeposit by remember { mutableStateOf(true) }
  var amountText by remember { mutableStateOf("") }
  var reason by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Adjust ${account.accountName}") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Current: ${formatCurrency(account.currentBalance, currencySymbol)}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilterChip(selected = isDeposit, onClick = { isDeposit = true }, label = { Text("+ Deposit / Add") })
          FilterChip(selected = !isDeposit, onClick = { isDeposit = false }, label = { Text("- Withdraw") })
        }
        OutlinedTextField(
          value = amountText,
          onValueChange = { amountText = it },
          label = { Text("Amount ($currencySymbol)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = reason,
          onValueChange = { reason = it },
          label = { Text("Reason (e.g. Owner capital, Cash count)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val amt = amountText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
          if (amt > 0) {
            onApply(
              if (isDeposit) amt else -amt,
              reason.trim().ifBlank { "Manual Balance Adjustment" },
            )
          }
        },
        enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0,
      ) { Text("Apply") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransferBetweenAccountsDialog(
  accounts: List<CashBankAccount>,
  currencySymbol: String,
  onDismiss: () -> Unit,
  onConfirm: (String, String, Double, String) -> Unit,
) {
  var fromAccountId by remember(accounts) { mutableStateOf(accounts.firstOrNull()?.id ?: "") }
  var toAccountId by remember(accounts) { mutableStateOf(accounts.getOrNull(1)?.id ?: "") }
  var fromExpanded by remember { mutableStateOf(false) }
  var toExpanded by remember { mutableStateOf(false) }
  var amountText by remember { mutableStateOf("") }
  var notes by remember { mutableStateOf("") }

  val fromAcc = accounts.firstOrNull { it.id == fromAccountId } ?: accounts.firstOrNull()
  val toAcc = accounts.firstOrNull { it.id == toAccountId }
  val amount = amountText.toDoubleOrNull() ?: 0.0
  val valid = fromAcc != null && toAcc != null && fromAcc.id != toAcc.id && amount > 0.0

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Transfer Between Accounts") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ExposedDropdownMenuBox(expanded = fromExpanded, onExpandedChange = { fromExpanded = it }) {
          OutlinedTextField(
            value = fromAcc?.let { "${it.accountName} (${formatCurrency(it.currentBalance, currencySymbol)})" } ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("From Account *") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromExpanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
          )
          ExposedDropdownMenu(expanded = fromExpanded, onDismissRequest = { fromExpanded = false }) {
            accounts.forEach { acc ->
              DropdownMenuItem(
                text = { Text("${acc.accountName} (${formatCurrency(acc.currentBalance, currencySymbol)})") },
                onClick = {
                  fromAccountId = acc.id
                  fromExpanded = false
                },
              )
            }
          }
        }

        ExposedDropdownMenuBox(expanded = toExpanded, onExpandedChange = { toExpanded = it }) {
          OutlinedTextField(
            value = toAcc?.let { "${it.accountName} (${formatCurrency(it.currentBalance, currencySymbol)})" } ?: "Select destination",
            onValueChange = {},
            readOnly = true,
            label = { Text("To Account *") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toExpanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
          )
          ExposedDropdownMenu(expanded = toExpanded, onDismissRequest = { toExpanded = false }) {
            accounts.filter { it.id != fromAccountId }.forEach { acc ->
              DropdownMenuItem(
                text = { Text("${acc.accountName} (${formatCurrency(acc.currentBalance, currencySymbol)})") },
                onClick = {
                  toAccountId = acc.id
                  toExpanded = false
                },
              )
            }
          }
        }

        OutlinedTextField(
          value = amountText,
          onValueChange = { amountText = it },
          label = { Text("Amount ($currencySymbol) *") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("transfer_amount_input"),
        )

        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Reference / Notes (e.g. Cash Deposit to Bank)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (fromAcc != null && toAcc != null) {
            onConfirm(fromAcc.id, toAcc.id, amount, notes.trim())
          }
        },
        enabled = valid,
        modifier = Modifier.testTag("confirm_transfer_button"),
      ) {
        Text("Transfer")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}
