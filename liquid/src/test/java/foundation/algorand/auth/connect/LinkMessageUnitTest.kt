package foundation.algorand.auth.connect

import org.junit.Test

class LinkMessageUnitTest {
    @Test
    fun testLinkMessage() {
        val linkMessage = LinkMessage(1.0, "wallet", "credId")
        assert(linkMessage.requestId == 1.0)
        assert(linkMessage.wallet == "wallet")
        assert(linkMessage.credId == "credId")
    }
    @Test
    fun testLinkMessageFromJson() {
        val json = "{\"data\":{\"requestId\":1.0,\"wallet\":\"wallet\",\"credId\":\"credId\"}}"
        val linkMessage = LinkMessage.fromJson(json)
        assert(linkMessage.requestId == 1.0)
        assert(linkMessage.wallet == "wallet")
        assert(linkMessage.credId == "credId")
    }
    @Test
    fun testLinkMessageToJson() {
        val linkMessage = LinkMessage(1.0, "wallet", "credId")
        val json = linkMessage.toJson()
        assert(json.get("requestId") == 1.0)
        assert(json.get("wallet") == "wallet")
        assert(json.get("credId") == "credId")
    }
}
