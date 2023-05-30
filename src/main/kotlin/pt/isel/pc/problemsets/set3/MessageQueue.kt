package pt.isel.pc.problemsets.set3

import kotlinx.coroutines.*
import java.util.concurrent.TimeoutException
import java.util.concurrent.locks.ReentrantLock
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.jvm.Throws
import kotlin.time.Duration


class MessageQueue<T>(private val capacity: Int) {

    init {
        require(capacity >= 0) { "Capacity must be greater than 0 but was $capacity." }
    }

    private val lock = ReentrantLock()

    private var producers = mutableListOf<ProducerRequest>()
    private var consumers = mutableListOf<ConsumerRequest>()

    private var items = mutableListOf<T>()

    private inner class ProducerRequest(
        var item: T,
        val continuation: Continuation<Unit>
    )

    private inner class ConsumerRequest(
        var continuation: Continuation<T>?
    )

    suspend fun enqueue(message: T) {
        lock.lock()

        // fast path
        if (consumers.isNotEmpty()) {
            val consumerReq = consumers.removeFirst()
            consumerReq.continuation!!.resume(message)
            lock.unlock()
            return
        }

        if (items.size + 1 <= capacity) {
            items.add(message)
            lock.unlock()
            return
        }

        // wait path
        return suspendCancellableCoroutine { continuation ->
            val request = ProducerRequest(message, continuation)
            producers.add(request)
            lock.unlock()
        }
    }

    @Throws(TimeoutException::class)
    suspend fun dequeue(timeout: Duration): T {
        lock.lock()

        // fast path
        if (consumers.isEmpty() && items.isNotEmpty()) {
            val res = consume()
            lock.unlock()
            return res
        }

        // wait path
        val req = ConsumerRequest(null)

        try {
            return withTimeout(timeout) {
                suspendCancellableCoroutine { continuation ->
                    req.continuation = continuation
                    consumers.add(req)
                    lock.unlock()
                }
            }
        } catch (e: TimeoutCancellationException) {
            consumers.remove(req)
            throw TimeoutException(e.message)
        }
    }

    private fun consume(): T {
        if (producers.isNotEmpty()) {
            val producerReq = producers.removeFirst()
            items.add(producerReq.item)
            producerReq.continuation.resume(Unit)
        }
        return items.removeFirst()
    }
}