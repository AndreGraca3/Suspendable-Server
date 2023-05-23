package pt.isel.pc.problemsets.set3

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration


class MessageQueue<T>(private val capacity: Int) {

    suspend fun enqueue(message: T) {
        TODO()
    }

    suspend fun dequeue(timeout: Duration): T {
        TODO()
    }
}


fun main() = runBlocking {
    launch {
        delay(8000)
        println(", world!")
    }

    print("Hello")
}