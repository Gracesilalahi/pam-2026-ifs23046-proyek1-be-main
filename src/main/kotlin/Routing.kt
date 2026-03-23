package org.delcom

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.delcom.data.AppException
import org.delcom.data.ErrorResponse
import org.delcom.helpers.JWTConstants
import org.delcom.helpers.parseMessageToMap
import org.delcom.services.ClothingService
import org.delcom.services.AuthService
import org.delcom.services.UserService
import org.koin.ktor.ext.inject

/**
 * Konfigurasi Routing API Digital Wardrobe.
 * Mengelola endpoint Auth, Users, dan Clothing.
 */
fun Application.configureRouting() {
    val clothingService: ClothingService by inject()
    val authService: AuthService by inject()
    val userService: UserService by inject()

    install(StatusPages) {
        // Tangkap AppException untuk validasi data atau error bisnis
        exception<AppException> { call, cause ->
            val dataMap: Map<String, List<String>> = parseMessageToMap(cause.message)

            call.respond(
                status = HttpStatusCode.fromValue(cause.code),
                message = ErrorResponse(
                    status = "fail",
                    message = if (dataMap.isEmpty()) cause.message else "Data yang dikirimkan tidak valid!",
                    // PERBAIKAN: Kirim dataMap langsung (sebagai objek JSON), jangan pakai .toString()
                    data = if (dataMap.isEmpty()) null else dataMap
                )
            )
        }

        // Tangkap semua Throwable lainnya (Error sistem 500)
        exception<Throwable> { call, cause ->
            call.respond(
                status = HttpStatusCode.InternalServerError,
                message = ErrorResponse(
                    status = "error",
                    message = cause.message ?: "Terjadi kesalahan pada server",
                    data = null
                )
            )
        }
    }

    routing {
        get("/") {
            // Update identitas sesuai info user
            call.respondText("API Digital Wardrobe Berjalan. Dibuat oleh Grace Sania Silalahi - ifs23046.")
        }

        // --- 1. Route Auth (Public) ---
        route("/auth") {
            post("/login") { authService.postLogin(call) }
            post("/register") { authService.postRegister(call) }
            post("/refresh-token") { authService.postRefreshToken(call) }
            post("/logout") { authService.postLogout(call) }
        }

        // --- 2. Route Private (Butuh Login/JWT) ---
        authenticate(JWTConstants.NAME) {
            // Route Profil User
            route("/users") {
                get("/me") { userService.getMe(call) }
                put("/me") { userService.putMe(call) }
                put("/me/password") { userService.putMyPassword(call) }
                put("/me/photo") { userService.putMyPhoto(call) }
            }

            // Route Digital Wardrobe (CRUD Pakaian)
            route("/clothing") {
                get { clothingService.getAll(call) }        // Mendukung Filter & Search
                post { clothingService.post(call) }       // Tambah Pakaian

                route("/{id}") {
                    get { clothingService.getById(call) }   // Detail Pakaian
                    put { clothingService.put(call) }       // Update Data
                    put("/photo") { clothingService.putPhoto(call) } // Upload Foto
                    delete { clothingService.delete(call) } // Hapus Pakaian
                }
            }
        }

        // --- 3. Route Gambar (Public agar bisa dibaca Coil di Android) ---
        route("/images") {
            get("users/{id}") { userService.getPhoto(call) }
            get("clothing/{id}") { clothingService.getPhoto(call) }
        }
    }
}