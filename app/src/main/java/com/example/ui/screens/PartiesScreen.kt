package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.BusinessProfile
import com.example.data.model.BusinessTransaction
import com.example.data.model.Party
import com.example.data.model.PartyType
import com.example.data.model.partyTypeEnum
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusBadgeChip
import com.example.ui.theme.LocalDukaanColors
import com.example.ui.util.formatCurrency
import com.example.ui.util.shareText

private enum class PartyFilter(val label: String) {
  ALL("All"),
  CUSTOMERS("Customers"),
  SUPPLIERS("Suppliers"),
  RECEIVABLE("To Receive"),
  PAYABLE("To Pay"),
}

@Composable
fun PartiesScreen(
  parties: List<Party>,
  transactions: List<BusinessTransaction>,
  profile: BusinessProfile?,
  openAddDialogInitially: Boolean = false,
  onConsumeInitialDialog: () -> Unit = {},
  onSaveParty: (Party, () -> Unit) -> Unit,
  onDeleteParty: (String) -> Unit,
  onNewSaleForParty: (Party) -> Unit,
  onRecordPaymentForParty: (Party, Boolean) -> Unit, // isPaymentIn = true for receive, false for pay
  onTransactionClick: (BusinessTransaction) -> Unit,
) {
  val sym = profile?.currencySymbol ?: "₹"
  val semantic = LocalDukaanColors.current
  val context = LocalContext.current

  var searchQuery by remember { mutableStateOf("") }
  var filter by remember { mutableStateOf(PartyFilter.ALL) }
  var selectedPartyId by remember { mutableStateOf<String?>(null) }
  var editingParty by remember { mutableStateOf<Party?>(null) }
  var showEditorDialog by remember { mutableStateOf(openAddDialogInitially) }

  if (openAddDialogInitially) {
    onConsumeInitialDialog()
  }

  val filteredParties =
    remember(parties, searchQuery, filter) {
      parties.filter { party ->
        val matchesQuery =
          searchQuery.isBlank() ||
            party.name.contains(searchQuery, ignoreCase = true) ||
            party.phone.contains(searchQuery, ignoreCase = true) ||
            party.gstin.contains(searchQuery, ignoreCase = true)
        val matchesFilter =
          when (filter) {
            PartyFilter.ALL -> true
            PartyFilter.CUSTOMERS ->
              party.partyType == PartyType.CUSTOMER.name || party.partyType == PartyType.BOTH.name
            PartyFilter.SUPPLIERS ->
              party.partyType == PartyType.SUPPLIER.name || party.partyType == PartyType.BOTH.name
            PartyFilter.RECEIVABLE -> party.currentBalance > 0.01
            PartyFilter.PAYABLE -> party.currentBalance < -0.01
          }
        matchesQuery && matchesFilter
      }
    }

  val selectedParty =
    remember(parties, selectedPartyId) {
      parties.firstOrNull { it.id == selectedPartyId }
    }

  val totalReceivable = parties.filter { it.currentBalance > 0 }.sumOf { it.currentBalance }
  val totalPayable = parties.filter { it.currentBalance < 0 }.sumOf { -it.currentBalance }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val isWide = maxWidth >= 840.dp

    if (!isWide && selectedParty != null) {
      BackHandler { selectedPartyId = null }
      PartyDetailPane(
        party = selectedParty,
        partyTransactions = transactions.filter { it.partyId == selectedParty.id || it.partyName.equals(selectedParty.name, true) },
        currencySymbol = sym,
        businessName = profile?.businessName ?: "Dukaan Stock",
        showBackButton = true,
        onBack = { selectedPartyId = null },
        onEdit = {
          editingParty = selectedParty
          showEditorDialog = true
        },
        onDelete = {
          onDeleteParty(selectedParty.id)
          selectedPartyId = null
        },
        onNewSale = { onNewSaleForParty(selectedParty) },
        onPaymentIn = { onRecordPaymentForParty(selectedParty, true) },
        onPaymentOut = { onRecordPaymentForParty(selectedParty, false) },
        onSendReminder = {
          val due = formatCurrency(kotlin.math.abs(selectedParty.currentBalance), sym)
          val msg =
            if (selectedParty.currentBalance >= 0) {
              "Hello ${selectedParty.name}, friendly payment reminder from ${profile?.businessName ?: "Dukaan Stock"} for pending balance of $due. Thank you!"
            } else {
              "Hello ${selectedParty.name}, statement summary from ${profile?.businessName ?: "Dukaan Stock"}: Payable balance $due."
            }
          shareText(context, "Payment Reminder - ${selectedParty.name}", msg)
        },
        onTransactionClick = onTransactionClick,
        modifier = Modifier.fillMaxSize().padding(16.dp),
      )
    } else {
      Row(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        // Master List Column
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text("Parties (Khata)", style = MaterialTheme.typography.headlineSmall)
              Text(
                "To Receive: ${formatCurrency(totalReceivable, sym)}  •  To Pay: ${formatCurrency(totalPayable, sym)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Button(
              onClick = {
                editingParty = null
                showEditorDialog = true
              },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.height(48.dp).testTag("add_party_button"),
            ) {
              Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Add Party")
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search party by name, phone or GSTIN...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().testTag("party_search_input"),
          )

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            PartyFilter.entries.forEach { f ->
              FilterChip(
                selected = filter == f,
                onClick = { filter = f },
                label = { Text(f.label) },
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          if (filteredParties.isEmpty()) {
            EmptyStateCard(
              icon = Icons.Filled.People,
              title = if (parties.isEmpty()) "No parties added yet" else "No matching parties",
              subtitle = "Add your customers and suppliers to track credit balances, GSTIN details, and full party ledgers.",
              actionLabel = "Add First Party",
              onAction = {
                editingParty = null
                showEditorDialog = true
              },
            )
          } else {
            LazyColumn(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              items(filteredParties, key = { it.id }) { party ->
                val isSelected = isWide && party.id == (selectedParty?.id ?: filteredParties.firstOrNull()?.id)
                val bal = party.currentBalance
                val (balColor, balBg) =
                  when {
                    bal > 0.01 -> semantic.positive to semantic.positiveContainer
                    bal < -0.01 -> semantic.negative to semantic.negativeContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant to MaterialTheme.colorScheme.surfaceVariant
                  }

                Surface(
                  modifier =
                    Modifier
                      .fillMaxWidth()
                      .clickable { selectedPartyId = party.id }
                      .testTag("party_card_${party.id}"),
                  shape = RoundedCornerShape(16.dp),
                  color =
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    else MaterialTheme.colorScheme.surface,
                  border =
                    BorderStroke(
                      width = if (isSelected) 1.5.dp else 1.dp,
                      color =
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                    ),
                ) {
                  Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Surface(
                      shape = CircleShape,
                      color = MaterialTheme.colorScheme.primaryContainer,
                      modifier = Modifier.size(44.dp),
                    ) {
                      Box(contentAlignment = Alignment.Center) {
                        Text(
                          text = party.name.take(1).uppercase(),
                          style = MaterialTheme.typography.titleMedium,
                          fontWeight = FontWeight.Bold,
                          color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                      }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = party.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                      Text(
                        text =
                          listOfNotNull(
                            party.partyTypeEnum().label,
                            party.phone.takeIf { it.isNotBlank() },
                          ).joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                      Text(
                        text = formatCurrency(kotlin.math.abs(bal), sym),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = balColor,
                      )
                      StatusBadgeChip(
                        label =
                          when {
                            bal > 0.01 -> "To Receive"
                            bal < -0.01 -> "To Pay"
                            else -> "Settled"
                          },
                        color = balColor,
                        containerColor = balBg,
                      )
                    }
                  }
                }
              }
            }
          }
        }

        // Desktop/Tablet Master-Detail Right Pane
        if (isWide) {
          val activeParty = selectedParty ?: filteredParties.firstOrNull()
          Box(modifier = Modifier.weight(1.1f).fillMaxHeight()) {
            if (activeParty != null) {
              PartyDetailPane(
                party = activeParty,
                partyTransactions =
                  transactions.filter {
                    it.partyId == activeParty.id || it.partyName.equals(activeParty.name, true)
                  },
                currencySymbol = sym,
                businessName = profile?.businessName ?: "Dukaan Stock",
                showBackButton = false,
                onBack = {},
                onEdit = {
                  editingParty = activeParty
                  showEditorDialog = true
                },
                onDelete = {
                  onDeleteParty(activeParty.id)
                  selectedPartyId = null
                },
                onNewSale = { onNewSaleForParty(activeParty) },
                onPaymentIn = { onRecordPaymentForParty(activeParty, true) },
                onPaymentOut = { onRecordPaymentForParty(activeParty, false) },
                onSendReminder = {
                  val due = formatCurrency(kotlin.math.abs(activeParty.currentBalance), sym)
                  val msg =
                    if (activeParty.currentBalance >= 0) {
                      "Hello ${activeParty.name}, friendly payment reminder from ${profile?.businessName ?: "Dukaan Stock"} for pending balance of $due. Thank you!"
                    } else {
                      "Hello ${activeParty.name}, statement summary from ${profile?.businessName ?: "Dukaan Stock"}: Payable balance $due."
                    }
                  shareText(context, "Payment Reminder - ${activeParty.name}", msg)
                },
                onTransactionClick = onTransactionClick,
                modifier = Modifier.fillMaxSize(),
              )
            } else {
              EmptyStateCard(
                icon = Icons.Filled.People,
                title = "Select a Party",
                subtitle = "Choose a customer or supplier on the left to inspect their balance, GSTIN, and transaction history.",
              )
            }
          }
        }
      }
    }
  }

  if (showEditorDialog) {
    PartyEditorDialog(
      initial = editingParty,
      currencySymbol = sym,
      onDismiss = { showEditorDialog = false },
      onSave = { party ->
        onSaveParty(party) { showEditorDialog = false }
      },
    )
  }
}

@Composable
private fun PartyDetailPane(
  party: Party,
  partyTransactions: List<BusinessTransaction>,
  currencySymbol: String,
  businessName: String,
  showBackButton: Boolean,
  onBack: () -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  onNewSale: () -> Unit,
  onPaymentIn: () -> Unit,
  onPaymentOut: () -> Unit,
  onSendReminder: () -> Unit,
  onTransactionClick: (BusinessTransaction) -> Unit,
  modifier: Modifier = Modifier,
) {
  val semantic = LocalDukaanColors.current
  val bal = party.currentBalance

  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(modifier = Modifier.fillMaxSize().padding(18.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          if (showBackButton) {
            IconButton(onClick = onBack) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
          }
          Column {
            Text(party.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
              party.partyTypeEnum().label,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        Row {
          IconButton(onClick = onSendReminder) {
            Icon(Icons.Filled.Share, contentDescription = "Share Reminder")
          }
          IconButton(onClick = onEdit) {
            Icon(Icons.Filled.Edit, contentDescription = "Edit Party")
          }
          IconButton(onClick = onDelete) {
            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete Party", tint = MaterialTheme.colorScheme.error)
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Surface(
        shape = RoundedCornerShape(14.dp),
        color =
          when {
            bal > 0.01 -> semantic.positiveContainer
            bal < -0.01 -> semantic.negativeContainer
            else -> MaterialTheme.colorScheme.surfaceContainerLow
          },
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text(
              text =
                when {
                  bal > 0.01 -> "Receivable Balance"
                  bal < -0.01 -> "Payable Balance"
                  else -> "Balance Settled"
                },
              style = MaterialTheme.typography.labelMedium,
            )
            Text(
              text = formatCurrency(kotlin.math.abs(bal), currencySymbol),
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.Bold,
              color =
                when {
                  bal > 0.01 -> semantic.positive
                  bal < -0.01 -> semantic.negative
                  else -> MaterialTheme.colorScheme.onSurface
                },
            )
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = onPaymentIn, shape = RoundedCornerShape(10.dp)) {
              Icon(Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Pay In")
            }
            OutlinedButton(onClick = onPaymentOut, shape = RoundedCornerShape(10.dp)) {
              Text("Pay Out")
            }
            Button(onClick = onNewSale, shape = RoundedCornerShape(10.dp)) {
              Text("+ Sale")
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (party.phone.isNotBlank()) Text("Phone: ${party.phone}", style = MaterialTheme.typography.bodySmall)
        if (party.email.isNotBlank()) Text("Email: ${party.email}", style = MaterialTheme.typography.bodySmall)
        if (party.gstin.isNotBlank()) Text("GSTIN: ${party.gstin}", style = MaterialTheme.typography.bodySmall)
        if (party.billingAddress.isNotBlank()) Text("Address: ${party.billingAddress}", style = MaterialTheme.typography.bodySmall)
      }

      Spacer(modifier = Modifier.height(14.dp))
      HorizontalDivider()
      Spacer(modifier = Modifier.height(10.dp))

      Text(
        "Party Ledger (${partyTransactions.size})",
        style = MaterialTheme.typography.titleMedium,
      )
      Spacer(modifier = Modifier.height(8.dp))

      if (partyTransactions.isEmpty()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
          Text(
            "No transactions for ${party.name} yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      } else {
        LazyColumn(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(partyTransactions, key = { it.id }) { tx ->
            TransactionRowCard(
              tx = tx,
              currencySymbol = currencySymbol,
              onClick = { onTransactionClick(tx) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun PartyEditorDialog(
  initial: Party?,
  currencySymbol: String,
  onDismiss: () -> Unit,
  onSave: (Party) -> Unit,
) {
  var name by remember { mutableStateOf(initial?.name ?: "") }
  var partyType by remember { mutableStateOf(initial?.partyTypeEnum() ?: PartyType.CUSTOMER) }
  var phone by remember { mutableStateOf(initial?.phone ?: "") }
  var email by remember { mutableStateOf(initial?.email ?: "") }
  var gstin by remember { mutableStateOf(initial?.gstin ?: "") }
  var billingAddress by remember { mutableStateOf(initial?.billingAddress ?: "") }
  var openingBalanceText by remember { mutableStateOf(initial?.openingBalance?.takeIf { it != 0.0 }?.toString() ?: "") }
  var isReceivable by remember { mutableStateOf((initial?.openingBalance ?: 0.0) >= 0) }
  var notes by remember { mutableStateOf(initial?.notes ?: "") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (initial == null) "Add Party" else "Edit Party") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Party Name *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("party_name_input"),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          PartyType.entries.forEach { t ->
            FilterChip(
              selected = partyType == t,
              onClick = { partyType = t },
              label = { Text(t.label) },
            )
          }
        }
        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it },
          label = { Text("Phone Number") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text("Email") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = gstin,
          onValueChange = { gstin = it.uppercase() },
          label = { Text("GSTIN (Optional)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = billingAddress,
          onValueChange = { billingAddress = it },
          label = { Text("Billing Address") },
          modifier = Modifier.fillMaxWidth(),
        )
        if (initial == null) {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
              value = openingBalanceText,
              onValueChange = { openingBalanceText = it },
              label = { Text("Opening Bal ($currencySymbol)") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
            FilterChip(
              selected = isReceivable,
              onClick = { isReceivable = true },
              label = { Text("To Receive") },
            )
            FilterChip(
              selected = !isReceivable,
              onClick = { isReceivable = false },
              label = { Text("To Pay") },
            )
          }
        }
        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Notes") },
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val rawOpening = kotlin.math.abs(openingBalanceText.toDoubleOrNull() ?: 0.0)
          val signedOpening = if (isReceivable) rawOpening else -rawOpening
          val updated =
            (initial ?: Party()).copy(
              name = name.trim(),
              partyType = partyType.name,
              phone = phone.trim(),
              email = email.trim(),
              gstin = gstin.trim().uppercase(),
              billingAddress = billingAddress.trim(),
              shippingAddress = billingAddress.trim(),
              openingBalance = if (initial == null) signedOpening else initial.openingBalance,
              currentBalance = if (initial == null) signedOpening else initial.currentBalance,
              notes = notes.trim(),
            )
          onSave(updated)
        },
        enabled = name.trim().isNotEmpty(),
        modifier = Modifier.testTag("save_party_button"),
      ) {
        Text("Save")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    },
  )
}
