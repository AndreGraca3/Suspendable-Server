package pt.isel.pc.server

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import pt.isel.pc.utils.extractTimeout
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import pt.isel.pc.server.client.ConnectedClient

private val logger = LoggerFactory.getLogger("main")

/**
 * Entry point for the application
 * See [Server] and [ConnectedClient] for a high-level view of the architecture.
 */
fun main() {
    runBlocking(context = SupervisorJob()) {
        logger.info("main started")
        // By default, we listen on port 8080 of all interfaces
        val server = Server("0.0.0.0", 8080, this)

        // Shutdown hook to handle SIG_TERM signals (gracious shutdown)
        Runtime.getRuntime().addShutdownHook(
            Thread {
                this.launch {
                    logger.info("shutdown hook started")
                    server.shutdown()
                    logger.info("waiting for server to end")
                    server.join()
                    logger.info("server ended")
                }
            }
        )

        launch(Dispatchers.IO) {
            while (true) {
                val input = readln()
                when {
                    input == "/exit" -> {
                        server.shutdown()
                        break
                    }

                    input.startsWith("/shutdown") -> {
                        val timeout = extractTimeout(input)
                        if (timeout == null) {
                            println("Invalid usage. /shutdown [timeout]")
                            continue
                        }
                        server.shutdownWithTimeout(timeout.toDuration(DurationUnit.SECONDS))
                        break
                    }
                }
            }
        }

        server.join()
        logger.info("server ending")
    }
}