package org.delcom

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.github.cdimascio.dotenv.dotenv
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import kotlinx.serialization.json.Json
import org.delcom.helpers.JWTConstants
import org.delcom.helpers.configureDatabases
import org.delcom.helpers.configureStaticFiles
import org.delcom.module.appModule
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main(args: Array<String>) {
    // 1. Ubah ignoreIfMissing jadi true agar tidak crash kalau file .env tidak ada di server
    val dotenv = dotenv {
        directory = "."
        ignoreIfMissing = true
    }

    // 2. Hanya set property kalau di environment sistem BELUM ADA
    // Ini biar settingan Delcom (seperti PORT) tidak tertimpa oleh isi .env lokal
    dotenv.entries().forEach { entry ->
        if (System.getenv(entry.key) == null) {
            System.setProperty(entry.key, entry.value)
        }
    }

    EngineMain.main(args)
}

fun Application.module() {

    val jwtSecret = environment.config.property("ktor.jwt.secret").getString()

    install(Authentication) {
        jwt(JWTConstants.NAME) {
            realm = JWTConstants.REALM

            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwtSecret))
                    .withIssuer(JWTConstants.ISSUER)
                    .withAudience(JWTConstants.AUDIENCE)
                    .build()
            )

            validate { credential ->
                val userId = credential.payload
                    .getClaim("userId")
                    .asString()

                if (!userId.isNullOrBlank())
                    JWTPrincipal(credential.payload)
                else null
            }

            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf(
                        "status" to "error",
                        "message" to "Token tidak valid"
                    )
                )
            }
        }
    }

    install(CORS) {
        anyHost()

        // HTTP Methods
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Options)

        // Headers yang umum dikirim browser
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.Accept)
        allowHeader(HttpHeaders.Origin)
        allowHeader(HttpHeaders.AccessControlAllowOrigin)

        // Izinkan credentials (cookie/token) jika diperlukan
        allowCredentials = true

        // Izinkan browser membaca header response ini
        exposeHeader(HttpHeaders.ContentDisposition)
    }

    install(ContentNegotiation) {
        json(
            Json {
                explicitNulls = false
                prettyPrint = true
                ignoreUnknownKeys = true
            }
        )
    }

    install(Koin) {
        slf4jLogger()
        // Teruskan instance Application ke appModule agar bisa membaca baseUrl dan jwtSecret
        modules(appModule(this@module))
    }

    configureDatabases()
    configureStaticFiles()   // Daftarkan folder uploads/ sebagai file statis publik
    configureRouting()
}
