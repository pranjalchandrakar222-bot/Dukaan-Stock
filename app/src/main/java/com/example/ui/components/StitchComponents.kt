package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.BusinessProfile
import com.example.data.model.BusinessTransaction
import com.example.data.model.TxStatus
import com.example.data.model.TxType
import com.example.ui.theme.LocalDukaanColors
import com.example.ui.util.amountInWords
import com.example.ui.util.buildShareInvoiceText
import com.example.ui.util.formatCurrency
import com.example.ui.util.formatDate
import com.example.ui.util.formatQty
import com.example.ui.util.shareText

@Composable
fun KpiMetricCard(
  title: String,
  value: String,
  subtitle: String,
  icon: ImageVector,
  accentColor: Color,
  containerColor: Color,
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    onClick = { onClick?.invoke() },
    enabled = onClick != null,
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
          modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(containerColor),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(18.dp),
          )
        }
      }
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun StatusBadgeChip(
  label: String,
  color: Color,
  containerColor: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(999.dp),
    color = containerColor,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.SemiBold,
      color = color,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    )
  }
}

@Composable
fun txStatusColors(status: TxStatus): Pair<Color, Color> {
  val semantic = LocalDukaanColors.current
  return when (status) {
    TxStatus.PAID, TxStatus.CONVERTED -> semantic.positive to semantic.positiveContainer
    TxStatus.PARTIAL, TxStatus.OPEN -> semantic.warning to semantic.warningContainer
    TxStatus.UNPAID, TxStatus.CANCELLED -> semantic.negative to semantic.negativeContainer
  }
}

@Composable
fun EmptyStateCard(
  icon: ImageVector,
  title: String,
  subtitle: String,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(
      modifier = Modifier.padding(28.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
        modifier = Modifier.size(64.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(30.dp),
          )
        }
      }
      Spacer(modifier = Modifier.height(14.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      if (actionLabel != null && onAction != null) {
        Spacer(modifier = Modifier.height(18.dp))
        Button(
          onClick = onAction,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.testTag("empty_state_action_button"),
        ) {
          Text(actionLabel)
        }
      }
    }
  }
}

