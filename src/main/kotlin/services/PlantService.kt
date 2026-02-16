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
    // Mengambil semua data tumbuhan
    suspend fun getAllPlants(call: ApplicationCall) {
        val search = call.request.queryParameters["search"] ?: ""

        val plants = plantRepository.getPlants(search)

        val response = DataResponse(
            "success",
            "Berhasil mengambil daftar tumbuhan",
            mapOf(Pair("plants", plants))
        )
        call.respond(response)
    }

    // Mengambil data tumbuhan berdasarkan id
    suspend fun getPlantById(call: ApplicationCall) {
        val id = call.parameters["id"]
            ?: throw AppException(400, "ID tumbuhan tidak boleh kosong!")

        val plant = plantRepository.getPlantById(id) ?: throw AppException(404, "Data tumbuhan tidak tersedia!")

        val response = DataResponse(
            "success",
            "Berhasil mengambil data tumbuhan",
            mapOf(Pair("plant", plant))
        )
        call.respond(response)
    }

    // Ambil data request
    private suspend fun getPlantRequest(call: ApplicationCall): PlantRequest {
        // Buat object penampung
        val plantReq = PlantRequest()

        val multipartData = call.receiveMultipart(formFieldLimit = 1024 * 1024 * 5)
        multipartData.forEachPart { part ->
            when (part) {
                // Ambil request berupa teks
                is PartData.FormItem -> {
                    when (part.name) {
                        "nama" -> plantReq.nama = part.value.trim()
                        "deskripsi" -> plantReq.deskripsi = part.value
                        "manfaat" -> plantReq.manfaat = part.value
                        "efekSamping" -> plantReq.efekSamping = part.value
                    }
                }

                // Upload file
                is PartData.FileItem -> {
                    val ext = part.originalFileName
                        ?.substringAfterLast('.', "")
                        ?.let { if (it.isNotEmpty()) ".$it" else "" }
                        ?: ""

                    val fileName = UUID.randomUUID().toString() + ext
                    val filePath = "uploads/plants/$fileName"

                    val file = File(filePath)
                    file.parentFile.mkdirs() // pastikan folder ada

                    part.provider().copyAndClose(file.writeChannel())
                    plantReq.pathGambar = filePath
                }

                else -> {}
            }

            part.dispose()
        }

        return plantReq
    }

    // Validasi request data dari pengguna
    private fun validatePlantRequest(plantReq: PlantRequest){
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

    // Menambahkan data tumbuhan
    suspend fun createPlant(call: ApplicationCall) {
        // Ambil data request
        val plantReq = getPlantRequest(call)

        // Validasi request
        validatePlantRequest(plantReq)

        // periksa plant dengan nama yang sama
        val existPlant = plantRepository.getPlantByName(plantReq.nama)
        if(existPlant != null){
            val tmpFile = File(plantReq.pathGambar)
            if(tmpFile.exists()){
                tmpFile.delete()
            }
            throw AppException(409, "Tumbuhan dengan nama ini sudah terdaftar!")
        }

        val plantId = plantRepository.addPlant(
            plantReq.toEntity()
        )

        val response = DataResponse(
            "success",
            "Berhasil menambahkan data tumbuhan",
            mapOf(Pair("plantId", plantId))
        )
        call.respond(response)
    }

    // Mengubah data tumbuhan
    suspend fun updatePlant(call: ApplicationCall) {
        val id = call.parameters["id"]
            ?: throw AppException(400, "ID tumbuhan tidak boleh kosong!")

        val oldPlant = plantRepository.getPlantById(id) ?: throw AppException(404, "Data tumbuhan tidak tersedia!")

        // Ambil data request
        val plantReq = getPlantRequest(call)

        if(plantReq.pathGambar.isEmpty()){
            plantReq.pathGambar = oldPlant.pathGambar
        }

        // Validasi request
        validatePlantRequest(plantReq)

        // periksa plant dengan nama yang sama jika nama diubah
        if(plantReq.nama != oldPlant.nama){
            val existPlant = plantRepository.getPlantByName(plantReq.nama)
            if(existPlant != null){
                val tmpFile = File(plantReq.pathGambar)
                if(tmpFile.exists()){
                    tmpFile.delete()
                }
                throw AppException(409, "Tumbuhan dengan nama ini sudah terdaftar!")
            }
        }

        // Hapus gambar lama jika mengupload file baru
        if(plantReq.pathGambar != oldPlant.pathGambar){
            val oldFile = File(oldPlant.pathGambar)
            if(oldFile.exists()){
                oldFile.delete()
            }
        }

        val isUpdated = plantRepository.updatePlant(
            id, plantReq.toEntity()
        )
        if (!isUpdated) {
            throw AppException(400, "Gagal memperbarui data tumbuhan!")
        }

        val response = DataResponse(
            "success",
            "Berhasil mengubah data tumbuhan",
            null
        )
        call.respond(response)
    }

    // Menghapus data tumbuhan
    suspend fun deletePlant(call: ApplicationCall) {
        val id = call.parameters["id"]
            ?: throw AppException(400, "ID tumbuhan tidak boleh kosong!")

        val oldPlant = plantRepository.getPlantById(id) ?: throw AppException(404, "Data tumbuhan tidak tersedia!")

        val oldFile = File(oldPlant.pathGambar)

        val isDeleted = plantRepository.removePlant(id)
        if (!isDeleted) {
            throw AppException(400, "Gagal menghapus data tumbuhan!")
        }

        // Hapus data gambar jika data tumbuhan sudah dihapus
        if (oldFile.exists()) {
            oldFile.delete()
        }

        val response = DataResponse(
            "success",
            "Berhasil menghapus data tumbuhan",
            null
        )
        call.respond(response)
    }

    // Mengambil gambar tumbuhan
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
}