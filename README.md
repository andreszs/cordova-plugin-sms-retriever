| License | Platform | Contribute |
| --- | --- | --- |
| ![License](https://img.shields.io/badge/license-MIT-orange.svg) | ![Platform](https://img.shields.io/badge/platform-android-green.svg) | [![Donate](https://img.shields.io/badge/donate-PayPal-green.svg)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=G33QACCVKYD7U) |

# cordova-plugin-sms-retriever

Cordova plugin to receive verification SMS in Android using the [SMS Retriever API](https://developers.google.com/identity/sms-retriever/overview).

# Prerequisites

This plugin requires the [Google Play Services 15.0.0](https://www.apkmirror.com/apk/google-inc/google-play-services/google-play-services-15-0-90-release/ "Google Play Services 15.0.0") or newer in order to work properly.

# Installation

- Add the plugin from NPM:
```bash
cordova plugin add cordova-plugin-sms-retriever
```
- Create your project and Android app in [Firebase Console](https://console.firebase.google.com/ "Firebase Console")
- Download the **google-services.json** file into your **platforms/android** folder.
- Make sure to sign your build with a keystore file.

# Methods

## startWatch
Start listening for a single incoming [verification SMS](https://developers.google.com/identity/sms-retriever/verify#1_construct_a_verification_message "verification SMS") for 5 minutes.

:warning:  Method moved from **window** to **cordova.plugins** object in version 2.0.0

- When a valid SMS is intercepted, the **onSMSArrive** event is fired and SMS watching is stopped.
- When the 5 minutes timeout is reached, SMS watching is stopped and the **failureCallback** returns **TIMEOUT**.

### Return values

- **SMS_RETRIEVER_STARTED**: Retriever started and waiting for incoming SMS.
- **SMS_RETRIEVER_ALREADY_STARTED**: Your  5 minutes for SMS retrieval are already running and won’t be reset by calling this method again!.
- **SMS_RETRIEVER_DONE**: Second callback, triggered when an SMS was intercepted.

When the SMS is returned, the retriever API is automatically stopped and no further messages will be intercepted until you start a new one. This is by API design, not a plugin or a demo app restriction.

### Example

```javascript
var onSuccess = function (strSuccess) {
	console.log(strSuccess);
};
var onFail = function (strError) {
	console.log(strError);
};
cordova.plugins.SMSRetriever.startWatch(onSuccess, onFail);
```

## getHashString

Get the 11-character hash string for your app using the [AppSignatureHelper](https://github.com/googlesamples/android-credentials/blob/master/sms-verification/android/app/src/main/java/com/google/samples/smartlock/sms_verify/AppSignatureHelper.java "AppSignatureHelper") class. This string must be appended to the SMS received in order for the API to read this message. 

:warning: Method moved from **window** to **cordova.plugins** object in version 2.0.0

### Remarks

- The hash will be different from debug and release builds, since they have different signatures.
- Calling this method with an active SMS retriever running will **void**  the retriever and the SMS wont be incercepted.
- Google advices against dynamically retrieving your hash code before sending the SMS:

> Do not use hash strings dynamically computed on the client in your verification messages.

Therefore, **do not** invoke this method from the published app. The hash is the same for all users, and bound to your keystore signing keys, so you can get it once and never again call this method.

### Return values

- *The 11-digit hash string for sending validation SMS.*

### Example

```javascript
var onSuccess = function (strHash) {
	console.log(strHash);
};
var onFail = function (strError) {
	console.log(strError);
};
cordova.plugins.SMSRetriever.getHashString(onSuccess , onFail);
```

# Events

## onSMSArrive

Event fired when a valid [verification SMS](https://developers.google.com/identity/sms-retriever/verify#1_construct_a_verification_message "verification SMS") with the hash string has arrived. You need call **startWatch()** first.

### Example

```javascript
document.addEventListener('onSMSArrive', function(args) {
	// SMS retrieved, get its contents
	console.info(args.message);
	// To Do: Extract the received one-time code and verify it on your server
});
```

# Construct a verification SMS

The verification SMS message you send to the user must:

- Be no longer than 140 bytes
- ~~Begin with the prefix <#>~~  *No longer needed since plugin version **2.0.0***
- Contain a one-time code that the client sends back to your server to complete the verification flow
- End with the 11-character hash string that identifies your app

Otherwise, the contents of the verification message can be whatever you choose. It is helpful to create a message from which you can easily extract the one-time code later on. For example, a valid verification message might look like the following:

    AZC123 is your code for andreszsogon.com SMS Retriever Demo App. hi5c8+bkQy0

:information_source: It is a good practice to prepend the verification code to the beginning of the SMS, in case the retriever fails, the user can see the code immediately from the notification bar.

# Demo App by Andrés Zsögön

You can download the [SMS Retriever plugin demo app](https://www.andreszsogon.com/cordova-sms-retriever-plugin-demo-app/) from the Play Store; its source code is provided in the **demo** folder.

[![ScreenShot](https://www.andreszsogon.com/wp-content/uploads/Screenshot_1650660474-165x300.png)](https://www.andreszsogon.com/cordova-sms-retriever-plugin-demo-app/ "![ScreenShot](https://www.andreszsogon.com/wp-content/uploads/Screenshot_1650660474-165x300.png)") [![ScreenShot](https://www.andreszsogon.com/wp-content/uploads/Screenshot_1650660479-165x300.png)](https://www.andreszsogon.com/cordova-sms-retriever-plugin-demo-app/ "![ScreenShot](https://www.andreszsogon.com/wp-content/uploads/Screenshot_1650660479-165x300.png)") [![ScreenShot](https://www.andreszsogon.com/wp-content/uploads/Screenshot_1650660603-165x300.png)](https://www.andreszsogon.com/cordova-sms-retriever-plugin-demo-app/ "![ScreenShot](https://www.andreszsogon.com/wp-content/uploads/Screenshot_1650660603-165x300.png)")

# FAQ

### Does the plugin work in the Android emulator?

The plugin will work in your emulator as long as you are using a **Google Play** ABI or System Image, instead of the regular Google APIs ones. This is because these images include the Google Play Store and Google Play Services.

### Does the plugin still work with the app minimized?

When the app is sent to the background, as long as Android has not unloaded it to recover memory, SMS watching will remain active and working correctly for 5 minutes.

### Does the plugin require SMS permissions?

**No**, the plugin does not require any permission because it relies on the [SMS Retriever API](https://developers.google.com/identity/sms-retriever/overview "SMS Retriever API").

### Does the plugin work in debug APK?

In the emulator you can test the plugin using the unsigned debug APK. Real devices require the production APK to work.

### Can I get the hash string dynamically?

Google advices against [computing the hash string](https://developers.google.com/identity/sms-retriever/verify#computing_your_apps_hash_string "computing the hash string") in the client for security concerns. Get the hash string in advance and then do not call the get hash method again in the final production app.

# Changelog

### 2.0.0
- :warning: Methods moved from the global **window** to the **cordova.plugins** object
- Improved all methods return values to make them easier to parse
- Removed the requirement include the <#> prefix in the SMS
- Improved stability and error checking
- Updated demo app, now available in Play Store

### 1.1.1
- Removed the `cordova-support-google-services` plugin dependency which is no longer required

### 1.1.0
- Added `cordova >= 7.1.0` engine to config.xml
- Added `cordova-android >= 6.3.0` engine to config.xml
- Added missing `com.google.android.gms:play-services-auth` framework to config.xml
- Bumped `PLAY_SERVICES_VERSION` to 15.0.0 in config.xml
- **Notice**: This plugin requires `classpath com.android.tools.build:gradle:4.0.1` in build.gradle, otherwise your build will probably fail with Cordova 9.0.0.

