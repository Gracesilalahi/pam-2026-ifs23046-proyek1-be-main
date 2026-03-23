package org.delcom.repositories

import org.delcom.entities.Clothing // Pastikan kamu sudah membuat model/entity Clothing

interface IClothingRepository {
    // Ditambah parameter category dan color untuk fitur Filter
    suspend fun getAll(
        userId: String,
        search: String?,
        category: String?,
        color: String?
    ): List<Clothing>

    suspend fun getById(clothingId: String): Clothing?
    suspend fun create(clothing: Clothing): String
    suspend fun update(userId: String, clothingId: String, newClothing: Clothing): Boolean
    suspend fun delete(userId: String, clothingId: String): Boolean
}