package com.opimobi.ohap;

/**
 * Implementation of central unit connection in an OHAP application.
 *
 * The central unit (superclass) has an URL that is used when connecting to the OHAP server.
 *
 * All items will register themselves automatically to the central unit they belong to. The
 * central unit provides {@link #getItemById(long)} method to retrieve an item from any
 * level of the container hierarchy by specifying the identifier of the item.
 *
 * Change history:
 * v1.0     Aapo Keskimolo      Initial version, implemented superclass abstract methods
 *
 * @see com.opimobi.ohap.CentralUnit
 *
 * @author Aapo Keskimolo &lt;aapokesk@gmail.com>
 * @version 1.0
 */


import android.util.Log;
import java.net.URL;


public class CentralUnitConnection extends CentralUnit {

    public CentralUnitConnection(URL newUrl) {
        super(newUrl);
    }

    @Override
    protected void listeningStateChanged(Container container, boolean listening) {

        if (listening) {
            container.startListening();
            System.out.println("OHAP Server is listening to connections.");
        }
        else {
            container.stopListening();
            System.out.println("OHAP Server stopped listening to connections.");
        }
    }

    @Override
    protected void changeBinaryValue(Device device, boolean value) {
        device.changeBinaryValue(value);
        Log.i("DEBUG", "Binary value changed: " + value);
    }

    @Override
    protected void changeDecimalValue(Device device, double value) {
        device.changeDecimalValue(value);
        Log.i("DEBUG", "Decimal value changed: " + value);
    }
}
