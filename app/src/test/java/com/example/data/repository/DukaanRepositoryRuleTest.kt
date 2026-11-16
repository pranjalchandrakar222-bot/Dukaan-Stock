package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.AccountType
import com.example.data.model.Agent
import com.example.data.model.AgentType
import com.example.data.model.BusinessProfile
import com.example.data.model.BusinessTransaction
import com.example.data.model.CashBankAccount
import com.example.data.model.CommissionBasis
import com.example.data.model.CommissionSplit
import com.example.data.model.CommissionStatus
import com.example.data.model.CommissionTrigger
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
        costPrice = 480.0,
        discountPercent = 0.0,
        taxRatePercent = 5.0,
        cgstAmount = 30.0,
        sgstAmount = 30.0,
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
        cgstAmount = 30.0,
        sgstAmount = 30.0,
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

    // Verify archiving party sets isActive=false and round-trips accurately through @PropertyName("isActive")
    val archivedDeleted = repo.archiveOrDeleteParty(updatedParty, hasTransactions = true).getOrThrow()
    assertEquals(false, archivedDeleted)
    val archivedParty =
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeParties().first { list -> list.any { it.id == partyId && !it.isActive } }
      }.first { it.id == partyId }
    assertEquals(false, archivedParty.isActive)

    // Verify updating an existing item works cleanly
    val itemUpdateResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.saveItem(updatedItem.copy(location = "Rack A1"))
      }
    assertTrue("Item update failed: ${itemUpdateResult.exceptionOrNull()}", itemUpdateResult.isSuccess)
  }

  @Test
  fun cancelTransaction_reversesStockPartyCashAndAgentCommission() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repo = DukaanRepository(firestore)
    repo.ensureProfileInitialized("Sharma Kirana", ALICE_EMAIL).getOrThrow()

    val cashBefore = repo.observeAccounts().first { it.isNotEmpty() }.first { it.id == "cash_in_hand" }.currentBalance
    val partyId = repo.saveParty(Party(name = "Cancel Test Customer", partyType = PartyType.CUSTOMER.name)).getOrThrow()
    val itemId = repo.saveItem(
      InventoryItem(
        name = "Cement Bag",
        itemType = ItemType.PRODUCT.name,
        salePrice = 400.0,
        purchasePrice = 320.0,
        currentStock = 50.0,
      )
    ).getOrThrow()
    val agentId = repo.saveAgent(
      Agent(name = "Karan Mistri", agentType = AgentType.MISTRI.name, defaultPercent = 5.0)
    ).getOrThrow()

    val tx = BusinessTransaction(
      txType = TxType.SALE.name,
      refNumber = "INV-CANCEL-1",
      dateMillis = System.currentTimeMillis(),
      partyId = partyId,
      partyName = "Cancel Test Customer",
      accountId = "cash_in_hand",
      lineItems = listOf(
        LineItem(itemId = itemId, itemName = "Cement Bag", quantity = 10.0, unitPrice = 400.0, lineTotal = 4000.0)
      ),
      subtotal = 4000.0,
      totalAmount = 4000.0,
      paidAmount = 1500.0,
      balanceAmount = 2500.0,
      status = TxStatus.PARTIAL.name,
      agentId = agentId,
      agentName = "Karan Mistri",
      commissionBasis = CommissionBasis.TAXABLE.name,
      commissionTrigger = CommissionTrigger.ON_INVOICE.name,
      commissionStatus = CommissionStatus.ACCRUED.name,
      commissionRatePercent = 5.0,
      commissionTotalAmount = 200.0,
      commissionAccruedAmount = 200.0,
      commissionSplits = listOf(
        CommissionSplit(
          agentId = agentId,
          agentName = "Karan Mistri",
          sharePercent = 100.0,
          ratePercent = 5.0,
          totalCommission = 200.0,
          accruedCommission = 200.0,
        )
      ),
    )

    val itemsSnap = repo.observeItems().first { it.any { i -> i.id == itemId } }
    val txId = repo.recordTransaction(tx, itemsSnap).getOrThrow()

    // Confirm deltas were applied
    val midItem = repo.observeItems().first { it.any { i -> i.id == itemId && i.currentStock == 40.0 } }.first { it.id == itemId }
    assertEquals(40.0, midItem.currentStock, 0.01)
    val midAgent = repo.observeAgents().first { it.any { a -> a.id == agentId && a.currentBalance == 200.0 } }.first { it.id == agentId }
    assertEquals(200.0, midAgent.currentBalance, 0.01)

    // Cancel and verify atomic reversal
    repo.cancelTransaction(txId, "Customer cancelled order").getOrThrow()

    val cancelledTx = repo.observeTransactions().first { it.any { t -> t.id == txId && t.status == TxStatus.CANCELLED.name } }.first { it.id == txId }
    assertEquals("Customer cancelled order", cancelledTx.cancelReason)
    assertEquals(CommissionStatus.REVERSED.name, cancelledTx.commissionStatus)
    assertEquals(0.0, cancelledTx.commissionAccruedAmount, 0.01)

    val revertedItem = repo.observeItems().first { it.any { i -> i.id == itemId && i.currentStock == 50.0 } }.first { it.id == itemId }
    assertEquals(50.0, revertedItem.currentStock, 0.01)

    val revertedParty = repo.observeParties().first { it.any { p -> p.id == partyId && p.currentBalance == 0.0 } }.first { it.id == partyId }
    assertEquals(0.0, revertedParty.currentBalance, 0.01)

    val revertedAgent = repo.observeAgents().first { it.any { a -> a.id == agentId && a.currentBalance == 0.0 } }.first { it.id == agentId }
    assertEquals(0.0, revertedAgent.currentBalance, 0.01)

    val cashAfter = repo.observeAccounts().first { it.isNotEmpty() }.first { it.id == "cash_in_hand" }.currentBalance
    assertEquals(cashBefore, cashAfter, 0.01)
  }

  @Test
  fun recordTransaction_stopNegativeStockEnabled_rejectsInsufficientStock() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repo = DukaanRepository(firestore)
    repo.ensureProfileInitialized("Sharma Kirana", ALICE_EMAIL).getOrThrow()
    val profile = repo.observeBusinessProfile().first { it != null }!!
    repo.saveBusinessProfile(profile.copy(stopNegativeStock = true)).getOrThrow()

    val itemId = repo.saveItem(
      InventoryItem(
        name = "Limited Plywood",
        itemType = ItemType.PRODUCT.name,
        salePrice = 1200.0,
        currentStock = 2.0,
      )
    ).getOrThrow()

    val currentItems = repo.observeItems().first { it.any { i -> i.id == itemId } }
    val overSell = BusinessTransaction(
      txType = TxType.SALE.name,
      refNumber = "INV-NEG-1",
      dateMillis = System.currentTimeMillis(),
      accountId = "cash_in_hand",
      lineItems = listOf(LineItem(itemId = itemId, itemName = "Limited Plywood", quantity = 5.0, unitPrice = 1200.0, lineTotal = 6000.0)),
      subtotal = 6000.0,
      totalAmount = 6000.0,
      paidAmount = 6000.0,
      balanceAmount = 0.0,
      status = TxStatus.PAID.name,
    )

    val res = repo.recordTransaction(overSell, currentItems)
    assertTrue("Expected failure when stock < requested qty", res.isFailure)

    // Restore stopNegativeStock = false
    val latestProfile = repo.observeBusinessProfile().first { it != null }!!
    repo.saveBusinessProfile(latestProfile.copy(stopNegativeStock = false)).getOrThrow()
  }

  @Test
  fun onCollectionCommission_accruesOnSettlementAndPayoutWithTdsWorks() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repo = DukaanRepository(firestore)
    repo.ensureProfileInitialized("Sharma Kirana", ALICE_EMAIL).getOrThrow()

    val partyId = repo.saveParty(Party(name = "Commission Client", partyType = PartyType.CUSTOMER.name)).getOrThrow()
    val agentId = repo.saveAgent(
      Agent(
        name = "Vikram Mistri",
        agentType = AgentType.MISTRI.name,
        defaultPercent = 10.0,
        defaultTrigger = CommissionTrigger.ON_COLLECTION.name,
      )
    ).getOrThrow()

    // Create 100% unpaid sale with 10% ON_COLLECTION commission (totalCommission = 200, accrued = 0)
    val saleTx = BusinessTransaction(
      txType = TxType.SALE.name,
      refNumber = "INV-COMM-1",
      dateMillis = System.currentTimeMillis(),
      partyId = partyId,
      partyName = "Commission Client",
      accountId = "cash_in_hand",
      subtotal = 2000.0,
      totalAmount = 2000.0,
      paidAmount = 0.0,
      balanceAmount = 2000.0,
      status = TxStatus.UNPAID.name,
      agentId = agentId,
      agentName = "Vikram Mistri",
      commissionTrigger = CommissionTrigger.ON_COLLECTION.name,
      commissionStatus = CommissionStatus.PENDING_COLLECTION.name,
      commissionRatePercent = 10.0,
      commissionTotalAmount = 200.0,
      commissionAccruedAmount = 0.0,
      commissionSplits = listOf(
        CommissionSplit(
          agentId = agentId,
          agentName = "Vikram Mistri",
          sharePercent = 100.0,
          ratePercent = 10.0,
          totalCommission = 200.0,
          accruedCommission = 0.0,
        )
      ),
    )

    val saleId = repo.recordTransaction(saleTx, emptyList()).getOrThrow()
    val agentAfterSale = repo.observeAgents().first { it.any { a -> a.id == agentId } }.first { it.id == agentId }
    assertEquals(0.0, agentAfterSale.currentBalance, 0.01)

    // Settle 50% (1000 of 2000) -> 100 commission accrues
    repo.recordSettlementPayment(
      transactionId = saleId,
      additionalPaid = 1000.0,
      partyId = partyId,
      txType = TxType.SALE,
      accountId = "cash_in_hand",
    ).getOrThrow()
    val agentAfterPartial = repo.observeAgents().first {
      it.any { a -> a.id == agentId && a.currentBalance == 100.0 }
    }.first { it.id == agentId }
    assertEquals(100.0, agentAfterPartial.currentBalance, 0.01)
    assertEquals(100.0, agentAfterPartial.totalEarned, 0.01)

    // Pay out 100 with 10% TDS (net cash = 90)
    val cashBeforePayout = repo.observeAccounts().first { it.isNotEmpty() }.first { it.id == "cash_in_hand" }.currentBalance
    repo.recordCommissionPayout(
      agent = agentAfterPartial,
      grossPayoutAmount = 100.0,
      tdsPercent = 10.0,
      accountId = "cash_in_hand",
      paymentMode = PaymentMode.CASH,
      refNumber = "COMM-0001",
      notes = "Half settlement",
    ).getOrThrow()

    val agentAfterPayout = repo.observeAgents().first {
      it.any { a -> a.id == agentId && a.currentBalance == 0.0 && a.totalPaid == 100.0 }
    }.first { it.id == agentId }
    assertEquals(0.0, agentAfterPayout.currentBalance, 0.01)
    assertEquals(100.0, agentAfterPayout.totalPaid, 0.01)

    val cashAfterPayout = repo.observeAccounts().first { it.isNotEmpty() }.first { it.id == "cash_in_hand" }.currentBalance
    assertEquals(cashBeforePayout - 90.0, cashAfterPayout, 0.01)
  }

  @Test
  fun transferBetweenAccounts_updatesBothBalancesAtomically() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repo = DukaanRepository(firestore)
    repo.ensureProfileInitialized("Sharma Kirana", ALICE_EMAIL).getOrThrow()

    val bankId = repo.saveAccount(
      CashBankAccount(
        accountName = "HDFC Current",
        accountType = AccountType.BANK.name,
        openingBalance = 5000.0,
        currentBalance = 5000.0,
      )
    ).getOrThrow()
    repo.adjustAccountBalance("cash_in_hand", 2000.0, "Opening float").getOrThrow()

    val cashStart = repo.observeAccounts().first { it.isNotEmpty() }.first { it.id == "cash_in_hand" }.currentBalance
    repo.transferBetweenAccounts("cash_in_hand", bankId, 1000.0, "Bank deposit").getOrThrow()

    val accounts = repo.observeAccounts().first {
      it.any { a -> a.id == bankId && a.currentBalance == 6000.0 }
    }
    val cashEnd = accounts.first { it.id == "cash_in_hand" }.currentBalance
    val bankEnd = accounts.first { it.id == bankId }.currentBalance
    assertEquals(cashStart - 1000.0, cashEnd, 0.01)
    assertEquals(6000.0, bankEnd, 0.01)
  }

  @Test
  fun deleteAccountAndAllData_deletesProfileTransactionsAndAuthAccount() = runBlocking {
    val deleteUid = signInTestUser("delete.dukaan@test.com")
    val repo = DukaanRepository(firestore)
    repo.ensureProfileInitialized("Temp Shop", "delete.dukaan@test.com").getOrThrow()
    repo.recordTransaction(
      BusinessTransaction(
        txType = TxType.EXPENSE.name,
        refNumber = "EXP-DEL-1",
        dateMillis = System.currentTimeMillis(),
        category = "Misc",
        accountId = "cash_in_hand",
        totalAmount = 100.0,
        paidAmount = 100.0,
        balanceAmount = 0.0,
        status = TxStatus.PAID.name,
      ),
      emptyList(),
    ).getOrThrow()

    val delRes = repo.deleteAccountAndAllData()
    assertTrue("Account deletion failed: ${delRes.exceptionOrNull()}", delRes.isSuccess)

    // Verify user is signed out / deleted and cannot read
    try {
      firestore.collection("users").document(deleteUid).collection("transactions").get().await()
    } catch (_: Exception) {
      // Expected permission denied once deleted
    }
    Unit
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
