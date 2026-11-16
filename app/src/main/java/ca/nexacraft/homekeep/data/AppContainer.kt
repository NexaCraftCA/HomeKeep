package ca.nexacraft.homekeep.data

import android.content.Context
import ca.nexacraft.homekeep.BuildConfig
import ca.nexacraft.homekeep.notify.Notifier

class AppContainer(context: Context) {

    private val baseUrl = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
    private val anonKey = BuildConfig.SUPABASE_ANON_KEY.trim()

    val isConfigured: Boolean =
        baseUrl.startsWith("https://") && anonKey.isNotBlank()

    val sessionStore = SessionStore(context)
    val api = SupabaseApi(
        baseUrl = baseUrl,
        anonKey = anonKey,
        sessionStore = sessionStore,
        bucket = BuildConfig.STORAGE_BUCKET,
    )
    val repository = HomeRepository(api)
    val notifier = Notifier(context)
}
