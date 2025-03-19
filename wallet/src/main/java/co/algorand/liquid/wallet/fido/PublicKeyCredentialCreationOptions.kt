/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.algorand.liquid.wallet.fido

import co.algorand.liquid.wallet.encoding.b64Decode
import org.json.JSONObject

class PublicKeyCredentialCreationOptions(requestJson: String) {
    private val json: JSONObject

    val rp: PublicKeyCredentialRpEntity
    val user: PublicKeyCredentialUserEntity
    val challenge: ByteArray
    private val pubKeyCredParams: List<PublicKeyCredentialParameters>

    private var timeout: Long
    private var excludeCredentials: List<PublicKeyCredentialDescriptor>
    private var authenticatorSelection: AuthenticatorSelectionCriteria
    private var attestation: String

    init {

        json = JSONObject(requestJson)
        val challengeString = json.getString("challenge")
        challenge = b64Decode(challengeString)
        val rpJson = json.getJSONObject("rp")
        rp = PublicKeyCredentialRpEntity(rpJson.getString("name"), rpJson.getString("id"))
        val rpUser = json.getJSONObject("user")
        val userId = b64Decode(rpUser.getString("id"))
        user = PublicKeyCredentialUserEntity(
            rpUser.getString("name"), userId, rpUser.getString("displayName"),
        )
        val pubKeyCredParamsJson = json.getJSONArray("pubKeyCredParams")
        val pubKeyCredParamsTmp: MutableList<PublicKeyCredentialParameters> = mutableListOf()
        for (i in 0 until pubKeyCredParamsJson.length()) {
            val e = pubKeyCredParamsJson.getJSONObject(i)
            pubKeyCredParamsTmp.add(
                PublicKeyCredentialParameters(
                    e.getString("type"),
                    e.getLong("alg"),
                ),
            )
        }
        pubKeyCredParams = pubKeyCredParamsTmp.toList()

        timeout = json.optLong("timeout", 0)

        excludeCredentials = emptyList()
        authenticatorSelection = AuthenticatorSelectionCriteria("platform", "required")
        attestation = json.optString("attestation", "none")
    }
}
