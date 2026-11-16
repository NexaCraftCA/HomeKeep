package ca.nexacraft.homekeep.data

import ca.nexacraft.homekeep.util.Dates
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import java.time.LocalDate

class HomeRepository(private val api: SupabaseApi) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        explicitNulls = true
    }

    private val assetsSerializer = ListSerializer(Asset.serializer())
    private val attachmentsSerializer = ListSerializer(Attachment.serializer())
    private val repairsSerializer = ListSerializer(Repair.serializer())
    private val remindersSerializer = ListSerializer(Reminder.serializer())

    suspend fun assets(): List<Asset> =
        api.getList("hk_assets", "select=*&order=created_at.desc", assetsSerializer)

    suspend fun asset(id: String): Asset? =
        api.getList("hk_assets", "select=*&id=eq.$id&limit=1", assetsSerializer).firstOrNull()

    suspend fun createAsset(input: AssetInput): Asset =
        api.insertOne(
            table = "hk_assets",
            payload = json.encodeToJsonElement(AssetInput.serializer(), input),
            deserializer = assetsSerializer,
        )

    suspend fun updateAsset(id: String, input: AssetInput): Asset =
        api.patchOne(
            table = "hk_assets",
            query = "id=eq.$id",
            payload = json.encodeToJsonElement(AssetInput.serializer(), input),
            deserializer = assetsSerializer,
        )

    suspend fun updateAssetPhoto(id: String, path: String) {
        api.patchOne(
            table = "hk_assets",
            query = "id=eq.$id",
            payload = buildJsonObject { put("photo_path", path) },
            deserializer = assetsSerializer,
        )
    }

    suspend fun deleteAsset(id: String) {
        api.deleteWhere("hk_assets", "id=eq.$id")
    }

    suspend fun attachments(assetId: String): List<Attachment> =
        api.getList(
            "hk_attachments",
            "select=*&asset_id=eq.$assetId&order=created_at.desc",
            attachmentsSerializer,
        )

    suspend fun uploadAttachment(
        assetId: String,
        bytes: ByteArray,
        fileName: String,
        mimeType: String,
        kind: String,
    ): Attachment {
        val userId = api.currentSession()?.user?.id ?: throw AuthRequiredException()
        val safeName = sanitizeFileName(fileName)
        val path = "$userId/$assetId/${System.currentTimeMillis()}-$safeName"
        api.uploadObject(path, bytes, mimeType)
        val input = AttachmentInput(
            assetId = assetId,
            kind = kind,
            filePath = path,
            fileName = fileName,
            mimeType = mimeType,
            byteSize = bytes.size.toLong(),
        )
        return api.insertOne(
            table = "hk_attachments",
            payload = json.encodeToJsonElement(AttachmentInput.serializer(), input),
            deserializer = attachmentsSerializer,
        )
    }

    suspend fun deleteAttachment(attachment: Attachment) {
        api.deleteObject(attachment.filePath)
        api.deleteWhere("hk_attachments", "id=eq.${attachment.id}")
    }

    suspend fun repairs(assetId: String): List<Repair> =
        api.getList(
            "hk_repairs",
            "select=*&asset_id=eq.$assetId&order=performed_on.desc",
            repairsSerializer,
        )

    suspend fun addRepair(input: RepairInput): Repair =
        api.insertOne(
            table = "hk_repairs",
            payload = json.encodeToJsonElement(RepairInput.serializer(), input),
            deserializer = repairsSerializer,
        )

    suspend fun deleteRepair(id: String) {
        api.deleteWhere("hk_repairs", "id=eq.$id")
    }

    suspend fun reminders(assetId: String): List<Reminder> =
        api.getList(
            "hk_reminders",
            "select=*&asset_id=eq.$assetId&order=due_on.asc",
            remindersSerializer,
        )

    suspend fun dueReminders(daysAhead: Long): List<Reminder> {
        val threshold = LocalDate.now().plusDays(daysAhead).toString()
        return api.getList(
            "hk_reminders",
            "select=*&completed_on=is.null&due_on=lte.$threshold&order=due_on.asc",
            remindersSerializer,
        )
    }

    suspend fun addReminder(input: ReminderInput): Reminder =
        api.insertOne(
            table = "hk_reminders",
            payload = json.encodeToJsonElement(ReminderInput.serializer(), input),
            deserializer = remindersSerializer,
        )

    suspend fun completeReminder(reminder: Reminder) {
        api.patchOne(
            table = "hk_reminders",
            query = "id=eq.${reminder.id}",
            payload = buildJsonObject { put("completed_on", Dates.today()) },
            deserializer = remindersSerializer,
        )
        val months = reminder.repeatMonths
        if (months != null && months > 0) {
            val nextDue = Dates.addMonths(reminder.dueOn, months) ?: return
            addReminder(
                ReminderInput(
                    assetId = reminder.assetId,
                    title = reminder.title,
                    dueOn = nextDue,
                    repeatMonths = months,
                    notes = reminder.notes,
                ),
            )
        }
    }

    suspend fun deleteReminder(id: String) {
        api.deleteWhere("hk_reminders", "id=eq.$id")
    }

    suspend fun deleteAccount() {
        api.deleteAccount()
    }

    private fun sanitizeFileName(name: String): String {
        val cleaned = name
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .trim('_', '.')
            .takeLast(80)
        return cleaned.ifBlank { "file" }
    }
}
