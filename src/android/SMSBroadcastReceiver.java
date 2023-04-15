package com.andreszs.smsretriever;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;

/**
 * BroadcastReceiver to wait for SMS messages. This can be registered either in the AndroidManifest or at runtime. Should filter Intents on SmsRetriever.SMS_RETRIEVED_ACTION.
 */
public class SMSBroadcastReceiver extends BroadcastReceiver {

    private OTPReceiveListener otpListener;

    public void setOTPListener(OTPReceiveListener otpListener) {
        this.otpListener = otpListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (SmsRetriever.SMS_RETRIEVED_ACTION.equals(intent.getAction())) {
            Bundle extras = intent.getExtras();
            Status status = (Status) extras.get(SmsRetriever.EXTRA_STATUS);
            if (status == null) {
                return;
            }
            switch (status.getStatusCode()) {
                case CommonStatusCodes.SUCCESS:
                    String message = (String) extras.get(SmsRetriever.EXTRA_SMS_MESSAGE);
                    if (otpListener != null) {
                        otpListener.onSmsReceived(message);
                    }
                    break;
                case CommonStatusCodes.TIMEOUT:
                    // Waiting for SMS timed out (5 minutes)
                    if (otpListener != null) {
                        otpListener.onSmsReceiveTimeOut();
                    }
                    break;

                case CommonStatusCodes.API_NOT_CONNECTED:

                    if (otpListener != null) {
                        otpListener.onSmsReceivedError("API NOT CONNECTED");
                    }

                    break;

                case CommonStatusCodes.NETWORK_ERROR:

                    if (otpListener != null) {
                        otpListener.onSmsReceivedError("NETWORK ERROR");
                    }

                    break;

                case CommonStatusCodes.ERROR:

                    if (otpListener != null) {
                        otpListener.onSmsReceivedError("SOME THING WENT WRONG");
                    }

                    break;

            }
        }
    }

    /**
     *
     */
    public interface OTPReceiveListener {

        void onSmsReceived(String smsMessage);

        void onSmsReceiveTimeOut();

        void onSmsReceivedError(String error);
    }
}