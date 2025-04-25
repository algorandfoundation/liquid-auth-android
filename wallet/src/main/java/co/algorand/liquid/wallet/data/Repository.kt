package co.algorand.liquid.wallet.data

import android.content.Context
import co.algorand.liquid.wallet.crypto.MnemonicManager
import co.algorand.liquid.wallet.data.model.Passkey
import co.algorand.liquid.wallet.data.model.PasskeyMetadata
import co.algorand.liquid.wallet.data.model.Site
import co.algorand.liquid.wallet.data.query.SiteWithPasskeys
import kotlinx.coroutines.flow.Flow


class KeysRepository(
    private val mnemonicManager: MnemonicManager,
    private val credentialDao: CredentialDao,
    private val applicationContext: Context,
) {
    suspend fun clear() {
        credentialDao.clearPasskeys()
        credentialDao.clearSites()
        mnemonicManager.clear()
    }

    // UI lookups
    fun siteListWithCredentials(): Flow<List<SiteWithPasskeys>> {
        return credentialDao.siteListWithCredentials()
    }

    fun credentialsForSite(url: String?): SiteWithPasskeys? {
        if (url == null) {
            return null
        }
        return credentialDao.getCredentialsFromSite(url)
    }

    // Site Mutations
    private suspend fun addSite(siteMetaData: Site): Long {
        return credentialDao.insertSite(siteMetaData)
    }

    private suspend fun deleteSite(entity: Site) {
        return credentialDao.deleteSite(entity)
    }


    // Passkey Mutations
    suspend fun updatePasskey(passkey: Passkey) {
        credentialDao.updatePasskey(passkey)
    }
    suspend fun removePasskey(passkey: Passkey) {
        val siteId = passkey.siteId
        credentialDao.deletePasskey(passkey)
        if (credentialDao.countPasskeys(siteId) == 0) {
            credentialDao.deleteSite(Site(id = siteId))
        }
    }
    suspend fun addNewPasskey(passkeyMetadata: PasskeyMetadata) {
        val site = credentialDao.getSite(passkeyMetadata.rpid)
        val siteId = site?.id ?: addSite(Site(url = passkeyMetadata.rpid, name = ""))

        credentialDao.insertPasskey(
            Passkey(
                userId = passkeyMetadata.uid,
                username = passkeyMetadata.username,
                userHandle = passkeyMetadata.displayName,
                credentialId = passkeyMetadata.credId,
                publicKey = passkeyMetadata.credPublicKey,
                privateKey = passkeyMetadata.credPrivateKey,
                siteId = siteId,
                count = 0,
                lastUsedTimeMs = 0L,
            ),
        )
    }

    fun getPasskey(credId: String): Passkey? {
        return credentialDao.getPasskey(credId)
    }
    fun getPasskeysCount(siteId: String?): Int {
        if (siteId == null) {
            return 0
        }

        val credentialsFromSite = credentialDao.getCredentialsFromSite(siteId)

        if (credentialsFromSite != null) {
            return credentialsFromSite.passkeys.size
        }

        return 0
    }
    companion object {
        private const val CREATE_PASSKEY_INTENT =
            "co.algorand.auth.wallet.CREATE_PASSKEY"
        private const val GET_PASSKEY_INTENT =
            "co.algorand.auth.wallet.GET_PASSKEY"
        const val KEY_ACCOUNT_LAST_USED_MS = "key_account_last_used_ms"
        const val KEY_ACCOUNT_ID = "key_account_id"
        const val USER_ACCOUNT = "user_account"
        const val CREDENTIAL_DESCRIPTION =
            "Your credential will be saved securely to the chosen account."
    }
}