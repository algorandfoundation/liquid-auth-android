package foundation.algorand.demo.credential

import android.content.Context
import android.os.Build
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.credentials.provider.CallingAppInfo
import foundation.algorand.demo.credential.db.Credential
import foundation.algorand.demo.credential.db.CredentialDatabase
import java.security.*
import java.security.spec.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi


interface CredentialRepository {
    val keyStore: KeyStore
    var db: CredentialDatabase
    suspend fun saveCredential(context: Context, credential: Credential)
    fun getDatabase(context: Context): CredentialDatabase
    fun generateCredentialId(): ByteArray
    fun getKeyPair(context: Context): KeyPair
    fun getKeyPair(context: Context, credentialId: ByteArray): KeyPair
    fun appInfoToOrigin(info: CallingAppInfo): String
    fun getCredential(context: Context, credentialId: ByteArray): Credential?
}
fun CredentialRepository(): CredentialRepository = Repository()
class Repository(): CredentialRepository {
    override var keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore")
    private var generator: KeyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC)
    override lateinit var db: CredentialDatabase
    init {
        keyStore.load(null)
    }
    companion object {
        const val TAG = "CredentialRepository"
    }
    override suspend fun saveCredential(context: Context, credential: Credential) {
        Log.d(TAG, "saveCredential($credential)")
        getDatabase(context)
        db.credentialDao().insertAll(credential)
    }
    override fun getDatabase(context: Context): CredentialDatabase {
        Log.d(TAG, "getDatabase($context)")
        if(!::db.isInitialized) {
            db = CredentialDatabase.getInstance(context)
        }
        return db
    }
    override fun generateCredentialId(): ByteArray {
        Log.d(TAG, "generateCredentialId()")
        val credentialId = ByteArray(32)
        SecureRandom().nextBytes(credentialId)
        return credentialId
    }

    @OptIn(ExperimentalEncodingApi::class)
    override fun getCredential(context: Context, credentialId: ByteArray): Credential? {
        getDatabase(context)
        return db.credentialDao().findById(Base64.encode(credentialId))
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun getKeyPairFromDatabase(context: Context, credentialId: ByteArray): KeyPair? {
        Log.d(TAG, "getKeyPairFromDatabase()")
        getDatabase(context)
        val credential = db.credentialDao().findById(Base64.encode(credentialId))
        if (credential != null) {
            val publicKeyBytes = Base64.decode(credential.publicKey)
            val privateKeyBytes = Base64.decode(credential.privateKey)
            val factory = KeyFactory.getInstance("EC")
            val publicKey = factory.generatePublic(X509EncodedKeySpec(publicKeyBytes))
            val privateKey = factory.generatePrivate(PKCS8EncodedKeySpec(privateKeyBytes))
            return KeyPair(publicKey, privateKey)
        }
        return null
    }
    override fun getKeyPair(context: Context): KeyPair{
        return getKeyPair(context, generateCredentialId())
    }
    override fun getKeyPair(context:Context, credentialId: ByteArray): KeyPair {
        Log.d(TAG, "getKeyPair($context, $credentialId)")
        val savedKeyPair = getKeyPairFromDatabase(context, credentialId)
        if (savedKeyPair != null) {
            return savedKeyPair
        }
        generator.initialize(ECGenParameterSpec("secp256r1"))
        return generator.generateKeyPair()
    }
    @OptIn(ExperimentalEncodingApi::class)
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun appInfoToOrigin(info: CallingAppInfo): String {
        val cert = info.signingInfo.apkContentsSigners[0].toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val certHash = md.digest(cert)
        // This is the format for origin
        return "android:apk-key-hash:${Base64.encode(certHash)}"
    }
}
