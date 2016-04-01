package com.opimobi.ohap;

/**
 * Created by Aapo Keskimolo on 29.3.2016.
 */


import java.net.URL;


public class CentralUnitConnection extends CentralUnit {

    public CentralUnitConnection(URL newUrl) {
        super(newUrl);
    }


    @Override
    protected void listeningStateChanged(Container container, boolean listening) {
        if (listening)
            container.startListening();
        else
            container.stopListening();
    }

    @Override
    protected void changeBinaryValue(Device device, boolean value) {
        device.changeBinaryValue(value);
    }

    @Override
    protected void changeDecimalValue(Device device, double value) {
        device.changeDecimalValue(value);
    }
}
