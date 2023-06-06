package pt.isel.pc.baseServer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import pt.isel.pc.problemsets.set3.MessageQueue
import pt.isel.pc.utils.BufferedSocketChannel
import java.io.BufferedWriter
import java.net.Socket
import java.nio.channels.AsynchronousSocketChannel
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Responsible for handling a single connected client. It is comprised by two threads:
 * - `readLoopThread` - responsible for (blocking) reading lines from the client socket. It is the only thread that
 *    reads from the client socket.
 * - `mainLoopThread` - responsible for handling control messages sent from the outside
 *    or from the inner `readLoopThread`. It is the only thread that writes to the client socket.
 */
class ConnectedClient(
    private val socket: AsynchronousSocketChannel,
    id: Int,
    private val roomContainer: RoomContainer,
    private val clientContainer: ConnectedClientContainer,
    private val scope: CoroutineScope
    ) {

    val name = "client-$id"

    // The control messages the main loop handles...
    private sealed interface ControlMessage {
        // ... a message sent by a room
        data class RoomMessage(val sender: ConnectedClient, val message: String) : ControlMessage

        // ... a line sent by the connected remote client
        data class RemoteClientRequest(val request: ClientRequest) : ControlMessage

        // ... the connected remote client closes the socket (local receive)
        object RemoteInputClosed : ControlMessage

        // ... the shutdown method was called
        object Shutdown : ControlMessage
    }

    suspend fun send(sender: ConnectedClient, message: String) {
        // just add a control message into the control queue
        controlQueue.enqueue(ControlMessage.RoomMessage(sender, message))
    }

    suspend fun shutdown() {
        // just add a control message into the control queue
        controlQueue.enqueue(ControlMessage.Shutdown)
    }

    suspend fun join() = mainLoopThread.join()

    private val controlQueue = MessageQueue<ControlMessage>(Int.MAX_VALUE)
    private val readLoopThread = scope.launch { readLoop() }
    private val mainLoopThread = scope.launch { mainLoop() }

    private var room: Room? = null

    private suspend fun mainLoop() {
        logger.info("[{}] main loop started", name)
        socket.use {
            val buffer = BufferedSocketChannel(it)
                buffer.writeLine(Messages.CLIENT_WELCOME)
                while (true) {
                    when (val control = controlQueue.dequeue(Long.MAX_VALUE.toDuration(DurationUnit.SECONDS))) {
                        is ControlMessage.Shutdown -> {
                            logger.info("[{}] received control message: {}", name, control)
                            buffer.writeLine(Messages.SERVER_IS_ENDING)
                            readLoopThread.interrupt()
                            break
                        }

                        is ControlMessage.RoomMessage -> {
                            logger.trace("[{}] received control message: {}", name, control)
                            buffer.writeLine(Messages.messageFromClient(control.sender.name, control.message))
                        }

                        is ControlMessage.RemoteClientRequest -> {
                            val line = control.request
                            if (handleRemoteClientRequest(line, writer)) break
                        }

                        ControlMessage.RemoteInputClosed -> {
                            logger.info("[{}] received control message: {}", name, control)
                            break
                        }
                    }
                }
            }
        }
        readLoopThread.join()
        clientContainer.remove(this)
        logger.info("[{}] main loop ending", name)


    private fun handleRemoteClientRequest(
        clientRequest: ClientRequest,
        writer: BufferedWriter,
    ): Boolean {
        when (clientRequest) {
            is ClientRequest.EnterRoomCommand -> {
                logger.info("[{}] received remote client request: {}", name, clientRequest)
                room?.remove(this)
                room = roomContainer.getByName(clientRequest.name).also {
                    it.add(this)
                }
                writer.writeLine(Messages.enteredRoom(clientRequest.name))
            }

            ClientRequest.LeaveRoomCommand -> {
                logger.info("[{}] received remote client request: {}", name, clientRequest)
                room?.remove(this)
                room = null
            }

            ClientRequest.ExitCommand -> {
                logger.info("[{}] received remote client request: {}", name, clientRequest)
                room?.remove(this)
                writer.writeLine(Messages.BYE)
                readLoopThread.interrupt()
                return true
            }

            is ClientRequest.InvalidRequest -> {
                logger.info("[{}] received remote client request: {}", name, clientRequest)
                writer.writeLine(Messages.ERR_INVALID_LINE)
            }

            is ClientRequest.Message -> {
                logger.trace("[{}] received remote client request: {}", name, clientRequest)
                val currentRoom = room
                if (currentRoom != null) {
                    currentRoom.post(this, clientRequest.value)
                } else {
                    writer.writeLine(Messages.ERR_NOT_IN_A_ROOM)
                }
            }
        }
        return false
    }

    private suspend fun readLoop() {
        socket.getInputStream().bufferedReader().use { reader ->
            try {
                while (true) {
                    val line: String? = reader.readLine()
                    if (line == null) {
                        logger.info("[{}] end of input stream reached", name)
                        controlQueue.enqueue(ControlMessage.RemoteInputClosed)
                        return
                    }
                    controlQueue.enqueue(ControlMessage.RemoteClientRequest(ClientRequest.parse(line)))
                }
            } catch (ex: Throwable) {
                logger.info("[{}]Exception on read loop: {}, {}", name, ex.javaClass.name, ex.message)
                // clear interrupt flag
                Thread.interrupted()
                controlQueue.enqueue(ControlMessage.RemoteInputClosed)
            }
            logger.info("[{}] client loop ending", name)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ConnectedClient::class.java)

        private fun BufferedWriter.writeLine(msg: String) {
            this.write(msg)
            this.newLine()
            this.flush()
        }
    }
}