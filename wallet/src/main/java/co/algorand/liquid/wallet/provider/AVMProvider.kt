package co.algorand.liquid.wallet.provider

import android.util.Log
import co.algorand.liquid.wallet.crypto.HDKeyManager
import co.algorand.liquid.wallet.encoding.b64Decode
import co.algorand.liquid.wallet.encoding.b64Encode
import com.algorand.algosdk.transaction.Transaction
import com.algorand.algosdk.util.Encoder
import com.fasterxml.uuid.Generators
import foundation.algorand.crypto.EncoderType
import foundation.algorand.provider.IBaseProvider
import foundation.algorand.provider.Message
import foundation.algorand.provider.avm.models.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi


/**
 * A provider for the Algorand Virtual Machine (AVM).
 * Used to test the provider.avm.models package.
 */
class AVMProvider(val providerId: String): IBaseProvider {
    val uuidGenerator = Generators.timeBasedEpochRandomGenerator()
    val encoder = foundation.algorand.crypto.avm.Encoder()

    private var keyManager: HDKeyManager? = null


    override fun handleMessage(message: Message): Any {
        val decoded = encoder.decode<RequestMessage>(message.data, message.encoding)
        when (decoded.reference) {
            "arc0027:sign_transactions:request" -> {
                val params = encoder.decode<SignTransactionsParams>(
                    encoder.encode(decoded.params, EncoderType.NONE), EncoderType.NONE
                )
                val result = processSignTransactions(params)
                return ResponseMessage(
                    id = uuidGenerator.generate().toString(),
                    reference = "arc0027:sign_transactions:response",
                    requestId = decoded.id,
                    result = result
                )
            }
            else -> {
                throw IllegalArgumentException("Invalid reference: ${decoded.reference}")
            }
        }
    }
    /**
     * Update the KeyPair
     */
    fun setKeyManager(manager: HDKeyManager) {
        this.keyManager = manager
    }
    /**
     * Decode Unsigned Transaction
     */
    @OptIn(ExperimentalEncodingApi::class)
    private fun decodeUnsignedTransaction(unsignedTxn: String): Transaction? {
        return Encoder.decodeFromMsgPack(b64Decode(unsignedTxn), Transaction::class.java)
    }

    /**
     * Process ARC27 Sign Transactions Requests
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun processSignTransactions(params: SignTransactionsParams): SignTransactionsResult {
        Log.d("AVMProvider", "processSignTransactions")
        require(params.validate())

        val signedTxns = mutableListOf<String>()
        val txnIds = mutableListOf<String>()
        params.txns.forEach { txn ->
            val inst = decodeUnsignedTransaction(txn.txn!!)
            val signature = keyManager!!.rawSign(inst!!.bytesToSign())
            signedTxns.add(Base64.UrlSafe.encode(signature!!))
            txnIds.add(inst.txID())
        }
        // Create the response payload
        return SignTransactionsResult(providerId, signedTxns)
    }
}

