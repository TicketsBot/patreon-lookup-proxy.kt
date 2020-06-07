package net.ticketsbot.patreonproxy.http

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import net.ticketsbot.patreonproxy.config.JSONConfiguration
import net.ticketsbot.patreonproxy.patreon.PatronManager
import java.util.*

class Server(val config: JSONConfiguration) : Runnable {

    override fun run() {
        val host = config.getGeneric("server.host", "0.0.0.0")
        val port = config.getGeneric("server.port", 80)

        val server = embeddedServer(Netty, port, host, module = getModule(config))
        server.start(false)
    }
}

private fun getModule(config: JSONConfiguration): Application.() -> Unit {
    return fun Application.() {
        install(DefaultHeaders)
        install(ContentNegotiation) {
            jackson()
        }

        install(Routing) {
            get("/ping") {
                call.respond(HttpStatusCode.OK, mapOf("success" to true))
            }

            get("/ispremium") {
                val key = call.request.queryParameters["key"]

                // Default to a difficult to guess PWD in case of error
                if(key != config.getGeneric("server.key", UUID.randomUUID().toString())) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Invalid secret key"))
                    return@get
                }

                val id = call.request.queryParameters["id"]

                val tier = PatronManager.patrons[id]
                val isPremium = tier != null

                val res = mutableMapOf<String, Any>("premium" to isPremium)

                if(tier != null) {
                    res["tier"] = tier.tierId
                }

                call.respond(HttpStatusCode.OK, res)
            }
        }
    }
}

