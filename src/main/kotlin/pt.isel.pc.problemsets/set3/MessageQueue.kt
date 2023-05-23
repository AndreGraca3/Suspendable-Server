package pt.isel.pc.problemsets.set3

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pt.isel.pc.problemsets.utils.NodeLinkedList
import java.util.concurrent.locks.ReentrantLock
import kotlin.time.Duration


class MessageQueue<T>(private val capacity: Int) {
    private val lock = ReentrantLock()

    init {
        require(capacity >= 0) { "Capacity must be greater than 0." }
    }

    private var items = mutableListOf<T>()

    private val producersQueue = NodeLinkedList<ProducersRequest>()
    private val consumersQueue = NodeLinkedList<ConsumersRequest>()

    private inner class ProducersRequest(
        val message: T,
        //val continuation: Continuation<Unit>,
        var isDone: Boolean = false
    )

    private inner class ConsumersRequest(
        var res: T? = null,
        //val continuation: Continuation<Unit>,
        var isDone: Boolean = false
    )

    suspend fun enqueue(message: T) {
        TODO()
    }

    suspend fun dequeue(timeout: Duration): T? {
        lock.lock()
        // fast path
        if (items.size >= 1 && consumersQueue.empty) {
            val res = items.removeAt(0)

            val head = producersQueue.headValue
            if (head != null) {
                items.add(head.message) // delegation effect
                producersQueue.pull()
                head.isDone = true
                //head.condition.signal()
            }
            lock.unlock()
            return res
        }

        // wait path

        val request = ConsumersRequest()
        val requestNode = consumersQueue.enqueue(request)
        val remainingNanos = timeout.inWholeNanoseconds
        while (true) {
            try {
                delay(remainingNanos)
            } catch (ex: InterruptedException) {
                if (request.isDone) { // delay of Interruption
                    Thread.currentThread().interrupt()
                    return request.res
                }
                quitWaitingInConsumersQueue(requestNode)
                throw ex
            }

            if (request.isDone) {
                return request.res
            }

            if (remainingNanos <= 0) {
                quitWaitingInConsumersQueue(requestNode)
                return request.res
            }
        }
    }

    private fun quitWaitingInProducersQueue(requestNode: NodeLinkedList.Node<ProducersRequest>) {
        producersQueue.remove(requestNode)
    }

    private fun quitWaitingInConsumersQueue(requestNode: NodeLinkedList.Node<ConsumersRequest>) {
        consumersQueue.remove(requestNode)
    }
}


fun main() {
    runBlocking {
        launch {
            delay(2000)
            println("[T${Thread.currentThread().id}]")
            println("rld!")
        }

        launch {
            delay(1000)
            println("[T${Thread.currentThread().id}]")
            launch {
                delay(1000)
                println("New Coroutine [T${Thread.currentThread().id}]")
                Thread.sleep(3000)
            }
            println(", wo")
        }

        println("[T${Thread.currentThread().id}]")
        println("Hello")
    }

    println("I'm blocked [T${Thread.currentThread().id}]")
}
