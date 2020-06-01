package com.example.atom.Headset;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;

public class StandardHeadsetReceiver extends BroadcastReceiver {
    public HeadsetState headsetState;
    private String mLoggingTag;
    private View mMainView;

    public StandardHeadsetReceiver() { }
    public StandardHeadsetReceiver(String loggingTag, View mainView) {
        headsetState = HeadsetState.DISCONNECTED;
        mLoggingTag = loggingTag;
        mMainView = mainView;
    }

    public void stopRunningService(Context context) {
        if (SocketService.SERVICE_RUNNING) {
            Intent intent = new Intent(context, SocketService.class);
            context.stopService(intent);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String intentAction = intent.getAction();
        String snack = null;
        if (intentAction != null) {
            switch (intentAction) {
                case SocketService.ACTION_CONNECTION_INITIATED:
                    Log.d(mLoggingTag, "Broadcast Received: Connection initiated!");
                    headsetState = HeadsetState.CONNECTING;
                    break;
                case SocketService.ACTION_UPDATE_CONNECTION_STATE:
                    Log.d(mLoggingTag, "Broadcast Received: Update connection state!");
                    headsetState = HeadsetState.CONNECTING;
                    break;
                case SocketService.ACTION_RESPOND_CONNECTED:
                    Log.d(mLoggingTag, "Broadcast Received: Connected!");
                    headsetState = HeadsetState.CONNECTED;
                    snack = "Connected to headset!";
                    break;
                case SocketService.ACTION_FAILED:
                    Log.d(mLoggingTag, "Broadcast Received: Connection failed!");
                    headsetState = HeadsetState.FAILED;
                    snack = "Sorry, failed to connect!";
                    stopRunningService(context);
                    break;
                case SocketService.ACTION_RESPOND_DISCONNECTED:
                    Log.d(mLoggingTag, "Broadcast Received: Disconnected!");
                    headsetState = HeadsetState.DISCONNECTED;
                    snack = "Disconnected from headset!";
                    stopRunningService(context);
                    break;
                default:
                    Log.d(mLoggingTag, "Broadcast Received: Unknown broadcast");
                    break;
            }
        }

        if (snack != null) {
            Snackbar.make(mMainView,
                    snack,
                    Snackbar.LENGTH_LONG).show();
        }
    }
}
