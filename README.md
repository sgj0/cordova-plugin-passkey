# Cordova Plugin for accessing the Credentials Manager API (Android Only)

Create and use passkeys from a Cordova Android app through
[`androidx.credentials`](https://developer.android.com/identity/sign-in/credential-manager).

Requirements: `cordova-android >= 13`.

## API

```js
cordova.plugins.passkey.createPasskey(options, onSuccess, onError);
cordova.plugins.passkey.getPasskey(options, onSuccess, onError);
```

- `options`: WebAuthn `PublicKeyCredentialCreationOptionsJSON` /
  `PublicKeyCredentialRequestOptionsJSON` (binary fields base64url encoded),
  given either as an object or as a JSON string.
- `onSuccess(credential)`: the `RegistrationResponseJSON` /
  `AuthenticationResponseJSON` object returned by the Credential Manager, ready
  to be sent to the relying party.
- `onError({ type, message })`: `type` is the Credential Manager exception type
  (for example `android.credentials.GetCredentialException.TYPE_USER_CANCELED`,
  `androidx.credentials.TYPE_NO_CREDENTIAL`, ...) or one of the plugin types:
  `INVALID_OPTIONS`, `ACTIVITY_UNAVAILABLE`, `UNSUPPORTED_RESPONSE`,
  `INVALID_RESPONSE`, `CANCELED` (pending request replaced, WebView reloaded or
  activity destroyed).

Only one request can be displayed at a time: starting a new one cancels the
previous one (its error callback receives `CANCELED`).

## Relying party setup

The origin sent by Android is `android:apk-key-hash:<base64url(sha256(signing cert))>`.
The RP must:

1. publish `https://<rp id>/.well-known/assetlinks.json` with the
   `delegate_permission/common.get_login_creds` relation for the app package and
   its signing certificate fingerprint;
2. accept the `android:apk-key-hash:...` origin when verifying `clientDataJSON`.
