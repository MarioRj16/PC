package pt.isel.pc.problemsets.set3.ex3.protocol

/**
 * Sealed hierarchy to represent success or error responses to client requests
 */
sealed interface ClientResponse {
    data class OkPublish(val numberSubs: Int) : ClientResponse
    data object OkSubscribe : ClientResponse
    data object OkUnsubscribe : ClientResponse
    data class Error(val error: ClientRequestError) : ClientResponse
}
