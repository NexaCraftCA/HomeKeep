package ca.nexacraft.homekeep.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import ca.nexacraft.homekeep.notify.ReminderScheduler
import ca.nexacraft.homekeep.ui.components.ConfirmDialog
import ca.nexacraft.homekeep.ui.components.SectionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    email: String?,
    busy: Boolean,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val privacyUrl = remember { "https://nexacraftca.github.io/HomeKeep/privacy.html" }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionCard(title = "Account") {
                Text(
                    text = email ?: "Signed in",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = "Your assets are private to this account and protected by row level security.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SectionCard(title = "Reminders") {
                Text(
                    text = "HomeKeep checks once a day and notifies you about maintenance due in the next week. " +
                        "Android may delay this when battery saver is on.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = { ReminderScheduler.runNow(context) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Check reminders now")
                }
            }

            SectionCard(title = "About") {
                Text("HomeKeep 1.0.0", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "A quiet register for the things you own: receipts, serials, warranties, repairs and upkeep.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, privacyUrl.toUri()),
                            )
                        }
                    },
                ) {
                    Text("Privacy policy")
                }
            }

            Button(
                onClick = onSignOut,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Text("Sign out")
            }

            TextButton(
                onClick = { confirmDelete = true },
                enabled = !busy,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Delete my account and data")
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete your account?",
            message = "This permanently deletes your account, assets, files, repairs and reminders. There is no undo.",
            confirmLabel = "Delete everything",
            onConfirm = {
                confirmDelete = false
                onDeleteAccount()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