@Composable
fun TaxInvoicePreviewCard(
  transaction: BusinessTransaction,
  profile: BusinessProfile?,
  accentColor: Color = MaterialTheme.colorScheme.primary,
  showShareAction: Boolean = true,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val sym = profile?.currencySymbol ?: "₹"
  val bizName = profile?.businessName?.ifBlank { "My Dukaan Store" } ?: "My Dukaan Store"
  val docTitle =
    when (TxType.entries.firstOrNull { it.name == transaction.txType }) {
      TxType.PURCHASE -> "PURCHASE BILL"
      TxType.ESTIMATE -> "ESTIMATE / QUOTATION"
      TxType.SALE_ORDER -> "SALE ORDER"
      TxType.PURCHASE_ORDER -> "PURCHASE ORDER"
      TxType.CREDIT_NOTE -> "CREDIT NOTE"
      TxType.DEBIT_NOTE -> "DEBIT NOTE"
      TxType.DELIVERY_CHALLAN -> "DELIVERY CHALLAN"
      else -> "TAX INVOICE"
    }

  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(18.dp),
    color = Color.White,
    shadowElevation = 2.dp,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = bizName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A),
          )
          if (!profile?.phone.isNullOrBlank()) {
            Text(
              text = "Ph: ${profile?.phone}",
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFF475569),
            )
          }
          if (!profile?.gstin.isNullOrBlank()) {
            Text(
              text = "GSTIN: ${profile?.gstin}",
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFF475569),
            )
          }
          if (!profile?.address.isNullOrBlank()) {
            Text(
              text = profile?.address.orEmpty(),
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFF64748B),
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        Column(horizontalAlignment = Alignment.End) {
          Surface(
            color = accentColor.copy(alpha = 0.12f),
            shape = RoundedCornerShape(8.dp),
          ) {
            Text(
              text = docTitle,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = accentColor,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            )
          }
          if (showShareAction) {
            IconButton(
              onClick = {
                shareText(
                  context = context,
                  subject = "$docTitle ${transaction.refNumber}",
                  text = buildShareInvoiceText(transaction, profile),
                )
              }
            ) {
              Icon(
                imageVector = Icons.Filled.Share,
                contentDescription = "Share Invoice",
                tint = accentColor,
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
      HorizontalDivider(color = Color(0xFFE2E8F0))
      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Bill To",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF64748B),
          )
          Text(
            text = transaction.partyName.ifBlank { "Cash / Walk-in Customer" },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF0F172A),
          )
        }
        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = "Invoice No: ${transaction.refNumber.ifBlank { "Draft" }}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = accentColor,
          )
          Text(
            text = "Date: ${formatDate(transaction.dateMillis)}",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF475569),
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Table Header
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .background(accentColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
      ) {
        Text("#", color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(22.dp))
        Text("Item", color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1.5f))
        Text("Qty", color = Color.White, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
        Text("Rate", color = Color.White, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End, modifier = Modifier.weight(0.9f))
        Text("GST", color = Color.White, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
        Text("Amt", color = Color.White, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
      }

      if (transaction.lineItems.isEmpty()) {
        Text(
          text = "Add items on the left to see live GST line-item breakdown",
          style = MaterialTheme.typography.bodySmall,
          color = Color(0xFF94A3B8),
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
        )
      } else {
        transaction.lineItems.forEachIndexed { idx, line ->
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("${idx + 1}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF475569), modifier = Modifier.width(22.dp))
            Column(modifier = Modifier.weight(1.5f)) {
              Text(
                line.itemName.ifBlank { "Item" },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF0F172A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              if (line.hsnSac.isNotBlank()) {
                Text("HSN: ${line.hsnSac}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
              }
            }
            Text(
              formatQty(line.quantity),
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFF334155),
              textAlign = TextAlign.End,
              modifier = Modifier.weight(0.6f),
            )
            Text(
              formatCurrency(line.unitPrice, sym),
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFF334155),
              textAlign = TextAlign.End,
              modifier = Modifier.weight(0.9f),
            )
            Text(
              "${formatQty(line.taxRatePercent)}%",
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFF64748B),
              textAlign = TextAlign.End,
              modifier = Modifier.weight(0.7f),
            )
            Text(
              formatCurrency(line.lineTotal, sym),
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.SemiBold,
              color = Color(0xFF0F172A),
              textAlign = TextAlign.End,
              modifier = Modifier.weight(1f),
            )
          }
          HorizontalDivider(color = Color(0xFFF1F5F9))
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(modifier = Modifier.weight(1f)) {
          Text("Amount In Words", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
          Text(
            text = amountInWords(transaction.totalAmount, sym),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF334155),
          )
          if (!profile?.bankDetails.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Bank Details", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
            Text(
              text = profile?.bankDetails.orEmpty(),
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFF475569),
            )
          }
        }

        Column(modifier = Modifier.weight(0.95f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          InvoiceSummaryLine("Sub Total", formatCurrency(transaction.subtotal, sym))
          if (transaction.discountAmount > 0.005) {
            InvoiceSummaryLine("Discount", "-${formatCurrency(transaction.discountAmount, sym)}")
          }
          if (transaction.taxAmount > 0.005) {
            InvoiceSummaryLine("CGST", formatCurrency(transaction.taxAmount / 2.0, sym))
            InvoiceSummaryLine("SGST", formatCurrency(transaction.taxAmount / 2.0, sym))
          }
          if (kotlin.math.abs(transaction.roundOffAmount) > 0.001) {
            InvoiceSummaryLine("Round Off", formatCurrency(transaction.roundOffAmount, sym))
          }
          Row(
            modifier =
              Modifier
                .fillMaxWidth()
                .background(accentColor, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text("Total", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
            Text(formatCurrency(transaction.totalAmount, sym), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
          }
          InvoiceSummaryLine("Received / Paid", formatCurrency(transaction.paidAmount, sym))
          InvoiceSummaryLine("Balance Due", formatCurrency(transaction.balanceAmount, sym), bold = true)
        }
      }

      if (!profile?.invoiceTerms.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = Color(0xFFE2E8F0))
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Terms: ${profile?.invoiceTerms}",
          style = MaterialTheme.typography.labelSmall,
          color = Color(0xFF64748B),
        )
      }
    }
  }
}

@Composable
private fun InvoiceSummaryLine(
  label: String,
  value: String,
  bold: Boolean = false,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodySmall,
      fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
      color = Color(0xFF475569),
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall,
      fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
      color = Color(0xFF0F172A),
    )
  }
}
