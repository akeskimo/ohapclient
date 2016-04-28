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
 * v1.3     Aapo Keskimolo      Multithreading, message handlers and networking
 *
 * @see com.opimobi.ohap.CentralUnit
 *
 * @author Aapo Keskimolo &lt;aapokesk@gmail.com>
 * @version 1.3
 */


import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import com.opimobi.ohap.message.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.Random;


public class CentralUnitConnection extends CentralUnit {

    // Log tag
    private final String TAG = this.getClass().getSimpleName();

    // Class specific
    private static CentralUnitConnection instance = null;
    private static int nListeners;
    private static int dummyNumber; // dummy counter
    private Device[] dummyDevices; // dummy object array

    // Networking
    private String defaultUrl = "http://default.url:9999";
    private boolean running = false;
    private boolean connected = false;
    private boolean autoConnect = false;
    private boolean listeningStart = false;
    private static final int retryAttempts = 3;
    private static HandlerThread handlerThread = null;
    private static IncomingThread incomingThread = null;
    private Socket socket = null;
    private Handler outgoingMessageHandler = null;
    private OutputStream outputStream = null;
    private InputStream inputStream = null;
    private ConnectionObserver observer = null;
    private static int incomingThreadCtr = 0;
    private boolean reconnectRequest;
    private boolean outgoingMessageActionFinished = true;

    // connection status
    private Status connectionStatus = Status.OFFLINE;


    // private constructor for singleton implementation
    private CentralUnitConnection() throws MalformedURLException {
        super(new URL("http://ohap.opimobi.com:8080/")); // this URL will be overwritten
    }


    public static CentralUnitConnection getInstance() {
        if (instance == null) {
            try {
                instance = new CentralUnitConnection();
            } catch (MalformedURLException e) {
                Log.d("CentralUnitConnection", "getInstance() Constructor has malformed URL: " + e.getMessage() );
            }
        }
        return instance;
    }

    public void initialize(URL url, ConnectionObserver observer) {
        // initializes central unit singleton
        instance.setURL(url);
        this.observer = observer;
    }

    public Status getConnectionStatus() {
        return connectionStatus;
    }

    public void setConnectionStatus(Status connectionStatus) {
        this.connectionStatus = connectionStatus;
    }


    public enum Status {
        ONLINE,
        OFFLINE,
        CONNECTING
    }


    public void initialize(URL url, ConnectionObserver observer, Container container, int nDummies) {
        // initializes central unit singleton with dummy devices
        createDummies(container, nDummies);
        setName("Dummy Container");
        setDescription("Dummies have been created during initialization of the class");
        instance.setURL(url);
        this.observer = observer;
    }

    public void setReconnectRequest(boolean reconnectRequest) {
        this.reconnectRequest = reconnectRequest;
    }

    public boolean getReconnectRequest() {
        return reconnectRequest;
    }


    private class IncomingMessageAction implements Runnable {
        // This will be the action executed on incoming messages. The action is queued in the
        // HandlerThread

        private IncomingMessage incomingMessage;

        public IncomingMessageAction(IncomingMessage incomingMessage) {
            this.incomingMessage = incomingMessage;
        }

        @Override
        public void run() {
            // Forward incoming message to the observer activity
            if (observer != null) {
                observer.handleMessageResponse(incomingMessage);
            }
        }
    }


    private class OutgoingMessageAction implements Runnable {
        // This will be the action executed on outgoing messages. The actions will be queued in
        // handlerThread

        private OutgoingMessage outgoingMessage;

        public OutgoingMessageAction(OutgoingMessage outgoingMessage) {
            this.outgoingMessage = outgoingMessage;
        }
        
