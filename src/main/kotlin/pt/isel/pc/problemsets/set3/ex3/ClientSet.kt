/*
package pt.isel.pc.problemsets.set3.ex3

import com.sun.security.ntlm.Client
import pt.isel.pc.problemsets.set3.ex2.MessageQueue
import java.util.concurrent.ConcurrentLinkedQueue

class ClientSet {
    private val clients = MessageQueue<RemoteClient>()
    private val topicSet = TopicSet()


    fun add(client: RemoteClient) {
        clients.add(client)
    }

    fun remove(client: RemoteClient) {
        clients.remove(client)
        topicSet.unsubscribe(client)
    }

    fun forEach(action: (RemoteClient) -> Unit) {
        clients.forEach(action)
    }

    fun isEmpty(): Boolean {
        return clients.isEmpty()
    }

    fun size(): Int {
        return clients.size
    }

    fun get(clientId: Int): RemoteClient? {
        return clients.find { client -> client.clientId == clientId.toString() }
    }

    fun notifySubscribers(message: PublishedMessage): Set<Subscriber> {
        val subscribers = topicSet.getSubscribersFor(message.topicName)
        subscribers.forEach {
            it.send(message)
        }
    }
}
*/
