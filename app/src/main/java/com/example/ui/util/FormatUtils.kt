package com.example.ui.util

import android.content.Context
import android.content.Intent
import com.example.data.model.BusinessProfile
import com.example.data.model.BusinessTransaction
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

private val amountFormatter = DecimalFormat("#,##,##0.00")
private val compactQtyFormatter = DecimalFormat("#,##0.##")

fun formatCurrency(amount: Double, symbol: String = "₹"): String {
  val sign = if (amount < -0.005) "-" else ""
  return "$sign$symbol${amountFormatter.format(abs(amount))}"
}

fun formatQty(qty: Double): String = compactQtyFormatter.format(qty)

fun formatDate(millis: Long): String {
  val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
  return sdf.format(Date(millis))
}

fun formatDateTime(millis: Long): String {
  val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
  return sdf.format(Date(millis))
}

fun amountInWords(amount: Double, currencySymbol: String = "₹"): String {
  val whole = abs(amount).toLong()
  val paise = ((abs(amount) - whole) * 100.0).roundToLong().toInt()
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
    if (tx.partyName.isNotBlank()) appendLine("Party: ${tx.partyName}")
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
    if (tx.taxAmount > 0) appendLine("GST Tax: ${formatCurrency(tx.taxAmount, sym)}")
    appendLine("Total: ${formatCurrency(tx.totalAmount, sym)}")
    appendLine("Paid: ${formatCurrency(tx.paidAmount, sym)}")
    appendLine("Balance: ${formatCurrency(tx.balanceAmount, sym)}")
    if (!profile?.invoiceTerms.isNullOrBlank()) {
      appendLine()
      appendLine(profile?.invoiceTerms)
    }
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
