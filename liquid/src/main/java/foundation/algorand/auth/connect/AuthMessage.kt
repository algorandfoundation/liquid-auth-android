package foundation.algorand.auth.connect

import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.barcode.common.Barcode
import org.json.JSONObject
import javax.inject.Inject

private fun Uri.findParameterValue(parameterName: String): String? {
    return query?.split('&')?.map {
        val parts = it.split('=')
        val name = parts.firstOrNull() ?: ""
        val value = parts.drop(1).firstOrNull() ?: ""
        Pair(name, value)
    }?.firstOrNull{it.first == parameterName}?.second
}
class AuthMessage @Inject constructor(
    var origin: String,
    val requestId: Double
) {

    companion object {
        const val TAG = "connect.Message"
        fun fromUri(uri: Uri): AuthMessage {
            Log.d(TAG, "fromUri($uri)")
            val origin = "https://${uri.host}"
            val requestId = uri.findParameterValue("requestId")!!.toDouble()
            return AuthMessage(origin, requestId)
        }
        /**
         * Parse the Uri string
         *
         * `liquid://<ORIGIN>/?requestId=<REQUEST_ID>`
         */
        fun fromString(stringContents: String): AuthMessage {
            Log.d(TAG, "fromString($stringContents)")
            if(stringContents.startsWith("liquid://")) {
               return fromUri(Uri.parse(stringContents))
            } else {
                // Fallback to JSON renderer
                val json = JSONObject(stringContents)
                val origin = json.get("origin").toString()
                val requestId = json.get("requestId").toString().toDouble()
                return AuthMessage(origin, requestId)
            }
        }
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
            return fromString(stringContents)
        }
    }
    fun toJSON() : JSONObject {
        val result = JSONObject()
        result.put("origin", origin)
        result.put("requestId", requestId)
        return result
    }
}
