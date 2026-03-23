// KITA KUNCI VERSINYA SECARA PAKSA DI SINI (Hapus 'by project')
val koin_version = "4.0.0"
val ktor_version = "3.0.3"
val kotlin_version = "2.1.0"
val logback_version = "1.4.14"
val dotenv_version = "6.4.1"

plugins {
    kotlin("jvm") version "2.1.0"
    id("io.ktor.plugin") version "3.0.3"
    kotlin("plugin.serialization") version "2.1.0"
}

group = "org.delcom"
version = "0.0.1"

application {
    mainClass.set("org.delcom.ApplicationKt")
}

// Surat perintah untuk KOTLIN
kotlin {
    jvmToolchain(21)
}

// Surat perintah untuk JAVA
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    // SEMUA KTOR KITA KUNCI KE ktor_version (3.0.3)
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-config-yaml:$ktor_version")
    implementation("io.ktor:ktor-server-cors:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("io.ktor:ktor-server-host-common:$ktor_version")
    implementation("io.ktor:ktor-server-status-pages:$ktor_version")
    implementation("io.ktor:ktor-server-auth:$ktor_version")
    implementation("io.ktor:ktor-server-auth-jwt:$ktor_version")

    // Database & Koin dll
    implementation("org.postgresql:postgresql:42.7.9")
    implementation("com.h2database:h2:2.3.232")
    implementation("org.jetbrains.exposed:exposed-core:0.61.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.61.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.61.0")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:0.61.0")
    implementation("io.insert-koin:koin-ktor:$koin_version")
    implementation("io.insert-koin:koin-logger-slf4j:$koin_version")
    implementation("io.github.cdimascio:dotenv-kotlin:$dotenv_version")
    implementation("org.mindrot:jbcrypt:0.4")

    // Testing
    testImplementation("io.ktor:ktor-server-test-host:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}