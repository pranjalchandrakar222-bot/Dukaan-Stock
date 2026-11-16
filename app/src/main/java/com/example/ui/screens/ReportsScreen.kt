package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
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
import com.example.data.model.Agent
import com.example.data.model.BusinessProfile
import com.example.data.model.BusinessTransaction
import com.example.data.model.InventoryItem
import com.example.data.model.ItemType
import com.example.data.model.Party
import com.example.data.model.TxStatus
import com.example.data.model.TxType
import com.example.data.model.agentTypeEnum
import com.example.data.model.txTypeEnum
import com.example.ui.components.KpiMetricCard
import com.example.ui.theme.LocalDukaanColors
import com.example.ui.util.MoneyUtils
import com.example.ui.util.PdfInvoiceHelper
import com.example.ui.util.formatCurrency
import com.example.ui.util.formatDate
import com.example.ui.util.formatQty
import com.example.ui.util.shareText
import com.example.ui.viewmodel.DashboardMetrics
import com.example.ui.viewmodel.DateRangeFilter
import java.util.Calendar

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
  AGENT_COMMISSION("Agent & Mistri Reports", "Commission Summary"),
  COMMISSION_LEDGER("Agent & Mistri Reports", "Commission Transactions"),
}

private fun filterByRange(
  txs: List<BusinessTransaction>,
  range: DateRangeFilter,
): List<BusinessTransaction> {
  if (range == DateRangeFilter.ALL_TIME) return txs
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
    DateRangeFilter.THIS_MONTH -> cal.set(Calendar.DAY_OF_MONTH, 1)
    DateRangeFilter.THIS_FY -> {
      val month = cal.get(Calendar.MONTH)
      val year = cal.get(Calendar.YEAR)
      val fyStartYear = if (month >= Calendar.APRIL) year else year - 1
      cal.set(fyStartYear, Calendar.APRIL, 1)
    }
    DateRangeFilter.ALL_TIME -> {}
  }
  val start = cal.timeInMillis
  return txs.filter { it.dateMillis >= start }
}

