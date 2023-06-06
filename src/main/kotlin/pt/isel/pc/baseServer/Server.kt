package pt.isel.pc.baseServer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import pt.isel.pc.problemsets.set3.suspendingAccept
import java.net.InetSocketAddress
import java.net.SocketException
import java.nio.channels.AsynchronousServerSocketChannel
import java.util.concurrent.CountDownLatch

/**
 * Represents a server to which clients can connect, enter and leave rooms, and send messages.
 */
class Server(
    private val listeningAddress: String,
    private val listeningPort: Int,
    private val scope: CoroutineScope
) : AutoCloseable {

    private val serverSocket: AsynchronousServerSocketChannel = AsynchronousServerSocketChannel.open()
    private val isListening = CountDownLatch(1)

    /**
     * The listening thread is mainly comprised by loop waiting for connections and creating a [ConnectedClient]
     * for each accepted connection.
     */
    private val listeningThread = scope.launch {
        serverSocket.use { serverSocket ->
            serverSocket.bind(InetSocketAddress(listeningAddress, listeningPort))
            logger.info("server socket bound to ({}:{})", listeningAddress, listeningPort)
            println(Messages.SERVER_IS_BOUND)
            isListening.countDown()
            acceptLoop(serverSocket)
        }
    }

    fun waitUntilListening() = isListening.await()

    fun shutdown() {
        // Currently, the only way to unblock the listening thread from the listen method is by closing
        // the server socket.
        logger.info("closing server socket as a way to 'interrupt' the listening thread")
        serverSocket.close()
    }

    suspend fun join() = listeningThread.join()

    override fun close() {
        shutdown()
        scope.launch { join() }
    }

    private suspend fun acceptLoop(serverSocket: AsynchronousServerSocketChannel) {
        var clientId = 0
        val roomContainer = RoomContainer()
        val clientContainer = ConnectedClientContainer()
        while (true) {
            try {
                logger.info("accepting new client")
                val socket = serverSocket.suspendingAccept()
                logger.info("client socket accepted, remote address is {}", socket.remoteAddress)
                println(Messages.SERVER_ACCEPTED_CLIENT)
                val client = ConnectedClient(socket, ++clientId, roomContainer, clientContainer, scope)
                clientContainer.add(client)
            } catch (ex: SocketException) {
                logger.info("SocketException, ending")
                // We assume that an exception means the server was asked to terminate
                break
            }
        }
        clientContainer.shutdown()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(Server::class.java)
    }
}