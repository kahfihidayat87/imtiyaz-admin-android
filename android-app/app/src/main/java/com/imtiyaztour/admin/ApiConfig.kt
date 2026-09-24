package com.imtiyaztour.admin

object ApiConfig {
    const val BASE_URL = "https://pastiumrah.com/"
    const val WP_BASE_URL = "https://pastiumrah.com/"

    // Endpoint invoice ada di Node.js (api.pastiumrah.com), bukan WordPress
    const val INVOICE_API_URL = "https://api.pastiumrah.com/"

    // API key proteksi endpoint tulis Node.js (dari hPanel environment variable)
    // PENTING: setelah rotate di hPanel, update nilai ini + rebuild admin app
    const val INVOICE_API_KEY = "IASDGENF394R6HH"
}
