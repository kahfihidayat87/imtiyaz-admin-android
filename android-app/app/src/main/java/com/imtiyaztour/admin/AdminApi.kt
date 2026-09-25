package com.imtiyaztour.admin

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface AdminApiService {
    @POST("wp-json/imtiyaz/v1/admin-login")
    suspend fun login(@Body body: AdminLoginRequest): AdminLoginResponse

    @POST("wp-json/imtiyaz/v1/admin-me")
    suspend fun me(@Body body: Map<String, String>): Map<String, String>?

    @POST("wp-json/imtiyaz/v1/admin-logout")
    suspend fun logout(@Body body: Map<String, String>): Map<String, Boolean>?

    @POST("wp-json/imtiyaz/v1/admin-dashboard")
    suspend fun dashboard(@Body body: Map<String, String>): DashboardResponse

    @POST("wp-json/imtiyaz/v1/admin-jamaah")
    suspend fun jamaahList(@Body body: Map<String, String>): JamaahListResponse

    @POST("wp-json/imtiyaz/v1/admin-announcement")
    suspend fun sendAnnouncement(@Body body: Map<String, String>): AnnouncementResponse

    @POST("wp-json/imtiyaz/v1/admin-jamaah-update")
    suspend fun jamaahUpdate(@Body body: Map<String, String>): SimpleResponse

    @POST("wp-json/imtiyaz/v1/admin-jamaah-create")
    suspend fun jamaahCreate(@Body body: JamaahCreateRequest): JamaahCreateResponse

    @POST("wp-json/imtiyaz/v1/admin-jamaah-delete")
    suspend fun jamaahDelete(@Body body: Map<String, String>): SimpleResponse

    @POST("wp-json/imtiyaz/v1/admin-checklist-update")
    suspend fun checklistUpdate(@Body body: ChecklistUpdateRequest): SimpleResponse

    @POST("wp-json/imtiyaz/v1/admin-toggle-tl")
    suspend fun toggleTL(@Body body: Map<String, String>): SimpleResponse

    @POST("wp-json/imtiyaz/v1/admin-kanal")
    suspend fun kanalList(@Body body: Map<String, String>): KanalListResponse

    @POST("wp-json/imtiyaz/v1/admin-kanal-save")
    suspend fun kanalSave(@Body body: Map<String, String>): KanalSaveResponse

    @POST("wp-json/imtiyaz/v1/admin-kanal-delete")
    suspend fun kanalDelete(@Body body: Map<String, String>): SimpleResponse

    @retrofit2.http.GET("wp-json/imtiyaz/v1/admin-jamaah/{id}")
    suspend fun jamaahDetail(
        @retrofit2.http.Path("id") id: Int,
        @retrofit2.http.Query("admin_id") adminId: String,
        @retrofit2.http.Query("token") token: String
    ): JamaahDetailResponse

    @POST("wp-json/imtiyaz/v1/admin-bukti-list")
    suspend fun buktiList(@Body body: Map<String, String>): BuktiListResponse

    @POST("wp-json/imtiyaz/v1/admin-bukti-approve")
    suspend fun buktiApprove(@Body body: Map<String, String>): SimpleResponse

    @POST("wp-json/imtiyaz/v1/admin-bukti-reject")
    suspend fun buktiReject(@Body body: Map<String, String>): SimpleResponse

    // ===== SUPER-ADMIN =====
    @POST("wp-json/imtiyaz/v1/admin-list")
    suspend fun adminList(@Body body: Map<String, String>): AdminListResponse

    @POST("wp-json/imtiyaz/v1/admin-create")
    suspend fun adminCreate(@Body body: AdminCreateRequest): AdminCreateResponse

    @POST("wp-json/imtiyaz/v1/admin-delete")
    suspend fun adminDelete(@Body body: Map<String, String>): SimpleResponse

    @POST("wp-json/imtiyaz/v1/admin-update-role")
    suspend fun adminUpdateRole(@Body body: Map<String, String>): SimpleResponse

    @POST("wp-json/imtiyaz/v1/admin-reset-password")
    suspend fun adminResetPassword(@Body body: Map<String, String>): SimpleResponse
}

object AdminApiClient {
    val service: AdminApiService by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AdminApiService::class.java)
    }
}
