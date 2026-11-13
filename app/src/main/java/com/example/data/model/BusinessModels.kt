package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
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
}

enum class TxStatus(val label: String) {
  PAID("Paid"),
  PARTIAL("Partial"),
  UNPAID("Unpaid"),
  OPEN("Open"),
  CONVERTED("Converted"),
  CANCELLED("Cancelled"),
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
  val bankDetails: String = "",
  val invoiceTerms: String = "Thanks for doing business with us!",
  val themeStyle: String = ThemeStyle.DYNAMIC.name,
  val stopNegativeStock: Boolean = false,
  val roundOffTotal: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

fun BusinessProfile.themeStyleEnum(): ThemeStyle =
  ThemeStyle.entries.firstOrNull { it.name == themeStyle } ?: ThemeStyle.DYNAMIC

@IgnoreExtraProperties
data class Party(
  @DocumentId val id: String = "",
  val userId: String = "",
  val name: String = "",
  val partyType: String = PartyType.CUSTOMER.name,
  val phone: String = "",
  val email: String = "",
  val gstin: String = "",
  val billingAddress: String = "",
  val shippingAddress: String = "",
  val openingBalance: Double = 0.0,
  val currentBalance: Double = 0.0, // Positive = Receivable (customer owes us), Negative = Payable
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
  val openingStock: Double = 0.0,
  val currentStock: Double = 0.0,
  val minStockLevel: Double = 5.0,
  val location: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

fun InventoryItem.itemTypeEnum(): ItemType =
  ItemType.entries.firstOrNull { it.name == itemType } ?: ItemType.PRODUCT

fun InventoryItem.isLowStock(): Boolean =
  itemType == ItemType.PRODUCT.name && minStockLevel > 0.0 && currentStock <= minStockLevel

@IgnoreExtraProperties
data class LineItem(
  val itemId: String = "",
  val itemName: String = "",
  val hsnSac: String = "",
  val unit: String = "PCS",
  val quantity: Double = 1.0,
  val unitPrice: Double = 0.0,
  val discountPercent: Double = 0.0,
  val taxRatePercent: Double = 0.0,
  val taxAmount: Double = 0.0,
  val lineTotal: Double = 0.0,
)

@IgnoreExtraProperties
data class BusinessTransaction(
  @DocumentId val id: String = "",
  val userId: String = "",
  val txType: String = TxType.SALE.name,
  val refNumber: String = "",
  val dateMillis: Long = 0L,
  val partyId: String = "",
  val partyName: String = "",
  val category: String = "",
  val paymentMode: String = PaymentMode.CASH.name,
  val accountId: String = "",
  val lineItems: List<LineItem> = emptyList(),
  val subtotal: Double = 0.0,
  val discountAmount: Double = 0.0,
  val taxAmount: Double = 0.0,
  val roundOffAmount: Double = 0.0,
  val totalAmount: Double = 0.0,
  val paidAmount: Double = 0.0,
  val balanceAmount: Double = 0.0,
  val status: String = TxStatus.PAID.name,
  val notes: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

fun BusinessTransaction.txTypeEnum(): TxType =
  TxType.entries.firstOrNull { it.name == txType } ?: TxType.SALE

fun BusinessTransaction.statusEnum(): TxStatus =
  TxStatus.entries.firstOrNull { it.name == status } ?: TxStatus.PAID

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
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

fun CashBankAccount.accountTypeEnum(): AccountType =
  AccountType.entries.firstOrNull { it.name == accountType } ?: AccountType.CASH
