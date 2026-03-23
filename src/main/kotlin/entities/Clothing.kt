package org.delcom.entities

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Clothing(
    var id: String = UUID.randomUUID().toString(),
    var userId: String,
    var name: String,         // Pengganti 'title'
    var category: String,     // Wajib untuk fitur FILTER
    var color: String? = null, // Opsional untuk fitur FILTER
    var description: String? = null,
    var photoUrl: String?,    // Pengganti 'cover' (path internal)
    var urlPhoto: String = "", // URL lengkap untuk diakses Frontend/Android

    @Contextual
    val createdAt: Instant = Clock.System.now(),
    @Contextual
    var updatedAt: Instant = Clock.System.now(),
)