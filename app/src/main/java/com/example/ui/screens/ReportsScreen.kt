package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BusinessProfile
import com.example.data.model.BusinessTransaction
import com.example.data.model.InventoryItem
import com.example.data.model.ItemType
import com.example.data.model.Party
import com.example.data.model.TxStatus
import com.example.data.model.TxType
import com.example.data.model.isLowStock
import com.example.data.model.txTypeEnum
import com.example.ui.components.KpiMetricCard
import com.example.ui.theme.LocalDukaanColors
import com.example.ui.util.formatCurrency
import com.example.ui.util.formatDate
import com.example.ui.util.formatQty
import com.example.ui.util.shareText
import com.example.ui.viewmodel.DashboardMetrics

private enum class ReportKind(val group: String, val label: String) {
  SALE("Transaction Reports", "Sale Report"),
  PURCHASE("Transaction Reports", "Purchase Report"),
  DAY_BOOK("Transaction Reports", "Day Book / All Txns"),
  PROFIT_LOSS("Transaction Reports", "Profit & Loss"),
  CASH_FLOW("Transaction Reports", "Cash Flow"),
  ALL_PARTIES("Party Reports", "All Parties Balance"),
  GSTR_1("GST Reports", "GSTR-1 (Outward)"),
  GSTR_2("GST Reports", "GSTR-2 (Inward)"),
  GSTR_3B("GST Reports", "GSTR-3B Summary"),
  HSN_SUMMARY("GST Reports", "HSN Summary"),
  STOCK_SUMMARY("Item / Stock Reports", "Stock Summary"),
  LOW_STOCK("Item / Stock Reports", "Low Stock Alert"),
}

@Composable
fun ReportsScreen(
  profile: BusinessProfile?,
  metrics: DashboardMetrics,
  parties: List<Party>,
  items: List<InventoryItem>,
  transactions: List<BusinessTransaction>,
) {
  val sym = profile?.currencySymbol ?: "₹"
  val semantic = LocalDukaanColors.current
  val context = LocalContext.current

  var selectedReport by remember { mutableStateOf(ReportKind.PROFIT_LOSS) }
  val activeTxs = remember(transactions) { transactions.filter { it.status != TxStatus.CANCELLED.name } }

  fun exportReportText(): String = buildString {
    appendLine("${profile?.businessName ?: "Dukaan Stock"} - ${selectedReport.label}")
    appendLine("Generated: ${formatDate(System.currentTimeMillis())}")
    appendLine("====================================")
    when (selectedReport) {
      ReportKind.PROFIT_LOSS -> {
        appendLine("Total Sales: ${formatCurrency(metrics.totalSales, sym)}")
        appendLine("Total Purchases: ${formatCurrency(metrics.totalPurchases, sym)}")
        appendLine("Total Expenses: ${formatCurrency(metrics.totalExpenses, sym)}")
        appendLine("Closing Stock Value: ${formatCurrency(metrics.stockValue, sym)}")
        appendLine("Estimated Net Profit: ${formatCurrency(metrics.netProfit, sym)}")
      }
      ReportKind.GSTR_1, ReportKind.GSTR_2, ReportKind.GSTR_3B -> {
        appendLine("Outward Sales Taxable + GST: ${formatCurrency(metrics.totalSales, sym)} (Output GST: ${formatCurrency(metrics.outputGst, sym)})")
        appendLine("Inward Purchase Taxable + GST: ${formatCurrency(metrics.totalPurchases, sym)} (Input GST: ${formatCurrency(metrics.inputGst, sym)})")
        appendLine("Net GST Payable: ${formatCurrency((metrics.outputGst - metrics.inputGst).coerceAtLeast(0.0), sym)}")
      }
      ReportKind.STOCK_SUMMARY, ReportKind.LOW_STOCK -> {
        val list = if (selectedReport == ReportKind.LOW_STOCK) metrics.lowStockItems else items
        list.forEach {
          appendLine("${it.name} | Stock: ${formatQty(it.currentStock)} ${it.unit} | Sale: ${formatCurrency(it.salePrice, sym)}")
        }
      }
      ReportKind.ALL_PARTIES -> {
        parties.forEach {
          appendLine("${it.name} | Balance: ${formatCurrency(it.currentBalance, sym)}")
        }
      }
      else -> {
        activeTxs.forEach {
          appendLine("${formatDate(it.dateMillis)} | ${it.txTypeEnum().shortLabel} | ${it.refNumber} | ${it.partyName} | ${formatCurrency(it.totalAmount, sym)}")
        }
      }
    }
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val isWide = maxWidth >= 840.dp

    if (isWide) {
      Row(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        // Left Sidebar of Reports (matching reference image__11)
        Surface(
          modifier = Modifier.width(260.dp).fillMaxHeight(),
          shape = RoundedCornerShape(18.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            val grouped = ReportKind.entries.groupBy { it.group }
            grouped.forEach { (groupName, kinds) ->
              item {
                Text(
                  text = groupName,
                  style = MaterialTheme.typography.labelLarge,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                )
              }
              items(kinds) { kind ->
                val active = selectedReport == kind
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color =
                    if (active) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface,
                  modifier =
                    Modifier
                      .fillMaxWidth()
                      .clickable { selectedReport = kind },
                ) {
                  Text(
                    text = kind.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    color =
                      if (active) MaterialTheme.colorScheme.onPrimaryContainer
                      else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                  )
                }
              }
            }
          }
        }

        // Right Live Report Content
        ReportDetailArea(
          selectedReport = selectedReport,
          metrics = metrics,
          parties = parties,
          items = items,
          activeTxs = activeTxs,
          currencySymbol = sym,
          onShare = {
            shareText(context, "${selectedReport.label} - Dukaan Stock", exportReportText())
          },
          modifier = Modifier.weight(1f).fillMaxHeight(),
        )
      }
    } else {
      Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          ReportKind.entries.forEach { kind ->
            FilterChip(
              selected = selectedReport == kind,
              onClick = { selectedReport = kind },
              label = { Text(kind.label) },
            )
          }
        }
        Spacer(modifier = Modifier.height(12.dp))
        ReportDetailArea(
          selectedReport = selectedReport,
          metrics = metrics,
          parties = parties,
          items = items,
          activeTxs = activeTxs,
          currencySymbol = sym,
          onShare = {
            shareText(context, "${selectedReport.label} - Dukaan Stock", exportReportText())
          },
          modifier = Modifier.weight(1f).fillMaxWidth(),
        )
      }
    }
  }
}

