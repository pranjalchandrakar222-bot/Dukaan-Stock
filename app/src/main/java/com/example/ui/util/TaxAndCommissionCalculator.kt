package com.example.ui.util

import com.example.data.model.Agent
import com.example.data.model.BusinessProfile
import com.example.data.model.CommissionBasis
import com.example.data.model.CommissionSplit
import com.example.data.model.CommissionStatus
import com.example.data.model.CommissionTrigger
import com.example.data.model.InventoryItem
import com.example.data.model.LineItem
import com.example.data.model.Party
import com.example.data.model.TxType
import kotlin.math.roundToLong

data class LineCalculationInput(
  val itemId: String = "",
  val itemName: String = "",
  val hsnSac: String = "",
  val unit: String = "PCS",
  val quantity: Double = 1.0,
  val unitPrice: Double = 0.0,
  val costPrice: Double = 0.0,
  val priceIncludesTax: Boolean = false,
  val discountPercent: Double = 0.0,
  val taxRatePercent: Double = 0.0,
  val itemCommissionPercent: Double = 0.0,
)

data class AgentSplitInput(
  val agent: Agent,
  val sharePercent: Double = 100.0,
  val overrideRatePercent: Double? = null,
)

data class InvoiceCalculationResult(
  val lineItems: List<LineItem>,
  val subtotal: Double,
  val lineDiscountTotal: Double,
  val billDiscountAmount: Double,
  val extraChargesAmount: Double,
  val cgstAmount: Double,
  val sgstAmount: Double,
  val igstAmount: Double,
  val taxAmount: Double,
  val roundOffAmount: Double,
  val totalAmount: Double,
  val commissionTotalAmount: Double,
  val commissionAccruedAmount: Double,
  val commissionStatus: CommissionStatus,
  val commissionSplits: List<CommissionSplit>,
)

object TaxAndCommissionCalculator {

  private val gstStateCodeMap = mapOf(
    "01" to "Jammu and Kashmir",
    "02" to "Himachal Pradesh",
    "03" to "Punjab",
    "04" to "Chandigarh",
    "05" to "Uttarakhand",
    "06" to "Haryana",
    "07" to "Delhi",
    "08" to "Rajasthan",
    "09" to "Uttar Pradesh",
    "10" to "Bihar",
    "11" to "Sikkim",
    "12" to "Arunachal Pradesh",
    "13" to "Nagaland",
    "14" to "Manipur",
    "15" to "Mizoram",
    "16" to "Tripura",
    "17" to "Meghalaya",
    "18" to "Assam",
    "19" to "West Bengal",
    "20" to "Jharkhand",
    "21" to "Odisha",
    "22" to "Chhattisgarh",
    "23" to "Madhya Pradesh",
    "24" to "Gujarat",
    "26" to "Dadra and Nagar Haveli and Daman and Diu",
    "27" to "Maharashtra",
    "29" to "Karnataka",
    "30" to "Goa",
    "31" to "Lakshadweep",
    "32" to "Kerala",
    "33" to "Tamil Nadu",
    "34" to "Puducherry",
    "35" to "Andaman and Nicobar Islands",
    "36" to "Telangana",
    "37" to "Andhra Pradesh",
    "38" to "Ladakh",
  )

  val indianStates: List<String> = gstStateCodeMap.values.sorted()

  fun stateFromGstin(gstin: String): String {
    val clean = gstin.trim()
    if (clean.length >= 2) {
      val code = clean.substring(0, 2)
      return gstStateCodeMap[code].orEmpty()
    }
    return ""
  }

  fun detectInterState(
    businessState: String,
    businessGstin: String,
    partyStateOrPlaceOfSupply: String,
    partyGstin: String,
  ): Boolean {
    val bizState = businessState.trim().ifBlank { stateFromGstin(businessGstin) }
    val partyState = partyStateOrPlaceOfSupply.trim().ifBlank { stateFromGstin(partyGstin) }
    if (bizState.isBlank() || partyState.isBlank()) return false
    return !bizState.equals(partyState, ignoreCase = true)
  }

