package net.sgj0.cordova.plugin.passkey;

import android.app.Activity;
import android.os.CancellationSignal;
import android.os.OutcomeReceiver;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.GetPublicKeyCredentialOption;
import androidx.credentials.PublicKeyCredential;
import androidx.credentials.exceptions.GetCredentialException;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.Executor;

public class PasskeyPlugin extends CordovaPlugin {

    private static final String TAG = "PasskeyPlugin";

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        Log.d(TAG, "Called with options: " + args);

        if ("getPasskey".equals(action)) {
            String requestJson = args.getString(0);
            getPasskey(requestJson, callbackContext);
            return true;
        }
        return false;
    }

    private void getPasskey(String requestJson, CallbackContext callbackContext) {
        Activity activity = this.cordova.getActivity();

        try {
            CredentialManager credentialManager = CredentialManager.create(activity);
            GetPublicKeyCredentialOption publicKeyCredentialOption = new GetPublicKeyCredentialOption(requestJson);

            GetCredentialRequest request = new GetCredentialRequest.Builder()
                    .addCredentialOption(publicKeyCredentialOption)
                    .build();

            Executor mainExecutor = ContextCompat.getMainExecutor(activity);
            CancellationSignal cancellationSignal = new CancellationSignal();

            credentialManager.getCredentialAsync(
                    activity,
                    request,
                    cancellationSignal,
                    mainExecutor,
                    new OutcomeReceiver<GetCredentialResponse, GetCredentialException>() {
                        @Override
                        public void onResult(GetCredentialResponse result) {
                            Credential credential = result.getCredential();
                            if (credential instanceof PublicKeyCredential) {
                                try {
                                    String authResponseJson = ((PublicKeyCredential) credential).getAuthenticationResponseJson();
                                    callbackContext.success(new JSONObject(authResponseJson));
                                } catch (JSONException e) {
                                    Log.e(TAG, "Erreur JSON: " + e.getMessage(), e);
                                    callbackContext.error("Erreur de parsing JSON: " + e.getMessage());
                                }
                            } else {
                                callbackContext.error("Type d'identifiant non pris en charge.");
                            }
                        }

                        @Override
                        public void onError(@NonNull GetCredentialException e) {
                            Log.e(TAG, "GetCredentialException: " + e.getMessage(), e);
                            callbackContext.error(e.getMessage() != null ? e.getMessage() : "Erreur inconnue lors de l'authentification par Passkey.");
                        }
                    }
            );
        } catch (Exception e) {
            Log.e(TAG, "Exception: " + e.getMessage(), e);
            callbackContext.error(e.getMessage());
        }
    }
}