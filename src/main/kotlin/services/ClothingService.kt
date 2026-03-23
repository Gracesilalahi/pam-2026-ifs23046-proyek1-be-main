package org.delcom.services

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
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
    /**
     * 1. Ambil Semua Koleksi (Read All)
     * PERBAIKAN: Menangani logika "Semua" agar lemari tidak kosong saat pertama dibuka.
     */
    // Di dalam file ClothingService.kt
    suspend fun getAll(call: ApplicationCall) {
        val user = ServiceHelper.getAuthUser(call, userRepo)
        val search = call.request.queryParameters["search"]
        val category = call.request.queryParameters["category"]
        val color = call.request.queryParameters["color"]

        val clothes = clothingRepo.getAll(user.id, search, category, color)

        call.respond(DataResponse(
            "success",
            "Berhasil mengambil daftar pakaian",
            // Pakai "clothes" agar sinkron dengan @SerialName di Android
            mapOf("clothes" to clothes)
        ))
    }

    /**
     * 2. Ambil Berdasarkan ID (Read One)
     */
    suspend fun getById(call: ApplicationCall) {
        val clothingId = call.parameters["id"] ?: throw AppException(400, "ID tidak valid!")
        val user = ServiceHelper.getAuthUser(call, userRepo)

        val clothing = clothingRepo.getById(clothingId)
        if (clothing == null || clothing.userId != user.id) {
            throw AppException(404, "Data pakaian tidak ditemukan!")
        }

        call.respond(DataResponse("success", "Berhasil", mapOf("clothing" to clothing)))
    }

    /**
     * 3. Upload/Ubah Foto Pakaian (Multipart)
     * SOLUSI FINAL: Memastikan deskripsi disalin sebelum update database.
     */
    suspend fun putPhoto(call: ApplicationCall) {
        val clothingId = call.parameters["id"] ?: throw AppException(400, "ID tidak valid!")
        val user = ServiceHelper.getAuthUser(call, userRepo)

        // 1. Ambil data lama
        val old = clothingRepo.getById(clothingId) ?: throw AppException(404, "Data tidak ada!")

        // 2. Siapkan request dengan data lama (SINKRONISASI AWAL)
        val request = ClothingRequest().apply {
            userId = user.id
            name = old.name
            category = old.category
            color = old.color
            description = old.description // Data deskripsi aman di sini
        }

        // 3. Proses upload foto (Hanya update photoUrl)
        val multipartData = call.receiveMultipart(formFieldLimit = 1024 * 1024 * 5)
        multipartData.forEachPart { part ->
            if (part is PartData.FileItem) {
                val ext = part.originalFileName?.substringAfterLast('.', "")?.let { ".$it" } ?: ""
                val fileName = UUID.randomUUID().toString() + ext
                val filePath = "uploads/clothing/$fileName"

                val file = File(filePath).apply { parentFile.mkdirs() }
                part.provider().copyAndClose(file.writeChannel())
                request.photoUrl = filePath
            }
            part.dispose()
        }

        if (request.photoUrl == null) throw AppException(400, "Foto gagal diunggah!")

        // 4. Update ke database (HAPUS pengulangan pengisian variabel di sini)
        if (clothingRepo.update(user.id, clothingId, request.toEntity())) {
            old.photoUrl?.let {
                val oldFile = File(it)
                if (oldFile.exists()) oldFile.delete()
            }
            call.respond(DataResponse("success", "Foto berhasil diperbarui", null))
        } else {
            throw AppException(400, "Gagal memperbarui database!")
        }
    }

    /**
     * 4. Tambah Pakaian Baru (Create)
     * PERBAIKAN: Menangani deskripsi dalam format Multipart/Form-Data
     */
    suspend fun post(call: ApplicationCall) {
        val user = ServiceHelper.getAuthUser(call, userRepo)
        val request = ClothingRequest().apply { userId = user.id }

        val multipartData = call.receiveMultipart()
        multipartData.forEachPart { part ->
            when (part) {
                is PartData.FormItem -> {
                    // Pastikan semua field teks tertangkap
                    when (part.name) {
                        "name" -> request.name = part.value
                        "category" -> request.category = part.value
                        "color" -> request.color = part.value
                        "description" -> request.description = part.value
                    }
                }
                is PartData.FileItem -> {
                    val ext = part.originalFileName?.substringAfterLast('.', "")?.let { ".$it" } ?: ""
                    val fileName = UUID.randomUUID().toString() + ext
                    val filePath = "uploads/clothing/$fileName"
                    val file = File(filePath).apply { parentFile.mkdirs() }
                    part.provider().copyAndClose(file.writeChannel())
                    request.photoUrl = filePath
                }
                else -> {}
            }
            part.dispose()
        }

        // Gunakan ValidatorHelper secara manual untuk hasil multipart
        val validator = ValidatorHelper(request.toMap())
        validator.required("name", "Nama pakaian wajib diisi")
        validator.required("category", "Kategori wajib diisi")
        // Validator description opsional, tapi pastikan dia terkirim
        validator.validate()

        val id = clothingRepo.create(request.toEntity())
        call.respond(DataResponse("success", "Berhasil menambah pakaian", mapOf("id" to id)))
    }

    /**
     * 5. Update Data Pakaian
     */
    suspend fun put(call: ApplicationCall) {
        val clothingId = call.parameters["id"] ?: throw AppException(400, "ID tidak valid!")
        val user = ServiceHelper.getAuthUser(call, userRepo)
        val request = call.receive<ClothingRequest>().apply { userId = user.id }

        ValidatorHelper(request.toMap()).apply {
            required("name", "Nama tidak boleh kosong")
            required("category", "Kategori tidak boleh kosong")
            validate()
        }

        val old = clothingRepo.getById(clothingId)
        if (old == null || old.userId != user.id) throw AppException(404, "Data tidak ada!")

        request.photoUrl = old.photoUrl

        if (clothingRepo.update(user.id, clothingId, request.toEntity())) {
            call.respond(DataResponse("success", "Data berhasil diubah", null))
        } else {
            throw AppException(400, "Gagal mengubah data!")
        }
    }

    /**
     * 6. Hapus Pakaian
     */
    suspend fun delete(call: ApplicationCall) {
        val clothingId = call.parameters["id"] ?: throw AppException(400, "ID tidak valid!")
        val user = ServiceHelper.getAuthUser(call, userRepo)

        val old = clothingRepo.getById(clothingId)
        if (old == null || old.userId != user.id) throw AppException(404, "Data tidak ada!")

        if (clothingRepo.delete(user.id, clothingId)) {
            // Hapus file fisik foto dari server agar storage tidak penuh
            old.photoUrl?.let { File(it).delete() }
            call.respond(DataResponse("success", "Berhasil dihapus", null))
        }
    }

    /**
     * 7. Ambil File Gambar untuk Coil di Android
     */
    suspend fun getPhoto(call: ApplicationCall) {
        val clothingId = call.parameters["id"] ?: throw AppException(400, "ID tidak valid!")
        val item = clothingRepo.getById(clothingId) ?: return call.respond(HttpStatusCode.NotFound)

        val photoPath = item.photoUrl ?: throw AppException(404, "Pakaian ini tidak punya foto")
        val file = File(photoPath)

        if (!file.exists()) throw AppException(404, "File foto tidak ditemukan di server")

        // Beri tahu browser/Android bahwa ini adalah gambar
        call.respondFile(file)
    }
}