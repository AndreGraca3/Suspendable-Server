package pt.isel.pc.server.room

import java.util.concurrent.ConcurrentHashMap

/**
 * Represents a container of rooms, organized by room name.
 */
class RoomContainer {

    private val roomMap = ConcurrentHashMap<String, Room>()

    fun getByName(name: String): Room = roomMap.computeIfAbsent(name) { Room(name) }
}