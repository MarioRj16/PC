package pt.isel.pc.problemsets.set3.ex3

import pt.isel.pc.problemsets.set3.ex3.protocol.ClientRequest
import pt.isel.pc.problemsets.set3.ex3.protocol.ClientRequestError
import pt.isel.pc.problemsets.set3.ex3.protocol.ClientResponse
import pt.isel.pc.problemsets.set3.ex3.protocol.ServerPush
import pt.isel.pc.problemsets.set3.ex3.protocol.parseClientRequest
import pt.isel.pc.problemsets.set3.ex3.protocol.serialize
import kotlin.test.Test
import kotlin.test.assertEquals

class ParseTests {

    @Test
    fun `test success parse cases`() {
        assertEquals(
            ClientRequest.Publish(TopicName("t1"), "hello world"),
            parseClientRequest("PUBLISH t1 hello world").successOrThrow,
        )
        assertEquals(
            ClientRequest.Publish(TopicName("t1"), "hello world"),
            parseClientRequest(" PUBLISH t1 hello world").successOrThrow,
        )
        assertEquals(
            ClientRequest.Publish(TopicName("t1"), " hello world"),
            parseClientRequest("PUBLISH t1  hello world").successOrThrow,
        )
        assertEquals(
            ClientRequest.Publish(TopicName("t1"), " hello world  "),
            parseClientRequest("PUBLISH t1  hello world  ").successOrThrow,
        )
        assertEquals(
            ClientRequest.Publish(TopicName("t1"), ""),
            parseClientRequest("PUBLISH t1").successOrThrow,
        )
        assertEquals(
            ClientRequest.Publish(TopicName("t1"), ""),
            parseClientRequest("PUBLISH t1 ").successOrThrow,
        )
        assertEquals(
            ClientRequest.Publish(TopicName("t1"), ""),
            parseClientRequest("  PUBLISH t1 ").successOrThrow,
        )
    }

    @Test
    fun `test error parse cases`() {
        assertEquals(
            ClientRequestError.MissingCommandName,
            parseClientRequest("").errorOrThrow,
        )
        assertEquals(
            ClientRequestError.MissingCommandName,
            parseClientRequest(" ").errorOrThrow,
        )
        assertEquals(
            ClientRequestError.UnknownCommandName,
            parseClientRequest("NOT-A-COMMAND").errorOrThrow,
        )
        assertEquals(
            ClientRequestError.InvalidArguments,
            parseClientRequest("PUBLISH").errorOrThrow,
        )
        assertEquals(
            ClientRequestError.InvalidArguments,
            parseClientRequest("SUBSCRIBE").errorOrThrow,
        )
    }

    @Test
    fun `test toString`() {
        assertEquals(
            "+0",
            serialize(ClientResponse.OkPublish(0)),
        )

        assertEquals(
            "+",
            serialize(ClientResponse.OkSubscribe),
        )

        assertEquals(
            "+",
            serialize(ClientResponse.OkUnsubscribe),
        )

        assertEquals(
            "-INVALID_ARGUMENTS",
            serialize(ClientResponse.Error(ClientRequestError.InvalidArguments)),
        )

        assertEquals(
            ">the-topic the content",
            serialize(
                ServerPush.PublishedMessage(
                    PublishedMessage(TopicName("the-topic"), "the content"),
                ),
            ),
        )
    }




    @Test
    fun `test parse cases with long topic name`() {
        val longTopicName = "t".repeat(256)
        assertEquals(
            ClientRequest.Publish(TopicName(longTopicName), "hello world"),
            parseClientRequest("PUBLISH $longTopicName hello world").successOrThrow,
        )
    }
}
