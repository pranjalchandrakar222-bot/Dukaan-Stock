package com.example.ui.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.BusinessProfile
import com.example.data.model.BusinessTransaction
import com.example.data.model.Party
import com.example.data.model.TxStatus
import com.example.data.model.TxType
import java.io.File
import java.io.FileOutputStream

object PdfInvoiceHelper {

  /**
   * Generates a clean A4 customer-facing PDF invoice/voucher and opens the system share sheet.
   * Internal Agent/Mistri commission figures are strictly excluded from customer PDFs.
   */
  fun generateAndShareInvoicePdf(
    context: Context,
    transaction: BusinessTransaction,
    profile: BusinessProfile?,
    party: Party? = null,
  ): Boolean {
    return try {
      val dir = File(context.cacheDir, "invoices").apply { mkdirs() }
      val safeRef = transaction.refNumber.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "invoice" }
      val outFile = File(dir, "${safeRef}_${System.currentTimeMillis()}.pdf")

      val pdfDoc = PdfDocument()
      val pageWidth = 595
      val pageHeight = 842
      var pageNumber = 1
      var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
      var page = pdfDoc.startPage(pageInfo)
      var canvas: Canvas = page.canvas

      val titlePaint = Paint().apply {
        color = Color.parseColor("#0F172A")
        textSize = 18f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      }
      val headerBadgePaint = Paint().apply {
        color = Color.parseColor("#1D4ED8")
        textSize = 14f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.RIGHT
      }
      val boldPaint = Paint().apply {
        color = Color.parseColor("#0F172A")
        textSize = 10f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      }
      val bodyPaint = Paint().apply {
        color = Color.parseColor("#334155")
        textSize = 9.5f
      }
      val mutedPaint = Paint().apply {
        color = Color.parseColor("#64748B")
        textSize = 8.5f
      }
      val linePaint = Paint().apply {
        color = Color.parseColor("#CBD5E1")
        strokeWidth = 1f
      }
      val tableHeaderBg = Paint().apply {
        color = Color.parseColor("#1D4ED8")
      }
      val whiteBold = Paint().apply {
        color = Color.WHITE
        textSize = 9.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      }

      val sym = profile?.currencySymbol ?: "₹"
      val bizName = profile?.businessName?.ifBlank { "Dukaan Stock" } ?: "Dukaan Stock"
      val docTitle = when (TxType.entries.firstOrNull { it.name == transaction.txType }) {
        TxType.PURCHASE -> "PURCHASE BILL"
        TxType.ESTIMATE -> "ESTIMATE / QUOTATION"
        TxType.SALE_ORDER -> "SALE ORDER"
        TxType.PURCHASE_ORDER -> "PURCHASE ORDER"
        TxType.CREDIT_NOTE -> "CREDIT NOTE"
        TxType.DEBIT_NOTE -> "DEBIT NOTE"
        TxType.DELIVERY_CHALLAN -> "DELIVERY CHALLAN"
        else -> "TAX INVOICE"
      }

      val margin = 36f
      val rightEdge = pageWidth - margin
      var y = 46f

      // Business Header
      canvas.drawText(bizName, margin, y, titlePaint)
      canvas.drawText(docTitle, rightEdge, y, headerBadgePaint)
      y += 16f

      if (!profile?.gstin.isNullOrBlank()) {
        canvas.drawText("GSTIN: ${profile?.gstin}", margin, y, bodyPaint)
        y += 13f
      }
      if (!profile?.phone.isNullOrBlank()) {
        canvas.drawText("Phone: ${profile?.phone}", margin, y, bodyPaint)
        y += 13f
      }
      if (!profile?.address.isNullOrBlank()) {
        val addr = listOfNotNull(
          profile?.address?.takeIf { it.isNotBlank() },
          profile?.stateName?.takeIf { it.isNotBlank() },
        ).joinToString(", ")
        canvas.drawText(addr.take(85), margin, y, mutedPaint)
        y += 14f
      }

      if (transaction.status == TxStatus.CANCELLED.name) {
        val cancelPaint = Paint().apply {
          color = Color.RED
          textSize = 12f
          typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("STATUS: CANCELLED", margin, y, cancelPaint)
        y += 16f
      }

      y += 4f
      canvas.drawLine(margin, y, rightEdge, y, linePaint)
      y += 16f

      // Party & Invoice Info
      canvas.drawText("BILL TO:", margin, y, mutedPaint)
      canvas.drawText("Ref No: ${transaction.refNumber}", rightEdge - 180f, y, boldPaint)
      y += 14f

      canvas.drawText(transaction.partyName.ifBlank { "Cash / Walk-in" }, margin, y, boldPaint)
      canvas.drawText("Date: ${formatDate(transaction.dateMillis)}", rightEdge - 180f, y, bodyPaint)
      y += 13f

      if (party?.gstin?.isNotBlank() == true) {
        canvas.drawText("Party GSTIN: ${party.gstin}", margin, y, bodyPaint)
      }
      if (transaction.dueDateMillis > 0L) {
        canvas.drawText("Due: ${formatDate(transaction.dueDateMillis)}", rightEdge - 180f, y, bodyPaint)
      }
      y += 13f

      if (party?.billingAddress?.isNotBlank() == true) {
        canvas.drawText("Billing: ${party.billingAddress.take(55)}", margin, y, mutedPaint)
      }
      if (transaction.placeOfSupply.isNotBlank()) {
        canvas.drawText("Supply: ${transaction.placeOfSupply}", rightEdge - 180f, y, mutedPaint)
      }
      y += 13f

      if (party?.shippingAddress?.isNotBlank() == true && party.shippingAddress != party.billingAddress) {
        canvas.drawText("Shipping: ${party.shippingAddress.take(55)}", margin, y, mutedPaint)
        y += 13f
      }

      y += 8f

      fun drawTableHeader() {
        canvas.drawRect(margin, y, rightEdge, y + 22f, tableHeaderBg)
        val ty = y + 14.5f
        canvas.drawText("#", margin + 6f, ty, whiteBold)
        canvas.drawText("Item / HSN", margin + 26f, ty, whiteBold)
        canvas.drawText("Qty", margin + 250f, ty, whiteBold)
        canvas.drawText("Rate", margin + 305f, ty, whiteBold)
        canvas.drawText("GST", margin + 380f, ty, whiteBold)
        canvas.drawText("Amount", rightEdge - 50f, ty, whiteBold)
        y += 26f
      }

      drawTableHeader()

      transaction.lineItems.forEachIndexed { idx, line ->
        if (y > pageHeight - 160) {
          pdfDoc.finishPage(page)
          pageNumber++
          pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
          page = pdfDoc.startPage(pageInfo)
          canvas = page.canvas
          y = 46f
          drawTableHeader()
        }
        val rowY = y + 11f
        canvas.drawText("${idx + 1}", margin + 6f, rowY, bodyPaint)
        val nameLabel = if (line.hsnSac.isNotBlank()) {
          "${line.itemName.take(28)} (${line.hsnSac})"
        } else {
          line.itemName.take(34)
        }
        canvas.drawText(nameLabel, margin + 26f, rowY, boldPaint)
        canvas.drawText("${formatQty(line.quantity)} ${line.unit}", margin + 250f, rowY, bodyPaint)
        canvas.drawText(formatCurrency(line.unitPrice, sym), margin + 305f, rowY, bodyPaint)
        canvas.drawText("${formatQty(line.taxRatePercent)}%", margin + 380f, rowY, bodyPaint)
        val amtText = formatCurrency(line.lineTotal, sym)
        val amtW = boldPaint.measureText(amtText)
        canvas.drawText(amtText, rightEdge - amtW - 4f, rowY, boldPaint)
        y += 20f
        canvas.drawLine(margin, y, rightEdge, y, linePaint)
      }

      y += 16f
      val summaryX = rightEdge - 200f
      canvas.drawText("Amount in Words:", margin, y, mutedPaint)
      canvas.drawText("Sub Total:", summaryX, y, bodyPaint)
      val subText = formatCurrency(transaction.subtotal, sym)
      canvas.drawText(subText, rightEdge - bodyPaint.measureText(subText), y, bodyPaint)
      y += 14f

      canvas.drawText(amountInWords(transaction.totalAmount, sym).take(48), margin, y, boldPaint)

      val totalDisc = transaction.discountAmount + transaction.billDiscountAmount
      if (totalDisc > 0.005) {
        canvas.drawText("Discount:", summaryX, y, bodyPaint)
        val dTxt = "-${formatCurrency(totalDisc, sym)}"
        canvas.drawText(dTxt, rightEdge - bodyPaint.measureText(dTxt), y, bodyPaint)
        y += 14f
      }
      if (transaction.extraChargesAmount > 0.005) {
        canvas.drawText(transaction.extraChargesLabel.ifBlank { "Extra Charges" } + ":", summaryX, y, bodyPaint)
        val exTxt = formatCurrency(transaction.extraChargesAmount, sym)
        canvas.drawText(exTxt, rightEdge - bodyPaint.measureText(exTxt), y, bodyPaint)
        y += 14f
      }

      if (transaction.taxAmount > 0.005) {
        if (transaction.isInterState || transaction.igstAmount > 0.005) {
          val igst = if (transaction.igstAmount > 0) transaction.igstAmount else transaction.taxAmount
          canvas.drawText("IGST:", summaryX, y, bodyPaint)
          val iTxt = formatCurrency(igst, sym)
          canvas.drawText(iTxt, rightEdge - bodyPaint.measureText(iTxt), y, bodyPaint)
          y += 14f
        } else {
          val cgst = if (transaction.cgstAmount > 0) transaction.cgstAmount else transaction.taxAmount / 2.0
          val sgst = if (transaction.sgstAmount > 0) transaction.sgstAmount else transaction.taxAmount - cgst
          canvas.drawText("CGST:", summaryX, y, bodyPaint)
          val cTxt = formatCurrency(cgst, sym)
          canvas.drawText(cTxt, rightEdge - bodyPaint.measureText(cTxt), y, bodyPaint)
          y += 14f
          canvas.drawText("SGST:", summaryX, y, bodyPaint)
          val sTxt = formatCurrency(sgst, sym)
          canvas.drawText(sTxt, rightEdge - bodyPaint.measureText(sTxt), y, bodyPaint)
          y += 14f
        }
      }

      if (kotlin.math.abs(transaction.roundOffAmount) > 0.001) {
        canvas.drawText("Round Off:", summaryX, y, bodyPaint)
        val rTxt = formatCurrency(transaction.roundOffAmount, sym)
        canvas.drawText(rTxt, rightEdge - bodyPaint.measureText(rTxt), y, bodyPaint)
        y += 14f
      }

      y += 4f
      canvas.drawRect(summaryX - 6f, y - 12f, rightEdge, y + 6f, tableHeaderBg)
      canvas.drawText("GRAND TOTAL", summaryX, y, whiteBold)
      val totTxt = formatCurrency(transaction.totalAmount, sym)
      canvas.drawText(totTxt, rightEdge - whiteBold.measureText(totTxt) - 4f, y, whiteBold)
      y += 20f

      canvas.drawText("Paid:", summaryX, y, bodyPaint)
      val pTxt = formatCurrency(transaction.paidAmount, sym)
      canvas.drawText(pTxt, rightEdge - bodyPaint.measureText(pTxt), y, bodyPaint)
      y += 14f

      canvas.drawText("Balance Due:", summaryX, y, boldPaint)
      val bTxt = formatCurrency(transaction.balanceAmount, sym)
      canvas.drawText(bTxt, rightEdge - boldPaint.measureText(bTxt), y, boldPaint)
      y += 26f

      if (!profile?.bankDetails.isNullOrBlank() || !profile?.upiId.isNullOrBlank()) {
        canvas.drawLine(margin, y, rightEdge, y, linePaint)
        y += 14f
        if (!profile?.upiId.isNullOrBlank()) {
          canvas.drawText("UPI Payment ID: ${profile?.upiId}", margin, y, boldPaint)
          y += 13f
        }
        if (!profile?.bankDetails.isNullOrBlank()) {
          canvas.drawText("Bank Details: ${profile?.bankDetails?.take(85)}", margin, y, bodyPaint)
          y += 14f
        }
      }

      if (!profile?.invoiceTerms.isNullOrBlank()) {
        y += 6f
        canvas.drawText("Terms: ${profile?.invoiceTerms?.take(90)}", margin, y, mutedPaint)
      }

      canvas.drawText("Authorized Signatory", rightEdge - 95f, pageHeight - 48f, boldPaint)

      pdfDoc.finishPage(page)
      FileOutputStream(outFile).use { fos -> pdfDoc.writeTo(fos) }
      pdfDoc.close()

      val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        outFile,
      )
      val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "$docTitle ${transaction.refNumber}")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      context.startActivity(Intent.createChooser(shareIntent, "Share Invoice PDF"))
      true
    } catch (_: Exception) {
      false
    }
  }

  fun shareCsvExport(context: Context, fileNamePrefix: String, csvContent: String): Boolean {
    return try {
      val dir = File(context.cacheDir, "exports").apply { mkdirs() }
      val file = File(dir, "${fileNamePrefix}_${System.currentTimeMillis()}.csv")
      file.writeText(csvContent)
      val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
      val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, fileNamePrefix)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      context.startActivity(Intent.createChooser(intent, "Export CSV Report"))
      true
    } catch (_: Exception) {
      false
    }
  }
}
