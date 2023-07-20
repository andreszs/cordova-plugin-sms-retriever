![npm](https://img.shields.io/npm/dt/cordova-plugin-sms-retriever) ![npm](https://img.shields.io/npm/v/cordova-plugin-sms-retriever) ![GitHub package.json version](https://img.shields.io/github/package-json/v/andreszs/cordova-plugin-sms-retriever?color=FF6D00&label=master&logo=github) ![GitHub code size in bytes](https://img.shields.io/github/languages/code-size/andreszs/cordova-plugin-sms-retriever) ![GitHub top language](https://img.shields.io/github/languages/top/andreszs/cordova-plugin-sms-retriever) ![GitHub](https://img.shields.io/github/license/andreszs/cordova-plugin-sms-retriever) ![GitHub last commit](https://img.shields.io/github/last-commit/andreszs/cordova-plugin-sms-retriever)

# cordova-plugin-sms-retriever

Cordova plugin to receive verification SMS in Android using the [SMS Retriever API](https://developers.google.com/identity/sms-retriever/overview).

# Prerequisites

This plugin requires the [Google Play Services 15.0.0](https://www.apkmirror.com/apk/google-inc/google-play-services/google-play-services-15-0-90-release/ "Google Play Services 15.0.0") or newer in order to work properly.

Minimum supported SDK version 24.
The target SDK version 33.


```xml
<platform name="android">
    ...
        <preference name="android-minSdkVersion" value="24" />
        <preference name="android-targetSdkVersion" value="33" />
    ...
</platform>
```

# Installation

- Install stable from NPM:
```bash
cordova plugin add cordova-plugin-sms-retriever
```
- Install master from GitHub:
```bash
cordova plugin add https://github.com/andreszs/cordova-plugin-sms-retriever
```
- Create your project and Android app in [Firebase Console](https://console.firebase.google.com/ "Firebase Console")
- Download the **google-services.json** file into your **platforms/android** folder.
- Make sure to sign your build with a keystore file.

## Ionic

### Dev version:

```bash
 npm i awesome-cordova-plugins-sms-retriever-api
```

### Prod version: https://github.com/danielsogl/awesome-cordova-plugins/pull/4528

```bash
 npm i @awesome-cordova-plugins/sms-retriever-api
```

## Ionic Demo
https://github.com/MaximBelov/cordova-plugin-sms-retriever-lab


# Methods

## getPhoneNumber

Opens a dialog to select your mobile numbers saved in phone and [returns selected phone number](https://developers.google.com/identity/sms-retriever/request#1_obtain_the_users_phone_number).


```javascript

var onSuccess = function (strSuccess) {
  console.log(strSuccess);
};
var onFail = function (strError) {
  console.log(strError);
};
cordova.plugins.SMSRetriever.getPhoneNumber(onSuccess, onFail);

```

## startWatch
Start listening for a single incoming [verification SMS](https://developers.google.com/identity/sms-retriever/verify#1_construct_a_verification_message "verification SMS") for 5 minutes.

```javascript
cordova.plugins.SMSRetriever.startWatch(successCallback, errorCallback);
```

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

## stopWatch
Stops listening for a single incoming verification SMS

### Return values

- **SMS_RETRIEVER_DONE**

### Example

```javascript
var onSuccess = function (strSuccess) {
  console.log(strSuccess);
};
var onFail = function (strError) {
  console.log(strError);
};
cordova.plugins.SMSRetriever.stopWatch(onSuccess, onFail);
```

## getHashString

Get the 11-character hash string for your app using the [AppSignatureHelper](https://github.com/googlesamples/android-credentials/blob/master/sms-verification/android/app/src/main/java/com/google/samples/smartlock/sms_verify/AppSignatureHelper.java "AppSignatureHelper") class. This string must be appended to the SMS received in order for the API to read this message.

```javascript
cordova.plugins.SMSRetriever.getHashString(successCallback, errorCallback);
```

:warning: Method moved from **window** to **cordova.plugins** object in version 2.0.0



### Remarks

- The hash will be different from debug and release builds, since they have different signatures.
- Play Store now re-signs signed APKs on upload. This will most certainly change the hash string.
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

### Remarks

- If the SMS is not retrieved in your debug build, try the signed production APK.

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
- Begin with the prefix <#>
- Contain a one-time code that the client sends back to your server to complete the verification flow
- End with the 11-character hash string that identifies your app

### Remarks

- Starting from **plugin 2.0.0**, the <#> prefix is no longer required by the plugin.
- Starting from an unknown **Play Services** version, the <#> is no longer required in the received SMS.

Otherwise, the contents of the verification message can be whatever you choose. It is helpful to create a message from which you can easily extract the one-time code later on. For example, a valid verification message might look like the following:

    <#> AZC123 is your code for andreszsogon.com SMS Retriever Demo App. hi5c8+bkQy0

:information_source: It is a good practice to prepend the verification code to the beginning of the SMS, in case the retriever fails, the user can see the code immediately from the notification bar.

# Plugin demo app

- [Compiled APK and reference](https://www.andreszsogon.com/cordova-sms-retriever-plugin-demo-app/) including testing procedure instructions
- [Source code for www folder](https://github.com/andreszs/cordova-plugin-demos)

<img src="https://github.com/andreszs/cordova-plugin-demos/blob/main/com.andreszs.smsretriever.demo/screenshots/sms_retriever_demo_2.png?raw=true" width="240" /> <img src="https://github.com/andreszs/cordova-plugin-demos/blob/main/com.andreszs.smsretriever.demo/screenshots/sms_retriever_demo_3.png?raw=true" width="240" /> <img src="https://github.com/andreszs/cordova-plugin-demos/blob/main/com.andreszs.smsretriever.demo/screenshots/sms_retriever_demo_4.png?raw=true" width="240" />

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

### When was the <#> SMS prefix removed?

The <#> prefix formerly required to retrieve the SMS was silently removed in an unknown Play Services version and no longer appears in the SMS Retriever API docs.

# Changelog

### 4.0.1

- Restored support for SDK versions: minimum 21, target 31.
- Tested on Android 5.1, 6.0, 13, 14.

### 4.0.0

- Added support autofill for [Android14](https://developer.android.com/about/versions/14/behavior-changes-14#runtime-receivers-exported)
- BREAKING CHANGES: SDK versions: minimum - 24 and target - 33.

### 3.0.0

- Added method stopWatch 
- Added method getPhoneNumber 

### 2.0.1

- Updated README with important details about SMS generation

### 2.0.0
- :warning: Methods moved from the global **window** to the **cordova.plugins** object
- Improved all methods return values to make them easier to parse
- Removed the requirement include the <#> prefix in the SMS
- Improved stability and error checking
- Updated demo app, now available in Play Store
