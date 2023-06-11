package pt.isel.pc.server.room

import pt.isel.pc.server.client.ConnectedClient
import java.util.HashSet
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Represents a room, namely by containing all the clients in the room
 */
class Room(
    private val name: String,
) {

    private val lock = ReentrantLock()
    private val connectedClients = HashSet<ConnectedClient>()

    fun add(connectedClient: ConnectedClient) = lock.withLock {
        connectedClients.add(connectedClient)
    }

    fun remove(connectedClient: ConnectedClient) = lock.withLock {
        connectedClients.remove(connectedClient)
    }

    suspend fun post(sender: ConnectedClient, message: String) {
        lock.lock()
        connectedClients.forEach {
            if (it != sender) {
                it.send(sender, message)
            }
        }
        lock.unlock()
    }

    override fun toString() = name
}