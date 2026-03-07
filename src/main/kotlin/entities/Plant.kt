package org.delcom.entities

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Plant(
    var id: String = UUID.randomUUID().toString(),
    var nama: String,
    var pathGambar: String,   // Path relatif file di server, contoh: "uploads/plants/uuid.png"
    var gambar: String = "", // URL publik yang dapat diakses langsung, contoh: "http://host/static/plants/uuid.png"
    var deskripsi: String,
    var manfaat: String,
    var efekSamping: String,

    @Contextual
    val createdAt: Instant = Clock.System.now(),
    @Contextual
    var updatedAt: Instant = Clock.System.now(),
)