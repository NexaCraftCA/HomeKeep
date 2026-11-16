package ca.nexacraft.homekeep.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ca.nexacraft.homekeep.data.Asset
import ca.nexacraft.homekeep.data.AssetInput
import ca.nexacraft.homekeep.ui.components.AssetCategories
import ca.nexacraft.homekeep.ui.components.CategorySelector
import ca.nexacraft.homekeep.ui.components.DateField
import ca.nexacraft.homekeep.util.asEditable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetEditScreen(
    existing: Asset?,
    busy: Boolean,
    onSave: (AssetInput) -> Unit,
    onBack: () -> Unit,
) {
    val key = existing?.id ?: "new"

    var name by rememberSaveable(key) { mutableStateOf(existing?.name.orEmpty()) }
    var category by rememberSaveable(key) {
        mutableStateOf(existing?.category?.takeIf { it.isNotBlank() } ?: AssetCategories.first())
    }
    var brand by rememberSaveable(key) { mutableStateOf(existing?.brand.orEmpty()) }
    var model by rememberSaveable(key) { mutableStateOf(existing?.model.orEmpty()) }
    var serial by rememberSaveable(key) { mutableStateOf(existing?.serialNumber.orEmpty()) }
    var location by rememberSaveable(key) { mutableStateOf(existing?.location.orEmpty()) }
    var purchaseDate by rememberSaveable(key) { mutableStateOf(existing?.purchaseDate) }
    var price by rememberSaveable(key) {
        mutableStateOf(existing?.purchasePrice.asEditable())
    }
    var retailer by rememberSaveable(key) { mutableStateOf(existing?.retailer.orEmpty()) }
    var warrantyDate by rememberSaveable(key) { mutableStateOf(existing?.warrantyExpiresOn) }
    var notes by rememberSaveable(key) { mutableStateOf(existing?.notes.orEmpty()) }
    var validation by remember { mutableStateOf<String?>(null) }

    fun submit() {
        if (name.isBlank()) {
            validation = "Give the asset a name."
            return
        }
        val parsedPrice = price.trim()
            .replace(',', '.')
            .takeIf { it.isNotEmpty() }
            ?.toDoubleOrNull()
        if (price.isNotBlank() && parsedPrice == null) {
            validation = "Price should be a number, for example 249.99."
            return
        }
        validation = null
        onSave(
            AssetInput(
                name = name.trim(),
                category = category,
                brand = brand.trimOrNull(),
                model = model.trimOrNull(),
                serialNumber = serial.trimOrNull(),
                location = location.trimOrNull(),
                purchaseDate = purchaseDate,
                purchasePrice = parsedPrice,
                retailer = retailer.trimOrNull(),
                warrantyExpiresOn = warrantyDate,
                notes = notes.trimOrNull(),
            ),
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "New asset" else "Edit asset") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 16.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        TextButton(onClick = { submit() }) { Text("Save") }
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
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                placeholder = { Text("e.g. Kitchen fridge") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            CategorySelector(selected = category, onSelect = { category = it })

            OutlinedTextField(
                value = brand,
                onValueChange = { brand = it },
                label = { Text("Brand") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text("Model") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = serial,
                onValueChange = { serial = it },
                label = { Text("Serial number") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location in the home") },
                placeholder = { Text("e.g. Basement") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            DateField(
                label = "Purchase date",
                value = purchaseDate,
                onChange = { purchaseDate = it },
            )

            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text("Purchase price") },
                placeholder = { Text("249.99") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = retailer,
                onValueChange = { retailer = it },
                label = { Text("Retailer") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            DateField(
                label = "Warranty ends",
                value = warrantyDate,
                onChange = { warrantyDate = it },
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            validation?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Text(
                text = "Tip: after saving, attach the receipt and photos from the asset page.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp),
            )
        }
    }
}

private fun String.trimOrNull(): String? = trim().takeIf { it.isNotEmpty() }
