package foundation.algorand.auth.verify

import android.util.Log
import com.google.mlkit.vision.barcode.common.Barcode
import foundation.algorand.auth.verify.crypto.KeyPairs
import org.apache.commons.codec.binary.Base64
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.KeyPair
import javax.inject.Inject

class Message @Inject constructor(
    var origin: String,
    val challenge: String,
    val requestId: Double,
    var wallet: String? = null,
    var signature: String? = null
) {
    companion object {
        const val TAG = "verify.Message"
        /**
         * Parse the `Barcode`
         *
         * uses JSON for serialization
         */
        fun fromBarcode(barcode: Barcode): Message {
            Log.d(TAG, "fromBarcode(${barcode.displayValue})")
            val json = JSONObject(barcode.displayValue.toString())
            val origin = json.get("origin").toString()
            val requestId = json.get("requestId").toString().toDouble()
            val challenge = json.get("challenge").toString()
            Log.d(TAG, "RETURN")
            return Message(origin, challenge, requestId)
        }
    }

    /**
     *
     */
    fun sign(keyPair: KeyPair) {
        val signatureBytes = KeyPairs.rawSignBytes(challenge.toByteArray(StandardCharsets.UTF_8), keyPair.private)
        signature = Base64.encodeBase64URLSafeString(signatureBytes)
    }

    fun toJSON() : JSONObject {
        val result = JSONObject()
        result.put("origin", origin)
        result.put("challenge", challenge)
        result.put("requestId", requestId)
        result.put("wallet", wallet)
        result.put("signature", signature)
        return result
    }
}
