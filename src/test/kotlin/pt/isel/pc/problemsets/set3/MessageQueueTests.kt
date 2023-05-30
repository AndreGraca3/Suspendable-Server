package pt.isel.pc.problemsets.set3

import kotlinx.coroutines.*
import java.util.concurrent.TimeoutException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.DurationUnit
import kotlin.time.toDuration


class MessageQueueTests<T>() {

    private val N = 5
    private val tid get() = Thread.currentThread().id

    @Test
    fun `1 producer and 1 consumer`() {
        val queue = MessageQueue<String>(N)
        runBlocking {
            launch {
                delay(2000)
                queue.enqueue("ISEL")
            }

            launch {
                val res = queue.dequeue(5.toDuration(DurationUnit.SECONDS))
                assertEquals("ISEL", res)
            }
        }
    }

    @Test
    fun `1 producer and 1 consumer with timeout`() {
        val queue = MessageQueue<String>(N)
        runBlocking {
            launch {
                delay(3000)
                queue.enqueue("ISEL")
            }

            launch {
                assertFailsWith<TimeoutException> {
                    val res = queue.dequeue(2.toDuration(DurationUnit.SECONDS))
                }
            }
        }
    }
}