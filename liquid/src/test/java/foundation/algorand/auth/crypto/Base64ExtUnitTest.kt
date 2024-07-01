package foundation.algorand.auth.crypto

import org.junit.Test

class Base64ExtUnitTest {
    @Test
    fun testBase64Ext() {
        val testString = "SGVsbG8gV29ybGQ="
        val encoded = testString.decodeBase64()
        val decoded = encoded.toBase64()
        assert(testString == decoded)
    }
}
