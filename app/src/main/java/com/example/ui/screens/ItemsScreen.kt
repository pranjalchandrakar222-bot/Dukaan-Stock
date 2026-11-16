package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.BusinessProfile
import com.example.data.model.BusinessTransaction
import com.example.data.model.InventoryItem
import com.example.data.model.ItemType
import com.example.data.model.isLowStock
import com.example.data.model.itemTypeEnum
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusBadgeChip
import com.example.ui.theme.LocalDukaanColors
import com.example.ui.util.formatCurrency
import com.example.ui.util.formatQty

private enum class ItemFilter(val label: String) {
  ALL("All Items"),
  PRODUCTS("Products"),
  SERVICES("Services"),
  LOW_STOCK("Low Stock"),
}

@Composable
fun ItemsScreen(
  items: List<InventoryItem>,
  transactions: List<BusinessTransaction>,
  profile: BusinessProfile?,
  openAddDialogInitially: Boolean = false,
  onConsumeInitialDialog: () -> Unit = {},
  onSaveItem: (InventoryItem, () -> Unit) -> Unit,
  onAdjustStock: (String, Double, String) -> Unit,
  onArchiveOrDeleteItem: (InventoryItem) -> Unit,
) {
  val sym = profile?.currencySymbol ?: "₹"
  val semantic = LocalDukaanColors.current

  var searchQuery by remember { mutableStateOf("") }
  var filter by remember { mutableStateOf(ItemFilter.ALL) }
  var showArchived by remember { mutableStateOf(false) }
  var selectedItemId by remember { mutableStateOf<String?>(null) }
  var editingItem by remember { mutableStateOf<InventoryItem?>(null) }
  var showEditorDialog by remember { mutableStateOf(openAddDialogInitially) }
  var stockAdjustItem by remember { mutableStateOf<InventoryItem?>(null) }
  var confirmDeleteItem by remember { mutableStateOf<InventoryItem?>(null) }

  if (openAddDialogInitially) {
    onConsumeInitialDialog()
  }

  val filteredItems =
    remember(items, searchQuery, filter, showArchived) {
      items.filter { item ->
        val activeOk = showArchived || item.isActive
        val matchesQuery =
          searchQuery.isBlank() ||
            item.name.contains(searchQuery, ignoreCase = true) ||
            item.itemCode.contains(searchQuery, ignoreCase = true) ||
            item.category.contains(searchQuery, ignoreCase = true) ||
            item.hsnSac.contains(searchQuery, ignoreCase = true)
        val matchesFilter =
          when (filter) {
            ItemFilter.ALL -> true
            ItemFilter.PRODUCTS -> item.itemType == ItemType.PRODUCT.name
            ItemFilter.SERVICES -> item.itemType == ItemType.SERVICE.name
            ItemFilter.LOW_STOCK -> item.isLowStock()
          }
        activeOk && matchesQuery && matchesFilter
      }
    }

  val selectedItem = remember(items, selectedItemId) { items.firstOrNull { it.id == selectedItemId } }
  val totalStockValue =
    items
      .filter { it.isActive && it.itemType == ItemType.PRODUCT.name && it.currentStock > 0 }
      .sumOf { it.currentStock * (if (it.purchasePrice > 0) it.purchasePrice else it.salePrice) }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val isWide = maxWidth >= 840.dp

    if (!isWide && selectedItem != null) {
      BackHandler { selectedItemId = null }
      ItemDetailPane(
        item = selectedItem,
        currencySymbol = sym,
        showBackButton = true,
        onBack = { selectedItemId = null },
        onEdit = {
          editingItem = selectedItem
          showEditorDialog = true
        },
        onAdjustStock = { stockAdjustItem = selectedItem },
        onDelete = { confirmDeleteItem = selectedItem },
        modifier = Modifier.fillMaxSize().padding(16.dp),
      )
    } else {
      Row(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text("Items & Stock", style = MaterialTheme.typography.headlineSmall)
              Text(
                "${items.count { it.isActive }} active items • Stock Value: ${formatCurrency(totalStockValue, sym)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Button(
              onClick = {
                editingItem = null
                showEditorDialog = true
              },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.height(48.dp).testTag("add_item_button"),
            ) {
              Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Add Item")
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search items by name, SKU, category, or HSN...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().testTag("item_search_input"),
          )

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            ItemFilter.entries.forEach { f ->
              FilterChip(
                selected = filter == f,
                onClick = { filter = f },
                label = { Text(f.label) },
              )
            }
            FilterChip(
              selected = showArchived,
              onClick = { showArchived = !showArchived },
              label = { Text("Show Archived") },
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          if (filteredItems.isEmpty()) {
            EmptyStateCard(
              icon = Icons.Filled.Inventory2,
              title = if (items.isEmpty()) "No items in inventory yet" else "No matching items",
              subtitle = "Add products or services with GST tax rates, wholesale/purchase prices, and low-stock thresholds.",
              actionLabel = "Add First Item",
              onAction = {
                editingItem = null
                showEditorDialog = true
              },
            )
          } else {
            LazyColumn(
              modifier = Modifier.weight(1f),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              items(filteredItems, key = { it.id }) { item ->
                val isSelected = isWide && item.id == (selectedItem?.id ?: filteredItems.firstOrNull()?.id)
                val lowStock = item.isLowStock()

                Surface(
                  modifier =
                    Modifier
                      .fillMaxWidth()
                      .clickable { selectedItemId = item.id }
                      .testTag("item_card_${item.id}"),
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
                    Column(modifier = Modifier.weight(1f)) {
                      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                          text = item.name,
                          style = MaterialTheme.typography.titleSmall,
                          fontWeight = FontWeight.SemiBold,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                          modifier = Modifier.weight(1f, fill = false),
                        )
                        if (!item.isActive) {
                          StatusBadgeChip("Archived", MaterialTheme.colorScheme.outline, MaterialTheme.colorScheme.surfaceVariant)
                        } else if (lowStock) {
                          StatusBadgeChip(
                            label = "LOW STOCK",
                            color = semantic.negative,
                            containerColor = semantic.negativeContainer,
                          )
                        }
                      }
                      Spacer(modifier = Modifier.height(2.dp))
                      Text(
                        text =
                          listOfNotNull(
                            item.hsnSac.takeIf { it.isNotBlank() }?.let { "HSN: $it" } ?: item.itemTypeEnum().label,
                            item.category.takeIf { it.isNotBlank() },
                            "GST ${formatQty(item.taxRatePercent)}%",
                            item.commissionPercent.takeIf { it > 0 }?.let { "Comm ${formatQty(it)}%" },
                          ).joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                      Text(
                        text = formatCurrency(item.salePrice, sym),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                      )
                      if (item.itemType == ItemType.PRODUCT.name) {
                        Text(
                          text = "Stock: ${formatQty(item.currentStock)} ${item.unit}",
                          style = MaterialTheme.typography.bodySmall,
                          color = if (lowStock) semantic.negative else semantic.positive,
                          fontWeight = FontWeight.Medium,
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }

        if (isWide) {
          val activeItem = selectedItem ?: filteredItems.firstOrNull()
          Box(modifier = Modifier.weight(1.05f).fillMaxHeight()) {
            if (activeItem != null) {
              ItemDetailPane(
                item = activeItem,
                currencySymbol = sym,
                showBackButton = false,
                onBack = {},
                onEdit = {
                  editingItem = activeItem
                  showEditorDialog = true
                },
                onAdjustStock = { stockAdjustItem = activeItem },
                onDelete = { confirmDeleteItem = activeItem },
                modifier = Modifier.fillMaxSize(),
              )
            } else {
              EmptyStateCard(
                icon = Icons.Filled.Inventory2,
                title = "Select an Item",
                subtitle = "Choose a product or service on the left to view pricing, GST tax rate, and stock adjustments.",
              )
            }
          }
        }
      }
    }
  }

  if (showEditorDialog) {
    ItemEditorDialog(
      initial = editingItem,
      currencySymbol = sym,
      onDismiss = { showEditorDialog = false },
      onSave = { item ->
        onSaveItem(item) { showEditorDialog = false }
      },
    )
  }

  val adjustTarget = stockAdjustItem
  if (adjustTarget != null) {
    StockAdjustDialog(
      item = adjustTarget,
      onDismiss = { stockAdjustItem = null },
      onApply = { delta, reason ->
        onAdjustStock(adjustTarget.id, delta, reason)
        stockAdjustItem = null
      },
    )
  }

  val delItem = confirmDeleteItem
  if (delItem != null) {
    val hasTxs = transactions.any { tx -> tx.lineItems.any { it.itemId == delItem.id } }
    AlertDialog(
      onDismissRequest = { confirmDeleteItem = null },
      title = { Text(if (hasTxs) "Archive ${delItem.name}?" else "Delete ${delItem.name}?") },
      text = {
        Text(
          if (hasTxs) {
            "${delItem.name} is referenced in existing invoices. Archiving hides it from billing while keeping GST & profit reports accurate."
          } else {
            "Permanently delete ${delItem.name}? This cannot be undone."
          }
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onArchiveOrDeleteItem(delItem)
            if (selectedItemId == delItem.id) selectedItemId = null
            confirmDeleteItem = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) {
          Text(if (hasTxs) "Archive" else "Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { confirmDeleteItem = null }) { Text("Cancel") }
      },
    )
  }
}

@Composable
private fun ItemDetailPane(
  item: InventoryItem,
  currencySymbol: String,
  showBackButton: Boolean,
  onBack: () -> Unit,
  onEdit: () -> Unit,
  onAdjustStock: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val semantic = LocalDukaanColors.current
  val lowStock = item.isLowStock()

  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(
      modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
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
            Text(item.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
              listOfNotNull(
                item.itemTypeEnum().label,
                item.category.takeIf { it.isNotBlank() },
                item.itemCode.takeIf { it.isNotBlank() }?.let { "SKU: $it" },
              ).joinToString(" • "),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        Row {
          IconButton(onClick = onEdit) {
            Icon(Icons.Filled.Edit, contentDescription = "Edit Item")
          }
          IconButton(onClick = onDelete) {
            Icon(Icons.Filled.DeleteOutline, contentDescription = "Archive or Delete Item", tint = MaterialTheme.colorScheme.error)
          }
        }
      }

      if (item.itemType == ItemType.PRODUCT.name) {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = if (lowStock) semantic.warningCardBg else semantic.positiveCardBg,
          border = BorderStroke(1.dp, if (lowStock) semantic.warningCardBorder else semantic.positiveCardBorder),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text(
                if (lowStock) "Low Stock Alert" else "Available Stock",
                style = MaterialTheme.typography.labelMedium,
                color = if (lowStock) semantic.warning else semantic.positive,
                fontWeight = FontWeight.SemiBold,
              )
              Text(
                "${formatQty(item.currentStock)} ${item.unit}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (lowStock) semantic.warning else semantic.positive,
              )
              Text(
                "Min Alert: ${formatQty(item.minStockLevel)} ${item.unit}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Button(
              onClick = onAdjustStock,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.height(44.dp).testTag("adjust_stock_button"),
            ) {
              Text("Adjust Stock")
            }
          }
        }
      }

      HorizontalDivider()

      Text("Pricing & Tax", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
      val marginPct = if (item.purchasePrice > 0.01 && item.salePrice > 0.0) {
        ((item.salePrice - item.purchasePrice) / item.purchasePrice) * 100.0
      } else null

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricBox(
          label = if (item.salePriceWithTax) "Sale Price (Incl. Tax)" else "Sale Price",
          value = formatCurrency(item.salePrice, currencySymbol),
          modifier = Modifier.weight(1f),
        )
        MetricBox(
          label = "Purchase Price",
          value = formatCurrency(item.purchasePrice, currencySymbol),
          subtitle = marginPct?.let {
            val sign = if (it >= 0) "+" else ""
            "Margin $sign${formatQty(it)}%"
          },
          subtitleColor = if ((marginPct ?: 0.0) >= 0) semantic.positive else semantic.negative,
          modifier = Modifier.weight(1f),
        )
      }
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricBox("Wholesale Price", formatCurrency(item.wholesalePrice, currencySymbol), modifier = Modifier.weight(1f))
        Surface(
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(12.dp),
          color = semantic.infoCardBg,
          border = BorderStroke(1.dp, semantic.infoCardBorder),
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text("GST Rate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              "${formatQty(item.taxRatePercent)}%",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
            if (item.hsnSac.isNotBlank()) {
              Text("HSN: ${item.hsnSac}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      }

      Surface(
        shape = RoundedCornerShape(14.dp),
        color = semantic.infoCardBg,
        border = BorderStroke(1.dp, semantic.infoCardBorder),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text(
            "Agent Commission (Internal)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = if (item.commissionPercent > 0) {
              "${formatQty(item.commissionPercent)}% default for this item"
            } else {
              "Uses assigned Agent / Mistri's default commission rate"
            },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
          )
        }
      }

      if (item.location.isNotBlank()) {
        MetricBox("Location / Godown", item.location, Modifier.fillMaxWidth())
      }
    }
  }
}

@Composable
private fun MetricBox(
  label: String,
  value: String,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  subtitleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Spacer(modifier = Modifier.height(2.dp))
      Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
      if (!subtitle.isNullOrBlank()) {
        Text(subtitle, style = MaterialTheme.typography.labelSmall, color = subtitleColor, fontWeight = FontWeight.Medium)
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemEditorDialog(
  initial: InventoryItem?,
  currencySymbol: String,
  onDismiss: () -> Unit,
  onSave: (InventoryItem) -> Unit,
) {
  var itemType by remember { mutableStateOf(initial?.itemTypeEnum() ?: ItemType.PRODUCT) }
  var name by remember { mutableStateOf(initial?.name ?: "") }
  var hsnSac by remember { mutableStateOf(initial?.hsnSac ?: "") }
  var itemCode by remember { mutableStateOf(initial?.itemCode ?: "") }
  var category by remember { mutableStateOf(initial?.category ?: "") }
  var unit by remember { mutableStateOf(initial?.unit ?: "PCS") }
  var unitExpanded by remember { mutableStateOf(false) }

  var editorTab by remember { mutableStateOf(0) }

  var salePriceText by remember { mutableStateOf(initial?.salePrice?.takeIf { it > 0 }?.toString() ?: "") }
  var saleWithTax by remember { mutableStateOf(initial?.salePriceWithTax ?: false) }
  var wholesaleText by remember { mutableStateOf(initial?.wholesalePrice?.takeIf { it > 0 }?.toString() ?: "") }
  var purchaseText by remember { mutableStateOf(initial?.purchasePrice?.takeIf { it > 0 }?.toString() ?: "") }
  var discountText by remember { mutableStateOf(initial?.discountPercent?.takeIf { it > 0 }?.toString() ?: "0") }
  var commissionText by remember { mutableStateOf(initial?.commissionPercent?.takeIf { it > 0 }?.toString() ?: "") }
  var taxRate by remember { mutableStateOf(initial?.taxRatePercent ?: 0.0) }
  var taxExpanded by remember { mutableStateOf(false) }

  var openingStockText by remember { mutableStateOf(initial?.openingStock?.toString() ?: "0") }
  var minStockText by remember { mutableStateOf(initial?.minStockLevel?.toString() ?: "5") }
  var location by remember { mutableStateOf(initial?.location ?: "") }

  val units = listOf("PCS", "KG", "GM", "BOX", "BAG", "LTR", "MTR", "DOZ", "SET", "SRV")
  val taxSlabs = listOf(0.0, 5.0, 12.0, 18.0, 28.0)

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (initial == null) "Add Item" else "Edit Item") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
          SegmentedButton(
            selected = itemType == ItemType.PRODUCT,
            onClick = { itemType = ItemType.PRODUCT },
            shape = SegmentedButtonDefaults.itemShape(0, 2),
          ) { Text("Product") }
          SegmentedButton(
            selected = itemType == ItemType.SERVICE,
            onClick = { itemType = ItemType.SERVICE },
            shape = SegmentedButtonDefaults.itemShape(1, 2),
          ) { Text("Service") }
        }

        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Item Name *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("item_name_input"),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = hsnSac,
            onValueChange = { hsnSac = it },
            label = { Text(if (itemType == ItemType.PRODUCT) "HSN" else "SAC") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          ExposedDropdownMenuBox(
            expanded = unitExpanded,
            onExpandedChange = { unitExpanded = it },
            modifier = Modifier.weight(1f),
          ) {
            OutlinedTextField(
              value = unit,
              onValueChange = {},
              readOnly = true,
              label = { Text("Unit") },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
              modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = unitExpanded, onDismissRequest = { unitExpanded = false }) {
              units.forEach { u ->
                DropdownMenuItem(
                  text = { Text(u) },
                  onClick = {
                    unit = u
                    unitExpanded = false
                  },
                )
              }
            }
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
          OutlinedTextField(
            value = itemCode,
            onValueChange = { itemCode = it },
            label = { Text("Item Code / SKU") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          FilledTonalButton(
            onClick = { itemCode = "DS-${(1000..9999).random()}" },
            shape = RoundedCornerShape(10.dp),
          ) {
            Text("Assign Code")
          }
        }

        OutlinedTextField(
          value = category,
          onValueChange = { category = it },
          label = { Text("Category") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
          SegmentedButton(
            selected = editorTab == 0,
            onClick = { editorTab = 0 },
            shape = SegmentedButtonDefaults.itemShape(0, 2),
          ) { Text("Pricing") }
          SegmentedButton(
            selected = editorTab == 1,
            onClick = { editorTab = 1 },
            shape = SegmentedButtonDefaults.itemShape(1, 2),
          ) { Text("Stock") }
        }

        if (editorTab == 0) {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = salePriceText,
              onValueChange = { salePriceText = it },
              label = { Text("Sale Price ($currencySymbol)") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier.weight(1f).testTag("item_sale_price_input"),
            )
            OutlinedTextField(
              value = purchaseText,
              onValueChange = { purchaseText = it },
              label = { Text("Purchase ($currencySymbol)") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = wholesaleText,
              onValueChange = { wholesaleText = it },
              label = { Text("Wholesale ($currencySymbol)") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
              value = discountText,
              onValueChange = { discountText = it },
              label = { Text("Discount %") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExposedDropdownMenuBox(
              expanded = taxExpanded,
              onExpandedChange = { taxExpanded = it },
              modifier = Modifier.weight(1f),
            ) {
              OutlinedTextField(
                value = "GST ${formatQty(taxRate)}%",
                onValueChange = {},
                readOnly = true,
                label = { Text("Tax Rate") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = taxExpanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
              )
              ExposedDropdownMenu(expanded = taxExpanded, onDismissRequest = { taxExpanded = false }) {
                taxSlabs.forEach { slab ->
                  DropdownMenuItem(
                    text = { Text("GST ${formatQty(slab)}%") },
                    onClick = {
                      taxRate = slab
                      taxExpanded = false
                    },
                  )
                }
              }
            }
            OutlinedTextField(
              value = commissionText,
              onValueChange = { commissionText = it },
              label = { Text("Agent Comm %") },
              placeholder = { Text("Optional") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Sale price includes tax", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = saleWithTax, onCheckedChange = { saleWithTax = it })
          }
        } else {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = openingStockText,
              onValueChange = { openingStockText = it },
              label = { Text("Opening Stock") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              enabled = initial == null,
              modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
              value = minStockText,
              onValueChange = { minStockText = it },
              label = { Text("Min Stock Alert") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
          }
          OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Item Location / Godown") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val openStock = openingStockText.toDoubleOrNull() ?: 0.0
          val updated =
            (initial ?: InventoryItem()).copy(
              name = name.trim(),
              itemType = itemType.name,
              hsnSac = hsnSac.trim(),
              itemCode = itemCode.trim(),
              category = category.trim(),
              unit = unit,
              salePrice = (salePriceText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0),
              salePriceWithTax = saleWithTax,
              wholesalePrice = (wholesaleText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0),
              purchasePrice = (purchaseText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0),
              discountPercent = (discountText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0),
              taxRatePercent = taxRate,
              commissionPercent = (commissionText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0),
              openingStock = if (initial == null) openStock else initial.openingStock,
              currentStock = if (initial == null) openStock else initial.currentStock,
              minStockLevel = (minStockText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0),
              location = location.trim(),
              isActive = true,
            )
          onSave(updated)
        },
        enabled = name.trim().isNotEmpty(),
        modifier = Modifier.testTag("save_item_button"),
      ) {
        Text("Save")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

@Composable
private fun StockAdjustDialog(
  item: InventoryItem,
  onDismiss: () -> Unit,
  onApply: (Double, String) -> Unit,
) {
  var isAdd by remember { mutableStateOf(true) }
  var qtyText by remember { mutableStateOf("") }
  var reason by remember { mutableStateOf("Physical Stock Count") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Adjust Stock: ${item.name}") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          "Current Stock: ${formatQty(item.currentStock)} ${item.unit}",
          style = MaterialTheme.typography.bodyMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilterChip(
            selected = isAdd,
            onClick = { isAdd = true },
            leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
            label = { Text("Add Stock") },
          )
          FilterChip(
            selected = !isAdd,
            onClick = { isAdd = false },
            leadingIcon = { Icon(Icons.Filled.Remove, contentDescription = null, modifier = Modifier.size(16.dp)) },
            label = { Text("Reduce Stock") },
          )
        }
        OutlinedTextField(
          value = qtyText,
          onValueChange = { qtyText = it },
          label = { Text("Quantity (${item.unit})") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = reason,
          onValueChange = { reason = it },
          label = { Text("Adjustment Reason") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val qty = qtyText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
          if (qty > 0) onApply(if (isAdd) qty else -qty, reason.ifBlank { "Manual Stock Adjustment" })
        },
        enabled = (qtyText.toDoubleOrNull() ?: 0.0) > 0,
      ) {
        Text("Apply")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}
