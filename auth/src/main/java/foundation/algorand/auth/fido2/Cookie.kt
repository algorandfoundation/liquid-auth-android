package foundation.algorand.auth.fido2

import okhttp3.Response

class Cookie {
    companion object {
        private const val SESSION_KEY = "connect.sid="

        fun fromResponse(response: Response) : String? {
            val cookie = response.headers("set-cookie").find { it.startsWith(SESSION_KEY) }
            return cookie
        }
        fun format(sessionId: String): String {
            return "${SESSION_KEY}$sessionId"
        }
    }

}
