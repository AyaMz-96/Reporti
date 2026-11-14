package com.example.reporti.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class AuthUser(
    val id: String,
    val fullName: String,
    val email: String,
    val phone: String?,
    val role: String = "citizen",
    val categoryIds: List<Long> = emptyList(),
    val sectorId: Long? = null,
)

class AuthException(message: String) : Exception(message)

private fun JSONObject.str(key: String): String? =
    if (isNull(key)) null else optString(key).ifBlank { null }

private fun extractCategoryIds(meta: JSONObject?): List<Long> {
    if (meta == null) return emptyList()
    val arr = meta.optJSONArray("categoryIds")
    if (arr != null) {
        val out = mutableListOf<Long>()
        for (i in 0 until arr.length()) {
            if (!arr.isNull(i)) out.add(arr.optLong(i))
        }
        return out.filter { it > 0 }
    }
    // Legacy single-field fallback (pre multi-category agents)
    val single = meta.optLong("categoryId", 0L)
    return if (single > 0) listOf(single) else emptyList()
}

private fun serializeCategoryIds(ids: List<Long>): String =
    ids.joinToString(",")

private fun parseCategoryIdsCsv(stored: String?): List<Long> =
    stored?.split(",")?.mapNotNull { it.trim().toLongOrNull() }?.filter { it > 0 } ?: emptyList()

class AuthRepository(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("reporti_auth", Context.MODE_PRIVATE)

    val accessToken: String? get() = prefs.getString("access_token", null)

    fun currentUser(): AuthUser? {
        if (accessToken == null) return null
        return AuthUser(
            id = prefs.getString("user_id", null) ?: return null,
            fullName = prefs.getString("full_name", "") ?: "",
            email = prefs.getString("email", "") ?: "",
            phone = prefs.getString("phone", null),
            role = prefs.getString("role", "citizen") ?: "citizen",
            categoryIds = parseCategoryIdsCsv(prefs.getString("category_ids", null)),
            sectorId = prefs.getLong("sector_id", 0L).takeIf { it > 0 },
        )
    }

    /** At app start: keep the session, refresh it if it has expired. */
    suspend fun restore(): AuthUser? {
        if (accessToken == null) return null
        val expiresAt = prefs.getLong("expires_at", 0L)
        if (expiresAt > System.currentTimeMillis() / 1000 + 30) return currentUser()
        val refresh = prefs.getString("refresh_token", null) ?: run { clear(); return null }
        return try {
            saveSession(request("POST", "/auth/v1/token?grant_type=refresh_token",
                JSONObject().put("refresh_token", refresh)))
        } catch (e: AuthException) {
            clear(); null
        } catch (e: IOException) {
            currentUser()
        }
    }

    suspend fun login(email: String, password: String): AuthUser =
        saveSession(request("POST", "/auth/v1/token?grant_type=password",
            JSONObject().put("email", email.trim()).put("password", password)))

    suspend fun register(fullName: String, email: String, phone: String?, password: String): AuthUser? {
        val meta = JSONObject().put("full_name", fullName.trim())
        if (!phone.isNullOrBlank()) meta.put("phone", phone.trim())
        val json = request("POST", "/auth/v1/signup",
            JSONObject().put("email", email.trim()).put("password", password).put("data", meta))
        return if (json.has("access_token")) saveSession(json) else null
    }

    suspend fun forgotPassword(email: String) {
        val redirect = URLEncoder.encode(AuthConfig.RESET_REDIRECT_URL, "UTF-8")
        request("POST", "/auth/v1/recover?redirect_to=$redirect", JSONObject().put("email", email.trim()))
    }

    /**
     * Update the caller's own user_metadata (categories, sector, etc.).
     * Supabase returns the updated User object directly (no session wrapper),
     * so we parse it and refresh the cached prefs + AuthTokenHolder.
     */
    suspend fun updateMetadata(updates: Map<String, Any?>): AuthUser {
        val token = accessToken ?: throw AuthException("Not logged in")
        val data = JSONObject()
        updates.forEach { (k, v) ->
            when (v) {
                null -> data.put(k, JSONObject.NULL)
                is List<*> -> data.put(k, JSONArray(v))
                else -> data.put(k, v)
            }
        }
        val response = request("PUT", "/auth/v1/user",
            JSONObject().put("data", data), bearer = token)
        return persistUser(response)
    }

    suspend fun logout() {
        val token = accessToken
        if (token != null) {
            try { request("POST", "/auth/v1/logout", JSONObject(), bearer = token) } catch (_: Exception) {}
        }
        clear()
    }

    // ---------- internal ----------

    private fun clear() {
        prefs.edit().clear().apply()
        AuthTokenHolder.accessToken = null
    }

    private fun persistUser(user: JSONObject): AuthUser {
        val meta = user.optJSONObject("user_metadata")
        val appMeta = user.optJSONObject("app_metadata")
        val email = user.str("email") ?: ""
        val u = AuthUser(
            id = user.getString("id"),
            fullName = meta?.str("full_name") ?: email,
            email = email,
            phone = meta?.str("phone"),
            role = appMeta?.str("role") ?: "citizen",
            categoryIds = extractCategoryIds(meta),
            sectorId = meta?.optLong("sectorId", 0L)?.takeIf { it > 0 },
        )
        prefs.edit()
            .putString("user_id", u.id)
            .putString("full_name", u.fullName)
            .putString("email", u.email)
            .putString("phone", u.phone)
            .putString("role", u.role)
            .putString("category_ids", serializeCategoryIds(u.categoryIds))
            .putLong("sector_id", u.sectorId ?: 0L)
            .apply()
        return u
    }

    private fun saveSession(json: JSONObject): AuthUser {
        val user = json.getJSONObject("user")
        val u = persistUser(user)
        prefs.edit()
            .putString("access_token", json.getString("access_token"))
            .putString("refresh_token", json.str("refresh_token"))
            .putLong("expires_at", System.currentTimeMillis() / 1000 + json.optLong("expires_in", 3600))
            .apply()
        AuthTokenHolder.accessToken = json.getString("access_token")
        return u
    }

    private suspend fun request(method: String, path: String, body: JSONObject, bearer: String? = null): JSONObject =
        withContext(Dispatchers.IO) {
            val conn = URL(AuthConfig.SUPABASE_URL + path).openConnection() as HttpURLConnection
            try {
                conn.requestMethod = method
                conn.connectTimeout = 15_000
                conn.readTimeout = 15_000
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("apikey", AuthConfig.SUPABASE_ANON_KEY)
                if (bearer != null) conn.setRequestProperty("Authorization", "Bearer $bearer")
                conn.outputStream.use { it.write(body.toString().toByteArray()) }

                val code = conn.responseCode
                val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                val json = runCatching { JSONObject(text) }.getOrDefault(JSONObject())
                if (code !in 200..299) {
                    throw AuthException(
                        json.str("error_description") ?: json.str("msg") ?: json.str("message")
                            ?: "Something went wrong. Please try again."
                    )
                }
                json
            } finally {
                conn.disconnect()
            }
        }
}
