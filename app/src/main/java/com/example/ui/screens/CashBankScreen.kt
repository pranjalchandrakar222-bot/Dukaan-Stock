package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.data.model.CashBankAccount
import com.example.data.model.accountTypeEnum
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.KpiMetricCard
import com.example.ui.theme.LocalDukaanColors
import com.example.ui.util.formatCurrency

@Composable
fun CashBankScreen(
  accounts: List<CashBankAccount>,
  profile: BusinessProfile?,
  onSaveAccount: (CashBankAccount, () -> Unit) -> Unit,
  onAdjustBalance: (String, Double) -> Unit,
) {
  val sym = profile?.currencySymbol ?: "₹"
  val semantic = LocalDukaanColors.current

  var showAddDialog by remember { mutableStateOf(false) }
  var adjustAccount by remember { mutableStateOf<CashBankAccount?>(null) }

  val totalLiquid = accounts.sumOf { it.currentBalance }
  val cashTotal = accounts.filter { it.accountType == AccountType.CASH.name }.sumOf { it.currentBalance }
  val bankTotal = accounts.filter { it.accountType != AccountType.CASH.name }.sumOf { it.currentBalance }

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
        Column {
          Text("Cash & Bank", style = MaterialTheme.typography.headlineSmall)
          Text(
            "Manage cash registers, bank accounts, and UPI wallets",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Button(
          onClick = { showAddDialog = true },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.height(48.dp).testTag("add_bank_account_button"),
        ) {
          Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Add Account")
        }
      }
    }

    item {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        KpiMetricCard(
          title = "Total Liquidity",
          value = formatCurrency(totalLiquid, sym),
          subtitle = "${accounts.size} active accounts",
          icon = Icons.Filled.AccountBalanceWallet,
          accentColor = MaterialTheme.colorScheme.primary,
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.weight(1f),
        )
        KpiMetricCard(
          title = "Cash in Hand",
          value = formatCurrency(cashTotal, sym),
          subtitle = "Bank & UPI: ${formatCurrency(bankTotal, sym)}",
          icon = Icons.Filled.AccountBalance,
          accentColor = semantic.positive,
          containerColor = semantic.positiveContainer,
          modifier = Modifier.weight(1f),
        )
      }
    }

    if (accounts.isEmpty()) {
      item {
        EmptyStateCard(
          icon = Icons.Filled.AccountBalance,
          title = "No bank or cash accounts yet",
          subtitle = "Add your Cash register, Bank account, or UPI handle to track incoming and outgoing money.",
          actionLabel = "Add Account",
          onAction = { showAddDialog = true },
        )
      }
    } else {
      items(accounts, key = { it.id }) { acc ->
        val type = acc.accountTypeEnum()
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
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
              Text(acc.accountName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
              Text(
                listOfNotNull(
                  type.label,
                  acc.bankName.takeIf { it.isNotBlank() },
                  acc.upiId.takeIf { it.isNotBlank() },
                ).joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = formatCurrency(acc.currentBalance, sym),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (acc.currentBalance >= 0) semantic.positive else semantic.negative,
              )
              Spacer(modifier = Modifier.height(4.dp))
              OutlinedButton(
                onClick = { adjustAccount = acc },
                shape = RoundedCornerShape(8.dp),
              ) {
                Text("Adjust")
              }
            }
          }
        }
      }
    }
  }

  if (showAddDialog) {
    AddAccountDialog(
      currencySymbol = sym,
      onDismiss = { showAddDialog = false },
      onSave = { acc ->
        onSaveAccount(acc) { showAddDialog = false }
      },
    )
  }

  val target = adjustAccount
  if (target != null) {
    AdjustAccountBalanceDialog(
      account = target,
      currencySymbol = sym,
      onDismiss = { adjustAccount = null },
      onApply = { delta ->
        onAdjustBalance(target.id, delta)
        adjustAccount = null
      },
    )
  }
}

@Composable
private fun AddAccountDialog(
  currencySymbol: String,
  onDismiss: () -> Unit,
  onSave: (CashBankAccount) -> Unit,
) {
  var name by remember { mutableStateOf("") }
  var type by remember { mutableStateOf(AccountType.BANK) }
  var bankName by remember { mutableStateOf("") }
  var accNumber by remember { mutableStateOf("") }
  var ifsc by remember { mutableStateOf("") }
  var upiId by remember { mutableStateOf("") }
  var openingText by remember { mutableStateOf("0") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add Cash / Bank Account") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
        OutlinedTextField(
          value = openingText,
          onValueChange = { openingText = it },
          label = { Text("Opening Balance ($currencySymbol)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val openBal = openingText.toDoubleOrNull() ?: 0.0
          onSave(
            CashBankAccount(
              accountName = name.trim(),
              accountType = type.name,
              bankName = bankName.trim(),
              accountNumber = accNumber.trim(),
              ifscCode = ifsc.trim(),
              upiId = upiId.trim(),
              openingBalance = openBal,
              currentBalance = openBal,
            )
          )
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
  onApply: (Double) -> Unit,
) {
  var isDeposit by remember { mutableStateOf(true) }
  var amountText by remember { mutableStateOf("") }

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
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val amt = amountText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
          if (amt > 0) onApply(if (isDeposit) amt else -amt)
        },
        enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0,
      ) { Text("Apply") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}
