package foundation.algorand.auth.connect

import org.json.JSONObject

class LinkMessage(val requestId: Double, val wallet: String, val credId: String? = null) {
    companion object {
        const val TAG = "connect.LinkMessage"
        fun fromJson(json: String): LinkMessage {
            val data = JSONObject(json).get("data") as JSONObject
            val requestId = data.get("requestId").toString().toDouble()
            val wallet = data.get("wallet").toString()
            val credId = data.get("credId").toString()
            return LinkMessage(requestId, wallet, credId)
        }
    }
    fun toJson(): JSONObject {
        val result = JSONObject()
        result.put("requestId", requestId)
        result.put("wallet", wallet)
        result.put("credId", credId)
        return result
    }
}
