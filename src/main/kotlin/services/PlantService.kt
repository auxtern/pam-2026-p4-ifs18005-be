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
import org.delcom.data.PlantRequest
import org.delcom.helpers.ValidatorHelper
import org.delcom.repositories.IPlantRepository
import java.io.File
import java.util.*

class PlantService(private val plantRepository: IPlantRepository) {

    // ─────────────────────────────────────────────
    // Mengambil semua data tumbuhan
    // ─────────────────────────────────────────────
    suspend fun getAllPlants(call: ApplicationCall) {
        val search = call.request.queryParameters["search"] ?: ""

        val plants = plantRepository.getPlants(search)

        val response = DataResponse(
            status = "success",
            message = "Berhasil mengambil daftar tumbuhan",
            data = mapOf("plants" to plants)
        )
        call.respond(response)
    }

    // ─────────────────────────────────────────────
    // Mengambil data tumbuhan berdasarkan id
    // ─────────────────────────────────────────────
    suspend fun getPlantById(call: ApplicationCall) {
        val id = call.parameters["id"]
            ?: throw AppException(400, "ID tumbuhan tidak boleh kosong!")

        val plant = plantRepository.getPlantById(id)
            ?: throw AppException(404, "Data tumbuhan tidak tersedia!")

        val response = DataResponse(
            status = "success",
            message = "Berhasil mengambil data tumbuhan",
            data = mapOf("plant" to plant)
        )
        call.respond(response)
    }

    // ─────────────────────────────────────────────
    // Menambahkan data tumbuhan
    // ─────────────────────────────────────────────
    suspend fun createPlant(call: ApplicationCall) {
        val plantReq = getPlantRequest(call)

        validatePlantRequest(plantReq)

        // Periksa plant dengan nama yang sama
        val existPlant = plantRepository.getPlantByName(plantReq.nama)
        if (existPlant != null) {
            cleanupTempFile(plantReq.pathGambar)
            throw AppException(409, "Tumbuhan dengan nama ini sudah terdaftar!")
        }

        val plantId = plantRepository.addPlant(plantReq.toEntity())

        val response = DataResponse(
            status = "success",
            message = "Berhasil menambahkan data tumbuhan",
            data = mapOf("plantId" to plantId)
        )
        call.respond(HttpStatusCode.Created, response)
    }

    // ─────────────────────────────────────────────
    // Mengubah data tumbuhan
    // ─────────────────────────────────────────────
    suspend fun updatePlant(call: ApplicationCall) {
        val id = call.parameters["id"]
            ?: throw AppException(400, "ID tumbuhan tidak boleh kosong!")

        val oldPlant = plantRepository.getPlantById(id)
            ?: throw AppException(404, "Data tumbuhan tidak tersedia!")

        val plantReq = getPlantRequest(call)

        // Gunakan path gambar lama jika tidak ada file baru yang diupload
        if (plantReq.pathGambar.isEmpty()) {
            plantReq.pathGambar = oldPlant.pathGambar
        }

        validatePlantRequest(plantReq)

        // Periksa plant dengan nama yang sama jika nama diubah
        if (plantReq.nama != oldPlant.nama) {
            val existPlant = plantRepository.getPlantByName(plantReq.nama)
            if (existPlant != null) {
                cleanupTempFile(plantReq.pathGambar)
                throw AppException(409, "Tumbuhan dengan nama ini sudah terdaftar!")
            }
        }

        // Hapus gambar lama jika file baru berhasil diupload
        if (plantReq.pathGambar != oldPlant.pathGambar) {
            cleanupTempFile(oldPlant.pathGambar)
        }

        val isUpdated = plantRepository.updatePlant(id, plantReq.toEntity())
        if (!isUpdated) {
            throw AppException(400, "Gagal memperbarui data tumbuhan!")
        }

        val response = DataResponse(
            status = "success",
            message = "Berhasil mengubah data tumbuhan",
            data = null
        )
        call.respond(response)
    }

