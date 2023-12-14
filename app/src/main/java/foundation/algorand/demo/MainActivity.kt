package foundation.algorand.demo

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.algorand.algosdk.account.Account
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import foundation.algorand.auth.verify.ConnectApi
import foundation.algorand.auth.verify.Message
import foundation.algorand.auth.verify.crypto.KeyPairs
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.bouncycastle.jce.provider.BouncyCastleProvider
import ru.gildor.coroutines.okhttp.await
import java.security.Security


class MainActivity : AppCompatActivity() {
    private lateinit var scanner: GmsBarcodeScanner
    val client = ConnectApi(OkHttpClient())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Security.removeProvider("BC")
        Security.insertProviderAt(BouncyCastleProvider(), 0)
        setContentView(R.layout.activity_main)
        val account = Account()
        scanner = GmsBarcodeScanning.getClient(this@MainActivity)
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                Toast.makeText(this@MainActivity, barcode.displayValue, Toast.LENGTH_LONG).show()
                barcode.displayValue?.let {
                    val msg = Message.fromBarcode(barcode)
                    msg.wallet = account.address.toString()
                    msg.origin = "http://192.168.101.245:3000"
                    lifecycleScope.launch {
                        client.submit(msg, KeyPairs.getKeyPair(account.toMnemonic())).await()
                    }
                }
            }
            .addOnCanceledListener {
                Toast.makeText(this@MainActivity, "Canceled", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_LONG).show()
            }
        Log.d("HEY", "hey")
    }
}