@Composable
fun ReportsScreen(
  profile: BusinessProfile?,
  metrics: DashboardMetrics,
  parties: List<Party>,
  items: List<InventoryItem>,
  agents: List<Agent>,
  transactions: List<BusinessTransaction>,
  selectedDateRange: DateRangeFilter,
  onSelectDateRange: (DateRangeFilter) -> Unit,
) {
  val sym = profile?.currencySymbol ?: "₹"
  val context = LocalContext.current

  var selectedReport by remember { mutableStateOf(ReportKind.PROFIT_LOSS) }
  val activeTxs = remember(transactions, selectedDateRange) {
    filterByRange(transactions.filter { it.status != TxStatus.CANCELLED.name }, selectedDateRange)
  }

  fun exportReportText(): String = buildString {
    appendLine("${profile?.businessName ?: "Dukaan Stock"} - ${selectedReport.label} (${selectedDateRange.label})")
    appendLine("Generated: ${formatDate(System.currentTimeMillis())}")
    appendLine("====================================")
    when (selectedReport) {
      ReportKind.PROFIT_LOSS -> {
        appendLine("Gross Sales: ${formatCurrency(metrics.totalSales, sym)}")
        appendLine("Less Credit Notes: -${formatCurrency(metrics.creditNotesTotal, sym)}")
        appendLine("Output GST: ${formatCurrency(metrics.outputGst, sym)}")
        appendLine("Less Cost of Goods Sold (COGS): -${formatCurrency(metrics.cogs, sym)}")
        appendLine("Less Business Expenses: -${formatCurrency(metrics.totalExpenses, sym)}")
        appendLine("Less Accrued Commissions: -${formatCurrency(metrics.commissionAccruedPeriod, sym)}")
        appendLine("Closing Stock Value: ${formatCurrency(metrics.stockValue, sym)}")
        appendLine("Net Profit: ${formatCurrency(metrics.netProfit, sym)}")
      }
      ReportKind.GSTR_1, ReportKind.GSTR_2, ReportKind.GSTR_3B -> {
        appendLine("Output GST (CGST+SGST+IGST): ${formatCurrency(metrics.outputGst, sym)}")
        appendLine("Input Tax Credit (ITC): ${formatCurrency(metrics.inputGst, sym)}")
        val netGst = metrics.outputGst - metrics.inputGst
        if (netGst >= 0) {
          appendLine("Net GST Payable: ${formatCurrency(netGst, sym)}")
        } else {
          appendLine("ITC Credit Carry-Forward: ${formatCurrency(-netGst, sym)}")
        }
      }
      ReportKind.STOCK_SUMMARY, ReportKind.LOW_STOCK -> {
        val list = if (selectedReport == ReportKind.LOW_STOCK) metrics.lowStockItems else items.filter { it.isActive }
        list.forEach {
          appendLine("${it.name} | Stock: ${formatQty(it.currentStock)} ${it.unit} | Sale: ${formatCurrency(it.salePrice, sym)}")
        }
      }
      ReportKind.ALL_PARTIES -> {
        parties.filter { it.isActive }.forEach {
          appendLine("${it.name} | Balance: ${formatCurrency(it.currentBalance, sym)}")
        }
      }
      ReportKind.AGENT_COMMISSION -> {
        agents.filter { it.isActive }.forEach { a ->
          appendLine("${a.name} (${a.agentTypeEnum().label}) | Earned: ${formatCurrency(a.totalEarned, sym)} | Paid: ${formatCurrency(a.totalPaid, sym)} | Pending: ${formatCurrency(a.currentBalance, sym)}")
        }
      }
      else -> {
        activeTxs.forEach {
          appendLine("${formatDate(it.dateMillis)} | ${it.txTypeEnum().shortLabel} | ${it.refNumber} | ${it.partyName} | ${formatCurrency(it.totalAmount, sym)}")
        }
      }
    }
  }

  fun exportReportCsv(): String = buildString {
    fun q(v: String) = "\"${v.replace("\"", "\"\"")}\""
    when (selectedReport) {
      ReportKind.ALL_PARTIES -> {
        appendLine("Name,Type,Phone,GSTIN,State,Balance")
        parties.filter { it.isActive }.forEach { p ->
          appendLine("${q(p.name)},${q(p.partyType)},${q(p.phone)},${q(p.gstin)},${q(p.stateName)},${p.currentBalance}")
        }
      }
      ReportKind.STOCK_SUMMARY, ReportKind.LOW_STOCK -> {
        appendLine("Name,Type,HSN,Unit,CurrentStock,MinStock,PurchasePrice,SalePrice,Value")
        val list = if (selectedReport == ReportKind.LOW_STOCK) metrics.lowStockItems else items.filter { it.isActive }
        list.forEach { it ->
          val valStock = it.currentStock * (if (it.purchasePrice > 0) it.purchasePrice else it.salePrice)
          appendLine("${q(it.name)},${q(it.itemType)},${q(it.hsnSac)},${q(it.unit)},${it.currentStock},${it.minStockLevel},${it.purchasePrice},${it.salePrice},$valStock")
        }
      }
      ReportKind.AGENT_COMMISSION -> {
        appendLine("Name,Role,Phone,Area,TotalEarned,TotalPaid,PendingBalance")
        agents.filter { it.isActive }.forEach { a ->
          appendLine("${q(a.name)},${q(a.agentType)},${q(a.phone)},${q(a.cityArea)},${a.totalEarned},${a.totalPaid},${a.currentBalance}")
        }
      }
      else -> {
        appendLine("Date,Type,RefNumber,Party,Subtotal,CGST,SGST,IGST,Total,Paid,Balance,Status")
        activeTxs.forEach { tx ->
          appendLine(
            "${q(formatDate(tx.dateMillis))},${q(tx.txType)},${q(tx.refNumber)},${q(tx.partyName)}," +
              "${tx.subtotal},${tx.cgstAmount},${tx.sgstAmount},${tx.igstAmount}," +
              "${tx.totalAmount},${tx.paidAmount},${tx.balanceAmount},${q(tx.status)}"
          )
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

        ReportDetailArea(
          selectedReport = selectedReport,
          selectedDateRange = selectedDateRange,
          onSelectDateRange = onSelectDateRange,
          metrics = metrics,
          parties = parties,
          items = items,
          agents = agents,
          activeTxs = activeTxs,
          currencySymbol = sym,
          onShare = {
            shareText(context, "${selectedReport.label} - Dukaan Stock", exportReportText())
          },
          onExportCsv = {
            if (!PdfInvoiceHelper.shareCsvExport(context, "dukaan_${selectedReport.name.lowercase()}", exportReportCsv())) {
              Toast.makeText(context, "Unable to export CSV", Toast.LENGTH_SHORT).show()
            }
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
        Spacer(modifier = Modifier.height(10.dp))
        ReportDetailArea(
          selectedReport = selectedReport,
          selectedDateRange = selectedDateRange,
          onSelectDateRange = onSelectDateRange,
          metrics = metrics,
          parties = parties,
          items = items,
          agents = agents,
          activeTxs = activeTxs,
          currencySymbol = sym,
          onShare = {
            shareText(context, "${selectedReport.label} - Dukaan Stock", exportReportText())
          },
          onExportCsv = {
            if (!PdfInvoiceHelper.shareCsvExport(context, "dukaan_${selectedReport.name.lowercase()}", exportReportCsv())) {
              Toast.makeText(context, "Unable to export CSV", Toast.LENGTH_SHORT).show()
            }
          },
          modifier = Modifier.weight(1f).fillMaxWidth(),
        )
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReportDetailArea(
  selectedReport: ReportKind,
  selectedDateRange: DateRangeFilter,
  onSelectDateRange: (DateRangeFilter) -> Unit,
  metrics: DashboardMetrics,
  parties: List<Party>,
  items: List<InventoryItem>,
  agents: List<Agent>,
  activeTxs: List<BusinessTransaction>,
  currencySymbol: String,
  onShare: () -> Unit,
  onExportCsv: () -> Unit,
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
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(selectedReport.group, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
              Text(selectedReport.label, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              FilledTonalButton(onClick = onExportCsv, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("CSV")
              }
              Button(onClick = onShare, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share")
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            DateRangeFilter.entries.forEach { range ->
              FilterChip(
                selected = selectedDateRange == range,
                onClick = { onSelectDateRange(range) },
                label = { Text(range.label) },
              )
            }
          }
        }
      }

      when (selectedReport) {
        ReportKind.PROFIT_LOSS -> {
          val netSalesExclTax = MoneyUtils.round2(metrics.netSales - metrics.outputGst)
          item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              KpiMetricCard(
                title = "Total Sales",
                value = formatCurrency(metrics.netSales, currencySymbol),
                subtitle = "Excl. GST: ${formatCurrency(netSalesExclTax, currencySymbol)}",
                icon = Icons.Filled.Assessment,
                accentColor = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                cardColor = semantic.infoCardBg,
                cardBorderColor = semantic.infoCardBorder,
                valueColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
              )
              KpiMetricCard(
                title = "Net Profit",
                value = formatCurrency(metrics.netProfit, currencySymbol),
                subtitle = "After COGS, Exp & Comm.",
                icon = Icons.Filled.Assessment,
                accentColor = if (metrics.netProfit >= 0) semantic.positive else semantic.negative,
                containerColor = if (metrics.netProfit >= 0) semantic.positiveContainer else semantic.negativeContainer,
                cardColor = if (metrics.netProfit >= 0) semantic.positiveCardBg else semantic.negativeCardBg,
                cardBorderColor = if (metrics.netProfit >= 0) semantic.positiveCardBorder else semantic.negativeCardBorder,
                valueColor = if (metrics.netProfit >= 0) semantic.positive else semantic.negative,
                modifier = Modifier.weight(1f),
              )
              KpiMetricCard(
                title = "Output GST",
                value = formatCurrency(metrics.outputGst, currencySymbol),
                subtitle = "ITC ${formatCurrency(metrics.inputGst, currencySymbol)}",
                icon = Icons.Filled.Assessment,
                accentColor = semantic.warning,
                containerColor = semantic.warningContainer,
                cardColor = semantic.warningCardBg,
                cardBorderColor = semantic.warningCardBorder,
                valueColor = semantic.warning,
                modifier = Modifier.weight(1f),
              )
            }
          }
          item {
            ReportSummaryCard(
              rows = listOf(
                "Gross Sales Revenue" to formatCurrency(metrics.totalSales, currencySymbol),
                "Less Credit Notes / Returns" to "-${formatCurrency(metrics.creditNotesTotal, currencySymbol)}",
                "Net Sales (Incl. GST)" to formatCurrency(metrics.netSales, currencySymbol),
                "Less Output GST Liability" to "-${formatCurrency(metrics.outputGst, currencySymbol)}",
                "Net Sales (Excl. Tax)" to formatCurrency(netSalesExclTax, currencySymbol),
                "Less Historical Cost of Goods Sold (COGS)" to "-${formatCurrency(metrics.cogs, currencySymbol)}",
                "Less Business Expenses" to "-${formatCurrency(metrics.totalExpenses, currencySymbol)}",
                "Less Accrued Agent Commissions" to "-${formatCurrency(metrics.commissionAccruedPeriod, currencySymbol)}",
                "Closing Inventory Value (On Hand)" to formatCurrency(metrics.stockValue, currencySymbol),
                "Net Period Profit / Loss" to formatCurrency(metrics.netProfit, currencySymbol),
              )
            )
          }
        }

        ReportKind.CASH_FLOW -> {
          val cashIn = activeTxs.sumOf { tx ->
            when (tx.txType) {
              TxType.SALE.name -> tx.paidAmount
              TxType.PAYMENT_IN.name -> tx.totalAmount
              TxType.DEBIT_NOTE.name -> tx.paidAmount
              else -> 0.0
            }
          }
          val cashOut = activeTxs.sumOf { tx ->
            when (tx.txType) {
              TxType.PURCHASE.name -> tx.paidAmount
              TxType.PAYMENT_OUT.name -> tx.totalAmount
              TxType.EXPENSE.name -> tx.totalAmount
              TxType.CREDIT_NOTE.name -> tx.paidAmount
              TxType.COMMISSION_PAYOUT.name -> tx.paidAmount
              else -> 0.0
            }
          }
          item {
            ReportSummaryCard(
              rows = listOf(
                "Total Cash / Bank Inflow" to formatCurrency(cashIn, currencySymbol),
                "Total Cash / Bank Outflow" to formatCurrency(cashOut, currencySymbol),
                "Net Period Cash Flow" to formatCurrency(cashIn - cashOut, currencySymbol),
                "Current Cash & Bank Liquidity" to formatCurrency(metrics.cashAndBankTotal, currencySymbol),
              )
            )
          }
        }

        ReportKind.GSTR_1 -> {
          val sales = activeTxs.filter { it.txType == TxType.SALE.name }
          val creditNotes = activeTxs.filter { it.txType == TxType.CREDIT_NOTE.name }
          val b2bPartyIds = parties.filter { it.gstin.isNotBlank() }.map { it.id }.toSet()
          val b2bTotal = sales.filter { it.partyId in b2bPartyIds }.sumOf { it.totalAmount }
          val b2cTotal = sales.filter { it.partyId !in b2bPartyIds }.sumOf { it.totalAmount }
          val taxableOut = MoneyUtils.round2(metrics.netSales - metrics.outputGst)
          item {
            ReportSummaryCard(
              rows = listOf(
                "Outward Invoices Count" to "${sales.size} sales, ${creditNotes.size} credit notes",
                "B2B Registered Supplies" to formatCurrency(b2bTotal, currencySymbol),
                "B2C Unregistered Supplies" to formatCurrency(b2cTotal, currencySymbol),
                "Net Outward Taxable Value" to formatCurrency(taxableOut, currencySymbol),
                "Output CGST" to formatCurrency(metrics.cgstOutput, currencySymbol),
                "Output SGST" to formatCurrency(metrics.sgstOutput, currencySymbol),
                "Output IGST" to formatCurrency(metrics.igstOutput, currencySymbol),
                "Total Output GST (After Credit Notes)" to formatCurrency(metrics.outputGst, currencySymbol),
              )
            )
          }
        }

        ReportKind.GSTR_2 -> {
          val purchases = activeTxs.filter { it.txType == TxType.PURCHASE.name }
          val debitNotes = activeTxs.filter { it.txType == TxType.DEBIT_NOTE.name }
          val netPurch = MoneyUtils.round2(metrics.totalPurchases - metrics.debitNotesTotal)
          val taxableIn = MoneyUtils.round2(netPurch - metrics.inputGst)
          item {
            ReportSummaryCard(
              rows = listOf(
                "Inward Bills Count" to "${purchases.size} purchases, ${debitNotes.size} debit notes",
                "Gross Purchase Value" to formatCurrency(metrics.totalPurchases, currencySymbol),
                "Less Debit Notes / Returns" to "-${formatCurrency(metrics.debitNotesTotal, currencySymbol)}",
                "Net Inward Taxable Value" to formatCurrency(taxableIn, currencySymbol),
                "Input CGST (ITC)" to formatCurrency(metrics.cgstInput, currencySymbol),
                "Input SGST (ITC)" to formatCurrency(metrics.sgstInput, currencySymbol),
                "Input IGST (ITC)" to formatCurrency(metrics.igstInput, currencySymbol),
                "Total Eligible ITC" to formatCurrency(metrics.inputGst, currencySymbol),
              )
            )
          }
        }

        ReportKind.GSTR_3B -> {
          val netGst = MoneyUtils.round2(metrics.outputGst - metrics.inputGst)
          val netLabel = if (netGst >= 0) "Net GST Payable (Cash Ledger)" else "ITC Credit Carry-Forward"
          val netDisplay = formatCurrency(kotlin.math.abs(netGst), currencySymbol)
          item {
            ReportSummaryCard(
              rows = listOf(
                "Output CGST" to formatCurrency(metrics.cgstOutput, currencySymbol),
                "Output SGST" to formatCurrency(metrics.sgstOutput, currencySymbol),
                "Output IGST" to formatCurrency(metrics.igstOutput, currencySymbol),
                "Total Output GST" to formatCurrency(metrics.outputGst, currencySymbol),
                "Less Input Tax Credit (CGST + SGST + IGST)" to "-${formatCurrency(metrics.inputGst, currencySymbol)}",
                netLabel to netDisplay,
              )
            )
          }
        }

        ReportKind.HSN_SUMMARY -> {
          val hsnLines =
            activeTxs
              .filter { it.txType == TxType.SALE.name || it.txType == TxType.CREDIT_NOTE.name }
              .flatMap { tx ->
                val sign = if (tx.txType == TxType.CREDIT_NOTE.name) -1.0 else 1.0
                tx.lineItems.map { it to sign }
              }
              .groupBy { (line, _) -> line.hsnSac.ifBlank { "No HSN" } }
          if (hsnLines.isEmpty()) {
            item { Text("No outward HSN line items recorded in this period.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
          } else {
            items(hsnLines.entries.toList()) { (hsn, pairs) ->
              val qty = pairs.sumOf { (line, sign) -> line.quantity * sign }
              val tax = pairs.sumOf { (line, sign) -> line.taxAmount * sign }
              val total = pairs.sumOf { (line, sign) -> line.lineTotal * sign }
              ReportLineRow(
                title = "HSN: $hsn",
                subtitle = "Net Qty: ${formatQty(qty)} • GST: ${formatCurrency(tax, currencySymbol)}",
                trailing = formatCurrency(total, currencySymbol),
              )
            }
          }
        }

        ReportKind.ALL_PARTIES -> {
          val activeParties = parties.filter { it.isActive }
          if (activeParties.isEmpty()) {
            item { Text("No active parties recorded.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
          } else {
            items(activeParties, key = { it.id }) { p ->
              ReportLineRow(
                title = p.name,
                subtitle = listOfNotNull(
                  p.phone.takeIf { it.isNotBlank() },
                  p.stateName.takeIf { it.isNotBlank() },
                  p.partyType,
                ).joinToString(" • "),
                trailing =
                  when {
                    p.currentBalance > 0.01 -> "+${formatCurrency(p.currentBalance, currencySymbol)} Recv"
                    p.currentBalance < -0.01 -> "-${formatCurrency(-p.currentBalance, currencySymbol)} Pay"
                    else -> "Settled"
                  },
              )
            }
          }
        }

        ReportKind.STOCK_SUMMARY, ReportKind.LOW_STOCK -> {
          val list =
            if (selectedReport == ReportKind.LOW_STOCK) metrics.lowStockItems
            else items.filter { it.isActive && it.itemType == ItemType.PRODUCT.name }
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

        ReportKind.AGENT_COMMISSION -> {
          val agentSalesMap = activeTxs
            .filter { it.txType == TxType.SALE.name && (it.agentId.isNotBlank() || it.commissionSplits.isNotEmpty()) }
            .flatMap { tx ->
              if (tx.commissionSplits.isNotEmpty()) {
                tx.commissionSplits.map { sp -> sp.agentId to (tx.totalAmount * sp.sharePercent / 100.0) }
              } else {
                listOf(tx.agentId to tx.totalAmount)
              }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, list) -> list.sum() }
          val totalAgentSales = agentSalesMap.values.sum()
          val activeAgents = agents.filter { it.isActive }
          val maxAgentSale = (activeAgents.maxOfOrNull { agentSalesMap[it.id] ?: 0.0 } ?: 1.0).coerceAtLeast(1.0)

          item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiMetricCard(
                  title = "Total Sales",
                  value = formatCurrency(totalAgentSales, currencySymbol),
                  subtitle = "Via Agents & Mistri",
                  icon = Icons.Filled.Assessment,
                  accentColor = MaterialTheme.colorScheme.primary,
                  containerColor = MaterialTheme.colorScheme.primaryContainer,
                  cardColor = semantic.infoCardBg,
                  cardBorderColor = semantic.infoCardBorder,
                  modifier = Modifier.weight(1f),
                )
                KpiMetricCard(
                  title = "Commission Earned",
                  value = formatCurrency(metrics.commissionAccruedPeriod, currencySymbol),
                  subtitle = "Period accrued",
                  icon = Icons.Filled.Engineering,
                  accentColor = MaterialTheme.colorScheme.primary,
                  containerColor = MaterialTheme.colorScheme.primaryContainer,
                  modifier = Modifier.weight(1f),
                )
              }
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiMetricCard(
                  title = "Paid Out",
                  value = formatCurrency(metrics.commissionPaidPeriod, currencySymbol),
                  subtitle = "Settled in period",
                  icon = Icons.Filled.Engineering,
                  accentColor = semantic.positive,
                  containerColor = semantic.positiveContainer,
                  cardColor = semantic.positiveCardBg,
                  cardBorderColor = semantic.positiveCardBorder,
                  valueColor = semantic.positive,
                  modifier = Modifier.weight(1f),
                )
                KpiMetricCard(
                  title = "Payable (Baaki)",
                  value = formatCurrency(metrics.commissionToPay, currencySymbol),
                  subtitle = "Due now",
                  icon = Icons.Filled.Engineering,
                  accentColor = semantic.warning,
                  containerColor = semantic.warningContainer,
                  cardColor = semantic.warningCardBg,
                  cardBorderColor = semantic.warningCardBorder,
                  valueColor = semantic.warning,
                  modifier = Modifier.weight(1f),
                )
              }
            }
          }

          if (activeAgents.isEmpty()) {
            item { Text("No active Agents / Mistri added yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
          } else {
            // Horizontal "Sales by Agent" Bar Chart Card
            item {
              Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
              ) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                  Text("Sales by Agent", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                  activeAgents.sortedByDescending { agentSalesMap[it.id] ?: 0.0 }.take(6).forEach { ag ->
                    val saleVal = agentSalesMap[ag.id] ?: 0.0
                    val ratio = (saleVal / maxAgentSale).toFloat().coerceIn(0.02f, 1f)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(ag.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Text(formatCurrency(saleVal, currencySymbol), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                      }
                      Box(
                        modifier = Modifier
                          .fillMaxWidth()
                          .height(8.dp)
                          .clip(RoundedCornerShape(4.dp))
                          .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                      ) {
                        Box(
                          modifier = Modifier
                            .fillMaxWidth(if (saleVal <= 0.0) 0f else ratio)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        )
                      }
                    }
                  }
                }
              }
            }

            // 4-Column Summary Table (Agent | Sale | Earned | Baaki)
            item {
              Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
              ) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                  Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Text("Agent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.4f))
                    Text("Sale", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                    Text("Earned", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End, modifier = Modifier.weight(0.95f))
                    Text("Baaki", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End, modifier = Modifier.weight(0.95f))
                  }
                  HorizontalDivider()
                  activeAgents.forEachIndexed { idx, ag ->
                    val saleVal = agentSalesMap[ag.id] ?: 0.0
                    Row(
                      modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Column(modifier = Modifier.weight(1.4f)) {
                        Text(ag.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(ag.agentTypeEnum().label.substringBefore(" /"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                      }
                      Text(formatCurrency(saleVal, currencySymbol), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                      Text(formatCurrency(ag.totalEarned, currencySymbol), style = MaterialTheme.typography.bodySmall, color = semantic.positive, textAlign = TextAlign.End, modifier = Modifier.weight(0.95f))
                      Text(formatCurrency(ag.currentBalance, currencySymbol), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = if (ag.currentBalance > 0.01) semantic.warning else semantic.positive, textAlign = TextAlign.End, modifier = Modifier.weight(0.95f))
                    }
                    if (idx < activeAgents.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                  }
                }
              }
            }
          }
        }

        ReportKind.COMMISSION_LEDGER -> {
          val commTxs = activeTxs.filter {
            it.commissionTotalAmount > 0.001 || it.txType == TxType.COMMISSION_PAYOUT.name
          }
          if (commTxs.isEmpty()) {
            item { Text("No commission-linked invoices or payouts in this period.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
          } else {
            items(commTxs, key = { it.id }) { tx ->
              val isPayout = tx.txType == TxType.COMMISSION_PAYOUT.name
              ReportLineRow(
                title = "${tx.refNumber} • ${tx.agentName.ifBlank { tx.partyName }}",
                subtitle = if (isPayout) {
                  "Payout (${formatDate(tx.dateMillis)}) • TDS: ${formatCurrency(tx.tdsAmount, currencySymbol)}"
                } else {
                  "${tx.txTypeEnum().shortLabel} (${formatDate(tx.dateMillis)}) • ${tx.commissionStatus}"
                },
                trailing = if (isPayout) {
                  "-${formatCurrency(tx.totalAmount, currencySymbol)}"
                } else {
                  formatCurrency(tx.commissionAccruedAmount, currencySymbol)
                },
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
          Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
          Spacer(modifier = Modifier.width(8.dp))
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
