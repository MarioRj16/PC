package pt.isel.pc.problemsets.set3.ex3.protocol

/**
 * Sealed hierarchy to represent messages sent by the server that are not responses to requests.
 */
sealed interface ServerPush {
    data class PublishedMessage(val message: pt.isel.pc.problemsets.set3.ex3.PublishedMessage) : ServerPush
    data object Hi : ServerPush
    data object Bye : ServerPush
}
