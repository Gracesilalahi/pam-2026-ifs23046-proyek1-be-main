package org.delcom

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.github.cdimascio.dotenv.dotenv
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.config.*
import kotlinx.serialization.json.Json
import org.delcom.helpers.JWTConstants
import org.delcom.helpers.configureDatabases
import org.delcom.helpers.configureStaticFiles
import org.delcom.module.appModule
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main(args: Array<String>) {
    // 1. Load .env (untuk lokal). Di server Delcom ini akan di-ignore.
    val dotenv = dotenv {
        directory = "."
        ignoreIfMissing = true
    }

    // 2. Masukkan semua isi .env ke System Property agar bisa dibaca kode lain
    dotenv.entries().forEach { entry ->
        if (System.getenv(entry.key) == null) {
            System.setProperty(entry.key, entry.value)
        }
    }

    // 3. Ambil PORT dari Delcom (Sangat krusial untuk menghindari 502)
    val portEnv = System.getenv("PORT") ?: System.getProperty("APP_PORT") ?: "30700"
    val resolvedPort = portEnv.toInt()

    println("🚀 GRACE, KTOR BERHASIL NYALA DI PORT: $resolvedPort")

    // 4. Jalankan Server Netty secara paksa di 0.0.0.0
    embeddedServer(Netty, port = resolvedPort, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

fun Application.module() {
    // 5. AMBIL DATA SETTING (Database, JWT, BaseUrl)
    val jwtSecret = System.getProperty("JWT_SECRET") ?: System.getenv("JWT_SECRET") ?: "rahasia_grace_123"
    val baseUrl = System.getProperty("APP_URL") ?: System.getenv("APP_URL") ?: "http://localhost:8080"

    val dbHost = System.getProperty("DB_HOST") ?: System.getenv("DB_HOST") ?: "127.0.0.1"
    val dbPort = System.getProperty("DB_PORT") ?: System.getenv("DB_PORT") ?: "5432"
    val dbName = System.getProperty("DB_NAME") ?: System.getenv("DB_NAME") ?: ""
    val dbUser = System.getProperty("DB_USER") ?: System.getenv("DB_USER") ?: ""
    val dbPass = System.getProperty("DB_PASSWORD") ?: System.getenv("DB_PASSWORD") ?: ""

    // 6. INJEKSI MANUAL: Biar helper/Koin tidak error 'Property not found'
    (environment.config as? MapApplicationConfig)?.apply {
        put("ktor.jwt.secret", jwtSecret)
        put("ktor.app.baseUrl", baseUrl)
        put("ktor.database.host", dbHost)
        put("ktor.database.port", dbPort)
        put("ktor.database.name", dbName)
        put("ktor.database.user", dbUser)
        put("ktor.database.password", dbPass)
    }

    // Konfigurasi JWT
    install(Authentication) {
        jwt(JWTConstants.NAME) {
            realm = JWTConstants.REALM
            verifier(
                JWT.require(Algorithm.HMAC256(jwtSecret))
                    .withIssuer(JWTConstants.ISSUER)
                    .withAudience(JWTConstants.AUDIENCE)
                    .build()
            )
            validate { credential ->
                val userId = credential.payload.getClaim("userId").asString()
                if (!userId.isNullOrBlank()) JWTPrincipal(credential.payload) else null
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, mapOf("status" to "error", "message" to "Token tidak valid"))
            }
        }
    }

    // Konfigurasi CORS
    install(CORS) {
        anyHost()
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowCredentials = true
    }

    // Konfigurasi JSON
    install(ContentNegotiation) {
        json(Json { explicitNulls = false; prettyPrint = true; ignoreUnknownKeys = true })
    }

    // Konfigurasi Koin
    install(Koin) {
        slf4jLogger()
        modules(appModule(this@module))
    }

    // Panggil Helper
    configureDatabases()
    configureStaticFiles()
    configureRouting()
}