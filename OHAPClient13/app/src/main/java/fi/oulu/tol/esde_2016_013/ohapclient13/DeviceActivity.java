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
 *
 * @author Aapo Keskimolo &lt;aapokesk@gmail.com>
 * @version 1.1
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
import java.net.URL;


public class DeviceActivity extends ActionBarActivity implements CompoundButton.OnCheckedChangeListener, SeekBar.OnSeekBarChangeListener {
    //////////////////////////////////////////////////////////////////////////////////
    // Main Activity for OHAPClient13.java
    //////////////////////////////////////////////////////////////////////////////////

    // Log tag
    private static final String TAG = "DeviceActivity";

    // Widgets
    private TextView textViewContainerName = null;
    private TextView textViewDeviceName = null;
    private TextView textViewDeviceDesc = null;
    private SeekBar seekBar = null;
    private Switch switch1 = null;
    private TextView textViewSwitchValue = null;
    private TextView textViewSeekBarValue = null;

    // OHAP applications
    CentralUnit centralUnit = null;
    Device dummyDevice = null;

    // Global Variables
    private Device.Type activeDeviceType = null;
    private Device.ValueType activeDeviceValueType = null;
    private String activeDeviceName = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Loading XML layout and saving Bundle of instance state
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        // Getting references for Text View Widgets
        textViewContainerName = (TextView)(findViewById(R.id.textViewContainerName));
        textViewDeviceName = (TextView)(findViewById(R.id.textViewDeviceName));
        textViewDeviceDesc = (TextView)(findViewById(R.id.textViewDeviceDesc));
        textViewSeekBarValue = (TextView)(findViewById(R.id.textViewSeekBar)); // displays dec value
        textViewSwitchValue = (TextView)(findViewById(R.id.textViewSwitch)); // displays bin value

        // Getting reference for seekbar (for changing decimal value)
        seekBar = (SeekBar)(findViewById(R.id.seekBar));
        seekBar.setOnSeekBarChangeListener(this); // start listening for seekbar user input

            // Getting reference for switch object (for changing binary value)
        switch1 = (Switch)(findViewById(R.id.switch1));
        switch1.setOnCheckedChangeListener(this); // start listening for switch bar user input


        //////////////////////////////////////////////////////////////////////////////////
        // Startup OHAP Server Application and instantiate devices
        //////////////////////////////////////////////////////////////////////////////////

        // Instantiating OHAP server
        try {
            // Java enforces try-catch statement for MalformedURLException
            centralUnit = new CentralUnitConnection(new URL("http://ohap.opimobi.com:8080/"));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        centralUnit.setName(getResources().getString(R.string.container_name));

        // Initializing attributes for the dummy device
        activeDeviceName = getResources().getString(R.string.device_name);
        activeDeviceType = Device.Type.ACTUATOR;
        activeDeviceValueType = Device.ValueType.DECIMAL;

        // Instantiating dummy device
        dummyDevice = new Device(centralUnit, 1, activeDeviceType, activeDeviceValueType);

        // Setting attributes for dummy device
        dummyDevice.setName(activeDeviceName);
        dummyDevice.setDescription(getResources().getString(R.string.device_desc));

        // Change App title to show active device type
        if (activeDeviceType == Device.Type.ACTUATOR) {
            setTitle("Actuator");
        }
        else if (activeDeviceType == Device.Type.SENSOR) {
            setTitle("Sensor");
        } else {
            Log.wtf(TAG, "Unable to set App title: Invalid device type (" + activeDeviceType + ")");
        }

        // Change the visibility of bars according to active device value type: Switch is visible
        // with binary and seekbar with decimal value type
        if (activeDeviceValueType == Device.ValueType.BINARY) {
            seekBar.setVisibility(View.GONE);
            textViewSeekBarValue.setVisibility(View.GONE);
        } else if (activeDeviceValueType == Device.ValueType.DECIMAL ) {
            switch1.setVisibility(View.GONE);
            textViewSwitchValue.setVisibility(View.GONE);
        } else {
            Log.wtf(TAG, "Unable to set device visibility: Invalid device value type (" + activeDeviceValueType + ")");
        }

        // set values on the TextView objects to the device values
        // TODO The top TextView should show the container hierarchy (not documented -> ask teacher)
        textViewContainerName.setText(centralUnit.getName());
        textViewDeviceName.setText(dummyDevice.getName());
        textViewDeviceDesc.setText(dummyDevice.getDescription());
    }


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        // Implementation for the method that is called whenever the binary Switch value has changed
        dummyDevice.setBinaryValue(isChecked);
        textViewSwitchValue.setText(Boolean.toString(dummyDevice.getBinaryValue())); // display device value
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        // Implementation for the method that is called whenever the decimal seekbar value has changed
        dummyDevice.setDecimalValue(progress);
        textViewSeekBarValue.setText(Double.toString(dummyDevice.getDecimalValue())); // display device value
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_device, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}



/**
 * Future ideas
  * - Create auto-testers for widgets
 */
