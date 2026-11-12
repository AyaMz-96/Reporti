package com.example.reporti.data

/**
 * Cross-cutting holder for the current Supabase access token.
 *
 * ApiClient reads it to attach `Authorization: Bearer <token>` to every request.
 * AuthRepository writes to it whenever the session changes (login, restore,
 * refresh, logout). Backed by @Volatile so reads from OkHttp's thread are safe.
 *
 * We intentionally keep this simple instead of injecting a provider: the mobile
 * app already has a single AuthRepository per app (created from Application
 * context) so there is no multi-tenant case.
 */
object AuthTokenHolder {
    @Volatile
    var accessToken: String? = null
}
