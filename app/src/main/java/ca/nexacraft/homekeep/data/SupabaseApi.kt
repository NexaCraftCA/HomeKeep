package ca.nexacraft.homekeep.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.content.OutgoingContent
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class AuthRequiredException : Exception("Your session expired. Please sign in again.")

class SupabaseException(message: String, val status: Int = 0) : Exception(message)

sealed interface SignUpResult {
    data class SignedIn(val session: AuthSession) : SignUpResult
    data object ConfirmationRequired : SignUpResult
}

class SupabaseApi(
    private val baseUrl: String,
    private val anonKey: String,
    private val sessionStore: SessionStore,
    private val bucket: String = "homekeep",
) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        explicitNulls = true
    }

    private val http = HttpClient(Android) {
        expectSuccess = false
        install(HttpTimeout) {
            requestTimeoutMillis = 45_000
            connectTimeoutMillis = 20_000
            socketTimeoutMillis = 45_000
        }
    }

    private val sessionMutex = Mutex()
    private var cachedSession: AuthSession? = null

    // ---------------------------------------------------------------- session

    suspend fun currentSession(): AuthSession? = sessionMutex.withLock {
        val stored = cachedSession ?: sessionStore.load()
        if (stored == null) return@withLock null
        if (stored.accessToken.isBlank()) {
            clearSession()
            return@withLock null
        }
        if (stored.expiresAt > nowSeconds() + 30) {
            cachedSession = stored
            return@withLock stored
        }
        val refreshToken = stored.refreshToken
        if (refreshToken.isNullOrBlank()) {
            clearSession()
            return@withLock null
        }
        try {
            refreshSession(refreshToken)
        } catch (error: Exception) {
            clearSession()
            null
        }
    }

    fun clearSession() {
        cachedSession = null
        sessionStore.clear()
    }

    private suspend fun sessionToken(): String =
        currentSession()?.accessToken ?: throw AuthRequiredException()

    private suspend fun refreshSession(refreshToken: String): AuthSession {
        val body = buildJsonObject { put("refresh_token", refreshToken) }
        val response = execute(
            method = HttpMethod.Post,
            path = "/auth/v1/token",
            query = "grant_type=refresh_token",
            body = body,
            token = anonKey,
        )
        val session = json.decodeFromString(AuthSession.serializer(), response.bodyAsText()).normalized()
        cachedSession = session
        sessionStore.save(session)
        return session
    }

    // ------------------------------------------------------------------ auth

    suspend fun signIn(email: String, password: String): AuthSession {
        val body = buildJsonObject {
            put("email", email)
            put("password", password)
        }
        val response = execute(
            method = HttpMethod.Post,
            path = "/auth/v1/token",
            query = "grant_type=password",
            body = body,
            token = anonKey,
        )
        val session = json.decodeFromString(AuthSession.serializer(), response.bodyAsText()).normalized()
        if (session.accessToken.isBlank()) {
            throw SupabaseException("Sign in failed. Check your email and password.")
        }
        cachedSession = session
        sessionStore.save(session)
        return session
    }

    suspend fun signUp(email: String, password: String): SignUpResult {
        val body = buildJsonObject {
            put("email", email)
            put("password", password)
        }
        val response = execute(
            method = HttpMethod.Post,
            path = "/auth/v1/signup",
            body = body,
            token = anonKey,
        )
        val payload = json.parseToJsonElement(response.bodyAsText()).jsonObject
        if (payload["access_token"] != null) {
            val session = json
                .decodeFromString(AuthSession.serializer(), payload.toString())
                .normalized()
            cachedSession = session
            sessionStore.save(session)
            return SignUpResult.SignedIn(session)
        }
        return SignUpResult.ConfirmationRequired
    }

    suspend fun signOut() {
        val token = cachedSession?.accessToken ?: sessionStore.load()?.accessToken
        if (!token.isNullOrBlank()) {
            runCatching {
                execute(
                    method = HttpMethod.Post,
                    path = "/auth/v1/logout",
                    token = token,
                )
            }
        }
        clearSession()
    }

    suspend fun deleteAccount() {
        execute(
            method = HttpMethod.Post,
            path = "/rest/v1/rpc/hk_delete_account",
            body = buildJsonObject { },
            token = sessionToken(),
        )
        clearSession()
    }

    // ----------------------------------------------------------------- rest

    suspend fun <T> getList(
        table: String,
        query: String,
        deserializer: DeserializationStrategy<List<T>>,
    ): List<T> {
        val response = execute(
            method = HttpMethod.Get,
            path = "/rest/v1/$table",
            query = query,
            token = sessionToken(),
        )
        return json.decodeFromString(deserializer, response.bodyAsText())
    }

    suspend fun <T> insertOne(
        table: String,
        payload: JsonElement,
        deserializer: DeserializationStrategy<List<T>>,
    ): T {
        val response = execute(
            method = HttpMethod.Post,
            path = "/rest/v1/$table",
            body = payload,
            token = sessionToken(),
            extraHeaders = mapOf("Prefer" to "return=representation"),
        )
        val rows = json.decodeFromString(deserializer, response.bodyAsText())
        return rows.firstOrNull() ?: throw SupabaseException("The server did not return the saved record.")
    }

    suspend fun <T> patchOne(
        table: String,
        query: String,
        payload: JsonElement,
        deserializer: DeserializationStrategy<List<T>>,
    ): T {
        val response = execute(
            method = HttpMethod.Patch,
            path = "/rest/v1/$table",
            query = query,
            body = payload,
            token = sessionToken(),
            extraHeaders = mapOf("Prefer" to "return=representation"),
        )
        val rows = json.decodeFromString(deserializer, response.bodyAsText())
        return rows.firstOrNull() ?: throw SupabaseException("The record could not be updated.")
    }

    suspend fun deleteWhere(table: String, query: String) {
        execute(
            method = HttpMethod.Delete,
            path = "/rest/v1/$table",
            query = query,
            token = sessionToken(),
        )
    }

    suspend fun rpc(name: String, payload: JsonElement = buildJsonObject { }) {
        execute(
            method = HttpMethod.Post,
            path = "/rest/v1/rpc/$name",
            body = payload,
            token = sessionToken(),
        )
    }

    // -------------------------------------------------------------- storage

    suspend fun uploadObject(path: String, bytes: ByteArray, mimeType: String) {
        execute(
            method = HttpMethod.Post,
            path = "/storage/v1/object/$bucket/$path",
            token = sessionToken(),
            extraHeaders = mapOf("x-upsert" to "true"),
            rawBody = ByteArrayContent(bytes, ContentType.parse(mimeType)),
        )
    }

    suspend fun deleteObject(path: String) {
        runCatching {
            execute(
                method = HttpMethod.Delete,
                path = "/storage/v1/object/$bucket/$path",
                token = sessionToken(),
            )
        }
    }

    suspend fun signedUrl(path: String, expiresInSeconds: Int = 3600): String {
        val body = buildJsonObject { put("expiresIn", expiresInSeconds) }
        val response = execute(
            method = HttpMethod.Post,
            path = "/storage/v1/object/sign/$bucket/$path",
            body = body,
            token = sessionToken(),
        )
        val payload = json.parseToJsonElement(response.bodyAsText()).jsonObject
        val signed = (payload["signedURL"] ?: payload["signedUrl"])
            ?.jsonPrimitive?.contentOrNull
            ?: throw SupabaseException("Could not create a link for this file.")
        return "$baseUrl/storage/v1$signed"
    }

    // -------------------------------------------------------------- plumbing

    private suspend fun execute(
        method: HttpMethod,
        path: String,
        query: String? = null,
        body: JsonElement? = null,
        token: String,
        extraHeaders: Map<String, String> = emptyMap(),
        rawBody: OutgoingContent? = null,
    ): HttpResponse {
        val url = if (query.isNullOrBlank()) "$baseUrl$path" else "$baseUrl$path?$query"

        val response = http.request(url) {
            this.method = method
            header("apikey", anonKey)
            header("Authorization", "Bearer $token")
            header("Accept", "application/json")
            extraHeaders.forEach { (name, value) -> header(name, value) }
            when {
                rawBody != null -> setBody(rawBody)
                body != null -> {
                    header("Content-Type", "application/json")
                    setBody(body.toString())
                }
            }
        }

        if (response.status.value >= 400) {
            val raw = runCatching { response.bodyAsText() }.getOrDefault("")
            throw SupabaseException(friendlyError(raw, response.status.value), response.status.value)
        }
        return response
    }

    private fun friendlyError(raw: String, status: Int): String {
        val payload = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull()
        val candidates = listOf("message", "error_description", "msg", "error", "hint")
        for (key in candidates) {
            val element = payload?.get(key) ?: continue
            val text = (element as? JsonPrimitive)?.contentOrNull
            if (!text.isNullOrBlank() && text != "null") return text
        }
        return when (status) {
            401 -> "Not authorised. Please sign in again."
            403 -> "You do not have access to that record."
            404 -> "Not found."
            else -> "Request failed. Please try again."
        }
    }

    private fun nowSeconds(): Long = System.currentTimeMillis() / 1000

    private fun AuthSession.normalized(): AuthSession {
        val expiry = if (expiresAt > 0) expiresAt else nowSeconds() + (expiresIn ?: 3600)
        return copy(expiresAt = expiry)
    }
}
