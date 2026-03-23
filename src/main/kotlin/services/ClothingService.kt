package org.delcom.services

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import org.delcom.data.AppException
import org.delcom.data.DataResponse
import org.delcom.data.ClothingRequest
import org.delcom.helpers.ServiceHelper
import org.delcom.helpers.ValidatorHelper
import org.delcom.repositories.IClothingRepository
import org.delcom.repositories.IUserRepository
import java.io.File
import java.util.*

class ClothingService(
    private val userRepo: IUserRepository,
    private val clothingRepo: IClothingRepository
) {
    suspend fun getAll(call: ApplicationCall) {
        val user = ServiceHelper.getAuthUser(call, userRepo)
        val search = call.request.queryParameters["search"]
        val category = call.request.queryParameters["category"]
        val color = call.request.queryParameters["color"]

        val clothes = clothingRepo.getAll(user.id, search, category, color)
        call.respond(DataResponse("success", "Berhasil mengambil daftar pakaian", mapOf("clothes" to clothes)))
    }

    suspend fun getById(call: ApplicationCall) {
        val clothingId = call.parameters["id"] ?: throw AppException(400, "ID tidak valid!")
        val user = ServiceHelper.getAuthUser(call, userRepo)
        val clothing = clothingRepo.getById(clothingId)
        if (clothing == null || clothing.userId != user.id) throw AppException(404, "Data tidak ditemukan!")
        call.respond(DataResponse("success", "Berhasil", mapOf("clothing" to clothing)))
    }

    suspend fun putPhoto(call: ApplicationCall) {
        val clothingId = call.parameters["id"] ?: throw AppException(400, "ID tidak valid!")
        val user = ServiceHelper.getAuthUser(call, userRepo)
        val old = clothingRepo.getById(clothingId) ?: throw AppException(404, "Data tidak ada!")

        if (!call.request.contentType().match(ContentType.MultiPart.FormData)) {
            throw AppException(415, "Gunakan format Multipart untuk upload foto!")
        }

        val request = ClothingRequest().apply {
            userId = user.id
            name = old.name
            category = old.category
            color = old.color
            description = old.description
        }

        val multipartData = call.receiveMultipart()
        multipartData.forEachPart { part ->
            if (part is PartData.FileItem) {
                val ext = part.originalFileName?.substringAfterLast('.', "")?.let { ".$it" } ?: ""
                val fileName = UUID.randomUUID().toString() + ext
                val filePath = "uploads/clothing/$fileName"
                val file = File(filePath).apply { parentFile.mkdirs() }
                part.provider().toInputStream().use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
                request.photoUrl = filePath
            }
            part.dispose()
        }

        if (request.photoUrl == null) throw AppException(400, "Foto gagal diunggah!")
        if (clothingRepo.update(user.id, clothingId, request.toEntity())) {
            old.photoUrl?.let { File(it).delete() }
            call.respond(DataResponse("success", "Foto berhasil diperbarui", null))
        } else throw AppException(400, "Gagal memperbarui database!")
    }

    suspend fun post(call: ApplicationCall) {
        try {
            val user = ServiceHelper.getAuthUser(call, userRepo)

            // Mengambil data body sebagai objek JSON (Kunci utama)
            val request = call.receive<ClothingRequest>()
            request.userId = user.id

            ValidatorHelper(request.toMap()).apply {
                required("name", "Nama pakaian wajib diisi")
                required("category", "Kategori wajib diisi")
                validate()
            }

            val id = clothingRepo.create(request.toEntity())
            call.respond(HttpStatusCode.Created, DataResponse("success", "Berhasil", mapOf("id" to id)))
        } catch (e: Exception) {
            call.application.environment.log.error("🔴 GAGAL POST CLOTHING: ${e.message}")
            throw e
        }
    }

    suspend fun put(call: ApplicationCall) {
        val clothingId = call.parameters["id"] ?: throw AppException(400, "ID tidak valid!")
        val user = ServiceHelper.getAuthUser(call, userRepo)
        val request = call.receive<ClothingRequest>().apply { userId = user.id }

        val old = clothingRepo.getById(clothingId)
        if (old == null || old.userId != user.id) throw AppException(404, "Data tidak ada!")
        request.photoUrl = old.photoUrl

        if (clothingRepo.update(user.id, clothingId, request.toEntity())) {
            call.respond(DataResponse("success", "Data berhasil diubah", null))
        } else throw AppException(400, "Gagal mengubah data!")
    }

    suspend fun delete(call: ApplicationCall) {
        val clothingId = call.parameters["id"] ?: throw AppException(400, "ID tidak valid!")
        val user = ServiceHelper.getAuthUser(call, userRepo)
        val old = clothingRepo.getById(clothingId)
        if (old == null || old.userId != user.id) throw AppException(404, "Data tidak ada!")
        if (clothingRepo.delete(user.id, clothingId)) {
            old.photoUrl?.let { File(it).delete() }
            call.respond(DataResponse("success", "Berhasil dihapus", null))
        }
    }

    suspend fun getPhoto(call: ApplicationCall) {
        val clothingId = call.parameters["id"] ?: throw AppException(400, "ID tidak valid!")
        val item = clothingRepo.getById(clothingId) ?: return call.respond(HttpStatusCode.NotFound)
        val file = File(item.photoUrl ?: "")
        if (!file.exists()) return call.respond(HttpStatusCode.NotFound)
        call.respondFile(file)
    }
}