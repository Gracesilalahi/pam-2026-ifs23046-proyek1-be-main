package org.delcom.data

import kotlinx.serialization.Serializable

/**
 * ErrorResponse yang diperbarui agar fleksibel.
 * Menggunakan tipe Any? agar bisa menampung String (untuk error umum)
 * atau Map (untuk error validasi).
 */
@Serializable
data class ErrorResponse(
    val status: String,
    val message: String,
    // Perbaikan: Ubah String? menjadi Any? agar bisa menerima Map
    val data: @kotlinx.serialization.Contextual Any? = null
)