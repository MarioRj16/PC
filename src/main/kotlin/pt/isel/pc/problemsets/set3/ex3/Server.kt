package pt.isel.pc.problemsets.set3.ex3

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import pt.isel.pc.problemsets.set3.ex2.MessageQueue
import suspendAccept
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketAddress
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.util.concurrent.LinkedBlockingQueue

/**
 * The server component.
 */
class Server private constructor(
    private val serverSocket: AsynchronousServerSocketChannel,
    private val controlQueue: MessageQueue<ControlMessage>,
) {
    private val limitClients = 4
    private val clientSet = ClientSet()
    private var currentClientId = 0
    private var state = State.RUNNING
    private var acceptCoroutineEnded = false

    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        scope.launch {
            controlLoop()
        }
        scope.launch {
            acceptLoop()
        }
    }

    fun shutdown() {
        controlQueue.enqueue(ControlMessage.Shutdowm)
    }

    fun remoteClientEnded(client: RemoteClient) {
        controlQueue.enqueue(ControlMessage.RemoteClientEnded(client))
    }

    suspend fun join() {
        scope.coroutineContext[Job]?.join()
    }

    private fun handleNewClientSocket(clientSocket: AsynchronousSocketChannel) {
        if (state != State.RUNNING) {
            clientSocket.close()
            return
        }
        if (clientSet.size() >= limitClients) {
            logger.info("Server: too many clients, closing new client")
            clientSocket.close()
            return
        }
        val newId = currentClientId++
        RemoteClient.start(this, newId.toString(), clientSocket, clientSet)
        logger.info("Server: started new remote client")
    }

    private fun handleRemoteClientEnded(remoteClient: RemoteClient) {
        logger.info("Server: remote client ended {}", remoteClient.clientId)
        if (state == State.SHUTTING_DOWN) {
            if (clientSet.isEmpty() && acceptCoroutineEnded) {
                state = State.SHUTDOWN
            }
        }
    }


    private fun handleShutdown() {
        if (state != State.RUNNING) {
            return
        }
        startShutdown()
    }

    private fun startShutdown() {
        serverSocket.close()
        clientSet.shutdown()
        state = State.SHUTTING_DOWN
    }

    private fun handleAcceptLoopEnded() {
        acceptCoroutineEnded = true
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
                    supervisorScope {
                        when (val controlMessage = controlQueue.dequeue()) {
                            is ControlMessage.NewClientSocket -> handleNewClientSocket(controlMessage.clientSocket)
                            is ControlMessage.RemoteClientEnded -> handleRemoteClientEnded(controlMessage.remoteClient)

                            ControlMessage.Shutdowm -> handleShutdown()
                            ControlMessage.AcceptLoopEnded -> handleAcceptLoopEnded()
                        }
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
                val clientSocket = serverSocket.suspendAccept()
                logger.info("New client socket accepted")
                controlQueue.enqueue(ControlMessage.NewClientSocket(clientSocket))
            }
        } catch (ex: Exception) {
            logger.info(" Exception on accept loop: {}", ex.message)
        } finally {
            controlQueue.enqueue(ControlMessage.AcceptLoopEnded)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(Server::class.java)
        fun start(address: SocketAddress): Server {
            val serverSocket = AsynchronousServerSocketChannel.open()
            serverSocket.bind(address)
            val controlQueue = MessageQueue<ControlMessage>()
            return Server(serverSocket, controlQueue)
        }
    }

    private sealed interface ControlMessage {
        data class NewClientSocket(val clientSocket: AsynchronousSocketChannel) : ControlMessage
        data class RemoteClientEnded(val remoteClient: RemoteClient) : ControlMessage
        object Shutdowm : ControlMessage
        object AcceptLoopEnded : ControlMessage
    }

    private enum class State {
        RUNNING,
        SHUTTING_DOWN,
        SHUTDOWN,
    }
}
