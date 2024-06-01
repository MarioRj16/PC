
package pt.isel.pc.problemsets.set3.ex3

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


class ClientSet {
    private val clients = mutableSetOf<RemoteClient>()
    private val topicSet = TopicSet()
    private val lock = ReentrantLock()


    fun shutdown() {
        clients.forEach {
            it.shutdown()
        }
    }

    fun size(): Int {
        lock.withLock {
            return clients.size
        }
    }

    fun isEmpty(): Boolean{
        lock.withLock {
            return clients.size == 0
        }
    }

    fun addClient(client: RemoteClient) {
        lock.withLock {
            clients.add(client)
        }
    }

    fun notifySubscribers(message: PublishedMessage){
        lock.withLock {
            val subscribers = topicSet.getSubscribersFor(message.topicName)
            subscribers.forEach {
                it.send(message)
            }
        }
    }

    fun removeClient(remoteClient: RemoteClient) {
        lock.withLock {
            clients.remove(remoteClient)
            topicSet.unsubscribe(remoteClient)
        }
    }


    fun subscribe(topicName: TopicName, subscriber: Subscriber) {
        topicSet.subscribe(topicName, subscriber)
    }

    fun unsubscribe(topicName: TopicName, subscriber: Subscriber) {
        topicSet.unsubscribe(topicName, subscriber)
    }

    fun getNumberOfSubscribers(topic: TopicName): Int{
        return topicSet.getSubscribersFor(topic).size
    }
}

