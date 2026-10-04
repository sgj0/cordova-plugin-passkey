package net.sgj0.cordova.plugin.passkey

import android.app.Activity
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.GetCredentialException
import org.apache.cordova.CordovaPlugin
import org.apache.cordova.CallbackContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Arrays;
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PasskeyPlugin : CordovaPlugin() {

    override fun execute(action: String, args: JSONArray, callbackContext: CallbackContext): Boolean {
        Log.d("PasskeyPlugin", "Called with options: " + args);

        if (action == "getPasskey") {
            val requestJson = args.getString(0)
            getPasskey(requestJson, callbackContext)
            return true
        }
        return false
    }

    private fun getPasskey(requestJson: String, callbackContext: CallbackContext) {
        val activity: Activity = cordova.activity
        val context = activity.applicationContext

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val credentialManager = CredentialManager.create(context)
                val publicKeyCredentialOption = GetPublicKeyCredentialOption(requestJson)
                val request = GetCredentialRequest(listOf(publicKeyCredentialOption))
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential

                if (credential is PublicKeyCredential) {
                    val authResponseJson = credential.authenticationResponseJson
                    callbackContext.success(JSONObject(authResponseJson))
                } else {
                    callbackContext.error("Type d'identifiant non priIntentPlugin.javas en charge.")
                }
            } catch (e: GetCredentialException) {
                Log.d("PasskeyPlugin", e.getMessage());
                Log.d("PasskeyPlugin", Arrays.toString(e.getStackTrace()));
                callbackContext.error(e.message ?: "Erreur inconnue lors de l'authentification par Passkey.")
            } catch (e: Exception) {
                Log.d("PasskeyPlugin", e.getMessage());
                Log.d("PasskeyPlugin", Arrays.toString(e.getStackTrace()));
                callbackContext.error(e.message)
            }
        }
    }
}