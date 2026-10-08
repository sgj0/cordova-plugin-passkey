package net.sgj0.cordova.plugin.passkey;

import android.app.Activity;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.credentials.CreateCredentialResponse;
import androidx.credentials.CreatePublicKeyCredentialRequest;
import androidx.credentials.CreatePublicKeyCredentialResponse;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.GetPublicKeyCredentialOption;
import androidx.credentials.PublicKeyCredential;
import androidx.credentials.exceptions.CreateCredentialException;
import androidx.credentials.exceptions.GetCredentialException;
import java.util.concurrent.Executor;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PasskeyPlugin extends CordovaPlugin {

  private static final String TAG = "PasskeyPlugin";

  @Override
  public boolean execute(
    String action,
    JSONArray args,
    CallbackContext callbackContext
  ) throws JSONException {
    if ("getPasskey".equals(action)) {
      String requestJson = args.getString(0);
      getPasskey(requestJson, callbackContext);
      return true;
    } else if ("createPasskey".equals(action)) {
      String requestJson = args.getString(0);
      createPasskey(requestJson, callbackContext);
      return true;
    }
    return false;
  }

  private void getPasskey(String requestJson, CallbackContext callbackContext) {
    Activity activity = this.cordova.getActivity();

    try {
      CredentialManager credentialManager = CredentialManager.create(activity);
      GetPublicKeyCredentialOption publicKeyCredentialOption =
        new GetPublicKeyCredentialOption(requestJson);

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
        new CredentialManagerCallback<
          GetCredentialResponse,
          GetCredentialException
        >() {
          @Override
          public void onResult(GetCredentialResponse result) {
            Credential credential = result.getCredential();
            if (credential instanceof PublicKeyCredential) {
              try {
                String authResponseJson = (
                  (PublicKeyCredential) credential
                ).getAuthenticationResponseJson();
                callbackContext.success(new JSONObject(authResponseJson));
              } catch (JSONException e) {
                callbackContext.error(
                  "Erreur de parsing JSON: " + e.getMessage()
                );
              }
            } else {
              callbackContext.error("Type d'identifiant non pris en charge.");
            }
          }

          @Override
          public void onError(@NonNull GetCredentialException e) {
            callbackContext.error("GetCredentialException: " + e.getMessage());
          }
        }
      );
    } catch (Exception e) {
      callbackContext.error(e.getMessage());
    }
  }

  private void createPasskey(
    String requestJson,
    CallbackContext callbackContext
  ) {
    Activity activity = this.cordova.getActivity();

    try {
      CredentialManager credentialManager = CredentialManager.create(activity);
      CreatePublicKeyCredentialRequest createRequest =
        new CreatePublicKeyCredentialRequest(requestJson);

      Executor mainExecutor = ContextCompat.getMainExecutor(activity);
      CancellationSignal cancellationSignal = new CancellationSignal();

      credentialManager.createCredentialAsync(
        activity,
        createRequest,
        cancellationSignal,
        mainExecutor,
        new CredentialManagerCallback<
          CreateCredentialResponse,
          CreateCredentialException
        >() {
          @Override
          public void onResult(CreateCredentialResponse result) {
            if (result instanceof CreatePublicKeyCredentialResponse) {
              try {
                String registrationResponseJson = (
                  (CreatePublicKeyCredentialResponse) result
                ).getRegistrationResponseJson();
                callbackContext.success(
                  new JSONObject(registrationResponseJson)
                );
              } catch (JSONException e) {
                callbackContext.error(
                  "Erreur de parsing JSON: " + e.getMessage()
                );
              }
            } else {
              callbackContext.error("Type de réponse non pris en charge.");
            }
          }

          @Override
          public void onError(@NonNull CreateCredentialException e) {
            callbackContext.error(
              "CreateCredentialException: " + e.getMessage()
            );
          }
        }
      );
    } catch (Exception e) {
      callbackContext.error(e.getMessage());
    }
  }
}
