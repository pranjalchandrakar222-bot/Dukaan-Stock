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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Agent
import com.example.data.model.BusinessProfile
import com.example.data.model.BusinessTransaction
import com.example.data.model.CashBankAccount
import com.example.data.model.CommissionBasis
import com.example.data.model.CommissionTrigger
import com.example.data.model.InventoryItem
import com.example.data.model.ItemType
import com.example.data.model.Party
import com.example.data.model.PaymentMode
import com.example.data.model.TxStatus
import com.example.data.model.TxType
import com.example.data.model.defaultBasisEnum
import com.example.data.model.defaultTriggerEnum
import com.example.ui.components.StatusBadgeChip
import com.example.ui.components.TaxInvoicePreviewCard
import com.example.ui.theme.LocalDukaanColors
import com.example.ui.util.AgentSplitInput
import com.example.ui.util.MoneyUtils
import com.example.ui.util.TaxAndCommissionCalculator
import com.example.ui.util.formatCurrency
import com.example.ui.util.formatDate
import com.example.ui.util.formatQty
import com.example.ui.viewmodel.DraftAgentSplit
import com.example.ui.viewmodel.DraftLine
import com.example.ui.viewmodel.InvoiceDraftState
import com.example.ui.viewmodel.InvoiceDraftViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceBuilderScreen(
  draftKey: String,
  draftViewModel: InvoiceDraftViewModel,
  initialTxType: TxType,
  initialParty: Party? = null,
  convertingFromTx: BusinessTransaction? = null,
  creditNoteForSale: BusinessTransaction? = null,
  profile: BusinessProfile?,
  parties: List<Party>,
  items: List<InventoryItem>,
  accounts: List<CashBankAccount>,
  agents: List<Agent>,
  isBusy: Boolean,
  onSaveTransaction: (BusinessTransaction, Boolean) -> Unit,
  onBack: () -> Unit,
) {
  BackHandler {
    draftViewModel.clearDraft()
    onBack()
  }

  LaunchedEffect(draftKey) {
    draftViewModel.initializeIfNeeded(
      draftKey = draftKey,
      initialTxType = initialTxType,
      initialParty = initialParty,
      profile = profile,
      accounts = accounts,
      agents = agents,
      items = items,
      convertingFromTx = convertingFromTx,
      creditNoteForSale = creditNoteForSale,
    )
  }

  val draft by draftViewModel.state.collectAsStateWithLifecycle()
  val sym = profile?.currencySymbol ?: "₹"
  val semantic = LocalDukaanColors.current

  var mobileTabIndex by remember { mutableStateOf(0) } // 0 = Form, 1 = Preview
  var itemPickerForRow by remember { mutableStateOf<Int?>(null) }
  var showDatePicker by remember { mutableStateOf(false) }
  var showDueDatePicker by remember { mutableStateOf(false) }
  var showAgentPicker by remember { mutableStateOf(false) }
  var partyMenuExpanded by remember { mutableStateOf(false) }

  val activeItems = remember(items) { items.filter { it.isActive } }
  val activeParties = remember(parties) { parties.filter { it.isActive } }
  val activeAccounts = remember(accounts) { accounts.filter { it.isActive } }
  val activeAgents = remember(agents) { agents.filter { it.isActive } }

  // Filter rows that have item name or positive price
  val activeDraftRows = remember(draft.lines) {
    draft.lines.filter { it.itemName.isNotBlank() || (it.priceText.toDoubleOrNull() ?: 0.0) > 0.0 }
  }

  val agentSplitInputs = remember(draft.agentSplits, agents) {
    draft.agentSplits.mapNotNull { ds ->
      val ag = agents.firstOrNull { it.id == ds.agentId } ?: return@mapNotNull null
      AgentSplitInput(
        agent = ag,
        sharePercent = ds.sharePercentText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 100.0,
        overrideRatePercent = ds.overrideRateText.toDoubleOrNull(),
      )
    }
  }

  val manualRateOverride = draft.manualCommissionRateText.toDoubleOrNull()
  val commissionApplies = draft.txType in setOf(
    TxType.SALE,
    TxType.ESTIMATE,
    TxType.SALE_ORDER,
    TxType.CREDIT_NOTE,
  ) && agentSplitInputs.isNotEmpty()

  val calc = remember(draft, activeDraftRows, agentSplitInputs, commissionApplies, manualRateOverride) {
    val billDisc = draft.billDiscountText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val extraAmt = draft.extraChargesAmountText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val paidInput = draft.paidText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    TaxAndCommissionCalculator.calculateInvoice(
      lines = activeDraftRows.map { it.toCalculationInput() },
      isInterState = draft.isInterState,
      billDiscountAmount = billDisc,
      extraChargesAmount = extraAmt,
      roundOffEnabled = draft.roundOffEnabled,
      paidAmount = paidInput,
      fullyPaid = draft.fullyPaidChecked,
      agentSplits = agentSplitInputs,
      commissionBasis = draft.commissionBasis,
      commissionTrigger = draft.commissionTrigger,
      manualCommissionRateOverride = manualRateOverride,
      commissionApplies = commissionApplies,
    )
  }

  val isQuoteOrOrder = draft.txType in setOf(
    TxType.ESTIMATE,
    TxType.SALE_ORDER,
    TxType.PURCHASE_ORDER,
    TxType.DELIVERY_CHALLAN,
  )

  val effectivePaid = if (isQuoteOrOrder) 0.0 else if (draft.fullyPaidChecked && draft.paidText.isBlank()) {
    calc.totalAmount
  } else {
    (draft.paidText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, calc.totalAmount)
  }
  val balanceAmount = if (isQuoteOrOrder) 0.0 else MoneyUtils.round2((calc.totalAmount - effectivePaid).coerceAtLeast(0.0))

  val txStatus = when {
    isQuoteOrOrder -> TxStatus.OPEN
    balanceAmount <= 0.01 && calc.totalAmount > 0 -> TxStatus.PAID
    effectivePaid > 0.01 -> TxStatus.PARTIAL
    else -> TxStatus.UNPAID
  }

  // Stock & business validation
  val negativeStockBlocked = remember(draft.lines, draft.txType, profile?.stopNegativeStock, items) {
    if (profile?.stopNegativeStock != true || (draft.txType != TxType.SALE && draft.txType != TxType.DEBIT_NOTE)) {
      null
    } else {
      val requestedByItem = activeDraftRows
        .filter { it.itemId.isNotBlank() && it.isProduct }
        .groupBy { it.itemId }
        .mapValues { (_, list) -> list.sumOf { it.qtyText.toDoubleOrNull() ?: 0.0 } }
      requestedByItem.entries.firstOrNull { (itemId, reqQty) ->
        val liveStock = items.firstOrNull { it.id == itemId }?.currentStock ?: 0.0
        reqQty > liveStock + 0.0001
      }?.let { (itemId, reqQty) ->
        val item = items.firstOrNull { it.id == itemId }
        "Insufficient stock for ${item?.name ?: "item"}: need ${formatQty(reqQty)}, available ${formatQty(item?.currentStock ?: 0.0)}"
      }
    }
  }

  val validationError: String? = when {
    activeDraftRows.isEmpty() -> "Add at least one item to save"
    activeDraftRows.any { (it.qtyText.toDoubleOrNull() ?: 0.0) <= 0.0 } -> "Quantity must be greater than zero"
    !isQuoteOrOrder && balanceAmount > 0.01 && draft.partyId.isBlank() && draft.partyName.isBlank() ->
      "Select or enter a Party for credit balance (${formatCurrency(balanceAmount, sym)})"
    negativeStockBlocked != null -> negativeStockBlocked
    else -> null
  }

  val primaryAgent = agentSplitInputs.firstOrNull()?.agent
  val draftTransaction = BusinessTransaction(
    txType = draft.txType.name,
    refNumber = draft.refNumber,
    dateMillis = draft.dateMillis,
    dueDateMillis = draft.dueDateMillis,
    partyId = draft.partyId,
    partyName = draft.partyName.ifBlank { "Cash / Walk-in" },
    placeOfSupply = draft.placeOfSupply,
    isInterState = draft.isInterState,
    paymentMode = draft.paymentMode.name,
    accountId = draft.selectedAccountId,
    linkedTxId = draft.linkedTxId,
    lineItems = calc.lineItems,
    subtotal = calc.subtotal,
    discountAmount = calc.lineDiscountTotal,
    billDiscountAmount = calc.billDiscountAmount,
    extraChargesLabel = draft.extraChargesLabel.ifBlank { if (calc.extraChargesAmount > 0) "Extra Charges" else "" },
    extraChargesAmount = calc.extraChargesAmount,
    cgstAmount = calc.cgstAmount,
    sgstAmount = calc.sgstAmount,
    igstAmount = calc.igstAmount,
    taxAmount = calc.taxAmount,
    roundOffAmount = calc.roundOffAmount,
    totalAmount = calc.totalAmount,
    paidAmount = effectivePaid,
    balanceAmount = balanceAmount,
    status = txStatus.name,
    agentId = primaryAgent?.id.orEmpty(),
    agentName = if (agentSplitInputs.size > 1) {
      agentSplitInputs.joinToString(", ") { it.agent.name }.take(100)
    } else {
      primaryAgent?.name.orEmpty()
    },
    commissionBasis = if (commissionApplies) draft.commissionBasis.name else CommissionBasis.TAXABLE.name,
    commissionTrigger = if (commissionApplies) draft.commissionTrigger.name else CommissionTrigger.ON_INVOICE.name,
    commissionStatus = calc.commissionStatus.name,
    commissionRatePercent = calc.commissionSplits.firstOrNull()?.ratePercent ?: 0.0,
    commissionTotalAmount = calc.commissionTotalAmount,
    commissionAccruedAmount = calc.commissionAccruedAmount,
    commissionSplits = calc.commissionSplits,
    notes = draft.notes,
  )

  val selectedPartyObj = remember(draft.partyId, parties) {
    parties.firstOrNull { it.id == draft.partyId }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text("Create ${draft.txType.label}")
            if (convertingFromTx != null) {
              Text(
                "Converting ${convertingFromTx.refNumber}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
              )
            } else if (creditNoteForSale != null) {
              Text(
                "Return for ${creditNoteForSale.refNumber}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
              )
            }
          }
        },
        navigationIcon = {
          IconButton(
            onClick = {
              draftViewModel.clearDraft()
              onBack()
            }
          ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          Button(
            onClick = {
              if (validationError == null) {
                onSaveTransaction(
                  draftTransaction,
                  !draft.refManuallyEdited,
                )
              }
            },
            enabled = validationError == null && !isBusy,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(end = 12.dp).testTag("save_invoice_button"),
          ) {
            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Save ${draft.txType.shortLabel}")
          }
        },
      )
    }
  ) { innerPadding ->
    BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
      val isWide = maxWidth >= 900.dp

      if (isWide) {
        Row(
          modifier = Modifier.fillMaxSize().padding(16.dp),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          InvoiceFormColumn(
            draft = draft,
            profile = profile,
            parties = activeParties,
            items = items,
            accounts = activeAccounts,
            agents = activeAgents,
            calcTotal = calc.totalAmount,
            balanceAmount = balanceAmount,
            cgst = calc.cgstAmount,
            sgst = calc.sgstAmount,
            igst = calc.igstAmount,
            commissionTotal = calc.commissionTotalAmount,
            commissionAccrued = calc.commissionAccruedAmount,
            validationError = validationError,
            currencySymbol = sym,
            partyMenuExpanded = partyMenuExpanded,
            onPartyMenuExpandedChange = { partyMenuExpanded = it },
            onUpdateDraft = { transform -> draftViewModel.update(transform) },
            onPickItemForRow = { rowIdx -> itemPickerForRow = rowIdx },
            onOpenDatePicker = { showDatePicker = true },
            onOpenDueDatePicker = { showDueDatePicker = true },
            onOpenAgentPicker = { showAgentPicker = true },
            modifier = Modifier.weight(1.15f).fillMaxHeight(),
          )

          Column(
            modifier =
              Modifier
                .weight(0.85f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "Customer Invoice Preview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
              )
              StatusBadgeChip(
                label = "Commissions Hidden",
                color = semantic.positive,
                containerColor = semantic.positiveContainer,
              )
            }
            TaxInvoicePreviewCard(
              transaction = draftTransaction,
              profile = profile,
              party = selectedPartyObj,
              accentColor = MaterialTheme.colorScheme.primary,
              showShareAction = false,
            )
          }
        }
      } else {
        Column(modifier = Modifier.fillMaxSize()) {
          SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
          ) {
            SegmentedButton(
              selected = mobileTabIndex == 0,
              onClick = { mobileTabIndex = 0 },
              shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) {
              Text("Edit Details")
            }
            SegmentedButton(
              selected = mobileTabIndex == 1,
              onClick = { mobileTabIndex = 1 },
              shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
              icon = { Icon(Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(16.dp)) },
            ) {
              Text("Customer Preview")
            }
          }

          if (mobileTabIndex == 0) {
            InvoiceFormColumn(
              draft = draft,
              profile = profile,
              parties = activeParties,
              items = items,
              accounts = activeAccounts,
              agents = activeAgents,
              calcTotal = calc.totalAmount,
              balanceAmount = balanceAmount,
              cgst = calc.cgstAmount,
              sgst = calc.sgstAmount,
              igst = calc.igstAmount,
              commissionTotal = calc.commissionTotalAmount,
              commissionAccrued = calc.commissionAccruedAmount,
              validationError = validationError,
              currencySymbol = sym,
              partyMenuExpanded = partyMenuExpanded,
              onPartyMenuExpandedChange = { partyMenuExpanded = it },
              onUpdateDraft = { transform -> draftViewModel.update(transform) },
              onPickItemForRow = { rowIdx -> itemPickerForRow = rowIdx },
              onOpenDatePicker = { showDatePicker = true },
              onOpenDueDatePicker = { showDueDatePicker = true },
              onOpenAgentPicker = { showAgentPicker = true },
              modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            )
          } else {
            Column(
              modifier =
                Modifier
                  .weight(1f)
                  .verticalScroll(rememberScrollState())
                  .padding(16.dp),
            ) {
              TaxInvoicePreviewCard(
                transaction = draftTransaction,
                profile = profile,
                party = selectedPartyObj,
                accentColor = MaterialTheme.colorScheme.primary,
                showShareAction = false,
              )
            }
          }
        }
      }
    }
  }

  // Item Picker Dialog
  val pickingRow = itemPickerForRow
  if (pickingRow != null) {
    var itemQuery by remember { mutableStateOf("") }
    val filteredItems = activeItems.filter {
      itemQuery.isBlank() || it.name.contains(itemQuery, true) || it.itemCode.contains(itemQuery, true)
    }
    AlertDialog(
      onDismissRequest = { itemPickerForRow = null },
      title = { Text("Select Item from Inventory") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = itemQuery,
            onValueChange = { itemQuery = it },
            placeholder = { Text("Filter items...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
          if (filteredItems.isEmpty()) {
            Text("No matching active items. Type name & rate directly on the row.")
          } else {
            LazyColumn(
              modifier = Modifier.height(300.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              itemsIndexed(filteredItems) { _, item ->
                val resolvedPrice = TaxAndCommissionCalculator.resolveItemUnitPrice(
                  item = item,
                  txType = draft.txType,
                  useWholesalePrice = draft.useWholesalePrice,
                )
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = MaterialTheme.colorScheme.surfaceContainerLow,
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                  modifier =
                    Modifier
                      .fillMaxWidth()
                      .clickable {
                        draftViewModel.update { cur ->
                          val updatedLines = cur.lines.toMutableList()
                          if (pickingRow in updatedLines.indices) {
                            updatedLines[pickingRow] = updatedLines[pickingRow].copy(
                              itemId = item.id,
                              itemName = item.name,
                              hsnSac = item.hsnSac,
                              unit = item.unit,
                              priceText = resolvedPrice.toString(),
                              costPrice = item.purchasePrice,
                              priceIncludesTax = if (cur.txType == TxType.PURCHASE || cur.txType == TxType.PURCHASE_ORDER) {
                                false
                              } else {
                                item.salePriceWithTax
                              },
                              discountText = item.discountPercent.toString(),
                              taxRate = item.taxRatePercent,
                              itemCommissionPercent = item.commissionPercent,
                              availableStock = item.currentStock,
                              isProduct = item.itemType == ItemType.PRODUCT.name,
                            )
                          }
                          cur.copy(lines = updatedLines)
                        }
                        itemPickerForRow = null
                      },
                ) {
                  Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(item.name, style = MaterialTheme.typography.titleSmall)
                      Text(
                        buildString {
                          append("Stock: ${formatQty(item.currentStock)} ${item.unit} • GST ${formatQty(item.taxRatePercent)}%")
                          if (item.salePriceWithTax) append(" (Incl)")
                          if (item.commissionPercent > 0) append(" • Comm ${formatQty(item.commissionPercent)}%")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                    Text(
                      formatCurrency(resolvedPrice, sym),
                      style = MaterialTheme.typography.labelLarge,
                      fontWeight = FontWeight.Bold,
                    )
                  }
                }
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { itemPickerForRow = null }) { Text("Close") }
      },
    )
  }

  // Date Pickers
  if (showDatePicker) {
    val dateState = rememberDatePickerState(initialSelectedDateMillis = draft.dateMillis)
    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            dateState.selectedDateMillis?.let { millis ->
              draftViewModel.update { cur ->
                val newRef = if (!cur.refManuallyEdited) {
                  TaxAndCommissionCalculator.formatDocumentRef(cur.txType, profile, millis)
                } else cur.refNumber
                cur.copy(dateMillis = millis, refNumber = newRef)
              }
            }
            showDatePicker = false
          }
        ) { Text("OK") }
      },
      dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
    ) {
      DatePicker(state = dateState)
    }
  }

  if (showDueDatePicker) {
    val initDue = if (draft.dueDateMillis > 0L) draft.dueDateMillis else draft.dateMillis + 7 * 86_400_000L
    val dateState = rememberDatePickerState(initialSelectedDateMillis = initDue)
    DatePickerDialog(
      onDismissRequest = { showDueDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            dateState.selectedDateMillis?.let { millis ->
              draftViewModel.update { it.copy(dueDateMillis = millis) }
            }
            showDueDatePicker = false
          }
        ) { Text("OK") }
      },
      dismissButton = {
        Row {
          TextButton(onClick = {
            draftViewModel.update { it.copy(dueDateMillis = 0L) }
            showDueDatePicker = false
          }) { Text("Clear") }
          TextButton(onClick = { showDueDatePicker = false }) { Text("Cancel") }
        }
      },
    ) {
      DatePicker(state = dateState)
    }
  }

  // Agent / Mistri Picker Dialog
  if (showAgentPicker) {
    AlertDialog(
      onDismissRequest = { showAgentPicker = false },
      title = { Text("Assign Agent / Mistri (Internal Only)") },
      text = {
        if (activeAgents.isEmpty()) {
          Text("No active Agents or Mistris yet. Add them from the Agents & Mistri screen to track commissions.")
        } else {
          LazyColumn(
            modifier = Modifier.height(280.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            itemsIndexed(activeAgents) { _, ag ->
              val alreadyAdded = draft.agentSplits.any { it.agentId == ag.id }
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (alreadyAdded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                else MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth().clickable {
                  draftViewModel.update { cur ->
                    if (cur.agentSplits.any { it.agentId == ag.id }) {
                      cur
                    } else {
                      val next = cur.agentSplits + DraftAgentSplit(ag.id, ag.name, ag.agentType, "100")
                      // Rebalance shares evenly when adding multiple
                      val evenShare = (100.0 / next.size).let { MoneyUtils.round2(it).toString().removeSuffix(".0") }
                      val balanced = next.map { it.copy(sharePercentText = evenShare) }
                      cur.copy(
                        agentSplits = balanced,
                        commissionBasis = if (cur.agentSplits.isEmpty()) ag.defaultBasisEnum() else cur.commissionBasis,
                        commissionTrigger = if (cur.agentSplits.isEmpty()) ag.defaultTriggerEnum() else cur.commissionTrigger,
                      )
                    }
                  }
                  showAgentPicker = false
                },
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(ag.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                      "${ag.agentType} • ${formatQty(ag.defaultPercent)}% (${ag.defaultBasisEnum().label})",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  if (alreadyAdded) {
                    Text("Assigned", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                  }
                }
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showAgentPicker = false }) { Text("Close") }
      },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InvoiceFormColumn(
  draft: InvoiceDraftState,
  profile: BusinessProfile?,
  parties: List<Party>,
  items: List<InventoryItem>,
  accounts: List<CashBankAccount>,
  agents: List<Agent>,
  calcTotal: Double,
  balanceAmount: Double,
  cgst: Double,
  sgst: Double,
  igst: Double,
  commissionTotal: Double,
  commissionAccrued: Double,
  validationError: String?,
  currencySymbol: String,
  partyMenuExpanded: Boolean,
  onPartyMenuExpandedChange: (Boolean) -> Unit,
  onUpdateDraft: ((InvoiceDraftState) -> InvoiceDraftState) -> Unit,
  onPickItemForRow: (Int) -> Unit,
  onOpenDatePicker: () -> Unit,
  onOpenDueDatePicker: () -> Unit,
  onOpenAgentPicker: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val semantic = LocalDukaanColors.current
  val docTypes = listOf(
    TxType.SALE,
    TxType.PURCHASE,
    TxType.ESTIMATE,
    TxType.SALE_ORDER,
    TxType.PURCHASE_ORDER,
    TxType.CREDIT_NOTE,
    TxType.DEBIT_NOTE,
    TxType.DELIVERY_CHALLAN,
  )

  LazyColumn(
    modifier = modifier,
    contentPadding = PaddingValues(bottom = 24.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    if (validationError != null) {
      item {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = semantic.warningContainer,
          border = BorderStroke(1.dp, semantic.warning.copy(alpha = 0.4f)),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = semantic.warning, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(validationError, style = MaterialTheme.typography.bodySmall, color = semantic.onWarningContainer)
          }
        }
      }
    }

    // Document Type & Ref No
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
          expanded = expanded,
          onExpandedChange = { expanded = it },
          modifier = Modifier.weight(1f),
        ) {
          OutlinedTextField(
            value = draft.txType.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Document Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
          )
          ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            docTypes.forEach { t ->
              DropdownMenuItem(
                text = { Text(t.label) },
                onClick = {
                  onUpdateDraft { cur ->
                    val newRef = if (!cur.refManuallyEdited) {
                      TaxAndCommissionCalculator.formatDocumentRef(t, profile, cur.dateMillis)
                    } else cur.refNumber
                    cur.copy(txType = t, refNumber = newRef)
                  }
                  expanded = false
                },
              )
            }
          }
        }
        OutlinedTextField(
          value = draft.refNumber,
          onValueChange = { txt -> onUpdateDraft { it.copy(refNumber = txt, refManuallyEdited = true) } },
          label = { Text("Invoice / Ref No.") },
          singleLine = true,
          modifier = Modifier.weight(0.85f),
        )
      }
    }

    // Dates, Party, Place of Supply, Retail/Wholesale
    item {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            OutlinedButton(
              onClick = onOpenDatePicker,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).height(48.dp),
            ) {
              Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Date: ${formatDate(draft.dateMillis)}", maxLines = 1)
            }
            OutlinedButton(
              onClick = onOpenDueDatePicker,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).height(48.dp),
            ) {
              Text(
                if (draft.dueDateMillis > 0L) "Due: ${formatDate(draft.dueDateMillis)}" else "+ Due Date",
                maxLines = 1,
              )
            }
          }

          ExposedDropdownMenuBox(
            expanded = partyMenuExpanded && parties.isNotEmpty(),
            onExpandedChange = onPartyMenuExpandedChange,
          ) {
            OutlinedTextField(
              value = draft.partyName,
              onValueChange = { txt ->
                onUpdateDraft { it.copy(partyName = txt, partyId = "", partyGstin = "") }
              },
              label = { Text("Customer / Supplier Name") },
              placeholder = { Text("Type name or pick from Parties") },
              trailingIcon = {
                if (parties.isNotEmpty()) {
                  ExposedDropdownMenuDefaults.TrailingIcon(expanded = partyMenuExpanded)
                }
              },
              singleLine = true,
              modifier =
                Modifier
                  .menuAnchor(MenuAnchorType.PrimaryEditable)
                  .fillMaxWidth()
                  .testTag("invoice_party_input"),
            )
            ExposedDropdownMenu(
              expanded = partyMenuExpanded && parties.isNotEmpty(),
              onDismissRequest = { onPartyMenuExpandedChange(false) },
            ) {
              parties.forEach { party ->
                DropdownMenuItem(
                  text = {
                    Column {
                      Text(party.name, fontWeight = FontWeight.SemiBold)
                      Text(
                        listOfNotNull(
                          party.phone.takeIf { it.isNotBlank() },
                          party.stateName.takeIf { it.isNotBlank() },
                        ).joinToString(" • ").ifBlank { party.partyType },
                        style = MaterialTheme.typography.bodySmall,
                      )
                    }
                  },
                  onClick = {
                    onUpdateDraft { cur ->
                      val pos = party.stateName.ifBlank {
                        TaxAndCommissionCalculator.stateFromGstin(party.gstin)
                      }.ifBlank { cur.placeOfSupply }
                      val autoInter = if (!cur.interStateManuallySet) {
                        TaxAndCommissionCalculator.detectInterState(
                          businessState = profile?.stateName.orEmpty(),
                          businessGstin = profile?.gstin.orEmpty(),
                          partyStateOrPlaceOfSupply = pos,
                          partyGstin = party.gstin,
                        )
                      } else cur.isInterState

                      // Auto-assign party's default Agent if none assigned yet
                      val defAgent = party.defaultAgentId.takeIf { it.isNotBlank() }?.let { id ->
                        agents.firstOrNull { it.id == id && it.isActive }
                      }
                      val nextSplits = if (cur.agentSplits.isEmpty() && defAgent != null) {
                        listOf(DraftAgentSplit(defAgent.id, defAgent.name, defAgent.agentType, "100"))
                      } else cur.agentSplits

                      cur.copy(
                        partyId = party.id,
                        partyName = party.name,
                        partyGstin = party.gstin,
                        placeOfSupply = pos,
                        isInterState = autoInter,
                        agentSplits = nextSplits,
                        commissionBasis = if (cur.agentSplits.isEmpty() && defAgent != null) defAgent.defaultBasisEnum() else cur.commissionBasis,
                        commissionTrigger = if (cur.agentSplits.isEmpty() && defAgent != null) defAgent.defaultTriggerEnum() else cur.commissionTrigger,
                      )
                    }
                    onPartyMenuExpandedChange(false)
                  },
                )
              }
            }
          }

          // Place of Supply + IGST + Price Tier
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            var posExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
              expanded = posExpanded,
              onExpandedChange = { posExpanded = it },
              modifier = Modifier.weight(1f),
            ) {
              OutlinedTextField(
                value = draft.placeOfSupply,
                onValueChange = { txt ->
                  onUpdateDraft { cur ->
                    val autoInter = TaxAndCommissionCalculator.detectInterState(
                      profile?.stateName.orEmpty(),
                      profile?.gstin.orEmpty(),
                      txt,
                      cur.partyGstin,
                    )
                    cur.copy(placeOfSupply = txt, isInterState = autoInter)
                  }
                },
                label = { Text("Place of Supply") },
                singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = posExpanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth(),
              )
              ExposedDropdownMenu(expanded = posExpanded, onDismissRequest = { posExpanded = false }) {
                TaxAndCommissionCalculator.indianStates.forEach { st ->
                  DropdownMenuItem(
                    text = { Text(st) },
                    onClick = {
                      onUpdateDraft { cur ->
                        val autoInter = TaxAndCommissionCalculator.detectInterState(
                          profile?.stateName.orEmpty(),
                          profile?.gstin.orEmpty(),
                          st,
                          cur.partyGstin,
                        )
                        cur.copy(placeOfSupply = st, isInterState = autoInter, interStateManuallySet = false)
                      }
                      posExpanded = false
                    },
                  )
                }
              }
            }

            FilterChip(
              selected = draft.isInterState,
              onClick = {
                onUpdateDraft { it.copy(isInterState = !it.isInterState, interStateManuallySet = true) }
              },
              label = { Text(if (draft.isInterState) "IGST (Inter)" else "CGST+SGST") },
            )
          }

          if (draft.txType in setOf(TxType.SALE, TxType.ESTIMATE, TxType.SALE_ORDER)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text("Wholesale Pricing Tier", style = MaterialTheme.typography.bodySmall)
              Switch(
                checked = draft.useWholesalePrice,
                onCheckedChange = { useWs ->
                  onUpdateDraft { cur ->
                    val updatedLines = cur.lines.map { dl ->
                      val matched = items.firstOrNull { it.id == dl.itemId } ?: return@map dl
                      val newPrice = TaxAndCommissionCalculator.resolveItemUnitPrice(matched, cur.txType, useWs)
                      dl.copy(priceText = newPrice.toString())
                    }
                    cur.copy(useWholesalePrice = useWs, lines = updatedLines)
                  }
                },
              )
            }
          }
        }
      }
    }

    // Line Items Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("Line Items", style = MaterialTheme.typography.titleMedium)
        OutlinedButton(
          onClick = { onUpdateDraft { it.copy(lines = it.lines + DraftLine()) } },
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.testTag("add_invoice_row_button"),
        ) {
          Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Add Row")
        }
      }
    }

    itemsIndexed(draft.lines, key = { _, line -> line.rowId }) { index, row ->
      val lineResult = TaxAndCommissionCalculator.calculateInvoice(
        lines = listOf(row.toCalculationInput()),
        isInterState = draft.isInterState,
        roundOffEnabled = false,
      )
      val lineCalc = lineResult.lineItems.firstOrNull()
      val qtyVal = row.qtyText.toDoubleOrNull() ?: 0.0
      val lowStockWarn = (draft.txType == TxType.SALE || draft.txType == TxType.DEBIT_NOTE) &&
        row.itemId.isNotBlank() && row.isProduct && qtyVal > row.availableStock + 0.0001

      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
          width = if (lowStockWarn) 1.5.dp else 1.dp,
          color = if (lowStockWarn) semantic.warning else MaterialTheme.colorScheme.outlineVariant,
        ),
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                "Item #${index + 1}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
              )
              if (lowStockWarn) {
                StatusBadgeChip(
                  label = "Stock ${formatQty(row.availableStock)}",
                  color = semantic.negative,
                  containerColor = semantic.negativeContainer,
                )
              }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
              FilterChip(
                selected = row.priceIncludesTax,
                onClick = {
                  onUpdateDraft { cur ->
                    val list = cur.lines.toMutableList()
                    if (index in list.indices) list[index] = row.copy(priceIncludesTax = !row.priceIncludesTax)
                    cur.copy(lines = list)
                  }
                },
                label = { Text(if (row.priceIncludesTax) "Tax Incl." else "Tax Excl.") },
              )
              TextButton(onClick = { onPickItemForRow(index) }) {
                Text("Pick Item")
              }
              if (draft.lines.size > 1) {
                IconButton(
                  onClick = {
                    onUpdateDraft { cur ->
                      cur.copy(lines = cur.lines.filterIndexed { i, _ -> i != index })
                    }
                  }
                ) {
                  Icon(
                    Icons.Filled.DeleteOutline,
                    contentDescription = "Remove row",
                    tint = MaterialTheme.colorScheme.error,
                  )
                }
              }
            }
          }

          OutlinedTextField(
            value = row.itemName,
            onValueChange = { txt ->
              onUpdateDraft { cur ->
                val list = cur.lines.toMutableList()
                if (index in list.indices) list[index] = row.copy(itemName = txt)
                cur.copy(lines = list)
              }
            },
            label = { Text("Item / Service Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("invoice_item_name_$index"),
          )

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = row.qtyText,
              onValueChange = { txt ->
                onUpdateDraft { cur ->
                  val list = cur.lines.toMutableList()
                  if (index in list.indices) list[index] = row.copy(qtyText = txt)
                  cur.copy(lines = list)
                }
              },
              label = { Text("Qty") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier.weight(0.7f),
            )
            OutlinedTextField(
              value = row.unit,
              onValueChange = { txt ->
                onUpdateDraft { cur ->
                  val list = cur.lines.toMutableList()
                  if (index in list.indices) list[index] = row.copy(unit = txt)
                  cur.copy(lines = list)
                }
              },
              label = { Text("Unit") },
              singleLine = true,
              modifier = Modifier.weight(0.7f),
            )
            OutlinedTextField(
              value = row.priceText,
              onValueChange = { txt ->
                onUpdateDraft { cur ->
                  val list = cur.lines.toMutableList()
                  if (index in list.indices) list[index] = row.copy(priceText = txt)
                  cur.copy(lines = list)
                }
              },
              label = { Text("Rate ($currencySymbol)") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier.weight(1.1f).testTag("invoice_item_price_$index"),
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            OutlinedTextField(
              value = row.discountText,
              onValueChange = { txt ->
                onUpdateDraft { cur ->
                  val list = cur.lines.toMutableList()
                  if (index in list.indices) list[index] = row.copy(discountText = txt)
                  cur.copy(lines = list)
                }
              },
              label = { Text("Disc %") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier.weight(0.8f),
            )

            var taxExpanded by remember { mutableStateOf(false) }
            val taxSlabs = listOf(0.0, 5.0, 12.0, 18.0, 28.0)
            ExposedDropdownMenuBox(
              expanded = taxExpanded,
              onExpandedChange = { taxExpanded = it },
              modifier = Modifier.weight(1f),
            ) {
              OutlinedTextField(
                value = "GST ${formatQty(row.taxRate)}%",
                onValueChange = {},
                readOnly = true,
                label = { Text("Tax") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = taxExpanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
              )
              ExposedDropdownMenu(expanded = taxExpanded, onDismissRequest = { taxExpanded = false }) {
                taxSlabs.forEach { slab ->
                  DropdownMenuItem(
                    text = { Text("GST ${formatQty(slab)}%") },
                    onClick = {
                      onUpdateDraft { cur ->
                        val list = cur.lines.toMutableList()
                        if (index in list.indices) list[index] = row.copy(taxRate = slab)
                        cur.copy(lines = list)
                      }
                      taxExpanded = false
                    },
                  )
                }
              }
            }

            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
              Text("Line Total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(
                formatCurrency(lineCalc?.lineTotal ?: 0.0, currencySymbol),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }
    }

    // Internal Agent / Mistri Commission Section (Strictly Internal — Hidden from Customer PDF)
    if (draft.txType in setOf(TxType.SALE, TxType.ESTIMATE, TxType.SALE_ORDER, TxType.CREDIT_NOTE)) {
      item {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = semantic.infoCardBg,
          border = BorderStroke(1.dp, semantic.infoCardBorder),
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                  modifier = Modifier.size(34.dp),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      Icons.Filled.Engineering,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(18.dp),
                    )
                  }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                  Text("Agent / Mistri", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                  Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, semantic.infoCardBorder),
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Icon(
                        Icons.Filled.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(11.dp),
                        tint = MaterialTheme.colorScheme.primary,
                      )
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(
                        "Internal only • Hidden from Customer PDF",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                      )
                    }
                  }
                }
              }
              OutlinedButton(
                onClick = onOpenAgentPicker,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("assign_agent_button"),
              ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (draft.agentSplits.isEmpty()) "Assign" else "Add Split")
              }
            }

            if (draft.agentSplits.isNotEmpty()) {
              draft.agentSplits.forEachIndexed { idx, split ->
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = MaterialTheme.colorScheme.surface,
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(split.agentName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                      Text(split.agentType, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (draft.agentSplits.size > 1) {
                      OutlinedTextField(
                        value = split.sharePercentText,
                        onValueChange = { txt ->
                          onUpdateDraft { cur ->
                            val list = cur.agentSplits.toMutableList()
                            if (idx in list.indices) list[idx] = split.copy(sharePercentText = txt)
                            cur.copy(agentSplits = list)
                          }
                        },
                        label = { Text("Share %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.width(90.dp),
                      )
                    }
                    IconButton(
                      onClick = {
                        onUpdateDraft { cur ->
                          cur.copy(agentSplits = cur.agentSplits.filterIndexed { i, _ -> i != idx })
                        }
                      }
                    ) {
                      Icon(Icons.Filled.Close, contentDescription = "Remove Agent", tint = MaterialTheme.colorScheme.error)
                    }
                  }
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                CommissionBasis.entries.forEach { b ->
                  FilterChip(
                    selected = draft.commissionBasis == b,
                    onClick = { onUpdateDraft { it.copy(commissionBasis = b) } },
                    label = { Text(b.label) },
                  )
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                CommissionTrigger.entries.forEach { tr ->
                  FilterChip(
                    selected = draft.commissionTrigger == tr,
                    onClick = { onUpdateDraft { it.copy(commissionTrigger = tr) } },
                    label = { Text(tr.label) },
                  )
                }
              }

              OutlinedTextField(
                value = draft.manualCommissionRateText,
                onValueChange = { txt -> onUpdateDraft { it.copy(manualCommissionRateText = txt) } },
                label = {
                  Text(
                    if (draft.commissionBasis == CommissionBasis.PER_UNIT) {
                      "Override Rate ($currencySymbol / unit) — Optional"
                    } else {
                      "Override Commission % (Blank = Item / Agent Default)"
                    }
                  )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("invoice_commission_override_input"),
              )

              Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, semantic.infoCardBorder),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(10.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Column {
                    Text("Total Commission", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                      formatCurrency(commissionTotal, currencySymbol),
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.primary,
                    )
                  }
                  Column(horizontalAlignment = Alignment.End) {
                    Text(
                      if (draft.commissionTrigger == CommissionTrigger.ON_COLLECTION) "Accrued on Paid" else "Accrued Now",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                      formatCurrency(commissionAccrued, currencySymbol),
                      fontWeight = FontWeight.Bold,
                      color = semantic.positive,
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    // Bill Discount, Extra Charges, Payment & Calculation Card
    item {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Charges, Payment & Totals", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
              Checkbox(
                checked = draft.roundOffEnabled,
                onCheckedChange = { chk -> onUpdateDraft { it.copy(roundOffEnabled = chk) } },
              )
              Text("Round Off", style = MaterialTheme.typography.bodySmall)
            }
          }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = draft.billDiscountText,
              onValueChange = { txt -> onUpdateDraft { it.copy(billDiscountText = txt) } },
              label = { Text("Bill Discount ($currencySymbol)") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
              value = draft.extraChargesAmountText,
              onValueChange = { txt -> onUpdateDraft { it.copy(extraChargesAmountText = txt) } },
              label = { Text("Extra Charges ($currencySymbol)") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
          }

          if ((draft.extraChargesAmountText.toDoubleOrNull() ?: 0.0) > 0) {
            OutlinedTextField(
              value = draft.extraChargesLabel,
              onValueChange = { txt -> onUpdateDraft { it.copy(extraChargesLabel = txt) } },
              label = { Text("Extra Charge Label (e.g. Freight, Loading)") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
            )
          }

          // Live Tax Split Summary
          if (cgst > 0.005 || sgst > 0.005 || igst > 0.005) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                if (draft.isInterState) "IGST Breakdown:" else "GST Breakdown:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                if (draft.isInterState) {
                  "IGST ${formatCurrency(igst, currencySymbol)}"
                } else {
                  "CGST ${formatCurrency(cgst, currencySymbol)} + SGST ${formatCurrency(sgst, currencySymbol)}"
                },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            PaymentMode.entries.forEach { mode ->
              FilterChip(
                selected = draft.paymentMode == mode,
                onClick = { onUpdateDraft { it.copy(paymentMode = mode) } },
                label = { Text(mode.label) },
              )
            }
          }

          if (accounts.isNotEmpty()) {
            var accExpanded by remember { mutableStateOf(false) }
            val selectedAcc = accounts.firstOrNull { it.id == draft.selectedAccountId } ?: accounts.first()
            ExposedDropdownMenuBox(expanded = accExpanded, onExpandedChange = { accExpanded = it }) {
              OutlinedTextField(
                value = selectedAcc.accountName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Deposit / Pay Account") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accExpanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
              )
              ExposedDropdownMenu(expanded = accExpanded, onDismissRequest = { accExpanded = false }) {
                accounts.forEach { acc ->
                  DropdownMenuItem(
                    text = { Text("${acc.accountName} (${formatCurrency(acc.currentBalance, currencySymbol)})") },
                    onClick = {
                      onUpdateDraft { it.copy(selectedAccountId = acc.id) }
                      accExpanded = false
                    },
                  )
                }
              }
            }
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
              checked = draft.fullyPaidChecked,
              onCheckedChange = { chk ->
                onUpdateDraft { it.copy(fullyPaidChecked = chk, paidText = if (chk) "" else it.paidText) }
              },
            )
            Text("Received / Paid in full (${formatCurrency(calcTotal, currencySymbol)})", style = MaterialTheme.typography.bodyMedium)
          }

          if (!draft.fullyPaidChecked) {
            OutlinedTextField(
              value = draft.paidText,
              onValueChange = { txt -> onUpdateDraft { it.copy(fullyPaidChecked = false, paidText = txt) } },
              label = { Text("Amount Received / Paid ($currencySymbol)") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
            )
          }

          OutlinedTextField(
            value = draft.notes,
            onValueChange = { txt -> onUpdateDraft { it.copy(notes = txt) } },
            label = { Text("Remarks / Description") },
            modifier = Modifier.fillMaxWidth(),
          )

          Row(
            modifier =
              Modifier
                .fillMaxWidth()
                .background(semantic.positiveContainer, RoundedCornerShape(12.dp))
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text("Balance Due", style = MaterialTheme.typography.labelMedium, color = semantic.onPositiveContainer)
              Text(
                formatCurrency(balanceAmount, currencySymbol),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = semantic.positive,
              )
            }
            Column(horizontalAlignment = Alignment.End) {
              Text("Invoice Total", style = MaterialTheme.typography.labelMedium, color = semantic.onPositiveContainer)
              Text(
                formatCurrency(calcTotal, currencySymbol),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = semantic.onPositiveContainer,
              )
            }
          }
        }
      }
    }
  }
}
