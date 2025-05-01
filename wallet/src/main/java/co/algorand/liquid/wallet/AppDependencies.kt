package co.algorand.liquid.wallet

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.drawable.Icon
import android.os.IBinder
import androidx.credentials.CredentialManager
import androidx.room.Room
import co.algorand.liquid.wallet.crypto.HDKeyManager
import co.algorand.liquid.wallet.crypto.MnemonicManager
import co.algorand.liquid.wallet.data.CredentialDatabase
import co.algorand.liquid.wallet.data.KeysRepository
import co.algorand.liquid.wallet.data.ServiceRepository
import co.algorand.liquid.wallet.fido.Cookies
import co.algorand.liquid.wallet.provider.AVMProvider
import com.fasterxml.uuid.Generators
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import foundation.algorand.auth.connect.SignalService
import foundation.algorand.auth.fido2.AssertionApi
import foundation.algorand.auth.fido2.AttestationApi
import okhttp3.OkHttpClient


object AppDependencies {
    // App Dependencies
    var httpClient = OkHttpClient.Builder().cookieJar(Cookies()).build()
    lateinit var credentialManager: CredentialManager
    lateinit var xHDKeyManager: HDKeyManager
    lateinit var scanner: GmsBarcodeScanner
    lateinit var mnemonicManager: MnemonicManager
    lateinit var database: CredentialDatabase
    lateinit var keysRepository: KeysRepository
    lateinit var serviceRepository: ServiceRepository

    // WebRTC Service
    lateinit var mConnection: ServiceConnection
    lateinit var signalService: SignalService
    var mBounded = false

    // FIDO Configuration
    var attestationApi = AttestationApi(httpClient)
    var assertionApi = AssertionApi(httpClient)

    // AVM Provider Configuration
    val uuidGenerator = Generators.timeBasedEpochRandomGenerator()
    val providerId = uuidGenerator.generate().toString() // Add a fixed provider for your wallet
    private val provider = AVMProvider(providerId)
    var providerIcon: Icon? = null

    fun init(context: Context) {
        credentialManager = CredentialManager.create(context)
        // Handle the Service Connection
        mConnection =
            object : ServiceConnection {
                override fun onServiceDisconnected(name: ComponentName) {
                    mBounded = false
                }

                override fun onServiceConnected(name: ComponentName, service: IBinder) {
                    mBounded = true
                    val mLocalBinder = service as SignalService.LocalBinder
                    signalService = mLocalBinder.getServerInstance()
                }
            }

        scanner = GmsBarcodeScanning.getClient(context)
        mnemonicManager = MnemonicManager(context)
        xHDKeyManager = HDKeyManager()

        var rootKey = mnemonicManager.fetch()

        if(rootKey !== null){
            xHDKeyManager.setRootKey(rootKey)
        }
        database = Room.databaseBuilder(context, CredentialDatabase::class.java, "credentials.db")
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build()

        providerIcon = Icon.createWithResource(context, R.drawable.android_secure)

        keysRepository =
            KeysRepository(
                credentialDao = database.credentialDao(),
                applicationContext = context,
                mnemonicManager = mnemonicManager
            )
        serviceRepository = ServiceRepository(
            keysRepository = keysRepository,
            applicationContext = context
        )
    }
}
