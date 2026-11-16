package com.example.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.model.BusinessProfile
import com.example.data.model.BusinessTransaction
import com.example.data.model.TxType
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private val compactQtyFormatter = DecimalFormat("#,##0.##")

private fun formatIndianNumber(absAmount: Double): String {
  val totalPaise = MoneyUtils.toPaise(MoneyUtils.round2(absAmount))
  val whole = totalPaise / 100L
  val paise = (totalPaise % 100L).toInt()
  val wholeStr = whole.toString()
  val grouped = if (wholeStr.length <= 3) {
    wholeStr
  } else {
    val last3 = wholeStr.takeLast(3)
    val prefix = wholeStr.dropLast(3)
    val prefixGrouped = prefix.reversed().chunked(2).joinToString(",").reversed()
    "$prefixGrouped,$last3"
  }
  return "$grouped.${paise.toString().padStart(2, '0')}"
}

fun formatCurrency(amount: Double, symbol: String = "₹"): String {
  val rounded = MoneyUtils.round2(amount)
  val sign = if (rounded < -0.005) "-" else ""
  return "$sign$symbol${formatIndianNumber(abs(rounded))}"
}

fun formatQty(qty: Double): String = compactQtyFormatter.format(qty)

fun formatDate(millis: Long): String {
  if (millis <= 0L) return "-"
  val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
  return sdf.format(Date(millis))
}

fun formatDateTime(millis: Long): String {
  if (millis <= 0L) return "-"
  val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
  return sdf.format(Date(millis))
}

fun amountInWords(amount: Double, currencySymbol: String = "₹"): String {
  val paiseTotal = MoneyUtils.toPaise(abs(amount))
  val whole = paiseTotal / 100L
  val paise = (paiseTotal % 100L).toInt()
  val currencyName =
    when (currencySymbol) {
      "$" -> "Dollars"
      "€" -> "Euros"
      "£" -> "Pounds"
      else -> "Rupees"
    }
  val subUnitName =
    when (currencySymbol) {
      "$", "€" -> "Cents"
      "£" -> "Pence"
      else -> "Paise"
    }
  val words = if (whole == 0L) "Zero" else convertNumberToWords(whole)
  return if (paise > 0) {
    "$words $currencyName and ${convertNumberToWords(paise.toLong())} $subUnitName Only"
  } else {
    "$words $currencyName Only"
  }
}

private val units =
  arrayOf(
    "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
    "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen",
  )

