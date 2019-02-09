/* package com.andreszs.cordova.sms; */
package com.andreszs.smsretriever;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Telephony;
import android.util.Log;
import android.widget.Toast;

//import java.security.MessageDigest;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;

import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.auth.api.phone.SmsRetrieverClient;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;


public class SMSRetriever extends CordovaPlugin {

	private SmsRetrieverClient smsRetrieverClient;
	private SmsBrReceiver smsReceiver;
	private static final String TAG = "cordova-plugin-sms-retriever";
	private static final String ACTION_START_WATCH = "startWatch";
	private static final String ACTION_STOP_WATCH = "stopWatch";
	//private static final String SMS_RECEIVED_ACTION = "android.provider.Telephony.SMS_RECEIVED";

	//private JSONArray requestArgs;
	private CallbackContext callbackContext;


	/**
	* Sets the context of the Command. This can then be used to do things like
	* get file paths associated with the Activity.
	*
	* @param cordova The context of the main Activity.
	* @param webView The CordovaWebView Cordova is running in.
	*/
	@Override
	public void initialize(CordovaInterface cordova, CordovaWebView webView) {
		Log.d(TAG, "initialize");
		super.initialize(cordova, webView);
		Context applicationContext = cordova.getActivity().getApplicationContext();

		// Get an instance of SmsRetrieverClient, used to start listening for a matching SMS message.
		SmsRetrieverClient client = SmsRetriever.getClient(Context);
	}

	public boolean execute(String action, JSONArray inputs, CallbackContext callbackContext) throws JSONException {
		PluginResult result = null;
		this.callbackContext = callbackContext;
		this.requestArgs = inputs;
		if (action.equals(ACTION_START_WATCH)) {
			result = this.startWatch(callbackContext);
		} else if (action.equals(ACTION_STOP_WATCH)) {
			result = this.stopWatch(callbackContext);
		} else {
			Log.d(TAG, String.format("Invalid action passed: %s", action));
			result = new PluginResult(PluginResult.Status.INVALID_ACTION);
		}
		if (result != null) {
			callbackContext.sendPluginResult(result);
		}
		return true;
	}

	public void onDestroy() {
		this.stopWatch(null);
	}

	private PluginResult startWatch(CallbackContext callbackContext) {
		Log.d(TAG, ACTION_START_WATCH);

		// Starts SmsRetriever, which waits for ONE matching SMS message until timeout
		// (5 minutes). The matching SMS message will be sent via a Broadcast Intent with
		// action SmsRetriever#SMS_RETRIEVED_ACTION.
		Task<Void> task = smsRetrieverClient.startSmsRetriever();

		// Listen for success/failure of the start Task.
		task.addOnSuccessListener(new OnSuccessListener<Void>() {
			@Override
			public void onSuccess(Void aVoid) {
				// Successfully started retriever, expect broadcast intent
				Log.d(TAG, "SmsRetrievalResult started successfully");
				//Toast.makeText(cordova.getActivity().getApplicationContext(), getString(R.string.verifier_registered), Toast.LENGTH_SHORT).show();
				Toast.makeText(cordova.getActivity().getApplicationContext(), "SmsRetrievalResult started successfully", Toast.LENGTH_SHORT).show();
				callbackContext.success("SmsRetrievalResult started successfully");

				// Use a BroadcastReceiver to receive the verification message.
				IntentFilter intentFilter = new IntentFilter();
				intentFilter.addAction(SmsRetriever.SMS_RETRIEVED_ACTION);
				cordova.getActivity().getApplicationContext().registerReceiver(SmsBrReceiver, intentFilter);
			}
		});
		task.addOnFailureListener(new OnFailureListener() {
			@Override
			public void onFailure(@NonNull Exception e) {
				// Failed to start retriever, inspect Exception for more details
				Log.d(TAG, "SmsRetrievalResult start failed.", e);
				Toast.makeText(cordova.getActivity().getApplicationContext(), "startSmsRetriever error", Toast.LENGTH_SHORT).show();
				callbackContext.error(e);
			}
		});

		return null;
	}

	private PluginResult stopWatch(CallbackContext callbackContext) {
		Log.d(TAG, ACTION_STOP_WATCH);

		if (this.SmsBrReceiver != null) {
			try {
				webView.getContext().unregisterReceiver(this.SmsBrReceiver);
				callbackContext.success();
			} catch (Exception e) {
				Log.d(LOG_TAG, "error unregistering network receiver: " + e.getMessage());
				callbackContext.error(e);
			} finally {
				this.SmsBrReceiver = null;
			}
		}

		return null;
	}

	/*
	private void onSMSArrive(JSONObject json) {
		webView.loadUrl("javascript:try{cordova.fireDocumentEvent('onSMSArrive', {'data': "+json+"});}catch(e){console.log('exception firing onSMSArrive event from native');};");
	}
	*/

	/**
	* BroadcastReceiver to wait for SMS messages. This can be registered either
	* in the AndroidManifest or at runtime. Should filter Intents on
	* SmsRetriever.SMS_RETRIEVED_ACTION.
	*/
	private BroadcastReceiver SmsBrReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (SmsRetriever.SMS_RETRIEVED_ACTION.equals(intent.getAction())) {
				Bundle extras = intent.getExtras();
				Status status = (Status) extras.get(SmsRetriever.EXTRA_STATUS);

				switch(status.getStatusCode()) {
					case CommonStatusCodes.SUCCESS:
						// Get SMS message contents
						String smsMessage = (String) extras.get(SmsRetriever.EXTRA_SMS_MESSAGE);
						Log.d(TAG, "Retrieved sms code: " + smsMessage);
						// Extract one-time code from the message and complete verification by sending the code back to your server.

						Toast.makeText(cordova.getActivity().getApplicationContext(), smsMessage, Toast.LENGTH_LONG).show();
						PluginResult result = new PluginResult(PluginResult.Status.OK, smsMessage);
						this.callbackContext.sendPluginResult(result);

						break;

					case CommonStatusCodes.TIMEOUT:
						// Waiting for SMS timed out (5 minutes)
						Toast.makeText(cordova.getActivity().getApplicationContext(), "TIMEOUT ERROR", Toast.LENGTH_LONG).show();
						PluginResult resultTimeout = new PluginResult(PluginResult.Status.ERROR, "TIMEOUT");
						this.callbackContext.sendPluginResult(resultTimeout);

						break;
				}

			}
		}


	};

}