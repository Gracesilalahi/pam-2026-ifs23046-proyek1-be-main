package org.delcom.data

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import org.delcom.entities.Clothing

@Serializable
data class ClothingRequest(
    var userId: String = "",
    var name: String = "",
    var category: String = "",
    var color: String? = null,
    var description: String? = null,
    var photoUrl: String? = null,
) {
    // Digunakan oleh ValidatorHelper di Service
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "userId" to userId,
            "name" to name,
            "category" to category,
            "color" to color,
            "description" to description,
            "photoUrl" to photoUrl,
        )
    }

    // Mengonversi data request menjadi objek Entity untuk Database
    fun toEntity(): Clothing {
        return Clothing(
            userId = userId,
            name = name,
            category = category,
            color = color,
            description = description,
            photoUrl = photoUrl,
            updatedAt = Clock.System.now()
        )
    }
}