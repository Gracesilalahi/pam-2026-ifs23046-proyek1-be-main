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
    val dotenv = io.github.cdimascio.dotenv.dotenv {
        directory = "."
        ignoreIfMissing = true
    }

    // 1. Masukkan isi .env ke System Property
    dotenv.entries().forEach { entry ->
        if (System.getenv(entry.key) == null) {
            System.setProperty(entry.key, entry.value)
        }
    }

    // 2. JURUS ANTI-502: Tentukan Port secara Manual
    // Ambil PORT (jatah Delcom), kalau kosong ambil APP_PORT (.env), kalau kosong pakai 8000
    val resolvedPort = System.getenv("PORT")
        ?: System.getProperty("APP_PORT")
        ?: "8000"

    // Set ke System Property "PORT" agar dibaca application.yaml
    System.setProperty("PORT", resolvedPort)

    println("🚀 APLIKASI MENCOBA JALAN DI PORT: $resolvedPort")

    io.ktor.server.netty.EngineMain.main(args)
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
                val userId = credential.payload.getClaim("userId").asString()
                if (!userId.isNullOrBlank()) JWTPrincipal(credential.payload) else null
            }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf("status" to "error", "message" to "Token tidak valid")
                )
            }
        }
    }

    install(CORS) {
        anyHost()
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Options)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.Accept)
        allowHeader(HttpHeaders.Origin)
        allowHeader(HttpHeaders.AccessControlAllowOrigin)
        allowCredentials = true
        exposeHeader(HttpHeaders.ContentDisposition)
    }

    install(ContentNegotiation) {
        json(Json {
            explicitNulls = false
            prettyPrint = true
            ignoreUnknownKeys = true
        })
    }

    install(Koin) {
        slf4jLogger()
        modules(appModule(this@module))
    }

    configureDatabases()
    configureStaticFiles()
    configureRouting()
}