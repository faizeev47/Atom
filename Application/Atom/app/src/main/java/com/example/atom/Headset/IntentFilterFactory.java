package com.example.atom.Headset;

import android.content.IntentFilter;

import static com.example.atom.Headset.ServiceAction.CONNECTION;
import static com.example.atom.Headset.ServiceAction.CONNECTION_FAILED;
import static com.example.atom.Headset.ServiceAction.CONNECTION_INITIATION;
import static com.example.atom.Headset.ServiceAction.DISCONNECTION;
import static com.example.atom.Headset.ServiceAction.STATE_UPDATE;

public class IntentFilterFactory {
    public static IntentFilter createFilter(ServiceAction... actions) {
        IntentFilter intentFilter = new IntentFilter();
        for(ServiceAction action: actions) {
            switch (action) {
                case CONNECTION_FAILED:
                    intentFilter.addAction(SocketService.ACTION_FAILED);
                    break;
                case CONNECTION_INITIATION:
                    intentFilter.addAction(SocketService.ACTION_CONNECTION_INITIATED);
                    break;
                case CONNECTION:
                    intentFilter.addAction(SocketService.ACTION_RESPOND_CONNECTED);
                    break;
                case STATUS_UPDATE:
                    intentFilter.addAction(SocketService.ACTION_RETURN_CONNECTION_STATUS);
                    break;
                case DISCONNECTION:
                    intentFilter.addAction(SocketService.ACTION_RESPOND_DISCONNECTED);
                    break;
                case STATE_UPDATE:
                    intentFilter.addAction(SocketService.ACTION_UPDATE_CONNECTION_STATE);
                    break;
                case ATTENTION_UPDATE:
                    intentFilter.addAction(SocketService.ACTION_RETURN_ATTENTION_VALUE);
                    break;
            }
        }
        return intentFilter;
    }

    public static IntentFilter createStandardFilter() {
        return IntentFilterFactory.createFilter(
                CONNECTION_INITIATION,
                STATE_UPDATE,
                CONNECTION,
                CONNECTION_FAILED,
                DISCONNECTION);
    }
}
