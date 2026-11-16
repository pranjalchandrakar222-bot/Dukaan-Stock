package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material3.Text as M3Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Agent
import com.example.data.model.BusinessProfile
import com.example.data.model.BusinessTransaction
import com.example.data.model.Party
import com.example.data.model.PartyType
import com.example.data.model.TxStatus
import com.example.data.model.TxType
import com.example.data.model.partyTypeEnum
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MonogramAvatar
import com.example.ui.components.StatusBadgeChip
import com.example.ui.theme.LocalDukaanColors
import com.example.ui.util.TaxAndCommissionCalculator
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
  agents: List<Agent>,
  transactions: List<BusinessTransaction>,
  profile: BusinessProfile?,
  openAddDialogInitially: Boolean = false,
  onConsumeInitialDialog: () -> Unit = {},
  onSaveParty: (Party, () -> Unit) -> Unit,
  onArchiveOrDeleteParty: (Party) -> Unit,
  onNewSaleForParty: (Party) -> Unit,
  onRecordPaymentForParty: (Party, Boolean) -> Unit,
  onTransactionClick: (BusinessTransaction) -> Unit,
) {
  val sym = profile?.currencySymbol ?: "₹"
  val semantic = LocalDukaanColors.current
  val context = LocalContext.current

  var searchQuery by remember { mutableStateOf("") }
  var filter by remember { mutableStateOf(PartyFilter.ALL) }
  var showArchived by remember { mutableStateOf(false) }
  var selectedPartyId by remember { mutableStateOf<String?>(null) }
  var editingParty by remember { mutableStateOf<Party?>(null) }
  var showEditorDialog by remember { mutableStateOf(openAddDialogInitially) }
  var confirmDeleteParty by remember { mutableStateOf<Party?>(null) }

  if (openAddDialogInitially) {
    onConsumeInitialDialog()
  }

  val filteredParties =
    remember(parties, searchQuery, filter, showArchived) {
      parties.filter { party ->
        val activeOk = showArchived || party.isActive
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
        activeOk && matchesQuery && matchesFilter
      }
    }

  val selectedParty =
    remember(parties, selectedPartyId) {
      parties.firstOrNull { it.id == selectedPartyId }
    }

  val activeCount = parties.count { it.isActive }
  val totalReceivable = parties.filter { it.isActive && it.currentBalance > 0 }.sumOf { it.currentBalance }
  val totalPayable = parties.filter { it.isActive && it.currentBalance < 0 }.sumOf { -it.currentBalance }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val isWide = maxWidth >= 840.dp

    if (!isWide && selectedParty != null) {
      BackHandler { selectedPartyId = null }
      PartyDetailPane(
        party = selectedParty,
        defaultAgentName = agents.firstOrNull { it.id == selectedParty.defaultAgentId }?.name.orEmpty(),
        partyTransactions = transactions.filter { it.partyId == selectedParty.id || it.partyName.equals(selectedParty.name, true) },
        currencySymbol = sym,
        showBackButton = true,
        onBack = { selectedPartyId = null },
        onEdit = {
          editingParty = selectedParty
          showEditorDialog = true
        },
        onDelete = { confirmDeleteParty = selectedParty },
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
          shareText(context, "Statement - ${selectedParty.name}", msg)
        },
        onTransactionClick = onTransactionClick,
        modifier = Modifier.fillMaxSize().padding(16.dp),
      )
    } else {
      Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Screen Header Row matching Stitch Mockup
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Parties (Khata)", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
              "$activeCount parties • To Receive ${formatCurrency(totalReceivable, sym)} • To Pay ${formatCurrency(totalPayable, sym)}",
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

        Spacer(modifier = Modifier.height(14.dp))

        Row(
          modifier = Modifier.fillMaxSize(),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          // Left Party List Panel
          Surface(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          ) {
            Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
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
                FilterChip(
                  selected = showArchived,
                  onClick = { showArchived = !showArchived },
                  label = { Text("Archived") },
                )
              }

              Spacer(modifier = Modifier.height(10.dp))

              if (filteredParties.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
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
                }
              } else {
                LazyColumn(
                  modifier = Modifier.weight(1f),
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  items(filteredParties, key = { it.id }) { party ->
                    val isSelected = isWide && party.id == (selectedParty?.id ?: filteredParties.firstOrNull()?.id)
                    val bal = party.currentBalance
                    val balColor =
                      when {
                        bal > 0.01 -> semantic.positive
                        bal < -0.01 -> semantic.negative
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                      }

                    Surface(
                      modifier =
                        Modifier
                          .fillMaxWidth()
                          .clickable { selectedPartyId = party.id }
                          .testTag("party_card_${party.id}"),
                      shape = RoundedCornerShape(14.dp),
                      color =
                        if (isSelected) semantic.infoCardBg
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
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                      ) {
                        MonogramAvatar(name = party.name, sizeDp = 42, cornerDp = 999)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                          Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                          ) {
                            Text(
                              text = party.name,
                              style = MaterialTheme.typography.titleSmall,
                              fontWeight = FontWeight.SemiBold,
                              maxLines = 1,
                              overflow = TextOverflow.Ellipsis,
                            )
                            if (!party.isActive) {
                              StatusBadgeChip(
                                "Archived",
                                MaterialTheme.colorScheme.outline,
                                MaterialTheme.colorScheme.surfaceVariant,
                              )
                            }
                          }
                          Text(
                            text =
                              listOfNotNull(
                                party.partyTypeEnum().label,
                                party.phone.takeIf { it.isNotBlank() }
                                  ?: party.gstin.takeIf { it.isNotBlank() }?.let { "GSTIN $it" },
                              ).joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                          )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                          text =
                            if (kotlin.math.abs(bal) <= 0.01) "Settled"
                            else formatCurrency(kotlin.math.abs(bal), sym),
                          style = MaterialTheme.typography.titleSmall,
                          fontWeight = FontWeight.Bold,
                          color = balColor,
                        )
                      }
                    }
                  }
                }
              }
            }
          }

          // Right Selected Party Khata Detail Pane (Computer / Tablet)
          if (isWide) {
            val activeParty = selectedParty ?: filteredParties.firstOrNull()
            Box(modifier = Modifier.weight(1.18f).fillMaxHeight()) {
              if (activeParty != null) {
                PartyDetailPane(
                  party = activeParty,
                  defaultAgentName = agents.firstOrNull { it.id == activeParty.defaultAgentId }?.name.orEmpty(),
                  partyTransactions =
                    transactions.filter {
                      it.partyId == activeParty.id || it.partyName.equals(activeParty.name, true)
                    },
                  currencySymbol = sym,
                  showBackButton = false,
                  onBack = {},
                  onEdit = {
                    editingParty = activeParty
                    showEditorDialog = true
                  },
                  onDelete = { confirmDeleteParty = activeParty },
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
                    shareText(context, "Statement - ${activeParty.name}", msg)
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
  }

  if (showEditorDialog) {
    PartyEditorDialog(
      initial = editingParty,
      agents = agents.filter { it.isActive },
      currencySymbol = sym,
      onDismiss = { showEditorDialog = false },
      onSave = { party ->
        onSaveParty(party) { showEditorDialog = false }
      },
    )
  }

  val delTarget = confirmDeleteParty
  if (delTarget != null) {
    val hasTxs = transactions.any { it.partyId == delTarget.id }
    val willArchive = hasTxs || kotlin.math.abs(delTarget.currentBalance) > 0.01
    AlertDialog(
      onDismissRequest = { confirmDeleteParty = null },
      title = { Text(if (willArchive) "Archive ${delTarget.name}?" else "Delete ${delTarget.name}?") },
      text = {
        Text(
          if (willArchive) {
            "${delTarget.name} has existing invoices or balance. Archiving hides them from new bills while keeping ledger history intact."
          } else {
            "Permanently delete ${delTarget.name}? This action cannot be undone."
          }
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onArchiveOrDeleteParty(delTarget)
            if (selectedPartyId == delTarget.id) selectedPartyId = null
            confirmDeleteParty = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) {
          Text(if (willArchive) "Archive" else "Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { confirmDeleteParty = null }) { Text("Cancel") }
      },
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PartyDetailPane(
  party: Party,
  defaultAgentName: String,
  partyTransactions: List<BusinessTransaction>,
  currencySymbol: String,
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
  var activeTab by remember(party.id) { mutableIntStateOf(0) }

  val nonCancelled = remember(partyTransactions) {
    partyTransactions.filter { it.status != TxStatus.CANCELLED.name }
  }
  val salesTotal = remember(nonCancelled) {
    nonCancelled.filter { it.txType == TxType.SALE.name }.sumOf { it.totalAmount }
  }
  val salesCount = remember(nonCancelled) {
    nonCancelled.count { it.txType == TxType.SALE.name }
  }
  val purchasesTotal = remember(nonCancelled) {
    nonCancelled.filter { it.txType == TxType.PURCHASE.name }.sumOf { it.totalAmount }
  }
  val unpaidInvoices = remember(nonCancelled) {
    nonCancelled.filter {
      (it.txType == TxType.SALE.name || it.txType == TxType.PURCHASE.name) && it.balanceAmount > 0.01
    }
  }
  val unpaidSum = remember(unpaidInvoices) { unpaidInvoices.sumOf { it.balanceAmount } }
  val invoiceOnlyTxs = remember(partyTransactions) {
    partyTransactions.filter {
      it.txType in setOf(
        TxType.SALE.name,
        TxType.PURCHASE.name,
        TxType.ESTIMATE.name,
        TxType.SALE_ORDER.name,
        TxType.DELIVERY_CHALLAN.name,
        TxType.CREDIT_NOTE.name,
        TxType.DEBIT_NOTE.name,
      )
    }
  }

  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(modifier = Modifier.fillMaxSize().padding(18.dp)) {
      // Party Header with Monogram Avatar, uppercase role badge, GSTIN & Edit/Delete
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
          MonogramAvatar(name = party.name, sizeDp = 50, cornerDp = 14)
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              party.name,
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              StatusBadgeChip(
                label = party.partyTypeEnum().label.uppercase(),
                color = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
              )
              val meta = listOfNotNull(
                party.phone.takeIf { it.isNotBlank() },
                party.gstin.takeIf { it.isNotBlank() }?.let { "GSTIN $it" },
                party.stateName.takeIf { it.isNotBlank() },
              ).joinToString(" • ")
              if (meta.isNotBlank()) {
                Text(
                  text = meta,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }
        Row {
          IconButton(onClick = onEdit) {
            Icon(Icons.Filled.Edit, contentDescription = "Edit Party")
          }
          IconButton(onClick = onDelete) {
            Icon(
              Icons.Filled.DeleteOutline,
              contentDescription = "Archive or Delete Party",
              tint = MaterialTheme.colorScheme.error,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Action Buttons Row: + Sale, Pay In, Pay Out, Statement
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Button(
          onClick = onNewSale,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.height(42.dp),
        ) {
          Text("+ Sale")
        }
        OutlinedButton(
          onClick = onPaymentIn,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.height(42.dp),
        ) {
          Text("Pay In")
        }
        OutlinedButton(
          onClick = onPaymentOut,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.height(42.dp),
        ) {
          Text("Pay Out")
        }
        OutlinedButton(
          onClick = onSendReminder,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.height(42.dp),
        ) {
          Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Statement")
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // 3 Tinted KPI Cards (Net Balance, Total Volume, Overdue / Unpaid)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        val netBg = when {
          bal > 0.01 -> semantic.positiveCardBg
          bal < -0.01 -> semantic.negativeCardBg
          else -> MaterialTheme.colorScheme.surfaceContainerLow
        }
        val netBorder = when {
          bal > 0.01 -> semantic.positiveCardBorder
          bal < -0.01 -> semantic.negativeCardBorder
          else -> MaterialTheme.colorScheme.outlineVariant
        }
        val netTextColor = when {
          bal > 0.01 -> semantic.positive
          bal < -0.01 -> semantic.negative
          else -> MaterialTheme.colorScheme.onSurface
        }

        PartySummaryMiniCard(
          label = when {
            bal > 0.01 -> "Net To Receive"
            bal < -0.01 -> "Net To Pay"
            else -> "Settled Balance"
          },
          value = formatCurrency(kotlin.math.abs(bal), currencySymbol),
          subtitle = if (party.openingBalance != 0.0) "Op: ${formatCurrency(party.openingBalance, currencySymbol)}" else "${nonCancelled.size} entries",
          bg = netBg,
          border = netBorder,
          valueColor = netTextColor,
          modifier = Modifier.weight(1f),
        )

        val isSupplierOnly = party.partyTypeEnum() == PartyType.SUPPLIER
        PartySummaryMiniCard(
          label = if (isSupplierOnly) "Total Purchases" else "Total Sales",
          value = formatCurrency(if (isSupplierOnly) purchasesTotal else salesTotal, currencySymbol),
          subtitle = if (isSupplierOnly) "${nonCancelled.count { it.txType == TxType.PURCHASE.name }} bills" else "$salesCount invoices",
          bg = MaterialTheme.colorScheme.surface,
          border = MaterialTheme.colorScheme.outlineVariant,
          valueColor = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.weight(1f),
        )

        PartySummaryMiniCard(
          label = "Open / Unpaid",
          value = formatCurrency(unpaidSum, currencySymbol),
          subtitle = if (unpaidInvoices.isEmpty()) "All clear" else "${unpaidInvoices.size} pending bills",
          bg = if (unpaidSum > 0.01) semantic.warningCardBg else MaterialTheme.colorScheme.surface,
          border = if (unpaidSum > 0.01) semantic.warningCardBorder else MaterialTheme.colorScheme.outlineVariant,
          valueColor = if (unpaidSum > 0.01) semantic.warning else MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.weight(1f),
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Segmented Tabs: Khata | Invoices | Info
      SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        listOf("Khata", "Invoices (${invoiceOnlyTxs.size})", "Info").forEachIndexed { idx, label ->
          SegmentedButton(
            selected = activeTab == idx,
            onClick = { activeTab = idx },
            shape = SegmentedButtonDefaults.itemShape(index = idx, count = 3),
          ) {
            M3Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      when (activeTab) {
        0 -> {
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
        1 -> {
          if (invoiceOnlyTxs.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
              Text(
                "No invoices or orders recorded for ${party.name} yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          } else {
            LazyColumn(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              items(invoiceOnlyTxs, key = { it.id }) { tx ->
                TransactionRowCard(
                  tx = tx,
                  currencySymbol = currencySymbol,
                  onClick = { onTransactionClick(tx) },
                )
              }
            }
          }
        }
        else -> {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.weight(1f).fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              PartyInfoField("Party Type", party.partyTypeEnum().label)
              if (party.phone.isNotBlank()) PartyInfoField("Phone", party.phone)
              if (party.email.isNotBlank()) PartyInfoField("Email", party.email)
              if (party.gstin.isNotBlank()) PartyInfoField("GSTIN", party.gstin)
              if (party.stateName.isNotBlank()) PartyInfoField("State / Place of Supply", party.stateName)
              if (party.billingAddress.isNotBlank()) PartyInfoField("Billing Address", party.billingAddress)
              if (party.shippingAddress.isNotBlank()) PartyInfoField("Shipping Address", party.shippingAddress)
              if (defaultAgentName.isNotBlank()) PartyInfoField("Default Agent / Mistri", defaultAgentName)
              PartyInfoField("Opening Balance", formatCurrency(party.openingBalance, currencySymbol))
              if (party.notes.isNotBlank()) PartyInfoField("Notes", party.notes)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PartySummaryMiniCard(
  label: String,
  value: String,
  subtitle: String,
  bg: Color,
  border: Color,
  valueColor: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(14.dp),
    color = bg,
    border = BorderStroke(1.dp, border),
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = valueColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun PartyInfoField(label: String, value: String) {
  Column {
    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    Spacer(modifier = Modifier.height(6.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PartyEditorDialog(
  initial: Party?,
  agents: List<Agent>,
  currencySymbol: String,
  onDismiss: () -> Unit,
  onSave: (Party) -> Unit,
) {
  var name by remember { mutableStateOf(initial?.name ?: "") }
  var partyType by remember { mutableStateOf(initial?.partyTypeEnum() ?: PartyType.CUSTOMER) }
  var phone by remember { mutableStateOf(initial?.phone ?: "") }
  var email by remember { mutableStateOf(initial?.email ?: "") }
  var gstin by remember { mutableStateOf(initial?.gstin ?: "") }
  var stateName by remember { mutableStateOf(initial?.stateName ?: "") }
  var stateExpanded by remember { mutableStateOf(false) }
  var billingAddress by remember { mutableStateOf(initial?.billingAddress ?: "") }
  var sameAsBilling by remember {
    mutableStateOf(
      initial == null || initial.shippingAddress.isBlank() || initial.shippingAddress == initial.billingAddress
    )
  }
  var shippingAddress by remember { mutableStateOf(initial?.shippingAddress ?: "") }
  var defaultAgentId by remember { mutableStateOf(initial?.defaultAgentId ?: "") }
  var agentExpanded by remember { mutableStateOf(false) }
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
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
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
          onValueChange = { txt ->
            val upper = txt.uppercase()
            gstin = upper
            val inferred = TaxAndCommissionCalculator.stateFromGstin(upper)
            if (inferred.isNotBlank() && stateName.isBlank()) {
              stateName = inferred
            }
          },
          label = { Text("GSTIN (Optional)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        ExposedDropdownMenuBox(expanded = stateExpanded, onExpandedChange = { stateExpanded = it }) {
          OutlinedTextField(
            value = stateName,
            onValueChange = { stateName = it },
            label = { Text("State (for CGST/SGST vs IGST)") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stateExpanded) },
            singleLine = true,
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth(),
          )
          ExposedDropdownMenu(expanded = stateExpanded, onDismissRequest = { stateExpanded = false }) {
            TaxAndCommissionCalculator.indianStates.forEach { st ->
              DropdownMenuItem(
                text = { Text(st) },
                onClick = {
                  stateName = st
                  stateExpanded = false
                },
              )
            }
          }
        }
        OutlinedTextField(
          value = billingAddress,
          onValueChange = { billingAddress = it },
          label = { Text("Billing Address") },
          modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(checked = sameAsBilling, onCheckedChange = { sameAsBilling = it })
          Text("Shipping address same as billing", style = MaterialTheme.typography.bodySmall)
        }
        if (!sameAsBilling) {
          OutlinedTextField(
            value = shippingAddress,
            onValueChange = { shippingAddress = it },
            label = { Text("Shipping Address") },
            modifier = Modifier.fillMaxWidth().testTag("party_shipping_address_input"),
          )
        }
        if (agents.isNotEmpty()) {
          val selectedAg = agents.firstOrNull { it.id == defaultAgentId }
          ExposedDropdownMenuBox(expanded = agentExpanded, onExpandedChange = { agentExpanded = it }) {
            OutlinedTextField(
              value = selectedAg?.name ?: "None",
              onValueChange = {},
              readOnly = true,
              label = { Text("Default Agent / Mistri (Optional)") },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = agentExpanded) },
              modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = agentExpanded, onDismissRequest = { agentExpanded = false }) {
              DropdownMenuItem(
                text = { Text("None") },
                onClick = {
                  defaultAgentId = ""
                  agentExpanded = false
                },
              )
              agents.forEach { ag ->
                DropdownMenuItem(
                  text = { Text("${ag.name} (${ag.agentType})") },
                  onClick = {
                    defaultAgentId = ag.id
                    agentExpanded = false
                  },
                )
              }
            }
          }
        }
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
          val cleanBilling = billingAddress.trim()
          val cleanShipping = if (sameAsBilling) cleanBilling else shippingAddress.trim()
          val updated =
            (initial ?: Party()).copy(
              name = name.trim(),
              partyType = partyType.name,
              phone = phone.trim(),
              email = email.trim(),
              gstin = gstin.trim().uppercase(),
              stateName = stateName.trim(),
              billingAddress = cleanBilling,
              shippingAddress = cleanShipping,
              defaultAgentId = defaultAgentId,
              openingBalance = if (initial == null) signedOpening else initial.openingBalance,
              currentBalance = if (initial == null) signedOpening else initial.currentBalance,
              isActive = true,
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