        @Override
        public void run() {
            // Write outgoing message to outputstream and forward it to outgoing handler

            outgoingMessageActionFinished = false;

            if (isConnected()) {
                if (socket != null) {

                    try {
                        outgoingMessage.writeTo(outputStream);
                        Log.i(TAG, "OutGoingMessageAction.run() Sent message: " + outgoingMessage);

                    } catch (IOException e) {
                        Log.e(TAG, "OutGoingMessageAction.run() Unable to write to outputStream: " + e.getMessage());
                    }
                }

            } else {
                String errorMsg = "Unable to sent message: Client has no connection.";
                Log.e(TAG, "OutGoingMessageAction.run() " + errorMsg);
            }

            outgoingMessageActionFinished = true;
        }
    }


    private class ActivityAction implements Runnable {
        // This class will post an error message to the HandlerThread queue

        private String messageAction;

        public ActivityAction(String messageAction) {
            this.messageAction = messageAction;
        }

        @Override
        public void run() {
            // Forward error message

            if (null != observer) {
                observer.handleActivityResponse(messageAction);
                Log.d(TAG, "ActivityAction.run() Sent new activity action message: " + messageAction);
            } else {
                Log.e(TAG, "ActivityAction.run() Unable to sent message: Observer = null" );
            }
        }
    }


        private class IncomingThread extends Thread {

        @Override
        public void run() {

            Handler incomingHandler = new Handler(Looper.getMainLooper());

            incomingThreadCtr++;
            Log.d(TAG, "Incoming threads running: " + incomingThreadCtr);

            try {
                // Tie outgoing handler to secondary handler thread looper
                outgoingMessageHandler = new Handler(handlerThread.getLooper());
            } catch (Exception e) {
                Log.d(TAG, "IncomingThread.run() Unable to instantiate outgoing message handler");
                return;
            }

            connect(incomingHandler);

//            if (listeningStart) {
//                // inform the server that centralUnitConnection has started listening
//                sendListeningStart(instance);
//                listeningStart = false;
//            }

            while (running) {
                // parses and forwards all incoming messages to incoming handler

                IncomingMessage msg = new IncomingMessage();
                boolean dataCame = false;

                if (socket != null) {
                    try {
                        msg.readFrom(inputStream);
                        dataCame = true;
                    } catch (IOException e) {Log.e(TAG, "incomingThread.run() Socket read error");

                        if (e.getMessage() != null) {
                            if (e.getMessage().equals("End of message input.")) {
                                // if server closes connection, reattempt to connect
                                Log.e(TAG, "incomingThread.run() Server closed connection.");
                                setConnected(false);
                                setRunning(false);
                                break;
                            }
                        }

//                        if (socket.isClosed()) {
//                            Log.e(TAG, "incomingThread.run() Socket is closed! Exiting thread loop");
//                            break;
//                        }

                    } catch (Exception e) {
                        Log.e(TAG, "incomingThread.run() Unhandled exception: " + e.getMessage());
                        break;
                    }

                    if (dataCame) {
                        incomingHandler.post(new IncomingMessageAction(msg));
                    }
                }
            }
            close(incomingHandler);
            incomingThreadCtr--;
        }