private val tens =
  arrayOf("", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety")

private fun convertTwoDigits(n: Int): String =
  if (n < 20) units[n] else "${tens[n / 10]}${if (n % 10 != 0) " ${units[n % 10]}" else ""}"

private fun convertNumberToWords(n: Long): String {
  if (n == 0L) return ""
  val sb = StringBuilder()
  val crore = (n / 10_000_000L).toInt()
  val lakh = ((n / 100_000L) % 100L).toInt()
  val thousand = ((n / 1_000L) % 100L).toInt()
  val hundred = ((n / 100L) % 10L).toInt()
  val rest = (n % 100L).toInt()

  if (crore > 0) sb.append("${convertNumberToWords(crore.toLong())} Crore ")
  if (lakh > 0) sb.append("${convertTwoDigits(lakh)} Lakh ")
  if (thousand > 0) sb.append("${convertTwoDigits(thousand)} Thousand ")
  if (hundred > 0) sb.append("${units[hundred]} Hundred ")
  if (rest > 0) {
    if (sb.isNotEmpty()) sb.append("and ")
    sb.append(convertTwoDigits(rest))
  }
  return sb.toString().trim()
}

/**
 * Customer-facing text invoice share. Intentionally omits all internal Agent/Mistri commission data.
 */
fun buildShareInvoiceText(tx: BusinessTransaction, profile: BusinessProfile?): String {
  val sym = profile?.currencySymbol ?: "₹"
  val biz = profile?.businessName?.ifBlank { "Dukaan Stock" } ?: "Dukaan Stock"
  return buildString {
    appendLine("*$biz*")
    if (!profile?.phone.isNullOrBlank()) appendLine("Phone: ${profile?.phone}")
    if (!profile?.gstin.isNullOrBlank()) appendLine("GSTIN: ${profile?.gstin}")
    appendLine("----------------------------")
    appendLine("Ref No: ${tx.refNumber}")
    appendLine("Date: ${formatDate(tx.dateMillis)}")
    if (tx.dueDateMillis > 0L) appendLine("Due Date: ${formatDate(tx.dueDateMillis)}")
    if (tx.partyName.isNotBlank()) appendLine("Party: ${tx.partyName}")
    if (tx.placeOfSupply.isNotBlank()) appendLine("Place of Supply: ${tx.placeOfSupply}")
    appendLine("----------------------------")
    if (tx.lineItems.isNotEmpty()) {
      tx.lineItems.forEachIndexed { idx, line ->
        appendLine(
          "${idx + 1}. ${line.itemName} (${formatQty(line.quantity)} ${line.unit} x ${formatCurrency(line.unitPrice, sym)}) = ${formatCurrency(line.lineTotal, sym)}"
        )
      }
      appendLine("----------------------------")
    }
    appendLine("Subtotal: ${formatCurrency(tx.subtotal, sym)}")
    val totalDisc = tx.discountAmount + tx.billDiscountAmount
    if (totalDisc > 0.005) appendLine("Discount: -${formatCurrency(totalDisc, sym)}")
    if (tx.extraChargesAmount > 0.005) {
      appendLine("${tx.extraChargesLabel.ifBlank { "Extra Charges" }}: ${formatCurrency(tx.extraChargesAmount, sym)}")
    }
    if (tx.taxAmount > 0.005) {
      if (tx.isInterState || tx.igstAmount > 0.005) {
        appendLine("IGST: ${formatCurrency(if (tx.igstAmount > 0) tx.igstAmount else tx.taxAmount, sym)}")
      } else {
        val cgst = if (tx.cgstAmount > 0) tx.cgstAmount else tx.taxAmount / 2.0
        val sgst = if (tx.sgstAmount > 0) tx.sgstAmount else tx.taxAmount - cgst
        appendLine("CGST: ${formatCurrency(cgst, sym)} | SGST: ${formatCurrency(sgst, sym)}")
      }
    }
    appendLine("Total: ${formatCurrency(tx.totalAmount, sym)}")
    appendLine("Paid: ${formatCurrency(tx.paidAmount, sym)}")
    appendLine("Balance: ${formatCurrency(tx.balanceAmount, sym)}")
    if (!profile?.upiId.isNullOrBlank() && tx.balanceAmount > 0.01 && tx.txType == TxType.SALE.name) {
      appendLine("UPI ID: ${profile?.upiId}")
    }
    if (!profile?.invoiceTerms.isNullOrBlank()) {
      appendLine()
      appendLine(profile?.invoiceTerms)
    }
  }
}

fun buildUpiPaymentUri(
  upiId: String,
  payeeName: String,
  amount: Double,
  note: String,
): String {
  val cleanPa = Uri.encode(upiId.trim())
  val cleanPn = Uri.encode(payeeName.trim().ifBlank { "Merchant" })
  val cleanAm = String.format(Locale.US, "%.2f", MoneyUtils.round2(amount))
  val cleanTn = Uri.encode(note.trim().ifBlank { "Invoice Payment" })
  return "upi://pay?pa=$cleanPa&pn=$cleanPn&am=$cleanAm&cu=INR&tn=$cleanTn"
}

fun launchUpiIntent(context: Context, upiUri: String): Boolean {
  return try {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(upiUri))
    context.startActivity(Intent.createChooser(intent, "Pay via UPI"))
    true
  } catch (_: Exception) {
    false
  }
}

fun shareText(context: Context, subject: String, text: String) {
  val intent =
    Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_SUBJECT, subject)
      putExtra(Intent.EXTRA_TEXT, text)
    }
  context.startActivity(Intent.createChooser(intent, subject))
}
