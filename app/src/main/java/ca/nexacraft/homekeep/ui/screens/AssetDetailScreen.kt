package ca.nexacraft.homekeep.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ca.nexacraft.homekeep.data.AssetDetail
import ca.nexacraft.homekeep.data.Attachment
import ca.nexacraft.homekeep.data.Reminder
import ca.nexacraft.homekeep.data.ReminderInput
import ca.nexacraft.homekeep.data.Repair
import ca.nexacraft.homekeep.data.RepairInput
import ca.nexacraft.homekeep.ui.components.ConfirmDialog
import ca.nexacraft.homekeep.ui.components.DateField
import ca.nexacraft.homekeep.ui.components.EmptyState
import ca.nexacraft.homekeep.ui.components.InfoRow
import ca.nexacraft.homekeep.ui.components.SectionCard
import ca.nexacraft.homekeep.ui.components.WarrantyBadge
import ca.nexacraft.homekeep.util.Dates
import ca.nexacraft.homekeep.util.formatMoney
import coil.compose.AsyncImage
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetDetailScreen(
    detail: AssetDetail?,
    loading: Boolean,
    photoUrl: String?,
    busy: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onUpload: (Uri, String) -> Unit,
    onOpenAttachment: (Attachment) -> Unit,
    onDeleteAttachment: (Attachment) -> Unit,
    onAddRepair: (RepairInput) -> Unit,
    onAddReminder: (ReminderInput) -> Unit,
    onCompleteReminder: (Reminder) -> Unit,
    onDeleteReminder: (Reminder) -> Unit,
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var showRepairDialog by rememberSaveable { mutableStateOf(false) }
    var showReminderDialog by rememberSaveable { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri -> if (uri != null) onUpload(uri, "photo") }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) onUpload(uri, "receipt") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = detail?.asset?.name ?: "Asset",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (detail != null) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit asset")
                        }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete asset")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        when {
            detail == null && loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            detail == null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                    EmptyState(
                        title = "Asset unavailable",
                        message = "It may have been deleted on another device.",
                    )
                }
            }
            else -> {
                val asset = detail.asset
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item {
                        PhotoCard(
                            photoUrl = photoUrl,
                            busy = busy,
                            onAddPhoto = { photoPicker.launch("image/*") },
                        )
                    }

                    item {
                        SectionCard(title = "Overview") {
                            if (!asset.warrantyExpiresOn.isNullOrBlank()) {
                                WarrantyBadge(asset.warrantyExpiresOn)
                            }
                            InfoRow("Category", asset.category.takeIf { it.isNotBlank() })
                            InfoRow("Brand", asset.brand)
                            InfoRow("Model", asset.model)
                            InfoRow("Serial number", asset.serialNumber)
                            InfoRow("Location", asset.location)
                            InfoRow("Purchased", asset.purchaseDate?.let { Dates.pretty(it) })
                            InfoRow("Price", formatMoney(asset.purchasePrice, asset.currency))
                            InfoRow("Retailer", asset.retailer)
                            InfoRow("Warranty ends", asset.warrantyExpiresOn?.let { Dates.pretty(it) })
                            if (!asset.notes.isNullOrBlank()) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "Notes",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(asset.notes, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }

                    item {
                        SectionCard(
                            title = "Receipts & documents",
                            action = {
                                TextButton(onClick = { filePicker.launch(arrayOf("*/*")) }) {
                                    Text("Add")
                                }
                            },
                        ) {
                            if (detail.attachments.isEmpty()) {
                                Text(
                                    text = "Attach receipts, warranty PDFs or manuals here.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            detail.attachments.forEach { attachment ->
                                AttachmentRow(
                                    attachment = attachment,
                                    onOpen = { onOpenAttachment(attachment) },
                                    onDelete = { onDeleteAttachment(attachment) },
                                )
                            }
                        }
                    }

                    item {
                        SectionCard(
                            title = "Repair history",
                            action = {
                                TextButton(onClick = { showRepairDialog = true }) { Text("Add") }
                            },
                        ) {
                            if (detail.repairs.isEmpty()) {
                                Text(
                                    text = "No repairs or services logged yet.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            detail.repairs.forEach { repair ->
                                RepairRow(repair = repair)
                            }
                        }
                    }

                    item {
                        SectionCard(
                            title = "Maintenance reminders",
                            action = {
                                TextButton(onClick = { showReminderDialog = true }) { Text("Add") }
                            },
                        ) {
                            if (detail.reminders.isEmpty()) {
                                Text(
                                    text = "Add a reminder for filter changes, servicing or warranty checks.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            detail.reminders.forEach { reminder ->
                                ReminderRow(
                                    reminder = reminder,
                                    onComplete = { onCompleteReminder(reminder) },
                                    onDelete = { onDeleteReminder(reminder) },
                                )
                            }
                        }
                    }

                    item {
                        OutlinedButton(
                            onClick = { confirmDelete = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                        ) {
                            Text("Delete asset")
                        }
                    }

                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete this asset?",
            message = "The asset, its files, repair history and reminders will be removed. This cannot be undone.",
            confirmLabel = "Delete",
            onConfirm = {
                confirmDelete = false
                onDelete()
            },
            onDismiss = { confirmDelete = false },
        )
    }

    if (showRepairDialog && detail != null) {
        AddRepairDialog(
            assetId = detail.asset.id,
            onDismiss = { showRepairDialog = false },
            onSave = {
                showRepairDialog = false
                onAddRepair(it)
            },
        )
    }

    if (showReminderDialog && detail != null) {
        AddReminderDialog(
            assetId = detail.asset.id,
            onDismiss = { showReminderDialog = false },
            onSave = {
                showReminderDialog = false
                onAddReminder(it)
            },
        )
    }
}

@Composable
private fun PhotoCard(photoUrl: String?, busy: Boolean, onAddPhoto: () -> Unit) {
    Card(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column {
            if (photoUrl != null) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = "Photo of the asset",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No photo yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(onClick = onAddPhoto, enabled = !busy) {
                    Text(if (photoUrl == null) "Add photo" else "Replace photo")
                }
                if (busy) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }
        }
    }
}

@Composable
private fun AttachmentRow(
    attachment: Attachment,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = attachment.fileName ?: "Document",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = attachment.kind.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onOpen) { Text("Open") }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Delete file",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RepairRow(repair: Repair) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(repair.description, style = MaterialTheme.typography.bodyMedium)
        val meta = listOfNotNull(
            repair.performedOn?.let { Dates.pretty(it) },
            repair.vendor,
            formatMoney(repair.cost),
        ).joinToString(" · ")
        if (meta.isNotBlank()) {
            Text(
                text = meta,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!repair.notes.isNullOrBlank()) {
            Text(
                text = repair.notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReminderRow(
    reminder: Reminder,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
) {
    val completed = reminder.completedOn != null
    val days = Dates.daysUntil(reminder.dueOn)
    val dueColor = when {
        completed -> MaterialTheme.colorScheme.onSurfaceVariant
        days != null && days < 0 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = completed,
            onCheckedChange = { checked -> if (checked) onComplete() },
            enabled = !completed,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = reminder.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (completed) FontWeight.Normal else FontWeight.Medium,
            )
            Text(
                text = when {
                    completed -> "Done on ${Dates.pretty(reminder.completedOn)}"
                    days != null && days < 0 -> "Overdue since ${Dates.pretty(reminder.dueOn)}"
                    days == 0L -> "Due today"
                    days == 1L -> "Due tomorrow"
                    else -> "Due ${Dates.pretty(reminder.dueOn)}"
                },
                style = MaterialTheme.typography.labelSmall,
                color = dueColor,
            )
            if (!reminder.notes.isNullOrBlank()) {
                Text(
                    text = reminder.notes,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Delete reminder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AddRepairDialog(
    assetId: String,
    onDismiss: () -> Unit,
    onSave: (RepairInput) -> Unit,
) {
    var description by rememberSaveable { mutableStateOf("") }
    var vendor by rememberSaveable { mutableStateOf("") }
    var cost by rememberSaveable { mutableStateOf("") }
    var performedOn by rememberSaveable { mutableStateOf(Dates.today()) }
    var notes by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log a repair") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("What was done") },
                    modifier = Modifier.fillMaxWidth(),
                )
                DateField(
                    label = "Date",
                    value = performedOn,
                    onChange = { performedOn = it ?: Dates.today() },
                )
                OutlinedTextField(
                    value = vendor,
                    onValueChange = { vendor = it },
                    label = { Text("Repair shop / vendor") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = cost,
                    onValueChange = { cost = it },
                    label = { Text("Cost") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (description.isBlank()) {
                        error = "Describe the repair."
                        return@TextButton
                    }
                    val parsedCost = cost.trim()
                        .replace(',', '.')
                        .takeIf { it.isNotEmpty() }
                        ?.toDoubleOrNull()
                    if (cost.isNotBlank() && parsedCost == null) {
                        error = "Cost should be a number."
                        return@TextButton
                    }
                    onSave(
                        RepairInput(
                            assetId = assetId,
                            description = description.trim(),
                            vendor = vendor.trim().takeIf { it.isNotEmpty() },
                            cost = parsedCost,
                            performedOn = performedOn,
                            notes = notes.trim().takeIf { it.isNotEmpty() },
                        ),
                    )
                },
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun AddReminderDialog(
    assetId: String,
    onDismiss: () -> Unit,
    onSave: (ReminderInput) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var dueOn by rememberSaveable { mutableStateOf(LocalDate.now().plusWeeks(1).toString()) }
    var repeatMonths by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a reminder") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Reminder") },
                    placeholder = { Text("e.g. Replace water filter") },
                    modifier = Modifier.fillMaxWidth(),
                )
                DateField(
                    label = "Due date",
                    value = dueOn,
                    onChange = { dueOn = it ?: dueOn },
                )
                OutlinedTextField(
                    value = repeatMonths,
                    onValueChange = { repeatMonths = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Repeat every N months (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isBlank()) {
                        error = "Give the reminder a title."
                        return@TextButton
                    }
                    onSave(
                        ReminderInput(
                            assetId = assetId,
                            title = title.trim(),
                            dueOn = dueOn,
                            repeatMonths = repeatMonths.toIntOrNull()?.takeIf { it in 1..120 },
                            notes = notes.trim().takeIf { it.isNotEmpty() },
                        ),
                    )
                },
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
