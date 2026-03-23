package org.delcom.helpers

import io.ktor.server.application.*
import org.delcom.tables.UserTable
import org.delcom.tables.ClothingTable
import org.delcom.tables.RefreshTokenTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabases() {
    // Kita harus mengambil nilainya dari application.yaml lewat environment.config
    val dbHost = environment.config.property("ktor.database.host").getString()
    val dbPort = environment.config.property("ktor.database.port").getString()
    val dbName = environment.config.property("ktor.database.name").getString()
    val dbUser = environment.config.property("ktor.database.user").getString()
    val dbPassword = environment.config.property("ktor.database.password").getString()

    // Sekarang variabel di atas sudah ada nilainya dan bisa dipakai di sini
    val db = Database.connect(
        url = "jdbc:postgresql://$dbHost:$dbPort/$dbName",
        driver = "org.postgresql.Driver",
        user = dbUser,
        password = dbPassword
    )

    // PROSES AUTO-CREATE TABEL
    transaction(db) {
        // Ini akan membuat tabel jika belum ada di PostgreSQL
        SchemaUtils.create(UserTable, ClothingTable, RefreshTokenTable)
    }
}