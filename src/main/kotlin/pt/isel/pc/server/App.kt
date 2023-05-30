package pt.isel.pc.server

import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("main")

fun main() {
    Runtime.getRuntime().addShutdownHook(
        Thread { logger.info("bye") }
    )
}