package com.imtiyaztour.admin

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface AdminApiService {
    @POST("api/admin/admin-login")
    suspend fun login(@Body body: AdminLoginRequest): AdminLoginResponse

    @POST("api/admin/admin-me")
    suspend fun me(@Body body: Map<String, String>): Map<String, String>?

    @POST("api/admin/admin-logout")
    suspend fun logout(@Body body: Map<String, String>): Map<String, Boolean>?

    @POST("api/admin/admin-dashboard")
    suspend fun dashboard(@Body body: Map<String, String>): DashboardResponse

    @POST("api/admin/admin-jamaah")
    suspend fun jamaahList(@Body body: Map<String, String>): JamaahListResponse

    @POST("api/admin/admin-announcement")
    suspend fun sendAnnouncement(@Body body: Map<String, String>): AnnouncementResponse

    @POST("api/admin/admin-jamaah-update")
    suspend fun jamaahUpdate(@Body body: Map<String, String>): SimpleResponse

    @POST("api/admin/admin-jamaah-create")
    suspend fun jamaahCreate(@Body body: JamaahCreateRequest): JamaahCreateResponse

    @POST("api/admin/admin-jamaah-delete")
    suspend fun jamaahDelete(@Body body: Map<String, String>): SimpleResponse

    @POST("api/admin/admin-invoice-data-save")
    suspend fun invoiceDataSave(@Body body: InvoiceDataSaveRequest): SimpleResponse

    @POST("api/admin/admin-invoice-data-get")
    suspend fun invoiceDataGet(@Body body: Map<String, String>): InvoiceDataGetResponse

    @POST("api/admin/admin-checklist-update")
    suspend fun checklistUpdate(@Body body: ChecklistUpdateRequest): SimpleResponse

    @POST("api/admin/admin-toggle-tl")
    suspend fun toggleTL(@Body body: Map<String, String>): SimpleResponse

    @POST("api/admin/admin-kesehatan-get")
    suspend fun kesehatanGet(@Body body: Map<String, String>): KesehatanGetResponse

    @POST("api/admin/admin-kesehatan-save")
    suspend fun kesehatanSave(@Body body: KesehatanSaveRequest): SimpleResponse

    @retrofit2.http.GET("api/admin/admin-jamaah/{id}")
    suspend fun jamaahDetail(
        @retrofit2.http.Path("id") id: Int,
        @retrofit2.http.Query("admin_id") adminId: String,
        @retrofit2.http.Query("token") token: String
    ): JamaahDetailResponse

    @POST("api/admin/admin-bukti-list")
    suspend fun buktiList(@Body body: Map<String, String>): BuktiListResponse

    @POST("api/admin/admin-bukti-approve")
    suspend fun buktiApprove(@Body body: Map<String, String>): SimpleResponse

    @POST("api/admin/admin-bukti-reject")
    suspend fun buktiReject(@Body body: Map<String, String>): SimpleResponse

    // ===== SUPER-ADMIN =====
    @POST("api/admin/admin-skrining-list")
    suspend fun skriningList(@Body body: Map<String, String>): SkriningListResponse

    @POST("api/admin/admin-list")
    suspend fun adminList(@Body body: Map<String, String>): AdminListResponse

    @POST("api/admin/admin-create")
    suspend fun adminCreate(@Body body: AdminCreateRequest): AdminCreateResponse

    @POST("api/admin/admin-delete")
    suspend fun adminDelete(@Body body: Map<String, String>): SimpleResponse

    @POST("api/admin/admin-update-role")
    suspend fun adminUpdateRole(@Body body: Map<String, String>): SimpleResponse

    @POST("api/admin/admin-reset-password")
    suspend fun adminResetPassword(@Body body: Map<String, String>): SimpleResponse

    // Fase 2 D3: Firebase Custom Token untuk admin
    @POST("api/firebase-token-admin")
    suspend fun firebaseAdminToken(@Body body: Map<String, String>): FirebaseAdminTokenResponse
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

    @POST("api/admin/admin-radio-revoke")
    suspend fun adminRadioRevoke(@Body body: Map<String, String>): SimpleResponse

}
