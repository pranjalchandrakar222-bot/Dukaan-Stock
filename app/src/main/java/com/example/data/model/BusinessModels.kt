package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

enum class ThemeStyle(val label: String) {
  DYNAMIC("Dynamic Color"),
  COBALT("Stitch Cobalt"),
  EMERALD("Dukaan Emerald"),
  TERRACOTTA("Amber Rust"),
  SLATE("Executive Slate"),
}

enum class PartyType(val label: String) {
  CUSTOMER("Customer"),
  SUPPLIER("Supplier"),
  BOTH("Customer & Supplier"),
}

enum class ItemType(val label: String) {
  PRODUCT("Product"),
  SERVICE("Service"),
}

enum class AccountType(val label: String) {
  CASH("Cash Register"),
  BANK("Bank Account"),
  UPI("UPI / Wallet"),
}

enum class PaymentMode(val label: String) {
  CASH("Cash"),
  UPI("UPI"),
  BANK_TRANSFER("Bank"),
  CARD("Card"),
  CHEQUE("Cheque"),
}

enum class TxType(val label: String, val shortLabel: String) {
  SALE("Sale Invoice", "Sale"),
  PAYMENT_IN("Payment-In", "Pay-In"),
  ESTIMATE("Estimate / Quotation", "Estimate"),
  SALE_ORDER("Sale Order", "Sale Order"),
  DELIVERY_CHALLAN("Delivery Challan", "Challan"),
  CREDIT_NOTE("Credit Note / Return", "Cr. Note"),
  PURCHASE("Purchase Bill", "Purchase"),
  PAYMENT_OUT("Payment-Out", "Pay-Out"),
  EXPENSE("Business Expense", "Expense"),
  PURCHASE_ORDER("Purchase Order", "Purch. Order"),
  DEBIT_NOTE("Debit Note", "Dr. Note"),
  COMMISSION_PAYOUT("Commission Payout", "Comm. Pay"),
  CASH_TRANSFER("Cash / Bank Transfer", "Transfer"),
  CASH_ADJUSTMENT("Account Adjustment", "Acc. Adj"),
  STOCK_ADJUSTMENT("Stock Adjustment", "Stock Adj"),
}

enum class TxStatus(val label: String) {
  PAID("Paid"),
  PARTIAL("Partial"),
  UNPAID("Unpaid"),
  OPEN("Open"),
  CONVERTED("Converted"),
  CANCELLED("Cancelled"),
}

enum class AgentType(val label: String) {
  AGENT("Agent / Broker"),
  MISTRI("Mistri / Technician"),
  SALESMAN("Field Salesman"),
  ARCHITECT("Architect / Designer"),
  CONTRACTOR("Contractor"),
  REFERRAL("Referral Partner"),
}

enum class CommissionBasis(val label: String) {
  TAXABLE("Net Taxable Value"),
  TOTAL("Invoice Total"),
  PROFIT("Net Profit Margin"),
  PER_UNIT("Fixed per Unit"),
}

enum class CommissionTrigger(val label: String) {
  ON_INVOICE("On Invoice Creation"),
  ON_COLLECTION("On Payment Collection"),
}

enum class CommissionStatus(val label: String) {
  NONE("None"),
  ACCRUED("Accrued"),
  PENDING_COLLECTION("Pending Collection"),
  PARTIAL("Partially Accrued"),
  PAID("Paid"),
  REVERSED("Reversed"),
}

@IgnoreExtraProperties
data class BusinessProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val businessName: String = "My Dukaan Store",
  val phone: String = "",
  val email: String = "",
  val gstin: String = "",
  val address: String = "",
  val stateName: String = "",
  val currencySymbol: String = "₹",
  val invoicePrefix: String = "INV-",
  val nextInvoiceNumber: Long = 1L,
  val nextPurchaseNumber: Long = 1L,
  val nextEstimateNumber: Long = 1L,
  val nextSaleOrderNumber: Long = 1L,
  val nextChallanNumber: Long = 1L,
  val nextCreditNoteNumber: Long = 1L,
  val nextDebitNoteNumber: Long = 1L,
  val nextPaymentNumber: Long = 1L,
  val nextExpenseNumber: Long = 1L,
  val nextCommissionPayoutNumber: Long = 1L,
  val useFinancialYearPrefix: Boolean = true,
  val upiId: String = "",
  val bankDetails: String = "",
  val invoiceTerms: String = "Thanks for doing business with us!",
  val themeStyle: String = ThemeStyle.COBALT.name,
  val stopNegativeStock: Boolean = false,
  val roundOffTotal: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

