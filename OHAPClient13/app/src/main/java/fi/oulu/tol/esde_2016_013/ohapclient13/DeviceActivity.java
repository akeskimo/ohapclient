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
 * v1.3     Aapo Keskimolo      Removed "device value" from switch/seekbar status bar
 *
 * @author Aapo Keskimolo &lt;aapokesk@gmail.com>
 * @version 1.3
 */

import com.opimobi.ohap.*;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import android.widget.SeekBar;
import android.widget.TextView;

import java.net.MalformedURLException;


public class DeviceActivity extends ActionBarActivity {
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
        seekBar = (SeekBar)(findViewById(R.id.seekBar));
        switch1 = (Switch)(findViewById(R.id.switch1));


        // Implementing listener for seekbar for changing decimal value of the Device
        seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

           @Override
           public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
               try {
                   activeDevice.setDecimalValue(progress);
                   textViewSeekBar.setText( String.format("%1.0f %%", activeDevice.getDecimalValue() )); // display device value
               } catch (Exception e) {
                   Log.e(TAG, "onProgressChanged() Unable to set device value: " + e.getMessage() );
               }
           }

           @Override
           public void onStartTrackingTouch(SeekBar seekBar) { }

           @Override
           public void onStopTrackingTouch(SeekBar seekBar) { }
        });


        // Implementing listener for switch for changing binary value of the Device
        switch1.setOnCheckedChangeListener( new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                    activeDevice.setBinaryValue(isChecked);
                    textViewSwitch.setText( Boolean.toString(activeDevice.getBinaryValue() )); // display device value
                } catch (Exception e) {
                    Log.e(TAG, "onProgressChanged() Unable to device value: " + e.getMessage() );
                }
            }

        });


        // Get URL from the parent intent (ContainerActivity)
        String newUrl = getIntent().getStringExtra(CENTRAL_UNIT_URL);
        final String central_unit_url = newUrl != null ? newUrl : getResources().getString(R.string.server_url);
        Log.i(TAG, "onCreate() New CENTRAL_UNIT_URL received from intent: " + central_unit_url);

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

        } catch (Exception e) {
            Log.e(TAG, "onCreate() Unhandled error occurred: " + e.getMessage());
        }

        // Get Device id from the parent intent (ContainerActivity)
        // Note: Item index is the index corresponding to the row user has clicked on the ListView
        // of ContainerActivity
        int newId = getIntent().getIntExtra(DEVICE_ID, -1); // if id not found, -1 is returned
        Log.i(TAG, "onCreate() New DEVICE_ID received from intent: " + newId);
        if (newId == -1) {
            Log.e(TAG, "onCreate() No device id found. Forcing active device_id to 1 (single device displayed)");
        }
        final int device_id = newId != -1 ? newId: 1;


        // If CentralUnit is instantiated, get the Device from CentralUnit
        if (centralUnit != null)
            // get device id
            activeDevice = (Device)centralUnit.getItemById(device_id);
        else
            Log.e(TAG, "onCreate() Central device == null!");


        // If Device was returned by CentralUnit, log device info and update UI widgets
        if (activeDevice != null) {

            // Log device information
            Log.i(TAG, "onCreate() Device: " + activeDevice
                    + " name: " + activeDevice.getName()
                    + " device id: " + activeDevice.getId()
                    + " type: " + activeDevice.getType()
                    + " valueType: " + activeDevice.getValueType());

            // Update header text on the action bar
            if (activeDevice.getType() == Device.Type.ACTUATOR) {
                setTitle("ACTUATOR");
            } else if (activeDevice.getType() == Device.Type.SENSOR) {
                setTitle("SENSOR");
            } else {
                setTitle("-");
                Log.wtf(TAG, "onCreate() Unable to set App title: Invalid device type (" + activeDevice.getType() + ")");
            }


            // Visibility of bars is changed according to the active device value type:
            if (activeDevice.getValueType() == Device.ValueType.BINARY) {
                // Switch is visible when active device has binary value type, only
                seekBar.setVisibility( View.GONE);
                textViewSeekBar.setVisibility( View.GONE);
                switch1.setChecked( activeDevice.getBinaryValue() );
            } else if (activeDevice.getValueType() == Device.ValueType.DECIMAL) {
                // Seekbar is visible when active device has decimal value type, only
                switch1.setVisibility( View.GONE);
                textViewSwitch.setVisibility( View.GONE);
                seekBar.setProgress( (int)activeDevice.getDecimalValue() );
            } else {
                Log.wtf(TAG, "onCreate() Unable to set device visibility: Invalid device value type (" + activeDevice.getValueType() + ")");
            }

            // Setting values on the TextView objects to the device values
            textViewContainerName.setText( centralUnit.getName());
            textViewDeviceName.setText( activeDevice.getName());
            textViewDeviceDesc.setText( activeDevice.getDescription());
        }

        else {
            Log.e(TAG, "onCreate() Active device == null! Unable to set View attributes! Device id: " + newId + ", container id: " + centralUnit.getId());
        }
    }

    public void onDestroy() {
        Log.i(TAG, "onDestroy() Activity has been terminated.");
        super.onDestroy();
    }

}