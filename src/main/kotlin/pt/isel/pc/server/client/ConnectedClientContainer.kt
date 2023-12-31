package pt.isel.pc.server.client

import pt.isel.pc.server.messages.Messages
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Container of all the active clients
 */
class ConnectedClientContainer {

    private val lock = ReentrantLock()
    private val clients = HashSet<ConnectedClient>()
    private var isShuttingDown: Boolean = false

    fun add(connectedClient: ConnectedClient) = lock.withLock {
        if (isShuttingDown) {
            throw IllegalStateException("Shutting down")
        }
        clients.add(connectedClient)
    }

    fun remove(connectedClient: ConnectedClient) = lock.withLock {
        clients.remove(connectedClient)
    }

    suspend fun warnShutdown(timeout: Long) {
        lock.lock()
        clients.toList().forEach {
            it.sendSystemMessage(Messages.serverEndingSoon(timeout))
        }
        lock.unlock()
    }

    suspend fun shutdown() {
        val clientList = lock.withLock {
            isShuttingDown = true
            clients.toList()
        }
        clientList.forEach {
            it.shutdown()
        }
        clientList.forEach {
            it.join()
        }
    }
}