package foundation.algorand.demo.services

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.os.OutcomeReceiver
import android.os.CancellationSignal
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.provider.BeginCreateCredentialRequest
import androidx.credentials.provider.BeginCreateCredentialResponse
import androidx.credentials.provider.BeginCreatePublicKeyCredentialRequest
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.BeginGetPublicKeyCredentialOption
import androidx.credentials.provider.CreateEntry
import androidx.credentials.provider.CredentialProviderService
import androidx.credentials.provider.ProviderClearCredentialStateRequest
import androidx.credentials.provider.PublicKeyCredentialEntry
import foundation.algorand.demo.R
import foundation.algorand.demo.credential.CredentialRepository
import foundation.algorand.demo.credential.db.Credential
import kotlinx.coroutines.*
import org.json.JSONObject

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class LiquidCredentialProviderService: CredentialProviderService() {
    private val credentialRepository = CredentialRepository()
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    companion object {
        const val TAG = "LiquidCredentialProviderService"
        //TODO: App Lock Intents
        const val GET_PASSKEY_INTENT = 1
        const val CREATE_PASSKEY_INTENT = 2
        const val GET_PASSKEY_ACTION = "foundation.algorand.demo.GET_PASSKEY"
        const val CREATE_PASSKEY_ACTION = "foundation.algorand.demo.CREATE_PASSKEY"
    }
    /**
     * Handle Create Credential Requests
     */
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onBeginCreateCredentialRequest(
        request: BeginCreateCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginCreateCredentialResponse, CreateCredentialException>
    ) {
        val response: BeginCreateCredentialResponse? = processCreateCredentialRequest(request)
        if (response != null) {
            callback.onResult(response)
        } else {
            callback.onError(CreateCredentialUnknownException())
        }
    }

    /**
     * Process incoming Create Credential Requests
     */
    private fun processCreateCredentialRequest(request: BeginCreateCredentialRequest): BeginCreateCredentialResponse? {
        when (request) {
            is BeginCreatePublicKeyCredentialRequest -> {
                return handleCreatePasskeyQuery(request)
            }
        }
        return null
    }

    /**
     * Create a new PassKey Entry
     *
     * This returns an Entry list for the user to interact with.
     * A PendingIntent must be configured to receive the data from the WebAuthn client
     */
    private fun handleCreatePasskeyQuery(
        request: BeginCreatePublicKeyCredentialRequest
    ): BeginCreateCredentialResponse {
        Log.d(TAG, request.requestJson)


        val createEntries: MutableList<CreateEntry> = mutableListOf()
        val name =  JSONObject(request.requestJson).getJSONObject("user").get("name").toString()

        createEntries.add( CreateEntry(
            name,
            createNewPendingIntent(CREATE_PASSKEY_ACTION, CREATE_PASSKEY_INTENT, null)
        )
        )
        return BeginCreateCredentialResponse(createEntries)
    }
    /**
     * Handle Get Credential Requests
     */
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onBeginGetCredentialRequest(
        request: BeginGetCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<BeginGetCredentialResponse, GetCredentialException>,
    ) {
        try {
            callback.onResult(processGetCredentialRequest(request))
        } catch (e: GetCredentialException) {
            callback.onError(GetCredentialUnknownException())
        }
    }

    /**
     * Fake a list of available PublicKeyCredential Entries
     */
    private fun processGetCredentialRequest(request: BeginGetCredentialRequest): BeginGetCredentialResponse{
        Log.v(TAG, "processing GetCredentialRequest")
        val deferredCredentials: Deferred<List<Credential>> = scope.async {
            credentialRepository.getDatabase(this@LiquidCredentialProviderService).credentialDao().getAllRegular()
        }
        val credentials = runBlocking {
            deferredCredentials.await()
        }
        return BeginGetCredentialResponse(credentials.map {
            val data = Bundle()
            data.putString("credentialId", it.credentialId)
            data.putString("userHandle", it.userHandle)
            PublicKeyCredentialEntry.Builder(
                this@LiquidCredentialProviderService,
                it.userHandle,
                createNewPendingIntent(GET_PASSKEY_ACTION, GET_PASSKEY_INTENT, data),
                // TODO: filter the request for PublicKeyCredentialOptions
                request.beginGetCredentialOptions[0] as BeginGetPublicKeyCredentialOption
            )
                .setIcon(Icon.createWithResource(this@LiquidCredentialProviderService, R.mipmap.ic_launcher))
                .build()
        })
    }
    override fun onClearCredentialStateRequest(
        request: ProviderClearCredentialStateRequest,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<Void?, ClearCredentialException>
    ) {
        Log.d(TAG, "onClearCredentialStateRequest")
        TODO("Not yet implemented")
    }

    private fun createNewPendingIntent(action: String, requestCode: Int, extra: Bundle?): PendingIntent{
        val intent = Intent(action).setPackage("foundation.algorand.demo")
        if (extra != null) {
            intent.putExtra("CREDENTIAL_DATA", extra)
        }
        return PendingIntent.getActivity(
            applicationContext, requestCode,
            intent, (PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        )
    }
    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
