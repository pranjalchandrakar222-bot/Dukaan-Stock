package com.example.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.ui.theme.LocalDukaanColors

@Composable
fun SignInScreen(
  onAuthSuccess: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }
  var isLoading by remember { mutableStateOf(false) }
  var isCheckingSilent by remember { mutableStateOf(true) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = {
        isCheckingSilent = false
        onAuthSuccess()
      },
      onUnauthenticated = {
        isCheckingSilent = false
      },
      scope = scope,
    )
  }

  val semantic = LocalDukaanColors.current

  BoxWithConstraints(
    modifier =
      Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .safeDrawingPadding()
  ) {
    val isWide = maxWidth >= 840.dp

    if (isWide) {
      Row(modifier = Modifier.fillMaxSize()) {
        // Left Stitch Hero Panel for Tablet / Desktop
        Box(
          modifier =
            Modifier
              .weight(1.05f)
              .fillMaxHeight()
              .background(
                Brush.linearGradient(
                  colors = listOf(semantic.sidebarBg, Color(0xFF1E294E))
                )
              )
              .padding(48.dp)
        ) {
          SignInHeroContent(
            isDarkPanel = true,
            modifier = Modifier.align(Alignment.CenterStart).widthIn(max = 500.dp),
          )
        }

        // Right Sign-In Card
        Box(
          modifier = Modifier.weight(0.95f).fillMaxHeight().padding(32.dp),
          contentAlignment = Alignment.Center,
        ) {
          SignInActionCard(
            isLoading = isLoading,
            isCheckingSilent = isCheckingSilent,
            errorMessage = errorMessage,
            onSignInClick = {
              errorMessage = null
              isLoading = true
              onGoogleSignInClicked(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                  isLoading = false
                  onAuthSuccess()
                },
                onAuthError = { msg ->
                  isLoading = false
                  errorMessage = msg
                },
                scope = scope,
                onAuthCancelled = { isLoading = false },
              )
            },
            modifier = Modifier.widthIn(max = 420.dp),
          )
        }
      }
    } else {
      Column(
        modifier =
          Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        SignInHeroContent(
          isDarkPanel = false,
          modifier = Modifier.widthIn(max = 460.dp),
        )
        Spacer(modifier = Modifier.height(24.dp))
        SignInActionCard(
          isLoading = isLoading,
          isCheckingSilent = isCheckingSilent,
          errorMessage = errorMessage,
          onSignInClick = {
            errorMessage = null
            isLoading = true
            onGoogleSignInClicked(
              context = context,
              credentialManager = credentialManager,
              onAuthSuccess = {
                isLoading = false
                onAuthSuccess()
              },
              onAuthError = { msg ->
                isLoading = false
                errorMessage = msg
              },
              scope = scope,
              onAuthCancelled = { isLoading = false },
            )
          },
          modifier = Modifier.widthIn(max = 460.dp),
        )
      }
    }
  }
}

@Composable
private fun SignInHeroContent(
  isDarkPanel: Boolean,
  modifier: Modifier = Modifier,
) {
  val titleColor = if (isDarkPanel) Color.White else MaterialTheme.colorScheme.onBackground
  val bodyColor =
    if (isDarkPanel) Color(0xFFCBD5E1) else MaterialTheme.colorScheme.onSurfaceVariant
  val chipBg =
    if (isDarkPanel) Color(0xFF1F2A4D) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
  val chipContent =
    if (isDarkPanel) Color(0xFFE2E8F0) else MaterialTheme.colorScheme.onPrimaryContainer

  Column(modifier = modifier) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Image(
        painter = painterResource(id = R.drawable.ic_dukaan_logo_1794570957444),
        contentDescription = "Dukaan Stock Logo",
        contentScale = ContentScale.Crop,
        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)),
      )
      Spacer(modifier = Modifier.width(16.dp))
      Column {
        Text(
          text = "Dukaan Stock",
          style = MaterialTheme.typography.headlineMedium,
          color = titleColor,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = "Smart GST Billing, Stock & Khata",
          style = MaterialTheme.typography.bodyMedium,
          color = bodyColor,
        )
      }
    }

    Spacer(modifier = Modifier.height(28.dp))

    Text(
      text = "Run your entire business from phone or desktop.",
      style = MaterialTheme.typography.headlineSmall,
      color = titleColor,
    )
    Spacer(modifier = Modifier.height(10.dp))
    Text(
      text =
        "Create GST tax invoices with live preview, track item stock & low-stock alerts, manage customer/supplier ledgers, and view GSTR & P&L reports synced in real time.",
      style = MaterialTheme.typography.bodyLarge,
      color = bodyColor,
    )

    Spacer(modifier = Modifier.height(24.dp))

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      FeatureRow(
        icon = Icons.AutoMirrored.Filled.ReceiptLong,
        title = "Instant GST Tax Invoicing",
        subtitle = "Live invoice preview, CGST/SGST split & share",
        bg = chipBg,
        fg = chipContent,
        titleColor = titleColor,
        subtitleColor = bodyColor,
      )
      FeatureRow(
        icon = Icons.Filled.Inventory2,
        title = "Live Inventory & Low Stock Alerts",
        subtitle = "Auto stock deduction on sales & restock on purchases",
        bg = chipBg,
        fg = chipContent,
        titleColor = titleColor,
        subtitleColor = bodyColor,
      )
      FeatureRow(
        icon = Icons.Filled.People,
        title = "Party Khata & Cash/Bank Ledger",
        subtitle = "Track receivables, payables, expenses & UPI balances",
        bg = chipBg,
        fg = chipContent,
        titleColor = titleColor,
        subtitleColor = bodyColor,
      )
    }
  }
}

@Composable
private fun FeatureRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  bg: Color,
  fg: Color,
  titleColor: Color,
  subtitleColor: Color,
) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
      modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(bg),
      contentAlignment = Alignment.Center,
    ) {
      Icon(imageVector = icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
    }
    Spacer(modifier = Modifier.width(12.dp))
    Column {
      Text(text = title, style = MaterialTheme.typography.titleSmall, color = titleColor)
      Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = subtitleColor)
    }
  }
}

@Composable
private fun SignInActionCard(
  isLoading: Boolean,
  isCheckingSilent: Boolean,
  errorMessage: String?,
  onSignInClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(24.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(
      modifier = Modifier.padding(28.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(52.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Filled.CloudDone,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
      Text(
        text = "Welcome to Dukaan Stock",
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "Sign in with Google to securely sync your business books, inventory, and invoices across devices.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(22.dp))

      if (errorMessage != null) {
        Surface(
          color = MaterialTheme.colorScheme.errorContainer,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        ) {
          Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(12.dp),
          )
        }
      }

      Button(
        onClick = onSignInClick,
        enabled = !isLoading && !isCheckingSilent,
        shape = RoundedCornerShape(14.dp),
        colors =
          ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
        modifier =
          Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("google_sign_in_button"),
      ) {
        if (isLoading || isCheckingSilent) {
          CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            strokeWidth = 2.5.dp,
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(if (isCheckingSilent) "Checking session..." else "Signing in...")
        } else {
          Text(
            text = "Sign in with Google",
            style = MaterialTheme.typography.labelLarge,
          )
        }
      }

      Spacer(modifier = Modifier.height(18.dp))

      Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            Icons.Filled.Devices,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            "Mobile & Desktop UI",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            Icons.Filled.AccountBalanceWallet,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            "Private Cloud Sync",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}
