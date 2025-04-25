package co.algorand.liquid.wallet

import android.content.Context
import android.graphics.drawable.Icon
import androidx.room.Room
import co.algorand.liquid.wallet.crypto.HDKeyManager
import co.algorand.liquid.wallet.crypto.MnemonicManager
import co.algorand.liquid.wallet.data.CredentialDatabase
import co.algorand.liquid.wallet.data.KeysRepository
import co.algorand.liquid.wallet.data.ServiceRepository
import co.algorand.liquid.wallet.fido.Cookies
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import okhttp3.OkHttpClient


object AppDependencies {
    var httpClient = OkHttpClient.Builder().cookieJar(Cookies()).build()
    lateinit var xHDKeyManager: HDKeyManager
    lateinit var scanner: GmsBarcodeScanner
    lateinit var mnemonicManager: MnemonicManager
    lateinit var database: CredentialDatabase
    lateinit var keysRepository: KeysRepository
    lateinit var serviceRepository: ServiceRepository

    var providerIcon: Icon? = null

    fun init(context: Context) {
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
