package org.delcom.repositories

import org.delcom.dao.PlantDAO
import org.delcom.entities.Plant
import org.delcom.helpers.daoToModel
import org.delcom.helpers.suspendTransaction
import org.delcom.tables.PlantTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.lowerCase
import java.util.UUID

class PlantRepository(private val baseUrl: String) : IPlantRepository {

    override suspend fun getPlants(search: String): List<Plant> = suspendTransaction {
        if (search.isBlank()) {
            PlantDAO.all()
                .orderBy(PlantTable.createdAt to SortOrder.DESC)
                .limit(20)
                .map { daoToModel(it, baseUrl) }
        } else {
            val keyword = "%${search.lowercase()}%"
            PlantDAO
                .find { PlantTable.nama.lowerCase() like keyword }
                .orderBy(PlantTable.nama to SortOrder.ASC)
                .limit(20)
                .map { daoToModel(it, baseUrl) }
        }
    }

    override suspend fun getPlantById(id: String): Plant? = suspendTransaction {
        PlantDAO
            .find { PlantTable.id eq UUID.fromString(id) }
            .limit(1)
            .map { daoToModel(it, baseUrl) }
            .firstOrNull()
    }

    override suspend fun getPlantByName(name: String): Plant? = suspendTransaction {
        PlantDAO
            .find { PlantTable.nama eq name }
            .limit(1)
            .map { daoToModel(it, baseUrl) }
            .firstOrNull()
    }

    override suspend fun addPlant(plant: Plant): String = suspendTransaction {
        val plantDAO = PlantDAO.new {
            nama = plant.nama
            pathGambar = plant.pathGambar
            deskripsi = plant.deskripsi
            manfaat = plant.manfaat
            efekSamping = plant.efekSamping
            createdAt = plant.createdAt
            updatedAt = plant.updatedAt
        }
        plantDAO.id.value.toString()
    }

    override suspend fun updatePlant(id: String, newPlant: Plant): Boolean = suspendTransaction {
        val plantDAO = PlantDAO
            .find { PlantTable.id eq UUID.fromString(id) }
            .limit(1)
            .firstOrNull()

        if (plantDAO != null) {
            plantDAO.nama = newPlant.nama
            plantDAO.pathGambar = newPlant.pathGambar
            plantDAO.deskripsi = newPlant.deskripsi
            plantDAO.manfaat = newPlant.manfaat
            plantDAO.efekSamping = newPlant.efekSamping
            plantDAO.updatedAt = newPlant.updatedAt
            true
        } else {
            false
        }
    }

    override suspend fun removePlant(id: String): Boolean = suspendTransaction {
        val rowsDeleted = PlantTable.deleteWhere {
            PlantTable.id eq UUID.fromString(id)
        }
        rowsDeleted == 1
    }
}