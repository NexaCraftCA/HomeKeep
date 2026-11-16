package ca.nexacraft.homekeep.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ca.nexacraft.homekeep.data.Asset
import ca.nexacraft.homekeep.ui.components.ChipRow
import ca.nexacraft.homekeep.ui.components.EmptyState
import ca.nexacraft.homekeep.ui.components.WarrantyBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetListScreen(
    assets: List<Asset>,
    loading: Boolean,
    onAdd: () -> Unit,
    onOpen: (Asset) -> Unit,
    onSettings: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("All") }

    val categories = remember(assets) {
        listOf("All") + assets.map { it.category.ifBlank { "Other" } }.distinct().sorted()
    }

    val filtered = remember(assets, query, category) {
        val needle = query.trim().lowercase()
        assets.filter { asset ->
            val matchesCategory = category == "All" || asset.category == category
            val haystack = listOfNotNull(
                asset.name,
                asset.brand,
                asset.model,
                asset.serialNumber,
                asset.location,
                asset.retailer,
            ).joinToString(" ").lowercase()
            matchesCategory && (needle.isEmpty() || haystack.contains(needle))
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("HomeKeep", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = if (assets.isEmpty()) {
                                "Your home, catalogued"
                            } else {
                                "${assets.size} asset${if (assets.size == 1) "" else "s"}"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = "Add asset")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search")
                        }
                    }
                },
                placeholder = { Text("Search name, brand, serial...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )

            if (categories.size > 1) {
                Spacer(Modifier.height(8.dp))
                ChipRow(
                    values = categories,
                    selected = category,
                    onSelect = { category = it },
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    loading && assets.isEmpty() -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    filtered.isEmpty() -> {
                        EmptyState(
                            title = if (assets.isEmpty()) "No assets yet" else "Nothing matches",
                            message = if (assets.isEmpty()) {
                                "Tap + to add your first appliance, gadget or piece of furniture."
                            } else {
                                "Try a different search or category."
                            },
                            modifier = Modifier.align(Alignment.TopCenter),
                        )
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = 96.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(filtered, key = { it.id }) { asset ->
                                AssetCard(asset = asset, onClick = { onOpen(asset) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AssetCard(asset: Asset, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(46.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = asset.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = asset.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                val subtitle = listOfNotNull(
                    asset.category.takeIf { it.isNotBlank() },
                    asset.brand,
                    asset.model,
                ).joinToString(" · ")
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val serial = asset.serialNumber
                if (!serial.isNullOrBlank()) {
                    Text(
                        text = "S/N $serial",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!asset.warrantyExpiresOn.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    WarrantyBadge(asset.warrantyExpiresOn)
                }
            }
        }
    }
}
