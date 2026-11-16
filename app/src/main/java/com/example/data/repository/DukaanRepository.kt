package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.R
import com.example.data.model.AccountType
import com.example.data.model.Agent
import com.example.data.model.BusinessProfile
import com.example.data.model.BusinessTransaction
import com.example.data.model.CashBankAccount
import com.example.data.model.CommissionSplit
import com.example.data.model.CommissionStatus
import com.example.data.model.CommissionTrigger
import com.example.data.model.InventoryItem
import com.example.data.model.ItemType
import com.example.data.model.Party
import com.example.data.model.PaymentMode
import com.example.data.model.ThemeStyle
import com.example.data.model.TxStatus
import com.example.data.model.TxType
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.example.ui.util.MoneyUtils
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.math.abs

class DukaanRepository(
  private val db: FirebaseFirestore,
  private val appContext: Context? = null,
) {

  constructor(context: Context) : this(
    db = FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    appContext = context.applicationContext,
  )

  private val auth = Firebase.auth

  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

  private fun userDoc(uid: String) = db.collection("users").document(uid)

  fun isNetworkAvailable(): Boolean {
    val ctx = appContext ?: return true
    val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
    val net = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(net) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
  }

  fun observeHasPendingWrites(): Flow<Boolean> = flow {
    val uid = requireUserId()
    val ref = userDoc(uid).collection("transactions")
    emitAll(
      ref
        .snapshots(MetadataChanges.INCLUDE)
        .map { snap -> snap.metadata.hasPendingWrites() }
        .catch { emit(false) }
    )
  }

  suspend fun ensureProfileInitialized(
    defaultBusinessName: String,
    defaultEmail: String,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val profileRef = userDoc(uid).collection("settings").document("profile")
    try {
      val snapshot = profileRef.get().await()
      if (!snapshot.exists()) {
        val payload = mapOf(
          "userId" to uid,
          "businessName" to defaultBusinessName.ifBlank { "My Dukaan Store" },
          "phone" to "",
          "email" to defaultEmail,
          "gstin" to "",
          "address" to "",
          "stateName" to "",
          "currencySymbol" to "₹",
          "invoicePrefix" to "INV-",
          "nextInvoiceNumber" to 1L,
          "nextPurchaseNumber" to 1L,
          "nextEstimateNumber" to 1L,
          "nextSaleOrderNumber" to 1L,
          "nextChallanNumber" to 1L,
          "nextCreditNoteNumber" to 1L,
          "nextDebitNoteNumber" to 1L,
          "nextPaymentNumber" to 1L,
          "nextExpenseNumber" to 1L,
          "nextCommissionPayoutNumber" to 1L,
          "useFinancialYearPrefix" to true,
          "upiId" to "",
          "bankDetails" to "",
          "invoiceTerms" to "Thanks for doing business with us!",
          "themeStyle" to ThemeStyle.COBALT.name,
          "stopNegativeStock" to false,
          "roundOffTotal" to true,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        profileRef.set(payload).await()
      }

      val accountsRef = userDoc(uid).collection("accounts")
      val existingAccounts = accountsRef.limit(1).get().await()
      if (existingAccounts.isEmpty) {
        val cashRef = accountsRef.document("cash_in_hand")
        cashRef.set(
          mapOf(
            "userId" to uid,
            "accountName" to "Cash in Hand",
            "accountType" to AccountType.CASH.name,
            "bankName" to "",
            "accountNumber" to "",
            "ifscCode" to "",
            "upiId" to "",
            "openingBalance" to 0.0,
            "currentBalance" to 0.0,
            "isActive" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, profileRef.path)
      throw e
    }
  }

  fun observeBusinessProfile(): Flow<BusinessProfile?> = flow {
    val uid = requireUserId()
    val ref = userDoc(uid).collection("settings").document("profile")
    emitAll(
      ref.snapshots()
        .map { snapshot ->
          if (snapshot.exists()) {
            snapshot.toObject(
              BusinessProfile::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
          } else {
            null
          }
        }
        .catch { error ->
          logListenerError(error, OperationType.GET, ref.path, uid)
          throw error
        }
    )
  }

  suspend fun saveBusinessProfile(profile: BusinessProfile): Result<Unit> = runCatching {
    val uid = requireUserId()
    val ref = userDoc(uid).collection("settings").document("profile")
    try {
      val existing = ref.get().await()
      if (existing.exists()) {
        ref.update(
          mapOf(
            "businessName" to profile.businessName.ifBlank { "My Dukaan Store" },
            "phone" to profile.phone.trim(),
            "email" to profile.email.trim(),
            "gstin" to profile.gstin.trim().uppercase(),
            "address" to profile.address.trim(),
            "stateName" to profile.stateName.trim(),
            "currencySymbol" to profile.currencySymbol.ifBlank { "₹" },
            "invoicePrefix" to profile.invoicePrefix,
            "nextInvoiceNumber" to profile.nextInvoiceNumber.coerceAtLeast(1L),
            "nextPurchaseNumber" to profile.nextPurchaseNumber.coerceAtLeast(1L),
            "nextEstimateNumber" to profile.nextEstimateNumber.coerceAtLeast(1L),
            "nextSaleOrderNumber" to profile.nextSaleOrderNumber.coerceAtLeast(1L),
            "nextChallanNumber" to profile.nextChallanNumber.coerceAtLeast(1L),
            "nextCreditNoteNumber" to profile.nextCreditNoteNumber.coerceAtLeast(1L),
            "nextDebitNoteNumber" to profile.nextDebitNoteNumber.coerceAtLeast(1L),
            "nextPaymentNumber" to profile.nextPaymentNumber.coerceAtLeast(1L),
            "nextExpenseNumber" to profile.nextExpenseNumber.coerceAtLeast(1L),
            "nextCommissionPayoutNumber" to profile.nextCommissionPayoutNumber.coerceAtLeast(1L),
            "useFinancialYearPrefix" to profile.useFinancialYearPrefix,
            "upiId" to profile.upiId.trim(),
            "bankDetails" to profile.bankDetails.trim(),
            "invoiceTerms" to profile.invoiceTerms.trim(),
            "themeStyle" to profile.themeStyle.ifBlank { ThemeStyle.COBALT.name },
            "stopNegativeStock" to profile.stopNegativeStock,
            "roundOffTotal" to profile.roundOffTotal,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      } else {
        ref.set(
          mapOf(
            "userId" to uid,
            "businessName" to profile.businessName.ifBlank { "My Dukaan Store" },
            "phone" to profile.phone,
            "email" to profile.email,
            "gstin" to profile.gstin,
            "address" to profile.address,
            "stateName" to profile.stateName,
            "currencySymbol" to profile.currencySymbol.ifBlank { "₹" },
            "invoicePrefix" to profile.invoicePrefix,
            "nextInvoiceNumber" to profile.nextInvoiceNumber.coerceAtLeast(1L),
            "nextPurchaseNumber" to profile.nextPurchaseNumber.coerceAtLeast(1L),
            "nextEstimateNumber" to profile.nextEstimateNumber.coerceAtLeast(1L),
            "nextSaleOrderNumber" to profile.nextSaleOrderNumber.coerceAtLeast(1L),
            "nextChallanNumber" to profile.nextChallanNumber.coerceAtLeast(1L),
            "nextCreditNoteNumber" to profile.nextCreditNoteNumber.coerceAtLeast(1L),
            "nextDebitNoteNumber" to profile.nextDebitNoteNumber.coerceAtLeast(1L),
            "nextPaymentNumber" to profile.nextPaymentNumber.coerceAtLeast(1L),
            "nextExpenseNumber" to profile.nextExpenseNumber.coerceAtLeast(1L),
            "nextCommissionPayoutNumber" to profile.nextCommissionPayoutNumber.coerceAtLeast(1L),
            "useFinancialYearPrefix" to profile.useFinancialYearPrefix,
            "upiId" to profile.upiId,
            "bankDetails" to profile.bankDetails,
            "invoiceTerms" to profile.invoiceTerms,
            "themeStyle" to profile.themeStyle.ifBlank { ThemeStyle.COBALT.name },
            "stopNegativeStock" to profile.stopNegativeStock,
            "roundOffTotal" to profile.roundOffTotal,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, ref.path)
      throw e
    }
  }

  fun observeParties(): Flow<List<Party>> = flow {
    val uid = requireUserId()
    val ref = userDoc(uid).collection("parties")
    emitAll(
      ref.orderBy("name", Query.Direction.ASCENDING)
        .snapshots()
        .map { snap ->
          snap.toObjects(
            Party::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .catch { error ->
          logListenerError(error, OperationType.LIST, ref.path, uid)
          throw error
        }
    )
  }

  suspend fun saveParty(party: Party): Result<String> = runCatching {
    val uid = requireUserId()
    val col = userDoc(uid).collection("parties")
    val isCreate = party.id.isBlank()
    val docRef = if (isCreate) col.document() else col.document(party.id)
    try {
      if (isCreate) {
        docRef.set(
          mapOf(
            "userId" to uid,
            "name" to party.name.trim(),
            "partyType" to party.partyType,
            "phone" to party.phone.trim(),
            "email" to party.email.trim(),
            "gstin" to party.gstin.trim().uppercase(),
            "stateName" to party.stateName.trim(),
            "billingAddress" to party.billingAddress.trim(),
            "shippingAddress" to party.shippingAddress.trim(),
            "openingBalance" to MoneyUtils.round2(party.openingBalance),
            "currentBalance" to MoneyUtils.round2(party.currentBalance),
            "defaultAgentId" to party.defaultAgentId.trim(),
            "isActive" to party.isActive,
            "notes" to party.notes.trim(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      } else {
        docRef.update(
          mapOf(
            "name" to party.name.trim(),
            "partyType" to party.partyType,
            "phone" to party.phone.trim(),
            "email" to party.email.trim(),
            "gstin" to party.gstin.trim().uppercase(),
            "stateName" to party.stateName.trim(),
            "billingAddress" to party.billingAddress.trim(),
            "shippingAddress" to party.shippingAddress.trim(),
            "openingBalance" to MoneyUtils.round2(party.openingBalance),
            "currentBalance" to MoneyUtils.round2(party.currentBalance),
            "defaultAgentId" to party.defaultAgentId.trim(),
            "isActive" to party.isActive,
            "notes" to party.notes.trim(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }
      docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, if (isCreate) OperationType.CREATE else OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun archiveOrDeleteParty(party: Party, hasTransactions: Boolean): Result<Boolean> = runCatching {
    val uid = requireUserId()
    val docRef = userDoc(uid).collection("parties").document(party.id)
    try {
      if (hasTransactions || abs(party.currentBalance) > 0.01) {
        docRef.update(
          mapOf(
            "isActive" to false,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
        false // archived
      } else {
        docRef.delete().await()
        true // deleted
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  suspend fun deleteParty(partyId: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = userDoc(uid).collection("parties").document(partyId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  fun observeItems(): Flow<List<InventoryItem>> = flow {
    val uid = requireUserId()
    val ref = userDoc(uid).collection("items")
    emitAll(
      ref.orderBy("name", Query.Direction.ASCENDING)
        .snapshots()
        .map { snap ->
          snap.toObjects(
            InventoryItem::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .catch { error ->
          logListenerError(error, OperationType.LIST, ref.path, uid)
          throw error
        }
    )
  }

  suspend fun saveItem(item: InventoryItem): Result<String> = runCatching {
    val uid = requireUserId()
    val col = userDoc(uid).collection("items")
    val isCreate = item.id.isBlank()
    val docRef = if (isCreate) col.document() else col.document(item.id)
    try {
      if (isCreate) {
        docRef.set(
          mapOf(
            "userId" to uid,
            "name" to item.name.trim(),
            "itemType" to item.itemType,
            "hsnSac" to item.hsnSac.trim(),
            "itemCode" to item.itemCode.trim(),
            "category" to item.category.trim(),
            "unit" to item.unit.ifBlank { "PCS" },
            "salePrice" to item.salePrice,
            "salePriceWithTax" to item.salePriceWithTax,
            "wholesalePrice" to item.wholesalePrice,
            "purchasePrice" to item.purchasePrice,
            "discountPercent" to item.discountPercent,
            "taxRatePercent" to item.taxRatePercent,
            "commissionPercent" to item.commissionPercent,
            "openingStock" to item.openingStock,
            "currentStock" to item.currentStock,
            "minStockLevel" to item.minStockLevel,
            "location" to item.location.trim(),
            "isActive" to item.isActive,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      } else {
        docRef.update(
          mapOf(
            "name" to item.name.trim(),
            "itemType" to item.itemType,
            "hsnSac" to item.hsnSac.trim(),
            "itemCode" to item.itemCode.trim(),
            "category" to item.category.trim(),
            "unit" to item.unit.ifBlank { "PCS" },
            "salePrice" to item.salePrice,
            "salePriceWithTax" to item.salePriceWithTax,
            "wholesalePrice" to item.wholesalePrice,
            "purchasePrice" to item.purchasePrice,
            "discountPercent" to item.discountPercent,
            "taxRatePercent" to item.taxRatePercent,
            "commissionPercent" to item.commissionPercent,
            "openingStock" to item.openingStock,
            "currentStock" to item.currentStock,
            "minStockLevel" to item.minStockLevel,
            "location" to item.location.trim(),
            "isActive" to item.isActive,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }
      docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, if (isCreate) OperationType.CREATE else OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun adjustItemStock(
    itemId: String,
    deltaQty: Double,
    reason: String = "Manual Stock Adjustment",
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = userDoc(uid).collection("items").document(itemId)
    val txRef = userDoc(uid).collection("transactions").document()
    val profileRef = userDoc(uid).collection("settings").document("profile")
    try {
      db.runTransaction { tr ->
        val itemSnap = tr.get(docRef)
        val itemName = itemSnap.getString("name") ?: "Item"
        val unit = itemSnap.getString("unit") ?: "PCS"
        val currentStock = itemSnap.getDouble("currentStock") ?: 0.0
        val purchasePrice = itemSnap.getDouble("purchasePrice") ?: 0.0
        val profileSnap = tr.get(profileRef)
        val stopNegative = profileSnap.getBoolean("stopNegativeStock") ?: false
        val newStock = MoneyUtils.round2(currentStock + deltaQty)
        if (stopNegative && deltaQty < 0 && newStock < -0.0001) {
          throw IllegalStateException("Cannot reduce stock below zero for $itemName (Current: $currentStock)")
        }

        tr.update(
          docRef,
          mapOf(
            "currentStock" to newStock,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        tr.set(
          txRef,
          mapOf(
            "userId" to uid,
            "txType" to TxType.STOCK_ADJUSTMENT.name,
            "refNumber" to "STKA-${System.currentTimeMillis() % 100000L}",
            "dateMillis" to System.currentTimeMillis(),
            "partyName" to itemName,
            "category" to if (deltaQty >= 0) "Stock Added" else "Stock Reduced",
            "lineItems" to listOf(
              mapOf(
                "itemId" to itemId,
                "itemName" to itemName,
                "unit" to unit,
                "quantity" to abs(deltaQty),
                "unitPrice" to purchasePrice,
                "costPrice" to purchasePrice,
                "lineTotal" to MoneyUtils.round2(abs(deltaQty) * purchasePrice),
              )
            ),
            "subtotal" to MoneyUtils.round2(abs(deltaQty) * purchasePrice),
            "totalAmount" to MoneyUtils.round2(abs(deltaQty) * purchasePrice),
            "paidAmount" to 0.0,
            "balanceAmount" to 0.0,
            "status" to TxStatus.PAID.name,
            "notes" to "${if (deltaQty >= 0) "+" else "-"}${abs(deltaQty)} $unit ($reason)",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
      }.await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun archiveOrDeleteItem(item: InventoryItem, hasTransactions: Boolean): Result<Boolean> = runCatching {
    val uid = requireUserId()
    val docRef = userDoc(uid).collection("items").document(item.id)
    try {
      if (hasTransactions) {
        docRef.update(
          mapOf(
            "isActive" to false,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
        false
      } else {
        docRef.delete().await()
        true
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  suspend fun deleteItem(itemId: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = userDoc(uid).collection("items").document(itemId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  fun observeAccounts(): Flow<List<CashBankAccount>> = flow {
    val uid = requireUserId()
    val ref = userDoc(uid).collection("accounts")
    emitAll(
      ref.orderBy("accountName", Query.Direction.ASCENDING)
        .snapshots()
        .map { snap ->
          snap.toObjects(
            CashBankAccount::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .catch { error ->
          logListenerError(error, OperationType.LIST, ref.path, uid)
          throw error
        }
    )
  }

  suspend fun saveAccount(account: CashBankAccount): Result<String> = runCatching {
    val uid = requireUserId()
    val col = userDoc(uid).collection("accounts")
    val isCreate = account.id.isBlank()
    val docRef = if (isCreate) col.document() else col.document(account.id)
    try {
      if (isCreate) {
        docRef.set(
          mapOf(
            "userId" to uid,
            "accountName" to account.accountName.trim(),
            "accountType" to account.accountType,
            "bankName" to account.bankName.trim(),
            "accountNumber" to account.accountNumber.trim(),
            "ifscCode" to account.ifscCode.trim().uppercase(),
            "upiId" to account.upiId.trim(),
            "openingBalance" to MoneyUtils.round2(account.openingBalance),
            "currentBalance" to MoneyUtils.round2(account.currentBalance),
            "isActive" to account.isActive,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      } else {
        docRef.update(
          mapOf(
            "accountName" to account.accountName.trim(),
            "accountType" to account.accountType,
            "bankName" to account.bankName.trim(),
            "accountNumber" to account.accountNumber.trim(),
            "ifscCode" to account.ifscCode.trim().uppercase(),
            "upiId" to account.upiId.trim(),
            "openingBalance" to MoneyUtils.round2(account.openingBalance),
            "currentBalance" to MoneyUtils.round2(account.currentBalance),
            "isActive" to account.isActive,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }
      docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, if (isCreate) OperationType.CREATE else OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun adjustAccountBalance(
    accountId: String,
    delta: Double,
    reason: String = "Manual Account Adjustment",
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = userDoc(uid).collection("accounts").document(accountId)
    val txRef = userDoc(uid).collection("transactions").document()
    try {
      db.runTransaction { tr ->
        val accSnap = tr.get(docRef)
        val accName = accSnap.getString("accountName") ?: "Account"
        val curBal = accSnap.getDouble("currentBalance") ?: 0.0
        val newBal = MoneyUtils.round2(curBal + delta)
        tr.update(
          docRef,
          mapOf(
            "currentBalance" to newBal,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        tr.set(
          txRef,
          mapOf(
            "userId" to uid,
            "txType" to TxType.CASH_ADJUSTMENT.name,
            "refNumber" to "ADJ-${System.currentTimeMillis() % 100000L}",
            "dateMillis" to System.currentTimeMillis(),
            "partyName" to accName,
            "category" to if (delta >= 0) "Deposit / Add" else "Withdrawal / Deduct",
            "accountId" to accountId,
            "totalAmount" to MoneyUtils.round2(abs(delta)),
            "paidAmount" to MoneyUtils.round2(abs(delta)),
            "balanceAmount" to 0.0,
            "status" to TxStatus.PAID.name,
            "notes" to reason.ifBlank { "Manual Balance Adjustment" },
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
      }.await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun transferBetweenAccounts(
    fromAccountId: String,
    toAccountId: String,
    amount: Double,
    notes: String = "",
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    require(fromAccountId.isNotBlank() && toAccountId.isNotBlank() && fromAccountId != toAccountId) {
      "Select two different accounts for transfer"
    }
    val cleanAmount = MoneyUtils.round2(amount)
    require(cleanAmount > 0.0) { "Transfer amount must be positive" }

    val fromRef = userDoc(uid).collection("accounts").document(fromAccountId)
    val toRef = userDoc(uid).collection("accounts").document(toAccountId)
    val txRef = userDoc(uid).collection("transactions").document()
    try {
      db.runTransaction { tr ->
        val fromSnap = tr.get(fromRef)
        val toSnap = tr.get(toRef)
        val fromName = fromSnap.getString("accountName") ?: "Source Account"
        val toName = toSnap.getString("accountName") ?: "Target Account"
        val fromBal = fromSnap.getDouble("currentBalance") ?: 0.0
        val toBal = toSnap.getDouble("currentBalance") ?: 0.0

        tr.update(
          fromRef,
          mapOf(
            "currentBalance" to MoneyUtils.round2(fromBal - cleanAmount),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        tr.update(
          toRef,
          mapOf(
            "currentBalance" to MoneyUtils.round2(toBal + cleanAmount),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        tr.set(
          txRef,
          mapOf(
            "userId" to uid,
            "txType" to TxType.CASH_TRANSFER.name,
            "refNumber" to "XFER-${System.currentTimeMillis() % 100000L}",
            "dateMillis" to System.currentTimeMillis(),
            "partyName" to "$fromName -> $toName",
            "category" to "Account Transfer",
            "paymentMode" to PaymentMode.BANK_TRANSFER.name,
            "accountId" to fromAccountId,
            "toAccountId" to toAccountId,
            "totalAmount" to cleanAmount,
            "paidAmount" to cleanAmount,
            "balanceAmount" to 0.0,
            "status" to TxStatus.PAID.name,
            "notes" to notes.trim().ifBlank { "Contra transfer $fromName to $toName" },
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
      }.await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, txRef.path)
      throw e
    }
  }

  suspend fun archiveOrDeleteAccount(account: CashBankAccount, hasTransactions: Boolean): Result<Boolean> = runCatching {
    val uid = requireUserId()
    val docRef = userDoc(uid).collection("accounts").document(account.id)
    try {
      if (hasTransactions || abs(account.currentBalance) > 0.01) {
        docRef.update(
          mapOf(
            "isActive" to false,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
        false
      } else {
        docRef.delete().await()
        true
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  // ==================== AGENTS & MISTRIS ====================

  fun observeAgents(): Flow<List<Agent>> = flow {
    val uid = requireUserId()
    val ref = userDoc(uid).collection("agents")
    emitAll(
      ref.orderBy("name", Query.Direction.ASCENDING)
        .snapshots()
        .map { snap ->
          snap.toObjects(
            Agent::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .catch { error ->
          logListenerError(error, OperationType.LIST, ref.path, uid)
          throw error
        }
    )
  }

  suspend fun saveAgent(agent: Agent): Result<String> = runCatching {
    val uid = requireUserId()
    val col = userDoc(uid).collection("agents")
    val isCreate = agent.id.isBlank()
    val docRef = if (isCreate) col.document() else col.document(agent.id)
    try {
      if (isCreate) {
        docRef.set(
          mapOf(
            "userId" to uid,
            "name" to agent.name.trim(),
            "agentType" to agent.agentType,
            "phone" to agent.phone.trim(),
            "cityArea" to agent.cityArea.trim(),
            "upiId" to agent.upiId.trim(),
            "bankDetails" to agent.bankDetails.trim(),
            "defaultPercent" to agent.defaultPercent,
            "defaultPerUnitAmount" to agent.defaultPerUnitAmount,
            "defaultBasis" to agent.defaultBasis,
            "defaultTrigger" to agent.defaultTrigger,
            "openingBalance" to MoneyUtils.round2(agent.openingBalance),
            "currentBalance" to MoneyUtils.round2(agent.currentBalance),
            "totalEarned" to MoneyUtils.round2(agent.totalEarned),
            "totalPaid" to MoneyUtils.round2(agent.totalPaid),
            "isActive" to agent.isActive,
            "notes" to agent.notes.trim(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      } else {
        docRef.update(
          mapOf(
            "name" to agent.name.trim(),
            "agentType" to agent.agentType,
            "phone" to agent.phone.trim(),
            "cityArea" to agent.cityArea.trim(),
            "upiId" to agent.upiId.trim(),
            "bankDetails" to agent.bankDetails.trim(),
            "defaultPercent" to agent.defaultPercent,
            "defaultPerUnitAmount" to agent.defaultPerUnitAmount,
            "defaultBasis" to agent.defaultBasis,
            "defaultTrigger" to agent.defaultTrigger,
            "openingBalance" to MoneyUtils.round2(agent.openingBalance),
            "currentBalance" to MoneyUtils.round2(agent.currentBalance),
            "totalEarned" to MoneyUtils.round2(agent.totalEarned),
            "totalPaid" to MoneyUtils.round2(agent.totalPaid),
            "isActive" to agent.isActive,
            "notes" to agent.notes.trim(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }
      docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, if (isCreate) OperationType.CREATE else OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun archiveOrDeleteAgent(agent: Agent, hasTransactions: Boolean): Result<Boolean> = runCatching {
    val uid = requireUserId()
    val docRef = userDoc(uid).collection("agents").document(agent.id)
    try {
      if (hasTransactions || abs(agent.currentBalance) > 0.01) {
        docRef.update(
          mapOf(
            "isActive" to false,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
        false
      } else {
        docRef.delete().await()
        true
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  suspend fun recordCommissionPayout(
    agent: Agent,
    grossPayoutAmount: Double,
    tdsPercent: Double,
    accountId: String,
    paymentMode: PaymentMode,
    refNumber: String,
    notes: String,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val gross = MoneyUtils.round2(grossPayoutAmount)
    require(gross > 0.0) { "Payout amount must be greater than zero" }
    val cleanTdsPct = tdsPercent.coerceIn(0.0, 50.0)
    val tdsAmt = MoneyUtils.round2(gross * cleanTdsPct / 100.0)
    val netCashPaid = MoneyUtils.round2((gross - tdsAmt).coerceAtLeast(0.0))

    val agentRef = userDoc(uid).collection("agents").document(agent.id)
    val txRef = userDoc(uid).collection("transactions").document()
    val accRef = if (accountId.isNotBlank()) userDoc(uid).collection("accounts").document(accountId) else null
    val profileRef = userDoc(uid).collection("settings").document("profile")

    try {
      db.runTransaction { tr ->
        val agentSnap = tr.get(agentRef)
        val accSnap = accRef?.let { tr.get(it) }
        val profileSnap = tr.get(profileRef)

        val liveAgentBal = agentSnap.getDouble("currentBalance") ?: 0.0
        val liveEarned = agentSnap.getDouble("totalEarned") ?: 0.0
        val livePaid = agentSnap.getDouble("totalPaid") ?: 0.0

        tr.update(
          agentRef,
          mapOf(
            "currentBalance" to MoneyUtils.round2(liveAgentBal - gross),
            "totalEarned" to liveEarned,
            "totalPaid" to MoneyUtils.round2(livePaid + gross),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )

        if (accRef != null && accSnap != null && netCashPaid > 0.001) {
          val liveAccBal = accSnap.getDouble("currentBalance") ?: 0.0
          tr.update(
            accRef,
            mapOf(
              "currentBalance" to MoneyUtils.round2(liveAccBal - netCashPaid),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
        }

        if (profileSnap.exists()) {
          val curSeq = profileSnap.getLong("nextCommissionPayoutNumber") ?: 1L
          tr.update(
            profileRef,
            mapOf(
              "nextCommissionPayoutNumber" to (curSeq + 1L),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
        }

        tr.set(
          txRef,
          mapOf(
            "userId" to uid,
            "txType" to TxType.COMMISSION_PAYOUT.name,
            "refNumber" to refNumber.trim(),
            "dateMillis" to System.currentTimeMillis(),
            "partyName" to agent.name,
            "category" to "Commission Payout (${agent.agentType})",
            "paymentMode" to paymentMode.name,
            "accountId" to accountId,
            "totalAmount" to gross,
            "paidAmount" to netCashPaid,
            "balanceAmount" to 0.0,
            "status" to TxStatus.PAID.name,
            "agentId" to agent.id,
            "agentName" to agent.name,
            "commissionStatus" to CommissionStatus.PAID.name,
            "tdsPercent" to cleanTdsPct,
            "tdsAmount" to tdsAmt,
            "notes" to notes.trim(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
      }.await()
      txRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, txRef.path)
      throw e
    }
  }

  // ==================== TRANSACTIONS ====================

  fun observeTransactions(): Flow<List<BusinessTransaction>> = flow {
    val uid = requireUserId()
    val ref = userDoc(uid).collection("transactions")
    emitAll(
      ref.orderBy("dateMillis", Query.Direction.DESCENDING)
        .snapshots()
        .map { snap ->
          snap.toObjects(
            BusinessTransaction::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .catch { error ->
          logListenerError(error, OperationType.LIST, ref.path, uid)
          throw error
        }
    )
  }

  private fun counterFieldForTxType(txType: TxType): String =
    when (txType) {
      TxType.SALE -> "nextInvoiceNumber"
      TxType.PURCHASE -> "nextPurchaseNumber"
      TxType.ESTIMATE -> "nextEstimateNumber"
      TxType.SALE_ORDER, TxType.PURCHASE_ORDER -> "nextSaleOrderNumber"
      TxType.DELIVERY_CHALLAN -> "nextChallanNumber"
      TxType.CREDIT_NOTE -> "nextCreditNoteNumber"
      TxType.DEBIT_NOTE -> "nextDebitNoteNumber"
      TxType.PAYMENT_IN, TxType.PAYMENT_OUT -> "nextPaymentNumber"
      TxType.EXPENSE -> "nextExpenseNumber"
      TxType.COMMISSION_PAYOUT -> "nextCommissionPayoutNumber"
      else -> "nextInvoiceNumber"
    }

  suspend fun recordTransaction(
    tx: BusinessTransaction,
    knownItems: List<InventoryItem>,
    incrementInvoiceSequence: Boolean = false,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val txRef = userDoc(uid).collection("transactions").document()
    val txType = TxType.entries.firstOrNull { it.name == tx.txType } ?: TxType.SALE

    val lineMaps = tx.lineItems.map { line ->
      mapOf(
        "itemId" to line.itemId,
        "itemName" to line.itemName,
        "hsnSac" to line.hsnSac,
        "unit" to line.unit,
        "quantity" to line.quantity,
        "unitPrice" to line.unitPrice,
        "costPrice" to line.costPrice,
        "priceIncludesTax" to line.priceIncludesTax,
        "discountPercent" to line.discountPercent,
        "taxRatePercent" to line.taxRatePercent,
        "cgstAmount" to line.cgstAmount,
        "sgstAmount" to line.sgstAmount,
        "igstAmount" to line.igstAmount,
        "taxAmount" to line.taxAmount,
        "lineTotal" to line.lineTotal,
        "commissionPercent" to line.commissionPercent,
        "commissionAmount" to line.commissionAmount,
      )
    }

    val splitMaps = tx.commissionSplits.map { s ->
      mapOf(
        "agentId" to s.agentId,
        "agentName" to s.agentName,
        "agentType" to s.agentType,
        "sharePercent" to s.sharePercent,
        "ratePercent" to s.ratePercent,
        "totalCommission" to s.totalCommission,
        "accruedCommission" to s.accruedCommission,
      )
    }

    val txPayload = buildMap<String, Any> {
      put("userId", uid)
      put("txType", tx.txType)
      put("refNumber", tx.refNumber.trim())
      put("dateMillis", if (tx.dateMillis > 0L) tx.dateMillis else System.currentTimeMillis())
      if (tx.dueDateMillis > 0L) put("dueDateMillis", tx.dueDateMillis)
      put("partyId", tx.partyId)
      put("partyName", tx.partyName.trim())
      put("placeOfSupply", tx.placeOfSupply.trim())
      put("isInterState", tx.isInterState)
      put("category", tx.category.trim())
      put("paymentMode", tx.paymentMode)
      put("accountId", tx.accountId)
      put("toAccountId", tx.toAccountId)
      put("linkedTxId", tx.linkedTxId)
      put("lineItems", lineMaps)
      put("subtotal", tx.subtotal)
      put("discountAmount", tx.discountAmount)
      put("billDiscountAmount", tx.billDiscountAmount)
      put("extraChargesLabel", tx.extraChargesLabel.trim())
      put("extraChargesAmount", tx.extraChargesAmount)
      put("cgstAmount", tx.cgstAmount)
      put("sgstAmount", tx.sgstAmount)
      put("igstAmount", tx.igstAmount)
      put("taxAmount", tx.taxAmount)
      put("roundOffAmount", tx.roundOffAmount)
      put("totalAmount", tx.totalAmount)
      put("paidAmount", tx.paidAmount)
      put("balanceAmount", tx.balanceAmount)
      put("status", tx.status)
      put("agentId", tx.agentId)
      put("agentName", tx.agentName.trim())
      put("commissionBasis", tx.commissionBasis)
      put("commissionTrigger", tx.commissionTrigger)
      put("commissionStatus", tx.commissionStatus)
      put("commissionRatePercent", tx.commissionRatePercent)
      put("commissionTotalAmount", tx.commissionTotalAmount)
      put("commissionAccruedAmount", tx.commissionAccruedAmount)
      put("commissionSplits", splitMaps)
      put("tdsPercent", tx.tdsPercent)
      put("tdsAmount", tx.tdsAmount)
      put("notes", tx.notes.trim())
      put("createdAt", FieldValue.serverTimestamp())
      put("updatedAt", FieldValue.serverTimestamp())
    }

    val stockSign = when (txType) {
      TxType.SALE, TxType.DEBIT_NOTE -> -1.0
      TxType.PURCHASE, TxType.CREDIT_NOTE -> 1.0
      else -> 0.0
    }
    val qtyByItem = if (stockSign != 0.0) {
      tx.lineItems
        .filter { it.itemId.isNotBlank() && it.quantity > 0.0 }
        .groupBy { it.itemId }
        .mapValues { (_, lines) -> lines.sumOf { it.quantity } }
    } else {
      emptyMap()
    }

    val partyDelta = if (tx.partyId.isNotBlank()) {
      when (txType) {
        TxType.SALE -> tx.balanceAmount
        TxType.PAYMENT_IN -> -tx.totalAmount
        TxType.CREDIT_NOTE -> -tx.totalAmount
        TxType.PURCHASE -> -tx.balanceAmount
        TxType.PAYMENT_OUT -> tx.totalAmount
        TxType.DEBIT_NOTE -> tx.totalAmount
        else -> 0.0
      }
    } else 0.0

    val cashDelta = if (tx.accountId.isNotBlank()) {
      when (txType) {
        TxType.SALE -> tx.paidAmount
        TxType.PAYMENT_IN -> tx.totalAmount
        TxType.DEBIT_NOTE -> tx.paidAmount
        TxType.PURCHASE -> -tx.paidAmount
        TxType.PAYMENT_OUT -> -tx.totalAmount
        TxType.EXPENSE -> -tx.totalAmount
        TxType.CREDIT_NOTE -> -tx.paidAmount
        TxType.COMMISSION_PAYOUT -> -tx.paidAmount
        else -> 0.0
      }
    } else 0.0

    // Effective agent splits for accrued commissions
    val effectiveSplits: List<CommissionSplit> = when {
      tx.commissionSplits.isNotEmpty() -> tx.commissionSplits.filter { it.agentId.isNotBlank() && it.accruedCommission > 0.001 }
      tx.agentId.isNotBlank() && tx.commissionAccruedAmount > 0.001 -> listOf(
        CommissionSplit(
          agentId = tx.agentId,
          agentName = tx.agentName,
          sharePercent = 100.0,
          ratePercent = tx.commissionRatePercent,
          totalCommission = tx.commissionTotalAmount,
          accruedCommission = tx.commissionAccruedAmount,
        )
      )
      else -> emptyList()
    }

    val profileRef = userDoc(uid).collection("settings").document("profile")
    val partyRef = if (tx.partyId.isNotBlank() && abs(partyDelta) > 0.001) {
      userDoc(uid).collection("parties").document(tx.partyId)
    } else null
    val accRef = if (tx.accountId.isNotBlank() && abs(cashDelta) > 0.001) {
      userDoc(uid).collection("accounts").document(tx.accountId)
    } else null
    val linkedRef = if (tx.linkedTxId.isNotBlank()) {
      userDoc(uid).collection("transactions").document(tx.linkedTxId)
    } else null

    try {
      db.runTransaction { tr ->
        // ---- ALL READS FIRST ----
        val profileSnap = tr.get(profileRef)
        val stopNegative = profileSnap.getBoolean("stopNegativeStock") ?: false

        val itemSnaps = qtyByItem.keys.associateWith { itemId ->
          tr.get(userDoc(uid).collection("items").document(itemId))
        }
        val partySnap = partyRef?.let { tr.get(it) }
        val accSnap = accRef?.let { tr.get(it) }
        val linkedSnap = linkedRef?.let { tr.get(it) }

        val agentIdsToLoad = mutableSetOf<String>()
        if (txType == TxType.SALE || txType == TxType.CREDIT_NOTE) {
          effectiveSplits.forEach { agentIdsToLoad.add(it.agentId) }
        }
        // Also load linked invoice's agents if this is PAYMENT_IN settling a linked invoice
        var linkedInvoiceCommissionUpdates = emptyList<CommissionSplit>()
        var linkedNewAccrued = 0.0
        var linkedNewCommStatus = CommissionStatus.NONE.name
        var linkedNewPaid = 0.0
        var linkedNewBal = 0.0
        var linkedNewStatus = TxStatus.PAID.name
        val linkedAgentDeltas = mutableMapOf<String, Double>()

        if (txType == TxType.PAYMENT_IN && linkedSnap != null && linkedSnap.exists()) {
          val linkedObj = linkedSnap.toObject(BusinessTransaction::class.java)
          if (linkedObj != null && linkedObj.status != TxStatus.CANCELLED.name && linkedObj.balanceAmount > 0.005) {
            val applied = tx.totalAmount.coerceAtMost(linkedObj.balanceAmount)
            linkedNewPaid = MoneyUtils.round2(linkedObj.paidAmount + applied)
            linkedNewBal = MoneyUtils.round2((linkedObj.totalAmount - linkedNewPaid).coerceAtLeast(0.0))
            linkedNewStatus = if (linkedNewBal <= 0.01) TxStatus.PAID.name else TxStatus.PARTIAL.name
            if (linkedObj.commissionTrigger == CommissionTrigger.ON_COLLECTION.name &&
              linkedObj.commissionTotalAmount > 0.001 &&
              linkedObj.totalAmount > 0.005
            ) {
              val ratio = (linkedNewPaid / linkedObj.totalAmount).coerceIn(0.0, 1.0)
              linkedNewAccrued = MoneyUtils.round2(linkedObj.commissionTotalAmount * ratio)
              val deltaComm = MoneyUtils.round2((linkedNewAccrued - linkedObj.commissionAccruedAmount).coerceAtLeast(0.0))
              linkedNewCommStatus = if (linkedNewAccrued >= linkedObj.commissionTotalAmount - 0.01) {
                CommissionStatus.ACCRUED.name
              } else if (linkedNewAccrued > 0.01) {
                CommissionStatus.PARTIAL.name
              } else {
                CommissionStatus.PENDING_COLLECTION.name
              }
              if (linkedObj.commissionSplits.isNotEmpty()) {
                linkedInvoiceCommissionUpdates = linkedObj.commissionSplits.map { sp ->
                  val newSpAccrued = MoneyUtils.round2(sp.totalCommission * ratio)
                  val spDelta = MoneyUtils.round2((newSpAccrued - sp.accruedCommission).coerceAtLeast(0.0))
                  if (sp.agentId.isNotBlank() && spDelta > 0.001) {
                    agentIdsToLoad.add(sp.agentId)
                    linkedAgentDeltas[sp.agentId] = (linkedAgentDeltas[sp.agentId] ?: 0.0) + spDelta
                  }
                  sp.copy(accruedCommission = newSpAccrued)
                }
              } else if (linkedObj.agentId.isNotBlank() && deltaComm > 0.001) {
                agentIdsToLoad.add(linkedObj.agentId)
                linkedAgentDeltas[linkedObj.agentId] = deltaComm
              }
            } else {
              linkedNewAccrued = linkedObj.commissionAccruedAmount
              linkedNewCommStatus = linkedObj.commissionStatus
              linkedInvoiceCommissionUpdates = linkedObj.commissionSplits
            }
          }
        }

        val agentSnaps = agentIdsToLoad.associateWith { agentId ->
          tr.get(userDoc(uid).collection("agents").document(agentId))
        }

        // ---- VALIDATION ----
        qtyByItem.forEach { (itemId, qtySum) ->
          val snap = itemSnaps[itemId]
          val matched = knownItems.firstOrNull { it.id == itemId }
          val itemType = snap?.getString("itemType") ?: matched?.itemType ?: ItemType.PRODUCT.name
          if (itemType == ItemType.PRODUCT.name && snap != null && snap.exists()) {
            val liveStock = snap.getDouble("currentStock") ?: matched?.currentStock ?: 0.0
            val itemName = snap.getString("name") ?: matched?.name ?: "Item"
            if (stopNegative && stockSign < 0 && liveStock + qtySum * stockSign < -0.0001) {
              throw IllegalStateException("Insufficient stock for $itemName (Available: $liveStock, Needed: $qtySum)")
            }
          }
        }

        // ---- ALL WRITES ----
        tr.set(txRef, txPayload)

        qtyByItem.forEach { (itemId, qtySum) ->
          val snap = itemSnaps[itemId]
          val matched = knownItems.firstOrNull { it.id == itemId }
          val itemType = snap?.getString("itemType") ?: matched?.itemType ?: ItemType.PRODUCT.name
          if (itemType == ItemType.PRODUCT.name && snap != null && snap.exists()) {
            val liveStock = snap.getDouble("currentStock") ?: matched?.currentStock ?: 0.0
            val itemRef = userDoc(uid).collection("items").document(itemId)
            tr.update(
              itemRef,
              mapOf(
                "currentStock" to MoneyUtils.round2(liveStock + qtySum * stockSign),
                "updatedAt" to FieldValue.serverTimestamp(),
              )
            )
          }
        }

        if (partyRef != null && partySnap != null && partySnap.exists()) {
          val liveBal = partySnap.getDouble("currentBalance") ?: 0.0
          tr.update(
            partyRef,
            mapOf(
              "currentBalance" to MoneyUtils.round2(liveBal + partyDelta),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
        }

        if (accRef != null && accSnap != null && accSnap.exists()) {
          val liveAcc = accSnap.getDouble("currentBalance") ?: 0.0
          tr.update(
            accRef,
            mapOf(
              "currentBalance" to MoneyUtils.round2(liveAcc + cashDelta),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
        }

        // Agent commission accrual on SALE or clawback on CREDIT_NOTE
        if (txType == TxType.SALE || txType == TxType.CREDIT_NOTE) {
          val commSign = if (txType == TxType.SALE) 1.0 else -1.0
          effectiveSplits.forEach { sp ->
            val aSnap = agentSnaps[sp.agentId]
            if (aSnap != null && aSnap.exists()) {
              val curBal = aSnap.getDouble("currentBalance") ?: 0.0
              val curEarned = aSnap.getDouble("totalEarned") ?: 0.0
              val curPaid = aSnap.getDouble("totalPaid") ?: 0.0
              val delta = MoneyUtils.round2(sp.accruedCommission * commSign)
              tr.update(
                userDoc(uid).collection("agents").document(sp.agentId),
                mapOf(
                  "currentBalance" to MoneyUtils.round2(curBal + delta),
                  "totalEarned" to MoneyUtils.round2(curEarned + delta),
                  "totalPaid" to curPaid,
                  "updatedAt" to FieldValue.serverTimestamp(),
                )
              )
            }
          }
        }

        // If converting Estimate/Order/Challan -> Sale, mark source CONVERTED
        if (txType == TxType.SALE && linkedRef != null && linkedSnap != null && linkedSnap.exists()) {
          tr.update(
            linkedRef,
            mapOf(
              "status" to TxStatus.CONVERTED.name,
              "linkedTxId" to txRef.id,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
        }

        // If PAYMENT_IN linked to specific invoice, update invoice paid/balance & ON_COLLECTION commissions
        if (txType == TxType.PAYMENT_IN && linkedRef != null && linkedSnap != null && linkedSnap.exists()) {
          val linkedSplitMaps = linkedInvoiceCommissionUpdates.map { s ->
            mapOf(
              "agentId" to s.agentId,
              "agentName" to s.agentName,
              "agentType" to s.agentType,
              "sharePercent" to s.sharePercent,
              "ratePercent" to s.ratePercent,
              "totalCommission" to s.totalCommission,
              "accruedCommission" to s.accruedCommission,
            )
          }
          tr.update(
            linkedRef,
            mapOf(
              "paidAmount" to linkedNewPaid,
              "balanceAmount" to linkedNewBal,
              "status" to linkedNewStatus,
              "commissionAccruedAmount" to linkedNewAccrued,
              "commissionStatus" to linkedNewCommStatus,
              "commissionSplits" to linkedSplitMaps,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          linkedAgentDeltas.forEach { (agId, delta) ->
            val aSnap = agentSnaps[agId]
            if (aSnap != null && aSnap.exists()) {
              val curBal = aSnap.getDouble("currentBalance") ?: 0.0
              val curEarned = aSnap.getDouble("totalEarned") ?: 0.0
              val curPaid = aSnap.getDouble("totalPaid") ?: 0.0
              tr.update(
                userDoc(uid).collection("agents").document(agId),
                mapOf(
                  "currentBalance" to MoneyUtils.round2(curBal + delta),
                  "totalEarned" to MoneyUtils.round2(curEarned + delta),
                  "totalPaid" to curPaid,
                  "updatedAt" to FieldValue.serverTimestamp(),
                )
              )
            }
          }
        }

        if (incrementInvoiceSequence && profileSnap.exists()) {
          val field = counterFieldForTxType(txType)
          val curSeq = profileSnap.getLong(field) ?: 1L
          tr.update(
            profileRef,
            mapOf(
              field to (curSeq + 1L),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
        }
      }.await()
      txRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: FirebaseFirestoreException) {
      if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE && !isNetworkAvailable()) {
        // Offline fallback: queue batch write cleanly in Firestore local cache
        val batch = db.batch()
        batch.set(txRef, txPayload)
        qtyByItem.forEach { (itemId, qtySum) ->
          val matched = knownItems.firstOrNull { it.id == itemId }
          if (matched == null || matched.itemType == ItemType.PRODUCT.name) {
            batch.update(
              userDoc(uid).collection("items").document(itemId),
              mapOf(
                "currentStock" to FieldValue.increment(qtySum * stockSign),
                "updatedAt" to FieldValue.serverTimestamp(),
              )
            )
          }
        }
        if (partyRef != null) {
          batch.update(
            partyRef,
            mapOf(
              "currentBalance" to FieldValue.increment(partyDelta),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
        }
        if (accRef != null) {
          batch.update(
            accRef,
            mapOf(
              "currentBalance" to FieldValue.increment(cashDelta),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
        }
        if (incrementInvoiceSequence) {
          val field = counterFieldForTxType(txType)
          batch.update(
            profileRef,
            mapOf(
              field to FieldValue.increment(1L),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
        }
        batch.commit()
        txRef.id
      } else {
        handleFirestoreError(e, OperationType.WRITE, txRef.path)
        throw e
      }
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, txRef.path)
      throw e
    }
  }

  suspend fun recordSettlementPayment(
    transactionId: String,
    additionalPaid: Double,
    newTotalPaid: Double = 0.0,
    newBalance: Double = 0.0,
    newStatus: TxStatus = TxStatus.PAID,
    partyId: String = "",
    txType: TxType = TxType.SALE,
    accountId: String = "",
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val txRef = userDoc(uid).collection("transactions").document(transactionId)
    val partyRef = if (partyId.isNotBlank()) userDoc(uid).collection("parties").document(partyId) else null
    val accRef = if (accountId.isNotBlank()) userDoc(uid).collection("accounts").document(accountId) else null

    try {
      db.runTransaction { tr ->
        val txSnap = tr.get(txRef)
        val liveTx = txSnap.toObject(BusinessTransaction::class.java)
          ?: throw IllegalStateException("Transaction not found")
        if (liveTx.status == TxStatus.CANCELLED.name) {
          throw IllegalStateException("Cannot record settlement on a cancelled transaction")
        }
        if (liveTx.balanceAmount <= 0.005) {
          throw IllegalStateException("Transaction is already settled")
        }

        val livePartyRef = when {
          partyRef != null -> partyRef
          liveTx.partyId.isNotBlank() -> userDoc(uid).collection("parties").document(liveTx.partyId)
          else -> null
        }
        val partySnap = livePartyRef?.let { tr.get(it) }
        val accSnap = accRef?.let { tr.get(it) }

        val effectiveTxType = TxType.entries.firstOrNull { it.name == liveTx.txType } ?: txType
        val payAmt = MoneyUtils.round2(additionalPaid.coerceAtMost(liveTx.balanceAmount))
        if (payAmt <= 0.0) throw IllegalArgumentException("Payment amount must be positive")

        val computedNewPaid = MoneyUtils.round2(liveTx.paidAmount + payAmt)
        val computedNewBal = MoneyUtils.round2((liveTx.totalAmount - computedNewPaid).coerceAtLeast(0.0))
        val computedStatus = if (computedNewBal <= 0.01) TxStatus.PAID else TxStatus.PARTIAL

        // Proportional ON_COLLECTION commission accrual
        val agentIds = mutableSetOf<String>()
        val agentDeltas = mutableMapOf<String, Double>()
        var updatedAccrued = liveTx.commissionAccruedAmount
        var updatedCommStatus = liveTx.commissionStatus
        var updatedSplits = liveTx.commissionSplits

        if (effectiveTxType == TxType.SALE &&
          liveTx.commissionTrigger == CommissionTrigger.ON_COLLECTION.name &&
          liveTx.commissionTotalAmount > 0.001 &&
          liveTx.totalAmount > 0.005
        ) {
          val ratio = (computedNewPaid / liveTx.totalAmount).coerceIn(0.0, 1.0)
          updatedAccrued = MoneyUtils.round2(liveTx.commissionTotalAmount * ratio)
          updatedCommStatus = if (updatedAccrued >= liveTx.commissionTotalAmount - 0.01) {
            CommissionStatus.ACCRUED.name
          } else if (updatedAccrued > 0.01) {
            CommissionStatus.PARTIAL.name
          } else {
            CommissionStatus.PENDING_COLLECTION.name
          }

          if (liveTx.commissionSplits.isNotEmpty()) {
            updatedSplits = liveTx.commissionSplits.map { sp ->
              val newSpAccrued = MoneyUtils.round2(sp.totalCommission * ratio)
              val delta = MoneyUtils.round2((newSpAccrued - sp.accruedCommission).coerceAtLeast(0.0))
              if (sp.agentId.isNotBlank() && delta > 0.001) {
                agentIds.add(sp.agentId)
                agentDeltas[sp.agentId] = (agentDeltas[sp.agentId] ?: 0.0) + delta
              }
              sp.copy(accruedCommission = newSpAccrued)
            }
          } else if (liveTx.agentId.isNotBlank()) {
            val delta = MoneyUtils.round2((updatedAccrued - liveTx.commissionAccruedAmount).coerceAtLeast(0.0))
            if (delta > 0.001) {
              agentIds.add(liveTx.agentId)
              agentDeltas[liveTx.agentId] = delta
            }
          }
        }

        val agentSnaps = agentIds.associateWith { id ->
          tr.get(userDoc(uid).collection("agents").document(id))
        }

        // ---- WRITES ----
        val splitMaps = updatedSplits.map { s ->
          mapOf(
            "agentId" to s.agentId,
            "agentName" to s.agentName,
            "agentType" to s.agentType,
            "sharePercent" to s.sharePercent,
            "ratePercent" to s.ratePercent,
            "totalCommission" to s.totalCommission,
            "accruedCommission" to s.accruedCommission,
          )
        }
        tr.update(
          txRef,
          mapOf(
            "paidAmount" to computedNewPaid,
            "balanceAmount" to computedNewBal,
            "status" to computedStatus.name,
            "commissionAccruedAmount" to updatedAccrued,
            "commissionStatus" to updatedCommStatus,
            "commissionSplits" to splitMaps,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )

        if (livePartyRef != null && partySnap != null && partySnap.exists()) {
          val liveBal = partySnap.getDouble("currentBalance") ?: 0.0
          val partyDelta = when (effectiveTxType) {
            TxType.SALE, TxType.DEBIT_NOTE -> -payAmt
            else -> payAmt
          }
          tr.update(
            livePartyRef,
            mapOf(
              "currentBalance" to MoneyUtils.round2(liveBal + partyDelta),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
        }

        if (accRef != null && accSnap != null && accSnap.exists()) {
          val liveAcc = accSnap.getDouble("currentBalance") ?: 0.0
          val cashDelta = when (effectiveTxType) {
            TxType.SALE, TxType.DEBIT_NOTE -> payAmt
            else -> -payAmt
          }
          tr.update(
            accRef,
            mapOf(
              "currentBalance" to MoneyUtils.round2(liveAcc + cashDelta),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
        }

        agentDeltas.forEach { (agId, delta) ->
          val aSnap = agentSnaps[agId]
          if (aSnap != null && aSnap.exists()) {
            val curBal = aSnap.getDouble("currentBalance") ?: 0.0
            val curEarned = aSnap.getDouble("totalEarned") ?: 0.0
            val curPaid = aSnap.getDouble("totalPaid") ?: 0.0
            tr.update(
              userDoc(uid).collection("agents").document(agId),
              mapOf(
                "currentBalance" to MoneyUtils.round2(curBal + delta),
                "totalEarned" to MoneyUtils.round2(curEarned + delta),
                "totalPaid" to curPaid,
                "updatedAt" to FieldValue.serverTimestamp(),
              )
            )
          }
        }
      }.await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, txRef.path)
      throw e
    }
  }

  /**
   * Atomically cancels a transaction and reverses ALL stock, party balance,
   * cash/bank balance, and Agent/Mistri commission ledger effects inside one Firestore transaction.
   */
  suspend fun cancelTransaction(
    txId: String,
    reason: String = "Cancelled by user",
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val txRef = userDoc(uid).collection("transactions").document(txId)

    try {
      db.runTransaction { tr ->
        val txSnap = tr.get(txRef)
        val liveTx = txSnap.toObject(BusinessTransaction::class.java)
          ?: throw IllegalStateException("Transaction not found")
        if (liveTx.status == TxStatus.CANCELLED.name) {
          return@runTransaction
        }

        val txType = TxType.entries.firstOrNull { it.name == liveTx.txType } ?: TxType.SALE

        // Original stockSign that was applied when recording:
        val origStockSign = when (txType) {
          TxType.SALE, TxType.DEBIT_NOTE -> -1.0
          TxType.PURCHASE, TxType.CREDIT_NOTE -> 1.0
          else -> 0.0
        }
        val reverseQtyByItem: Map<String, Double> = if (origStockSign != 0.0) {
          liveTx.lineItems
            .filter { it.itemId.isNotBlank() && it.quantity > 0.0 }
            .groupBy { it.itemId }
            .mapValues { (_, lines) -> lines.sumOf { it.quantity } * (-origStockSign) }
        } else emptyMap()

        val origPartyDelta = if (liveTx.partyId.isNotBlank()) {
          when (txType) {
            TxType.SALE -> liveTx.balanceAmount
            TxType.PAYMENT_IN -> -liveTx.totalAmount
            TxType.CREDIT_NOTE -> -liveTx.totalAmount
            TxType.PURCHASE -> -liveTx.balanceAmount
            TxType.PAYMENT_OUT -> liveTx.totalAmount
            TxType.DEBIT_NOTE -> liveTx.totalAmount
            else -> 0.0
          }
        } else 0.0

        val origCashDelta = if (liveTx.accountId.isNotBlank()) {
          when (txType) {
            TxType.SALE -> liveTx.paidAmount
            TxType.PAYMENT_IN -> liveTx.totalAmount
            TxType.DEBIT_NOTE -> liveTx.paidAmount
            TxType.PURCHASE -> -liveTx.paidAmount
            TxType.PAYMENT_OUT -> -liveTx.totalAmount
            TxType.EXPENSE -> -liveTx.totalAmount
            TxType.CREDIT_NOTE -> -liveTx.paidAmount
            TxType.COMMISSION_PAYOUT -> -liveTx.paidAmount
            TxType.CASH_TRANSFER -> -liveTx.totalAmount
            else -> 0.0
          }
        } else 0.0

        val toAccountReverseDelta = if (txType == TxType.CASH_TRANSFER && liveTx.toAccountId.isNotBlank()) {
          -liveTx.totalAmount
        } else 0.0

        // ---- READS ----
        val itemSnaps = reverseQtyByItem.keys.associateWith { itemId ->
          tr.get(userDoc(uid).collection("items").document(itemId))
        }
        val partyRef = if (liveTx.partyId.isNotBlank() && abs(origPartyDelta) > 0.001) {
          userDoc(uid).collection("parties").document(liveTx.partyId)
        } else null
        val partySnap = partyRef?.let { tr.get(it) }

        val accRef = if (liveTx.accountId.isNotBlank() && abs(origCashDelta) > 0.001) {
          userDoc(uid).collection("accounts").document(liveTx.accountId)
        } else null
        val accSnap = accRef?.let { tr.get(it) }

        val toAccRef = if (liveTx.toAccountId.isNotBlank() && abs(toAccountReverseDelta) > 0.001) {
          userDoc(uid).collection("accounts").document(liveTx.toAccountId)
        } else null
        val toAccSnap = toAccRef?.let { tr.get(it) }

        val reverseSplits = when {
          liveTx.commissionSplits.isNotEmpty() -> liveTx.commissionSplits.filter { it.agentId.isNotBlank() && it.accruedCommission > 0.001 }
          liveTx.agentId.isNotBlank() && liveTx.commissionAccruedAmount > 0.001 -> listOf(
            CommissionSplit(agentId = liveTx.agentId, accruedCommission = liveTx.commissionAccruedAmount)
          )
          else -> emptyList()
        }

        val agentIdsToLoad = mutableSetOf<String>()
        if (txType == TxType.SALE || txType == TxType.CREDIT_NOTE) {
          reverseSplits.forEach { agentIdsToLoad.add(it.agentId) }
        } else if (txType == TxType.COMMISSION_PAYOUT && liveTx.agentId.isNotBlank()) {
          agentIdsToLoad.add(liveTx.agentId)
        }
        val agentSnaps = agentIdsToLoad.associateWith { agId ->
          tr.get(userDoc(uid).collection("agents").document(agId))
        }

        // ---- WRITES ----
        reverseQtyByItem.forEach { (itemId, reverseDelta) ->
          val snap = itemSnaps[itemId]
          if (snap != null && snap.exists() && snap.getString("itemType") != ItemType.SERVICE.name) {
            val curStock = snap.getDouble("currentStock") ?: 0.0
            tr.update(
              userDoc(uid).collection("items").document(itemId),
              mapOf(
                "currentStock" to MoneyUtils.round2(curStock + reverseDelta),
                "updatedAt" to FieldValue.serverTimestamp(),
              )
            )
          }
        }

        if (partyRef != null && partySnap != null && partySnap.exists()) {
          val curBal = partySnap.getDouble("currentBalance") ?: 0.0
          tr.update(
            partyRef,
            mapOf(
              "currentBalance" to MoneyUtils.round2(curBal - origPartyDelta),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
        }

        if (accRef != null && accSnap != null && accSnap.exists()) {
          val curAcc = accSnap.getDouble("currentBalance") ?: 0.0
          tr.update(
            accRef,
            mapOf(
              "currentBalance" to MoneyUtils.round2(curAcc - origCashDelta),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
        }

        if (toAccRef != null && toAccSnap != null && toAccSnap.exists()) {
          val curTo = toAccSnap.getDouble("currentBalance") ?: 0.0
          tr.update(
            toAccRef,
            mapOf(
              "currentBalance" to MoneyUtils.round2(curTo + toAccountReverseDelta),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
        }

        if (txType == TxType.SALE || txType == TxType.CREDIT_NOTE) {
          val reverseCommSign = if (txType == TxType.SALE) -1.0 else 1.0
          reverseSplits.forEach { sp ->
            val aSnap = agentSnaps[sp.agentId]
            if (aSnap != null && aSnap.exists()) {
              val curBal = aSnap.getDouble("currentBalance") ?: 0.0
              val curEarned = aSnap.getDouble("totalEarned") ?: 0.0
              val curPaid = aSnap.getDouble("totalPaid") ?: 0.0
              val delta = MoneyUtils.round2(sp.accruedCommission * reverseCommSign)
              tr.update(
                userDoc(uid).collection("agents").document(sp.agentId),
                mapOf(
                  "currentBalance" to MoneyUtils.round2(curBal + delta),
                  "totalEarned" to MoneyUtils.round2(curEarned + delta),
                  "totalPaid" to curPaid,
                  "updatedAt" to FieldValue.serverTimestamp(),
                )
              )
            }
          }
        } else if (txType == TxType.COMMISSION_PAYOUT && liveTx.agentId.isNotBlank()) {
          val aSnap = agentSnaps[liveTx.agentId]
          if (aSnap != null && aSnap.exists()) {
            val curBal = aSnap.getDouble("currentBalance") ?: 0.0
            val curEarned = aSnap.getDouble("totalEarned") ?: 0.0
            val curPaid = aSnap.getDouble("totalPaid") ?: 0.0
            tr.update(
              userDoc(uid).collection("agents").document(liveTx.agentId),
              mapOf(
                "currentBalance" to MoneyUtils.round2(curBal + liveTx.totalAmount),
                "totalEarned" to curEarned,
                "totalPaid" to MoneyUtils.round2((curPaid - liveTx.totalAmount).coerceAtLeast(0.0)),
                "updatedAt" to FieldValue.serverTimestamp(),
              )
            )
          }
        }

        val reversedSplits = liveTx.commissionSplits.map { s ->
          mapOf(
            "agentId" to s.agentId,
            "agentName" to s.agentName,
            "agentType" to s.agentType,
            "sharePercent" to s.sharePercent,
            "ratePercent" to s.ratePercent,
            "totalCommission" to s.totalCommission,
            "accruedCommission" to 0.0,
          )
        }
        val cancelFields = buildMap<String, Any> {
          put("status", TxStatus.CANCELLED.name)
          put("cancelReason", reason.trim().ifBlank { "Cancelled by user" }.take(300))
          put("cancelledAtMillis", System.currentTimeMillis())
          if (liveTx.commissionTotalAmount > 0.001 || liveTx.commissionStatus != CommissionStatus.NONE.name) {
            put("commissionStatus", CommissionStatus.REVERSED.name)
            put("commissionAccruedAmount", 0.0)
            put("commissionSplits", reversedSplits)
          }
          put("updatedAt", FieldValue.serverTimestamp())
        }
        tr.update(txRef, cancelFields)
      }.await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, txRef.path)
      throw e
    }
  }

  /**
   * Google Play compliant in-app account & cloud data deletion.
   * Step 1 deletes settings/profile (which unlocks transaction deletion in firestore.rules),
   * Step 2 batch-deletes all user subcollections, Step 3 deletes FirebaseAuth account.
   */
  suspend fun deleteAccountAndAllData(): Result<Unit> = runCatching {
    val user = auth.currentUser ?: throw IllegalStateException("No authenticated user")
    val uid = user.uid
    val uDoc = userDoc(uid)

    try {
      // 1. Delete settings/profile first
      val settingsSnap = uDoc.collection("settings").get().await()
      settingsSnap.documents.forEach { it.reference.delete().await() }

      // 2. Delete all user subcollections in chunks of 400
      val subcollections = listOf("transactions", "parties", "items", "accounts", "agents")
      for (colName in subcollections) {
        val docs = uDoc.collection(colName).get().await().documents
        docs.chunked(400).forEach { chunk ->
          val batch = db.batch()
          chunk.forEach { d -> batch.delete(d.reference) }
          batch.commit().await()
        }
      }

      // 3. Delete Firebase Auth account
      user.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, uDoc.path)
      throw e
    }
  }

  private fun logListenerError(
    error: Throwable,
    op: OperationType,
    path: String,
    uid: String,
  ) {
    val firestoreError =
      generateSequence(error) { it.cause }
        .filterIsInstance<FirebaseFirestoreException>()
        .firstOrNull()
    if (firestoreError == null && error is CancellationException) return
    val exceptionToLog = firestoreError ?: (error as? Exception)
    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
      handleFirestoreError(exceptionToLog, op, path)
    }
  }
}
