package org.delcom.repositories

import org.delcom.entities.Plant

interface IPlantRepository {
    suspend fun getPlants(search: String): List<Plant>
    suspend fun getPlantById(id: String): Plant?
    suspend fun getPlantByName(name: String): Plant?
    suspend fun addPlant(plant: Plant): String
    suspend fun updatePlant(id: String, newPlant: Plant): Boolean
    suspend fun removePlant(id: String): Boolean
}