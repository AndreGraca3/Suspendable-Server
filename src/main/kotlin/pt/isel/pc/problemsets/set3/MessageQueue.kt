package pt.isel.pc.problemsets.set3

import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration


class MessageQueue<T>(private val capacity: Int) {

    val producersSize get() = producers.size
    val consumersSize get() = consumers.size
    val itemsSize get() = items.size

    init {
        require(capacity >= 0) { "Capacity must be greater than 0 but was $capacity." }
    }

    private val lock = ReentrantLock()

    private var producers = mutableListOf<ProducerRequest>()
    private var consumers = mutableListOf<ConsumerRequest>()

    private var items = mutableListOf<T>()

    private inner class ProducerRequest(
        var item: T,
        var continuation: Continuation<Unit>? = null,
        var isDone: Boolean = false
    )

    private inner class ConsumerRequest(
        var res: T? = null,
        var continuation: Continuation<T>? = null,
        var isDone: Boolean = false
    )

    suspend fun enqueue(message: T) {
        lock.lock()

        // fast path
        if (consumers.isNotEmpty()) {
            val consumerReq = consumers.removeFirst()
            consumerReq.res = message
            consumerReq.continuation!!.resume(message)
            consumerReq.isDone = true
            lock.unlock()
            return
        }

        if (items.size + 1 <= capacity) {
            items.add(message)
            lock.unlock()
            return
        }

        // wait path
        val req = ProducerRequest(message)

        try {
            return suspendCancellableCoroutine { continuation ->
                req.continuation = continuation
                producers.add(req)
                lock.unlock()
            }
        } catch (e: CancellationException) {
            lock.withLock {
                if (!req.isDone) {
                    producers.remove(req)
                    throw e
                }
            }
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
        val req = ConsumerRequest()

        try {
            return withTimeout(timeout) {
                suspendCancellableCoroutine { continuation ->
                    req.continuation = continuation
                    consumers.add(req)
                    lock.unlock()
                }
            }
        } catch (e: CancellationException) { // note: TimeoutCancellationException is derivative of CancellationException
            lock.withLock {
                if (!req.isDone) {
                    consumers.remove(req)
                    throw e
                }
                return req.res!!
                // else succeed
                // NOTE: the coroutine is still cancelled; the caller
                //       is responsible for dealing with that situation
                //       (acquire returned with permits but the
                //        coroutine was cancelled simultaneously)
            }
        }
    }

    private fun consume(): T {
        if (producers.isNotEmpty()) {
            val producerReq = producers.removeFirst()
            items.add(producerReq.item)
            producerReq.isDone = true
            producerReq.continuation!!.resume(Unit)
        }
        return items.removeFirst()
    }
}