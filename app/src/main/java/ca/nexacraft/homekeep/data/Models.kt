package ca.nexacraft.homekeep.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthUser(
    val id: String = "",
    val email: String? = null,
)

@Serializable
data class AuthSession(
    @SerialName("access_token") val accessToken: String = "",
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_at") val expiresAt: Long = 0,
    @SerialName("expires_in") val expiresIn: Long? = null,
    val user: AuthUser? = null,
)

@Serializable
data class Asset(
    val id: String = "",
    @SerialName("user_id") val userId: String? = null,
    val name: String = "",
    val category: String = "Other",
    val brand: String? = null,
    val model: String? = null,
    @SerialName("serial_number") val serialNumber: String? = null,
    val location: String? = null,
    @SerialName("purchase_date") val purchaseDate: String? = null,
    @SerialName("purchase_price") val purchasePrice: Double? = null,
    val currency: String? = null,
    val retailer: String? = null,
    @SerialName("warranty_expires_on") val warrantyExpiresOn: String? = null,
    val notes: String? = null,
    @SerialName("photo_path") val photoPath: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class AssetInput(
    val name: String,
    val category: String,
    val brand: String?,
    val model: String?,
    @SerialName("serial_number") val serialNumber: String?,
    val location: String?,
    @SerialName("purchase_date") val purchaseDate: String?,
    @SerialName("purchase_price") val purchasePrice: Double?,
    val retailer: String?,
    @SerialName("warranty_expires_on") val warrantyExpiresOn: String?,
    val notes: String?,
)

@Serializable
data class Attachment(
    val id: String = "",
    @SerialName("asset_id") val assetId: String = "",
    val kind: String = "receipt",
    @SerialName("file_path") val filePath: String = "",
    @SerialName("file_name") val fileName: String? = null,
    @SerialName("mime_type") val mimeType: String? = null,
    @SerialName("byte_size") val byteSize: Long? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class AttachmentInput(
    @SerialName("asset_id") val assetId: String,
    val kind: String,
    @SerialName("file_path") val filePath: String,
    @SerialName("file_name") val fileName: String?,
    @SerialName("mime_type") val mimeType: String?,
    @SerialName("byte_size") val byteSize: Long?,
)

@Serializable
data class Repair(
    val id: String = "",
    @SerialName("asset_id") val assetId: String = "",
    val description: String = "",
    val vendor: String? = null,
    val cost: Double? = null,
    @SerialName("performed_on") val performedOn: String? = null,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class RepairInput(
    @SerialName("asset_id") val assetId: String,
    val description: String,
    val vendor: String?,
    val cost: Double?,
    @SerialName("performed_on") val performedOn: String,
    val notes: String?,
)

@Serializable
data class Reminder(
    val id: String = "",
    @SerialName("asset_id") val assetId: String = "",
    val title: String = "",
    @SerialName("due_on") val dueOn: String = "",
    @SerialName("repeat_months") val repeatMonths: Int? = null,
    val notes: String? = null,
    @SerialName("completed_on") val completedOn: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class ReminderInput(
    @SerialName("asset_id") val assetId: String,
    val title: String,
    @SerialName("due_on") val dueOn: String,
    @SerialName("repeat_months") val repeatMonths: Int?,
    val notes: String?,
)

data class AssetDetail(
    val asset: Asset,
    val attachments: List<Attachment> = emptyList(),
    val repairs: List<Repair> = emptyList(),
    val reminders: List<Reminder> = emptyList(),
)
