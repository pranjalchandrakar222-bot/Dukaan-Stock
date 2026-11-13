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
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.BusinessProfile
import com.example.data.model.BusinessTransaction
import com.example.data.model.TxStatus
import com.example.data.model.TxType
import com.example.data.model.statusEnum
import com.example.data.model.txTypeEnum
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.StatusBadgeChip
import com.example.ui.components.txStatusColors
import com.example.ui.theme.LocalDukaanColors
import com.example.ui.util.formatCurrency
import com.example.ui.util.formatDate
import com.example.ui.util.formatQty
import com.example.ui.viewmodel.DashboardMetrics

@Composable
fun HomeDashboardScreen(
  profile: BusinessProfile?,
  metrics: DashboardMetrics,
  recentTransactions: List<BusinessTransaction>,
  onNewSale: () -> Unit,
  onNewPurchase: () -> Unit,
  onAddExpense: () -> Unit,
  onAddItem: () -> Unit,
  onAddParty: () -> Unit,
  onOpenReports: () -> Unit,
  onOpenItems: () -> Unit,
  onOpenTransactions: () -> Unit,
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
      // Hero Greeting / Business Quick Action Header
      item {
        Surface(
          shape = RoundedCornerShape(22.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
        ) {
          Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = profile?.businessName?.ifBlank { "My Dukaan Store" } ?: "My Dukaan Store",
                  style = MaterialTheme.typography.headlineSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text =
                    if (recentTransactions.isEmpty()) {
                      "Create your first GST invoice in less than a minute"
                    } else {
                      "${recentTransactions.size} recorded transactions • Live cloud sync active"
                    },
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              if (isWide) {
                Button(
                  onClick = onNewSale,
                  shape = RoundedCornerShape(14.dp),
                  modifier = Modifier.height(48.dp).testTag("home_new_sale_button"),
                ) {
                  Icon(Icons.Filled.Add, contentDescription = null)
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("New Sale Invoice")
                }
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              QuickActionPill(
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                label = "+ Sale Invoice",
                highlighted = true,
                onClick = onNewSale,
                tag = "quick_action_sale",
              )
              QuickActionPill(
                icon = Icons.Filled.ShoppingCart,
                label = "+ Purchase Bill",
                onClick = onNewPurchase,
                tag = "quick_action_purchase",
              )
              QuickActionPill(
                icon = Icons.Filled.AccountBalanceWallet,
                label = "+ Expense",
                onClick = onAddExpense,
                tag = "quick_action_expense",
              )
              QuickActionPill(
                icon = Icons.Filled.Inventory2,
                label = "+ Add Item",
                onClick = onAddItem,
                tag = "quick_action_item",
              )
              QuickActionPill(
                icon = Icons.Filled.PersonAdd,
                label = "+ Add Party",
                onClick = onAddParty,
                tag = "quick_action_party",
              )
            }
          }
        }
      }

      // KPI Metric Cards Grid
      item {
        if (isWide) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            KpiMetricCard(
              title = "Total Sales",
              value = formatCurrency(metrics.totalSales, sym),
              subtitle = "Output GST: ${formatCurrency(metrics.outputGst, sym)}",
              icon = Icons.AutoMirrored.Filled.TrendingUp,
              accentColor = MaterialTheme.colorScheme.primary,
              containerColor = MaterialTheme.colorScheme.primaryContainer,
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
              modifier = Modifier.weight(1f),
            )
            KpiMetricCard(
              title = "To Pay",
              value = formatCurrency(metrics.totalPayable, sym),
              subtitle = "Supplier payables",
              icon = Icons.Filled.ArrowUpward,
              accentColor = semantic.negative,
              containerColor = semantic.negativeContainer,
              modifier = Modifier.weight(1f),
            )
            KpiMetricCard(
              title = "Stock Value",
              value = formatCurrency(metrics.stockValue, sym),
              subtitle = "${metrics.lowStockItems.size} low stock alerts",
              icon = Icons.Filled.Inventory2,
              accentColor = semantic.warning,
              containerColor = semantic.warningContainer,
              onClick = onOpenItems,
              modifier = Modifier.weight(1f),
            )
            KpiMetricCard(
              title = "Cash & Bank",
              value = formatCurrency(metrics.cashAndBankTotal, sym),
              subtitle = "Net Profit: ${formatCurrency(metrics.netProfit, sym)}",
              icon = Icons.Filled.AccountBalanceWallet,
              accentColor = MaterialTheme.colorScheme.secondary,
              containerColor = MaterialTheme.colorScheme.secondaryContainer,
              onClick = onOpenReports,
              modifier = Modifier.weight(1f),
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
                value = formatCurrency(metrics.totalSales, sym),
                subtitle = "GST ${formatCurrency(metrics.outputGst, sym)}",
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                accentColor = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                onClick = onOpenTransactions,
                modifier = Modifier.weight(1f),
              )
              KpiMetricCard(
                title = "Cash & Bank",
                value = formatCurrency(metrics.cashAndBankTotal, sym),
                subtitle = "Profit ${formatCurrency(metrics.netProfit, sym)}",
                icon = Icons.Filled.AccountBalanceWallet,
                accentColor = MaterialTheme.colorScheme.secondary,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                onClick = onOpenReports,
                modifier = Modifier.weight(1f),
              )
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              KpiMetricCard(
                title = "To Receive",
                value = formatCurrency(metrics.totalReceivable, sym),
                subtitle = "Customer dues",
                icon = Icons.Filled.ArrowDownward,
                accentColor = semantic.positive,
                containerColor = semantic.positiveContainer,
                modifier = Modifier.weight(1f),
              )
              KpiMetricCard(
                title = "To Pay",
                value = formatCurrency(metrics.totalPayable, sym),
                subtitle = "Supplier dues",
                icon = Icons.Filled.ArrowUpward,
                accentColor = semantic.negative,
                containerColor = semantic.negativeContainer,
                modifier = Modifier.weight(1f),
              )
            }
          }
        }
      }

      // Low Stock Alert Section
      if (metrics.lowStockItems.isNotEmpty()) {
        item {
          Surface(
            shape = RoundedCornerShape(18.dp),
            color = semantic.warningContainer.copy(alpha = 0.65f),
            border = BorderStroke(1.dp, semantic.warning.copy(alpha = 0.35f)),
          ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    Icons.Filled.WarningAmber,
                    contentDescription = null,
                    tint = semantic.warning,
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "Low Stock Alerts (${metrics.lowStockItems.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = semantic.onWarningContainer,
                  )
                }
                TextButton(onClick = onOpenItems) {
                  Text("View All Stock")
                }
              }
              Spacer(modifier = Modifier.height(8.dp))
              Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                metrics.lowStockItems.take(8).forEach { item ->
                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    onClick = onOpenItems,
                  ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                      Text(
                        text = item.name,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                      Text(
                        text = "Left: ${formatQty(item.currentStock)} ${item.unit} (Min ${formatQty(item.minStockLevel)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = semantic.negative,
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }

      // Recent Transactions Header
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
        items(recentTransactions.take(10), key = { it.id }) { tx ->
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

@Composable
private fun QuickActionPill(
  icon: ImageVector,
  label: String,
  highlighted: Boolean = false,
  onClick: () -> Unit,
  tag: String,
) {
  if (highlighted) {
    Button(
      onClick = onClick,
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier.height(48.dp).testTag(tag),
    ) {
      Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text(label)
    }
  } else {
    FilledTonalButton(
      onClick = onClick,
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier.height(48.dp).testTag(tag),
    ) {
      Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text(label)
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
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = tx.partyName.ifBlank { tx.category.ifBlank { type.label } },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
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
