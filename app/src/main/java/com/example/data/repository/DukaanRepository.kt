package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AccountType
import com.example.data.model.BusinessProfile
import com.example.data.model.BusinessTransaction
import com.example.data.model.CashBankAccount
import com.example.data.model.InventoryItem
import com.example.data.model.ItemType
import com.example.data.model.Party
import com.example.data.model.ThemeStyle
import com.example.data.model.TxStatus
import com.example.data.model.TxType
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class DukaanRepository(private val db: FirebaseFirestore) {

  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private val auth = Firebase.auth

  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

  private fun userDoc(uid: String) = db.collection("users").document(uid)

  suspend fun ensureProfileInitialized(
    defaultBusinessName: String,
    defaultEmail: String,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val profileRef = userDoc(uid).collection("settings").document("profile")
    try {
      val snapshot = profileRef.get().await()
      if (!snapshot.exists()) {
        val payload =
          mapOf(
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
            "bankDetails" to "",
            "invoiceTerms" to "Thanks for doing business with us!",
            "themeStyle" to ThemeStyle.DYNAMIC.name,
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
        cashRef
          .set(
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
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
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
      ref
        .snapshots()
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
        ref
          .set(
            profile.copy(
              id = "profile",
              userId = uid,
              createdAt = existing.getTimestamp("createdAt") ?: profile.createdAt,
              updatedAt = null,
            )
          )
          .await()
      } else {
        ref
          .set(
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
              "bankDetails" to profile.bankDetails,
              "invoiceTerms" to profile.invoiceTerms,
              "themeStyle" to profile.themeStyle.ifBlank { ThemeStyle.DYNAMIC.name },
              "stopNegativeStock" to profile.stopNegativeStock,
              "roundOffTotal" to profile.roundOffTotal,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
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
      ref
        .orderBy("name", Query.Direction.ASCENDING)
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
        docRef
          .set(
            mapOf(
              "userId" to uid,
              "name" to party.name.trim(),
              "partyType" to party.partyType,
              "phone" to party.phone.trim(),
              "email" to party.email.trim(),
              "gstin" to party.gstin.trim().uppercase(),
              "billingAddress" to party.billingAddress.trim(),
              "shippingAddress" to party.shippingAddress.trim(),
              "openingBalance" to party.openingBalance,
              "currentBalance" to party.currentBalance,
              "notes" to party.notes.trim(),
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      } else {
        requireNotNull(party.createdAt) { "Party createdAt must be non-null for update" }
        docRef.set(party.copy(userId = uid, updatedAt = null)).await()
      }
      docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, if (isCreate) OperationType.CREATE else OperationType.UPDATE, docRef.path)
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
      ref
        .orderBy("name", Query.Direction.ASCENDING)
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
        docRef
          .set(
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
              "openingStock" to item.openingStock,
              "currentStock" to item.currentStock,
              "minStockLevel" to item.minStockLevel,
              "location" to item.location.trim(),
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      } else {
        requireNotNull(item.createdAt) { "Item createdAt must be non-null for update" }
        docRef.set(item.copy(userId = uid, updatedAt = null)).await()
      }
      docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, if (isCreate) OperationType.CREATE else OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun adjustItemStock(itemId: String, deltaQty: Double): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = userDoc(uid).collection("items").document(itemId)
    try {
      docRef
        .update(
          mapOf(
            "currentStock" to FieldValue.increment(deltaQty),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
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
      ref
        .orderBy("accountName", Query.Direction.ASCENDING)
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
        docRef
          .set(
            mapOf(
              "userId" to uid,
              "accountName" to account.accountName.trim(),
              "accountType" to account.accountType,
              "bankName" to account.bankName.trim(),
              "accountNumber" to account.accountNumber.trim(),
              "ifscCode" to account.ifscCode.trim().uppercase(),
              "upiId" to account.upiId.trim(),
              "openingBalance" to account.openingBalance,
              "currentBalance" to account.currentBalance,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      } else {
        requireNotNull(account.createdAt) { "Account createdAt must be non-null for update" }
        docRef.set(account.copy(userId = uid, updatedAt = null)).await()
      }
      docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, if (isCreate) OperationType.CREATE else OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun adjustAccountBalance(accountId: String, delta: Double): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = userDoc(uid).collection("accounts").document(accountId)
    try {
      docRef
        .update(
          mapOf(
            "currentBalance" to FieldValue.increment(delta),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  fun observeTransactions(): Flow<List<BusinessTransaction>> = flow {
    val uid = requireUserId()
    val ref = userDoc(uid).collection("transactions")
    emitAll(
      ref
        .orderBy("dateMillis", Query.Direction.DESCENDING)
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

  suspend fun recordTransaction(
    tx: BusinessTransaction,
    knownItems: List<InventoryItem>,
    incrementInvoiceSequence: Boolean = false,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val txRef = userDoc(uid).collection("transactions").document()
    val txType = TxType.entries.firstOrNull { it.name == tx.txType } ?: TxType.SALE
    val batch = db.batch()

    val lineMaps =
      tx.lineItems.map { line ->
        mapOf(
          "itemId" to line.itemId,
          "itemName" to line.itemName,
          "hsnSac" to line.hsnSac,
          "unit" to line.unit,
          "quantity" to line.quantity,
          "unitPrice" to line.unitPrice,
          "discountPercent" to line.discountPercent,
          "taxRatePercent" to line.taxRatePercent,
          "taxAmount" to line.taxAmount,
          "lineTotal" to line.lineTotal,
        )
      }

    val txPayload =
      mapOf(
        "userId" to uid,
        "txType" to tx.txType,
        "refNumber" to tx.refNumber.trim(),
        "dateMillis" to if (tx.dateMillis > 0L) tx.dateMillis else System.currentTimeMillis(),
        "partyId" to tx.partyId,
        "partyName" to tx.partyName.trim(),
        "category" to tx.category.trim(),
        "paymentMode" to tx.paymentMode,
        "accountId" to tx.accountId,
        "lineItems" to lineMaps,
        "subtotal" to tx.subtotal,
        "discountAmount" to tx.discountAmount,
        "taxAmount" to tx.taxAmount,
        "roundOffAmount" to tx.roundOffAmount,
        "totalAmount" to tx.totalAmount,
        "paidAmount" to tx.paidAmount,
        "balanceAmount" to tx.balanceAmount,
        "status" to tx.status,
        "notes" to tx.notes.trim(),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    batch.set(txRef, txPayload)

    // 1. Inventory stock delta for product line items
    val stockSign =
      when (txType) {
        TxType.SALE, TxType.DEBIT_NOTE -> -1.0
        TxType.PURCHASE, TxType.CREDIT_NOTE -> 1.0
        else -> 0.0
      }
    if (stockSign != 0.0) {
      val qtyByItem =
        tx.lineItems
          .filter { it.itemId.isNotBlank() && it.quantity > 0.0 }
          .groupBy { it.itemId }
      qtyByItem.forEach { (itemId, lines) ->
        val matched = knownItems.firstOrNull { it.id == itemId }
        if (matched == null || matched.itemType == ItemType.PRODUCT.name) {
          val qtySum = lines.sumOf { it.quantity }
          val itemRef = userDoc(uid).collection("items").document(itemId)
          batch.update(
            itemRef,
            mapOf(
              "currentStock" to FieldValue.increment(qtySum * stockSign),
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
        }
      }
    }

    // 2. Party balance delta (+ = Customer owes us more, - = We owe supplier more / customer paid)
    if (tx.partyId.isNotBlank()) {
      val partyDelta =
        when (txType) {
          TxType.SALE -> tx.balanceAmount
          TxType.PAYMENT_IN -> -tx.totalAmount
          TxType.CREDIT_NOTE -> -tx.totalAmount
          TxType.PURCHASE -> -tx.balanceAmount
          TxType.PAYMENT_OUT -> tx.totalAmount
          TxType.DEBIT_NOTE -> tx.totalAmount
          else -> 0.0
        }
      if (kotlin.math.abs(partyDelta) > 0.001) {
        val partyRef = userDoc(uid).collection("parties").document(tx.partyId)
        batch.update(
          partyRef,
          mapOf(
            "currentBalance" to FieldValue.increment(partyDelta),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }
    }

    // 3. Cash / Bank Account delta
    if (tx.accountId.isNotBlank()) {
      val cashDelta =
        when (txType) {
          TxType.SALE -> tx.paidAmount
          TxType.PAYMENT_IN -> tx.totalAmount
          TxType.DEBIT_NOTE -> tx.paidAmount
          TxType.PURCHASE -> -tx.paidAmount
          TxType.PAYMENT_OUT -> -tx.totalAmount
          TxType.EXPENSE -> -tx.totalAmount
          TxType.CREDIT_NOTE -> -tx.paidAmount
          else -> 0.0
        }
      if (kotlin.math.abs(cashDelta) > 0.001) {
        val accRef = userDoc(uid).collection("accounts").document(tx.accountId)
        batch.update(
          accRef,
          mapOf(
            "currentBalance" to FieldValue.increment(cashDelta),
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        )
      }
    }

    // 4. Optional invoice sequence increment
    if (incrementInvoiceSequence) {
      val profileRef = userDoc(uid).collection("settings").document("profile")
      batch.update(
        profileRef,
        mapOf(
          "nextInvoiceNumber" to FieldValue.increment(1L),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
    }

    try {
      batch.commit().await()
      txRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, txRef.path)
      throw e
    }
  }

  suspend fun recordSettlementPayment(
    transactionId: String,
    additionalPaid: Double,
    newTotalPaid: Double,
    newBalance: Double,
    newStatus: TxStatus,
    partyId: String,
    txType: TxType,
    accountId: String,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val txRef = userDoc(uid).collection("transactions").document(transactionId)
    val batch = db.batch()
    batch.update(
      txRef,
      mapOf(
        "paidAmount" to newTotalPaid,
        "balanceAmount" to newBalance,
        "status" to newStatus.name,
        "updatedAt" to FieldValue.serverTimestamp(),
      ),
    )

    if (partyId.isNotBlank()) {
      val partyDelta = if (txType == TxType.SALE) -additionalPaid else additionalPaid
      batch.update(
        userDoc(uid).collection("parties").document(partyId),
        mapOf(
          "currentBalance" to FieldValue.increment(partyDelta),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
    }

    if (accountId.isNotBlank()) {
      val cashDelta = if (txType == TxType.SALE) additionalPaid else -additionalPaid
      batch.update(
        userDoc(uid).collection("accounts").document(accountId),
        mapOf(
          "currentBalance" to FieldValue.increment(cashDelta),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
    }

    try {
      batch.commit().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, txRef.path)
      throw e
    }
  }

  suspend fun cancelTransaction(txId: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val txRef = userDoc(uid).collection("transactions").document(txId)
    try {
      txRef
        .update(
          mapOf(
            "status" to TxStatus.CANCELLED.name,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, txRef.path)
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
