package com.example.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.model.Agent
import com.example.data.model.BusinessTransaction
import com.example.data.model.InventoryItem
import com.example.data.model.Party
import com.example.data.model.ThemeStyle
import com.example.data.model.TxStatus
import com.example.data.model.TxType
import com.example.data.model.themeStyleEnum
import com.example.data.model.txTypeEnum
import com.example.data.repository.DukaanRepository
import com.example.ui.auth.SignInScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOutUser
import com.example.ui.components.NewFeatureBadge
import com.example.ui.components.TaxInvoicePreviewCard
import com.example.ui.screens.AgentsScreen
import com.example.ui.screens.CancelTransactionDialog
import com.example.ui.screens.CashBankScreen
import com.example.ui.screens.DarkModePref
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.InvoiceBuilderScreen
import com.example.ui.screens.ItemsScreen
import com.example.ui.screens.PartiesScreen
import com.example.ui.screens.QuickVoucherDialog
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransactionHubMode
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.DukaanStockTheme
import com.example.ui.theme.LocalDukaanColors
import com.example.ui.util.formatCurrency
import com.example.ui.viewmodel.DukaanViewModel
import com.example.ui.viewmodel.InvoiceDraftViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.dukaanPrefs by preferencesDataStore(name = "dukaan_prefs")
private val KEY_DARK_MODE = stringPreferencesKey("dark_mode_pref")

