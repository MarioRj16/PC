package pt.isel.pc.problemsets.set3.ex3

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import pt.isel.pc.problemsets.set3.ex2.MessageQueue
import pt.isel.pc.problemsets.set3.ex3.protocol.ClientRequest
import pt.isel.pc.problemsets.set3.ex3.protocol.ClientResponse
import pt.isel.pc.problemsets.set3.ex3.protocol.ServerPush
import pt.isel.pc.problemsets.set3.ex3.protocol.parseClientRequest
import pt.isel.pc.problemsets.set3.ex3.protocol.serialize
import pt.isel.pc.problemsets.set3.ex3.utils.LineReader
import pt.isel.pc.problemsets.set3.ex3.utils.SuccessOrError
import pt.isel.pc.problemsets.set3.ex3.utils.suspendWriteLn
import suspendRead
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel

/**
 * The component responsible to interact with a remote client, via a [Socket].
 */
class RemoteClient private constructor(
    private val server: Server,
    val clientId: String,
    private val clientSocket: AsynchronousSocketChannel,
    private val clients: ClientSet
) : Subscriber {
    private val controlQueue = MessageQueue<ControlMessage>()
    private var state = State.RUNNING

    private val scope = CoroutineScope(Dispatchers.Default)


    init {
        clients.addClient(this)
        scope.launch{
            controlLoop()
        }
        scope.launch {
            readLoop()
        }
    }

    fun shutdown() {
        controlQueue.enqueue(ControlMessage.Shutdown)
    }

    override fun send(message: PublishedMessage) {
        controlQueue.enqueue(ControlMessage.Message(message))
    }

    private suspend fun handleShutdown() {
        if (state != State.RUNNING) {
            return
        }

        clientSocket.suspendWriteLn(ByteBuffer.wrap(serialize(ServerPush.Bye).toByteArray()))
        clientSocket.close()
        state = State.SHUTTING_DOWN
    }

    private suspend fun handleMessage( message: PublishedMessage) {
        if (state != State.RUNNING) {
            return
        }

        clientSocket.suspendWriteLn(ByteBuffer.wrap(serialize(ServerPush.PublishedMessage(message)).toByteArray()))
    }

    private suspend fun handleClientSocketLine( line: String) {
        if (state != State.RUNNING) {
            return
        }
        val response = when (val res = parseClientRequest(line)) {
            is SuccessOrError.Success -> when (val request = res.value) {
                is ClientRequest.Publish -> {
                    clients.notifySubscribers(PublishedMessage(request.topic, request.message))
                    ClientResponse.OkPublish(clients.getNumberOfSubscribers(request.topic))
                }

                is ClientRequest.Subscribe -> {
                    request.topics.forEach {
                        clients.subscribe(it,this)
                    }
                    ClientResponse.OkSubscribe
                }

                is ClientRequest.Unsubscribe -> {
                    request.topics.forEach {
                        clients.unsubscribe(it,this)
                    }
                    ClientResponse.OkUnsubscribe
                }
            }

            is SuccessOrError.Error -> {
                ClientResponse.Error(res.error)
            }
        }
        clientSocket.suspendWriteLn(ByteBuffer.wrap(serialize(response).toByteArray()))
    }

    private fun handleClientSocketError(throwable: Throwable) {
        logger.info("Client socket operation thrown: {}", throwable.message)
    }

    private fun handleClientSocketEnded() {
        if (state != State.RUNNING) {
            return
        }
        state = State.SHUTTING_DOWN
    }

    private fun handleReadLoopEnded() {
        state = State.SHUTDOWN
    }

    private suspend fun controlLoop() {
        try {
            clientSocket.suspendWriteLn(ByteBuffer.wrap(serialize(ServerPush.Hi).toByteArray()))
            while (state != State.SHUTDOWN) {
                val controlMessage = controlQueue.dequeue()
                logger.info("[{}] main thread received {}", clientId, controlMessage)
                when (controlMessage) {
                    ControlMessage.Shutdown -> handleShutdown()
                    is ControlMessage.Message -> handleMessage(controlMessage.value)
                    is ControlMessage.ClientSocketLine -> handleClientSocketLine(controlMessage.value)
                    ControlMessage.ClientSocketEnded -> handleClientSocketEnded()
                    is ControlMessage.ClientSocketError -> handleClientSocketError(controlMessage.throwable)
                    ControlMessage.ReadLoopEnded -> handleReadLoopEnded()
                }
            }

        } finally {
            logger.info("[{}] remote client ending", clientId)
            clients.removeClient(this)
            server.remoteClientEnded(this)
        }
    }

    private suspend fun readLoop() {
        try {
            val reader = LineReader{ clientSocket.suspendRead(it) }
            while (true) {
                val line = reader.readLine()
                if (line == null) {
                    logger.info("[{}] end of input stream reached", clientId)
                    controlQueue.enqueue(ControlMessage.ClientSocketEnded)
                    return
                }
                logger.info("[{}] line received: {}", clientId, line)
                controlQueue.enqueue(ControlMessage.ClientSocketLine(line))
            }
        } catch (ex: Throwable) {
            logger.info("[{}]Exception on read loop: {}, {}", clientId, ex.javaClass.name, ex.message)
            controlQueue.enqueue(ControlMessage.ClientSocketError(ex))
        } finally {
            logger.info("[{}] client loop ending", clientId)
            controlQueue.enqueue(ControlMessage.ReadLoopEnded)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RemoteClient::class.java)
        fun start(server: Server, clientId: String, socket: AsynchronousSocketChannel, clients: ClientSet): RemoteClient {
            return RemoteClient(
                server,
                clientId,
                socket,
                clients
            )
        }
    }

    private sealed interface ControlMessage {
        data class Message(val value: PublishedMessage) : ControlMessage
        object Shutdown : ControlMessage
        object ClientSocketEnded : ControlMessage
        data class ClientSocketError(val throwable: Throwable) : ControlMessage
        data class ClientSocketLine(val value: String) : ControlMessage
        object ReadLoopEnded : ControlMessage
    }

    private enum class State {
        RUNNING,
        SHUTTING_DOWN,
        SHUTDOWN,
    }
}
