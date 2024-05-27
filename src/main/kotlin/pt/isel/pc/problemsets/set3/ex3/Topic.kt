package pt.isel.pc.problemsets.set3.ex3

/**
 * Represents a topic and the subscribers to those topics.
 *
 */
class Topic(
    val name: TopicName,
) {
    val subscribers = mutableSetOf<Subscriber>()
}
