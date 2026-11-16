package ca.nexacraft.homekeep.ui

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ca.nexacraft.homekeep.HomeKeepApp
import ca.nexacraft.homekeep.data.Asset
import ca.nexacraft.homekeep.data.AssetDetail
import ca.nexacraft.homekeep.data.AssetInput
import ca.nexacraft.homekeep.data.Attachment
import ca.nexacraft.homekeep.data.AuthRequiredException
import ca.nexacraft.homekeep.data.Reminder
import ca.nexacraft.homekeep.data.ReminderInput
import ca.nexacraft.homekeep.data.RepairInput
import ca.nexacraft.homekeep.data.SignUpResult
import ca.nexacraft.homekeep.data.SupabaseException
import ca.nexacraft.homekeep.notify.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val booting: Boolean = true,
    val configured: Boolean = true,
    val signedIn: Boolean = false,
    val email: String? = null,
    val assets: List<Asset> = emptyList(),
    val assetsLoading: Boolean = false,
    val detail: AssetDetail? = null,
    val detailLoading: Boolean = false,
    val photoUrl: String? = null,
    val authBusy: Boolean = false,
    val busy: Boolean = false,
    val error: String? = null,
    val message: String? = null,
)

class HomeKeepViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as HomeKeepApp).container
    private val api = container.api
    private val repository = container.repository

    private val _state = MutableStateFlow(UiState(configured = container.isConfigured))
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        bootstrap()
    }

    private fun bootstrap() {
        val current = _state.value
        if (!current.configured) {
            _state.update { it.copy(booting = false) }
            return
        }
        viewModelScope.launch {
            val session = api.currentSession()
            if (session == null) {
                _state.update { it.copy(booting = false, signedIn = false) }
                return@launch
            }
            _state.update {
                it.copy(booting = false, signedIn = true, email = session.user?.email)
            }
            loadAssets()
        }
    }

    fun refresh() {
        loadAssets()
    }

    private fun loadAssets() {
        if (!_state.value.configured) return
        viewModelScope.launch {
            _state.update { it.copy(assetsLoading = true) }
            try {
                val assets = repository.assets()
                _state.update { it.copy(assets = assets, assetsLoading = false) }
            } catch (error: Exception) {
                handle(error)
                _state.update { it.copy(assetsLoading = false) }
            }
        }
    }

    // ------------------------------------------------------------------ auth

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            showError("Enter your email and password.")
            return
        }
        authAction {
            val session = api.signIn(email.trim(), password)
            _state.update {
                it.copy(
                    signedIn = true,
                    email = session.user?.email ?: email.trim(),
                    message = "Welcome back.",
                )
            }
            loadAssets()
            ReminderScheduler.runNow(getApplication())
        }
    }

    fun signUp(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            showError("Enter your email and password.")
            return
        }
        if (password.length < 6) {
            showError("Use a password with at least 6 characters.")
            return
        }
        authAction {
            when (val result = api.signUp(email.trim(), password)) {
                is SignUpResult.SignedIn -> {
                    _state.update {
                        it.copy(
                            signedIn = true,
                            email = result.session.user?.email ?: email.trim(),
                            message = "Your account is ready.",
                        )
                    }
                    loadAssets()
                    ReminderScheduler.runNow(getApplication())
                }
                SignUpResult.ConfirmationRequired -> _state.update {
                    it.copy(message = "Check your email to confirm the account, then sign in.")
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            api.signOut()
            _state.value = UiState(
                booting = false,
                configured = container.isConfigured,
                signedIn = false,
            )
        }
    }

    private fun authAction(block: suspend () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(authBusy = true, error = null) }
            try {
                block()
            } catch (error: Exception) {
                handle(error)
            } finally {
                _state.update { it.copy(authBusy = false) }
            }
        }
    }

    // ---------------------------------------------------------------- assets

    fun openAsset(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(detailLoading = true, detail = null, photoUrl = null) }
            try {
                val asset = repository.asset(id)
                    ?: throw SupabaseException("That asset no longer exists.")
                val detail = AssetDetail(
                    asset = asset,
                    attachments = repository.attachments(id),
                    repairs = repository.repairs(id),
                    reminders = repository.reminders(id),
                )
                val photoUrl = asset.photoPath?.let { path ->
                    runCatching { api.signedUrl(path) }.getOrNull()
                }
                _state.update {
                    it.copy(detail = detail, detailLoading = false, photoUrl = photoUrl)
                }
            } catch (error: Exception) {
                handle(error)
                _state.update { it.copy(detailLoading = false) }
            }
        }
    }

    fun refreshDetail() {
        _state.value.detail?.asset?.id?.let { openAsset(it) }
    }

    fun saveAsset(existingId: String?, input: AssetInput, onSaved: (String) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            try {
                val saved = if (existingId == null) {
                    repository.createAsset(input)
                } else {
                    repository.updateAsset(existingId, input)
                }
                _state.update { it.copy(message = if (existingId == null) "Asset added." else "Changes saved.") }
                loadAssets()
                onSaved(saved.id)
            } catch (error: Exception) {
                handle(error)
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    fun deleteAsset(id: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            try {
                val attachments = _state.value.detail?.attachments ?: repository.attachments(id)
                attachments.forEach { api.deleteObject(it.filePath) }
                repository.deleteAsset(id)
                _state.update {
                    it.copy(
                        assets = it.assets.filterNot { asset -> asset.id == id },
                        detail = null,
                        message = "Asset deleted.",
                    )
                }
                onDeleted()
            } catch (error: Exception) {
                handle(error)
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    // ----------------------------------------------------------- attachments

    fun uploadAttachment(assetId: String, uri: Uri, kind: String) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            try {
                val resolver = context.contentResolver
                val mime = resolver.getType(uri) ?: "application/octet-stream"
                val name = displayName(uri) ?: "file"
                val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw SupabaseException("That file could not be read.")
                if (bytes.size > MAX_UPLOAD_BYTES) {
                    throw SupabaseException("Files must be 10 MB or smaller.")
                }
                val attachment = repository.uploadAttachment(assetId, bytes, name, mime, kind)
                if (kind == "photo") {
                    repository.updateAssetPhoto(assetId, attachment.filePath)
                }
                _state.update {
                    it.copy(message = if (kind == "photo") "Photo updated." else "File added.")
                }
                openAsset(assetId)
            } catch (error: Exception) {
                handle(error)
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    fun deleteAttachment(attachment: Attachment) {
        val assetId = attachment.assetId
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            try {
                repository.deleteAttachment(attachment)
                _state.update { it.copy(message = "File removed.") }
                openAsset(assetId)
            } catch (error: Exception) {
                handle(error)
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    fun openAttachment(attachment: Attachment, onUrl: (String) -> Unit) {
        viewModelScope.launch {
            try {
                onUrl(api.signedUrl(attachment.filePath))
            } catch (error: Exception) {
                handle(error)
            }
        }
    }

    private fun displayName(uri: Uri): String? = runCatching {
        getApplication<Application>().contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()

    // --------------------------------------------------------------- repairs

    fun addRepair(input: RepairInput) {
        val assetId = input.assetId
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            try {
                repository.addRepair(input)
                _state.update { it.copy(message = "Repair logged.") }
                openAsset(assetId)
            } catch (error: Exception) {
                handle(error)
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    fun deleteRepair(assetId: String, repairId: String) {
        viewModelScope.launch {
            try {
                repository.deleteRepair(repairId)
                openAsset(assetId)
            } catch (error: Exception) {
                handle(error)
            }
        }
    }

    // ------------------------------------------------------------- reminders

    fun addReminder(input: ReminderInput) {
        val assetId = input.assetId
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            try {
                repository.addReminder(input)
                _state.update { it.copy(message = "Reminder added.") }
                openAsset(assetId)
                ReminderScheduler.runNow(getApplication())
            } catch (error: Exception) {
                handle(error)
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    fun completeReminder(reminder: Reminder) {
        viewModelScope.launch {
            try {
                repository.completeReminder(reminder)
                _state.update { it.copy(message = "Reminder completed.") }
                openAsset(reminder.assetId)
            } catch (error: Exception) {
                handle(error)
            }
        }
    }

    fun deleteReminder(assetId: String, reminderId: String) {
        viewModelScope.launch {
            try {
                repository.deleteReminder(reminderId)
                openAsset(assetId)
            } catch (error: Exception) {
                handle(error)
            }
        }
    }

    // --------------------------------------------------------------- account

    fun deleteAccount(onDeleted: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            try {
                repository.deleteAccount()
                _state.value = UiState(
                    booting = false,
                    configured = container.isConfigured,
                    signedIn = false,
                    message = "Your account and data were deleted.",
                )
                onDeleted()
            } catch (error: Exception) {
                handle(error)
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    // ----------------------------------------------------------------- misc

    fun consumeMessage() {
        _state.update { it.copy(error = null, message = null) }
    }

    private fun showError(text: String) {
        _state.update { it.copy(error = text) }
    }

    private fun handle(error: Throwable) {
        if (error is AuthRequiredException) {
            _state.update {
                it.copy(
                    booting = false,
                    signedIn = false,
                    email = null,
                    assets = emptyList(),
                    detail = null,
                    photoUrl = null,
                )
            }
            return
        }
        val text = error.message?.takeIf { it.isNotBlank() } ?: "Something went wrong."
        _state.update { it.copy(error = text) }
    }

    private companion object {
        const val MAX_UPLOAD_BYTES = 10L * 1024L * 1024L
    }
}
