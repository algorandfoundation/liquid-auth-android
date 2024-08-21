package foundation.algorand.demo.provider

import android.util.Log
import com.algorand.algosdk.transaction.Transaction
import com.algorand.algosdk.util.Encoder
import com.fasterxml.uuid.Generators
import foundation.algorand.crypto.EncoderType
import foundation.algorand.crypto.avm.KeyPairs
import foundation.algorand.provider.Message
import foundation.algorand.provider.avm.models.*
import java.security.KeyPair
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi


/**
 * A provider for the Algorand Virtual Machine (AVM).
 * Used to test the provider.avm.models package.
 */
class AVMProvider(val providerId: String) {
    val uuidGenerator = Generators.timeBasedEpochRandomGenerator()
    val encoder = foundation.algorand.crypto.avm.Encoder()
    var keyPair: KeyPair? = null
    /**
     * Handle a message from a channel
     */
    fun handleRequestMessage(msg: Message, keyPair: KeyPair): ResponseMessage {
        val message = encoder.decode<RequestMessage>(msg.data, msg.encoding)
        // TODO: secure the keyPair
        this.keyPair = keyPair
        when (message.reference) {
            "arc0027:sign_transactions:request" -> {
                val params = encoder.decode<SignTransactionsParams>(
                    encoder.encode(message.params, EncoderType.NONE), EncoderType.NONE
                )
                val result = processSignTransactions(params)
                return ResponseMessage(
                    id = uuidGenerator.generate().toString(),
                    reference = "arc0027:sign_transactions:response",
                    requestId = message.id,
                    result = result
                )
            }
//            "arc0027:post_transactions:request" -> {
//                val request = encoder.decode<PostTransactionsParams>(
//                    encoder.encode(message.params, EncoderType.NONE), EncoderType.NONE
//                )
//                return processPostTransactions(request)
//            }
//            "arc0027:sign_and_post_transactions:request" -> {
//                val request = encoder.decode<SignAndPostTransactionsParams>(
//                    encoder.encode(message.params, EncoderType.NONE), EncoderType.NONE
//                )
//                return processSignAndPostTransactions(request)
//            }
//            "arc0027:sign_message:request" -> {
//                val request = encoder.decode<SignMessageParams>(
//                    encoder.encode(message.params, EncoderType.NONE), EncoderType.NONE
//                )
//                return processSignMessage(request)
//            }
            else -> {
                throw IllegalArgumentException("Invalid reference: ${message.reference}")
            }
        }
    }
    /**
     * Decode Unsigned Transaction
     */
    @OptIn(ExperimentalEncodingApi::class)
    private fun decodeUnsignedTransaction(unsignedTxn: String): Transaction? {
        return Encoder.decodeFromMsgPack(Base64.decode(unsignedTxn), Transaction::class.java)
    }
    @OptIn(ExperimentalEncodingApi::class)
    fun processSignTransactions(params: SignTransactionsParams): SignTransactionsResult {
        Log.d("AVMProvider", "processSignTransactions")
        require(params.validate())

        val signedTxns = mutableListOf<String>()
        val txnIds = mutableListOf<String>()
        params.txns.forEach { txn ->
            val inst = decodeUnsignedTransaction(Base64.encode(Base64.UrlSafe.decode(txn.txn!!)))
            val signature = KeyPairs.rawSignBytes(inst!!.bytesToSign(), this.keyPair!!.private)
            signedTxns.add(Base64.UrlSafe.encode(signature!!))
            txnIds.add(inst.txID())
        }
        // Create the response payload
        return SignTransactionsResult(providerId, signedTxns)
    }

    fun processPostTransactions(params: PostTransactionsParams): PostTransactionsResult {
        require(params.validate())
        // TODO: Handle the event
        return PostTransactionsResult(providerId, listOf())
    }

    fun processSignAndPostTransactions(params: SignAndPostTransactionsParams): SignAndPostTransactionsResult {
        require(params.validate())
        // TODO: Handle the event
        return SignAndPostTransactionsResult(providerId, listOf())
    }

    fun processSignMessage(params: SignMessageParams): SignMessageResult {
        require(params.validate())
        // TODO: Handle the event
        return SignMessageResult(providerId, "", "")
    }
}

