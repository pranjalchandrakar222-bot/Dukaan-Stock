package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.BusinessProfile
import com.example.data.model.BusinessTransaction
import com.example.data.model.CashBankAccount
import com.example.data.model.TxStatus
import com.example.data.model.TxType
import com.example.data.model.statusEnum
import com.example.data.model.txTypeEnum
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.StatusBadgeChip
import com.example.ui.components.txStatusColors
import com.example.ui.theme.LocalDukaanColors
import com.example.ui.theme.StitchHeroEnd
import com.example.ui.theme.StitchHeroMid
import com.example.ui.theme.StitchHeroStart
import com.example.ui.util.formatCurrency
import com.example.ui.util.formatDate
import com.example.ui.util.formatQty
import com.example.ui.viewmodel.DashboardMetrics
import com.example.ui.viewmodel.DateRangeFilter

@Composable
fun HomeDashboardScreen(
  profile: BusinessProfile?,
  metrics: DashboardMetrics,
  accounts: List<CashBankAccount>,
  recentTransactions: List<BusinessTransaction>,
  selectedDateRange: DateRangeFilter,
  hasPendingWrites: Boolean,
  onSelectDateRange: (DateRangeFilter) -> Unit,
  onNewSale: () -> Unit,
  onNewPurchase: () -> Unit,
  onAddExpense: () -> Unit,
  onAddItem: () -> Unit,
  onAddParty: () -> Unit,
  onOpenReports: () -> Unit,
  onOpenItems: () -> Unit,
  onOpenTransactions: () -> Unit,
  onOpenAgents: () -> Unit,
  onOpenCashBank: () -> Unit,
  onTransactionClick: (BusinessTransaction) -> Unit,
) {
  val sym = profile?.currencySymbol ?: "₹"
  val semantic = LocalDukaanColors.current

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val isWide = maxWidth >= 840.dp

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Deep Royal Cobalt Hero Banner matching Stitch Mockup
      item {
        Surface(
          shape = RoundedCornerShape(22.dp),
          color = Color.Transparent,
          shadowElevation = 4.dp,
        ) {
          Column(
            modifier =
              Modifier
                .fillMaxWidth()
                .background(
                  Brush.linearGradient(
                    colors = listOf(StitchHeroStart, StitchHeroMid, StitchHeroEnd)
                  )
                )
                .padding(20.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Image(
                  painter = painterResource(id = R.drawable.ic_dukaan_logo),
                  contentDescription = "Dukaan Stock Logo",
                  contentScale = ContentScale.Fit,
                  modifier = Modifier.size(48.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = profile?.businessName?.ifBlank { "My Dukaan Store" } ?: "My Dukaan Store",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text =
                      if (recentTransactions.isEmpty()) {
                        "Create your first GST invoice in less than a minute"
                      } else {
                        val syncNote = if (hasPendingWrites) "Syncing..." else "${metrics.lowStockItems.size} low stock alerts"
                        "Net Profit ${formatCurrency(metrics.netProfit, sym)}  •  Output GST ${formatCurrency(metrics.outputGst, sym)}  •  $syncNote"
                      },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFDBEAFE),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
              if (isWide) {
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                  onClick = onNewSale,
                  shape = RoundedCornerShape(14.dp),
                  colors =
                    ButtonDefaults.buttonColors(
                      containerColor = Color.White,
                      contentColor = StitchHeroStart,
                    ),
                  modifier = Modifier.height(48.dp).testTag("home_new_sale_button"),
                ) {
                  Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("New Sale Invoice", fontWeight = FontWeight.Bold)
                }
              }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              HeroQuickPill(
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                label = "+ Sale Invoice",
                emphasized = true,
                onClick = onNewSale,
                tag = "quick_action_sale",
              )
              HeroQuickPill(
                icon = Icons.Filled.ShoppingCart,
                label = "+ Purchase Bill",
                onClick = onNewPurchase,
                tag = "quick_action_purchase",
              )
              HeroQuickPill(
                icon = Icons.Filled.AccountBalanceWallet,
                label = "+ Expense",
                onClick = onAddExpense,
                tag = "quick_action_expense",
              )
              HeroQuickPill(
                icon = Icons.Filled.Inventory2,
                label = "+ Add Item",
                onClick = onAddItem,
                tag = "quick_action_item",
              )
              HeroQuickPill(
                icon = Icons.Filled.PersonAdd,
                label = "+ Add Party",
                onClick = onAddParty,
                tag = "quick_action_party",
              )
              HeroQuickPill(
                icon = Icons.Filled.Engineering,
                label = "+ Agent Payout",
                onClick = onOpenAgents,
                tag = "quick_action_agent_payout",
              )
            }
          }
        }
      }

      // Date Range Filter Chips
      item {
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          DateRangeFilter.entries.forEach { range ->
            FilterChip(
              selected = selectedDateRange == range,
              onClick = { onSelectDateRange(range) },
              label = { Text(range.label) },
              modifier = Modifier.testTag("home_range_${range.name.lowercase()}"),
            )
          }
        }
      }

      // 4 Pastel-Tinted KPI Cards (matching Computer UI & Mobile clean 2x2 grid)
      item {
        if (isWide) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            KpiMetricCard(
              title = "Total Sales (${selectedDateRange.label})",
              value = formatCurrency(metrics.netSales, sym),
              subtitle = "Output GST: ${formatCurrency(metrics.outputGst, sym)}",
              icon = Icons.AutoMirrored.Filled.TrendingUp,
              accentColor = MaterialTheme.colorScheme.primary,
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              cardColor = semantic.infoCardBg,
              cardBorderColor = semantic.infoCardBorder,
              onClick = onOpenTransactions,
              modifier = Modifier.weight(1f),
            )
            KpiMetricCard(
              title = "To Receive",
              value = formatCurrency(metrics.totalReceivable, sym),
              subtitle = "Customer receivables",
              icon = Icons.Filled.ArrowDownward,
              accentColor = semantic.positive,
              containerColor = semantic.positiveContainer,
              cardColor = semantic.positiveCardBg,
              cardBorderColor = semantic.positiveCardBorder,
              valueColor = semantic.positive,
              modifier = Modifier.weight(1f),
            )
            KpiMetricCard(
              title = "To Pay",
              value = formatCurrency(metrics.totalPayable, sym),
              subtitle = "Supplier payables",
              icon = Icons.Filled.ArrowUpward,
              accentColor = semantic.negative,
              containerColor = semantic.negativeContainer,
              cardColor = semantic.negativeCardBg,
              cardBorderColor = semantic.negativeCardBorder,
              valueColor = semantic.negative,
              modifier = Modifier.weight(1f),
            )
            KpiMetricCard(
              title = "Commission to Pay",
              value = formatCurrency(metrics.commissionToPay, sym),
              subtitle = "Accrued: ${formatCurrency(metrics.commissionAccruedPeriod, sym)}",
              icon = Icons.Filled.Engineering,
              accentColor = semantic.warning,
              containerColor = semantic.warningContainer,
              cardColor = semantic.warningCardBg,
              cardBorderColor = semantic.warningCardBorder,
              valueColor = semantic.warning,
              onClick = onOpenAgents,
              modifier = Modifier.weight(1f).testTag("kpi_commission_to_pay"),
            )
          }
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              KpiMetricCard(
                title = "Total Sales",
                value = formatCurrency(metrics.netSales, sym),
                subtitle = "GST ${formatCurrency(metrics.outputGst, sym)}",
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                accentColor = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                cardColor = semantic.infoCardBg,
                cardBorderColor = semantic.infoCardBorder,
                onClick = onOpenTransactions,
                modifier = Modifier.weight(1f),
              )
              KpiMetricCard(
                title = "To Receive",
                value = formatCurrency(metrics.totalReceivable, sym),
                subtitle = "Customer dues",
                icon = Icons.Filled.ArrowDownward,
                accentColor = semantic.positive,
                containerColor = semantic.positiveContainer,
                cardColor = semantic.positiveCardBg,
                cardBorderColor = semantic.positiveCardBorder,
                valueColor = semantic.positive,
                modifier = Modifier.weight(1f),
              )
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              KpiMetricCard(
                title = "To Pay",
                value = formatCurrency(metrics.totalPayable, sym),
                subtitle = "Supplier dues",
                icon = Icons.Filled.ArrowUpward,
                accentColor = semantic.negative,
                containerColor = semantic.negativeContainer,
                cardColor = semantic.negativeCardBg,
                cardBorderColor = semantic.negativeCardBorder,
                valueColor = semantic.negative,
                modifier = Modifier.weight(1f),
              )
              KpiMetricCard(
                title = "Commission to Pay",
                value = formatCurrency(metrics.commissionToPay, sym),
                subtitle = "Agents & Mistris",
                icon = Icons.Filled.Engineering,
                accentColor = semantic.warning,
                containerColor = semantic.warningContainer,
                cardColor = semantic.warningCardBg,
                cardBorderColor = semantic.warningCardBorder,
                valueColor = semantic.warning,
                onClick = onOpenAgents,
                modifier = Modifier.weight(1f).testTag("kpi_commission_to_pay"),
              )
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              KpiMetricCard(
                title = "Cash & Bank",
                value = formatCurrency(metrics.cashAndBankTotal, sym),
                subtitle = "Profit ${formatCurrency(metrics.netProfit, sym)}",
                icon = Icons.Filled.AccountBalanceWallet,
                accentColor = MaterialTheme.colorScheme.secondary,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                onClick = onOpenCashBank,
                modifier = Modifier.weight(1f),
              )
              KpiMetricCard(
                title = "Stock Value",
                value = formatCurrency(metrics.stockValue, sym),
                subtitle = "${metrics.lowStockItems.size} low alerts",
                icon = Icons.Filled.Inventory2,
                accentColor = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                onClick = onOpenItems,
                modifier = Modifier.weight(1f),
              )
            }
          }
        }
      }

      // Main Content Area: 2-Column Split on Wide Computer/Tablet, Stacked on Mobile
      if (isWide) {
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top,
          ) {
            // Left: Recent Transactions Card
            Surface(
              modifier = Modifier.weight(1.35f),
              shape = RoundedCornerShape(20.dp),
              color = MaterialTheme.colorScheme.surface,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
              Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                  )
                  TextButton(onClick = onOpenTransactions) {
                    Text("See All")
                  }
                }
                if (recentTransactions.isEmpty()) {
                  EmptyStateCard(
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    title = "No transactions recorded yet",
                    subtitle = "Start by adding items and parties, or create your first Sale Invoice.",
                    actionLabel = "Create First Invoice",
                    onAction = onNewSale,
                  )
                } else {
                  recentTransactions.take(8).forEachIndexed { idx, tx ->
                    TransactionRowCard(
                      tx = tx,
                      currencySymbol = sym,
                      onClick = { onTransactionClick(tx) },
                    )
                    if (idx < minOf(7, recentTransactions.lastIndex)) {
                      Spacer(modifier = Modifier.height(2.dp))
                    }
                  }
                }
              }
            }

            // Right: Low Stock Alerts + Cash & Bank Summary
            Column(
              modifier = Modifier.weight(0.95f),
              verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
              LowStockSummaryCard(
                metrics = metrics,
                onOpenItems = onOpenItems,
              )
              CashBankSummaryCard(
                accounts = accounts,
                totalBalance = metrics.cashAndBankTotal,
                currencySymbol = sym,
                onOpenCashBank = onOpenCashBank,
                onOpenReports = onOpenReports,
              )
            }
          }
        }
      } else {
        if (metrics.lowStockItems.isNotEmpty()) {
          item {
            LowStockSummaryCard(
              metrics = metrics,
              onOpenItems = onOpenItems,
            )
          }
        }
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Recent Transactions",
              style = MaterialTheme.typography.titleLarge,
            )
            if (recentTransactions.isNotEmpty()) {
              TextButton(onClick = onOpenTransactions) {
                Text("See All")
              }
            }
          }
        }
        if (recentTransactions.isEmpty()) {
          item {
            EmptyStateCard(
              icon = Icons.AutoMirrored.Filled.ReceiptLong,
              title = "No transactions recorded yet",
              subtitle =
                "Start by adding your inventory items and parties, or jump straight into creating your first Sale Invoice.",
              actionLabel = "Create Your First Invoice",
              onAction = onNewSale,
            )
          }
        } else {
          item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              recentTransactions.take(10).forEach { tx ->
                TransactionRowCard(
                  tx = tx,
                  currencySymbol = sym,
                  onClick = { onTransactionClick(tx) },
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun LowStockSummaryCard(
  metrics: DashboardMetrics,
  onOpenItems: () -> Unit,
) {
  val semantic = LocalDukaanColors.current
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = if (metrics.lowStockItems.isNotEmpty()) semantic.warningCardBg else MaterialTheme.colorScheme.surface,
    border =
      BorderStroke(
        1.dp,
        if (metrics.lowStockItems.isNotEmpty()) semantic.warningCardBorder else MaterialTheme.colorScheme.outlineVariant,
      ),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            Icons.Filled.WarningAmber,
            contentDescription = null,
            tint = if (metrics.lowStockItems.isNotEmpty()) semantic.warning else semantic.positive,
            modifier = Modifier.size(20.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Low Stock Alerts (${metrics.lowStockItems.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (metrics.lowStockItems.isNotEmpty()) semantic.onWarningContainer else MaterialTheme.colorScheme.onSurface,
          )
        }
        TextButton(onClick = onOpenItems) {
          Text("Stock")
        }
      }

      if (metrics.lowStockItems.isEmpty()) {
        Text(
          text = "All active inventory items are comfortably above minimum stock thresholds.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        metrics.lowStockItems.take(5).forEach { item ->
          Row(
            modifier = Modifier.fillMaxWidth().clickable { onOpenItems() }.padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = item.name,
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Medium,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Left: ${formatQty(item.currentStock)} ${item.unit} (Min ${formatQty(item.minStockLevel)})",
              style = MaterialTheme.typography.labelMedium,
              color = semantic.negative,
              fontWeight = FontWeight.SemiBold,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun CashBankSummaryCard(
  accounts: List<CashBankAccount>,
  totalBalance: Double,
  currencySymbol: String,
  onOpenCashBank: () -> Unit,
  onOpenReports: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            Icons.Filled.AccountBalance,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text("Cash & Bank", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Row {
          TextButton(onClick = onOpenReports) { Text("P&L") }
          TextButton(onClick = onOpenCashBank) { Text("Accounts") }
        }
      }

      if (accounts.isEmpty()) {
        Text(
          "No cash or bank accounts active.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else {
        accounts.take(4).forEach { acc ->
          Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              acc.accountName,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              formatCurrency(acc.currentBalance, currencySymbol),
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold,
            )
          }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text("Total Liquidity", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
          Text(
            formatCurrency(totalBalance, currencySymbol),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }
    }
  }
}

@Composable
private fun HeroQuickPill(
  icon: ImageVector,
  label: String,
  emphasized: Boolean = false,
  onClick: () -> Unit,
  tag: String,
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(999.dp),
    color = if (emphasized) Color.White else Color.White.copy(alpha = 0.14f),
    border = BorderStroke(1.dp, if (emphasized) Color.White else Color.White.copy(alpha = 0.25f)),
    modifier = Modifier.height(48.dp).testTag(tag),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        icon,
        contentDescription = null,
        tint = if (emphasized) StitchHeroStart else Color.White,
        modifier = Modifier.size(16.dp),
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = if (emphasized) StitchHeroStart else Color.White,
      )
    }
  }
}

@Composable
fun TransactionRowCard(
  tx: BusinessTransaction,
  currencySymbol: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val type = tx.txTypeEnum()
  val status = tx.statusEnum()
  val (statusFg, statusBg) = txStatusColors(status)
  val semantic = LocalDukaanColors.current

  val isIncoming =
    type == TxType.SALE || type == TxType.PAYMENT_IN || type == TxType.DEBIT_NOTE

  Surface(
    modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
              if (isIncoming) semantic.positiveContainer else MaterialTheme.colorScheme.surfaceVariant
            ),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector =
            if (isIncoming) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
          contentDescription = null,
          tint =
            if (isIncoming) semantic.positive else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(20.dp),
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = tx.partyName.ifBlank { tx.category.ifBlank { type.label } },
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "${type.shortLabel} • ${tx.refNumber} • ${formatDate(tx.dateMillis)}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Spacer(modifier = Modifier.width(10.dp))

      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = formatCurrency(tx.totalAmount, currencySymbol),
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        StatusBadgeChip(
          label =
            if (status == TxStatus.PARTIAL && tx.balanceAmount > 0) {
              "Bal ${formatCurrency(tx.balanceAmount, currencySymbol)}"
            } else {
              status.label
            },
          color = statusFg,
          containerColor = statusBg,
        )
      }
    }
  }
}
