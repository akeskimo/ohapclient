package com.opimobi.ohap;

/**
 * Implementation of central unit connection in an OHAP application. This class is implemented as a
 * Singleton and its instance carries all the Device and Container object information that can be
 * used by all applications that create instance of it.
 *
 * Change history:
 * v1.0     Aapo Keskimolo      Initial version, implemented superclass abstract methods
 * v1.1     Aapo Keskimolo      Added logging
 * v1.2     Aapo Keskimolo      Defined methods for networking and method for generating dummy devices in the constructor
 *
 * @see com.opimobi.ohap.CentralUnit
 *
 * @author Aapo Keskimolo &lt;aapokesk@gmail.com>
 * @version 1.2
 */


import android.util.Log;
import android.view.KeyCharacterMap;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Random;


public class CentralUnitConnection extends CentralUnit {

    // instance of the singleton class
    private static CentralUnitConnection instance = null;

    private final String TAG = this.getClass().getSimpleName(); // Tag log
    private static int nListeners; // number of containers

    // dummy devices
    private static final int maxDummyNumber = 20; // number of dummy devices to be created (MAX 20!)
    private static int nDummy; // dummy counter
    private Device[] dummyDevices; // dummy object array

    // private constructor to prevent instantiation outside of class
    private CentralUnitConnection() throws MalformedURLException {
        super( new URL("http://ohap.opimobi.com:8080/") );
        createDummies(this);
    }

    public static CentralUnitConnection getInstance() throws MalformedURLException {
        if (instance == null) {
            instance = new CentralUnitConnection();
        }
        return instance;
    }

    @Override
    protected void listeningStateChanged(Container container, boolean listening) {

        if (listening) {
            sendListeningStart(container);
            nListeners++;
            if (nListeners == 1) {
                startNetworking();
            }
        }
        else {
            sendListeningStop(container);
            nListeners--;
            if (nListeners == 0) {
                stopNetworking();
            }
        }
    }

    @Override
    protected void changeBinaryValue(Device device, boolean value) {
        device.changeBinaryValue(value);
        Log.d(TAG, "changeBinaryValue() Binary value changed: " + value);
    }

    @Override
    protected void changeDecimalValue(Device device, double value) {
        device.changeDecimalValue(value);
        Log.d(TAG, "changeDecimalValue() Decimal value changed: " + value);
    }

    public void startNetworking() {
        Log.d(TAG, "startNetworking() Not implemented method has been called.");
    }

    public void stopNetworking() {
        Log.d(TAG, "stopNetworking() Not implemented method has been called.");
    }

    public void sendListeningStart( Container container) {
        container.startListening();
        Log.i(TAG, "sendListeningStart() Container " + container.getName() + " (id: " + container.getId() + ") started listening.");
    }

    public void sendListeningStop(Container container) {
        container.stopListening();
        Log.i(TAG, "sendListeningStop() Container " + container.getName() + " (id: " + container.getId() + ") stopped listening.");
    }


    public void createDummies(Container container) {
        dummyDevices = new Device[maxDummyNumber];
        // fill array with dummy objects
        // note: dummies will be automatically registered to the container
        for (int i = 0; i < maxDummyNumber; i++) {
            dummyDevices[i] = getRandomDummyDevice(this);
        }
    }

    private Device getRandomDummyDevice(Container container) {

        nDummy++; // increase dummy ctr, which will act as an id/index in the container
        Device newDevice;

        // create random numbers
        Random rand = new Random();
        int randNum = rand.nextInt(2)+1; // random number [1,4]

        if (randNum == 1) {
            newDevice = new Device(container, nDummy, Device.Type.ACTUATOR, Device.ValueType.BINARY);
            newDevice.setName("Ceiling Lamp");
            newDevice.setBinaryValue(false);
        }
        else {
            newDevice = new Device(container, nDummy, Device.Type.SENSOR, Device.ValueType.DECIMAL);
            newDevice.setDecimalValue(0);
            newDevice.setName("Temperature Sensor");
        }

//        // hard coded dummy types
//        int [] dummyTypes = {1,2,1,1,2,1,2,2,1,2,1,1,2,1,1,2,1,2,1,2};
//
//        if (dummyTypes[nDummy-1] == 1) {
//            newDevice = new Device(container, nDummy, Device.Type.ACTUATOR, Device.ValueType.BINARY);
//            newDevice.setName("Ceiling Lamp");
//        }
//        else {
//            newDevice = new Device(container, nDummy, Device.Type.SENSOR, Device.ValueType.DECIMAL);
//            newDevice.setName("Temperature Sensor");
//        }

        // Enter description that will be shown on the DeviceActivity
        // long desc
//        newDevice.setDescription( "(ID" + nDummy + "," + newDevice.getType() + "," + newDevice.getValueType() + ")" );
        // short desc
        newDevice.setDescription( "DeviceID: " + newDevice.getId() );

        Log.i(TAG, "getRandomDummyDevice() Dummy#" + nDummy +
                " created, name: " + newDevice.getName() +
                ", item id: " + newDevice.getId() +
                ", type: " + newDevice.getType() +
                ", valueType: " + newDevice.getValueType() );

        return newDevice; // return device initialized with dummy parameters
    }
}
