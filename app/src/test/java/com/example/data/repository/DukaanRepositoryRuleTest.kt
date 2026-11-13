package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.BusinessTransaction
import com.example.data.model.InventoryItem
import com.example.data.model.ItemType
import com.example.data.model.LineItem
import com.example.data.model.Party
import com.example.data.model.PartyType
import com.example.data.model.PaymentMode
import com.example.data.model.TxStatus
import com.example.data.model.TxType
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class DukaanRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun ensureProfileInitialized_createsProfileAndDefaultCashAccount() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repo = DukaanRepository(firestore)

    val initResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.ensureProfileInitialized("Sharma Kirana", ALICE_EMAIL)
      }
    assertTrue("Profile init failed: ${initResult.exceptionOrNull()}", initResult.isSuccess)

    val profile =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeBusinessProfile().first { it != null }
      }
    assertNotNull(profile)

    val accounts =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeAccounts().first { it.isNotEmpty() }
      }
    assertTrue(accounts.any { it.id == "cash_in_hand" && it.accountName == "Cash in Hand" })
  }

  @Test
  fun savePartySaveItemAndRecordSale_updatesStockAndBalancesAtomically() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repo = DukaanRepository(firestore)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.ensureProfileInitialized("Sharma Kirana", ALICE_EMAIL).getOrThrow()
    }

    val partyId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo
          .saveParty(
            Party(
              name = "Verma Enterprises",
              partyType = PartyType.CUSTOMER.name,
              phone = "9876543210",
              openingBalance = 0.0,
              currentBalance = 0.0,
            )
          )
          .getOrThrow()
      }
    assertTrue(partyId.isNotEmpty())

    val itemId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo
          .saveItem(
            InventoryItem(
              name = "Premium Basmati 5KG",
              itemType = ItemType.PRODUCT.name,
              unit = "BAG",
              salePrice = 600.0,
              purchasePrice = 480.0,
              taxRatePercent = 5.0,
              openingStock = 20.0,
              currentStock = 20.0,
              minStockLevel = 5.0,
            )
          )
          .getOrThrow()
      }
    assertTrue(itemId.isNotEmpty())

    val lineItem =
      LineItem(
        itemId = itemId,
        itemName = "Premium Basmati 5KG",
        unit = "BAG",
        quantity = 2.0,
        unitPrice = 600.0,
        discountPercent = 0.0,
        taxRatePercent = 5.0,
        taxAmount = 60.0,
        lineTotal = 1260.0,
      )
    val tx =
      BusinessTransaction(
        txType = TxType.SALE.name,
        refNumber = "INV-1",
        dateMillis = System.currentTimeMillis(),
        partyId = partyId,
        partyName = "Verma Enterprises",
        paymentMode = PaymentMode.CASH.name,
        accountId = "cash_in_hand",
        lineItems = listOf(lineItem),
        subtotal = 1200.0,
        discountAmount = 0.0,
        taxAmount = 60.0,
        roundOffAmount = 0.0,
        totalAmount = 1260.0,
        paidAmount = 1000.0,
        balanceAmount = 260.0,
        status = TxStatus.PARTIAL.name,
      )

    val currentItems =
      withTimeout(FLOW_TIMEOUT_MS) { repo.observeItems().first { list -> list.any { it.id == itemId } } }
    val txId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.recordTransaction(tx, currentItems, incrementInvoiceSequence = true).getOrThrow()
      }
    assertTrue(txId.isNotEmpty())

    val updatedItem =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeItems().first { list -> list.any { it.id == itemId && it.currentStock == 18.0 } }
      }.first { it.id == itemId }
    assertEquals(18.0, updatedItem.currentStock, 0.01)

    val updatedParty =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeParties().first { list -> list.any { it.id == partyId && it.currentBalance == 260.0 } }
      }.first { it.id == partyId }
    assertEquals(260.0, updatedParty.currentBalance, 0.01)

    // Verify updating the existing party preserves createdAt and succeeds under strict rules
    val updateResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.saveParty(updatedParty.copy(notes = "Verified wholesale buyer"))
      }
    assertTrue("Party update failed: ${updateResult.exceptionOrNull()}", updateResult.isSuccess)
  }

  @Test
  fun crossUserRead_alicePartiesByBob_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = DukaanRepository(firestore)
    val partyId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        aliceRepo
          .saveParty(Party(name = "Private Customer", partyType = PartyType.CUSTOMER.name))
          .getOrThrow()
      }

    signInTestUser(BOB_EMAIL)
    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore
          .collection("users")
          .document(aliceUid)
          .collection("parties")
          .document(partyId)
          .get()
          .await()
      }
      fail("Expected FirebaseFirestoreException PERMISSION_DENIED for cross-user read")
    } catch (e: Exception) {
      val fsEx = e.unwrapFirestoreException()
      assertNotNull("Expected FirebaseFirestoreException, got $e", fsEx)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx!!.code)
    }
  }

  @Test
  fun unauthenticatedObserverAndDirectRead_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    auth.signOut()

    try {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore.collection("users").document(aliceUid).collection("parties").get().await()
      }
      fail("Expected FirebaseFirestoreException PERMISSION_DENIED when signed out")
    } catch (e: Exception) {
      val fsEx = e.unwrapFirestoreException()
      assertNotNull("Expected FirebaseFirestoreException, got $e", fsEx)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx!!.code)
    }
  }

  private companion object {
    const val ALICE_EMAIL = "alice.dukaan@test.com"
    const val BOB_EMAIL = "bob.dukaan@test.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
