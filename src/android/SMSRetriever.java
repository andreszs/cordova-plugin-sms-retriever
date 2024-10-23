package com.andreszs.smsretriever;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.identity.GetPhoneNumberHintIntentRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.auth.api.phone.SmsRetrieverClient;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;

public class SMSRetriever extends CordovaPlugin implements SMSBroadcastReceiver.OTPReceiveListener {

    private static String LOG_TAG;
    private static final int RESOLVE_HINT = 234433;
    private static GoogleApiClient apiClient;

    private Context applicationContext;
    private CallbackContext startWatchCallbackContext;
    private CallbackContext getPhoneNumberCallbackContext;
    private SMSBroadcastReceiver smsBroadcastReceiver;

    private static final String ACTION_START_WATCH = "startWatch";
    private static final String ACTION_GET_SIGNATURE = "getHashString";
    private static final String ACTION_STOP_WATCH = "stopWatch";
    private static final String ACTION_GET_PHONE_NUMBER = "getPhoneNumber";

    public SMSRetriever() {
        LOG_TAG = SMSRetriever.class.getSimpleName();
        Log.i(LOG_TAG, "Constructed");
    }

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        applicationContext = cordova.getActivity().getApplicationContext();
    }

    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        Log.i(LOG_TAG, String.format("Executing the %s action started", action));

        switch (action) {
            case ACTION_START_WATCH:
                this.startWatch(callbackContext);
                break;
            case ACTION_STOP_WATCH:
                this.stopWatch(callbackContext);
                break;
            case ACTION_GET_SIGNATURE:
                this.getHashString(callbackContext);
                break;
            case ACTION_GET_PHONE_NUMBER:
                this.getPhoneNumber(callbackContext);
                break;
            default:
                Log.w(LOG_TAG, String.format("Invalid action passed: %s", action));
                PluginResult result = new PluginResult(PluginResult.Status.INVALID_ACTION);
                callbackContext.sendPluginResult(result);
                break;
        }
        return true;
    }

    public void onDestroy() {
        this.unregisterListeners();
    }

    public void onSmsReceived(String smsMessage) {
        Log.w(LOG_TAG, "SMS_RECEIVED");
        Log.w(LOG_TAG, smsMessage);
        try {
            JSONObject item = new JSONObject();
            item.put("sms", smsMessage);
            PluginResult result = new PluginResult(PluginResult.Status.OK, item);
            startWatchCallbackContext.sendPluginResult(result);
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            startWatchCallbackContext.error(e.getMessage());
        }
        this.unregisterListeners();
    }

    public void onSmsReceiveTimeOut() {
        Log.w(LOG_TAG, "TIMEOUT");
        PluginResult resultTimeout = new PluginResult(PluginResult.Status.ERROR, "TIMEOUT");
        startWatchCallbackContext.sendPluginResult(resultTimeout);
        this.unregisterListeners();
    }

    public void onSmsReceivedError(String error) {
        PluginResult result = new PluginResult(PluginResult.Status.ERROR, error);
        startWatchCallbackContext.sendPluginResult(result);
        this.unregisterListeners();
    }

    private void startWatch(final CallbackContext callbackContext) {
        if (startWatchCallbackContext != null) {
            PluginResult result = new PluginResult(PluginResult.Status.OK, "SMS_RETRIEVER_ALREADY_STARTED");
            callbackContext.sendPluginResult(result);
            return;
        }
        // Starts SmsRetriever, which waits for ONE matching SMS message until timeout
        // (5 minutes). The matching SMS message will be sent via a Broadcast Intent with
        // action SmsRetriever#SMS_RETRIEVED_ACTION.
        startWatchCallbackContext = callbackContext;
        try {
            smsBroadcastReceiver = new SMSBroadcastReceiver();
            smsBroadcastReceiver.setOTPListener(this);

            // Get an instance of SmsRetrieverClient, used to start listening for a matching SMS message.
            SmsRetrieverClient smsRetrieverClient = SmsRetriever.getClient(applicationContext);
            Task<Void> task = smsRetrieverClient.startSmsRetriever();

            // Listen for success/failure of the start Task.
            task.addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    // Successfully started retriever, expect broadcast intent
                    Log.i(LOG_TAG, "smsRetrieverClient started successfully");

                    // Use a BroadcastReceiver to receive the verification message.
                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction(SmsRetriever.SMS_RETRIEVED_ACTION);

                    try {
                        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S) {
                            try {
								// Access the RECEIVER_EXPORTED constant through reflection
								int receiverExportedFlag = Context.class.getField("RECEIVER_EXPORTED").getInt(null);
								applicationContext.registerReceiver(smsBroadcastReceiver, intentFilter, receiverExportedFlag);
							} catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException e) {
								// In case of any exceptions, fallback to registering without the flag
								applicationContext.registerReceiver(smsBroadcastReceiver, intentFilter);
							}
                        } else {
                            applicationContext.registerReceiver(smsBroadcastReceiver, intentFilter);
                        }	
                        PluginResult result = new PluginResult(PluginResult.Status.OK, "SMS_RETRIEVER_STARTED");
                        result.setKeepCallback(true);
                        startWatchCallbackContext.sendPluginResult(result);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, e.getMessage());
                        PluginResult result = new PluginResult(PluginResult.Status.ERROR, e.getMessage());
                        startWatchCallbackContext.sendPluginResult(result);
                    }
                }
            });
            task.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    // Failed to start retriever, inspect Exception for more details
                    Log.e(LOG_TAG, e.getMessage());
                    PluginResult result = new PluginResult(PluginResult.Status.ERROR, e.getMessage());
                    startWatchCallbackContext.sendPluginResult(result);
                }
            });
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
            PluginResult result = new PluginResult(PluginResult.Status.ERROR, e.getMessage());
            startWatchCallbackContext.sendPluginResult(result);
        }
    }

    private void stopWatch(final CallbackContext callbackContext) {
        callbackContext.success();
        if (startWatchCallbackContext != null) {
            startWatchCallbackContext.success("SMS_RETRIEVER_DONE");
        }
        this.unregisterListeners();
    }

    private void unregisterListeners() {
        if (smsBroadcastReceiver != null) {
            try {
                applicationContext.unregisterReceiver(smsBroadcastReceiver);
                smsBroadcastReceiver = null;
                Log.i(LOG_TAG, "SMS Retriever unregistered successfully");
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());
            }
        }

        if (startWatchCallbackContext != null) {
            startWatchCallbackContext = null;
        }
    }

    private void getPhoneNumber(final CallbackContext callbackContext) {
        this.getPhoneNumberCallbackContext = callbackContext;
        FragmentActivity activity = cordova.getActivity();
        if (apiClient == null) {
            apiClient = new GoogleApiClient.Builder(activity)
                    .addApi(Auth.GOOGLE_SIGN_IN_API)
                    .build();
        }

        cordova.setActivityResultCallback(this);

        GetPhoneNumberHintIntentRequest request = GetPhoneNumberHintIntentRequest.builder().build();
        Identity.getSignInClient(activity)
                .getPhoneNumberHintIntent(request)
                .addOnSuccessListener( result -> {
                    try {
                        activity.startIntentSenderForResult(result.getIntentSender(), RESOLVE_HINT, null, 0, 0, 0);
                    } catch(Exception e) {
                        String message = "Launching the PendingIntent failed";
                        getPhoneNumberCallbackContext.error(message);
                        Log.e(LOG_TAG, message, e);
                    }
                })
                .addOnFailureListener(e -> {
                    getPhoneNumberCallbackContext.error("Phone Number Hint disabled. Enable: Settings -> Google -> Autofill -> Phone number sharing");
                    Log.e(LOG_TAG, "Phone Number Hint disabled", e);
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        try {
            if (requestCode == RESOLVE_HINT) {
                if (resultCode == Activity.RESULT_OK) {
                    FragmentActivity activity = cordova.getActivity();
                    String phoneNumber = Identity.getSignInClient(activity).getPhoneNumberFromIntent(intent);
                    getPhoneNumberCallbackContext.success(phoneNumber);
                } else {
                    throw new NullPointerException("Phone number not selected");
                }

            }
        } catch (ApiException | NullPointerException ex) {
            getPhoneNumberCallbackContext.error(ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Google Play services uses the hash string to determine which verification messages to send to your app. The hash string is made of your app's package name and your app's public key certificate. https://developers.google.com/identity/sms-retriever/verify#computing_your_apps_hash_string
     */
    private void getHashString(final CallbackContext callbackContext) {
        ArrayList<String> appCodes = new AppSignatureHashHelper(applicationContext).getAppSignatures();
        if (appCodes.size() == 0 || appCodes.get(0) == null) {
            String err = "Unable to find package to obtain hash";
            PluginResult result = new PluginResult(PluginResult.Status.ERROR, err);
            callbackContext.sendPluginResult(result);
        } else {
            String hash = appCodes.get(0);
            PluginResult result = new PluginResult(PluginResult.Status.OK, hash);
            callbackContext.sendPluginResult(result);
        }
    }
}
