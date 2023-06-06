package pt.isel.pc.problemsets.set3

import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.nio.charset.CharsetDecoder
import java.nio.charset.CharsetEncoder
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private val encoder: CharsetEncoder = Charsets.UTF_8.newEncoder()
private val decoder: CharsetDecoder = Charsets.UTF_8.newDecoder()

/**
 * Accepts server connections in Server Socket Channel and
 * creates a Socket Channel for the current connection.
 */

suspend fun AsynchronousServerSocketChannel.suspendingAccept(): AsynchronousSocketChannel {
    return suspendCancellableCoroutine { continuation ->
        accept(null, object : CompletionHandler<AsynchronousSocketChannel, Any?> {
            override fun completed(sessionSocket: AsynchronousSocketChannel, attachment: Any?) {
                println("suspendingAccept success!")
                continuation.resume(sessionSocket)
            }

            override fun failed(exc: Throwable, attachment: Any?) {
                println("suspendingAccept failed!")
                continuation.resumeWithException(exc)
            }
        })
    }
}

suspend fun AsynchronousSocketChannel.suspendingWrite(buffer: ByteBuffer): Int {
    return suspendCancellableCoroutine { continuation ->

        write(encoder.encode(buffer.asCharBuffer()), null, object : CompletionHandler<Int, Any?> {
            override fun completed(result: Int, attachment: Any?) {
                println("suspendingWrite success!")
                continuation.resume(result)
            }

            override fun failed(exc: Throwable, attachment: Any?) {
                println("suspendingWrite failed!")
                continuation.resumeWithException(exc)
            }
        })
    }
}

suspend fun AsynchronousSocketChannel.suspendRead(buffer: ByteBuffer): Int {
    return suspendCancellableCoroutine { continuation ->

        read(buffer, null, object : CompletionHandler<Int, Any?>{
            override fun completed(result: Int, attachment: Any?) {
                println("suspendRead success!")
                continuation.resume(result)
            }

            override fun failed(exc: Throwable, attachment: Any?) {
                println("suspendRead failed!")
                continuation.resumeWithException(exc)
            }

        })

    }
}