        private void connect(Handler incomingHandler) {
            // Connect to server socket
            // Timeout = 5000 ms
            // Attempts = unlimited

            int timeout = 5000;
            int attempt = 0;

            while (running) {
                attempt++;

                try {

                    socket = new Socket();
                    socket.connect(new InetSocketAddress(
                                    instance.getURL().getHost(),
                                    instance.getURL().getPort()),
                                    timeout);
                    socket.setSoTimeout(timeout);
                    inputStream = socket.getInputStream();
                    outputStream = socket.getOutputStream();
                    Log.i(TAG, "IncomingThread.connect() Connected to " + getURL() + ".");

                    setConnected(true);
                    setConnectionStatus(Status.ONLINE);

                    incomingHandler.post(new ActivityAction("Connected"));

                    sendLogin("someguy", "password");

                    return;

                } catch (IOException e) {
                    // If connection failed -> update flag, log and post error to UI

                    setConnectionStatus(Status.OFFLINE);

                    Log.e(TAG, "IncomingThread.connect() Unable to connect to " + getURL().getHost() + ":" + getURL().getPort() + "\nreason: " + e.getMessage() + ". Attempt = " + attempt + " Retrying in " + timeout / 1000 + " seconds.");

                    if (observer != null && incomingHandler != null) {
                        if (attempt == 1) {
                            // send error message to connection observer
                            String errorMsg = "No connection";
                            incomingHandler.post(new ActivityAction(errorMsg));
                        } else if (attempt > 1) {
                            incomingHandler.post(new ActivityAction("Reconnecting"));
                        }
                    }
                    try {
                        incomingThread.sleep(timeout);
                    } catch (InterruptedException ie) {
                        Log.e(TAG, "connect() incomingThread sleep interrupted again: " + ie.getMessage());
                    }
                } catch (IllegalArgumentException iae) {
                    Log.e(TAG, "IncomingThread.connect() Invalid host/port: " + iae.getMessage() + "\ncentral unit url: " + instance.getURL() + "\nSetting url = http://192.168.0.100:18001");
                    try {
                        instance.setURL(new URL("http://192.168.0.100:18001"));
                    } catch (MalformedURLException e) {
                        Log.e(TAG, "IncomingThread.connect() Malformed url: " + e.getMessage());
                    }
                }
            }

            setConnected(false);
            setConnectionStatus(Status.OFFLINE);
        }


        private void close(Handler incomingHandler) {

            if (socket != null) {

                sendLogout();

                // wait for the outgoing message action to finish
                while(!outgoingMessageActionFinished) {}

                try {
                    if (socket != null) {
                        socket.close();
                    }
                    if (observer != null && incomingHandler != null) {
                        // notify observers
                        String errorMsg = "Connection closed";
                        incomingHandler.post(new ActivityAction(errorMsg));
                        setConnected(false);
                    } else {
                        Log.e(TAG, "close() Attempt to notify observer failed: observer or incominghandler = null");
                    }
                } catch (IOException e) {
                    Log.e(TAG, "close() Unable to close socket: " + e.getMessage());
                }
                socket = null;
                outgoingMessageHandler = null;
                inputStream = null;
                outputStream = null;
                Log.i(TAG, "close() Connection to server was closed.");

                setConnected(false);
                setConnectionStatus(Status.OFFLINE);
            }
        }
    }

    public void reconnect(ConnectionObserver observer) {
        // Reconnect to server
        if (running) {
            stop();
        }
        start(observer);
    }

    public int getRetryAttempts() {
        return retryAttempts;
    }


    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    private void setConnected(boolean connected) {
        this.connected = connected;
    }

    public boolean isConnected() {
        return connected;
    }

    public void start(ConnectionObserver observer) {
        this.observer = observer;
        Log.d(TAG, "start() Number of listeners = " + nListeners);
        listeningStateChanged(this, true);
    }

    public void stop() {
//        this.observer = null;
        listeningStateChanged(this, false);
    }

    public void startNetworking() {
        Log.d(TAG, "startNetworking() Called");
        setRunning(true);
        handlerThread = new HandlerThread("handlerThread");
        handlerThread.start();
        incomingThread = new IncomingThread();
        incomingThread.start();
    }

