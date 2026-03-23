package org.delcom.dao

import org.delcom.tables.ClothingTable
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.util.UUID

// Kita gunakan nama ClothingEntity atau tetap ClothingDAO sesuai preferensimu
class ClothingDAO(id: EntityID<UUID>) : Entity<UUID>(id) {
    companion object : EntityClass<UUID, ClothingDAO>(ClothingTable)

    // Pemetaan kolom dari ClothingTable
    var userId by ClothingTable.userId
    var name by ClothingTable.name          // Sebelumnya 'title'
    var category by ClothingTable.category  // Kolom baru untuk filter
    var color by ClothingTable.color        // Kolom baru untuk filter
    var photoUrl by ClothingTable.photoUrl  // Sebelumnya 'cover'
    var description by ClothingTable.description
    var createdAt by ClothingTable.createdAt
    var updatedAt by ClothingTable.updatedAt
}