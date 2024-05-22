package foundation.algorand.auth.crypto

import android.util.Log
import cash.z.ecc.android.bip39.Mnemonics.MnemonicCode
import cash.z.ecc.android.bip39.Mnemonics.WordCount
import java.security.InvalidKeyException
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.NoSuchAlgorithmException
import java.security.PrivateKey
import java.security.Signature
import java.security.SignatureException

class KeyPairs {
    companion object {
        const val TAG = "verify.crypto.KeyPairs"
        const val KEY_ALGO = "Ed25519"
        private const val SK_SIZE = 32
        const val SK_SIZE_BITS = SK_SIZE * 8
        @Throws(NoSuchAlgorithmException::class)
        fun rawSignBytes(bytes: ByteArray, key: PrivateKey): ByteArray? {
            try {
                val signer = Signature.getInstance("EdDSA")
                signer.initSign(key)
                signer.update(bytes)
                return signer.sign()
            } catch (e: InvalidKeyException) {
                throw RuntimeException("unexpected behavior", e)
            } catch (e: SignatureException) {
                throw RuntimeException("unexpected behavior", e)
            }
        }
        fun getKeyPair(): KeyPair {
            val mnemonicCode: MnemonicCode = MnemonicCode(WordCount.COUNT_24)
            return getKeyPair(mnemonicCode.joinToString(" "))
        }
        fun getKeyPair(mnemonic: String): KeyPair {
            Log.d(TAG, "getKeyPair(******)")
            val generator = KeyPairGenerator.getInstance(KEY_ALGO)
            val entropy = MnemonicCode(mnemonic.toCharArray()).toEntropy()
            val fixedRandom = FixedSecureRandom(entropy)
            generator.initialize(SK_SIZE_BITS, fixedRandom)
            return generator.genKeyPair()
        }
    }
}
