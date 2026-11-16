package ca.nexacraft.homekeep.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ca.nexacraft.homekeep.HomeKeepApp
import ca.nexacraft.homekeep.util.Dates

class ReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? HomeKeepApp ?: return Result.success()
        val container = app.container
        if (!container.isConfigured) return Result.success()
        if (container.api.currentSession() == null) return Result.success()

        val reminders = runCatching {
            container.repository.dueReminders(DAYS_AHEAD)
        }.getOrElse { return Result.retry() }

        if (reminders.isEmpty()) return Result.success()

        val assetNames = runCatching {
            container.repository.assets().associate { it.id to it.name }
        }.getOrDefault(emptyMap())

        container.notifier.ensureChannel()

        reminders.forEach { reminder ->
            val days = Dates.daysUntil(reminder.dueOn)
            val timing = when {
                days == null -> Dates.pretty(reminder.dueOn)
                days < 0 -> "Overdue since ${Dates.pretty(reminder.dueOn)}"
                days == 0L -> "Due today"
                days == 1L -> "Due tomorrow"
                else -> "Due in $days days (${Dates.pretty(reminder.dueOn)})"
            }
            val assetName = assetNames[reminder.assetId] ?: "Home asset"
            container.notifier.notify(
                id = reminder.id.hashCode(),
                title = reminder.title,
                text = "$assetName - $timing",
            )
        }

        return Result.success()
    }

    private companion object {
        const val DAYS_AHEAD = 7L
    }
}
