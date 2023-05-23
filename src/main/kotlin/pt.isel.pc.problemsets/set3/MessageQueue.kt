package pt.isel.pc.problemsets.set3

import kotlin.time.Duration


class MessageQueue<T>(private val capacity: Int) {

    suspend fun enqueue(message:T) {
        TODO()
    }

    suspend fun dequeue(timeout: Duration): T {
        TODO()
    }
}