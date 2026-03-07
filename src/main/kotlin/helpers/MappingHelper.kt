package org.delcom.helpers

import kotlinx.coroutines.Dispatchers
import org.delcom.dao.PlantDAO
import org.delcom.entities.Plant
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

suspend fun <T> suspendTransaction(block: Transaction.() -> T): T =
    newSuspendedTransaction(Dispatchers.IO, statement = block)

/**
 * Mengonversi PlantDAO ke Plant entity.
 * Parameter [baseUrl] digunakan untuk membangun URL publik gambar.
 */
fun daoToModel(dao: PlantDAO, baseUrl: String) = Plant(
    id = dao.id.value.toString(),
    nama = dao.nama,
    pathGambar = dao.pathGambar,
    gambar = buildImageUrl(baseUrl, dao.pathGambar),
    deskripsi = dao.deskripsi,
    manfaat = dao.manfaat,
    efekSamping = dao.efekSamping,
    createdAt = dao.createdAt,
    updatedAt = dao.updatedAt,
)

/**
 * Membangun URL publik gambar dari path relatif.
 * Contoh: "uploads/plants/uuid.png" → "http://host:port/static/plants/uuid.png"
 *
 * Folder "uploads/" pada path relatif dipetakan ke route "/static/"
 * yang dilayani oleh Ktor Static Content plugin.
 */
fun buildImageUrl(baseUrl: String, pathGambar: String): String {
    // Hilangkan prefix "uploads/" dan ganti dengan "/static/"
    val relativePath = pathGambar.removePrefix("uploads/")
    return "$baseUrl/static/$relativePath"
}