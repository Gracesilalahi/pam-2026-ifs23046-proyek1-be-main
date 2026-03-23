package org.delcom

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.request.*
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
        // 1. Tangkap AppException (Error bisnis/validasi)
        exception<AppException> { call, cause ->
            val dataMap: Map<String, List<String>> = parseMessageToMap(cause.message)
            call.respond(
                status = HttpStatusCode.fromValue(cause.code),
                message = ErrorResponse(
                    status = "fail",
                    message = if (dataMap.isEmpty()) cause.message else "Data yang dikirimkan tidak valid!",
                    data = if (dataMap.isEmpty()) null else dataMap
                )
            )
        }

        // 2. KUNCI PERBAIKAN: Tangkap error transformasi JSON (PENTING!)
        // Ini akan menangkap jika Content-Type salah atau JSON tidak bisa di-parse
        exception<ContentTransformationException> { call, cause ->
            call.respond(
                status = HttpStatusCode.UnsupportedMediaType,
                message = ErrorResponse(
                    status = "error",
                    message = "Server tidak mendukung format data ini atau JSON rusak: ${cause.message}",
                    data = null
                )
            )
        }

        // 3. Tangkap semua error sistem lainnya (Error 500)
        exception<Throwable> { call, cause ->
            // Print error ke console server agar bisa dibaca di Log Delcom
            call.application.environment.log.error("🔴 CRITICAL ERROR: ${cause.message}", cause)

            call.respond(
                status = HttpStatusCode.InternalServerError,
                message = ErrorResponse(
                    status = "error",
                    message = cause.message ?: "Terjadi kesalahan pada server internal",
                    data = null
                )
            )
        }
    }

    routing {
        get("/") {
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
                get { clothingService.getAll(call) }

                // Tambah Pakaian (Pastikan Service sudah pakai call.receive<ClothingRequest>())
                post {
                    clothingService.post(call)
                }

                route("/{id}") {
                    get { clothingService.getById(call) }
                    put { clothingService.put(call) }
                    put("/photo") { clothingService.putPhoto(call) }
                    delete { clothingService.delete(call) }
                }
            }
        }

        // --- 3. Route Gambar ---
        route("/images") {
            get("users/{id}") { userService.getPhoto(call) }
            get("clothing/{id}") { clothingService.getPhoto(call) }
        }
    }
}