package com.imtiyaztour.admin

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// ============================================================================
// [INVOICE] v1.0 — Retrofit client khusus endpoint invoice di Node.js
// (api.pastiumrah.com), TERPISAH dari AdminApiClient yang pakai WordPress
// ============================================================================

interface InvoiceApiService {
    @POST("api/admin-invoice-generate")
    suspend fun generateInvoice(
        @Header("x-api-key") apiKey: String,
        @Body body: InvoiceRequest
    ): InvoiceResponse

    @POST("api/invoice-list")
    suspend fun listInvoice(@Body body: Map<String, String>): List<InvoiceListItem>
}

object InvoiceApiClient {
    val service: InvoiceApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
            .baseUrl(ApiConfig.INVOICE_API_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(InvoiceApiService::class.java)
    }
}
