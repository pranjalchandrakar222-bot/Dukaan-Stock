package com.example.ui.util

import com.example.data.model.Agent
import com.example.data.model.AgentType
import com.example.data.model.BusinessProfile
import com.example.data.model.CommissionBasis
import com.example.data.model.CommissionStatus
import com.example.data.model.CommissionTrigger
import com.example.data.model.InventoryItem
import com.example.data.model.TxType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaxAndCommissionCalculatorTest {

  @Test
  fun inclusiveTaxExtraction_intraState_computesExactTaxableAndCgstSgst() {
    val result =
      TaxAndCommissionCalculator.calculateInvoice(
        lines = listOf(
          LineCalculationInput(
            itemId = "item1",
            itemName = "Tiles Box",
            quantity = 2.0,
            unitPrice = 118.0,
            priceIncludesTax = true,
            discountPercent = 0.0,
            taxRatePercent = 18.0,
          )
        ),
        isInterState = false,
        roundOffEnabled = false,
      )

    assertEquals(200.0, result.subtotal, 0.01)
    assertEquals(18.0, result.cgstAmount, 0.01)
    assertEquals(18.0, result.sgstAmount, 0.01)
    assertEquals(0.0, result.igstAmount, 0.01)
    assertEquals(36.0, result.taxAmount, 0.01)
    assertEquals(236.0, result.totalAmount, 0.01)
  }

  @Test
  fun interStateSupply_computesIgstAndZeroCgstSgst() {
    assertTrue(
      TaxAndCommissionCalculator.detectInterState(
        businessState = "Maharashtra",
        businessGstin = "27ABCDE1234F1Z5",
        partyStateOrPlaceOfSupply = "",
        partyGstin = "07ABCDE1234F1Z2",
      )
    )
    assertFalse(
      TaxAndCommissionCalculator.detectInterState(
        businessState = "Maharashtra",
        businessGstin = "",
        partyStateOrPlaceOfSupply = "Maharashtra",
        partyGstin = "",
      )
    )

    val result =
      TaxAndCommissionCalculator.calculateInvoice(
        lines = listOf(
          LineCalculationInput(
            itemId = "item1",
            itemName = "Granite Slab",
            quantity = 1.0,
            unitPrice = 1000.0,
            priceIncludesTax = false,
            taxRatePercent = 18.0,
          )
        ),
        isInterState = true,
        billDiscountAmount = 100.0,
        extraChargesAmount = 50.0,
        roundOffEnabled = false,
      )

    // Taxable after bill discount = 900 -> 18% IGST = 162 -> +50 extra charges = 1112
    assertEquals(1000.0, result.subtotal, 0.01)
    assertEquals(100.0, result.billDiscountAmount, 0.01)
    assertEquals(0.0, result.cgstAmount, 0.01)
    assertEquals(0.0, result.sgstAmount, 0.01)
    assertEquals(162.0, result.igstAmount, 0.01)
    assertEquals(1112.0, result.totalAmount, 0.01)
  }

  @Test
  fun commissionCalculation_multiAgentSplitAndOnCollectionPartialAccrual() {
    val mistri = Agent(
      id = "a1",
      name = "Ramesh Mistri",
      agentType = AgentType.MISTRI.name,
      defaultPercent = 5.0,
      defaultBasis = CommissionBasis.TAXABLE.name,
      defaultTrigger = CommissionTrigger.ON_COLLECTION.name,
    )
    val broker = Agent(
      id = "a2",
      name = "Suresh Broker",
      agentType = AgentType.AGENT.name,
      defaultPercent = 5.0,
    )

    // Total taxable = 1000, GST 18% = 180, Total = 1180.
    // Commission rate on item = 10% -> totalCommission = 100.
    // Paid = 590 (50% of 1180) -> ON_COLLECTION accruedCommission = 50.
    // Split 60% / 40% -> Mistri accrued = 30, Broker accrued = 20.
    val res =
      TaxAndCommissionCalculator.calculateInvoice(
        lines = listOf(
          LineCalculationInput(
            itemId = "i1",
            itemName = "Italian Marble",
            quantity = 10.0,
            unitPrice = 100.0,
            taxRatePercent = 18.0,
            itemCommissionPercent = 10.0,
          )
        ),
        isInterState = false,
        roundOffEnabled = false,
        paidAmount = 590.0,
        fullyPaid = false,
        agentSplits = listOf(
          AgentSplitInput(mistri, sharePercent = 60.0),
          AgentSplitInput(broker, sharePercent = 40.0),
        ),
        commissionBasis = CommissionBasis.TAXABLE,
        commissionTrigger = CommissionTrigger.ON_COLLECTION,
      )

    assertEquals(100.0, res.commissionTotalAmount, 0.01)
    assertEquals(50.0, res.commissionAccruedAmount, 0.01)
    assertEquals(CommissionStatus.PARTIAL, res.commissionStatus)
    assertEquals(2, res.commissionSplits.size)
    assertEquals(60.0, res.commissionSplits[0].totalCommission, 0.01)
    assertEquals(30.0, res.commissionSplits[0].accruedCommission, 0.01)
    assertEquals(40.0, res.commissionSplits[1].totalCommission, 0.01)
    assertEquals(20.0, res.commissionSplits[1].accruedCommission, 0.01)
  }

  @Test
  fun profitAndPerUnitCommission_computesAccurately() {
    val contractor = Agent(
      id = "c1",
      name = "Mohan Contractor",
      defaultPercent = 10.0,
      defaultPerUnitAmount = 25.0,
    )

    // Profit = 1000 - (700 * 1) = 300 -> 10% = 30
    val profitRes = TaxAndCommissionCalculator.calculateInvoice(
      lines = listOf(
        LineCalculationInput(
          itemId = "i1",
          itemName = "Door Lock",
          quantity = 1.0,
          unitPrice = 1000.0,
          costPrice = 700.0,
        )
      ),
      isInterState = false,
      agentSplits = listOf(AgentSplitInput(contractor)),
      commissionBasis = CommissionBasis.PROFIT,
      commissionTrigger = CommissionTrigger.ON_INVOICE,
    )
    assertEquals(30.0, profitRes.commissionTotalAmount, 0.01)
    assertEquals(30.0, profitRes.commissionAccruedAmount, 0.01)

    // Per-unit = 4 units * 25 = 100
    val unitRes = TaxAndCommissionCalculator.calculateInvoice(
      lines = listOf(
        LineCalculationInput(
          itemId = "i2",
          itemName = "Cement Bag",
          quantity = 4.0,
          unitPrice = 400.0,
        )
      ),
      isInterState = false,
      agentSplits = listOf(AgentSplitInput(contractor)),
      commissionBasis = CommissionBasis.PER_UNIT,
      commissionTrigger = CommissionTrigger.ON_INVOICE,
    )
    assertEquals(100.0, unitRes.commissionTotalAmount, 0.01)
  }

  @Test
  fun resolveItemUnitPrice_respectsWholesaleAndPurchase() {
    val item = InventoryItem(
      salePrice = 100.0,
      wholesalePrice = 85.0,
      purchasePrice = 60.0,
    )
    assertEquals(100.0, TaxAndCommissionCalculator.resolveItemUnitPrice(item, TxType.SALE, false), 0.01)
    assertEquals(85.0, TaxAndCommissionCalculator.resolveItemUnitPrice(item, TxType.SALE, true), 0.01)
    assertEquals(60.0, TaxAndCommissionCalculator.resolveItemUnitPrice(item, TxType.PURCHASE, true), 0.01)
  }

  @Test
  fun formatDocumentRef_supportsFyAndSequentialPrefix() {
    val profile = BusinessProfile(
      invoicePrefix = "INV-",
      nextInvoiceNumber = 12L,
      useFinancialYearPrefix = false,
    )
    assertEquals("INV-0012", TaxAndCommissionCalculator.formatDocumentRef(TxType.SALE, profile))
  }
}