@Composable
private fun ReportDetailArea(
  selectedReport: ReportKind,
  metrics: DashboardMetrics,
  parties: List<Party>,
  items: List<InventoryItem>,
  activeTxs: List<BusinessTransaction>,
  currencySymbol: String,
  onShare: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val semantic = LocalDukaanColors.current

  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text(selectedReport.group, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(selectedReport.label, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
          }
          Button(onClick = onShare, shape = RoundedCornerShape(12.dp)) {
            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Share Report")
          }
        }
      }

      when (selectedReport) {
        ReportKind.PROFIT_LOSS -> {
          item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              KpiMetricCard(
                title = "Total Sales",
                value = formatCurrency(metrics.totalSales, currencySymbol),
                subtitle = "Excl. GST: ${formatCurrency(metrics.totalSales - metrics.outputGst, currencySymbol)}",
                icon = Icons.Filled.Assessment,
                accentColor = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.weight(1f),
              )
              KpiMetricCard(
                title = "Net Profit",
                value = formatCurrency(metrics.netProfit, currencySymbol),
                subtitle = "After COGS & Expenses",
                icon = Icons.Filled.Assessment,
                accentColor = if (metrics.netProfit >= 0) semantic.positive else semantic.negative,
                containerColor = if (metrics.netProfit >= 0) semantic.positiveContainer else semantic.negativeContainer,
                modifier = Modifier.weight(1f),
              )
            }
          }
          item {
            ReportSummaryCard(
              rows = listOf(
                "Sale Revenue (Gross)" to formatCurrency(metrics.totalSales, currencySymbol),
                "Output GST Collected" to formatCurrency(metrics.outputGst, currencySymbol),
                "Total Purchases" to formatCurrency(metrics.totalPurchases, currencySymbol),
                "Business Expenses" to formatCurrency(metrics.totalExpenses, currencySymbol),
                "Closing Inventory Value" to formatCurrency(metrics.stockValue, currencySymbol),
                "Estimated Net Profit" to formatCurrency(metrics.netProfit, currencySymbol),
              )
            )
          }
        }

        ReportKind.CASH_FLOW -> {
          val cashIn =
            activeTxs
              .filter { it.txType == TxType.SALE.name || it.txType == TxType.PAYMENT_IN.name }
              .sumOf { if (it.txType == TxType.PAYMENT_IN.name) it.totalAmount else it.paidAmount }
          val cashOut =
            activeTxs
              .filter { it.txType == TxType.PURCHASE.name || it.txType == TxType.PAYMENT_OUT.name || it.txType == TxType.EXPENSE.name }
              .sumOf { if (it.txType == TxType.PURCHASE.name) it.paidAmount else it.totalAmount }
          item {
            ReportSummaryCard(
              rows = listOf(
                "Total Cash / Bank Inflow" to formatCurrency(cashIn, currencySymbol),
                "Total Cash / Bank Outflow" to formatCurrency(cashOut, currencySymbol),
                "Net Period Cash Flow" to formatCurrency(cashIn - cashOut, currencySymbol),
                "Current Cash & Bank Balance" to formatCurrency(metrics.cashAndBankTotal, currencySymbol),
              )
            )
          }
        }

        ReportKind.GSTR_1, ReportKind.GSTR_2, ReportKind.GSTR_3B -> {
          val sales = activeTxs.filter { it.txType == TxType.SALE.name }
          val purchases = activeTxs.filter { it.txType == TxType.PURCHASE.name }
          item {
            ReportSummaryCard(
              rows = listOf(
                "Outward Supplies (GSTR-1)" to formatCurrency(sales.sumOf { it.totalAmount }, currencySymbol),
                "Output CGST" to formatCurrency(metrics.outputGst / 2.0, currencySymbol),
                "Output SGST" to formatCurrency(metrics.outputGst / 2.0, currencySymbol),
                "Inward Supplies (GSTR-2)" to formatCurrency(purchases.sumOf { it.totalAmount }, currencySymbol),
                "Input Tax Credit (ITC)" to formatCurrency(metrics.inputGst, currencySymbol),
                "Net GST Payable (GSTR-3B)" to formatCurrency((metrics.outputGst - metrics.inputGst).coerceAtLeast(0.0), currencySymbol),
              )
            )
          }
        }

        ReportKind.HSN_SUMMARY -> {
          val hsnGroups =
            activeTxs
              .filter { it.txType == TxType.SALE.name }
              .flatMap { it.lineItems }
              .groupBy { it.hsnSac.ifBlank { "No HSN" } }
          if (hsnGroups.isEmpty()) {
            item { Text("No outward HSN line items recorded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
          } else {
            items(hsnGroups.entries.toList()) { (hsn, lines) ->
              val qty = lines.sumOf { it.quantity }
              val tax = lines.sumOf { it.taxAmount }
              val total = lines.sumOf { it.lineTotal }
              ReportLineRow(
                title = "HSN: $hsn",
                subtitle = "Total Qty: ${formatQty(qty)} • GST: ${formatCurrency(tax, currencySymbol)}",
                trailing = formatCurrency(total, currencySymbol),
              )
            }
          }
        }

        ReportKind.ALL_PARTIES -> {
          items(parties, key = { it.id }) { p ->
            ReportLineRow(
              title = p.name,
              subtitle = p.phone.ifBlank { p.partyType },
              trailing =
                when {
                  p.currentBalance > 0.01 -> "+${formatCurrency(p.currentBalance, currencySymbol)} Recv"
                  p.currentBalance < -0.01 -> "-${formatCurrency(-p.currentBalance, currencySymbol)} Pay"
                  else -> "Settled"
                },
            )
          }
        }

        ReportKind.STOCK_SUMMARY, ReportKind.LOW_STOCK -> {
          val list =
            if (selectedReport == ReportKind.LOW_STOCK) metrics.lowStockItems
            else items.filter { it.itemType == ItemType.PRODUCT.name }
          if (list.isEmpty()) {
            item { Text("No items to display in this stock report.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
          } else {
            items(list, key = { it.id }) { item ->
              val stockVal = item.currentStock * (if (item.purchasePrice > 0) item.purchasePrice else item.salePrice)
              ReportLineRow(
                title = item.name,
                subtitle = "Qty: ${formatQty(item.currentStock)} ${item.unit} • Min: ${formatQty(item.minStockLevel)}",
                trailing = formatCurrency(stockVal, currencySymbol),
              )
            }
          }
        }

        ReportKind.SALE, ReportKind.PURCHASE, ReportKind.DAY_BOOK -> {
          val txs =
            when (selectedReport) {
              ReportKind.SALE -> activeTxs.filter { it.txType == TxType.SALE.name }
              ReportKind.PURCHASE -> activeTxs.filter { it.txType == TxType.PURCHASE.name }
              else -> activeTxs
            }
          if (txs.isEmpty()) {
            item { Text("No transactions found for this report.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
          } else {
            items(txs, key = { it.id }) { tx ->
              ReportLineRow(
                title = "${tx.refNumber} • ${tx.partyName.ifBlank { tx.category }}",
                subtitle = "${tx.txTypeEnum().shortLabel} • ${formatDate(tx.dateMillis)} • GST: ${formatCurrency(tx.taxAmount, currencySymbol)}",
                trailing = formatCurrency(tx.totalAmount, currencySymbol),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ReportSummaryCard(rows: List<Pair<String, String>>) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
      rows.forEachIndexed { idx, (label, value) ->
        Row(
          modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(label, style = MaterialTheme.typography.bodyMedium)
          Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        if (idx < rows.lastIndex) HorizontalDivider()
      }
    }
  }
}

@Composable
private fun ReportLineRow(title: String, subtitle: String, trailing: String) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Spacer(modifier = Modifier.width(8.dp))
      Text(trailing, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
  }
}