    public void stopNetworking() {

        Log.d(TAG, "stopNetworking() Called");

        sendLogout();

        setRunning(false);

        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                Log.d(TAG, "stopNetworking() Socket failed to close.");
            }
            socket = null;
        }

        setConnected(false);

        if (handlerThread != null) {
            handlerThread.interrupt();
            handlerThread = null;
        }

        if (incomingThread != null) {
            incomingThread.interrupt();
            try {
                incomingThread.join();
            } catch (Exception e) {
                Log.d(TAG, "stopNetworking() Unable to join thread: " + e.getMessage());
            }
            incomingThread = null;
        }
    }

    public void setAutoConnect(boolean autoConnect) {
        this.autoConnect = autoConnect;
    }

    public boolean isAutoConnect() {
        return autoConnect;
    }


    @Override
    protected void listeningStateChanged(Container container, boolean listening) {

        if (listening) {
            // start container listening

            nListeners++;

            if (nListeners == 1) {
                startNetworking();
            }

            // send listening start
            if (isConnected()) {
                // if central unit is already connected, inform server that container has started
                // listening
                sendListeningStart(container);
            } else {
                // if there is no connection, wait for network thread and then send listening start
                listeningStart = true;
            }

        } else {
            // stop container from listening

            if (nListeners > 0) {
                nListeners--;
                sendListeningStop(container);
            }

            if (nListeners == 0) {
                stopNetworking();
            }
        }

        Log.d(TAG, "listeningStateChanged() Number of listeners: " + nListeners);
    }

    @Override
    protected void changeBinaryValue(Device device, boolean value) {
        sendBinaryValueChanged(device, value);
        Log.d(TAG, "changeDecimalValue() Device " + device.getId() + " binary value changed to: " + value);
    }

    @Override
    protected void changeDecimalValue(Device device, double value) {
        device.changeDecimalValue(value);
        Log.d(TAG, "changeDecimalValue() Device " + device.getId() + " decimal value changed to: " + value);
    }


    // The sending of OHAP Protocol messages to the tcp-ohap-server application
    // Protocol version:    0.3.1 (March 11, 2016)
    // Messages:            From client to server

    public void sendLogin(String username, String password) {

        if (null != outgoingMessageHandler) {
            Log.d(TAG, "Send login message");
            OutgoingMessage outgoingMessage = new OutgoingMessage();
            outgoingMessage.integer8(0x00)
                    .integer8(0x01)
                    .text(username)
                    .text(password);
            outgoingMessageHandler.post(new OutgoingMessageAction(outgoingMessage));
            Log.d(TAG, "sendLogin() Sent login to server: " + getURL().getHost() + ":" + getURL().getPort());
        }
    }


    public void sendLogout() {

        if (outgoingMessageHandler != null) {
            OutgoingMessage outgoingMessage = new OutgoingMessage();
            outgoingMessage.integer8(0x01)
                    .text("So long, Dr. Strangelove");
            outgoingMessageHandler.post(new OutgoingMessageAction(outgoingMessage));
            Log.d(TAG, "sendLogout() Sent logout to server: " + getURL().getHost() + ":" + getURL().getPort());
        }
    }


    public void sendPing() {

        if (null != outgoingMessageHandler) {
            long pingIdentifier = SystemClock.uptimeMillis();
            OutgoingMessage outgoingMessage = new OutgoingMessage();
            outgoingMessage.integer8(0x02)
                    .integer32(pingIdentifier);
            outgoingMessageHandler.post(new OutgoingMessageAction(outgoingMessage));
            Log.d(TAG, "sendPing() Sent ping to server: " + getURL().getHost() + ":" + getURL().getPort());
        }
    }

    public void sendPong() {

        if (null != outgoingMessageHandler) {
            long pongIdentifier = SystemClock.uptimeMillis();
            OutgoingMessage outgoingMessage = new OutgoingMessage();
            outgoingMessage.integer8(0x03)
                    .integer32(pongIdentifier);
            outgoingMessageHandler.post(new OutgoingMessageAction(outgoingMessage));
            Log.d(TAG, "sendPong() Sent pong to server: " + getURL().getHost() + ":" + getURL().getPort());
        }
    }

    public void sendDecimalSensorChanged(Device device) {

        if (null != outgoingMessageHandler) {
            OutgoingMessage outgoingMessage = new OutgoingMessage();
            outgoingMessage.integer8(0x04)
                    .integer32(device.getId())
                    .decimal64(device.getDecimalValue())
                    .integer32(device.getParent().getId())
                    .text(device.getName())
                    .text(device.getDescription())
                    .binary8(device.isInternal())
                    .decimal64(device.getMinValue())
                    .decimal64(device.getMaxValue())
                    .text(device.getUnit())
                    .text(device.getUnitAbbreviation());
            outgoingMessageHandler.post(new OutgoingMessageAction(outgoingMessage));
            Log.d(TAG, "sendDecimalSensorChanged() Sent new decimal sensor info for item " + device.getId());
        }
    }


    public void sendDecimalActuatorChanged(Device device) {

        if (null != outgoingMessageHandler) {
            OutgoingMessage outgoingMessage = new OutgoingMessage();
            outgoingMessage.integer8(0x05)
                    .integer32(device.getId())
                    .decimal64(device.getDecimalValue())
                    .integer32(device.getParent().getId())
                    .text(device.getName())
                    .text(device.getDescription())
                    .binary8(device.isInternal())
                    .decimal64(device.getMinValue())
                    .decimal64(device.getMaxValue())
                    .text(device.getUnit())
                    .text(device.getUnitAbbreviation());
            outgoingMessageHandler.post(new OutgoingMessageAction(outgoingMessage));
            Log.d(TAG, "sendDecimalActuatorChanged() Sent new decimal actuator info for item " + device.getId());
        }
    }


    public void sendBinarySensorChanged(Device device) {

        if (null != outgoingMessageHandler) {
            OutgoingMessage outgoingMessage = new OutgoingMessage();
            outgoingMessage.integer8(0x06)
                    .integer32(device.getId())
                    .binary8(device.getBinaryValue())
                    .integer32(device.getParent().getId())
                    .text(device.getName())
                    .text(device.getDescription())
                    .binary8(device.isInternal());
            outgoingMessageHandler.post(new OutgoingMessageAction(outgoingMessage));
            Log.d(TAG, "sendBinarySensorChanged() Sent new binary sensor info for item " + device.getId());
        }
    }

    public void sendBinaryActuatorChanged(Device device) {

        if (null != outgoingMessageHandler) {
            OutgoingMessage outgoingMessage = new OutgoingMessage();
            outgoingMessage.integer8(0x07)
                    .integer32(device.getId())
                    .binary8(device.getBinaryValue())
                    .integer32(device.getParent().getId())
                    .text(device.getName())
                    .text(device.getDescription())
                    .binary8(device.isInternal());
            outgoingMessageHandler.post(new OutgoingMessageAction(outgoingMessage));
            Log.d(TAG, "sendBinarySensorChanged() Sent new binary actuator info for item " + device.getId());
        }
    }


    public void sendContainerChanged(Container container) {

        if (null != outgoingMessageHandler) {
            OutgoingMessage outgoingMessage = new OutgoingMessage();
            outgoingMessage.integer8(0x08)
                    .integer32(container.getId())
                    .integer32(container.getParent().getId())
                    .text(container.getName())
                    .text(container.getDescription())
                    .binary8(container.isInternal());
            outgoingMessageHandler.post(new OutgoingMessageAction(outgoingMessage));
            Log.d(TAG, "sendContainerChanged() Sent new container info for container " + container.getId());
        }
    }


    public void sendDecimalValueChanged(Device device, double value ) {

        if (null != outgoingMessageHandler) {
            OutgoingMessage outgoingMessage = new OutgoingMessage();
            outgoingMessage.integer8(0x09)
                    .integer32(device.getId())
                    .decimal64(value);
            outgoingMessageHandler.post(new OutgoingMessageAction(outgoingMessage));
            Log.d(TAG, "sendDecimalValueChanged() Sent device " + device.getId() + " new decimal value = " + value);
        }
    }


    public void sendBinaryValueChanged(Device device, boolean value) {

        if (null != outgoingMessageHandler) {
            OutgoingMessage outgoingMessage = new OutgoingMessage();
            outgoingMessage.integer8(0x0a)
                    .integer32(device.getId())
                    .binary8(value);
            outgoingMessageHandler.post(new OutgoingMessageAction(outgoingMessage));
            Log.d(TAG, "sendBinaryValueChanged() Sent device " + device.getId() + " new binary value = " + value);
        }
    }


    public void sendItemRemoved(Item item) {

        if (null != outgoingMessageHandler) {
            OutgoingMessage outgoingMessage = new OutgoingMessage();
            outgoingMessage.integer8(0x0b)
                    .integer32(item.getId());
            outgoingMessageHandler.post(new OutgoingMessageAction(outgoingMessage));
            Log.d(TAG, "sendItemRemoved() Sent ping to server: " + getURL().getHost() + ":" + getURL().getPort());
        }
    }


    public void sendListeningStart( Container container ) {
        // informs the server that container has started listening

        if (outgoingMessageHandler != null) {
            OutgoingMessage outgoingMessage = new OutgoingMessage();
            outgoingMessage.integer8(0x0c)
                    .integer32(container.getId());
            outgoingMessageHandler.post(new OutgoingMessageAction(outgoingMessage));
            Log.d(TAG, "sendListeningStart() Sent listening start for container " + container.getId());
         }
        else
            Log.e(TAG, "sendListeningStart() Unable to send listening start to server: outgoingMessagehandler = null");
    }


    public void sendListeningStop( Container container) {
        // informs the server that container has stopped listening

        if (outgoingMessageHandler != null) {
            OutgoingMessage outgoingMessage = new OutgoingMessage();
            outgoingMessage.integer8(0x0d)
                    .integer32(container.getId());
            outgoingMessageHandler.post(new OutgoingMessageAction(outgoingMessage));
            Log.d(TAG, "sendListeningStop() Sent listening stop for container " + container.getId());
        }
        else
            Log.e(TAG, "sendListeningStop() Unable to send listening stop to server: outgoingMessagehandler = null");
    }



    // NOTE: CODE BELOW THIS LINE IS FOR CREATING DUMMY OBJECTS IN THE CONTAINER (ONLY)


    public void createDummies(Container container, int nDummies) {
        dummyDevices = new Device[nDummies];
        // fill array with dummy objects
        // note: dummies will be automatically registered to the container
        for (int i = 0; i < nDummies; i++) {
            dummyDevices[i] = getRandomDummyDevice(container);
        }
    }

    private Device getRandomDummyDevice(Container container) {

        dummyNumber++; // increase dummy ctr, which will act as an id/index in the container
        Device newDevice;

        // create random numbers
        Random rand = new Random();
        int randNum = rand.nextInt(2)+1; // random number [1,4]

        if (randNum == 1) {
            newDevice = new Device(container, dummyNumber, Device.Type.ACTUATOR, Device.ValueType.BINARY);
            newDevice.setName("Ceiling Lamp");
            newDevice.setBinaryValue(false);
        }
        else {
            newDevice = new Device(container, dummyNumber, Device.Type.SENSOR, Device.ValueType.DECIMAL);
            newDevice.setDecimalValue(0);
            newDevice.setName("Temperature Sensor");
        }

        newDevice.setDescription( "DeviceID: " + newDevice.getId() );

        Log.i(TAG, "getRandomDummyDevice() Dummy#" + dummyNumber +
                " created, name: " + newDevice.getName() +
                ", item id: " + newDevice.getId() +
                ", type: " + newDevice.getType() +
                ", valueType: " + newDevice.getValueType() );

        return newDevice; // return device initialized with dummy parameters
    }

    public void resetListeners() {
        nListeners = 0;
    }

    public void resetUrl() {
        try {
            instance.setURL(new URL(defaultUrl));
        } catch (MalformedURLException e) {
            Log.e(TAG, "resetUrl() Malformed default URL value: " + defaultUrl + ",reason: " + e.getMessage() );
        }
    }
}
