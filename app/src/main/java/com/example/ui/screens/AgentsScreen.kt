package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Agent
import com.example.data.model.AgentType
import com.example.data.model.BusinessProfile
import com.example.data.model.BusinessTransaction
import com.example.data.model.CashBankAccount
import com.example.data.model.CommissionBasis
import com.example.data.model.CommissionStatus
import com.example.data.model.CommissionTrigger
import com.example.data.model.PaymentMode
import com.example.data.model.TxStatus
import com.example.data.model.TxType
import com.example.data.model.agentTypeEnum
import com.example.data.model.commissionStatusEnum
import com.example.data.model.defaultBasisEnum
import com.example.data.model.defaultTriggerEnum
import com.example.data.model.txTypeEnum
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.MonogramAvatar
import com.example.ui.components.StatusBadgeChip
import com.example.ui.theme.LocalDukaanColors
import com.example.ui.util.MoneyUtils
import com.example.ui.util.TaxAndCommissionCalculator
import com.example.ui.util.formatCurrency
import com.example.ui.util.formatDate
import com.example.ui.util.formatQty
import com.example.ui.util.shareText

@Composable
fun AgentsScreen(
  agents: List<Agent>,
  transactions: List<BusinessTransaction>,
  accounts: List<CashBankAccount>,
  profile: BusinessProfile?,
  onSaveAgent: (Agent, () -> Unit) -> Unit,
  onArchiveOrDeleteAgent: (Agent) -> Unit,
  onRecordPayout: (Agent, Double, Double, String, PaymentMode, String, String, () -> Unit) -> Unit,
  onTransactionClick: (BusinessTransaction) -> Unit,
) {
  val sym = profile?.currencySymbol ?: "₹"
  val semantic = LocalDukaanColors.current
  val context = LocalContext.current

  var searchQuery by remember { mutableStateOf("") }
  var typeFilter by remember { mutableStateOf<AgentType?>(null) }
  var onlyPending by remember { mutableStateOf(false) }
  var showArchived by remember { mutableStateOf(false) }
  var selectedAgentId by remember { mutableStateOf<String?>(null) }
  var editingAgent by remember { mutableStateOf<Agent?>(null) }
  var showEditorDialog by remember { mutableStateOf(false) }
  var payoutTarget by remember { mutableStateOf<Agent?>(null) }
  var deleteConfirmAgent by remember { mutableStateOf<Agent?>(null) }

  val filteredAgents = remember(agents, searchQuery, typeFilter, onlyPending, showArchived) {
    agents.filter { ag ->
      val activeOk = showArchived || ag.isActive
      val typeOk = typeFilter == null || ag.agentType == typeFilter!!.name
      val pendingOk = !onlyPending || ag.currentBalance > 0.01
      val queryOk = searchQuery.isBlank() ||
        ag.name.contains(searchQuery, true) ||
        ag.phone.contains(searchQuery, true) ||
        ag.cityArea.contains(searchQuery, true)
      activeOk && typeOk && pendingOk && queryOk
    }
  }

  val activeAgents = remember(agents) { agents.filter { it.isActive } }
  val selectedAgent = remember(agents, selectedAgentId) {
    agents.firstOrNull { it.id == selectedAgentId }
  }

  val totalCommissionPayable = activeAgents.filter { it.currentBalance > 0 }.sumOf { it.currentBalance }
  val totalEarned = activeAgents.sumOf { it.totalEarned }
  val totalPaid = activeAgents.sumOf { it.totalPaid }
  val pendingAgentsCount = activeAgents.count { it.currentBalance > 0.01 }

  val agentLinkedSales = remember(transactions) {
    transactions.filter {
      it.status != TxStatus.CANCELLED.name &&
        it.txType == TxType.SALE.name &&
        (it.agentId.isNotBlank() || it.commissionSplits.isNotEmpty())
    }
  }
  val totalSalesViaAgents = agentLinkedSales.sumOf { it.totalAmount }

  fun agentLedgerFor(agent: Agent): List<BusinessTransaction> =
    transactions.filter { tx ->
      tx.agentId == agent.id || tx.commissionSplits.any { it.agentId == agent.id }
    }

  fun shareStatement(agent: Agent) {
    val ledger = agentLedgerFor(agent).filter { it.status != TxStatus.CANCELLED.name }.take(25)
    val text = buildString {
      appendLine("*${profile?.businessName ?: "Dukaan Stock"} — Commission Statement*")
      appendLine("Partner: ${agent.name} (${agent.agentTypeEnum().label})")
      if (agent.phone.isNotBlank()) appendLine("Phone: ${agent.phone}")
      appendLine("--------------------------------")
      appendLine("Total Commission Earned: ${formatCurrency(agent.totalEarned, sym)}")
      appendLine("Total Paid Out: ${formatCurrency(agent.totalPaid, sym)}")
      appendLine("*Balance Payable: ${formatCurrency(agent.currentBalance, sym)}*")
      if (ledger.isNotEmpty()) {
        appendLine("--------------------------------")
        appendLine("Recent Entries:")
        ledger.forEach { tx ->
          val splitShare = tx.commissionSplits.firstOrNull { it.agentId == agent.id }?.accruedCommission
            ?: tx.commissionAccruedAmount
          val label = when (tx.txTypeEnum()) {
            TxType.COMMISSION_PAYOUT -> "Paid ${formatCurrency(tx.totalAmount, sym)}"
            TxType.CREDIT_NOTE -> "Return Reversal -${formatCurrency(splitShare, sym)}"
            else -> "Earned ${formatCurrency(splitShare, sym)}"
          }
          appendLine("${formatDate(tx.dateMillis)} • ${tx.refNumber} • $label")
        }
      }
    }
    shareText(context, "Commission Statement - ${agent.name}", text)
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val isWide = maxWidth >= 840.dp

    if (!isWide && selectedAgent != null) {
      BackHandler { selectedAgentId = null }
      AgentDetailPane(
        agent = selectedAgent,
        ledger = agentLedgerFor(selectedAgent),
        currencySymbol = sym,
        showBackButton = true,
        onBack = { selectedAgentId = null },
        onEdit = {
          editingAgent = selectedAgent
          showEditorDialog = true
        },
        onDelete = { deleteConfirmAgent = selectedAgent },
        onPayCommission = { payoutTarget = selectedAgent },
        onShareStatement = { shareStatement(selectedAgent) },
        onTransactionClick = onTransactionClick,
        modifier = Modifier.fillMaxSize().padding(16.dp),
      )
    } else {
      Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Top Header Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Agents & Mistri", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
              "Commission Khata • ${activeAgents.size} partners",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Button(
            onClick = {
              editingAgent = null
              showEditorDialog = true
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(48.dp).testTag("add_agent_button"),
          ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add Agent")
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // KPI Summary Row (2 on compact, 4 on wide Computer UI matching screen-1 & 10)
        if (isWide) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiMetricCard(
              title = "Commission to Pay",
              value = formatCurrency(totalCommissionPayable, sym),
              subtitle = "$pendingAgentsCount agents due (Baaki)",
              icon = Icons.Filled.Engineering,
              accentColor = semantic.warning,
              containerColor = semantic.warningContainer,
              cardColor = semantic.warningCardBg,
              cardBorderColor = semantic.warningCardBorder,
              valueColor = semantic.warning,
              modifier = Modifier.weight(1f),
            )
            KpiMetricCard(
              title = "Sales via Agents",
              value = formatCurrency(totalSalesViaAgents, sym),
              subtitle = "${agentLinkedSales.size} attributed bills",
              icon = Icons.AutoMirrored.Filled.ReceiptLong,
              accentColor = MaterialTheme.colorScheme.primary,
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              cardColor = semantic.infoCardBg,
              cardBorderColor = semantic.infoCardBorder,
              modifier = Modifier.weight(1f),
            )
            KpiMetricCard(
              title = "Commission Earned",
              value = formatCurrency(totalEarned, sym),
              subtitle = "Total accrued lifetime",
              icon = Icons.Filled.AccountBalanceWallet,
              accentColor = MaterialTheme.colorScheme.primary,
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              modifier = Modifier.weight(1f),
            )
            KpiMetricCard(
              title = "Paid Out",
              value = formatCurrency(totalPaid, sym),
              subtitle = "Settled via Cash/Bank",
              icon = Icons.Filled.CheckCircle,
              accentColor = semantic.positive,
              containerColor = semantic.positiveContainer,
              cardColor = semantic.positiveCardBg,
              cardBorderColor = semantic.positiveCardBorder,
              valueColor = semantic.positive,
              modifier = Modifier.weight(1f),
            )
          }
        } else {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiMetricCard(
              title = "Commission to Pay",
              value = formatCurrency(totalCommissionPayable, sym),
              subtitle = "$pendingAgentsCount agents due",
              icon = Icons.Filled.Engineering,
              accentColor = semantic.warning,
              containerColor = semantic.warningContainer,
              cardColor = semantic.warningCardBg,
              cardBorderColor = semantic.warningCardBorder,
              valueColor = semantic.warning,
              modifier = Modifier.weight(1f),
            )
            KpiMetricCard(
              title = "Sales via Agents",
              value = formatCurrency(totalSalesViaAgents, sym),
              subtitle = "${agentLinkedSales.size} invoices",
              icon = Icons.AutoMirrored.Filled.ReceiptLong,
              accentColor = MaterialTheme.colorScheme.primary,
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              cardColor = semantic.infoCardBg,
              cardBorderColor = semantic.infoCardBorder,
              modifier = Modifier.weight(1f),
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
          modifier = Modifier.fillMaxSize(),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            OutlinedTextField(
              value = searchQuery,
              onValueChange = { searchQuery = it },
              placeholder = { Text("Search mistri, agent, phone, or area...") },
              leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
              singleLine = true,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth().testTag("agent_search_input"),
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              FilterChip(
                selected = typeFilter == null && !onlyPending,
                onClick = {
                  typeFilter = null
                  onlyPending = false
                },
                label = { Text("All (${activeAgents.size})") },
              )
              FilterChip(
                selected = onlyPending,
                onClick = { onlyPending = !onlyPending },
                label = { Text("Pending ($pendingAgentsCount)") },
              )
              AgentType.entries.forEach { t ->
                val count = activeAgents.count { it.agentType == t.name }
                FilterChip(
                  selected = typeFilter == t,
                  onClick = { typeFilter = if (typeFilter == t) null else t },
                  label = { Text("${t.label.substringBefore(" /")} ($count)") },
                )
              }
              FilterChip(
                selected = showArchived,
                onClick = { showArchived = !showArchived },
                label = { Text("Archived") },
              )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (filteredAgents.isEmpty()) {
              EmptyStateCard(
                icon = Icons.Filled.Engineering,
                title = if (agents.isEmpty()) "No agents or mistris added yet" else "No matching partners",
                subtitle = "Track Mistri, Broker, Architect, Contractor, or Salesman commissions separately—kept 100% hidden from customer invoices.",
                actionLabel = "Add First Agent / Mistri",
                onAction = {
                  editingAgent = null
                  showEditorDialog = true
                },
              )
            } else {
              LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                items(filteredAgents, key = { it.id }) { ag ->
                  val isSelected = isWide && ag.id == (selectedAgent?.id ?: filteredAgents.firstOrNull()?.id)
                  val hasDue = ag.currentBalance > 0.01
                  val balColor = if (hasDue) semantic.warning else semantic.positive
                  val rateLabel = if (ag.defaultBasisEnum() == CommissionBasis.PER_UNIT) {
                    "${formatCurrency(ag.defaultPerUnitAmount, sym)}/unit"
                  } else {
                    "${formatQty(ag.defaultPercent)}% ${ag.defaultBasisEnum().name.lowercase()}"
                  }
                  val roleColor = when (ag.agentTypeEnum()) {
                    AgentType.MISTRI -> semantic.warning
                    AgentType.ARCHITECT -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.primary
                  }
                  val roleBg = when (ag.agentTypeEnum()) {
                    AgentType.MISTRI -> semantic.warningContainer
                    AgentType.ARCHITECT -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.primaryContainer
                  }

                  Surface(
                    modifier =
                      Modifier
                        .fillMaxWidth()
                        .clickable { selectedAgentId = ag.id }
                        .testTag("agent_card_${ag.id}"),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) semantic.infoCardBg else MaterialTheme.colorScheme.surface,
                    border =
                      BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                      ),
                  ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        MonogramAvatar(name = ag.name, sizeDp = 44, cornerDp = 999)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                              text = ag.name,
                              style = MaterialTheme.typography.titleSmall,
                              fontWeight = FontWeight.Bold,
                              maxLines = 1,
                              overflow = TextOverflow.Ellipsis,
                              modifier = Modifier.weight(1f, fill = false),
                            )
                            StatusBadgeChip(
                              label = ag.agentTypeEnum().label.substringBefore(" /").uppercase(),
                              color = roleColor,
                              containerColor = roleBg,
                            )
                            if (!ag.isActive) {
                              StatusBadgeChip("Archived", MaterialTheme.colorScheme.outline, MaterialTheme.colorScheme.surfaceVariant)
                            }
                          }
                          Spacer(modifier = Modifier.height(2.dp))
                          Text(
                            text = listOfNotNull(
                              rateLabel,
                              ag.cityArea.takeIf { it.isNotBlank() },
                              ag.phone.takeIf { it.isNotBlank() },
                            ).joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                          )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                          Text(
                            text = formatCurrency(ag.currentBalance, sym),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = balColor,
                          )
                          Text(
                            text = if (hasDue) "Baaki" else "Settled",
                            style = MaterialTheme.typography.labelSmall,
                            color = balColor,
                          )
                        }
                      }

                      Spacer(modifier = Modifier.height(10.dp))
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                      ) {
                        OutlinedButton(
                          onClick = { selectedAgentId = ag.id },
                          shape = RoundedCornerShape(10.dp),
                          modifier = Modifier.weight(1f).height(40.dp),
                        ) {
                          Text("Ledger", maxLines = 1)
                        }
                        Button(
                          onClick = { payoutTarget = ag },
                          shape = RoundedCornerShape(10.dp),
                          colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasDue) MaterialTheme.colorScheme.primary else semantic.positive,
                          ),
                          modifier = Modifier.weight(1f).height(40.dp),
                        ) {
                          Text(
                            if (hasDue) "Pay ${formatCurrency(ag.currentBalance, sym)}" else "Pay",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                          )
                        }
                      }
                    }
                  }
                }
              }
            }
          }

          if (isWide) {
            val activeAgent = selectedAgent ?: filteredAgents.firstOrNull()
            Box(modifier = Modifier.weight(1.15f).fillMaxHeight()) {
              if (activeAgent != null) {
                AgentDetailPane(
                  agent = activeAgent,
                  ledger = agentLedgerFor(activeAgent),
                  currencySymbol = sym,
                  showBackButton = false,
                  onBack = {},
                  onEdit = {
                    editingAgent = activeAgent
                    showEditorDialog = true
                  },
                  onDelete = { deleteConfirmAgent = activeAgent },
                  onPayCommission = { payoutTarget = activeAgent },
                  onShareStatement = { shareStatement(activeAgent) },
                  onTransactionClick = onTransactionClick,
                  modifier = Modifier.fillMaxSize(),
                )
              } else {
                EmptyStateCard(
                  icon = Icons.Filled.Engineering,
                  title = "Select an Agent / Mistri",
                  subtitle = "Choose a commission partner on the left to view attributed sales, pending payouts, and settlement history.",
                )
              }
            }
          }
        }
      }
    }
  }

  if (showEditorDialog) {
    AgentEditorDialog(
      initial = editingAgent,
      currencySymbol = sym,
      onDismiss = { showEditorDialog = false },
      onSave = { ag ->
        onSaveAgent(ag) { showEditorDialog = false }
      },
    )
  }

  val targetPay = payoutTarget
  if (targetPay != null) {
    PayCommissionDialog(
      agent = targetPay,
      accounts = accounts.filter { it.isActive },
      profile = profile,
      currencySymbol = sym,
      onDismiss = { payoutTarget = null },
      onConfirm = { gross, tdsPct, accId, mode, refNo, notes ->
        onRecordPayout(targetPay, gross, tdsPct, accId, mode, refNo, notes) {
          payoutTarget = null
        }
      },
    )
  }

  val targetDel = deleteConfirmAgent
  if (targetDel != null) {
    val hasTxs = agentLedgerFor(targetDel).isNotEmpty()
    AlertDialog(
      onDismissRequest = { deleteConfirmAgent = null },
      title = { Text(if (hasTxs) "Archive ${targetDel.name}?" else "Delete ${targetDel.name}?") },
      text = {
        Text(
          if (hasTxs || kotlin.math.abs(targetDel.currentBalance) > 0.01) {
            "${targetDel.name} has commission history or unpaid balance. They will be archived safely without losing past invoice reports."
          } else {
            "Permanently delete ${targetDel.name}? This cannot be undone."
          }
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onArchiveOrDeleteAgent(targetDel)
            if (selectedAgentId == targetDel.id) selectedAgentId = null
            deleteConfirmAgent = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) {
          Text(if (hasTxs) "Archive" else "Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { deleteConfirmAgent = null }) { Text("Cancel") }
      },
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AgentDetailPane(
  agent: Agent,
  ledger: List<BusinessTransaction>,
  currencySymbol: String,
  showBackButton: Boolean,
  onBack: () -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  onPayCommission: () -> Unit,
  onShareStatement: () -> Unit,
  onTransactionClick: (BusinessTransaction) -> Unit,
  modifier: Modifier = Modifier,
) {
  val semantic = LocalDukaanColors.current
  var activeTab by remember(agent.id) { mutableIntStateOf(0) }
  val attributedBills = remember(ledger) {
    ledger.filter { it.txType == TxType.SALE.name || it.txType == TxType.CREDIT_NOTE.name }
  }

  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(modifier = Modifier.fillMaxSize().padding(18.dp)) {
      // Header with Monogram Avatar, Role, Phone/Area/UPI & Actions
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          if (showBackButton) {
            IconButton(onClick = onBack) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
          }
          MonogramAvatar(name = agent.name, sizeDp = 50, cornerDp = 14)
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(agent.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              StatusBadgeChip(
                label = agent.agentTypeEnum().label.substringBefore(" /").uppercase(),
                color = semantic.warning,
                containerColor = semantic.warningContainer,
              )
              val sub = listOfNotNull(
                agent.phone.takeIf { it.isNotBlank() },
                agent.cityArea.takeIf { it.isNotBlank() },
              ).joinToString(" • ")
              if (sub.isNotBlank()) {
                Text(
                  sub,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }
        Row {
          IconButton(onClick = onEdit) {
            Icon(Icons.Filled.Edit, contentDescription = "Edit Agent")
          }
          IconButton(onClick = onDelete) {
            Icon(Icons.Filled.DeleteOutline, contentDescription = "Archive or Delete Agent", tint = MaterialTheme.colorScheme.error)
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // 3 Tinted KPI Boxes: Earned | Paid | Payable (Baaki)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        AgentStatBox(
          label = "Earned",
          value = formatCurrency(agent.totalEarned, currencySymbol),
          bg = MaterialTheme.colorScheme.surfaceContainerLow,
          border = MaterialTheme.colorScheme.outlineVariant,
          valueColor = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.weight(1f),
        )
        AgentStatBox(
          label = "Paid Out",
          value = formatCurrency(agent.totalPaid, currencySymbol),
          bg = semantic.positiveCardBg,
          border = semantic.positiveCardBorder,
          valueColor = semantic.positive,
          modifier = Modifier.weight(1f),
        )
        AgentStatBox(
          label = "Payable (Baaki)",
          value = formatCurrency(agent.currentBalance, currencySymbol),
          bg = if (agent.currentBalance > 0.01) semantic.warningCardBg else semantic.positiveCardBg,
          border = if (agent.currentBalance > 0.01) semantic.warningCardBorder else semantic.positiveCardBorder,
          valueColor = if (agent.currentBalance > 0.01) semantic.warning else semantic.positive,
          modifier = Modifier.weight(1f),
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Segmented 3-Tab Switcher: Ledger | Bills | Info
      SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        listOf("Ledger (${ledger.size})", "Bills (${attributedBills.size})", "Info").forEachIndexed { idx, label ->
          SegmentedButton(
            selected = activeTab == idx,
            onClick = { activeTab = idx },
            shape = SegmentedButtonDefaults.itemShape(index = idx, count = 3),
          ) {
            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      when (activeTab) {
        0 -> AgentLedgerList(
          agent = agent,
          entries = ledger,
          currencySymbol = currencySymbol,
          onTransactionClick = onTransactionClick,
          modifier = Modifier.weight(1f),
        )
        1 -> AgentLedgerList(
          agent = agent,
          entries = attributedBills,
          currencySymbol = currencySymbol,
          onTransactionClick = onTransactionClick,
          modifier = Modifier.weight(1f),
        )
        else -> {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.weight(1f).fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              AgentInfoField("Role / Category", agent.agentTypeEnum().label)
              if (agent.phone.isNotBlank()) AgentInfoField("Phone", agent.phone)
              if (agent.cityArea.isNotBlank()) AgentInfoField("City / Area", agent.cityArea)
              AgentInfoField(
                "Default Commission Rule",
                if (agent.defaultBasisEnum() == CommissionBasis.PER_UNIT) {
                  "${formatCurrency(agent.defaultPerUnitAmount, currencySymbol)} per unit (${agent.defaultTriggerEnum().label})"
                } else {
                  "${formatQty(agent.defaultPercent)}% of ${agent.defaultBasisEnum().label} • ${agent.defaultTriggerEnum().label}"
                },
              )
              if (agent.upiId.isNotBlank()) AgentInfoField("UPI ID", agent.upiId)
              if (agent.bankDetails.isNotBlank()) AgentInfoField("Bank Details", agent.bankDetails)
              AgentInfoField("Opening Balance", formatCurrency(agent.openingBalance, currencySymbol))
              if (agent.notes.isNotBlank()) AgentInfoField("Notes / Trade", agent.notes)
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Bottom Action Bar: Statement & Pay Commission CTA
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedButton(
          onClick = onShareStatement,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f).height(48.dp),
        ) {
          Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Statement")
        }
        Button(
          onClick = onPayCommission,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1.25f).height(48.dp).testTag("pay_commission_button"),
        ) {
          Icon(Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            if (agent.currentBalance > 0.01) "Pay ${formatCurrency(agent.currentBalance, currencySymbol)}" else "Pay Commission",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

@Composable
private fun AgentStatBox(
  label: String,
  value: String,
  bg: Color,
  border: Color,
  valueColor: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(14.dp),
    color = bg,
    border = BorderStroke(1.dp, border),
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Spacer(modifier = Modifier.height(3.dp))
      Text(
        value,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = valueColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun AgentLedgerList(
  agent: Agent,
  entries: List<BusinessTransaction>,
  currencySymbol: String,
  onTransactionClick: (BusinessTransaction) -> Unit,
  modifier: Modifier = Modifier,
) {
  val semantic = LocalDukaanColors.current
  if (entries.isEmpty()) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
      Text(
        "No invoices or payouts linked to ${agent.name} yet.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  } else {
    LazyColumn(
      modifier = modifier,
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      items(entries, key = { it.id }) { tx ->
        val split = tx.commissionSplits.firstOrNull { it.agentId == agent.id }
        val commAmt = split?.accruedCommission ?: tx.commissionAccruedAmount
        val isPayout = tx.txType == TxType.COMMISSION_PAYOUT.name
        val isReturn = tx.txType == TxType.CREDIT_NOTE.name
        val isCancelled = tx.status == TxStatus.CANCELLED.name

        val (badgeLabel, badgeFg, badgeBg) = when {
          isCancelled -> Triple("CANCELLED", MaterialTheme.colorScheme.outline, MaterialTheme.colorScheme.surfaceVariant)
          isPayout -> Triple("PAID", MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.surfaceVariant)
          isReturn -> Triple("REVERSED", semantic.negative, semantic.negativeContainer)
          tx.commissionStatusEnum() == CommissionStatus.PENDING_COLLECTION -> Triple("ON COLLECTION", semantic.warning, semantic.warningContainer)
          tx.commissionStatusEnum() == CommissionStatus.PARTIAL -> Triple("PARTIAL", semantic.warning, semantic.warningContainer)
          else -> Triple("ACCRUED", semantic.positive, semantic.positiveContainer)
        }

        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.fillMaxWidth().clickable { onTransactionClick(tx) },
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                "${tx.refNumber} • ${tx.partyName.ifBlank { agent.name }}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                if (isPayout) {
                  "Commission Payout • ${formatDate(tx.dateMillis)}"
                } else {
                  "${tx.txTypeEnum().shortLabel} • ${formatDate(tx.dateMillis)} • Inv: ${formatCurrency(tx.totalAmount, currencySymbol)}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = when {
                  isPayout -> "-${formatCurrency(tx.totalAmount, currencySymbol)}"
                  isReturn -> "-${formatCurrency(commAmt, currencySymbol)}"
                  else -> "+${formatCurrency(commAmt, currencySymbol)}"
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = when {
                  isCancelled -> MaterialTheme.colorScheme.outline
                  isPayout || isReturn -> semantic.negative
                  else -> semantic.positive
                },
              )
              Spacer(modifier = Modifier.height(3.dp))
              StatusBadgeChip(label = badgeLabel, color = badgeFg, containerColor = badgeBg)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AgentInfoField(label: String, value: String) {
  Column {
    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    Spacer(modifier = Modifier.height(6.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
  }
}

@Composable
private fun AgentEditorDialog(
  initial: Agent?,
  currencySymbol: String,
  onDismiss: () -> Unit,
  onSave: (Agent) -> Unit,
) {
  var name by remember { mutableStateOf(initial?.name ?: "") }
  var agentType by remember { mutableStateOf(initial?.agentTypeEnum() ?: AgentType.MISTRI) }
  var phone by remember { mutableStateOf(initial?.phone ?: "") }
  var cityArea by remember { mutableStateOf(initial?.cityArea ?: "") }
  var upiId by remember { mutableStateOf(initial?.upiId ?: "") }
  var bankDetails by remember { mutableStateOf(initial?.bankDetails ?: "") }
  var defaultPercentText by remember { mutableStateOf((initial?.defaultPercent ?: 2.0).toString()) }
  var defaultPerUnitText by remember { mutableStateOf((initial?.defaultPerUnitAmount ?: 0.0).takeIf { it > 0 }?.toString() ?: "") }
  var basis by remember { mutableStateOf(initial?.defaultBasisEnum() ?: CommissionBasis.TAXABLE) }
  var trigger by remember { mutableStateOf(initial?.defaultTriggerEnum() ?: CommissionTrigger.ON_INVOICE) }
  var openingBalText by remember { mutableStateOf(initial?.openingBalance?.takeIf { it != 0.0 }?.toString() ?: "") }
  var notes by remember { mutableStateOf(initial?.notes ?: "") }
  val semantic = LocalDukaanColors.current

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (initial == null) "Add Agent / Mistri" else "Edit Agent / Mistri") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Partner Name *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("agent_name_input"),
        )
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          AgentType.entries.forEach { t ->
            FilterChip(
              selected = agentType == t,
              onClick = { agentType = t },
              label = { Text(t.label.substringBefore(" /")) },
            )
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = cityArea,
            onValueChange = { cityArea = it },
            label = { Text("City / Area") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = defaultPercentText,
            onValueChange = { defaultPercentText = it },
            label = { Text("Commission %") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("agent_percent_input"),
          )
          OutlinedTextField(
            value = defaultPerUnitText,
            onValueChange = { defaultPerUnitText = it },
            label = { Text("Per Unit ($currencySymbol)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }
        Text("Commission Calculation Basis", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          CommissionBasis.entries.forEach { b ->
            val selected = basis == b
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (selected) semantic.infoCardBg else MaterialTheme.colorScheme.surfaceContainerLow,
              border = BorderStroke(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
              ),
              modifier = Modifier.fillMaxWidth().clickable { basis = b },
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                RadioButton(selected = selected, onClick = { basis = b })
                Spacer(modifier = Modifier.width(4.dp))
                Text(b.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                if (b == CommissionBasis.TAXABLE) {
                  StatusBadgeChip("Recommended", semantic.positive, semantic.positiveContainer)
                }
              }
            }
          }
        }
        Text("Commission Accrual Trigger", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          CommissionTrigger.entries.forEach { tr ->
            val selected = trigger == tr
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (selected) semantic.infoCardBg else MaterialTheme.colorScheme.surfaceContainerLow,
              border = BorderStroke(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
              ),
              modifier = Modifier.fillMaxWidth().clickable { trigger = tr },
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                RadioButton(selected = selected, onClick = { trigger = tr })
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                  Text(tr.label, style = MaterialTheme.typography.bodyMedium)
                  Text(
                    if (tr == CommissionTrigger.ON_COLLECTION) "Safe: accrues only as customer pays" else "Accrues immediately when invoice saves",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }
        OutlinedTextField(
          value = upiId,
          onValueChange = { upiId = it },
          label = { Text("UPI ID (for Payouts)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = bankDetails,
          onValueChange = { bankDetails = it },
          label = { Text("Bank Details (Optional)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        if (initial == null) {
          OutlinedTextField(
            value = openingBalText,
            onValueChange = { openingBalText = it },
            label = { Text("Opening Payable Balance ($currencySymbol)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )
        }
        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Notes / Trade") },
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val openBal = MoneyUtils.round2((openingBalText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0))
          onSave(
            (initial ?: Agent()).copy(
              name = name.trim(),
              agentType = agentType.name,
              phone = phone.trim(),
              cityArea = cityArea.trim(),
              upiId = upiId.trim(),
              bankDetails = bankDetails.trim(),
              defaultPercent = (defaultPercentText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100.0),
              defaultPerUnitAmount = (defaultPerUnitText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0),
              defaultBasis = basis.name,
              defaultTrigger = trigger.name,
              openingBalance = if (initial == null) openBal else initial.openingBalance,
              currentBalance = if (initial == null) openBal else initial.currentBalance,
              totalEarned = if (initial == null) openBal else initial.totalEarned,
              isActive = true,
              notes = notes.trim(),
            )
          )
        },
        enabled = name.trim().isNotEmpty(),
        modifier = Modifier.testTag("save_agent_button"),
      ) {
        Text("Save Partner")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

@Composable
private fun PayCommissionDialog(
  agent: Agent,
  accounts: List<CashBankAccount>,
  profile: BusinessProfile?,
  currencySymbol: String,
  onDismiss: () -> Unit,
  onConfirm: (Double, Double, String, PaymentMode, String, String) -> Unit,
) {
  val semantic = LocalDukaanColors.current
  var grossText by remember {
    mutableStateOf(agent.currentBalance.takeIf { it > 0 }?.let { MoneyUtils.round2(it).toString() } ?: "")
  }
  var enableTds by remember { mutableStateOf(false) }
  var tdsText by remember { mutableStateOf("5") }
  var refNumber by remember {
    mutableStateOf(TaxAndCommissionCalculator.formatDocumentRef(TxType.COMMISSION_PAYOUT, profile))
  }
  var paymentMode by remember { mutableStateOf(PaymentMode.CASH) }
  var selectedAccountId by remember(accounts) { mutableStateOf(accounts.firstOrNull()?.id ?: "") }
  var notes by remember { mutableStateOf("") }

  val gross = grossText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
  val tdsPct = if (enableTds) (tdsText.toDoubleOrNull()?.coerceIn(0.0, 50.0) ?: 0.0) else 0.0
  val tdsAmt = MoneyUtils.round2(gross * tdsPct / 100.0)
  val netPaid = MoneyUtils.round2((gross - tdsAmt).coerceAtLeast(0.0))

  val selectedAcc = accounts.firstOrNull { it.id == selectedAccountId } ?: accounts.firstOrNull()
  val remainingAgentDue = MoneyUtils.round2((agent.currentBalance - gross).coerceAtLeast(0.0))
  val accBalAfter = selectedAcc?.let { MoneyUtils.round2(it.currentBalance - netPaid) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Column {
        Text("Pay Commission", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
          "${agent.name} • Payable ${formatCurrency(agent.currentBalance, currencySymbol)}",
          style = MaterialTheme.typography.bodySmall,
          color = semantic.warning,
          fontWeight = FontWeight.SemiBold,
        )
      }
    },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedTextField(
            value = grossText,
            onValueChange = { grossText = it },
            label = { Text("Gross Payout ($currencySymbol) *") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f).testTag("payout_amount_input"),
          )
          if (agent.currentBalance > 0.01) {
            OutlinedButton(
              onClick = { grossText = MoneyUtils.round2(agent.currentBalance).toString() },
              shape = RoundedCornerShape(10.dp),
            ) {
              Text("Full Due")
            }
          }
        }

        if (accounts.isNotEmpty()) {
          Text("Pay From Account", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            accounts.forEach { acc ->
              val selected = acc.id == selectedAccountId
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (selected) semantic.infoCardBg else MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(
                  width = if (selected) 1.5.dp else 1.dp,
                  color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                ),
                modifier = Modifier.clickable { selectedAccountId = acc.id },
              ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                  Text(acc.accountName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                  Text(
                    formatCurrency(acc.currentBalance, currencySymbol),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
              }
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          PaymentMode.entries.take(4).forEach { m ->
            FilterChip(
              selected = paymentMode == m,
              onClick = { paymentMode = m },
              label = { Text(m.label) },
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text("Deduct TDS % (Optional)", style = MaterialTheme.typography.bodyMedium)
              Switch(checked = enableTds, onCheckedChange = { enableTds = it })
            }
            if (enableTds) {
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                  value = tdsText,
                  onValueChange = { tdsText = it },
                  label = { Text("TDS %") },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                  singleLine = true,
                  modifier = Modifier.weight(1f),
                )
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                  Text("TDS: ${formatCurrency(tdsAmt, currencySymbol)}", style = MaterialTheme.typography.bodySmall)
                  Text(
                    "Net Paid: ${formatCurrency(netPaid, currencySymbol)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = semantic.positive,
                  )
                }
              }
            }
          }
        }

        OutlinedTextField(
          value = refNumber,
          onValueChange = { refNumber = it },
          label = { Text("Voucher Ref No.") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Notes / Remarks") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        // Post-payout live preview box
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = semantic.positiveCardBg,
          border = BorderStroke(1.dp, semantic.positiveCardBorder),
        ) {
          Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Net Cash / Bank Outflow", style = MaterialTheme.typography.bodySmall)
              Text(formatCurrency(netPaid, currencySymbol), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Agent Baaki After Payout", style = MaterialTheme.typography.bodySmall)
              Text(
                formatCurrency(remainingAgentDue, currencySymbol),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = if (remainingAgentDue > 0.01) semantic.warning else semantic.positive,
              )
            }
            if (selectedAcc != null && accBalAfter != null) {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${selectedAcc.accountName} After", style = MaterialTheme.typography.bodySmall)
                Text(formatCurrency(accBalAfter, currencySymbol), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onConfirm(gross, tdsPct, selectedAccountId, paymentMode, refNumber, notes)
        },
        enabled = gross > 0.0,
        colors = ButtonDefaults.buttonColors(containerColor = semantic.positive),
        modifier = Modifier.testTag("confirm_payout_button"),
      ) {
        Text("Pay & Save", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}