enum class AppNavTarget(
  val label: String,
  val compactLabel: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
) {
  HOME("Home", "Home", Icons.Filled.Home, Icons.Outlined.Home),
  PARTIES("Parties", "Parties", Icons.Filled.People, Icons.Outlined.People),
  ITEMS("Items", "Items", Icons.Filled.Inventory2, Icons.Outlined.Inventory2),
  SALE("Sale & Billing", "Sale", Icons.AutoMirrored.Filled.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong),
  PURCHASE_EXPENSE("Purchase & Expense", "Purchase", Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart),
  AGENTS("Agents & Mistri", "Agents", Icons.Filled.Engineering, Icons.Outlined.Engineering),
  CASH_BANK("Cash & Bank", "Bank", Icons.Filled.AccountBalance, Icons.Outlined.AccountBalance),
  REPORTS("Reports", "Reports", Icons.Filled.Assessment, Icons.Outlined.Assessment),
  SETTINGS("Settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
  MORE_HUB("More", "More", Icons.Filled.Apps, Icons.Outlined.Apps),
}

@Composable
fun DukaanApp() {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  val darkModePref by
    remember(context) {
      context.dukaanPrefs.data.map { prefs ->
        val raw = prefs[KEY_DARK_MODE]
        DarkModePref.entries.firstOrNull { it.name == raw } ?: DarkModePref.SYSTEM
      }
    }.collectAsStateWithLifecycle(initialValue = DarkModePref.SYSTEM)

  val isDarkTheme =
    when (darkModePref) {
      DarkModePref.SYSTEM -> isSystemInDarkTheme()
      DarkModePref.LIGHT -> false
      DarkModePref.DARK -> true
    }

  val auth = Firebase.auth
  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  val user = currentUser

  if (user == null) {
    DukaanStockTheme(themeStyle = ThemeStyle.COBALT, darkTheme = isDarkTheme) {
      SignInScreen(onAuthSuccess = {})
    }
  } else {
    key(user.uid) {
      val viewModel: DukaanViewModel =
        viewModel(
          key = user.uid,
          factory =
            viewModelFactory {
              initializer {
                val app =
                  checkNotNull(
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                  ) {
                    "APPLICATION_KEY missing from CreationExtras"
                  }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                DukaanViewModel(
                  repository = DukaanRepository(db, app.applicationContext),
                  currentUserId = user.uid,
                  initialBusinessName =
                    user.displayName
                      ?.takeIf { it.isNotBlank() }
                      ?.let { "$it Store" }
                      ?: "My Dukaan Store",
                  initialEmail = user.email.orEmpty(),
                )
              }
            },
        )

      DukaanAuthenticatedShell(
        viewModel = viewModel,
        userEmail = user.email.orEmpty(),
        darkModePref = darkModePref,
        isDarkTheme = isDarkTheme,
        onDarkModePrefChange = { newPref ->
          scope.launch {
            context.dukaanPrefs.edit { it[KEY_DARK_MODE] = newPref.name }
          }
        },
        onSignOut = {
          val cm = CredentialManager.create(context)
          signOutUser(credentialManager = cm, scope = scope)
        },
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun DukaanAuthenticatedShell(
  viewModel: DukaanViewModel,
  userEmail: String,
  darkModePref: DarkModePref,
  isDarkTheme: Boolean,
  onDarkModePrefChange: (DarkModePref) -> Unit,
  onSignOut: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val draftViewModel: InvoiceDraftViewModel = viewModel()

  val profileState by viewModel.profileState.collectAsStateWithLifecycle()
  val partiesState by viewModel.partiesState.collectAsStateWithLifecycle()
  val itemsState by viewModel.itemsState.collectAsStateWithLifecycle()
  val transactionsState by viewModel.transactionsState.collectAsStateWithLifecycle()
  val accountsState by viewModel.accountsState.collectAsStateWithLifecycle()
  val agentsState by viewModel.agentsState.collectAsStateWithLifecycle()
  val dateRangeFilter by viewModel.dateRangeFilter.collectAsStateWithLifecycle()
  val hasPendingWrites by viewModel.hasPendingWritesState.collectAsStateWithLifecycle()
  val metrics by viewModel.metricsState.collectAsStateWithLifecycle()
  val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

  val profile = (profileState as? UiState.Success)?.data
  val parties = (partiesState as? UiState.Success)?.data.orEmpty()
  val items = (itemsState as? UiState.Success)?.data.orEmpty()
  val transactions = (transactionsState as? UiState.Success)?.data.orEmpty()
  val accounts = (accountsState as? UiState.Success)?.data.orEmpty()
  val agents = (agentsState as? UiState.Success)?.data.orEmpty()

  val activeThemeStyle = profile?.themeStyleEnum() ?: ThemeStyle.COBALT

  DukaanStockTheme(themeStyle = activeThemeStyle, darkTheme = isDarkTheme) {
    val semantic = LocalDukaanColors.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
      viewModel.uiMessages.collect { msg ->
        snackbarHostState.showSnackbar(msg)
      }
    }

    var activeNav by rememberSaveable { mutableStateOf(AppNavTarget.HOME) }
    var openAddPartyInitially by remember { mutableStateOf(false) }
    var openAddItemInitially by remember { mutableStateOf(false) }

    var invoiceDraftCounter by remember { mutableIntStateOf(0) }
    var invoiceBuilderType by remember { mutableStateOf<TxType?>(null) }
    var invoiceBuilderParty by remember { mutableStateOf<Party?>(null) }
    var convertingFromTx by remember { mutableStateOf<BusinessTransaction?>(null) }
    var creditNoteForSale by remember { mutableStateOf<BusinessTransaction?>(null) }

    var quickVoucherType by remember { mutableStateOf<TxType?>(null) }
    var quickVoucherParty by remember { mutableStateOf<Party?>(null) }
    var selectedTransactionForDetail by remember { mutableStateOf<BusinessTransaction?>(null) }
    var cancelConfirmDetailTx by remember { mutableStateOf<BusinessTransaction?>(null) }
    var showGlobalSearch by remember { mutableStateOf(false) }
    var showReauthDialog by remember { mutableStateOf(false) }

    fun openInvoiceBuilder(
      type: TxType,
      party: Party? = null,
      convertFrom: BusinessTransaction? = null,
      creditForSale: BusinessTransaction? = null,
    ) {
      invoiceDraftCounter += 1
      invoiceBuilderParty = party
      convertingFromTx = convertFrom
      creditNoteForSale = creditForSale
      invoiceBuilderType = type
    }

    fun closeInvoiceBuilder() {
      draftViewModel.clearDraft()
      invoiceBuilderType = null
      invoiceBuilderParty = null
      convertingFromTx = null
      creditNoteForSale = null
    }

    // Full-screen Invoice Builder Overlay
    val builderType = invoiceBuilderType
    if (builderType != null) {
      val draftKey =
        "draft_${invoiceDraftCounter}_${builderType.name}_${invoiceBuilderParty?.id.orEmpty()}_${convertingFromTx?.id.orEmpty()}_${creditNoteForSale?.id.orEmpty()}"
      Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        InvoiceBuilderScreen(
          draftKey = draftKey,
          draftViewModel = draftViewModel,
          initialTxType = builderType,
          initialParty = invoiceBuilderParty,
          convertingFromTx = convertingFromTx,
          creditNoteForSale = creditNoteForSale,
          profile = profile,
          parties = parties,
          items = items,
          accounts = accounts,
          agents = agents,
          isBusy = isBusy,
          onSaveTransaction = { tx, incSeq ->
            viewModel.recordTransaction(tx, incrementInvoiceSequence = incSeq) {
              closeInvoiceBuilder()
            }
          },
          onBack = { closeInvoiceBuilder() },
        )
      }
      return@DukaanStockTheme
    }

    val desktopNavItems =
      listOf(
        AppNavTarget.HOME,
        AppNavTarget.PARTIES,
        AppNavTarget.ITEMS,
        AppNavTarget.SALE,
        AppNavTarget.PURCHASE_EXPENSE,
        AppNavTarget.AGENTS,
        AppNavTarget.CASH_BANK,
        AppNavTarget.REPORTS,
        AppNavTarget.SETTINGS,
      )

    val compactBottomItems =
      listOf(
        AppNavTarget.HOME,
        AppNavTarget.PARTIES,
        AppNavTarget.ITEMS,
        AppNavTarget.SALE,
        AppNavTarget.MORE_HUB,
      )

    val businessTitle = profile?.businessName?.ifBlank { "Dukaan Stock" } ?: "Dukaan Stock"
    val isOnline = viewModel.isOnlineNow

    BoxWithConstraints(
      modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
      val isExpanded = maxWidth >= 840.dp
      val isMedium = maxWidth >= 600.dp && maxWidth < 840.dp

      // On compact devices, pressing Back from sub-destinations returns to More or Home
      if (!isExpanded && !isMedium && activeNav != AppNavTarget.HOME) {
        BackHandler {
          activeNav =
            when (activeNav) {
              AppNavTarget.PURCHASE_EXPENSE,
              AppNavTarget.AGENTS,
              AppNavTarget.CASH_BANK,
              AppNavTarget.REPORTS,
              AppNavTarget.SETTINGS -> AppNavTarget.MORE_HUB
              else -> AppNavTarget.HOME
            }
        }
      }

      val screenBody: @Composable () -> Unit = {
        val initialError =
          listOf(profileState, partiesState, itemsState, transactionsState, accountsState, agentsState)
            .filterIsInstance<UiState.Error>()
            .firstOrNull()

        if (profileState is UiState.Loading && partiesState is UiState.Loading) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              CircularProgressIndicator()
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                "Syncing Dukaan Stock workspace...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        } else {
          Column(modifier = Modifier.fillMaxSize()) {
            if (initialError != null) {
              Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
              ) {
                Text(
                  text = initialError.message,
                  color = MaterialTheme.colorScheme.onErrorContainer,
                  style = MaterialTheme.typography.bodySmall,
                  modifier = Modifier.padding(12.dp),
                )
              }
            }

            when (activeNav) {
              AppNavTarget.HOME -> {
                HomeDashboardScreen(
                  profile = profile,
                  metrics = metrics,
                  accounts = accounts.filter { it.isActive },
                  recentTransactions = transactions,
                  selectedDateRange = dateRangeFilter,
                  hasPendingWrites = hasPendingWrites,
                  onSelectDateRange = { viewModel.setDateRangeFilter(it) },
                  onNewSale = { openInvoiceBuilder(TxType.SALE) },
                  onNewPurchase = { openInvoiceBuilder(TxType.PURCHASE) },
                  onAddExpense = {
                    quickVoucherParty = null
                    quickVoucherType = TxType.EXPENSE
                  },
                  onAddItem = {
                    openAddItemInitially = true
                    activeNav = AppNavTarget.ITEMS
                  },
                  onAddParty = {
                    openAddPartyInitially = true
                    activeNav = AppNavTarget.PARTIES
                  },
                  onOpenReports = { activeNav = AppNavTarget.REPORTS },
                  onOpenItems = { activeNav = AppNavTarget.ITEMS },
                  onOpenTransactions = { activeNav = AppNavTarget.SALE },
                  onOpenAgents = { activeNav = AppNavTarget.AGENTS },
                  onOpenCashBank = { activeNav = AppNavTarget.CASH_BANK },
                  onTransactionClick = { tx -> selectedTransactionForDetail = tx },
                )
              }

              AppNavTarget.PARTIES -> {
                PartiesScreen(
                  parties = parties,
                  agents = agents,
                  transactions = transactions,
                  profile = profile,
                  openAddDialogInitially = openAddPartyInitially,
                  onConsumeInitialDialog = { openAddPartyInitially = false },
                  onSaveParty = { party, done -> viewModel.saveParty(party, done) },
                  onArchiveOrDeleteParty = { party -> viewModel.archiveOrDeleteParty(party) },
                  onNewSaleForParty = { party -> openInvoiceBuilder(TxType.SALE, party = party) },
                  onRecordPaymentForParty = { party, isPayIn ->
                    quickVoucherParty = party
                    quickVoucherType = if (isPayIn) TxType.PAYMENT_IN else TxType.PAYMENT_OUT
                  },
                  onTransactionClick = { tx -> selectedTransactionForDetail = tx },
                )
              }

              AppNavTarget.ITEMS -> {
                ItemsScreen(
                  items = items,
                  transactions = transactions,
                  profile = profile,
                  openAddDialogInitially = openAddItemInitially,
                  onConsumeInitialDialog = { openAddItemInitially = false },
                  onSaveItem = { item, done -> viewModel.saveItem(item, done) },
                  onAdjustStock = { itemId, delta, reason -> viewModel.adjustStock(itemId, delta, reason) },
                  onArchiveOrDeleteItem = { item -> viewModel.archiveOrDeleteItem(item) },
                )
              }

              AppNavTarget.SALE -> {
                TransactionsScreen(
                  mode = TransactionHubMode.SALE,
                  transactions = transactions,
                  profile = profile,
                  parties = parties,
                  accounts = accounts,
                  selectedTransactionForDetail = selectedTransactionForDetail,
                  onSelectTransaction = { selectedTransactionForDetail = it },
                  onOpenInvoiceBuilder = { type -> openInvoiceBuilder(type) },
                  onConvertToSale = { srcTx -> openInvoiceBuilder(TxType.SALE, convertFrom = srcTx) },
                  onCreateCreditNote = { saleTx -> openInvoiceBuilder(TxType.CREDIT_NOTE, creditForSale = saleTx) },
                  onRecordQuickVoucher = { tx -> viewModel.recordTransaction(tx, incrementInvoiceSequence = true) },
                  onRecordSettlement = { tx, amt, accId ->
                    viewModel.recordSettlement(tx, amt, accId)
                  },
                  onCancelTransaction = { id, reason -> viewModel.cancelTransaction(id, reason) },
                )
              }

              AppNavTarget.PURCHASE_EXPENSE -> {
                TransactionsScreen(
                  mode = TransactionHubMode.PURCHASE_EXPENSE,
                  transactions = transactions,
                  profile = profile,
                  parties = parties,
                  accounts = accounts,
                  selectedTransactionForDetail = selectedTransactionForDetail,
                  onSelectTransaction = { selectedTransactionForDetail = it },
                  onOpenInvoiceBuilder = { type -> openInvoiceBuilder(type) },
                  onConvertToSale = { srcTx -> openInvoiceBuilder(TxType.SALE, convertFrom = srcTx) },
                  onCreateCreditNote = { saleTx -> openInvoiceBuilder(TxType.CREDIT_NOTE, creditForSale = saleTx) },
                  onRecordQuickVoucher = { tx -> viewModel.recordTransaction(tx, incrementInvoiceSequence = true) },
                  onRecordSettlement = { tx, amt, accId ->
                    viewModel.recordSettlement(tx, amt, accId)
                  },
                  onCancelTransaction = { id, reason -> viewModel.cancelTransaction(id, reason) },
                )
              }

              AppNavTarget.AGENTS -> {
                AgentsScreen(
                  agents = agents,
                  transactions = transactions,
                  accounts = accounts,
                  profile = profile,
                  onSaveAgent = { agent, done -> viewModel.saveAgent(agent, done) },
                  onArchiveOrDeleteAgent = { agent -> viewModel.archiveOrDeleteAgent(agent) },
                  onRecordPayout = { agent, gross, tdsPct, accId, mode, ref, notes, done ->
                    viewModel.recordCommissionPayout(agent, gross, tdsPct, accId, mode, ref, notes, done)
                  },
                  onTransactionClick = { tx -> selectedTransactionForDetail = tx },
                )
              }

              AppNavTarget.CASH_BANK -> {
                CashBankScreen(
                  accounts = accounts,
                  transactions = transactions,
                  profile = profile,
                  onSaveAccount = { acc, done -> viewModel.saveAccount(acc, done) },
                  onAdjustBalance = { accId, delta, reason -> viewModel.adjustAccountBalance(accId, delta, reason) },
                  onTransferBetweenAccounts = { fromId, toId, amt, notes, done ->
                    viewModel.transferBetweenAccounts(fromId, toId, amt, notes, done)
                  },
                  onArchiveOrDeleteAccount = { acc -> viewModel.archiveOrDeleteAccount(acc) },
                )
              }

              AppNavTarget.REPORTS -> {
                ReportsScreen(
                  profile = profile,
                  metrics = metrics,
                  parties = parties,
                  items = items,
                  agents = agents,
                  transactions = transactions,
                  selectedDateRange = dateRangeFilter,
                  onSelectDateRange = { viewModel.setDateRangeFilter(it) },
                )
              }

              AppNavTarget.SETTINGS -> {
                SettingsScreen(
                  profile = profile,
                  userEmail = userEmail,
                  darkModePref = darkModePref,
                  onDarkModePrefChange = onDarkModePrefChange,
                  onSaveProfile = { updated -> viewModel.saveProfile(updated) },
                  onSelectThemeStyle = { style -> viewModel.updateThemeStyle(style) },
                  onSignOut = onSignOut,
                  onDeleteAccount = {
                    viewModel.deleteAccountAndData(
                      onSuccess = {
                        val cm = CredentialManager.create(context)
                        signOutUser(credentialManager = cm, scope = scope)
                      },
                      onReauthRequired = { showReauthDialog = true },
                    )
                  },
                )
              }

              AppNavTarget.MORE_HUB -> {
                MoreHubScreen(
                  businessName = businessTitle,
                  userEmail = userEmail,
                  onNavigate = { target -> activeNav = target },
                )
              }
            }
          }
        }
      }

      if (isExpanded) {
        Row(modifier = Modifier.fillMaxSize()) {
          Column(
            modifier =
              Modifier
                .width(248.dp)
                .fillMaxHeight()
                .background(semantic.sidebarBg)
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 14.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Image(
                painter = painterResource(id = R.drawable.ic_dukaan_logo),
                contentDescription = "Dukaan Stock Logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(40.dp),
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Dukaan Stock",
                  style = MaterialTheme.typography.titleMedium,
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = businessTitle,
                  style = MaterialTheme.typography.labelSmall,
                  color = Color(0xFF94A3B8),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
              shape = RoundedCornerShape(999.dp),
              color = semantic.sidebarActive,
              border = BorderStroke(1.dp, Color(0xFF2B3A63)),
              modifier =
                Modifier
                  .fillMaxWidth()
                  .height(48.dp)
                  .clickable { showGlobalSearch = true }
                  .testTag("sidebar_open_anything_button"),
            ) {
              Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = Icons.Filled.Search,
                  contentDescription = "Open Anything Search",
                  tint = Color(0xFF94A3B8),
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Open Anything...",
                  style = MaterialTheme.typography.bodySmall,
                  color = Color(0xFFCBD5E1),
                )
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(
              modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              desktopNavItems.forEach { target ->
                val selected = activeNav == target
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = if (selected) semantic.sidebarActive else Color.Transparent,
                  modifier =
                    Modifier
                      .fillMaxWidth()
                      .height(48.dp)
                      .clickable { activeNav = target }
                      .testTag("nav_${target.name.lowercase()}"),
                ) {
                  Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    if (selected) {
                      Box(
                        modifier =
                          Modifier
                            .width(3.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.primary)
                      )
                      Spacer(modifier = Modifier.width(10.dp))
                    } else {
                      Spacer(modifier = Modifier.width(13.dp))
                    }
                    Icon(
                      imageVector = if (selected) target.selectedIcon else target.unselectedIcon,
                      contentDescription = null,
                      tint = if (selected) Color.White else Color(0xFF94A3B8),
                      modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                      text = target.label,
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                      color = if (selected) Color.White else semantic.sidebarText,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                      modifier = Modifier.weight(1f),
                    )
                    if (target == AppNavTarget.AGENTS) {
                      Spacer(modifier = Modifier.width(6.dp))
                      NewFeatureBadge()
                    }
                  }
                }
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
              shape = RoundedCornerShape(14.dp),
              color = semantic.sidebarActive,
              modifier =
                Modifier
                  .fillMaxWidth()
                  .clickable { activeNav = AppNavTarget.SETTINGS }
                  .testTag("sidebar_company_card"),
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Box(
                  modifier =
                    Modifier
                      .size(10.dp)
                      .clip(CircleShape)
                      .background(if (isOnline) Color(0xFF34D399) else Color(0xFFF59E0B))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = businessTitle,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = userEmail.ifBlank { "Cloud Sync Active" },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }
          }

          Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
              Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
              ) {
                Column(modifier = Modifier.statusBarsPadding()) {
                  Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.clickable { activeNav = AppNavTarget.SETTINGS },
                    ) {
                      Box(
                        modifier =
                          Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                      )
                      Spacer(modifier = Modifier.width(10.dp))
                      Text(
                        text =
                          if (profile?.businessName.isNullOrBlank()) "Enter Business Name"
                          else profile?.businessName.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                      )
                      Spacer(modifier = Modifier.width(12.dp))
                      val syncBg = when {
                        !isOnline -> semantic.warningContainer
                        hasPendingWrites -> MaterialTheme.colorScheme.primaryContainer
                        else -> semantic.positiveContainer
                      }
                      val syncTint = when {
                        !isOnline -> semantic.warning
                        hasPendingWrites -> MaterialTheme.colorScheme.primary
                        else -> semantic.positive
                      }
                      val syncIcon = when {
                        !isOnline -> Icons.Filled.CloudOff
                        hasPendingWrites -> Icons.Filled.CloudSync
                        else -> Icons.Filled.CloudDone
                      }
                      val syncLabel = when {
                        !isOnline -> "Offline (Saved Locally)"
                        hasPendingWrites -> "Syncing..."
                        else -> "Cloud Synced"
                      }
                      Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = syncBg,
                      ) {
                        Row(
                          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                          verticalAlignment = Alignment.CenterVertically,
                        ) {
                          Icon(
                            syncIcon,
                            contentDescription = null,
                            tint = syncTint,
                            modifier = Modifier.size(14.dp),
                          )
                          Spacer(modifier = Modifier.width(4.dp))
                          Text(
                            syncLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = syncTint,
                          )
                        }
                      }
                    }

                    Row(
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      IconButton(
                        onClick = { showGlobalSearch = true },
                        modifier = Modifier.testTag("top_search_button"),
                      ) {
                        Icon(Icons.Filled.Search, contentDescription = "Global Search")
                      }
                      IconButton(
                        onClick = {
                          onDarkModePrefChange(
                            if (isDarkTheme) DarkModePref.LIGHT else DarkModePref.DARK
                          )
                        },
                        modifier = Modifier.testTag("toggle_dark_mode_button"),
                      ) {
                        Icon(
                          imageVector = if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                          contentDescription = "Toggle Appearance",
                        )
                      }
                      Button(
                        onClick = { openInvoiceBuilder(TxType.SALE) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp).testTag("top_new_sale_button"),
                      ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New Sale")
                      }
                    }
                  }
                  HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
              }
            },
          ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
              screenBody()
            }
          }
        }
      } else if (isMedium) {
        Row(modifier = Modifier.fillMaxSize()) {
          NavigationRail(
            containerColor = MaterialTheme.colorScheme.surface,
            header = {
              FloatingActionButton(
                onClick = { openInvoiceBuilder(TxType.SALE) },
                modifier = Modifier.padding(top = 8.dp).testTag("rail_new_sale_fab"),
              ) {
                Icon(Icons.Filled.Add, contentDescription = "New Sale Invoice")
              }
            },
          ) {
            desktopNavItems.forEach { target ->
              val selected = activeNav == target
              NavigationRailItem(
                selected = selected,
                onClick = { activeNav = target },
                icon = {
                  Icon(
                    if (selected) target.selectedIcon else target.unselectedIcon,
                    contentDescription = target.label,
                  )
                },
                label = { Text(target.compactLabel, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                modifier = Modifier.testTag("rail_${target.name.lowercase()}"),
              )
            }
          }

          Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
              TopAppBar(
                title = {
                  Column {
                    Text(businessTitle, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(activeNav.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  }
                },
                actions = {
                  IconButton(onClick = { showGlobalSearch = true }) {
                    Icon(Icons.Filled.Search, contentDescription = "Open Anything Search")
                  }
                  IconButton(
                    onClick = {
                      onDarkModePrefChange(if (isDarkTheme) DarkModePref.LIGHT else DarkModePref.DARK)
                    }
                  ) {
                    Icon(
                      if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                      contentDescription = "Toggle Appearance",
                    )
                  }
                },
              )
            },
          ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
              screenBody()
            }
          }
        }
      } else {
        val effectiveTabSelected: (AppNavTarget) -> Boolean = { tab ->
          when (tab) {
            AppNavTarget.MORE_HUB ->
              activeNav == AppNavTarget.MORE_HUB ||
                activeNav == AppNavTarget.PURCHASE_EXPENSE ||
                activeNav == AppNavTarget.AGENTS ||
                activeNav == AppNavTarget.CASH_BANK ||
                activeNav == AppNavTarget.REPORTS ||
                activeNav == AppNavTarget.SETTINGS
            else -> activeNav == tab
          }
        }

        Scaffold(
          snackbarHost = { SnackbarHost(snackbarHostState) },
          contentWindowInsets = WindowInsets.navigationBars,
          topBar = {
            TopAppBar(
              navigationIcon = {
                Image(
                  painter = painterResource(id = R.drawable.ic_dukaan_logo),
                  contentDescription = "Dukaan Stock",
                  contentScale = ContentScale.Fit,
                  modifier =
                    Modifier
                      .padding(start = 12.dp)
                      .size(36.dp)
                      .clickable { activeNav = AppNavTarget.HOME },
                )
              },
              title = {
                Column(modifier = Modifier.clickable { activeNav = AppNavTarget.SETTINGS }) {
                  Text(
                    text = businessTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                      text = activeNav.label,
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Box(
                      modifier = Modifier.size(6.dp).clip(CircleShape).background(
                        when {
                          !isOnline -> semantic.warning
                          hasPendingWrites -> MaterialTheme.colorScheme.primary
                          else -> semantic.positive
                        }
                      )
                    )
                    Text(
                      text = when {
                        !isOnline -> "Offline"
                        hasPendingWrites -> "Syncing"
                        else -> "Synced"
                      },
                      style = MaterialTheme.typography.labelSmall,
                      color = when {
                        !isOnline -> semantic.warning
                        hasPendingWrites -> MaterialTheme.colorScheme.primary
                        else -> semantic.positive
                      },
                    )
                  }
                }
              },
              actions = {
                IconButton(
                  onClick = { showGlobalSearch = true },
                  modifier = Modifier.testTag("mobile_search_button"),
                ) {
                  Icon(Icons.Filled.Search, contentDescription = "Search")
                }
                IconButton(
                  onClick = {
                    onDarkModePrefChange(if (isDarkTheme) DarkModePref.LIGHT else DarkModePref.DARK)
                  },
                  modifier = Modifier.testTag("mobile_theme_toggle_button"),
                ) {
                  Icon(
                    if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    contentDescription = "Toggle Light or Dark Mode",
                  )
                }
              },
            )
          },
          bottomBar = {
            NavigationBar(windowInsets = WindowInsets.navigationBars) {
              compactBottomItems.forEach { target ->
                val selected = effectiveTabSelected(target)
                NavigationBarItem(
                  selected = selected,
                  onClick = { activeNav = target },
                  icon = {
                    Icon(
                      if (selected) target.selectedIcon else target.unselectedIcon,
                      contentDescription = target.compactLabel,
                    )
                  },
                  label = { Text(target.compactLabel, maxLines = 1) },
                  modifier = Modifier.testTag("bottom_nav_${target.name.lowercase()}"),
                )
              }
            }
          },
          floatingActionButton = {
            if (activeNav == AppNavTarget.HOME || activeNav == AppNavTarget.SALE) {
              FloatingActionButton(
                onClick = { openInvoiceBuilder(TxType.SALE) },
                modifier = Modifier.testTag("mobile_new_sale_fab"),
              ) {
                Icon(Icons.Filled.Add, contentDescription = "New Sale Invoice")
              }
            }
          },
        ) { innerPadding ->
          Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            screenBody()
          }
        }
      }
    }

    // Shared Quick Voucher Dialog (Home "+ Expense" & Party "Pay-In / Pay-Out")
    val qvType = quickVoucherType
    if (qvType != null) {
      QuickVoucherDialog(
        txType = qvType,
        initialParty = quickVoucherParty,
        parties = parties.filter { it.isActive },
        accounts = accounts.filter { it.isActive },
        transactions = transactions,
        profile = profile,
        currencySymbol = profile?.currencySymbol ?: "₹",
        onDismiss = {
          quickVoucherType = null
          quickVoucherParty = null
        },
        onSave = { tx ->
          viewModel.recordTransaction(tx, incrementInvoiceSequence = true) {
            quickVoucherType = null
            quickVoucherParty = null
          }
        },
      )
    }

    // Shared Transaction Detail Preview Dialog when opened from Home, Parties, or Agents
    val detailTx = selectedTransactionForDetail
    if (detailTx != null && activeNav != AppNavTarget.SALE && activeNav != AppNavTarget.PURCHASE_EXPENSE) {
      val detailParty = parties.firstOrNull { it.id == detailTx.partyId }
      val canConvert =
        detailTx.status != TxStatus.CANCELLED.name &&
          detailTx.status != TxStatus.CONVERTED.name &&
          detailTx.txType in listOf(TxType.ESTIMATE.name, TxType.SALE_ORDER.name, TxType.DELIVERY_CHALLAN.name)
      val canReturn =
        detailTx.status != TxStatus.CANCELLED.name && detailTx.txType == TxType.SALE.name
      AlertDialog(
        onDismissRequest = { selectedTransactionForDetail = null },
        title = { Text("${detailTx.txTypeEnum().label} • ${detailTx.refNumber}") },
        text = {
          Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            TaxInvoicePreviewCard(
              transaction = detailTx,
              profile = profile,
              party = detailParty,
              accentColor = MaterialTheme.colorScheme.primary,
            )
          }
        },
        confirmButton = {
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            if (detailTx.status != TxStatus.CANCELLED.name) {
              OutlinedButton(
                onClick = {
                  cancelConfirmDetailTx = detailTx
                  selectedTransactionForDetail = null
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
              ) {
                Text("Cancel")
              }
            }
            if (canConvert) {
              FilledTonalButton(
                onClick = {
                  selectedTransactionForDetail = null
                  openInvoiceBuilder(TxType.SALE, convertFrom = detailTx)
                },
              ) {
                Icon(Icons.Filled.Transform, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Convert to Sale")
              }
            }
            if (canReturn) {
              FilledTonalButton(
                onClick = {
                  selectedTransactionForDetail = null
                  openInvoiceBuilder(TxType.CREDIT_NOTE, creditForSale = detailTx)
                },
              ) {
                Icon(Icons.AutoMirrored.Filled.AssignmentReturn, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Issue Return")
              }
            }
            Button(onClick = { selectedTransactionForDetail = null }) {
              Text("Close")
            }
          }
        },
      )
    }

    val cancelDetail = cancelConfirmDetailTx
    if (cancelDetail != null) {
      CancelTransactionDialog(
        tx = cancelDetail,
        onDismiss = { cancelConfirmDetailTx = null },
        onConfirm = { reason ->
          viewModel.cancelTransaction(cancelDetail.id, reason)
          cancelConfirmDetailTx = null
        },
      )
    }

    if (showReauthDialog) {
      AlertDialog(
        onDismissRequest = { showReauthDialog = false },
        title = { Text("Re-authentication Required") },
        text = {
          Text(
            "For your security, deleting your Google account requires a fresh sign-in session. Please sign out, sign back in with Google, and then retry account deletion."
          )
        },
        confirmButton = {
          Button(
            onClick = {
              showReauthDialog = false
              onSignOut()
            }
          ) { Text("Sign Out Now") }
        },
        dismissButton = {
          TextButton(onClick = { showReauthDialog = false }) { Text("Later") }
        },
      )
    }

    // Global "Open Anything" Search Dialog
    if (showGlobalSearch) {
      GlobalSearchDialog(
        parties = parties.filter { it.isActive },
        items = items.filter { it.isActive },
        agents = agents.filter { it.isActive },
        transactions = transactions,
        currencySymbol = profile?.currencySymbol ?: "₹",
        onDismiss = { showGlobalSearch = false },
        onNavigate = { target ->
          activeNav = target
          showGlobalSearch = false
        },
        onNewInvoice = { type ->
          showGlobalSearch = false
          openInvoiceBuilder(type)
        },
        onSelectTransaction = { tx ->
          showGlobalSearch = false
          selectedTransactionForDetail = tx
        },
      )
    }
  }
}

