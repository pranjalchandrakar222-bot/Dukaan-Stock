package com.example.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.VerifiedUser
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
import androidx.compose.ui.geometry.Offset
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

  val handleGoogleClick: () -> Unit = {
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
  }

  BoxWithConstraints(
    modifier =
      Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
  ) {
    val isWide = maxWidth >= 840.dp

    if (isWide) {
      Row(modifier = Modifier.fillMaxSize()) {
        // Left Stitch Hero Panel for Tablet / Desktop
        Box(
          modifier =
            Modifier
              .weight(1.08f)
              .fillMaxHeight()
              .background(
                Brush.linearGradient(
                  colors = listOf(Color(0xFF0A39A6), Color(0xFF0B2563), Color(0xFF0E1730))
                )
              )
        ) {
          HeroGlowBackdrop(modifier = Modifier.fillMaxSize())
          Column(
            modifier =
              Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 48.dp, vertical = 40.dp)
                .widthIn(max = 520.dp)
                .align(Alignment.CenterStart),
            verticalArrangement = Arrangement.Center,
          ) {
            BrandHeaderBlock(centered = false)
            Spacer(modifier = Modifier.height(28.dp))
            Text(
              text = "Smart GST Billing, Live Stock & Party Khata in one place.",
              style = MaterialTheme.typography.headlineSmall,
              color = Color.White,
              fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text =
                "Built for shopkeepers, wholesalers, and distributors across mobile and computer.",
              style = MaterialTheme.typography.bodyLarge,
              color = Color(0xFFCBD5E1),
            )
            Spacer(modifier = Modifier.height(28.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
              GlassFeatureCard(
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                title = "Instant GST Tax Invoicing",
                subtitle = "Live invoice preview, CGST/SGST split & one-tap payment tracking",
              )
              GlassFeatureCard(
                icon = Icons.Filled.Inventory2,
                title = "Live Inventory & Low Stock Alerts",
                subtitle = "Automatic stock updates on every sale, purchase & return",
              )
              GlassFeatureCard(
                icon = Icons.Filled.People,
                title = "Party Khata & Cash/Bank Ledger",
                subtitle = "Real-time receivables, payables, expenses & UPI balances",
              )
            }
          }
        }

        // Right Sign-In Card
        Box(
          modifier =
            Modifier
              .weight(0.92f)
              .fillMaxHeight()
              .safeDrawingPadding()
              .padding(32.dp),
          contentAlignment = Alignment.Center,
        ) {
          SignInActionCard(
            isLoading = isLoading,
            isCheckingSilent = isCheckingSilent,
            errorMessage = errorMessage,
            onSignInClick = handleGoogleClick,
            modifier = Modifier.widthIn(max = 420.dp),
          )
        }
      }
    } else {
      // Mobile / Compact First Impression: Vibrant Hero Header + Overlapping Elevated Sign-In Card
      Column(
        modifier =
          Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Box(
          modifier =
            Modifier
              .fillMaxWidth()
              .background(
                Brush.linearGradient(
                  colors = listOf(Color(0xFF0456D6), Color(0xFF012F9E), Color(0xFF0B1C47))
                ),
                shape = RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp),
              )
        ) {
          HeroGlowBackdrop(
            modifier =
              Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
          )
          Column(
            modifier =
              Modifier
                .fillMaxWidth()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 28.dp, bottom = 52.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            BrandHeaderBlock(centered = true)
            Spacer(modifier = Modifier.height(14.dp))
            Text(
              text = "Fast GST billing, stock tracking & party khata synced across mobile and computer.",
              style = MaterialTheme.typography.bodyMedium,
              color = Color(0xFFDBEAFE),
              textAlign = TextAlign.Center,
              modifier = Modifier.widthIn(max = 360.dp),
            )
          }
        }

        // Overlapping Elevated Action Card visible immediately above the fold
        Column(
          modifier =
            Modifier
              .fillMaxWidth()
              .widthIn(max = 460.dp)
              .offset(y = (-28).dp)
              .padding(horizontal = 18.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          SignInActionCard(
            isLoading = isLoading,
            isCheckingSilent = isCheckingSilent,
            errorMessage = errorMessage,
            onSignInClick = handleGoogleClick,
          )

          Spacer(modifier = Modifier.height(16.dp))

          // Compact 3-pill feature highlights bar
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            CompactFeatureBadge(
              icon = Icons.AutoMirrored.Filled.ReceiptLong,
              label = "GST Bills",
              modifier = Modifier.weight(1f),
            )
            CompactFeatureBadge(
              icon = Icons.Filled.Inventory2,
              label = "Live Stock",
              modifier = Modifier.weight(1f),
            )
            CompactFeatureBadge(
              icon = Icons.Filled.People,
              label = "Khata Sync",
              modifier = Modifier.weight(1f),
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
          ) {
            Icon(
              imageVector = Icons.Filled.VerifiedUser,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Secured by Google Sign-In & Private Cloud Backup",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun HeroGlowBackdrop(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    drawCircle(
      color = Color.White.copy(alpha = 0.08f),
      radius = size.minDimension * 0.42f,
      center = Offset(size.width * 0.88f, size.height * 0.16f),
    )
    drawCircle(
      color = Color(0xFF60A5FA).copy(alpha = 0.12f),
      radius = size.minDimension * 0.30f,
      center = Offset(size.width * 0.10f, size.height * 0.85f),
    )
  }
}

@Composable
private fun BrandHeaderBlock(centered: Boolean) {
  if (centered) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.White.copy(alpha = 0.14f),
        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.30f)),
        shadowElevation = 8.dp,
      ) {
        Image(
          painter = painterResource(id = R.drawable.ic_dukaan_logo),
          contentDescription = "Dukaan Stock Logo",
          contentScale = ContentScale.Fit,
          modifier = Modifier.padding(4.dp).size(72.dp).clip(RoundedCornerShape(18.dp)),
        )
      }
      Spacer(modifier = Modifier.height(14.dp))
      Text(
        text = "Dukaan Stock",
        style = MaterialTheme.typography.headlineMedium,
        color = Color.White,
        fontWeight = FontWeight.ExtraBold,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color.White.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
      ) {
        Text(
          text = "GST BILLING  •  INVENTORY  •  KHATA",
          style = MaterialTheme.typography.labelSmall,
          color = Color(0xFFE0F2FE),
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
        )
      }
    }
  } else {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.14f),
        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.28f)),
        shadowElevation = 8.dp,
      ) {
        Image(
          painter = painterResource(id = R.drawable.ic_dukaan_logo),
          contentDescription = "Dukaan Stock Logo",
          contentScale = ContentScale.Fit,
          modifier = Modifier.padding(4.dp).size(68.dp).clip(RoundedCornerShape(16.dp)),
        )
      }
      Spacer(modifier = Modifier.width(16.dp))
      Column {
        Text(
          text = "Dukaan Stock",
          style = MaterialTheme.typography.headlineMedium,
          color = Color.White,
          fontWeight = FontWeight.ExtraBold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
          shape = RoundedCornerShape(999.dp),
          color = Color.White.copy(alpha = 0.16f),
        ) {
          Text(
            text = "GST Billing • Stock • Khata",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFFE0F2FE),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun GlassFeatureCard(
  icon: ImageVector,
  title: String,
  subtitle: String,
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = Color.White.copy(alpha = 0.09f),
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(22.dp),
        )
      }
      Spacer(modifier = Modifier.width(14.dp))
      Column {
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall,
          color = Color.White,
          fontWeight = FontWeight.SemiBold,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = Color(0xFFCBD5E1),
        )
      }
    }
  }
}