  fun formatDocumentRef(
    txType: TxType,
    profile: BusinessProfile?,
    dateMillis: Long = System.currentTimeMillis(),
  ): String {
    val prefix = when (txType) {
      TxType.SALE -> profile?.invoicePrefix?.ifBlank { "INV-" } ?: "INV-"
      TxType.PURCHASE -> "PUR-"
      TxType.ESTIMATE -> "EST-"
      TxType.SALE_ORDER -> "SO-"
      TxType.PURCHASE_ORDER -> "PO-"
      TxType.CREDIT_NOTE -> "CN-"
      TxType.DEBIT_NOTE -> "DN-"
      TxType.DELIVERY_CHALLAN -> "DC-"
      TxType.PAYMENT_IN -> "RCPT-"
      TxType.PAYMENT_OUT -> "PAY-"
      TxType.EXPENSE -> "EXP-"
      TxType.COMMISSION_PAYOUT -> "COMM-"
      TxType.CASH_TRANSFER -> "XFER-"
      TxType.CASH_ADJUSTMENT -> "ADJ-"
      TxType.STOCK_ADJUSTMENT -> "STKA-"
    }
    val nextNum = when (txType) {
      TxType.SALE -> profile?.nextInvoiceNumber ?: 1L
      TxType.PURCHASE -> profile?.nextPurchaseNumber ?: 1L
      TxType.ESTIMATE -> profile?.nextEstimateNumber ?: 1L
      TxType.SALE_ORDER, TxType.PURCHASE_ORDER -> profile?.nextSaleOrderNumber ?: 1L
      TxType.DELIVERY_CHALLAN -> profile?.nextChallanNumber ?: 1L
      TxType.CREDIT_NOTE -> profile?.nextCreditNoteNumber ?: 1L
      TxType.DEBIT_NOTE -> profile?.nextDebitNoteNumber ?: 1L
      TxType.PAYMENT_IN, TxType.PAYMENT_OUT -> profile?.nextPaymentNumber ?: 1L
      TxType.EXPENSE -> profile?.nextExpenseNumber ?: 1L
      TxType.COMMISSION_PAYOUT -> profile?.nextCommissionPayoutNumber ?: 1L
      TxType.CASH_TRANSFER, TxType.CASH_ADJUSTMENT, TxType.STOCK_ADJUSTMENT -> System.currentTimeMillis() % 10000L
    }.coerceAtLeast(1L)

    val padded = nextNum.toString().padStart(4, '0')
    return if (profile?.useFinancialYearPrefix != false && txType in setOf(
        TxType.SALE,
        TxType.PURCHASE,
        TxType.ESTIMATE,
        TxType.SALE_ORDER,
        TxType.DELIVERY_CHALLAN,
        TxType.CREDIT_NOTE,
        TxType.DEBIT_NOTE,
        TxType.COMMISSION_PAYOUT,
      )
    ) {
      val fy = MoneyUtils.financialYearTag(dateMillis)
      "${prefix.trimEnd('-', '/')}/$fy/$padded"
    } else {
      "$prefix$padded"
    }
  }

