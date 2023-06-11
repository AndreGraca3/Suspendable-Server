package pt.isel.pc.problemsets.set3

import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Accepts server connections in Server Socket Channel and
 * creates a Socket Channel for the current connection.
 */

suspend fun AsynchronousServerSocketChannel.suspendingAccept(): AsynchronousSocketChannel {
    return suspendCancellableCoroutine { continuation ->
        accept(null, object : CompletionHandler<AsynchronousSocketChannel, Any?> {
            override fun completed(sessionSocket: AsynchronousSocketChannel, attachment: Any?) {
                continuation.resume(sessionSocket)
            }

            override fun failed(exc: Throwable, attachment: Any?) {
                continuation.resumeWithException(exc)
            }
        })
    }
}

suspend fun AsynchronousSocketChannel.suspendingWrite(buffer: ByteBuffer): Int {
    return suspendCancellableCoroutine { continuation ->

        write(buffer, null, object : CompletionHandler<Int, Any?> {
            override fun completed(result: Int, attachment: Any?) {
                continuation.resume(result)
            }

            override fun failed(exc: Throwable, attachment: Any?) {
                continuation.resumeWithException(exc)
            }
        })
    }
}

suspend fun AsynchronousSocketChannel.suspendingRead(buffer: ByteBuffer): Int {
    return suspendCancellableCoroutine { continuation ->

        read(buffer, null, object : CompletionHandler<Int, Any?>{
            override fun completed(result: Int, attachment: Any?) {
                continuation.resume(result)
            }

            override fun failed(exc: Throwable, attachment: Any?) {
                continuation.resumeWithException(exc)
            }

        })

    }
}