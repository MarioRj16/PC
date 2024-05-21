package pt.isel.pc.problemsets.set3.ex3.protocol

import pt.isel.pc.problemsets.set3.ex3.TopicName

/**
 * Sealed hierarchy (i.e. union type) to represent client requests.
 */
sealed interface ClientRequest {
    data class Publish(val topic: TopicName, val message: String) : ClientRequest
    data class Subscribe(val topics: List<TopicName>) : ClientRequest
    data class Unsubscribe(val topics: List<TopicName>) : ClientRequest
}
