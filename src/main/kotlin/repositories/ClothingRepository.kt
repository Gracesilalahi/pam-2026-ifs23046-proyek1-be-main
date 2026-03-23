package org.delcom.repositories

import org.delcom.dao.ClothingDAO
import org.delcom.entities.Clothing
import org.delcom.helpers.suspendTransaction
import org.delcom.helpers.clothingDAOToModel
import org.delcom.tables.ClothingTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import kotlinx.datetime.Clock // PERBAIKAN: Gunakan library Kotlinx Datetime
import java.util.*

class ClothingRepository(private val baseUrl: String) : IClothingRepository {

    /**
     * 1. AMBIL SEMUA DATA (Read All)
     */
    override suspend fun getAll(
        userId: String,
        search: String?,
        category: String?,
        color: String?
    ): List<Clothing> = suspendTransaction {
        val query = ClothingTable.selectAll().where {
            ClothingTable.userId eq UUID.fromString(userId)
        }

        // Fitur SEARCH (berdasarkan Nama Pakaian)
        if (!search.isNullOrBlank()) {
            query.andWhere { ClothingTable.name.lowerCase() like "%${search.lowercase()}%" }
        }

        // Fitur FILTER (berdasarkan Kategori)
        if (!category.isNullOrBlank() && category != "Semua") {
            query.andWhere { ClothingTable.category eq category }
        }

        // Fitur FILTER (berdasarkan Warna)
        if (!color.isNullOrBlank()) {
            query.andWhere { ClothingTable.color eq color }
        }

        query.orderBy(ClothingTable.createdAt to SortOrder.DESC)
            .map { clothingDAOToModel(ClothingDAO.wrapRow(it), baseUrl) }
    }

    /**
     * 2. AMBIL BERDASARKAN ID (Read One)
     */
    override suspend fun getById(clothingId: String): Clothing? = suspendTransaction {
        try {
            ClothingDAO.findById(UUID.fromString(clothingId))?.let {
                clothingDAOToModel(it, baseUrl)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 3. TAMBAH PAKAIAN (Create)
     */
    override suspend fun create(clothing: Clothing): String = suspendTransaction {
        val newId = ClothingTable.insertAndGetId {
            it[userId] = UUID.fromString(clothing.userId)
            it[name] = clothing.name
            it[category] = clothing.category
            it[color] = clothing.color
            it[photoUrl] = clothing.photoUrl
            it[description] = clothing.description
            it[createdAt] = clothing.createdAt
            it[updatedAt] = clothing.updatedAt
        }
        newId.value.toString()
    }

    /**
     * 4. UBAH DATA PAKAIAN (Update)
     * PERBAIKAN FINAL: Menambahkan logika "Safe Update" agar data teks tidak hilang
     * saat proses update foto.
     */
    override suspend fun update(userId: String, clothingId: String, newClothing: Clothing): Boolean = suspendTransaction {
        val found = ClothingDAO.find {
            (ClothingTable.id eq UUID.fromString(clothingId)) and
                    (ClothingTable.userId eq UUID.fromString(userId))
        }.firstOrNull()

        found?.apply {
            // Update data teks standar
            name = newClothing.name
            category = newClothing.category
            color = newClothing.color

            // LOGIKA SAFE UPDATE UNTUK DESKRIPSI
            // Hanya update jika deskripsi baru tidak null dan tidak kosong.
            // Jika null, biarkan menggunakan nilai deskripsi yang lama.
            if (!newClothing.description.isNullOrBlank()) {
                description = newClothing.description
            }

            // Update path foto
            if (!newClothing.photoUrl.isNullOrBlank()) {
                photoUrl = newClothing.photoUrl
            }

            // Selalu update waktu perubahan
            updatedAt = Clock.System.now()
        } != null
    }

    /**
     * 5. HAPUS PAKAIAN (Delete)
     */
    override suspend fun delete(userId: String, clothingId: String): Boolean = suspendTransaction {
        ClothingTable.deleteWhere {
            (id eq UUID.fromString(clothingId)) and (this.userId eq UUID.fromString(userId))
        } > 0
    }
}