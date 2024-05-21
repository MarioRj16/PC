package pt.isel.pc.problemsets.set3.ex3


/**
 * Represents a message published to a topic.
 */
data class PublishedMessage(
    val topicName: TopicName,
    val content: String,
)
