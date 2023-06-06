package pt.isel.pc.baseServer

import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("main")

/**
 * Entry point for the application
 * See [Server] and [ConnectedClient] for an high-level view of the architecture.
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
        server.join()
        logger.info("server ending")
    }
}