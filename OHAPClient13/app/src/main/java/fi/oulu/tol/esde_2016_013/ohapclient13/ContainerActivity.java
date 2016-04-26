package fi.oulu.tol.esde_2016_013.ohapclient13;

/**
 * Description:
 * Container Activity for an OHAP client application for monitoring and actuating house devices via
 * remote control Android application. This activity is the main entry point for OHAPClient13.
 *
 * Change history:
 * v1.0     Aapo Keskimolo      Initial version
 * v1.1     Aapo Keskimolo      Display device value on ListView and load shared preference upon startup
 * v1.2     Aapo Keskimolo      Added alert dialogs, connection observer, ping menu bar item and networking support
 *
 * @author Aapo Keskimolo &lt;aapokesk@gmail.com>
 * @version 1.2
 */

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.opimobi.ohap.CentralUnit;
import com.opimobi.ohap.CentralUnitConnection;
import com.opimobi.ohap.ConnectionObserver;
import com.opimobi.ohap.Container;
import com.opimobi.ohap.Device;
import com.opimobi.ohap.Item;
import com.opimobi.ohap.message.IncomingMessage;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import fi.oulu.tol.esde_2016_013.ohapclient13.utility.LogContainer;
import fi.oulu.tol.esde_2016_013.ohapclient13.utility.MessageLog;


public class ContainerActivity extends ActionBarActivity implements ConnectionObserver,
        SensorEventListener {

    // log tag
    private final String TAG = this.getClass().getSimpleName();

    // intent extras
    private final static String CENTRAL_UNIT_URL = "fi.oulu.tol.esde_2016_013.ohapclient13.CENTRAL_UNIT_URL";
    private final static String DEVICE_ID = "fi.oulu.tol.esde_2016_013.ohapclient13.DEVICE_ID";
    private final static String CONTAINER_ID = "fi.oulu.tol.esde_2016_013.ohapclient13.CONTAINER_ID";

    // central unit container
    private static CentralUnitConnection centralUnit = null;

    // container of the intent
    private Container container;

    // sensor for shake
    private SensorManager sensorManager = null;
    private Sensor accelerometer = null;
    private static final double SHAKE_GEFORCE_LIMIT = 2.2F;
    private static final int SHAKE_DELAY_IN_BETWEEN = 10000;
    private long shakeTimestamp;

    // alert dialog box
    private boolean alertShown = false;
    AlertDialog alert = null;

    // container listener
    List<Long> listeningContainerIds = new ArrayList<>();
    ArrayList<Item> listItems = new ArrayList<>();

    // log utilities place holder
    LogContainer logContainer = null;


    public CentralUnitConnection getCentralUnit() {
        if (centralUnit == null) {
            try {
                centralUnit = CentralUnitConnection.getInstance();
            } catch (MalformedURLException e) {
                Log.d(TAG, "getCentralUnit() Malformed URL exception: " + e.getMessage());
            }
        }
        return centralUnit;
    }


    /////////////////////////////////////////////////////////////////////////////////////////////
    // Android activity life cycle methods start below this line
    /////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_container);

        // log utilities
        logContainer = LogContainer.getInstance();

        // start sensor manager and get reference to accelerometer for shake detection
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE); // sensor system mgr
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER); // acc sensor

        try {
            // getting the instance of CentralUnit
            centralUnit = CentralUnitConnection.getInstance();
            Log.i(TAG, "onCreate() centralUnit obj: " + centralUnit + " name: " + centralUnit.getName() + " container id: " + centralUnit.getId() + " item count: " + centralUnit.getItemCount() + " listening state: " + centralUnit.isListening());

        } catch (MalformedURLException e) {
            Log.e(TAG, "onCreate() URL is invalid: " + e.getMessage());
        }

        // loads preferences (Central Unit URL, Auto-connect, etc.)
        loadPreferences();

