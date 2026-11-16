package ca.nexacraft.homekeep.ui

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.core.net.toUri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ca.nexacraft.homekeep.ui.screens.AssetDetailScreen
import ca.nexacraft.homekeep.ui.screens.AssetEditScreen
import ca.nexacraft.homekeep.ui.screens.AssetListScreen
import ca.nexacraft.homekeep.ui.screens.AuthScreen
import ca.nexacraft.homekeep.ui.screens.SettingsScreen

private const val ROUTE_LIST = "list"
private const val ROUTE_DETAIL = "detail"
private const val ROUTE_EDIT = "edit"
private const val ROUTE_SETTINGS = "settings"

@Composable
fun AppRoot(viewModel: HomeKeepViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var route by rememberSaveable { mutableStateOf(ROUTE_LIST) }
    var activeId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(state.message, state.error) {
        val text = state.error ?: state.message ?: return@LaunchedEffect
        viewModel.consumeMessage()
        snackbarHostState.showSnackbar(text)
    }

    LaunchedEffect(state.signedIn) {
        if (!state.signedIn) {
            route = ROUTE_LIST
            activeId = null
        }
    }

    LaunchedEffect(route, activeId) {
        val id = activeId
        if (route == ROUTE_DETAIL && id != null && state.detail?.asset?.id != id) {
            viewModel.openAsset(id)
        }
    }

    BackHandler(enabled = route != ROUTE_LIST) {
        route = when (route) {
            ROUTE_EDIT -> if (activeId == null) ROUTE_LIST else ROUTE_DETAIL
            else -> ROUTE_LIST
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (state.booting) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (!state.signedIn) {
            AuthScreen(
                configured = state.configured,
                busy = state.authBusy,
                onSignIn = viewModel::signIn,
                onCreateAccount = viewModel::signUp,
            )
        } else {
            when (route) {
                ROUTE_DETAIL -> {
                    AssetDetailScreen(
                        detail = state.detail,
                        loading = state.detailLoading,
                        photoUrl = state.photoUrl,
                        busy = state.busy,
                        onBack = { route = ROUTE_LIST },
                        onEdit = { route = ROUTE_EDIT },
                        onDelete = {
                            val id = activeId
                            if (id != null) {
                                viewModel.deleteAsset(id) {
                                    activeId = null
                                    route = ROUTE_LIST
                                }
                            }
                        },
                        onUpload = { uri, kind ->
                            val id = activeId
                            if (id != null) viewModel.uploadAttachment(id, uri, kind)
                        },
                        onOpenAttachment = { attachment ->
                            viewModel.openAttachment(attachment) { url -> openUrl(context, url) }
                        },
                        onDeleteAttachment = viewModel::deleteAttachment,
                        onAddRepair = viewModel::addRepair,
                        onAddReminder = viewModel::addReminder,
                        onCompleteReminder = viewModel::completeReminder,
                        onDeleteReminder = { reminder ->
                            viewModel.deleteReminder(reminder.assetId, reminder.id)
                        },
                    )
                }

                ROUTE_EDIT -> {
                    val existing = state.assets.firstOrNull { it.id == activeId }
                        ?: state.detail?.asset
                    AssetEditScreen(
                        existing = if (activeId == null) null else existing,
                        busy = state.busy,
                        onSave = { input ->
                            viewModel.saveAsset(activeId, input) { savedId ->
                                activeId = savedId
                                route = ROUTE_DETAIL
                            }
                        },
                        onBack = {
                            route = if (activeId == null) ROUTE_LIST else ROUTE_DETAIL
                        },
                    )
                }

                ROUTE_SETTINGS -> {
                    SettingsScreen(
                        email = state.email,
                        busy = state.busy,
                        onBack = { route = ROUTE_LIST },
                        onSignOut = {
                            route = ROUTE_LIST
                            activeId = null
                            viewModel.signOut()
                        },
                        onDeleteAccount = {
                            viewModel.deleteAccount {
                                activeId = null
                                route = ROUTE_LIST
                            }
                        },
                    )
                }

                else -> {
                    AssetListScreen(
                        assets = state.assets,
                        loading = state.assetsLoading,
                        onAdd = {
                            activeId = null
                            route = ROUTE_EDIT
                        },
                        onOpen = { asset ->
                            activeId = asset.id
                            route = ROUTE_DETAIL
                        },
                        onSettings = { route = ROUTE_SETTINGS },
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp),
        )
    }
}

private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }
}