@Composable
private fun CompactFeatureBadge(
  icon: ImageVector,
  label: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(
      modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Box(
        modifier =
          Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.size(18.dp),
        )
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        maxLines = 1,
      )
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
    shape = RoundedCornerShape(26.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 4.dp,
    shadowElevation = 6.dp,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(
      modifier = Modifier.padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(50.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Filled.CloudDone,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(24.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))
      Text(
        text = "Sign in to your Store",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "Sync your invoices, stock levels, and customer balances safely in real time.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(20.dp))

      AnimatedVisibility(
        visible = errorMessage != null,
        enter = fadeIn(),
        exit = fadeOut(),
      ) {
        Surface(
          color = MaterialTheme.colorScheme.errorContainer,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
        ) {
          Text(
            text = errorMessage.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(12.dp),
          )
        }
      }

      Button(
        onClick = onSignInClick,
        enabled = !isLoading && !isCheckingSilent,
        shape = RoundedCornerShape(16.dp),
        colors =
          ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
        modifier =
          Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("google_sign_in_button"),
      ) {
        if (isLoading || isCheckingSilent) {
          CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            strokeWidth = 2.5.dp,
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = if (isCheckingSilent) "Checking account..." else "Signing in...",
            style = MaterialTheme.typography.labelLarge,
          )
        } else {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Login,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "Continue with Google",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            Icons.Filled.Devices,
            contentDescription = null,
            modifier = Modifier.size(15.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            "Mobile & Desktop",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            Icons.Filled.AccountBalanceWallet,
            contentDescription = null,
            modifier = Modifier.size(15.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            "Instant Cloud Sync",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}
