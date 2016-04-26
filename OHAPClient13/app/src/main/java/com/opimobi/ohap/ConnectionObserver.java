package com.opimobi.ohap;

import com.opimobi.ohap.message.IncomingMessage;

/**
 * Created by akeskimo on 11.4.2016.
 */

public interface ConnectionObserver {
    // interface that will handle the communication between the network client and activity

    // handle incoming messages
    void handleMessageResponse(IncomingMessage incomingMessage);

    // handle activity responses
    void handleActivityResponse(String error);
}
