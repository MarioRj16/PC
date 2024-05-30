package pt.isel.pc.problemsets.set3.ex3.app

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import pt.isel.pc.problemsets.set3.ex3.Server
import java.net.InetSocketAddress

fun main() {
    // start server

    val server = Server.start(InetSocketAddress("0.0.0.0", 23))
    logger.info("Started server")

    // register shutdown hook
    val shutdownThread = Thread {
        runBlocking {
            logger.info("Starting shutdown process")
            server.shutdown()
            server.join()
        }
    }
    Runtime.getRuntime().addShutdownHook(shutdownThread)
    runBlocking {
            // wait for server to end
            logger.info("Waiting for server to end")
            server.join()
            logger.info("main ending")
    }
}

private val logger = LoggerFactory.getLogger("App")


