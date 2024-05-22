package pt.isel.pc.problemsets.set3.ex3

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketAddress
import java.util.concurrent.LinkedBlockingQueue

/**
 * The server component.
 */
class Server private constructor(
    private val serverSocket: ServerSocket,
    private val controlQueue: LinkedBlockingQueue<ControlMessage>,
) {

    private val clientSet = mutableSetOf<RemoteClient>()
    private val topicSet = TopicSet()
    private var currentClientId = 0
    private var state = State.RUNNING
    private var acceptThreadEnded = false

    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        scope.launch {
            controlLoop()
        }
        scope.launch {
            acceptLoop()
        }
    }

    fun shutdown() {
        controlQueue.put(ControlMessage.Shutdowm)
    }

    fun publish(message: PublishedMessage) {
        controlQueue.put(ControlMessage.Publish(message))
    }

    fun subscribe(topicName: TopicName, subscriber: Subscriber) {
        controlQueue.put(ControlMessage.Subscribe(topicName, subscriber))
    }

    fun unsubscribe(topicName: TopicName, subscriber: Subscriber) {
        controlQueue.put(ControlMessage.Unsubscribe(topicName, subscriber))
    }

    fun remoteClientEnded(client: RemoteClient) {
        controlQueue.put(ControlMessage.RemoteClientEnded(client))
    }

    suspend fun join() {
        scope.coroutineContext[Job]?.join()
    }

    private suspend fun handleNewClientSocket(clientSocket: Socket) {
        if (state != State.RUNNING) {
            clientSocket.close()
            return
        }
        val newId = currentClientId++
        val remoteClient = RemoteClient.start(this, newId.toString(), clientSocket)
        clientSet.add(remoteClient)
        logger.info("Server: started new remote client")
    }

    private suspend fun handleRemoteClientEnded(remoteClient: RemoteClient) {
        clientSet.remove(remoteClient)
        topicSet.unsubscribe(remoteClient)
        logger.info("Server: remote client ended {}", remoteClient.clientId)
        if (state == State.SHUTTING_DOWN) {
            if (clientSet.isEmpty() && acceptThreadEnded) {
                state = State.SHUTDOWN
            }
        }
    }

    private suspend fun handlePublish(message: PublishedMessage) {
        topicSet.getSubscribersFor(message.topicName).forEach {
            it.send(message)
        }
    }

    private suspend fun handleSubscribe(topicName: TopicName, subscriber: Subscriber) {
        topicSet.subscribe(topicName, subscriber)
    }

    private suspend fun handleUnsubscribe(topicName: TopicName, subscriber: Subscriber) {
        topicSet.unsubscribe(topicName, subscriber)
    }

    private suspend fun handleShutdown() {
        if (state != State.RUNNING) {
            return
        }
        startShutdown()
    }

    private suspend fun startShutdown() {
        serverSocket.close()
        clientSet.forEach {
            it.shutdown()
        }
        state = State.SHUTTING_DOWN
    }

    private suspend fun handleAcceptLoopEnded() {
        acceptThreadEnded = true
        if (state != State.SHUTTING_DOWN) {
            logger.info("Accept loop ended unexpectedly")
            startShutdown()
        }
        if (clientSet.isEmpty()) {
            state = State.SHUTDOWN
        }
    }

    private suspend fun controlLoop() {
        try {
            while (state != State.SHUTDOWN) {
                try {
                    when (val controlMessage = withContext(Dispatchers.IO) { controlQueue.take() }) {
                        is ControlMessage.NewClientSocket -> handleNewClientSocket(controlMessage.clientSocket)
                        is ControlMessage.RemoteClientEnded -> handleRemoteClientEnded(controlMessage.remoteClient)
                        is ControlMessage.Publish -> handlePublish(controlMessage.message)
                        is ControlMessage.Subscribe -> handleSubscribe(controlMessage.topicName, controlMessage.subscriber)
                        is ControlMessage.Unsubscribe -> handleUnsubscribe(controlMessage.topicName, controlMessage.subscriber)
                        ControlMessage.Shutdowm -> handleShutdown()
                        ControlMessage.AcceptLoopEnded -> handleAcceptLoopEnded()
                    }
                } catch (ex: Throwable) {
                    logger.info("Unexpected exception, ignoring it", ex)
                }
            }
        } finally {
            logger.info("server ending")
        }
    }

    private suspend fun acceptLoop() {
        try {
            while (true) {
                val clientSocket = withContext(Dispatchers.IO) { serverSocket.accept() }
                logger.info("New client socket accepted")
                controlQueue.put(ControlMessage.NewClientSocket(clientSocket))
            }
        } catch (ex: Exception) {
            logger.info("Exception on accept loop: {}", ex.message)
            // continue
        } finally {
            controlQueue.put(ControlMessage.AcceptLoopEnded)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(Server::class.java)
        fun start(address: SocketAddress): Server {
            val serverSocket = ServerSocket()
            serverSocket.bind(address)
            val controlQueue = LinkedBlockingQueue<ControlMessage>()
            return Server(serverSocket, controlQueue)
        }
    }

    private sealed interface ControlMessage {
        data class NewClientSocket(val clientSocket: Socket) : ControlMessage
        data class RemoteClientEnded(val remoteClient: RemoteClient) : ControlMessage
        data class Publish(val message: PublishedMessage) : ControlMessage
        data class Subscribe(val topicName: TopicName, val subscriber: Subscriber) : ControlMessage
        data class Unsubscribe(val topicName: TopicName, val subscriber: Subscriber) : ControlMessage
        object Shutdowm : ControlMessage
        object AcceptLoopEnded : ControlMessage
    }

    private enum class State {
        RUNNING,
        SHUTTING_DOWN,
        SHUTDOWN,
    }
}
