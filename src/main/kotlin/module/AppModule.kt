package org.delcom.module

import io.ktor.server.application.*
import org.delcom.repositories.IPlantRepository
import org.delcom.repositories.PlantRepository
import org.delcom.services.PlantService
import org.delcom.services.ProfileService
import org.koin.dsl.module

fun appModule(application: Application) = module {
    val baseUrl = application.environment.config
        .property("ktor.app.baseUrl")
        .getString()
        .trimEnd('/')   // Pastikan tidak ada trailing slash

    // Plant Repository (menerima baseUrl untuk membangun URL gambar)
    single<IPlantRepository> {
        PlantRepository(baseUrl)
    }

    // Plant Service
    single {
        PlantService(get())
    }

    // Profile Service (menerima baseUrl untuk membangun URL foto profil)
    single {
        ProfileService(baseUrl)
    }
}