package org.delcom.services

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.response.*
import org.delcom.data.DataResponse
import java.io.File

class ProfileService(private val baseUrl: String) {

    // ─────────────────────────────────────────────
    // Mengambil data profile pengembang
    // ─────────────────────────────────────────────
    suspend fun getProfile(call: ApplicationCall) {
        val response = DataResponse(
            status = "success",
            message = "Berhasil mengambil profile pengembang",
            data = mapOf(
                "username" to "abdullah.ubaid",
                "nama"     to "Abdullah Ubaid",
                "tentang"  to "Saya adalah seorang developer yang tertarik pada mobile development, backend API, dan berbagai teknologi pengembangan aplikasi. Senang belajar hal baru dan membangun aplikasi yang berguna.",
                "photo"    to "$baseUrl/static/profile/me.png",
            )
        )
        call.respond(response)
    }

    // ─────────────────────────────────────────────
    // Mengambil photo profile (endpoint kompatibilitas)
    // Client dianjurkan menggunakan field "photo" pada response /profile
    // yang sudah berisi URL langsung, daripada memanggil endpoint ini.
    // ─────────────────────────────────────────────
    suspend fun getProfilePhoto(call: ApplicationCall) {
        val file = File("uploads/profile/me.png")

        if (!file.exists()) {
            return call.respond(HttpStatusCode.NotFound)
        }

        call.respondFile(file)
    }
}