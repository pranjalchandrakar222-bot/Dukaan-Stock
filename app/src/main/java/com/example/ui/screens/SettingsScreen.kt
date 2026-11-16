package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BusinessProfile
import com.example.data.model.ThemeStyle
import com.example.data.model.themeStyleEnum
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalDukaanColors
import com.example.ui.theme.SlatePrimary
import com.example.ui.theme.TerracottaPrimary
import com.example.ui.util.TaxAndCommissionCalculator

enum class DarkModePref(val label: String) {
  SYSTEM("System"),
  LIGHT("Light"),
  DARK("Dark"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  profile: BusinessProfile?,
  userEmail: String,
  darkModePref: DarkModePref,
  onDarkModePrefChange: (DarkModePref) -> Unit,
  onSaveProfile: (BusinessProfile) -> Unit,
  onSelectThemeStyle: (ThemeStyle) -> Unit,
  onSignOut: () -> Unit,
  onDeleteAccount: () -> Unit,
) {
  val semantic = LocalDukaanColors.current

  var businessName by remember(profile) { mutableStateOf(profile?.businessName ?: "My Dukaan Store") }
  var phone by remember(profile) { mutableStateOf(profile?.phone ?: "") }
  var email by remember(profile) { mutableStateOf(profile?.email ?: userEmail) }
  var gstin by remember(profile) { mutableStateOf(profile?.gstin ?: "") }
  var address by remember(profile) { mutableStateOf(profile?.address ?: "") }
  var stateName by remember(profile) { mutableStateOf(profile?.stateName ?: "") }
  var stateMenuExpanded by remember { mutableStateOf(false) }
  var currencySymbol by remember(profile) { mutableStateOf(profile?.currencySymbol ?: "₹") }
  var invoicePrefix by remember(profile) { mutableStateOf(profile?.invoicePrefix ?: "INV-") }
  var useFinancialYearPrefix by remember(profile) { mutableStateOf(profile?.useFinancialYearPrefix ?: true) }
  var upiId by remember(profile) { mutableStateOf(profile?.upiId ?: "") }
  var bankDetails by remember(profile) { mutableStateOf(profile?.bankDetails ?: "") }
  var invoiceTerms by remember(profile) { mutableStateOf(profile?.invoiceTerms ?: "Thanks for doing business with us!") }
  var stopNegativeStock by remember(profile) { mutableStateOf(profile?.stopNegativeStock ?: false) }
  var roundOffTotal by remember(profile) { mutableStateOf(profile?.roundOffTotal ?: true) }
  val activeTheme = profile?.themeStyleEnum() ?: ThemeStyle.COBALT

  var showDeleteStep1 by remember { mutableStateOf(false) }
  var showDeleteStep2 by remember { mutableStateOf(false) }
  var deleteConfirmText by remember { mutableStateOf("") }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item {
      Text("Business & App Settings", style = MaterialTheme.typography.headlineSmall)
    }

    // Dynamic Color & Google Stitch Theme Card
    item {
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Dynamic Color & Stitch Theme", style = MaterialTheme.typography.titleMedium)
          }

          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            ThemeStyle.entries.forEach { style ->
              val selected = activeTheme == style
              val swatch =
                when (style) {
                  ThemeStyle.DYNAMIC -> MaterialTheme.colorScheme.primary
                  ThemeStyle.COBALT -> CobaltPrimary
                  ThemeStyle.EMERALD -> EmeraldPrimary
                  ThemeStyle.TERRACOTTA -> TerracottaPrimary
                  ThemeStyle.SLATE -> SlatePrimary
                }
              Surface(
                shape = RoundedCornerShape(14.dp),
                color =
                  if (selected) MaterialTheme.colorScheme.primaryContainer
                  else MaterialTheme.colorScheme.surfaceContainerLow,
                border =
                  BorderStroke(
                    width = if (selected) 1.5.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                  ),
                modifier = Modifier.clickable { onSelectThemeStyle(style) }.testTag("theme_option_${style.name}"),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Box(
                    modifier =
                      Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(swatch)
                        .border(1.dp, Color.White.copy(alpha = 0.7f), CircleShape),
                    contentAlignment = Alignment.Center,
                  ) {
                    if (selected) {
                      Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                  }
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(style.label, style = MaterialTheme.typography.labelLarge)
                }
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Filled.DarkMode, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Appearance Mode", style = MaterialTheme.typography.bodyMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              DarkModePref.entries.forEach { pref ->
                FilterChip(
                  selected = darkModePref == pref,
                  onClick = { onDarkModePrefChange(pref) },
                  label = { Text(pref.label) },
                )
              }
            }
          }
        }
      }
    }

    // Business Profile Form
    item {
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text("Business Profile, GST & Payments", style = MaterialTheme.typography.titleMedium)

          OutlinedTextField(
            value = businessName,
            onValueChange = { businessName = it },
            label = { Text("Business / Store Name *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("settings_business_name_input"),
          )

          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
              value = phone,
              onValueChange = { phone = it },
              label = { Text("Phone") },
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
              value = gstin,
              onValueChange = {
                val up = it.uppercase()
                gstin = up
                if (stateName.isBlank()) {
                  val derived = TaxAndCommissionCalculator.stateFromGstin(up)
                  if (derived.isNotBlank()) stateName = derived
                }
              },
              label = { Text("GSTIN") },
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
          }

          OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Business Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
          )

          OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Business Address") },
            modifier = Modifier.fillMaxWidth(),
          )

          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ExposedDropdownMenuBox(
              expanded = stateMenuExpanded,
              onExpandedChange = { stateMenuExpanded = it },
              modifier = Modifier.weight(1f),
            ) {
              OutlinedTextField(
                value = stateName,
                onValueChange = { stateName = it },
                label = { Text("Home State (for CGST/IGST)") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = stateMenuExpanded) },
                singleLine = true,
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth(),
              )
              val matchingStates = TaxAndCommissionCalculator.indianStates.filter {
                stateName.isBlank() || it.contains(stateName, ignoreCase = true)
              }
              if (matchingStates.isNotEmpty()) {
                ExposedDropdownMenu(
                  expanded = stateMenuExpanded,
                  onDismissRequest = { stateMenuExpanded = false },
                ) {
                  matchingStates.take(8).forEach { st ->
                    DropdownMenuItem(
                      text = { Text(st) },
                      onClick = {
                        stateName = st
                        stateMenuExpanded = false
                      },
                    )
                  }
                }
              }
            }
            OutlinedTextField(
              value = invoicePrefix,
              onValueChange = { invoicePrefix = it },
              label = { Text("Invoice Prefix") },
              singleLine = true,
              modifier = Modifier.weight(0.7f),
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Financial Year Invoice Prefix", style = MaterialTheme.typography.bodyMedium)
              Text("Format e.g. INV/26-27/0001", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
              checked = useFinancialYearPrefix,
              onCheckedChange = { useFinancialYearPrefix = it },
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Business Currency", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              listOf("₹", "$", "€", "£").forEach { sym ->
                FilterChip(
                  selected = currencySymbol == sym,
                  onClick = { currencySymbol = sym },
                  label = { Text(sym) },
                )
              }
            }
          }

          OutlinedTextField(
            value = upiId,
            onValueChange = { upiId = it },
            label = { Text("Business UPI ID (for Pay via UPI button on invoices)") },
            placeholder = { Text("e.g. myshop@okicici") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("settings_upi_input"),
          )

          OutlinedTextField(
            value = bankDetails,
            onValueChange = { bankDetails = it },
            label = { Text("Bank Details (shown on Tax Invoices)") },
            modifier = Modifier.fillMaxWidth(),
          )

          OutlinedTextField(
            value = invoiceTerms,
            onValueChange = { invoiceTerms = it },
            label = { Text("Invoice Terms & Conditions") },
            modifier = Modifier.fillMaxWidth(),
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Stop Sale on Negative Stock", style = MaterialTheme.typography.bodyMedium)
              Text("Enforce stock limit during billing & adjustments", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = stopNegativeStock, onCheckedChange = { stopNegativeStock = it })
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("Round Off Invoice Totals", style = MaterialTheme.typography.bodyMedium)
              Text("Automatically round totals to nearest unit", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = roundOffTotal, onCheckedChange = { roundOffTotal = it })
          }

          Button(
            onClick = {
              val base = profile ?: BusinessProfile()
              onSaveProfile(
                base.copy(
                  businessName = businessName.trim().ifBlank { "My Dukaan Store" },
                  phone = phone.trim(),
                  email = email.trim(),
                  gstin = gstin.trim().uppercase(),
                  address = address.trim(),
                  stateName = stateName.trim(),
                  currencySymbol = currencySymbol,
                  invoicePrefix = invoicePrefix.trim(),
                  useFinancialYearPrefix = useFinancialYearPrefix,
                  upiId = upiId.trim(),
                  bankDetails = bankDetails.trim(),
                  invoiceTerms = invoiceTerms.trim(),
                  stopNegativeStock = stopNegativeStock,
                  roundOffTotal = roundOffTotal,
                )
              )
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_settings_button"),
          ) {
            Text("Save Business Settings")
          }
        }
      }
    }

    // Cloud Sync & Google Account Card
    item {
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Icon(Icons.Filled.CloudDone, contentDescription = null, tint = semantic.positive)
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text("Cloud Sync & Backup Active", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                  userEmail.ifBlank { "Signed in with Google" },
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            OutlinedButton(
              onClick = onSignOut,
              colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.testTag("sign_out_button"),
            ) {
              Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Sign Out")
            }
          }

          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(14.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  "Delete Account & Cloud Data",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.error,
                )
                Text(
                  "Permanently erase all invoices, parties, items, agents, and account credentials.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              OutlinedButton(
                onClick = { showDeleteStep1 = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("delete_account_button"),
              ) {
                Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Delete")
              }
            }
          }
        }
      }
    }
  }

  if (showDeleteStep1) {
    AlertDialog(
      onDismissRequest = { showDeleteStep1 = false },
      title = { Text("Delete Account & All Data?") },
      text = {
        Text(
          "This permanently removes your business profile, parties, inventory items, agents, commission ledgers, cash/bank accounts, and every invoice from the cloud. Export any GST/PDF reports you need before continuing."
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showDeleteStep1 = false
            deleteConfirmText = ""
            showDeleteStep2 = true
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) {
          Text("Continue")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteStep1 = false }) { Text("Cancel") }
      },
    )
  }

  if (showDeleteStep2) {
    AlertDialog(
      onDismissRequest = { showDeleteStep2 = false },
      title = { Text("Final Confirmation") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Type DELETE in uppercase below to permanently erase your account.")
          OutlinedTextField(
            value = deleteConfirmText,
            onValueChange = { deleteConfirmText = it.trim().uppercase() },
            label = { Text("Type DELETE") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("confirm_delete_account_input"),
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            showDeleteStep2 = false
            onDeleteAccount()
          },
          enabled = deleteConfirmText == "DELETE",
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          modifier = Modifier.testTag("confirm_delete_account_button"),
        ) {
          Text("Delete Permanently")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteStep2 = false }) { Text("Cancel") }
      },
    )
  }
}
