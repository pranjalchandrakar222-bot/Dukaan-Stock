package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.data.model.Agent
import com.example.data.model.BusinessProfile
import com.example.data.model.BusinessTransaction
import com.example.data.model.CashBankAccount
import com.example.data.model.CommissionBasis
import com.example.data.model.CommissionTrigger
import com.example.data.model.InventoryItem
import com.example.data.model.Party
import com.example.data.model.PaymentMode
import com.example.data.model.TxType
import com.example.data.model.defaultBasisEnum
import com.example.data.model.defaultTriggerEnum
import com.example.ui.util.LineCalculationInput
import com.example.ui.util.TaxAndCommissionCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

data class DraftLine(
  val rowId: String = UUID.randomUUID().toString(),
  val itemId: String = "",
  val itemName: String = "",
  val hsnSac: String = "",
  val unit: String = "PCS",
  val qtyText: String = "1",
  val priceText: String = "",
  val costPrice: Double = 0.0,
  val priceIncludesTax: Boolean = false,
  val discountText: String = "0",
  val taxRate: Double = 0.0,
  val itemCommissionPercent: Double = 0.0,
  val availableStock: Double = 0.0,
  val isProduct: Boolean = true,
) {
  fun toCalculationInput(): LineCalculationInput =
    LineCalculationInput(
      itemId = itemId,
      itemName = itemName,
      hsnSac = hsnSac,
      unit = unit.ifBlank { "PCS" },
      quantity = qtyText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
      unitPrice = priceText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
      costPrice = costPrice,
      priceIncludesTax = priceIncludesTax,
      discountPercent = discountText.toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 0.0,
      taxRatePercent = taxRate,
      itemCommissionPercent = itemCommissionPercent,
    )
}

data class DraftAgentSplit(
  val agentId: String,
  val agentName: String,
  val agentType: String,
  val sharePercentText: String = "100",
  val overrideRateText: String = "",
)

data class InvoiceDraftState(
  val draftKey: String = "",
  val txType: TxType = TxType.SALE,
  val refNumber: String = "",
  val refManuallyEdited: Boolean = false,
  val dateMillis: Long = System.currentTimeMillis(),
  val dueDateMillis: Long = 0L,
  val partyId: String = "",
  val partyName: String = "",
  val partyGstin: String = "",
  val placeOfSupply: String = "",
  val isInterState: Boolean = false,
  val interStateManuallySet: Boolean = false,
  val useWholesalePrice: Boolean = false,
  val lines: List<DraftLine> = listOf(DraftLine()),
  val billDiscountText: String = "",
  val extraChargesLabel: String = "",
  val extraChargesAmountText: String = "",
  val roundOffEnabled: Boolean = true,
  val paymentMode: PaymentMode = PaymentMode.CASH,
  val selectedAccountId: String = "",
  val paidText: String = "",
  val fullyPaidChecked: Boolean = true,
  val notes: String = "",
  val linkedTxId: String = "",
  val agentSplits: List<DraftAgentSplit> = emptyList(),
  val commissionBasis: CommissionBasis = CommissionBasis.TAXABLE,
  val commissionTrigger: CommissionTrigger = CommissionTrigger.ON_INVOICE,
  val manualCommissionRateText: String = "",
)

class InvoiceDraftViewModel : ViewModel() {

  private val _state = MutableStateFlow(InvoiceDraftState())
  val state: StateFlow<InvoiceDraftState> = _state.asStateFlow()

