package com.example.ui.util

import com.example.data.model.BusinessProfile
import com.example.data.model.BusinessTransaction
import com.example.data.model.CommissionSplit
import com.example.data.model.CommissionStatus
import com.example.data.model.TxType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatUtilsTest {

  @Test
  fun indianCurrencyFormatting_usesLakhsAndCroresGrouping() {
    assertEquals("₹1,23,456.78", formatCurrency(123456.78, "₹"))
    assertEquals("₹1,23,45,678.90", formatCurrency(12345678.90, "₹"))
    assertEquals("-₹500.00", formatCurrency(-500.0, "₹"))
    assertEquals("One Lakh Twenty Three Thousand Four Hundred and Fifty Six Rupees Only", amountInWords(123456.0, "₹"))
  }

  @Test
  fun customerShareText_strictlyExcludesAgentAndCommissionDetails() {
    val tx = BusinessTransaction(
      txType = TxType.SALE.name,
      refNumber = "INV/26-27/0001",
      dateMillis = 1_750_000_000_000L,
      partyName = "Verma Builders",
      subtotal = 10000.0,
      taxAmount = 1800.0,
      cgstAmount = 900.0,
      sgstAmount = 900.0,
      totalAmount = 11800.0,
      paidAmount = 5000.0,
      balanceAmount = 6800.0,
      agentId = "agent_1",
      agentName = "Ramesh Mistri",
      commissionStatus = CommissionStatus.ACCRUED.name,
      commissionTotalAmount = 500.0,
      commissionAccruedAmount = 500.0,
      commissionSplits = listOf(
        CommissionSplit(agentId = "agent_1", agentName = "Ramesh Mistri", totalCommission = 500.0, accruedCommission = 500.0)
      ),
    )
    val profile = BusinessProfile(businessName = "Gupta Hardware", upiId = "gupta@okaxis")
    val shared = buildShareInvoiceText(tx, profile)

    assertTrue(shared.contains("Gupta Hardware"))
    assertTrue(shared.contains("Verma Builders"))
    assertTrue(shared.contains("CGST:"))
    assertTrue(shared.contains("UPI ID: gupta@okaxis"))
    assertFalse(shared.contains("Ramesh Mistri"))
    assertFalse(shared.contains("Commission", ignoreCase = true))
  }
}