  /**
   * Computes exact line-level GST (supporting inclusive tax extraction, CGST+SGST vs IGST),
   * proportional bill-level discount, extra charges, round-off, and Agent/Mistri commission splits.
   */
  fun calculateInvoice(
    lines: List<LineCalculationInput>,
    isInterState: Boolean,
    billDiscountAmount: Double = 0.0,
    extraChargesAmount: Double = 0.0,
    roundOffEnabled: Boolean = true,
    paidAmount: Double = 0.0,
    fullyPaid: Boolean = true,
    agentSplits: List<AgentSplitInput> = emptyList(),
    commissionBasis: CommissionBasis = CommissionBasis.TAXABLE,
    commissionTrigger: CommissionTrigger = CommissionTrigger.ON_INVOICE,
    manualCommissionRateOverride: Double? = null,
    commissionApplies: Boolean = true,
  ): InvoiceCalculationResult {
    // Pass 1: Extract base exclusive-of-tax price & line discount per row
    data class RowBase(
      val input: LineCalculationInput,
      val baseExclSubtotal: Double,
      val lineDiscExcl: Double,
      val netTaxableBeforeBillDisc: Double,
    )

    val rowBases = lines.map { row ->
      val qty = row.quantity.coerceAtLeast(0.0)
      val price = row.unitPrice.coerceAtLeast(0.0)
      val discPct = row.discountPercent.coerceIn(0.0, 100.0)
      val taxRate = row.taxRatePercent.coerceIn(0.0, 100.0)

      val unitExclTax = if (row.priceIncludesTax && taxRate > 0.0) {
        price / (1.0 + taxRate / 100.0)
      } else {
        price
      }
      val baseExcl = qty * unitExclTax
      val lineDisc = baseExcl * (discPct / 100.0)
      val netTaxable = (baseExcl - lineDisc).coerceAtLeast(0.0)
      RowBase(row, baseExcl, lineDisc, netTaxable)
    }

    val subtotalRaw = rowBases.sumOf { it.baseExclSubtotal }
    val lineDiscRaw = rowBases.sumOf { it.lineDiscExcl }
    val totalTaxableBeforeBillDisc = rowBases.sumOf { it.netTaxableBeforeBillDisc }
    val cleanBillDisc = billDiscountAmount.coerceIn(0.0, totalTaxableBeforeBillDisc)
    val billDiscScale = if (totalTaxableBeforeBillDisc > 0.0001) {
      ((totalTaxableBeforeBillDisc - cleanBillDisc) / totalTaxableBeforeBillDisc).coerceIn(0.0, 1.0)
    } else {
      1.0
    }

    val primaryAgent = agentSplits.firstOrNull()?.agent
    val computedLines = rowBases.map { rb ->
      val taxable = MoneyUtils.round2(rb.netTaxableBeforeBillDisc * billDiscScale)
      val taxRate = rb.input.taxRatePercent.coerceIn(0.0, 100.0)
      val lineTax = MoneyUtils.round2(taxable * (taxRate / 100.0))
      val cgst = if (isInterState) 0.0 else MoneyUtils.round2(lineTax / 2.0)
      val sgst = if (isInterState) 0.0 else MoneyUtils.round2(lineTax - cgst)
      val igst = if (isInterState) lineTax else 0.0
      val lineTotal = MoneyUtils.round2(taxable + lineTax)

      // Priority: invoice manual override > item.commissionPercent > primaryAgent.defaultPercent
      val effectiveRate = when {
        !commissionApplies || agentSplits.isEmpty() -> 0.0
        manualCommissionRateOverride != null -> manualCommissionRateOverride.coerceAtLeast(0.0)
        rb.input.itemCommissionPercent > 0.0 -> rb.input.itemCommissionPercent
        primaryAgent != null -> primaryAgent.defaultPercent
        else -> 0.0
      }

      val lineCommission = if (!commissionApplies || agentSplits.isEmpty()) {
        0.0
      } else {
        when (commissionBasis) {
          CommissionBasis.TAXABLE -> MoneyUtils.round2(taxable * effectiveRate / 100.0)
          CommissionBasis.TOTAL -> MoneyUtils.round2(lineTotal * effectiveRate / 100.0)
          CommissionBasis.PROFIT -> {
            val profit = (taxable - (rb.input.costPrice * rb.input.quantity)).coerceAtLeast(0.0)
            MoneyUtils.round2(profit * effectiveRate / 100.0)
          }
          CommissionBasis.PER_UNIT -> {
            val perUnit = manualCommissionRateOverride ?: primaryAgent?.defaultPerUnitAmount ?: 0.0
            MoneyUtils.round2(rb.input.quantity * perUnit)
          }
        }
      }

      LineItem(
        itemId = rb.input.itemId,
        itemName = rb.input.itemName,
        hsnSac = rb.input.hsnSac,
        unit = rb.input.unit.ifBlank { "PCS" },
        quantity = rb.input.quantity,
        unitPrice = rb.input.unitPrice,
        costPrice = rb.input.costPrice,
        priceIncludesTax = rb.input.priceIncludesTax,
        discountPercent = rb.input.discountPercent,
        taxRatePercent = rb.input.taxRatePercent,
        cgstAmount = cgst,
        sgstAmount = sgst,
        igstAmount = igst,
        taxAmount = lineTax,
        lineTotal = lineTotal,
        commissionPercent = effectiveRate,
        commissionAmount = lineCommission,
      )
    }

    val subtotal = MoneyUtils.round2(subtotalRaw)
    val lineDiscountTotal = MoneyUtils.round2(lineDiscRaw)
    val cgstSum = MoneyUtils.round2(computedLines.sumOf { it.cgstAmount })
    val sgstSum = MoneyUtils.round2(computedLines.sumOf { it.sgstAmount })
    val igstSum = MoneyUtils.round2(computedLines.sumOf { it.igstAmount })
    val taxSum = MoneyUtils.round2(computedLines.sumOf { it.taxAmount })
    val cleanExtraCharges = extraChargesAmount.coerceAtLeast(0.0)

    val preRoundTotal = MoneyUtils.round2(
      totalTaxableBeforeBillDisc - cleanBillDisc + taxSum + cleanExtraCharges
    )
    val grandTotal = if (roundOffEnabled) preRoundTotal.roundToLong().toDouble() else preRoundTotal
    val roundOff = MoneyUtils.round2(grandTotal - preRoundTotal)

    val effectivePaid = if (fullyPaid) grandTotal else paidAmount.coerceIn(0.0, grandTotal)
    val paidRatio = if (grandTotal > 0.005) (effectivePaid / grandTotal).coerceIn(0.0, 1.0) else 0.0

    val totalCommission = if (!commissionApplies || agentSplits.isEmpty()) {
      0.0
    } else {
      MoneyUtils.round2(computedLines.sumOf { it.commissionAmount })
    }

    val accruedCommission = when {
      !commissionApplies || agentSplits.isEmpty() || totalCommission <= 0.001 -> 0.0
      commissionTrigger == CommissionTrigger.ON_INVOICE -> totalCommission
      else -> MoneyUtils.round2(totalCommission * paidRatio)
    }

    val commStatus = when {
      !commissionApplies || agentSplits.isEmpty() || totalCommission <= 0.001 -> CommissionStatus.NONE
      commissionTrigger == CommissionTrigger.ON_INVOICE -> CommissionStatus.ACCRUED
      accruedCommission >= totalCommission - 0.01 -> CommissionStatus.ACCRUED
      accruedCommission > 0.01 -> CommissionStatus.PARTIAL
      else -> CommissionStatus.PENDING_COLLECTION
    }

    // Split commission across 1 or more assigned agents (normalized so shares sum to 100%)
    val splitTotalWeight = agentSplits.sumOf { it.sharePercent.coerceAtLeast(0.0) }
    val splitResults = if (!commissionApplies || agentSplits.isEmpty() || totalCommission <= 0.001) {
      emptyList()
    } else {
      var remainingTotal = totalCommission
      var remainingAccrued = accruedCommission
      agentSplits.mapIndexed { idx, split ->
        val isLast = idx == agentSplits.lastIndex
        val weight = if (splitTotalWeight > 0.0) split.sharePercent.coerceAtLeast(0.0) / splitTotalWeight else (1.0 / agentSplits.size)
        val shareTotal = if (isLast) MoneyUtils.round2(remainingTotal) else MoneyUtils.round2(totalCommission * weight)
        val shareAccrued = if (isLast) MoneyUtils.round2(remainingAccrued) else MoneyUtils.round2(accruedCommission * weight)
        remainingTotal -= shareTotal
        remainingAccrued -= shareAccrued

        val avgRate = if (computedLines.isNotEmpty()) {
          MoneyUtils.round2(computedLines.map { it.commissionPercent }.average() * weight)
        } else {
          split.overrideRatePercent ?: split.agent.defaultPercent
        }
        CommissionSplit(
          agentId = split.agent.id,
          agentName = split.agent.name,
          agentType = split.agent.agentType,
          sharePercent = MoneyUtils.round2(weight * 100.0),
          ratePercent = avgRate,
          totalCommission = shareTotal,
          accruedCommission = shareAccrued,
        )
      }
    }

    return InvoiceCalculationResult(
      lineItems = computedLines,
      subtotal = subtotal,
      lineDiscountTotal = lineDiscountTotal,
      billDiscountAmount = MoneyUtils.round2(cleanBillDisc),
      extraChargesAmount = MoneyUtils.round2(cleanExtraCharges),
      cgstAmount = cgstSum,
      sgstAmount = sgstSum,
      igstAmount = igstSum,
      taxAmount = taxSum,
      roundOffAmount = roundOff,
      totalAmount = grandTotal,
      commissionTotalAmount = totalCommission,
      commissionAccruedAmount = accruedCommission,
      commissionStatus = commStatus,
      commissionSplits = splitResults,
    )
  }

  /**
   * Resolves effective unit price for a selected inventory item according to retail/wholesale mode.
   */
  fun resolveItemUnitPrice(
    item: InventoryItem,
    txType: TxType,
    useWholesalePrice: Boolean,
  ): Double {
    return when {
      txType == TxType.PURCHASE || txType == TxType.PURCHASE_ORDER || txType == TxType.DEBIT_NOTE ->
        item.purchasePrice
      useWholesalePrice && item.wholesalePrice > 0.0 -> item.wholesalePrice
      else -> item.salePrice
    }
  }
}