fun BusinessProfile.themeStyleEnum(): ThemeStyle =
  ThemeStyle.entries.firstOrNull { it.name == themeStyle } ?: ThemeStyle.COBALT

@IgnoreExtraProperties
data class Party(
  @DocumentId val id: String = "",
  val userId: String = "",
  val name: String = "",
  val partyType: String = PartyType.CUSTOMER.name,
  val phone: String = "",
  val email: String = "",
  val gstin: String = "",
  val stateName: String = "",
  val billingAddress: String = "",
  val shippingAddress: String = "",
  val openingBalance: Double = 0.0,
  val currentBalance: Double = 0.0, // Positive = Receivable (customer owes us), Negative = Payable
  val defaultAgentId: String = "",
  @get:PropertyName("isActive") @field:PropertyName("isActive")
  val isActive: Boolean = true,
  val notes: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

fun Party.partyTypeEnum(): PartyType =
  PartyType.entries.firstOrNull { it.name == partyType } ?: PartyType.CUSTOMER

@IgnoreExtraProperties
data class InventoryItem(
  @DocumentId val id: String = "",
  val userId: String = "",
  val name: String = "",
  val itemType: String = ItemType.PRODUCT.name,
  val hsnSac: String = "",
  val itemCode: String = "",
  val category: String = "",
  val unit: String = "PCS",
  val salePrice: Double = 0.0,
  val salePriceWithTax: Boolean = false,
  val wholesalePrice: Double = 0.0,
  val purchasePrice: Double = 0.0,
  val discountPercent: Double = 0.0,
  val taxRatePercent: Double = 0.0,
  val commissionPercent: Double = 0.0,
  val openingStock: Double = 0.0,
  val currentStock: Double = 0.0,
  val minStockLevel: Double = 5.0,
  val location: String = "",
  @get:PropertyName("isActive") @field:PropertyName("isActive")
  val isActive: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

fun InventoryItem.itemTypeEnum(): ItemType =
  ItemType.entries.firstOrNull { it.name == itemType } ?: ItemType.PRODUCT

fun InventoryItem.isLowStock(): Boolean =
  isActive && itemType == ItemType.PRODUCT.name && minStockLevel > 0.0 && currentStock <= minStockLevel

@IgnoreExtraProperties
data class Agent(
  @DocumentId val id: String = "",
  val userId: String = "",
  val name: String = "",
  val agentType: String = AgentType.MISTRI.name,
  val phone: String = "",
  val cityArea: String = "",
  val upiId: String = "",
  val bankDetails: String = "",
  val defaultPercent: Double = 2.0,
  val defaultPerUnitAmount: Double = 0.0,
  val defaultBasis: String = CommissionBasis.TAXABLE.name,
  val defaultTrigger: String = CommissionTrigger.ON_INVOICE.name,
  val openingBalance: Double = 0.0,
  val currentBalance: Double = 0.0, // Positive = Commission payable to agent/mistri
  val totalEarned: Double = 0.0,
  val totalPaid: Double = 0.0,
  @get:PropertyName("isActive") @field:PropertyName("isActive")
  val isActive: Boolean = true,
  val notes: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

fun Agent.agentTypeEnum(): AgentType =
  AgentType.entries.firstOrNull { it.name == agentType } ?: AgentType.MISTRI

fun Agent.defaultBasisEnum(): CommissionBasis =
  CommissionBasis.entries.firstOrNull { it.name == defaultBasis } ?: CommissionBasis.TAXABLE

fun Agent.defaultTriggerEnum(): CommissionTrigger =
  CommissionTrigger.entries.firstOrNull { it.name == defaultTrigger } ?: CommissionTrigger.ON_INVOICE

@IgnoreExtraProperties
data class CommissionSplit(
  val agentId: String = "",
  val agentName: String = "",
  val agentType: String = AgentType.MISTRI.name,
  val sharePercent: Double = 100.0, // Share of total invoice commission
  val ratePercent: Double = 0.0, // Effective rate for this agent
  val totalCommission: Double = 0.0,
  val accruedCommission: Double = 0.0,
)

@IgnoreExtraProperties
data class LineItem(
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
  val cgstAmount: Double = 0.0,
  val sgstAmount: Double = 0.0,
  val igstAmount: Double = 0.0,
  val taxAmount: Double = 0.0,
  val lineTotal: Double = 0.0,
  val commissionPercent: Double = 0.0,
  val commissionAmount: Double = 0.0,
)

@IgnoreExtraProperties
data class BusinessTransaction(
  @DocumentId val id: String = "",
  val userId: String = "",
  val txType: String = TxType.SALE.name,
  val refNumber: String = "",
  val dateMillis: Long = 0L,
  val dueDateMillis: Long = 0L,
  val partyId: String = "",
  val partyName: String = "",
  val placeOfSupply: String = "",
  @get:PropertyName("isInterState") @field:PropertyName("isInterState")
  val isInterState: Boolean = false,
  val category: String = "",
  val paymentMode: String = PaymentMode.CASH.name,
  val accountId: String = "",
  val toAccountId: String = "",
  val linkedTxId: String = "",
  val lineItems: List<LineItem> = emptyList(),
  val subtotal: Double = 0.0,
  val discountAmount: Double = 0.0,
  val billDiscountAmount: Double = 0.0,
  val extraChargesLabel: String = "",
  val extraChargesAmount: Double = 0.0,
  val cgstAmount: Double = 0.0,
  val sgstAmount: Double = 0.0,
  val igstAmount: Double = 0.0,
  val taxAmount: Double = 0.0,
  val roundOffAmount: Double = 0.0,
  val totalAmount: Double = 0.0,
  val paidAmount: Double = 0.0,
  val balanceAmount: Double = 0.0,
  val status: String = TxStatus.PAID.name,
  val agentId: String = "",
  val agentName: String = "",
  val commissionBasis: String = CommissionBasis.TAXABLE.name,
  val commissionTrigger: String = CommissionTrigger.ON_INVOICE.name,
  val commissionStatus: String = CommissionStatus.NONE.name,
  val commissionRatePercent: Double = 0.0,
  val commissionTotalAmount: Double = 0.0,
  val commissionAccruedAmount: Double = 0.0,
  val commissionSplits: List<CommissionSplit> = emptyList(),
  val tdsPercent: Double = 0.0,
  val tdsAmount: Double = 0.0,
  val cancelReason: String = "",
  val cancelledAtMillis: Long = 0L,
  val notes: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

fun BusinessTransaction.txTypeEnum(): TxType =
  TxType.entries.firstOrNull { it.name == txType } ?: TxType.SALE

fun BusinessTransaction.statusEnum(): TxStatus =
  TxStatus.entries.firstOrNull { it.name == status } ?: TxStatus.PAID

fun BusinessTransaction.commissionStatusEnum(): CommissionStatus =
  CommissionStatus.entries.firstOrNull { it.name == commissionStatus } ?: CommissionStatus.NONE

@IgnoreExtraProperties
data class CashBankAccount(
  @DocumentId val id: String = "",
  val userId: String = "",
  val accountName: String = "",
  val accountType: String = AccountType.CASH.name,
  val bankName: String = "",
  val accountNumber: String = "",
  val ifscCode: String = "",
  val upiId: String = "",
  val openingBalance: Double = 0.0,
  val currentBalance: Double = 0.0,
  @get:PropertyName("isActive") @field:PropertyName("isActive")
  val isActive: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

fun CashBankAccount.accountTypeEnum(): AccountType =
  AccountType.entries.firstOrNull { it.name == accountType } ?: AccountType.CASH
