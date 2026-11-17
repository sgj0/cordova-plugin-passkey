package net.sgj0.cordova.plugin.passkey;

import android.app.Activity;
import android.os.CancellationSignal;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

  /** Error types that are not coming from the Credential Manager. */
  private static final String ERROR_INVALID_OPTIONS = "INVALID_OPTIONS";
  private static final String ERROR_ACTIVITY_UNAVAILABLE =
    "ACTIVITY_UNAVAILABLE";
  private static final String ERROR_UNSUPPORTED_RESPONSE =
    "UNSUPPORTED_RESPONSE";
  private static final String ERROR_INVALID_RESPONSE = "INVALID_RESPONSE";
  private static final String ERROR_CANCELED = "CANCELED";
  private static final String ERROR_UNKNOWN = "UNKNOWN";

  /** Only one Credential Manager ceremony can be displayed at a time. */
  private CancellationSignal pendingSignal;
  private CallbackContext pendingCallback;

  @Override
  protected void pluginInitialize() {
    super.pluginInitialize();
    CookieManager cookieManager = CookieManager.getInstance();
    cookieManager.setAcceptCookie(true);
    View view = this.webView.getView();
    if (view instanceof WebView) {
      cookieManager.setAcceptThirdPartyCookies((WebView) view, true);
    }
  }

  @Override
  public boolean execute(
    String action,
    JSONArray args,
    CallbackContext callbackContext
  ) {
    if (!"getPasskey".equals(action) && !"createPasskey".equals(action)) {
      return false;
    }

    final String requestJson = readRequestJson(args);
    if (requestJson == null) {
      sendError(
        callbackContext,
        ERROR_INVALID_OPTIONS,
        "The first argument must be a WebAuthn options object or its JSON string."
      );
      return true;
    }

    final boolean isGet = "getPasskey".equals(action);
    // The Credential Manager UI is bound to the activity: start it from the UI thread.
    cordova.getActivity().runOnUiThread(() -> {
      if (isGet) {
        getPasskey(requestJson, callbackContext);
      } else {
        createPasskey(requestJson, callbackContext);
      }
    });

    return true;
  }

  /**
   * Accept either a JSON object or a JSON string and always return a valid
   * JSON object string (or null when the input is not usable).
   */
  @Nullable
  private static String readRequestJson(JSONArray args) {
    Object raw = args.opt(0);

    try {
      if (raw instanceof JSONObject) {
        return raw.toString();
      }
      if (raw instanceof String) {
        // Validate early: AndroidX only reports a generic IllegalArgumentException.
        return new JSONObject((String) raw).toString();
      }
    } catch (JSONException e) {
      return null;
    }

    return null;
  }

  @Nullable
  private Activity getUsableActivity() {
    Activity activity = this.cordova.getActivity();
    if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
      return null;
    }
    return activity;
  }

  private CancellationSignal startCeremony(CallbackContext callbackContext) {
    // A new ceremony replaces the previous one (double tap, page navigation...).
    cancelPendingCeremony("A new passkey request has been started.");
    pendingSignal = new CancellationSignal();
    pendingCallback = callbackContext;
    return pendingSignal;
  }

  /** @return true if the callback was still the pending one (and is now released). */
  private boolean finishCeremony(CallbackContext callbackContext) {
    if (pendingCallback != callbackContext) {
      return false;
    }
    pendingCallback = null;
    pendingSignal = null;
    return true;
  }

  private void cancelPendingCeremony(String reason) {
    CancellationSignal signal = pendingSignal;
    CallbackContext callback = pendingCallback;
    pendingSignal = null;
    pendingCallback = null;

    if (signal != null && !signal.isCanceled()) {
      signal.cancel();
    }
    if (callback != null && !callback.isFinished()) {
      sendError(callback, ERROR_CANCELED, reason);
    }
  }

  @Override
  public void onReset() {
    cancelPendingCeremony("The WebView has been reloaded.");
    super.onReset();
  }

  @Override
  public void onDestroy() {
    cancelPendingCeremony("The activity has been destroyed.");
    super.onDestroy();
  }

  private void getPasskey(String requestJson, CallbackContext callbackContext) {
    Activity activity = getUsableActivity();
    if (activity == null) {
      sendError(
        callbackContext,
        ERROR_ACTIVITY_UNAVAILABLE,
        "The activity is not available."
      );
      return;
    }

    try {
      CredentialManager credentialManager = CredentialManager.create(activity);
      GetPublicKeyCredentialOption publicKeyCredentialOption =
        new GetPublicKeyCredentialOption(requestJson);

      GetCredentialRequest request = new GetCredentialRequest.Builder()
        .addCredentialOption(publicKeyCredentialOption)
        .build();

      Executor mainExecutor = ContextCompat.getMainExecutor(activity);
      CancellationSignal cancellationSignal = startCeremony(callbackContext);

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
            if (!finishCeremony(callbackContext)) {
              return;
            }

            Credential credential = result.getCredential();
            if (!(credential instanceof PublicKeyCredential)) {
              sendError(
                callbackContext,
                ERROR_UNSUPPORTED_RESPONSE,
                "Unsupported credential type: " + credential.getType()
              );
              return;
            }

            String authResponseJson = (
              (PublicKeyCredential) credential
            ).getAuthenticationResponseJson();
            sendJson(callbackContext, authResponseJson);
          }

          @Override
          public void onError(@NonNull GetCredentialException e) {
            if (!finishCeremony(callbackContext)) {
              return;
            }
            sendError(callbackContext, e.getType(), e.getErrorMessage());
          }
        }
      );
    } catch (Exception e) {
      finishCeremony(callbackContext);
      sendError(callbackContext, ERROR_INVALID_OPTIONS, e.getMessage());
    }
  }

  private void createPasskey(
    String requestJson,
    CallbackContext callbackContext
  ) {
    Activity activity = getUsableActivity();
    if (activity == null) {
      sendError(
        callbackContext,
        ERROR_ACTIVITY_UNAVAILABLE,
        "The activity is not available."
      );
      return;
    }

    try {
      CredentialManager credentialManager = CredentialManager.create(activity);
      CreatePublicKeyCredentialRequest createRequest =
        new CreatePublicKeyCredentialRequest(requestJson);

      Executor mainExecutor = ContextCompat.getMainExecutor(activity);
      CancellationSignal cancellationSignal = startCeremony(callbackContext);

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
            if (!finishCeremony(callbackContext)) {
              return;
            }

            if (!(result instanceof CreatePublicKeyCredentialResponse)) {
              sendError(
                callbackContext,
                ERROR_UNSUPPORTED_RESPONSE,
                "Unsupported response type: " + result.getType()
              );
              return;
            }

            String registrationResponseJson = (
              (CreatePublicKeyCredentialResponse) result
            ).getRegistrationResponseJson();
            sendJson(callbackContext, registrationResponseJson);
          }

          @Override
          public void onError(@NonNull CreateCredentialException e) {
            if (!finishCeremony(callbackContext)) {
              return;
            }
            sendError(callbackContext, e.getType(), e.getErrorMessage());
          }
        }
      );
    } catch (Exception e) {
      finishCeremony(callbackContext);
      sendError(callbackContext, ERROR_INVALID_OPTIONS, e.getMessage());
    }
  }

  private static void sendJson(CallbackContext callbackContext, String json) {
    try {
      callbackContext.success(new JSONObject(json));
    } catch (JSONException e) {
      sendError(callbackContext, ERROR_INVALID_RESPONSE, e.getMessage());
    }
  }

  /**
   * Errors are sent as { type, message } so the JS side can distinguish a user
   * cancellation (e.g. "android.credentials.GetCredentialException.TYPE_USER_CANCELED")
   * from a real failure. CredentialManager exceptions often have a null message,
   * the type is always set.
   */
  private static void sendError(
    CallbackContext callbackContext,
    @Nullable String type,
    @Nullable CharSequence message
  ) {
    String safeType = type != null ? type : ERROR_UNKNOWN;
    String safeMessage = message != null ? message.toString() : safeType;

    JSONObject error = new JSONObject();
    try {
      error.put("type", safeType);
      error.put("message", safeMessage);
      callbackContext.error(error);
    } catch (JSONException e) {
      callbackContext.error(safeType + ": " + safeMessage);
    }
  }
}
