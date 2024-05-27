package pt.isel.pc.problemsets.set3.ex3

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import pt.isel.pc.problemsets.set3.ex3.protocol.ClientRequest
import pt.isel.pc.problemsets.set3.ex3.protocol.ClientResponse
import pt.isel.pc.problemsets.set3.ex3.protocol.ServerPush
import pt.isel.pc.problemsets.set3.ex3.protocol.parseClientRequest
import pt.isel.pc.problemsets.set3.ex3.protocol.serialize
import pt.isel.pc.problemsets.set3.ex3.utils.SuccessOrError
import pt.isel.pc.problemsets.set3.ex3.utils.sendLine
import java.io.BufferedWriter
import java.io.Writer
import java.net.Socket
import java.util.concurrent.LinkedBlockingQueue

/**
 * The component responsible to interact with a remote client, via a [Socket].
 */
class RemoteClient private constructor(
    private val server: Server,
    val clientId: String,
    private val clientSocket: Socket,
) : Subscriber {
    private val controlQueue = LinkedBlockingQueue<ControlMessage>()
    private var state = State.RUNNING

    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        scope.launch {
            controlLoop()
        }
        scope.launch {
            readLoop()
        }
    }

    fun shutdown() {
        controlQueue.put(ControlMessage.Shutdown)
    }

    override fun send(message: PublishedMessage) {
        controlQueue.put(ControlMessage.Message(message))
    }

    private suspend fun handleShutdown(writer: Writer) {
        if (state != State.RUNNING) {
            return
        }
        writer.sendLine(serialize(ServerPush.Bye))
        clientSocket.close()
        state = State.SHUTTING_DOWN
    }

    private suspend fun handleMessage(writer: BufferedWriter, message: PublishedMessage) {
        if (state != State.RUNNING) {
            return
        }
        writer.sendLine(serialize(ServerPush.PublishedMessage(message)))
    }

    private suspend fun handleClientSocketLine(writer: BufferedWriter, line: String) {
        if (state != State.RUNNING) {
            return
        }
        val response = when (val res = parseClientRequest(line)) {
            is SuccessOrError.Success -> when (val request = res.value) {
                is ClientRequest.Publish -> {
                    server.publish(PublishedMessage(request.topic, request.message))
                    ClientResponse.OkPublish(server.getNumberOfSubscribers(request.topic))
                }

                is ClientRequest.Subscribe -> {
                    request.topics.forEach {
                        server.subscribe(it, this)
                    }
                    ClientResponse.OkSubscribe
                }

                is ClientRequest.Unsubscribe -> {
                    request.topics.forEach {
                        server.unsubscribe(it, this)
                    }
                    ClientResponse.OkUnsubscribe
                }
            }

            is SuccessOrError.Error -> {
                ClientResponse.Error(res.error)
            }
        }
        writer.sendLine(serialize(response))
    }

    private suspend fun handleClientSocketError(throwable: Throwable) {
        logger.info("Client socket operation thrown: {}", throwable.message)
    }

    private suspend fun handleClientSocketEnded() {
        if (state != State.RUNNING) {
            return
        }
        state = State.SHUTTING_DOWN
    }

    private suspend fun handleReadLoopEnded() {
        state = State.SHUTDOWN
    }

    private suspend fun controlLoop() {
        try {
            clientSocket.getOutputStream().bufferedWriter().use { writer ->
                writer.sendLine(serialize(ServerPush.Hi))
                while (state != State.SHUTDOWN) {
                    val controlMessage = withContext(Dispatchers.IO) { controlQueue.take() }
                    logger.info("[{}] main thread received {}", clientId, controlMessage)
                    when (controlMessage) {
                        ControlMessage.Shutdown -> handleShutdown(writer)
                        is ControlMessage.Message -> handleMessage(writer, controlMessage.value)
                        is ControlMessage.ClientSocketLine -> handleClientSocketLine(writer, controlMessage.value)
                        ControlMessage.ClientSocketEnded -> handleClientSocketEnded()
                        is ControlMessage.ClientSocketError -> handleClientSocketError(controlMessage.throwable)
                        ControlMessage.ReadLoopEnded -> handleReadLoopEnded()
                    }
                }
            }
        } finally {
            logger.info("[{}] remote client ending", clientId)
            server.remoteClientEnded(this)
        }
    }

    private suspend fun readLoop() {
        clientSocket.getInputStream().bufferedReader().use { reader ->
            try {
                while (true) {
                    val line: String? = withContext(Dispatchers.IO) { reader.readLine() }
                    if (line == null) {
                        logger.info("[{}] end of input stream reached", clientId)
                        controlQueue.put(ControlMessage.ClientSocketEnded)
                        return
                    }
                    logger.info("[{}] line received: {}", clientId, line)
                    controlQueue.put(ControlMessage.ClientSocketLine(line))
                }
            } catch (ex: Throwable) {
                logger.info("[{}]Exception on read loop: {}, {}", clientId, ex.javaClass.name, ex.message)
                controlQueue.put(ControlMessage.ClientSocketError(ex))
            } finally {
                logger.info("[{}] client loop ending", clientId)
                controlQueue.put(ControlMessage.ReadLoopEnded)
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RemoteClient::class.java)
        fun start(server: Server, clientId: String, socket: Socket): RemoteClient {
            return RemoteClient(
                server,
                clientId,
                socket,
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
