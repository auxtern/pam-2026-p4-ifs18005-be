package org.delcom

import io.github.cdimascio.dotenv.dotenv
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import kotlinx.serialization.json.Json
import org.delcom.module.appModule
import org.delcom.helpers.configureDatabases
import org.delcom.helpers.configureStaticFiles
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main(args: Array<String>) {
    val dotenv = dotenv {
        directory = "."
        ignoreIfMissing = false
    }

    dotenv.entries().forEach {
        System.setProperty(it.key, it.value)
    }

    EngineMain.main(args)
}

fun Application.module() {
    install(CORS) {
        anyHost()

        // HTTP Methods
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Options)

        // Headers yang umum dikirim browser
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.Accept)
        allowHeader(HttpHeaders.Origin)
        allowHeader(HttpHeaders.AccessControlAllowOrigin)

        // Izinkan credentials (cookie/token) jika diperlukan
        allowCredentials = true

        // Izinkan browser membaca header response ini
        exposeHeader(HttpHeaders.ContentDisposition)
    }

    install(ContentNegotiation) {
        json(
            Json {
                explicitNulls = false
                prettyPrint = true
                ignoreUnknownKeys = true
            }
        )
    }

    install(Koin) {
        slf4jLogger()
        // Teruskan instance Application ke appModule agar bisa membaca baseUrl
        modules(appModule(this@module))
    }

    configureDatabases()
    configureStaticFiles()   // Daftarkan folder uploads/ sebagai file statis publik
    configureRouting()
}