@Composable
private fun MoreHubScreen(
  businessName: String,
  userEmail: String,
  onNavigate: (AppNavTarget) -> Unit,
) {
  val semantic = LocalDukaanColors.current
  val entries =
    listOf(
      Triple(
        AppNavTarget.PURCHASE_EXPENSE,
        "Purchase Bills, Payment-Out & Expenses",
        MaterialTheme.colorScheme.primary,
      ),
      Triple(
        AppNavTarget.AGENTS,
        "Mistri, Agents, Salesmen & Commission Payouts",
        MaterialTheme.colorScheme.tertiary,
      ),
      Triple(
        AppNavTarget.CASH_BANK,
        "Cash Registers, Bank Accounts & Account Transfers",
        semantic.positive,
      ),
      Triple(
        AppNavTarget.REPORTS,
        "GSTR-1/2/3B, Profit & Loss, Commission & CSV Reports",
        semantic.warning,
      ),
      Triple(
        AppNavTarget.SETTINGS,
        "Business Profile, GSTIN, UPI ID, Theme & Account",
        MaterialTheme.colorScheme.secondary,
      ),
    )

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
          Text(businessName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
          Text(
            userEmail.ifBlank { "Google Cloud Sync Active" },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    items(entries) { (target, subtitle, tint) ->
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier =
          Modifier
            .fillMaxWidth()
            .clickable { onNavigate(target) }
            .testTag("more_hub_${target.name.lowercase()}"),
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier =
              Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = target.selectedIcon,
              contentDescription = null,
              tint = tint,
              modifier = Modifier.size(24.dp),
            )
          }
          Spacer(modifier = Modifier.width(14.dp))
          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              Text(target.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
              if (target == AppNavTarget.AGENTS) {
                NewFeatureBadge()
              }
            }
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          Spacer(modifier = Modifier.width(8.dp))
          Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
private fun GlobalSearchDialog(
  parties: List<Party>,
  items: List<InventoryItem>,
  agents: List<Agent>,
  transactions: List<BusinessTransaction>,
  currencySymbol: String,
  onDismiss: () -> Unit,
  onNavigate: (AppNavTarget) -> Unit,
  onNewInvoice: (TxType) -> Unit,
  onSelectTransaction: (BusinessTransaction) -> Unit,
) {
  var query by remember { mutableStateOf("") }
  val q = query.trim()

  val matchingScreens =
    listOf(
      AppNavTarget.HOME,
      AppNavTarget.PARTIES,
      AppNavTarget.ITEMS,
      AppNavTarget.SALE,
      AppNavTarget.PURCHASE_EXPENSE,
      AppNavTarget.AGENTS,
      AppNavTarget.CASH_BANK,
      AppNavTarget.REPORTS,
      AppNavTarget.SETTINGS,
    ).filter { q.isBlank() || it.label.contains(q, ignoreCase = true) }

  val matchingParties =
    if (q.isBlank()) emptyList()
    else parties.filter { it.name.contains(q, true) || it.phone.contains(q, true) }.take(5)

  val matchingItems =
    if (q.isBlank()) emptyList()
    else items.filter { it.name.contains(q, true) || it.itemCode.contains(q, true) }.take(5)

  val matchingAgents =
    if (q.isBlank()) emptyList()
    else agents.filter { it.name.contains(q, true) || it.phone.contains(q, true) || it.cityArea.contains(q, true) }.take(5)

  val matchingTxs =
    if (q.isBlank()) emptyList()
    else transactions.filter { it.refNumber.contains(q, true) || it.partyName.contains(q, true) }.take(5)

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Open Anything") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = query,
          onValueChange = { query = it },
          placeholder = { Text("Jump to screen, party, item, agent, or invoice...") },
          leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("global_search_input"),
        )
        LazyColumn(
          modifier = Modifier.height(300.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          if (q.isBlank() || "new sale invoice".contains(q, ignoreCase = true)) {
            item {
              SearchHitRow("Create New Sale Invoice", "Quick Billing") {
                onNewInvoice(TxType.SALE)
              }
            }
          }
          if (q.isBlank() || "new purchase bill".contains(q, ignoreCase = true)) {
            item {
              SearchHitRow("Create New Purchase Bill", "Inventory Restock") {
                onNewInvoice(TxType.PURCHASE)
              }
            }
          }
          items(matchingScreens) { target ->
            SearchHitRow(target.label, "Section") { onNavigate(target) }
          }
          items(matchingParties) { p ->
            SearchHitRow(p.name, "Party • ${formatCurrency(p.currentBalance, currencySymbol)}") {
              onNavigate(AppNavTarget.PARTIES)
            }
          }
          items(matchingItems) { item ->
            SearchHitRow(item.name, "Item • ${formatCurrency(item.salePrice, currencySymbol)}") {
              onNavigate(AppNavTarget.ITEMS)
            }
          }
          items(matchingAgents) { ag ->
            SearchHitRow(ag.name, "Agent / Mistri • Pay ${formatCurrency(ag.currentBalance, currencySymbol)}") {
              onNavigate(AppNavTarget.AGENTS)
            }
          }
          items(matchingTxs) { tx ->
            SearchHitRow(
              "${tx.refNumber} • ${tx.partyName}",
              "${tx.txTypeEnum().shortLabel} • ${formatCurrency(tx.totalAmount, currencySymbol)}",
            ) {
              onSelectTransaction(tx)
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text("Close") }
    },
  )
}

@Composable
private fun SearchHitRow(
  title: String,
  subtitle: String,
  onClick: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
      Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
    }
  }
}
