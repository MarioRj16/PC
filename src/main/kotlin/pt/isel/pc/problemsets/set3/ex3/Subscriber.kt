package pt.isel.pc.problemsets.set3.ex3

/**
 * A subscriber of messages sent to topics.
 */
interface Subscriber {
    fun send(message: PublishedMessage)
}
