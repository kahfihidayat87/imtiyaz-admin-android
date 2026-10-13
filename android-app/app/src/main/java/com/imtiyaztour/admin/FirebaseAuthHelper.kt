package com.imtiyaztour.admin

import android.util.Log
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

// Fase 2 D3: Firebase Custom Auth untuk admin (platform=admin).
data class FirebaseAdminTokenResponse(
    val token: String? = null,
    val uid: String? = null,
    val role: String? = null,
    val error: String? = null
)

object FirebaseAuthHelper {
    private const val TAG = "FirebaseAuthHelper"

    /**
     * Login Firebase pakai custom token dari server (/api/firebase-token-admin).
     * Return true kalau sukses. Tidak block login WP kalau gagal.
     */
    suspend fun loginFirebase(adminId: String, wpToken: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val resp = AdminApiClient.service.firebaseAdminToken(
                    mapOf("admin_id" to adminId, "token" to wpToken)
                )
                if (resp.token.isNullOrBlank()) {
                    Log.w(TAG, "Token kosong: ${resp.error}")
                    return@withContext false
                }
                Firebase.auth.signInWithCustomToken(resp.token).await()
                Log.i(TAG, "Firebase admin login OK — uid=${resp.uid}, role=${resp.role}")
                true
            } catch (e: Exception) {
                Log.w(TAG, "Firebase admin login gagal: ${e.message}")
                false
            }
        }

    fun logout() {
        try {
            Firebase.auth.signOut()
        } catch (e: Exception) {
            Log.w(TAG, "signOut error: ${e.message}")
        }
    }
}