    // ─────────────────────────────────────────────
    // Menghapus data tumbuhan
    // ─────────────────────────────────────────────
    suspend fun deletePlant(call: ApplicationCall) {
        val id = call.parameters["id"]
            ?: throw AppException(400, "ID tumbuhan tidak boleh kosong!")

        val oldPlant = plantRepository.getPlantById(id)
            ?: throw AppException(404, "Data tumbuhan tidak tersedia!")

        val isDeleted = plantRepository.removePlant(id)
        if (!isDeleted) {
            throw AppException(400, "Gagal menghapus data tumbuhan!")
        }

        // Hapus file gambar setelah data berhasil dihapus dari database
        cleanupTempFile(oldPlant.pathGambar)

        val response = DataResponse(
            status = "success",
            message = "Berhasil menghapus data tumbuhan",
            data = null
        )
        call.respond(response)
    }

    // ─────────────────────────────────────────────
    // Mengambil gambar tumbuhan (endpoint kompatibilitas)
    // Client dianjurkan menggunakan field "gambar" pada response data
    // yang sudah berisi URL langsung, daripada memanggil endpoint ini.
    // ─────────────────────────────────────────────
    suspend fun getPlantImage(call: ApplicationCall) {
        val id = call.parameters["id"]
            ?: return call.respond(HttpStatusCode.BadRequest)

        val plant = plantRepository.getPlantById(id)
            ?: return call.respond(HttpStatusCode.NotFound)

        val file = File(plant.pathGambar)

        if (!file.exists()) {
            return call.respond(HttpStatusCode.NotFound)
        }

        call.respondFile(file)
    }

    // ─────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────

    /**
     * Membaca data multipart dari request dan mengisinya ke PlantRequest.
     * File yang diupload disimpan di folder "uploads/plants/".
     */
    private suspend fun getPlantRequest(call: ApplicationCall): PlantRequest {
        val plantReq = PlantRequest()

        val multipartData = call.receiveMultipart(formFieldLimit = 1024 * 1024 * 5)
        multipartData.forEachPart { part ->
            when (part) {
                is PartData.FormItem -> {
                    when (part.name) {
                        "nama"        -> plantReq.nama = part.value.trim()
                        "deskripsi"   -> plantReq.deskripsi = part.value
                        "manfaat"     -> plantReq.manfaat = part.value
                        "efekSamping" -> plantReq.efekSamping = part.value
                    }
                }

                is PartData.FileItem -> {
                    val ext = part.originalFileName
                        ?.substringAfterLast('.', "")
                        ?.let { if (it.isNotEmpty()) ".$it" else "" }
                        ?: ""

                    val fileName = "${UUID.randomUUID()}$ext"

                    // File disimpan di "uploads/plants/" agar dapat dilayani
                    // oleh Static Content plugin melalui URL "/static/plants/..."
                    val filePath = "uploads/plants/$fileName"
                    val file = File(filePath)
                    file.parentFile.mkdirs()

                    part.provider().copyAndClose(file.writeChannel())
                    plantReq.pathGambar = filePath
                }

                else -> {}
            }
            part.dispose()
        }

        return plantReq
    }

    /**
     * Memvalidasi data request tumbuhan.
     */
    private fun validatePlantRequest(plantReq: PlantRequest) {
        val validatorHelper = ValidatorHelper(plantReq.toMap())
        validatorHelper.required("nama", "Nama tidak boleh kosong")
        validatorHelper.required("deskripsi", "Deskripsi tidak boleh kosong")
        validatorHelper.required("manfaat", "Manfaat tidak boleh kosong")
        validatorHelper.required("efekSamping", "Efek Samping tidak boleh kosong")
        validatorHelper.required("pathGambar", "Gambar tidak boleh kosong")
        validatorHelper.validate()

        val file = File(plantReq.pathGambar)
        if (!file.exists()) {
            throw AppException(400, "Gambar tumbuhan gagal diupload!")
        }
    }

    /**
     * Menghapus file sementara atau file lama dari disk.
     */
    private fun cleanupTempFile(path: String) {
        if (path.isBlank()) return
        val file = File(path)
        if (file.exists()) file.delete()
    }
}