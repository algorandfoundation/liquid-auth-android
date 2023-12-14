package foundation.algorand.auth.verify

import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.KeyPair
import javax.inject.Inject

class ConnectApi @Inject constructor(
    private val client: OkHttpClient,
) {

    /**
     * Submit Verification Message
     *
     */
    fun submit(message: Message, keyPair: KeyPair?) : Call {
        if(message.wallet === null){
            throw Exception("Missing wallet public key")
        }
        if(message.signature === null){
            if(keyPair === null){
                throw Exception("Message is not signed and no keyPair provided")
            } else {
                message.sign(keyPair)
            }
        }

        // TODO: Create specification for URL Scheme
        val path = "${message.origin}/connect/response"
        val body = message.toJSON().toString().toRequestBody("application/json".toMediaTypeOrNull())
        return client.newCall(
            Request.Builder()
                .url(path)
                .method("POST", body)
                .build()
        )
    }
}