  fun initializeIfNeeded(
    draftKey: String,
    initialTxType: TxType,
    initialParty: Party?,
    profile: BusinessProfile?,
    accounts: List<CashBankAccount>,
    agents: List<Agent>,
    items: List<InventoryItem>,
    convertingFromTx: BusinessTransaction? = null,
    creditNoteForSale: BusinessTransaction? = null,
  ) {
    if (_state.value.draftKey == draftKey) return

    val now = System.currentTimeMillis()
    val sourceTx = convertingFromTx ?: creditNoteForSale
    val targetTxType = when {
      creditNoteForSale != null -> TxType.CREDIT_NOTE
      convertingFromTx != null -> TxType.SALE
      else -> initialTxType
    }

    val defaultRef = TaxAndCommissionCalculator.formatDocumentRef(targetTxType, profile, now)
    val defaultAccId = accounts.firstOrNull { it.isActive }?.id ?: accounts.firstOrNull()?.id ?: ""

    if (sourceTx != null) {
      val mappedLines = sourceTx.lineItems.map { line ->
        val matchedItem = items.firstOrNull { it.id == line.itemId }
        DraftLine(
          itemId = line.itemId,
          itemName = line.itemName,
          hsnSac = line.hsnSac,
          unit = line.unit,
          qtyText = line.quantity.toString().removeSuffix(".0"),
          priceText = line.unitPrice.toString(),
          costPrice = if (line.costPrice > 0) line.costPrice else matchedItem?.purchasePrice ?: 0.0,
          priceIncludesTax = line.priceIncludesTax,
          discountText = line.discountPercent.toString(),
          taxRate = line.taxRatePercent,
          itemCommissionPercent = matchedItem?.commissionPercent ?: line.commissionPercent,
          availableStock = matchedItem?.currentStock ?: 0.0,
          isProduct = matchedItem?.itemType != "SERVICE",
        )
      }.ifEmpty { listOf(DraftLine()) }

      val mappedSplits = if (sourceTx.commissionSplits.isNotEmpty()) {
        sourceTx.commissionSplits.mapNotNull { sp ->
          val ag = agents.firstOrNull { it.id == sp.agentId } ?: return@mapNotNull null
          DraftAgentSplit(
            agentId = ag.id,
            agentName = ag.name,
            agentType = ag.agentType,
            sharePercentText = sp.sharePercent.toString().removeSuffix(".0"),
          )
        }
      } else if (sourceTx.agentId.isNotBlank()) {
        val ag = agents.firstOrNull { it.id == sourceTx.agentId }
        if (ag != null) {
          listOf(DraftAgentSplit(ag.id, ag.name, ag.agentType, "100"))
        } else emptyList()
      } else emptyList()

      val pos = sourceTx.placeOfSupply.ifBlank { profile?.stateName.orEmpty() }
      val linkedParty = initialParty

      _state.value = InvoiceDraftState(
        draftKey = draftKey,
        txType = targetTxType,
        refNumber = defaultRef,
        dateMillis = now,
        dueDateMillis = 0L,
        partyId = sourceTx.partyId,
        partyName = sourceTx.partyName,
        partyGstin = linkedParty?.gstin.orEmpty(),
        placeOfSupply = pos,
        isInterState = sourceTx.isInterState,
        interStateManuallySet = sourceTx.isInterState,
        lines = mappedLines,
        billDiscountText = sourceTx.billDiscountAmount.takeIf { it > 0 }?.toString() ?: "",
        extraChargesLabel = sourceTx.extraChargesLabel,
        extraChargesAmountText = sourceTx.extraChargesAmount.takeIf { it > 0 }?.toString() ?: "",
        roundOffEnabled = profile?.roundOffTotal ?: true,
        paymentMode = PaymentMode.entries.firstOrNull { it.name == sourceTx.paymentMode } ?: PaymentMode.CASH,
        selectedAccountId = sourceTx.accountId.ifBlank { defaultAccId },
        fullyPaidChecked = creditNoteForSale == null,
        notes = if (creditNoteForSale != null) {
          "Return against ${creditNoteForSale.refNumber}"
        } else {
          "Converted from ${sourceTx.refNumber}"
        },
        linkedTxId = sourceTx.id,
        agentSplits = mappedSplits,
        commissionBasis = CommissionBasis.entries.firstOrNull { it.name == sourceTx.commissionBasis }
          ?: CommissionBasis.TAXABLE,
        commissionTrigger = CommissionTrigger.entries.firstOrNull { it.name == sourceTx.commissionTrigger }
          ?: CommissionTrigger.ON_INVOICE,
      )
      return
    }

    val initPos = initialParty?.stateName?.ifBlank {
      TaxAndCommissionCalculator.stateFromGstin(initialParty.gstin)
    } ?: profile?.stateName.orEmpty()

    val isInter = TaxAndCommissionCalculator.detectInterState(
      businessState = profile?.stateName.orEmpty(),
      businessGstin = profile?.gstin.orEmpty(),
      partyStateOrPlaceOfSupply = initPos,
      partyGstin = initialParty?.gstin.orEmpty(),
    )

    val defaultAgent = initialParty?.defaultAgentId
      ?.takeIf { it.isNotBlank() }
      ?.let { agId -> agents.firstOrNull { it.id == agId && it.isActive } }

    val initSplits = if (defaultAgent != null) {
      listOf(DraftAgentSplit(defaultAgent.id, defaultAgent.name, defaultAgent.agentType, "100"))
    } else emptyList()

    _state.value = InvoiceDraftState(
      draftKey = draftKey,
      txType = targetTxType,
      refNumber = defaultRef,
      dateMillis = now,
      dueDateMillis = 0L,
      partyId = initialParty?.id.orEmpty(),
      partyName = initialParty?.name.orEmpty(),
      partyGstin = initialParty?.gstin.orEmpty(),
      placeOfSupply = initPos,
      isInterState = isInter,
      roundOffEnabled = profile?.roundOffTotal ?: true,
      selectedAccountId = defaultAccId,
      agentSplits = initSplits,
      commissionBasis = defaultAgent?.defaultBasisEnum() ?: CommissionBasis.TAXABLE,
      commissionTrigger = defaultAgent?.defaultTriggerEnum() ?: CommissionTrigger.ON_INVOICE,
    )
  }

  fun update(transform: (InvoiceDraftState) -> InvoiceDraftState) {
    _state.update(transform)
  }

  fun clearDraft() {
    _state.value = InvoiceDraftState()
  }
}
