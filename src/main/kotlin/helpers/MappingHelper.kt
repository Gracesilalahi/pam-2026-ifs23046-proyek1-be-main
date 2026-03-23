package org.delcom.helpers

import kotlinx.coroutines.Dispatchers
import org.delcom.dao.ClothingDAO
import org.delcom.dao.RefreshTokenDAO
import org.delcom.dao.UserDAO
import org.delcom.entities.Clothing
import org.delcom.entities.RefreshToken
import org.delcom.entities.User
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

suspend fun <T> suspendTransaction(block: Transaction.() -> T): T =
    newSuspendedTransaction(Dispatchers.IO, statement = block)

fun userDAOToModel(dao: UserDAO, baseUrl: String) = User(
    id = dao.id.value.toString(),
    name = dao.name,
    username = dao.username,
    password = dao.password,
    photo = dao.photo,
    urlPhoto = buildImageUrl(baseUrl, dao.photo ?: "uploads/defaults/user.png"),
    createdAt = dao.createdAt,
    updatedAt = dao.updatedAt
)

fun refreshTokenDAOToModel(dao: RefreshTokenDAO) = RefreshToken(
    dao.id.value.toString(),
    dao.userId.toString(),
    dao.refreshToken,
    dao.authToken,
    dao.createdAt,
)

// PERUBAHAN UTAMA: Transformasi dari Todo ke Clothing
fun clothingDAOToModel(dao: ClothingDAO, baseUrl: String) = Clothing(
    id = dao.id.value.toString(),
    userId = dao.userId.toString(),
    name = dao.name,           // Sebelumnya title
    category = dao.category,   // Field baru
    color = dao.color,         // Field baru
    description = dao.description,
    photoUrl = dao.photoUrl,   // Sebelumnya cover
    urlPhoto = buildImageUrl(baseUrl, dao.photoUrl ?: "uploads/defaults/clothing.png"),
    createdAt = dao.createdAt,
    updatedAt = dao.updatedAt
)

/**
 * Membangun URL publik gambar dari path relatif.
 * Contoh: "uploads/clothing/uuid.png" → "http://host:port/static/clothing/uuid.png"
 */
fun buildImageUrl(baseUrl: String, pathGambar: String): String {
    // Pastikan path tidak dimulai dengan slash ganda yang tidak sengaja
    val cleanPath = pathGambar.removePrefix("/")
    val relativePath = cleanPath.removePrefix("uploads/")
    return "$baseUrl/static/$relativePath"
}