package ca.nexacraft.homekeep.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.serialization.json.Json

class SessionStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true }

    fun load(): AuthSession? {
        val raw = prefs.getString(KEY_SESSION, null) ?: return null
        return runCatching { json.decodeFromString(AuthSession.serializer(), raw) }.getOrNull()
    }

    fun save(session: AuthSession) {
        prefs.edit {
            putString(KEY_SESSION, json.encodeToString(AuthSession.serializer(), session))
        }
    }

    fun clear() {
        prefs.edit { remove(KEY_SESSION) }
    }

    private companion object {
        const val PREF_NAME = "homekeep_session"
        const val KEY_SESSION = "session"
    }
}
