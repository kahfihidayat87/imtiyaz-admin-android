package com.imtiyaztour.admin

import android.content.Context

object Prefs {
    private const val NAME = "imtiyaz_admin_prefs"
    private fun get(ctx: Context) = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun getAdminId(ctx: Context): String = get(ctx).getString("admin_id", "") ?: ""
    fun getToken(ctx: Context): String = get(ctx).getString("admin_token", "") ?: ""
    fun getNama(ctx: Context): String = get(ctx).getString("admin_nama", "") ?: ""
    fun getRole(ctx: Context): String = get(ctx).getString("admin_role", "") ?: ""
    fun getKanalId(ctx: Context): String = get(ctx).getString("admin_kanal", "") ?: ""

    fun isLoggedIn(ctx: Context): Boolean =
        getAdminId(ctx).isNotBlank() && getToken(ctx).isNotBlank()

    fun saveLogin(ctx: Context, adminId: String, token: String, nama: String, role: String, kanalId: String) {
        get(ctx).edit()
            .putString("admin_id", adminId)
            .putString("admin_token", token)
            .putString("admin_nama", nama)
            .putString("admin_role", role)
            .putString("admin_kanal", kanalId)
            .apply()
    }

    fun clearLogin(ctx: Context) {
        get(ctx).edit()
            .remove("admin_id").remove("admin_token")
            .remove("admin_nama").remove("admin_role")
            .remove("admin_kanal").apply()
    }
}
