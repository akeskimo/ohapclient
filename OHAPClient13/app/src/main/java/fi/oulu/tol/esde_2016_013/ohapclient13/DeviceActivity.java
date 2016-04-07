package fi.oulu.tol.esde_2016_013.ohapclient13;

/**
 * Description:
 * Main Activity for an OHAP client application for monitoring and actuating house devices via
 * remote control Android application
 *
 * Compatibility:
 * Android SDK API15 and up
 *
 * Change history:
 * v1.0     Aapo Keskimolo      Initial version with layout, OHAP server, 1 dummy device, widgets and listeners
 * v1.1     Aapo Keskimolo      Changed visibility of widgets, Added logging, Changed app title to device name and did some maintenance
 * v1.2     Aapo Keskimolo      Made DeviceActivity 2nd entry for app and removed dummy code
 *
 * @author Aapo Keskimolo &lt;aapokesk@gmail.com>
 * @version 1.2
 */

import com.opimobi.ohap.*;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.SeekBar;
import android.widget.TextView;

import java.net.MalformedURLException;


public class DeviceActivity extends ActionBarActivity implements CompoundButton.OnCheckedChangeListener, SeekBar.OnSeekBarChangeListener {
    //////////////////////////////////////////////////////////////////////////////////
    // Device Activity for OHAPClient13 application
    //////////////////////////////////////////////////////////////////////////////////

    // Log tag
    private final String TAG = this.getClass().getSimpleName();

    // Intent extras
    private final static String CENTRAL_UNIT_URL = "fi.oulu.tol.esde_2016_013.ohapclient13.CENTRAL_UNIT_URL";
    private final static String DEVICE_ID = "fi.oulu.tol.esde_2016_013.ohapclient13.DEVICE_ID";

    // Widgets
    private TextView textViewContainerName = null;
    private TextView textViewDeviceName = null;
    private TextView textViewDeviceDesc = null;
    private SeekBar seekBar = null;
    private TextView textViewSeekBar = null;
    private Switch switch1 = null;
    private TextView textViewSwitch = null;

    // singleton central unit container
    private static CentralUnit centralUnit = null;
    // active device displayed on the UI
    private Device activeDevice = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Loading XML layout and saving Bundle of instance state
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        // Getting references for Text View Widgets
        textViewContainerName = (TextView)(findViewById(R.id.textViewContainerName));
        textViewDeviceName = (TextView)(findViewById(R.id.textViewDeviceName));
        textViewDeviceDesc = (TextView)(findViewById(R.id.textViewDeviceDesc));
        textViewSeekBar = (TextView)(findViewById(R.id.textViewSeekBar)); // displays dec value
        textViewSwitch = (TextView)(findViewById(R.id.textViewSwitch)); // displays bin value

        // Getting reference for seekbar (for changing decimal value)
        seekBar = (SeekBar)(findViewById(R.id.seekBar));
        seekBar.setOnSeekBarChangeListener(this); // start listening for seekbar user input

        // Getting reference for switch object (for changing binary value)
        switch1 = (Switch)(findViewById(R.id.switch1));
        switch1.setOnCheckedChangeListener(this); // start listening for switch bar user input

        // get url from intent
        String newUrl = getIntent().getStringExtra(CENTRAL_UNIT_URL);
        final String central_unit_url = newUrl != null ? newUrl : getResources().getString(R.string.server_url);
        Log.i(TAG, "onCreate() New url received from intent: " + central_unit_url);

        try {
            // Instantiating CentralUnit placeholder for all containers
            centralUnit = CentralUnitConnection.getInstance(); // get instance of singleton

            Log.i(TAG, "onCreate() CentralUnit: " + centralUnit
                    + " name: " + centralUnit.getName()
                    + " container id: " + centralUnit.getId()
                    + " item count: " + centralUnit.getItemCount()
                    + " listening state: " + centralUnit.isListening());
        } catch (MalformedURLException e) {
            Log.e(TAG, "onCreate() Unable to instantiate CentralUnit: " + e.getMessage());
        }

        // get device id from intent
        int newId = getIntent().getIntExtra(DEVICE_ID, -1);
        Log.i(TAG, "onCreate() Device id received from intent: " + newId);
        if (newId == -1) {
            Log.e(TAG, "onCreate() Invalid DEVICE_ID -> set DEVICE_ID = 1: " + newId);
        }
        final int device_id = newId != -1 ? newId: 1;


        // Casting device id
        if (centralUnit != null)
            activeDevice = (Device)centralUnit.getItemById(device_id);
        else
            Log.e(TAG, "onCreate() Central device == null!");

        if (activeDevice != null) {
            // Log device information
            Log.i(TAG, "onCreate() Device: " + activeDevice
                    + " name: " + activeDevice.getName()
                    + " device id: " + activeDevice.getId()
                    + " type: " + activeDevice.getType()
                    + " valueType: " + activeDevice.getValueType());

            // Changing App title to show active device type
            if (activeDevice.getType() == Device.Type.ACTUATOR) {
                setTitle("ACTUATOR");
            } else if (activeDevice.getType() == Device.Type.SENSOR) {
                setTitle("SENSOR");
            } else {
                setTitle("-");
                Log.wtf(TAG, "onCreate() Unable to set App title: Invalid device type (" + activeDevice.getType() + ")");
            }

            // Visibility of bars is changed according to the active device value type:
            // Switch is visible with binary and seekbar only with decimal values
            if (activeDevice.getValueType() == Device.ValueType.BINARY) {
                seekBar.setVisibility( View.GONE);
                textViewSeekBar.setVisibility( View.GONE);
                switch1.setChecked( activeDevice.getBinaryValue() );
            } else if (activeDevice.getValueType() == Device.ValueType.DECIMAL) {
                switch1.setVisibility( View.GONE);
                textViewSwitch.setVisibility( View.GONE);
                seekBar.setProgress( (int)activeDevice.getDecimalValue() );
            } else {
                Log.wtf(TAG, "onCreate() Unable to set device visibility: Invalid device value type (" + activeDevice.getValueType() + ")");
            }

            // Setting values on the TextView objects to the device values
            // TODO The top TextView should show the container hierarchy (will get from the server?)
            textViewContainerName.setText(getResources().getString(R.string.container_hierarchy));
            textViewDeviceName.setText(activeDevice.getName());
            textViewDeviceDesc.setText(activeDevice.getDescription());
        }
        else {
            Log.e(TAG, "onCreate() Active device == null! Unable to set View attributes! Device id: " + newId + ", container id: " + centralUnit.getId());
        }
    }


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        // Implementation for the method that is called whenever the binary Switch value has changed
        activeDevice.setBinaryValue(isChecked);
        try {
            textViewSwitch.setText( "Device value: " + Boolean.toString(activeDevice.getBinaryValue())); // display device value
        } catch (Exception e) {
            Log.wtf(TAG, "onCheckedChanged() Unable to set text on switch: " + e.getMessage() );
            e.printStackTrace();
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        // Implementation for the method that is called whenever the decimal seekbar value has changed
        activeDevice.setDecimalValue(progress);
        try {
            textViewSeekBar.setText( "Device value: " + Double.toString(activeDevice.getDecimalValue())); // display device value
        } catch (Exception e) {
            Log.wtf(TAG, "onProgressChanged() Unable to set text on seekBar: " + e.getMessage() );
            e.printStackTrace();
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_device, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.device_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onDestroy() {
        Log.i(TAG, "onDestroy() Activity has been terminated.");
        super.onDestroy();
    }

}