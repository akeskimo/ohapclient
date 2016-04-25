package com.opimobi.ohap;

import com.opimobi.ohap.message.IncomingMessage;

/**
 * Created by akeskimo on 11.4.2016.
 */

public interface ConnectionObserver {
    // interface that will handle the communication between the network client and activity
    void handleMessageResponse(IncomingMessage incomingMessage);
    void handleActivityResponse(String error);
}
