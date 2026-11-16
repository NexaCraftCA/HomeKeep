package ca.nexacraft.homekeep

import android.app.Application
import ca.nexacraft.homekeep.data.AppContainer
import ca.nexacraft.homekeep.notify.ReminderScheduler

class HomeKeepApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notifier.ensureChannel()
        ReminderScheduler.schedule(this)
    }
}