//        // initializes list view
//        createListView();
//
//        // initialize connection to server
//        connect();
    }


    @Override
    protected void onResume() {
        super.onResume();

        Log.d(TAG, "onResume() Called");

//        // refresh central unit
//        centralUnit = getCentralUnit();

        Container container;

//        if (getIntent().hasExtra(CONTAINER_ID)) {
            long containerId = getIntent().getLongExtra(CONTAINER_ID, 0);
            container = (Container) centralUnit.getItemById(containerId);
            Log.d(TAG, "onCreate() Got intent extra, CONTAINER_ID = " + containerId);
//        }

        // set activity title
        setTitle(container.getName());

        // create list view objects with list adapter
        createListView(container);

        // start networking
        connect();

        // register sensor listener
        sensorManager.registerListener(this, accelerometer, sensorManager.SENSOR_DELAY_NORMAL);
    }


    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause() Called");

        // unregister sensor listener
        sensorManager.unregisterListener(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_container, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Action menu bar items are handled here

        // get clicked menu identifier
        int id = item.getItemId();

        if (id == R.id.menu_log) {
            Intent intent = new Intent(ContainerActivity.this, LogActivity.class);
            startActivity(intent);
        } else if (id == R.id.menu_ping) {
            // ping server
            if (centralUnit.isConnected())
                centralUnit.sendPing();
            else
                Toast.makeText(this, "Ping message not sent: No connection to server", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.menu_settings) {
            // start preference activity
            Intent intent = new Intent(ContainerActivity.this, PreferencesActivity.class);
            startActivity(intent);
        } else if (id == R.id.menu_help) {
            // start helper activity
            Intent intent = new Intent(ContainerActivity.this, HelperActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart() Called");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop() Called");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        centralUnit.sendLogout();

        stopContainerListening();

        // stop networking
        centralUnit.stop();

        Log.d(TAG, "onDestroy() Called");
    }


    /////////////////////////////////////////////////////////////////////////////////////////////
    // Other methods start below this line
    /////////////////////////////////////////////////////////////////////////////////////////////

    private void createListView(final Container container) {
        try {
            // Create ListAdapter and set it to ListView
            ListView listView = (ListView) findViewById(R.id.listView);
            listView.setAdapter(new ContainerListAdapter(container));

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    // list view item onclick callback

                    Log.d(TAG, "createListView() Position = " + position + ", id = " + container.getItemByIndex(position).getId() + ", container = " + container.getItemByIndex(position).getName() );

                    Intent intent;

                    if (container.getItemByIndex(position) instanceof Container) {

                        // selected item is container, launch ContainerActivity
                        intent = new Intent(getApplicationContext(), ContainerActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

                        intent.putExtra(CONTAINER_ID, container.getItemByIndex(position).getId());

                    } else {

                        Log.d(TAG, "position = " + position + ", device = " + container.getItemByIndex(position).getName() );

                        // select item is device, launch DeviceActivity
                        intent = new Intent(ContainerActivity.this, DeviceActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                        intent.putExtra(CENTRAL_UNIT_URL, centralUnit.getURL().toString() );
                        intent.putExtra(DEVICE_ID, container.getItemByIndex(position).getId() );
                    }

//                    if (centralUnit.getItemByIndex(position) instanceof Container) {
//                        // if selected item is container, launch ContainerActivity
//                        intent = new Intent(getApplicationContext(), ContainerActivity.class);
////                        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
//                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                        intent.putExtra(CONTAINER_NAME, centralUnit.getItemByIndex(position+1).getName() );
//                        intent.putExtra(CONTAINER_ID, 1);
//
//                    } else {
//                        // if select item is not container, launch DeviceActivity
//                        intent = new Intent(ContainerActivity.this, DeviceActivity.class);
//                        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
//                        intent.putExtra(CENTRAL_UNIT_URL, centralUnit.getURL().toString() );
//                        intent.putExtra(DEVICE_ID, position );
//                    }
                    getApplicationContext().startActivity(intent);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "onCreate() Unable to create ListView / Launch intent: " + e.getMessage());
        }
    }


    private void loadPreferences() {
        // Load user default preferences on activity start

        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(this);
        String port_key, url_key, port_value, url_value, autoconnect_key;
        boolean autoConnect;

        // load autoconnect settings
        autoconnect_key = getString(R.string.pref_autoconnect_key);
        try {
            autoConnect = shared.getBoolean(autoconnect_key,
                    Boolean.parseBoolean(getString(R.string.pref_autoconnect_default)));
        } catch (Exception e) {
            Log.w(TAG, "loadPreferences() Unable to parse resource default value: " + getString(R.string.pref_autoconnect_default) + " to boolean. Forcing default value = true.");
            autoConnect = shared.getBoolean(autoconnect_key, true);
        }
        Log.i(TAG, "loadPreferences() Preference loaded, key = " + autoconnect_key + ", value = " + autoConnect);

        // load port settings
        port_key = getString(R.string.pref_port_key);
        port_value = shared.getString( port_key, getString(R.string.pref_port_default));
        Log.i(TAG, "loadPreferences() Preference loaded, key = " + port_key + ", value = " + port_value);

        // load url settings
        url_key = getString(R.string.pref_url_key);
        url_value = shared.getString( url_key, getString(R.string.pref_url_default));
        Log.i(TAG, "loadPreferences() Preference loaded, key = " + url_key + ", value = " + url_value);

        String address = url_value + ":" + port_value + "/";
        try {
            centralUnit.setAutoConnect(autoConnect);
            centralUnit.initialize(new URL(address), this);
//            centralUnit.setURL(new URL(address));
            Log.i(TAG, "loadPreferences() CentralUnit set with new URL: " + address);
        } catch (MalformedURLException e) {
            Log.e(TAG, "loadPreferences() Unable to set CentralUnit URL address \"" + address + ", reason: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "loadPreferences() Unhandled error occurred: " + e.getMessage() );
        }
    }


    private void connect() {
        // Handle connect request

        Log.d(TAG, "connect() isAutoConnect() = " + centralUnit.isAutoConnect() + ", checkNetwork() = " + checkNetwork() + ", isRunning() = " + centralUnit.isRunning() + ", isConnected() = " + centralUnit.isConnected());

        if (centralUnit.isAutoConnect()) {
            // auto-connect setting is enabled

            if (checkNetwork()) {
                // device is connected to network

                if (!centralUnit.isRunning()) {
                    // threads are not running (networking has not been started)
                    Log.d(TAG, "connect() Connecting to server...");
                    centralUnit.start(this);
                }

            } else {
                // Raise alert dialog box, if the device is not connected to network
                String ttl = "No network detected";
                String msg = "Connect the device to WIFI or mobile network";
                alertDialogMessageOk(ttl, msg, "Continue offline");
            }

        } else {
            // auto-connect feature is switched off

            // display a dialog to inform user that auto connection is switched off
            String ttl = "Auto-connect is off";
            String msg = "You may switch auto-connection on in 'Settings'-menu or shake the device to force reconnect";
            alertDialogMessageOk(ttl, msg, "OK");

            // stop networking
            stop(); // TODO Is stopping networking necessary or should the connection be left as it is?
        }
    }

    private void stop() {
        stopContainerListening();
        destroyItems();
        centralUnit.stop();
    }

    private void reconnect() {
        // Handle re-connect request
        Toast.makeText(this, "Reconnecting...", Toast.LENGTH_SHORT).show();
        centralUnit.reconnect(this);
    }

    private void destroyItems() {
        // removes all items/containers from central unit
        for (Item item: listItems) {
            item.destroy();
        }
    }


    private void stopContainerListening() {
        // note: before stop, containers must be stopped from listening

        for (long id: listeningContainerIds) {
            try {
                Container container = (Container) centralUnit.getItemById(id);
                if (container.isListening()) {
                    container.stopListening();
                    Log.d(TAG, "stopContainerListening() Container " + id + " stopped listening");
                }
            } catch(Exception e) {
                Log.d(TAG, "stopContainerListening() Unable to stop container " + id + "from listening!");
            }
        }

        if (centralUnit.isListening()) {
            centralUnit.stopListening();
            Log.d(TAG, "stopContainerListening() Central unit stopped listening");
        }
    }


    private boolean checkNetwork() {
        // returns true/false on whether the device has wifi or mobile network connection

        try {
            Context context = getApplicationContext();
            ConnectivityManager connMgr =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

            if (networkInfo != null && networkInfo.isConnected()) {
                return true;
            }

        } catch (Exception e) {
            Log.e(TAG, "Unable to check network state: " + e.getMessage() + ". Forcing checkNetwork() = true");
            return true; // assume that there is network
        }

        Log.i(TAG, "checkNetwork() No network detected");
        return false;
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        // get the magnitude of acceleration in 3 degrees of freedom (XYZ)
        // and determine total g-force for all axial directions

        double gX = event.values[0] / SensorManager.GRAVITY_EARTH;
        double gY = event.values[1] / SensorManager.GRAVITY_EARTH;
        double gZ = event.values[2] / SensorManager.GRAVITY_EARTH;

        // calculate total g-force (1g = 9.82m/s2)
        double totalGeForce = Math.sqrt(gX * gX + gY * gY + gZ * gZ);

        if (totalGeForce > SHAKE_GEFORCE_LIMIT) {
            // if shake speed is higher than threshold

            // get current system time in ms
            final long timestamp = System.currentTimeMillis();

            if (shakeTimestamp + SHAKE_DELAY_IN_BETWEEN > timestamp) {
                // if time interval between shakes is shorter than threshold, do nothing
                return;
            }

            // store time stamp of last shake
            shakeTimestamp = timestamp;

            // request reconnect to server
            reconnect();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // no use case
    }

    public void alertDialogMessageOk(String title, String message, String buttonText) {
        // Pop alert dialog box with 1 button

        final Builder alertBuilder = new Builder(this); // alert dialog box

        if (alertShown) {
            // dismiss the alert dialog if already visible
            alert.cancel();
            alertShown = false;
        }

        alertBuilder.setTitle(title);
        alertBuilder.setMessage(message);
        alertBuilder.setPositiveButton(buttonText, null);

        alert = alertBuilder.create();
        alert.show();
        alertShown = true;
    }

    public void alertDialogCustom(String title, String message, String... buttonText) {
        // Pop alert dialog box with 1 button

        final Builder alertBuilder = new Builder(this); // alert dialog box

        if (alertShown) {
            // dismiss the alert dialog if already visible
            alert.cancel();
            alertShown = false;
        }

        alertBuilder.setTitle(title);
//        alertBuilder.setMessage(message);

        alertBuilder.setItems(buttonText,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(getApplicationContext(), "Clicked " + which, Toast.LENGTH_SHORT).show();
                    }
                });

        alert = alertBuilder.create();
        alert.show();
        alertShown = true;
    }

    @Override
    public void handleMessageResponse(IncomingMessage incomingMessage) {
    // The receiving end of OHAP Protocol messages from tcp-ohap-server application
    // Protocol version:    0.3.1 (March 11, 2016)
    // Messages:            From server to client
    //
    // About OHAP protocol:
    // http://ohap.opimobi.com/ohap_specification_20160311.html (v0.3.1)
    //
    //
    // Example:
    //    message-type-binary-actuator      int8
    //    item-identifier                   int32
    //    binary-value                      boolean
    //    item-data-parent-identifier       int32
    //    item-data-name                    text
    //    item-data-description             text
    //    item-data-internal                binary8
    //
    //    itemIdentifier = incomingMessage.integer32();
    //    itemBinaryValue = incomingMessage.binary8();
    //    itemDataParentIdentifier = incomingMessage.integer32();
    //    itemDataName = incomingMessage.text();
    //    itemDataDescription = incomingMessage.text();
    //    itemDataInternal = incomingMessage.binary8();

    // TODO move to separate class?


        if (null != incomingMessage) {

            int msgType;
            long itemIdentifier;
            long itemDataParentIdentifier;
            String itemDataName;
            String itemDataDescription;
            boolean itemDataInternal;
            boolean itemBinaryValue;
            long pingIdentifier;
            String logoutErrorMsg;
            double itemDecimalValue;
            double decimalMin;
            double decimalMax;
            String decimalUnit;
            String decimalAbbreviation;

            Device device = null;
            MessageLog messageLog = null;
            String msg = "";
            String msgTypeText = "";

            msgType = incomingMessage.integer8(); // get message type
            switch (msgType) {

                case 0x01:
                    // handle error message (=server logout identifier)
                    msgTypeText = "message-type-logout";

                    logoutErrorMsg = incomingMessage.text();

                    msg = msgTypeText + "\n";
                    msg += "logout-error-text " + logoutErrorMsg;

                    Log.i(TAG, "handleMessageResponse() \n" + msg);
                    Log.e(TAG, "handleMessageResponse() Error received from server: " + logoutErrorMsg);

                    // inform user of the error with alert dialog
                    alertDialogMessageOk("Server error message", logoutErrorMsg, "OK");

                    break;

                case 0x02:
                    // handle message type ping
                    msgTypeText = "message-type-ping";

                    pingIdentifier = incomingMessage.integer32();

                    msg = msgTypeText + "\n";
                    msg += "ping-identifier: " + pingIdentifier + "\n";

                    Log.i(TAG, "handleMessageResponse() \n" + msg);

                    // send pong and notify user
                    centralUnit.sendPing();

                    // inform user
                    Toast.makeText(this, "Server ping", Toast.LENGTH_SHORT).show();

                    break;

                case 0x03:
                    // handle message type pong
                    msgTypeText = "message-type-pong";

                    // calculate latency using ping message
                    pingIdentifier = incomingMessage.integer32();
                    long latencyMs = (SystemClock.uptimeMillis()-pingIdentifier);

                    msg = msgTypeText + "\n";
                    msg += "ping-identifier: " + pingIdentifier + "\n";

                    Log.i(TAG, "handleMessageResponse() \n" + msg);
                    Log.i(TAG, "handleMessageResponse() Ping reply received from " + centralUnit.getURL().toString() + msg);

                    // inform user
                    Toast.makeText(this, "Latency: " + latencyMs + "ms", Toast.LENGTH_SHORT).show();

                    break;

                case 0x04:
                    // handle message type decimal sensor
                    msgTypeText = "message-type-decimal-sensor";

                    itemIdentifier = incomingMessage.integer32();
                    itemDecimalValue = incomingMessage.decimal64();
                    itemDataParentIdentifier = incomingMessage.integer32();
                    itemDataName = incomingMessage.text();
                    itemDataDescription = incomingMessage.text();
                    itemDataInternal = incomingMessage.binary8();
                    decimalMin = incomingMessage.decimal64();
                    decimalMax = incomingMessage.decimal64();
                    decimalUnit= incomingMessage.text();
                    decimalAbbreviation= incomingMessage.text();

                    msg = msgTypeText + "\n";
                    msg += "item-identifier: " + itemIdentifier + "\n";
                    msg += "decimal-value: " + itemDecimalValue + "\n";
                    msg += "item-data-parent-identifier: " + itemDataParentIdentifier + "\n";
                    msg += "item-data-name: " + itemDataName + "\n";
                    msg += "item-data-description: " + itemDataDescription + "\n";
                    msg += "item-data-internal: " + itemDataInternal + "\n";
                    msg += "decimal-min: " + decimalMin + "\n";
                    msg += "decimal-max: " + decimalMax + "\n";
                    msg += "decimal-unit: " + decimalUnit + "\n";
                    msg += "decimal-abbreviation: " + decimalAbbreviation + "\n";

                    Log.i(TAG, "handleMessageResponse() \n" + msg);

                    try {
                        // get parent container
                        Container container = (Container)centralUnit.getItemById(itemDataParentIdentifier);

                        // create new decimal sensor device
                        device = new Device(container, itemIdentifier, Device.Type.SENSOR, Device.ValueType.DECIMAL);
                        device.setDecimalValue(itemDecimalValue);
                        device.setMinMaxValues(decimalMin, decimalMax);
                        device.setUnit(decimalUnit, decimalAbbreviation);
                        device.setName(itemDataName);
                        device.setDescription(itemDataDescription);

                        // add item to placeholder
                        listItems.add(device);

                        Log.i(TAG, "handleMessageResponse() New device " + itemIdentifier + " added to container " + itemDataParentIdentifier);

                    } catch (Exception e) {
                        Log.e(TAG, "handleMessageResponse() Unable to create device: " + e.getMessage());
                    }

                    break;

                case 0x05:
                    msgTypeText = "message-type-decimal-actuator";

                    msg = msgTypeText + "\n";

                    Log.i(TAG, "handleMessageResponse() " + msg);

                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

                    break;

                case 0x06:
                    msgTypeText = "message-type-binary-sensor";

                    msg = msgTypeText + "\n";

                    Log.i(TAG, "handleMessageResponse() " + msg);

                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

                    break;

                case 0x07:
                    // handle message type binary actuator
                    msgTypeText = "message-type-binary-actuator";

                    itemIdentifier = incomingMessage.integer32();
                    itemBinaryValue = incomingMessage.binary8();
                    itemDataParentIdentifier = incomingMessage.integer32();
                    itemDataName = incomingMessage.text();
                    itemDataDescription = incomingMessage.text();
                    itemDataInternal = incomingMessage.binary8();

                    msg = msgTypeText + "\n";
                    msg += "item-identifier: " + itemIdentifier + "\n";
                    msg += "binary-value: " + itemBinaryValue + "\n";
                    msg += "item-data-parent-identifier: " + itemDataParentIdentifier + "\n";
                    msg += "item-data-name: " + itemDataName + "\n";
                    msg += "item-data-description: " + itemDataDescription + "\n";
                    msg += "item-data-internal: " + itemDataInternal + "\n";

                    Log.i(TAG, "handleMessageResponse() \n" + msg);

                    try {
                        // get parent container
                        Container container = (Container)centralUnit.getItemById(itemDataParentIdentifier);

                        // create new binary actuator
                        device = new Device(container, itemIdentifier, Device.Type.ACTUATOR, Device.ValueType.BINARY);
                        device.setBinaryValue(itemBinaryValue);
                        device.setName(itemDataName);
                        device.setDescription(itemDataDescription);

                        // add item to placeholder
                        listItems.add(device);

                        Log.i(TAG, "handleMessageResponse() New device " + itemIdentifier + " added to container " + itemDataParentIdentifier);

                    } catch (Exception e) {
                        Log.e(TAG, "handleMessageResponse() Unable to create device: " + e.getMessage());
                    }

                    break;

                case 0x08:
                    // handle message type container
                    msgTypeText = "message-type-container";

                    itemIdentifier = incomingMessage.integer32();
                    itemDataParentIdentifier = incomingMessage.integer32();
                    itemDataName = incomingMessage.text();
                    itemDataDescription = incomingMessage.text();
                    itemDataInternal = incomingMessage.binary8();

                    msg = msgTypeText + "\n";
                    msg += "item-identifier: " + itemIdentifier + "\n";
                    msg += "item-data-parent-identifier: " + itemDataParentIdentifier + "\n";
                    msg += "item-data-name: " + itemDataName + "\n";
                    msg += "item-data-description: " + itemDataDescription + "\n";
                    msg += "item-data-internal: " + itemDataInternal+ "\n";

                    Log.i(TAG, "handleMessageResponse() \n" + msg);

                    try {
                        // set container properties

                        if (itemIdentifier == 0) {
                            // update central unit with the information received
                            if (centralUnit.getId() == -1) {
                                centralUnit = getCentralUnit();
                                Log.d(TAG, "handleMessageResponse() New central unit: " + centralUnit.getId());
                            }

                            centralUnit.setName(itemDataName);
                            centralUnit.setDescription(itemDataDescription);
                            Log.i(TAG, "handleMessageResponse() New central unit: " + centralUnit.getId());

                            // send listening start to server
                            centralUnit.sendListeningStart(centralUnit);

                        } else {
                            Container c = (Container) centralUnit.getItemById(itemIdentifier);
                            if (c != null)
                                c.startListening();

                            // create new container with provided data
                            Container container = new Container(centralUnit, itemIdentifier);
                            container.setName(itemDataName);
                            container.setDescription(itemDataDescription);

                            // add item to placeholder
                            listItems.add(container);

                            // add container to listeners and send listening start
                            container.startListening();
                            listeningContainerIds.add(container.getId());

                            Log.i(TAG, "handleMessageResponse() New container: " + container.getId() + " added to parent " + container.getParent().getId());
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "handleMessageResponse() Unable to create new container: " + e.getMessage());
                    }

                    break;

                case 0x09:
                    // handle decimal value changed

                    msgTypeText = "message-type-decimal-changed";

                    itemIdentifier = incomingMessage.integer32();
                    itemDecimalValue = incomingMessage.decimal64();

                    msg = msgTypeText + "\n";
                    msg += "item-identifier: " + itemIdentifier + "\n";
                    msg += "item-decimal-value: " + itemDecimalValue + "\n";

                    Log.i(TAG, "handleMessageResponse() \n" + msg);

                    try {
                        // set properties
                        device = (Device) centralUnit.getItemById(itemIdentifier);
                        device.setDecimalValue(itemDecimalValue);
                        Log.i(TAG, "handleMessageResponse() Device " + device.getId() + " changed decimal value = " + device.getDecimalValue());
                    } catch (Exception e){
                        Log.e(TAG, "handleMessageResponse() Unable to set device " + device.getId() + " value, reason: " + e.getMessage());
                    }
                    break;

                case 0x0a:
                    // handle binary value changed
                    msgTypeText = "message-type-binary-changed";

                    itemIdentifier = incomingMessage.integer32();
                    itemBinaryValue = incomingMessage.binary8();

                    msg = msgTypeText + "\n";
                    msg += "item-identifier: " + itemIdentifier + "\n";
                    msg += "item-binary-value: " + itemBinaryValue + "\n";

                    Log.i(TAG, "handleMessageResponse() \n" + msg);

                    try {
                        // set properties
                        device = (Device) centralUnit.getItemById(itemIdentifier);
                        device.setBinaryValue(itemBinaryValue);
                        Log.i(TAG, "handleMessageResponse() Device " + device.getId() + " changed binary value = " + device.getBinaryValue());
                    } catch (Exception e){
                        Log.e(TAG, "handleMessageResponse() Unable to set device " + device.getId() + " value, reason: " + e.getMessage());
                    }
                    break;
                case 0x0b:
                    msgTypeText = "message-type-item-removed";

                    msg = msgTypeText + "\n";

                    Log.i(TAG, "handleMessageResponse() " + msg);
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    break;
                case 0x0c:
                    msgTypeText = "message-type-listening-start";

                    msg = msgTypeText + "\n";

                    Log.i(TAG, "handleMessageResponse() " + msg);
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    break;
                case 0x0d:
                    msgTypeText = "message-type-listening-stop";

                    msg = msgTypeText + "\n";

                    Log.i(TAG, "handleMessageResponse() " + msg);
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    break;
                default:
                    msgTypeText = "unsupported-message-type";

                    msg = msgTypeText + "\n";

                    alertDialogMessageOk("Message error", "Unsupported message-type \"" + msgType + "\"", "OK");
                    break;
            }

            // forward the message to message logging facility
            new MessageLog(logContainer, msg, msgTypeText);
            Log.d(TAG, "handleMessageResponse() Log item count " + logContainer.getItemCount());

        } else {
            Log.wtf(TAG, "handlePingResponse() Has been called with null message type!");
        }
    }

    @Override
    public void handleActivityResponse(String messageAction) {
        // handle connection error message sent from the container object

        // TODO change error message to enum types

        Log.d(TAG, "handleActivityResponse() New message action received: " + messageAction);

        String ttl = "Connection error";
        String msg;

        if (messageAction.equals("No connection")) {
//            if (!connecting) {
            msg = messageAction + " to server " + centralUnit.getURL().getHost() + ":" + centralUnit.getURL().getPort();
            alertDialogMessageOk(ttl, msg, "OK");
//            }

        } else if (messageAction.equals("Connection closed")) {
            msg = messageAction + " to server " + centralUnit.getURL().getHost() + ":" + centralUnit.getURL().getPort();
            alertDialogMessageOk(ttl, msg, "OK");
            stop();
            connect();

        } else if (messageAction.equals("Connected")){
            msg = "Connected to server: " + centralUnit.getURL().getHost() + ":" + centralUnit.getURL().getPort();
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();

        } else if (messageAction.equals("Reconnecting")){
            msg = "Reconnecting...";
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

        } else {
            msg = "Unhandled connection message received: " + messageAction;
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        }
    }


}
