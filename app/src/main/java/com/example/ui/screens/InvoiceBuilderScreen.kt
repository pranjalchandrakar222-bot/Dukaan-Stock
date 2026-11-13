package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.BusinessProfile
import com.example.data.model.BusinessTransaction
import com.example.data.model.CashBankAccount
import com.example.data.model.InventoryItem
import com.example.data.model.LineItem
import com.example.data.model.Party
import com.example.data.model.PaymentMode
import com.example.data.model.TxStatus
import com.example.data.model.TxType
import com.example.ui.components.TaxInvoicePreviewCard
import com.example.ui.theme.LocalDukaanColors
import com.example.ui.util.formatCurrency
import com.example.ui.util.formatDate
import com.example.ui.util.formatQty
import kotlin.math.roundToLong

private data class DraftLine(
  val itemId: String = "",
  val itemName: String = "",
  val hsnSac: String = "",
  val unit: String = "PCS",
  val qtyText: String = "1",
  val priceText: String = "",
  val discountText: String = "0",
  val taxRate: Double = 0.0,
) {
  fun toLineItem(): LineItem {
    val qty = qtyText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val price = priceText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val discPct = discountText.toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 0.0
    val gross = qty * price
    val taxable = gross * (1.0 - discPct / 100.0)
    val taxAmt = taxable * (taxRate / 100.0)
    return LineItem(
      itemId = itemId,
      itemName = itemName,
      hsnSac = hsnSac,
      unit = unit.ifBlank { "PCS" },
      quantity = qty,
      unitPrice = price,
      discountPercent = discPct,
      taxRatePercent = taxRate,
      taxAmount = taxAmt,
      lineTotal = taxable + taxAmt,
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceBuilderScreen(
  initialTxType: TxType,
  initialParty: Party? = null,
  profile: BusinessProfile?,
  parties: List<Party>,
  items: List<InventoryItem>,
  accounts: List<CashBankAccount>,
  isBusy: Boolean,
  onSaveTransaction: (BusinessTransaction, Boolean) -> Unit,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  val sym = profile?.currencySymbol ?: "₹"
  val semantic = LocalDukaanColors.current

  var selectedTxType by remember { mutableStateOf(initialTxType) }
  val prefix =
    when (selectedTxType) {
      TxType.SALE -> profile?.invoicePrefix?.ifBlank { "INV-" } ?: "INV-"
      TxType.PURCHASE -> "PUR-"
      TxType.ESTIMATE -> "EST-"
      TxType.SALE_ORDER -> "SO-"
      TxType.PURCHASE_ORDER -> "PO-"
      TxType.CREDIT_NOTE -> "CN-"
      TxType.DEBIT_NOTE -> "DN-"
      TxType.DELIVERY_CHALLAN -> "DC-"
      else -> "TXN-"
    }
  val defaultNum = (profile?.nextInvoiceNumber ?: 1L).toString().padStart(2, '0')
  var refNumber by remember(selectedTxType) { mutableStateOf("$prefix$defaultNum") }

  var selectedPartyId by remember { mutableStateOf(initialParty?.id ?: "") }
  var partyName by remember { mutableStateOf(initialParty?.name ?: "") }
  var partyMenuExpanded by remember { mutableStateOf(false) }

  val draftLines = remember {
    mutableStateListOf<DraftLine>().apply {
      add(DraftLine())
    }
  }

  var roundOffEnabled by remember { mutableStateOf(profile?.roundOffTotal ?: true) }
  var paymentMode by remember { mutableStateOf(PaymentMode.CASH) }
  var selectedAccountId by remember(accounts) { mutableStateOf(accounts.firstOrNull()?.id ?: "") }
  var paidText by remember { mutableStateOf("") }
  var fullyPaidChecked by remember { mutableStateOf(true) }
  var notes by remember { mutableStateOf("") }

  var mobileTabIndex by remember { mutableStateOf(0) } // 0 = Form, 1 = Preview
  var itemPickerForRow by remember { mutableStateOf<Int?>(null) }

  // Live calculations
  val computedLineItems = draftLines.filter { it.itemName.isNotBlank() || (it.priceText.toDoubleOrNull() ?: 0.0) > 0.0 }
    .map { it.toLineItem() }
  val subtotal = computedLineItems.sumOf { it.quantity * it.unitPrice }
  val discountTotal =
    computedLineItems.sumOf { (it.quantity * it.unitPrice) * (it.discountPercent / 100.0) }
  val taxTotal = computedLineItems.sumOf { it.taxAmount }
  val rawTotal = subtotal - discountTotal + taxTotal
  val roundedTotal = if (roundOffEnabled) rawTotal.roundToLong().toDouble() else rawTotal
  val roundOffAmount = roundedTotal - rawTotal
  val effectivePaid =
    if (fullyPaidChecked && paidText.isBlank()) roundedTotal
    else (paidText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0).coerceAtMost(roundedTotal)
  val balanceAmount = (roundedTotal - effectivePaid).coerceAtLeast(0.0)
  val txStatus =
    when {
      selectedTxType == TxType.ESTIMATE ||
        selectedTxType == TxType.SALE_ORDER ||
        selectedTxType == TxType.PURCHASE_ORDER ||
        selectedTxType == TxType.DELIVERY_CHALLAN -> TxStatus.OPEN
      balanceAmount <= 0.01 && roundedTotal > 0 -> TxStatus.PAID
      effectivePaid > 0.01 -> TxStatus.PARTIAL
      else -> TxStatus.UNPAID
    }

  val draftTransaction =
    BusinessTransaction(
      txType = selectedTxType.name,
      refNumber = refNumber,
      dateMillis = System.currentTimeMillis(),
      partyId = selectedPartyId,
      partyName = partyName.ifBlank { "Cash / Walk-in" },
      paymentMode = paymentMode.name,
      accountId = selectedAccountId,
      lineItems = computedLineItems,
      subtotal = subtotal,
      discountAmount = discountTotal,
      taxAmount = taxTotal,
      roundOffAmount = roundOffAmount,
      totalAmount = roundedTotal,
      paidAmount = effectivePaid,
      balanceAmount = balanceAmount,
      status = txStatus.name,
      notes = notes,
    )

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Create ${selectedTxType.label}") },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          Button(
            onClick = {
              if (computedLineItems.isNotEmpty()) {
                onSaveTransaction(
                  draftTransaction,
                  selectedTxType == TxType.SALE,
                )
              }
            },
            enabled = computedLineItems.isNotEmpty() && !isBusy,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(end = 12.dp).testTag("save_invoice_button"),
          ) {
            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Save ${selectedTxType.shortLabel}")
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
          // Left: Interactive Invoice Form
          InvoiceFormColumn(
            selectedTxType = selectedTxType,
            onSelectTxType = { selectedTxType = it },
            refNumber = refNumber,
            onRefNumberChange = { refNumber = it },
            partyName = partyName,
            onPartyNameChange = {
              partyName = it
              selectedPartyId = ""
            },
            parties = parties,
            onSelectParty = { party ->
              selectedPartyId = party.id
              partyName = party.name
            },
            partyMenuExpanded = partyMenuExpanded,
            onPartyMenuExpandedChange = { partyMenuExpanded = it },
            draftLines = draftLines,
            onPickItemForRow = { rowIdx -> itemPickerForRow = rowIdx },
            roundOffEnabled = roundOffEnabled,
            onRoundOffChange = { roundOffEnabled = it },
            paymentMode = paymentMode,
            onPaymentModeChange = { paymentMode = it },
            accounts = accounts,
            selectedAccountId = selectedAccountId,
            onSelectAccount = { selectedAccountId = it },
            fullyPaidChecked = fullyPaidChecked,
            onFullyPaidChange = {
              fullyPaidChecked = it
              if (it) paidText = ""
            },
            paidText = paidText,
            onPaidTextChange = {
              fullyPaidChecked = false
              paidText = it
            },
            notes = notes,
            onNotesChange = { notes = it },
            totalAmount = roundedTotal,
            balanceAmount = balanceAmount,
            currencySymbol = sym,
            modifier = Modifier.weight(1.15f).fillMaxHeight(),
          )

          // Right: Live Tax Invoice Preview (just like the reference screenshot)
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
                text = "Live Tax Invoice Preview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
              )
              Surface(
                shape = RoundedCornerShape(999.dp),
                color = semantic.positiveContainer,
              ) {
                Text(
                  text = "Auto-updating",
                  style = MaterialTheme.typography.labelSmall,
                  color = semantic.positive,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
              }
            }
            TaxInvoicePreviewCard(
              transaction = draftTransaction,
              profile = profile,
              accentColor = MaterialTheme.colorScheme.primary,
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
              Text("Live Preview")
            }
          }

          if (mobileTabIndex == 0) {
            InvoiceFormColumn(
              selectedTxType = selectedTxType,
              onSelectTxType = { selectedTxType = it },
              refNumber = refNumber,
              onRefNumberChange = { refNumber = it },
              partyName = partyName,
              onPartyNameChange = {
                partyName = it
                selectedPartyId = ""
              },
              parties = parties,
              onSelectParty = { party ->
                selectedPartyId = party.id
                partyName = party.name
              },
              partyMenuExpanded = partyMenuExpanded,
              onPartyMenuExpandedChange = { partyMenuExpanded = it },
              draftLines = draftLines,
              onPickItemForRow = { rowIdx -> itemPickerForRow = rowIdx },
              roundOffEnabled = roundOffEnabled,
              onRoundOffChange = { roundOffEnabled = it },
              paymentMode = paymentMode,
              onPaymentModeChange = { paymentMode = it },
              accounts = accounts,
              selectedAccountId = selectedAccountId,
              onSelectAccount = { selectedAccountId = it },
              fullyPaidChecked = fullyPaidChecked,
              onFullyPaidChange = {
                fullyPaidChecked = it
                if (it) paidText = ""
              },
              paidText = paidText,
              onPaidTextChange = {
                fullyPaidChecked = false
                paidText = it
              },
              notes = notes,
              onNotesChange = { notes = it },
              totalAmount = roundedTotal,
              balanceAmount = balanceAmount,
              currencySymbol = sym,
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
                accentColor = MaterialTheme.colorScheme.primary,
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
    AlertDialog(
      onDismissRequest = { itemPickerForRow = null },
      title = { Text("Select Item from Inventory") },
      text = {
        if (items.isEmpty()) {
          Text("No saved items yet. Type an item name & rate directly in the row, or add items in the Items tab.")
        } else {
          LazyColumn(
            modifier = Modifier.height(320.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            itemsIndexed(items) { _, item ->
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier =
                  Modifier
                    .fillMaxWidth()
                    .clickable {
                      val basePrice =
                        if (selectedTxType == TxType.PURCHASE || selectedTxType == TxType.PURCHASE_ORDER) {
                          item.purchasePrice
                        } else {
                          item.salePrice
                        }
                      draftLines[pickingRow] =
                        draftLines[pickingRow].copy(
                          itemId = item.id,
                          itemName = item.name,
                          hsnSac = item.hsnSac,
                          unit = item.unit,
                          priceText = basePrice.toString(),
                          discountText = item.discountPercent.toString(),
                          taxRate = item.taxRatePercent,
                        )
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
                      "Stock: ${formatQty(item.currentStock)} ${item.unit} • GST ${formatQty(item.taxRatePercent)}%",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  Text(
                    formatCurrency(item.salePrice, sym),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                  )
                }
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { itemPickerForRow = null }) {
          Text("Close")
        }
      },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InvoiceFormColumn(
  selectedTxType: TxType,
  onSelectTxType: (TxType) -> Unit,
  refNumber: String,
  onRefNumberChange: (String) -> Unit,
  partyName: String,
  onPartyNameChange: (String) -> Unit,
  parties: List<Party>,
  onSelectParty: (Party) -> Unit,
  partyMenuExpanded: Boolean,
  onPartyMenuExpandedChange: (Boolean) -> Unit,
  draftLines: MutableList<DraftLine>,
  onPickItemForRow: (Int) -> Unit,
  roundOffEnabled: Boolean,
  onRoundOffChange: (Boolean) -> Unit,
  paymentMode: PaymentMode,
  onPaymentModeChange: (PaymentMode) -> Unit,
  accounts: List<CashBankAccount>,
  selectedAccountId: String,
  onSelectAccount: (String) -> Unit,
  fullyPaidChecked: Boolean,
  onFullyPaidChange: (Boolean) -> Unit,
  paidText: String,
  onPaidTextChange: (String) -> Unit,
  notes: String,
  onNotesChange: (String) -> Unit,
  totalAmount: Double,
  balanceAmount: Double,
  currencySymbol: String,
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
    // Document Type Selector
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
            value = selectedTxType.label,
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
                  onSelectTxType(t)
                  expanded = false
                },
              )
            }
          }
        }
        OutlinedTextField(
          value = refNumber,
          onValueChange = onRefNumberChange,
          label = { Text("Invoice / Ref No.") },
          singleLine = true,
          modifier = Modifier.weight(0.8f),
        )
      }
    }

    // Party Selector + Date
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
          ) {
            Text("Bill To / Party", style = MaterialTheme.typography.titleSmall)
            Text(
              "Date: ${formatDate(System.currentTimeMillis())}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          ExposedDropdownMenuBox(
            expanded = partyMenuExpanded && parties.isNotEmpty(),
            onExpandedChange = onPartyMenuExpandedChange,
          ) {
            OutlinedTextField(
              value = partyName,
              onValueChange = onPartyNameChange,
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
                      if (party.phone.isNotBlank()) {
                        Text(party.phone, style = MaterialTheme.typography.bodySmall)
                      }
                    }
                  },
                  onClick = {
                    onSelectParty(party)
                    onPartyMenuExpandedChange(false)
                  },
                )
              }
            }
          }
        }
      }
    }

    // Line Items Section
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("Line Items", style = MaterialTheme.typography.titleMedium)
        OutlinedButton(
          onClick = { draftLines.add(DraftLine()) },
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.testTag("add_invoice_row_button"),
        ) {
          Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Add Row")
        }
      }
    }

    itemsIndexed(draftLines) { index, row ->
      val lineCalc = row.toLineItem()
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              "Item #${index + 1}",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.primary,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
              TextButton(onClick = { onPickItemForRow(index) }) {
                Text("Pick Item")
              }
              if (draftLines.size > 1) {
                IconButton(onClick = { draftLines.removeAt(index) }) {
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
            onValueChange = { draftLines[index] = row.copy(itemName = it) },
            label = { Text("Item / Service Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("invoice_item_name_$index"),
          )

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = row.qtyText,
              onValueChange = { draftLines[index] = row.copy(qtyText = it) },
              label = { Text("Qty") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier.weight(0.7f),
            )
            OutlinedTextField(
              value = row.unit,
              onValueChange = { draftLines[index] = row.copy(unit = it) },
              label = { Text("Unit") },
              singleLine = true,
              modifier = Modifier.weight(0.7f),
            )
            OutlinedTextField(
              value = row.priceText,
              onValueChange = { draftLines[index] = row.copy(priceText = it) },
              label = { Text("Price ($currencySymbol)") },
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
              onValueChange = { draftLines[index] = row.copy(discountText = it) },
              label = { Text("Disc %") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier.weight(0.8f),
            )

            // GST Rate Picker
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
                      draftLines[index] = row.copy(taxRate = slab)
                      taxExpanded = false
                    },
                  )
                }
              }
            }

            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
              Text("Line Total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              Text(
                formatCurrency(lineCalc.lineTotal, currencySymbol),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }
    }

    // Payment & Calculation Card
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
            Text("Payment & Totals", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
              Checkbox(checked = roundOffEnabled, onCheckedChange = onRoundOffChange)
              Text("Round Off", style = MaterialTheme.typography.bodySmall)
            }
          }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PaymentMode.entries.take(4).forEach { mode ->
              FilterChip(
                selected = paymentMode == mode,
                onClick = { onPaymentModeChange(mode) },
                label = { Text(mode.label) },
              )
            }
          }

          if (accounts.isNotEmpty()) {
            var accExpanded by remember { mutableStateOf(false) }
            val selectedAcc = accounts.firstOrNull { it.id == selectedAccountId } ?: accounts.first()
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
                      onSelectAccount(acc.id)
                      accExpanded = false
                    },
                  )
                }
              }
            }
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = fullyPaidChecked, onCheckedChange = onFullyPaidChange)
            Text("Received / Paid in full (${formatCurrency(totalAmount, currencySymbol)})", style = MaterialTheme.typography.bodyMedium)
          }

          if (!fullyPaidChecked) {
            OutlinedTextField(
              value = paidText,
              onValueChange = onPaidTextChange,
              label = { Text("Amount Received / Paid ($currencySymbol)") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
            )
          }

          OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
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
                formatCurrency(totalAmount, currencySymbol),
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
