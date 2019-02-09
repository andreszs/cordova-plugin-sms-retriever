package com.andreszs.smsretriever;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
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
import com.google.android.gms.auth.api.phone.SmsRetrieverClient;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

/* AppSignatureHelper */
import android.content.ContextWrapper;
import android.content.pm.Signature;
import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

public class SMSRetriever extends CordovaPlugin {

	private CallbackContext callbackContext;
	private SmsRetrieverClient smsRetrieverClient;

	private static final String TAG = "cordova-plugin-sms-retriever";
	private static final String ACTION_START_WATCH = "startWatch";
	private static final String ACTION_GET_SIGNATURE = "getHashString";

	/* AppSignatureHelper */
    private static final String HASH_TYPE = "SHA-256";
    public static final int NUM_HASHED_BYTES = 9;
	public static final int NUM_BASE64_CHAR = 11;

	/**
	* @param cordova The context of the main Activity.
	* @param webView The CordovaWebView Cordova is running in.
	*/
	@Override
	public void initialize(CordovaInterface cordova, CordovaWebView webView) {
		Log.d(TAG, "initialize");
		super.initialize(cordova, webView);

		// Get an instance of SmsRetrieverClient, used to start listening for a matching SMS message.
		smsRetrieverClient = SmsRetriever.getClient(cordova.getActivity().getApplicationContext());
	}

	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		PluginResult result = null;
		this.callbackContext = callbackContext;
		if (action.equals(ACTION_START_WATCH)) {
			this.startWatch(callbackContext);
		}else if (action.equals(ACTION_GET_SIGNATURE)) {
			this.getHashString(callbackContext);
		} else {
			Log.d(TAG, String.format("Invalid action passed: %s", action));
			result = new PluginResult(PluginResult.Status.INVALID_ACTION);
			callbackContext.sendPluginResult(result);
		}
		return true;
	}

	public void onDestroy() {
		if (SmsBrReceiver != null) {
			try {
				cordova.getActivity().getApplicationContext().unregisterReceiver(SmsBrReceiver);
				SmsBrReceiver = null;
				Log.d(TAG, "SMS Retriever unregistered successfully");
			} catch (Exception e) {
				Log.e(TAG, "Error unregistering network receiver: " + e.getMessage());
			}
		}
	}

	private void startWatch(final CallbackContext callbackContext) {
		Log.d(TAG, ACTION_START_WATCH);

		// Starts SmsRetriever, which waits for ONE matching SMS message until timeout
		// (5 minutes). The matching SMS message will be sent via a Broadcast Intent with
		// action SmsRetriever#SMS_RETRIEVED_ACTION.
		try {
			Task<Void> task = smsRetrieverClient.startSmsRetriever();

			// Listen for success/failure of the start Task.
			task.addOnSuccessListener(new OnSuccessListener<Void>() {
				@Override
				public void onSuccess(Void aVoid) {
					// Successfully started retriever, expect broadcast intent
					Log.d(TAG, "smsRetrieverClient started successfully");

					// Use a BroadcastReceiver to receive the verification message.
					IntentFilter intentFilter = new IntentFilter();
					intentFilter.addAction(SmsRetriever.SMS_RETRIEVED_ACTION);

					try {
						cordova.getActivity().getApplicationContext().registerReceiver(SmsBrReceiver, intentFilter);
						PluginResult result = new PluginResult(PluginResult.Status.OK, "SMS Retriever started OK for 5 minutes");
						result.setKeepCallback(true);
						callbackContext.sendPluginResult(result);
					} catch (Exception e) {
						PluginResult result = new PluginResult(PluginResult.Status.ERROR, e.getMessage());
						callbackContext.sendPluginResult(result);
					}
				}
			});
			task.addOnFailureListener(new OnFailureListener() {
				@Override
				public void onFailure(Exception e) {
					// Failed to start retriever, inspect Exception for more details
					Log.d(TAG, "smsRetrieverClient start failed.", e);

					PluginResult result = new PluginResult(PluginResult.Status.ERROR, e.getMessage());
					callbackContext.sendPluginResult(result);
				}
			});

		} catch (Exception e) {
			PluginResult result = new PluginResult(PluginResult.Status.ERROR, e.getMessage());
			callbackContext.sendPluginResult(result);
		}
	}

	/**
	 * Google Play services uses the hash string to determine which verification messages to send to your app.
	 * The hash string is made of your app's package name and your app's public key certificate.
	 * https://developers.google.com/identity/sms-retriever/verify#computing_your_apps_hash_string
	 */
	private void getHashString(final CallbackContext callbackContext){

		ArrayList<String> appCodes = this.getAppSignatures();

		if(appCodes.size() == 0 || appCodes.get(0) == null){

			String err = "Unable to find package to obtain hash";
			PluginResult result = new PluginResult(PluginResult.Status.ERROR, err);
			callbackContext.sendPluginResult(result);

		}else{

			String hash = appCodes.get(0);
			PluginResult result = new PluginResult(PluginResult.Status.OK, hash);
			callbackContext.sendPluginResult(result);

		}
	}

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
						Log.d(TAG, "Retrieved SMS: " + smsMessage);
						PluginResult result = new PluginResult(PluginResult.Status.OK, smsMessage);
						callbackContext.sendPluginResult(result);

						break;

					case CommonStatusCodes.TIMEOUT:
						// Waiting for SMS timed out (5 minutes)
						Log.e(TAG, "TIMEOUT");
						PluginResult resultTimeout = new PluginResult(PluginResult.Status.ERROR, "TIMEOUT");
						callbackContext.sendPluginResult(resultTimeout);

						break;
				}

			}
		}
	};

	/**
	 * Get all the app signatures for the current package
	 * https://github.com/googlesamples/android-credentials/blob/master/sms-verification/android/app/src/main/java/com/google/samples/smartlock/sms_verify/AppSignatureHelper.java
	 * @return
	 */
	private ArrayList<String> getAppSignatures() {
		ArrayList<String> appCodes = new ArrayList<String>();

		try {
			// Get all package signatures for the current package
			String packageName = cordova.getActivity().getApplicationContext().getPackageName();
			PackageManager packageManager = cordova.getActivity().getApplicationContext().getPackageManager();
			Signature[] signatures = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures;

			// For each signature create a compatible hash
			for (Signature signature : signatures) {
				String hash = hash(packageName, signature.toCharsString());
				if (hash != null) {
					appCodes.add(String.format("%s", hash));
				}
			}
		} catch (PackageManager.NameNotFoundException e) {
			Log.e(TAG, "Unable to find package to obtain hash.", e);
		}
		return appCodes;
	}

	private static String hash(String packageName, String signature) {
		String appInfo = packageName + " " + signature;
		try {
			MessageDigest messageDigest = MessageDigest.getInstance(HASH_TYPE);
			messageDigest.update(appInfo.getBytes(StandardCharsets.UTF_8));
			byte[] hashSignature = messageDigest.digest();

			// truncated into NUM_HASHED_BYTES
			hashSignature = Arrays.copyOfRange(hashSignature, 0, NUM_HASHED_BYTES);
			// encode into Base64
			String base64Hash = Base64.encodeToString(hashSignature, Base64.NO_PADDING | Base64.NO_WRAP);
			base64Hash = base64Hash.substring(0, NUM_BASE64_CHAR);

			return base64Hash;
		} catch (NoSuchAlgorithmException e) {
			Log.e(TAG, "hash:NoSuchAlgorithm", e);
		}
		return null;
	}

}