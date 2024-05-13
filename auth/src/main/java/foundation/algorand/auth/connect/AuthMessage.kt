package foundation.algorand.auth.connect

import android.util.Log
import com.google.mlkit.vision.barcode.common.Barcode
import org.json.JSONObject
import java.net.URI
import javax.inject.Inject

private fun URI.findParameterValue(parameterName: String): String? {
    return query.split('&').map {
        val parts = it.split('=')
        val name = parts.firstOrNull() ?: ""
        val value = parts.drop(1).firstOrNull() ?: ""
        Pair(name, value)
    }.firstOrNull{it.first == parameterName}?.second
}
class AuthMessage @Inject constructor(
    var origin: String,
    val requestId: Double
) {

    companion object {
        const val TAG = "connect.Message"
        /**
         * Parse the `Barcode`
         *
         * uses the following url scheme:
         *
         * `liquid://<ORIGIN>/?requestId=<REQUEST_ID>`
         */
        fun fromBarcode(barcode: Barcode): AuthMessage {
            Log.d(TAG, "fromBarcode(${barcode.displayValue})")
            val stringContents = barcode.displayValue ?: throw Exception("Barcode does not contain a display value")
            if(stringContents.startsWith("liquid://")) {
                val uri = URI(stringContents)
                val origin = "https://${uri.host}"
                val requestId = uri.findParameterValue("requestId")!!.toDouble()
                return AuthMessage(origin, requestId)
            } else {
                // Fallback to JSON renderer
                val json = JSONObject(barcode.displayValue.toString())
                val origin = json.get("origin").toString()
                val requestId = json.get("requestId").toString().toDouble()
                return AuthMessage(origin, requestId)
            }
        }
    }
    fun toJSON() : JSONObject {
        val result = JSONObject()
        result.put("origin", origin)
        result.put("requestId", requestId)
        return result
    }
}
