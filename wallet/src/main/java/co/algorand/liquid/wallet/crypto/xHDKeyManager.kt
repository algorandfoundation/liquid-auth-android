package co.algorand.liquid.wallet.crypto

import android.util.Log
import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.bip39.toSeed
import foundation.algorand.deterministicP256.DeterministicP256
import foundation.algorand.xhdwalletapi.KeyContext
import foundation.algorand.xhdwalletapi.XHDWalletAPIAndroid
import foundation.algorand.xhdwalletapi.encodeAddress
import java.security.KeyPair
import java.security.MessageDigest

const val EXCEPTION_KEY_NOT_FOUND = "Root key was not found"
const val EXCEPTION_KEY_EXISTS = "Root keys already exist"

class HDKeyManager {
    private var xPasskey = DeterministicP256()
    private var xHD: XHDWalletAPIAndroid? = null

    // Algorand ed25519 Spending Keys
    private var spendKey: ByteArray? = null
    // Deterministic P-256 Passkeys
    private var rootPasskey: ByteArray? = null

    fun getAddress(): String? {
        if(spendKey === null) return null
        return encodeAddress(spendKey!!)
    }

    fun generateCredentialId(keyPair: KeyPair): ByteArray {
        // Get the public key bytes
        val publicKeyBytes = keyPair.public.encoded

        // Compute SHA-256 hash of the public key
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val credentialId = messageDigest.digest(publicKeyBytes)

        return credentialId
    }

    fun setRootKey(seed: Mnemonics.MnemonicCode){
        Log.d(TAG, "setRootKey(${seed.joinToString(" ")})")
        if(xHD !== null || spendKey !== null){
            throw Exception(EXCEPTION_KEY_EXISTS)
        }
        xHD = XHDWalletAPIAndroid(seed.toSeed())
        // Just assume this address path is always for spending
        spendKey = xHD?.keyGen(KeyContext.Address, 0u, 0u, 0u)
        rootPasskey = xPasskey.genDerivedMainKeyWithBIP39(seed.joinToString(" "))
    }

    fun generatePasskey(origin: String, userHandle: String): KeyPair {
        return xPasskey.genDomainSpecificKeypair(rootPasskey!!, origin, userHandle.lowercase())
    }
    /**
     * Harden a number (set the highest bit to 1) Note that the input is UInt and the output is also
     * UInt
     *
     * @param num
     * @returns
     * @deprecated
     */
    private fun harden(num: UInt): UInt = 0x80000000.toUInt() + num

    /**
     * Get the BIP44 path from the context, account and keyIndex
     *
     * @param context
     * @param account
     * @param keyIndex
     * @returns
     * @deprecated
     */
    private fun getBIP44PathFromContext(
        context: KeyContext,
        account: UInt,
        change: UInt,
        keyIndex: UInt
    ): List<UInt> {
        return when (context) {
            KeyContext.Address -> listOf(harden(44u), harden(283u), harden(account), change, keyIndex)
            KeyContext.Identity -> listOf(harden(44u), harden(0u), harden(account), change, keyIndex)
        }
    }

    /**
     * @deprecated
     */
    fun rawSign(bytes: ByteArray): ByteArray? {
        return xHD?.rawSign(
            getBIP44PathFromContext(KeyContext.Address, 0u, 0u, 0u),
            bytes,
        )
    }

    fun signPasskey(keyPair: KeyPair, origin: String, userHandle: String, payload: ByteArray): ByteArray {
        Log.d(TAG, "signPasskey(${origin}, ${userHandle})")
        return xPasskey.signWithDomainSpecificKeyPair(keyPair, payload)
    }
    fun signTxn(txn: ByteArray): ByteArray? {
        Log.d(TAG, "signTxn(${txn.size})")
        if(spendKey === null || xHD === null){
            throw Exception(EXCEPTION_KEY_NOT_FOUND)
        }
        return xHD?.signAlgoTransaction(
            KeyContext.Address,
            0u,
            0u,
            0u,
            txn
        )
    }
    companion object {
        const val TAG = "xHDKeyManager"
    }
}