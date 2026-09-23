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
    suspend fun me(@Body body: Map<String, String>): Map<String, Any>?

    @POST("wp-json/imtiyaz/v1/admin-logout")
    suspend fun logout(@Body body: Map<String, String>): Map<String, Boolean>?

    @POST("wp-json/imtiyaz/v1/admin-dashboard")
    suspend fun dashboard(@Body body: Map<String, String>): DashboardResponse

    @POST("wp-json/imtiyaz/v1/admin-jamaah")
    suspend fun jamaahList(@Body body: Map<String, Any>): JamaahListResponse

    @POST("wp-json/imtiyaz/v1/admin-announcement")
    suspend fun sendAnnouncement(@Body body: Map<String, Any>): AnnouncementResponse

    @POST("wp-json/imtiyaz/v1/admin-jamaah-update")
    suspend fun jamaahUpdate(@Body body: Map<String, Any>): SimpleResponse

    @POST("wp-json/imtiyaz/v1/admin-checklist-update")
    suspend fun checklistUpdate(@Body body: Map<String, Any>): SimpleResponse

    @POST("wp-json/imtiyaz/v1/admin-toggle-tl")
    suspend fun toggleTL(@Body body: Map<String, Any>): SimpleResponse
}

object AdminApiClient {
    val service: AdminApiService by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AdminApiService::class.java)
    }
}
