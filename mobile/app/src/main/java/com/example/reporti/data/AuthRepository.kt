package com.example.reporti.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class AuthUser(val id: String, val fullName: String, val email: String, val phone: String?)

class AuthException(message: String) : Exception(message)

private fun JSONObject.str(key: String): String? =
    if (isNull(key)) null else optString(key).ifBlank { null }

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
        )
    }

    /** Au demarrage : garde la session, la rafraichit si elle a expire. */
    suspend fun restore(): AuthUser? {
        if (accessToken == null) return null
        val expiresAt = prefs.getLong("expires_at", 0L)
        if (expiresAt > System.currentTimeMillis() / 1000 + 30) return currentUser()
        val refresh = prefs.getString("refresh_token", null) ?: run { clear(); return null }
        return try {
            saveSession(post("/auth/v1/token?grant_type=refresh_token",
                JSONObject().put("refresh_token", refresh)))
        } catch (e: AuthException) {
            clear(); null
        } catch (e: IOException) {
            currentUser() // hors ligne : on ne deconnecte pas
        }
    }

    suspend fun login(email: String, password: String): AuthUser =
        saveSession(post("/auth/v1/token?grant_type=password",
            JSONObject().put("email", email.trim()).put("password", password)))

    /** Retourne null si Supabase demande de confirmer l'email avant de connecter. */
    suspend fun register(fullName: String, email: String, phone: String?, password: String): AuthUser? {
        val meta = JSONObject().put("full_name", fullName.trim())
        if (!phone.isNullOrBlank()) meta.put("phone", phone.trim())
        val json = post("/auth/v1/signup",
            JSONObject().put("email", email.trim()).put("password", password).put("data", meta))
        return if (json.has("access_token")) saveSession(json) else null
    }

    suspend fun forgotPassword(email: String) {
        val redirect = URLEncoder.encode(AuthConfig.RESET_REDIRECT_URL, "UTF-8")
        post("/auth/v1/recover?redirect_to=$redirect", JSONObject().put("email", email.trim()))
    }

    suspend fun logout() {
        val token = accessToken
        if (token != null) {
            try { post("/auth/v1/logout", JSONObject(), bearer = token) } catch (_: Exception) {}
        }
        clear()
    }

    // ---------- interne ----------

    private fun clear() = prefs.edit().clear().apply()

    private fun saveSession(json: JSONObject): AuthUser {
        val user = json.getJSONObject("user")
        val meta = user.optJSONObject("user_metadata")
        val email = user.str("email") ?: ""
        val u = AuthUser(
            id = user.getString("id"),
            fullName = meta?.str("full_name") ?: email,
            email = email,
            phone = meta?.str("phone"),
        )
        prefs.edit()
            .putString("access_token", json.getString("access_token"))
            .putString("refresh_token", json.str("refresh_token"))
            .putLong("expires_at", System.currentTimeMillis() / 1000 + json.optLong("expires_in", 3600))
            .putString("user_id", u.id)
            .putString("full_name", u.fullName)
            .putString("email", u.email)
            .putString("phone", u.phone)
            .apply()
        return u
    }

    private suspend fun post(path: String, body: JSONObject, bearer: String? = null): JSONObject =
        withContext(Dispatchers.IO) {
            val conn = URL(AuthConfig.SUPABASE_URL + path).openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "POST"
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