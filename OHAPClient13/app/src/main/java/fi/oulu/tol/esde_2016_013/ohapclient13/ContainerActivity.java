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
 * v1.3     Aapo Keskimolo      Fixed inconsistent container listener list
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
import android.widget.TextView;
import android.widget.Toast;

import com.opimobi.ohap.CentralUnitConnection;
import com.opimobi.ohap.ConnectionObserver;
import com.opimobi.ohap.Container;
import com.opimobi.ohap.Device;
import com.opimobi.ohap.Item;
import com.opimobi.ohap.message.IncomingMessage;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fi.oulu.tol.esde_2016_013.ohapclient13.utility.LogContainer;
import fi.oulu.tol.esde_2016_013.ohapclient13.utility.MessageLog;


public class ContainerActivity extends ActionBarActivity implements ConnectionObserver,
        SensorEventListener {

    // log tag
    private final String TAG = this.getClass().getSimpleName();

    // intent extras
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
    private boolean shakeReconnectEnabled = true;

    // alert dialog box
    private boolean alertShown = false;
    AlertDialog alert = null;

    // container listener
    List<Long> listeningContainerIds = new ArrayList<>();
    ArrayList<Container> listeningContainers = new ArrayList<>();
    ArrayList<Item> listItems = new ArrayList<>();

    // log utilities place holder
    LogContainer logContainer = null;

    // preference change variable
    private boolean preferenceChanged = false;

    // activity state
    private boolean active;

    // display headers
    TextView textViewContainerName;
    TextView textViewUrlAddress;
    TextView textViewStatus;
    private static int runnableCtr = 0;

    private TimerThread timer = null;

    // timer thread for querying connection status
    private class TimerThread extends Thread {

        // sleep time between polls
        private int sleepTime = 500;

        public void run() {

            Log.d(TAG, "timer().run() Started");

            while(true) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Log.d(TAG, "TimerThread.run() Interrupted: " + e.getMessage());
                    break;
                }

                runnableCtr++;
                runOnUiThread(updateViewRunnable);
            }
        }
    };


    // runnable object to update the connection status text view widgets
    private Runnable updateViewRunnable = new Runnable() {

        @Override
        public void run() {
            // update container activity headers (text views)
            textViewContainerName.setText(container.getName());
            textViewUrlAddress.setText(centralUnit.getURL().toString());
            textViewStatus.setText(centralUnit.getConnectionStatus().toString());

//            Log.d(TAG, "Container name = " + container.getName() + "\nURL = " + centralUnit.getURL() + "\nStatus = " + centralUnit.getConnectionStatus().toString());
        }
    };

    /////////////////////////////////////////////////////////////////////////////////////////////
    // Android activity life cycle methods start below this line
    /////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_container);

        // app title
        setTitle("OHAPClient13");

        // log utilities
        logContainer = LogContainer.getInstance();

        textViewContainerName = (TextView) findViewById(R.id.textViewContainerName);
        textViewUrlAddress =  (TextView) findViewById(R.id.textViewUrlAddress);
        textViewStatus = (TextView) findViewById(R.id.textViewConnectionStatus);

        // start sensor manager and get reference to accelerometer for shake detection
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE); // sensor system mgr
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER); // acc sensor

        // getting the instance of CentralUnit
        centralUnit = CentralUnitConnection.getInstance();
        Log.i(TAG, "onCreate() centralUnit obj: " + centralUnit + " name: " + centralUnit.getName() + " container id: " + centralUnit.getId() + " item count: " + centralUnit.getItemCount() + " listening state: " + centralUnit.isListening());

        // loads preferences (Central Unit URL, Auto-connect, etc.)
        loadPreferences();
    }


    @Override
    protected void onResume() {
        super.onResume();

        Log.d(TAG, "onResume() Called");

        // get container passed to the activity
        long containerId = getIntent().getLongExtra(CONTAINER_ID, 0);
        Log.d(TAG, "onResume() Got intent extra, CONTAINER_ID = " + containerId);
        container = (Container) centralUnit.getItemById(containerId);

        // create list view objects with list adapter
        createListView();

        // start networking
        connect();

        // register sensor listener
        if (shakeReconnectEnabled)
            sensorManager.registerListener(this, accelerometer, sensorManager.SENSOR_DELAY_NORMAL);

        // set display headers
        textViewContainerName.setText(centralUnit.getName());
        textViewUrlAddress.setText(centralUnit.getURL().toString());
        textViewStatus.setText(centralUnit.getConnectionStatus().toString());
    }


    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause() Called");

        // unregister sensor listener
        if (shakeReconnectEnabled)
            sensorManager.unregisterListener(this);

        active = false;
        stop();
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

        if (id == R.id.menu_main) {
            // return to main activity
            Intent intent = new Intent(ContainerActivity.this, ContainerActivity.class);
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
//
//        active = true;
//
//        if (timer == null)
//            timer = new TimerThread();
//
//        // start timer thread
//        if (!timer.isAlive())
//            timer.start();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop() Called");

//        active = false;
//        stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy() Called");

        // stop timer thread
        if (timer != null) {
            timer.interrupt();
            try {
                timer.join();
            } catch (InterruptedException e) {
                Log.e(TAG, "onDestroy() Unable to join timer thread: " + e.getMessage());
            }
            timer = null;
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////////////
    // Other methods start below this line
    /////////////////////////////////////////////////////////////////////////////////////////////

    private void createListView() {
        // Create ListAdapter and set it to ListView

        try {
            ListView listView = (ListView) findViewById(R.id.listView);
            listView.setAdapter(new ContainerListAdapter(container));

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    // list view item onclick callback

                    Log.d(TAG, "createListView() Position = " + position + ", id = " + container.getItemByIndex(position).getId() + ", container = " + container.getItemByIndex(position).getName() );

                    Intent intent;

                    if (container.getItemByIndex(position) instanceof Container) {

                        // selected item is container, launch container activity
                        intent = new Intent(getApplicationContext(), ContainerActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra(CONTAINER_ID, container.getItemByIndex(position).getId());

                    } else {

                        Log.d(TAG, "position = " + position + ", device = " + container.getItemByIndex(position).getName() );

                        // select item is device, launch device activity
                        intent = new Intent(ContainerActivity.this, DeviceActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra(DEVICE_ID, container.getItemByIndex(position).getId() );
                        intent.putExtra(CONTAINER_ID, container.getItemByIndex(position).getId() );
                    }

                    getApplicationContext().startActivity(intent);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "createListView() Unable to create ListView / Launch intent: " + e.getMessage());
        }
    }


    private void loadPreferences() {
        // load user default preferences on activity start

        // laoad shared preferences (settings stored in shared preference manager)
        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(this);

        String port_key;
        String url_key;
        String port_value;
        String url_value;
        String autoconnect_key;
        String shake_key;
        String shake_value;
        boolean autoConnect;

        // preference keys
        autoconnect_key = getString(R.string.pref_autoconnect_key);
        port_key = getString(R.string.pref_port_key);
        url_key = getString(R.string.pref_url_key);
        shake_key = getString(R.string.pref_reconnect_on_shake_key);


        // load auto-connect setting
        try {
            autoConnect = shared.getBoolean(autoconnect_key,
                    Boolean.parseBoolean(getString(R.string.pref_autoconnect_default)));
        } catch (Exception e) {
            Log.e(TAG, "loadPreferences() Unable to parse resource default value: " + getString(R.string.pref_autoconnect_default) + " to boolean. Forcing default value = true.");
            autoConnect = true;
        }
        Log.i(TAG, "loadPreferences() Preference loaded, key = " + autoconnect_key + ", value = " + autoConnect);



        // load reconnect shake settings
        try {
            shakeReconnectEnabled = shared.getBoolean(shake_key,
                    Boolean.parseBoolean(getString(R.string.pref_reconnect_on_shake_default)));
        } catch (Exception e) {
            Log.e(TAG, "loadPreferences() Unable to parse resource default value: " + getString(R.string.pref_reconnect_on_shake_default) + " to boolean. Forcing default value = true.");
            shakeReconnectEnabled = true;
        }
        Log.i(TAG, "loadPreferences() Preference loaded, key = " + shake_key + ", value = " + shakeReconnectEnabled);

        // load port settings
        port_value = shared.getString(port_key, getString(R.string.pref_port_default));

        if (port_value.equals("") || port_value.equals("-1")) {
            // if port is empty/invalid, use resource values
            Log.i(TAG, "loadPreference() Shared preference port is empty: Loading from xml resource");
            port_value = getString(R.string.pref_port_default);
            SharedPreferences.Editor editor = shared.edit();
            editor.putString(port_key, port_value);
            editor.commit();
        }

        Log.i(TAG, "loadPreferences() Preference loaded, key = " + port_key + ", value = " + port_value);

        // load url setting
        url_value = shared.getString( url_key, getString(R.string.pref_url_default));

        if (url_value.equals("")) {
            // if url is empty, use resource values
            Log.i(TAG, "loadPreference() Shared preference url is empty: Loading from xml resource");
            url_value = getString(R.string.pref_url_default);
            SharedPreferences.Editor editor = shared.edit();
            editor.putString(url_key, url_value);
            editor.commit();
        }

        Log.i(TAG, "loadPreferences() Preference loaded, key = " + url_key + ", value = " + url_value);


        // combine ip address with protocol and port
        String address = url_value + ":" + port_value + "/";

        try {
            // set central unit with the url obtained from preferences
            centralUnit.setAutoConnect(autoConnect);
            centralUnit.initialize(new URL(address), this);

            Log.i(TAG, "loadPreferences() CentralUnit initialized with URL: " + centralUnit.getURL());
        } catch (MalformedURLException e) {
            Log.e(TAG, "loadPreferences()Malformed url: \"" + address + "\", reason: " + e.getMessage());
        }
    }


    private void connect() {
        // handle connect request

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
                String msg = "Connect the device to WIFI or mobile network\n\nTry shaking the device to reconnect";
                alertDialogMessageOk(ttl, msg, "OK");
            }

        } else {
            // auto-connect feature is switched off

            stop();

            // display a dialog to inform user that auto connection is switched off
            String ttl = "Auto-connect is off";
            String msg = "You may switch auto-connection on in 'Settings'-menu or shake the device to force reconnect";
            alertDialogMessageOk(ttl, msg, "OK");
        }
    }

    private void stop() {
        // handle stop request
//        stopContainerListening();
//        destroyItems();
//        centralUnit.stop();
    }

    private void reconnect() {
        // handle re-connect request
        Toast.makeText(this, "Reconnecting...", Toast.LENGTH_SHORT).show();
        centralUnit.reconnect(this);
    }

    private void destroyItems() {
        // removes all items and containers from central unit
        for (Item item: listItems) {
            item.destroy();
        }
    }


    private void stopContainerListening() {
        // stops containers from listening
        Log.d(TAG, "stopContainerListening() Containers currently listening: " + listeningContainerIds);
        for (Container container: listeningContainers) {
            container.stopListening();
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

            // request reconnect to server (sensor action)
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
            // dismiss the alert dialog, in case it is visible
            alert.cancel();
            alertShown = false;
        }

        alertBuilder.setTitle(title);
        alertBuilder.setMessage(message);
        alertBuilder.setPositiveButton(buttonText, null);

        alert = alertBuilder.create();
        if (active) {
            alert.show();
            alertShown = true;
        }
    }

    public void alertDialogCustom(String title, String message, String... buttonText) {
        // pop alert dialog box with 1 button

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
    // The receiving end of OHAP Protocol messages from tcp-ohap-server
    // Protocol version:    0.3.1 (March 11, 2016)
    // Messages:            From server to client
    //
    // About OHAP protocol:
    // http://ohap.opimobi.com/ohap_specification_20160311.html (v0.3.1)
    //
    //
    // Protocol Message Example:
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

    // TODO move to separate file?


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

            Device newDevice = null;
            Container newContainer = null;
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
                    Log.i(TAG, "handleMessageResponse() Ping reply received from " + centralUnit.getURL().toString());

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
                        newContainer = (Container)centralUnit.getItemById(itemDataParentIdentifier);

                        // create new decimal sensor device
                        newDevice = new Device(newContainer, itemIdentifier, Device.Type.SENSOR, Device.ValueType.DECIMAL);
                        newDevice.setDecimalValue(itemDecimalValue);
                        newDevice.setMinMaxValues(decimalMin, decimalMax);
                        newDevice.setUnit(decimalUnit, decimalAbbreviation);
                        newDevice.setName(itemDataName);
                        newDevice.setDescription(itemDataDescription);

                        // add item to placeholder
                        listItems.add(newDevice);

                        Log.i(TAG, "handleMessageResponse() New device " + itemIdentifier + " added to container " + itemDataParentIdentifier);

                    } catch (Exception e) {
                        Log.e(TAG, "handleMessageResponse() Unable to create device: " + e.getMessage());
                    }

                    break;

                case 0x05:
                    msgTypeText = "message-type-decimal-actuator";

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
                        newContainer = (Container)centralUnit.getItemById(itemDataParentIdentifier);

                        // create new decimal sensor device
                        newDevice = new Device(newContainer, itemIdentifier, Device.Type.ACTUATOR, Device.ValueType.DECIMAL);
                        newDevice.setDecimalValue(itemDecimalValue);
                        newDevice.setMinMaxValues(decimalMin, decimalMax);
                        newDevice.setUnit(decimalUnit, decimalAbbreviation);
                        newDevice.setName(itemDataName);
                        newDevice.setDescription(itemDataDescription);

                        // add item to placeholder
                        listItems.add(newDevice);

                        Log.i(TAG, "handleMessageResponse() New device " + itemIdentifier + " added to container " + itemDataParentIdentifier);

                    } catch (Exception e) {
                        Log.e(TAG, "handleMessageResponse() Unable to create device: " + e.getMessage());
                    }

                    break;

                case 0x06:
                    msgTypeText = "message-type-binary-sensor";

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
                        newContainer = (Container)centralUnit.getItemById(itemDataParentIdentifier);

                        // create new binary actuator
                        newDevice = new Device(newContainer, itemIdentifier, Device.Type.SENSOR, Device.ValueType.BINARY);
                        newDevice.setBinaryValue(itemBinaryValue);
                        newDevice.setName(itemDataName);
                        newDevice.setDescription(itemDataDescription);

                        // add item to placeholder
                        listItems.add(newDevice);

                        Log.i(TAG, "handleMessageResponse() New device " + itemIdentifier + " added to container " + itemDataParentIdentifier);

                    } catch (Exception e) {
                        Log.e(TAG, "handleMessageResponse() Unable to create device: " + e.getMessage());
                    }

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
                        newContainer = (Container)centralUnit.getItemById(itemDataParentIdentifier);

                        // create new binary actuator
                        newDevice = new Device(newContainer, itemIdentifier, Device.Type.ACTUATOR, Device.ValueType.BINARY);
                        newDevice.setBinaryValue(itemBinaryValue);
                        newDevice.setName(itemDataName);
                        newDevice.setDescription(itemDataDescription);

                        // add item to placeholder
                        listItems.add(newDevice);

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
                            // new central unit

                            if (centralUnit.getId() == -1) {
                                centralUnit = CentralUnitConnection.getInstance();
                                Log.d(TAG, "handleMessageResponse() New central unit: " + centralUnit.getId());
                            }

                            newContainer = centralUnit;

                            centralUnit.setName(itemDataName);
                            centralUnit.setDescription(itemDataDescription);

                            Log.i(TAG, "handleMessageResponse() New central unit: " + centralUnit.getId());

                            // send listening start to server
                            centralUnit.sendListeningStart(centralUnit);
                            listeningContainerIds.add(container.getId());

                        } else {
                            // new child container

                            newContainer = (Container) centralUnit.getItemById(itemIdentifier);

                            if (newContainer != null) {
                                newContainer.startListening();
                                listeningContainerIds.add(container.getId());

                            } else {
                                // add new container
                                Container parentContainer = (Container)centralUnit.getItemById(itemDataParentIdentifier);

                                // create new container with provided data
                                newContainer = new Container(parentContainer, itemIdentifier);
                                newContainer.setName(itemDataName);
                                newContainer.setDescription(itemDataDescription);

                                // start listening
                                newContainer.startListening();
                                listeningContainerIds.add(newContainer.getId());

                                // add to item placeholder to be removed after application is closed
                                listItems.add(newContainer);

                                // add container to listeners and send listening start
                                //                            container.startListening();
                                //                            listeningContainerIds.add(container.getId());

                                Log.i(TAG, "handleMessageResponse() New container: " + newContainer.getId() + " added to parent " + newContainer.getParent().getId());
                            }
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "handleMessageResponse() Unable to create new container: " + e.getMessage());
                    }

                    if (!listeningContainers.contains(newContainer)) {
                        // add container to listening container list
                        listeningContainers.add(newContainer);
                        Log.i(TAG, "handleMessageResponse() New container: " + newContainer.getId() + " added to listening list");
                    } else
                        Log.i(TAG, "handleMessageResponse() Container: " + newContainer.getId() + " is already in the listening list (items: " + listeningContainers.size() + ")");

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
                        newDevice = (Device) centralUnit.getItemById(itemIdentifier);
                        newDevice.setDecimalValue(itemDecimalValue);
                        Log.i(TAG, "handleMessageResponse() Device " + newDevice.getId() + " changed decimal value = " + newDevice.getDecimalValue());
                    } catch (Exception e){
                        Log.e(TAG, "handleMessageResponse() Unable to set device " + newDevice.getId() + " value, reason: " + e.getMessage());
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
                        newDevice = (Device) centralUnit.getItemById(itemIdentifier);
                        newDevice.setBinaryValue(itemBinaryValue);
                        Log.i(TAG, "handleMessageResponse() Device " + newDevice.getId() + " changed binary value = " + newDevice.getBinaryValue());
                    } catch (Exception e){
                        Log.e(TAG, "handleMessageResponse() Unable to set device " + newDevice.getId() + " value, reason: " + e.getMessage());
                    }

                    break;

                case 0x0b:
                    // item removed message
                    msgTypeText = "message-type-item-removed";

                    itemIdentifier = incomingMessage.integer32();

                    msg = msgTypeText + "\n";
                    msg += "item-identifier: " + itemIdentifier + "\n";

                    Log.i(TAG, "handleMessageResponse() \n" + msg);

                    try {
                        // remove item
                        Item item = centralUnit.getItemById(itemIdentifier);
                        Log.i(TAG, "handleMessageResponse() Item " + item.getId() + " removed from container " + item.getParent().getId() );
                        item.destroy();
                    } catch (Exception e){
                        Log.e(TAG, "handleMessageResponse() Unable to remove device " + newDevice.getId() + ", reason: " + e.getMessage());
                    }

                    break;

                case 0x0c:
                    // container start listening
                    msgTypeText = "message-type-listening-start";

                    itemIdentifier = incomingMessage.integer32();

                    msg = msgTypeText + "\n";
                    msg += "item-identifier: " + itemIdentifier + "\n";

                    Log.i(TAG, "handleMessageResponse() \n" + msg);

                    try {
                        // add listener
                        newContainer = (Container) centralUnit.getItemById(itemIdentifier);
                        newContainer.startListening();
                        listeningContainerIds.add(newContainer.getId());
                        Log.i(TAG, "handleMessageResponse() Container " + newContainer.getId() + " started listening" );

                    } catch (Exception e){
                        Log.e(TAG, "handleMessageResponse() Container " + newContainer.getId() + " unable to start listening, reason: " + e.getMessage());
                    }

                    break;

                case 0x0d:
                    msgTypeText = "message-type-listening-stop";

                    itemIdentifier = incomingMessage.integer32();

                    msg = msgTypeText + "\n";
                    msg += "item-identifier: " + itemIdentifier + "\n";

                    Log.i(TAG, "handleMessageResponse() \n" + msg);

                    try {
                        // add listener
                        newContainer = (Container) centralUnit.getItemById(itemIdentifier);
                        newContainer.stopListening();
                        listeningContainerIds.remove(newContainer.getId());
                        Log.i(TAG, "handleMessageResponse() Container " + newContainer.getId() + " stopped listening" );

                    } catch (Exception e){
                        Log.e(TAG, "handleMessageResponse() Container " + newContainer.getId() + " unable to stop listening, reason: " + e.getMessage());
                    }

                    break;

                default:
                    msgTypeText = "unsupported-message-type";

                    msg = msgTypeText + "\n";

                    alertDialogMessageOk("Message error", "Unsupported message-type \"" + msgType + "\"", "OK");
                    break;
            }

            // forward the message to message logging facility
            new MessageLog(logContainer, msg, msgTypeText, "SERVER");
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
            msg = "Unable to connect to server\n" + centralUnit.getURL() + "\n\nCheck URL and port in settings";
            alertDialogMessageOk(ttl, msg, "OK");
            stop();
            connect();

        } else if (messageAction.equals("Connection closed")) {
            msg = messageAction + " to server " + centralUnit.getURL().getHost() + ":" + centralUnit.getURL().getPort();
            alertDialogMessageOk(ttl, msg, "OK");
            stopContainerListening();
            stop();
            connect();

        } else if (messageAction.equals("Connected")){
            msg = "Connected to server: " + centralUnit.getURL().getHost() + ":" + centralUnit.getURL().getPort();
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();

        } else if (messageAction.equals("Reconnecting")){
            msg = "Connecting to server...";
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

        } else {
            msg = "Unhandled connection message received: " + messageAction;
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        }
    }
}
