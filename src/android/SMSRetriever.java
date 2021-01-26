package com.andreszs.smsretriever;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;

import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.Task;

public class SMSRetriever extends CordovaPlugin {

    private CallbackContext callbackContext;

    private static final String TAG = "cordova-plugin-sms-api";
    private static final String ACTION_START_WATCH = "startWatch";

    private static final int SMS_CONSENT_REQUEST = 2;  // Set to an unused request code


    /**
     * @param cordova The context of the main Activity.
     * @param webView The CordovaWebView Cordova is running in.
     */
    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        Log.d(TAG, "initialize");
        super.initialize(cordova, webView);
    }


    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        PluginResult result = null;
        this.callbackContext = callbackContext;
        this.cordova.setActivityResultCallback(this);
        if (action.equals(ACTION_START_WATCH)) {
            this.startWatch(callbackContext);
        } else {
            Log.d(TAG, String.format("Invalid action passed: %s", action));
            result = new PluginResult(PluginResult.Status.INVALID_ACTION);
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);
        }
        return true;
    }

    public void onDestroy() {
        unregisterBroadcastReceiver();
    }

    private void startWatch(final CallbackContext callbackContext) {
        Log.d(TAG, ACTION_START_WATCH);

        try {
            PluginResult result = new PluginResult(PluginResult.Status.OK, "Start watching for SMS ...");
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);
            // Start listening for SMS User Consent broadcasts from senderPhoneNumber
            // The Task<Void> will be successful if SmsRetriever was able to start
            // SMS User Consent, and will error if there was an error starting.
            // If you know the phone number from which the SMS message will originate, specify it (otherwise, pass null).
            Task<Void> task = SmsRetriever.getClient(cordova.getActivity().getApplicationContext()).startSmsUserConsent(null);

            IntentFilter intentFilter = new IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION);
            cordova.getActivity().getApplicationContext().registerReceiver(smsVerificationReceiver, intentFilter, SmsRetriever.SEND_PERMISSION, null);

        } catch (Exception e) {
            PluginResult result = new PluginResult(PluginResult.Status.ERROR, e.getMessage());
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);
            unregisterBroadcastReceiver();
        }
    }

    private BroadcastReceiver smsVerificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (SmsRetriever.SMS_RETRIEVED_ACTION.equals(intent.getAction())) {
                Bundle extras = intent.getExtras();
                Status smsRetrieverStatus = (Status) extras.get(SmsRetriever.EXTRA_STATUS);

                switch (smsRetrieverStatus.getStatusCode()) {
                    case CommonStatusCodes.SUCCESS:
                        // Get consent intent
                        Intent consentIntent = extras.getParcelable(SmsRetriever.EXTRA_CONSENT_INTENT);
                        try {
                            // Start activity to show consent dialog to user, activity must be started in
                            // 5 minutes, otherwise you'll receive another TIMEOUT intent
                            cordova.getActivity().startActivityForResult(consentIntent, SMS_CONSENT_REQUEST);
                        } catch (Exception e) {
                            // Handle the exception ...
                            Log.e(TAG, "Error starting activity for result: " + e.getMessage());
                            unregisterBroadcastReceiver();
                        }
                        break;
                    case CommonStatusCodes.TIMEOUT:
                        // Time out occurred, handle the error.
                        Log.e(TAG, "Error Timeout: ");
                        unregisterBroadcastReceiver();
                        break;
                }
            }
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case SMS_CONSENT_REQUEST:
                if (resultCode == Activity.RESULT_OK) {
                    // Get SMS message content
                    String message = data.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE);
                    // Extract one-time code from the message and complete verification
                    // `sms` contains the entire text of the SMS message, so you will need
                    // to parse the string.
                    Log.d(TAG, "Retrieved SMS: " + message);
                    PluginResult result = new PluginResult(PluginResult.Status.OK, message);
                    result.setKeepCallback(true);
                    callbackContext.sendPluginResult(result);

                    // send one time code to the server

                } else {
                    // Consent canceled, handle the error ...
                    Log.e(TAG, "ERROR");
                    PluginResult result = new PluginResult(PluginResult.Status.ERROR, "CONSENT ERROR");
                    result.setKeepCallback(true);
                    callbackContext.sendPluginResult(result);
                }
                unregisterBroadcastReceiver();
                break;
        }
    }

    private void unregisterBroadcastReceiver() {
        if (smsVerificationReceiver != null) {
            try {
                cordova.getActivity().getApplicationContext().unregisterReceiver(smsVerificationReceiver);
                smsVerificationReceiver = null;
                Log.d(TAG, "SMS Retriever unregistered successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering network receiver: " + e.getMessage());
            }
        }
    }
}
