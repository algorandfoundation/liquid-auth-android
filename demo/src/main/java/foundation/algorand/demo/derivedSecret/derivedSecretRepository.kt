package foundation.algorand.demo.derivedSecret

import android.content.Context
import android.os.Build
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.credentials.provider.CallingAppInfo
import foundation.algorand.demo.derivedSecret.db.DerivedSecret
import foundation.algorand.demo.derivedSecret.db.DerivedSecretDatabase
import foundation.algorand.deterministicP256.DeterministicP256
import java.security.*
import java.security.spec.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import foundation.algorand.demo.derivedSecret.db.SecretType

// import java.security.interfaces.ECPrivateKey

interface DerivedSecretRepository {
    val keyStore: KeyStore
    var db: DerivedSecretDatabase
    fun saveDerivedParentSecret(context: Context, mnemonic: CharArray)
    fun getDatabase(context: Context): DerivedSecretDatabase
    fun getDerivedParentSecret(context: Context): DerivedSecret?
}

fun DerivedSecretRepository(): DerivedSecretRepository = Repository()

class Repository() : DerivedSecretRepository {
    override var keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore")
    private var generator: KeyPairGenerator =
            KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC)
    private var dP256: DeterministicP256 = DeterministicP256()
    override lateinit var db: DerivedSecretDatabase
    init {
        keyStore.load(null)
    }
    companion object {
        const val TAG = "DerivedSecretRepository"
    }

    override fun saveDerivedParentSecret(context: Context, mnemonic: CharArray) {
        Log.d(TAG, "saveDerivedParentSecret([mnemonic kept hidden])")
        getDatabase(context)

        getDerivedParentSecret(context)?.let { db.derivedSecretDao().delete(it) }

        db.derivedSecretDao()
                .insertAllNoSuspend(
                        DerivedSecret(
                                id = "derivedParentSecret",
                                derivedSecret =
                                        dP256.genDerivedMainKeyWithBIP39(mnemonic.concatToString())
                                                .contentToString(),
                                type = SecretType.PASSKEY,
                                mnemonic = mnemonic.concatToString() // We could choose to not store the mnemonic
                        )
                )
    }

    override fun getDatabase(context: Context): DerivedSecretDatabase {
        Log.d(TAG, "getDatabase($context)")
        if (!::db.isInitialized) {
            db = DerivedSecretDatabase.getInstance(context)
        }
        return db
    }

    override fun getDerivedParentSecret(context: Context): DerivedSecret? {
        getDatabase(context)
        return db.derivedSecretDao().findById("derivedParentSecret")
    }

}
