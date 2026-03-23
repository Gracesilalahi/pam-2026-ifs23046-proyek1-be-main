package org.delcom.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object ClothingTable : UUIDTable("clothing") {
    val userId = uuid("user_id").references(UserTable.id) // Pemilik item
    val name = varchar("name", 100) // Contoh: "Kaos Del", "Celana Jeans"
    val category = varchar("category", 50) // Untuk FITUR FILTER (Baju, Celana, Jaket)
    val color = varchar("color", 30).nullable() // Untuk FITUR FILTER
    val photoUrl = text("photo_url").nullable() // Simpan path gambar
    val description = text("description").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}