# TAHAP 1: Bawa "komputer" sendiri yang isinya KHUSUS Java 21
FROM gradle:8.7-jdk21 AS build
WORKDIR /app

# Masukkan semua kodingan Grace ke dalam komputer ini
COPY . .

# Paksa build menggunakan Java 21 (Abaikan mesin Delcom!)
RUN gradle buildFatJar --no-daemon

# TAHAP 2: Siapkan lingkungan yang bersih untuk menjalankan aplikasi
FROM eclipse-temurin:21-jre
WORKDIR /app

# Ambil hasil aplikasi yang sudah sukses di-build
COPY --from=build /app/build/libs/*-all.jar /app/app.jar

# NYALAKAN APLIKASINYA!
ENTRYPOINT ["java", "-jar", "/app/app.jar"]