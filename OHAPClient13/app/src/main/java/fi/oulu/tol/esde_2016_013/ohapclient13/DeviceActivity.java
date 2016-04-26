package fi.oulu.tol.esde_2016_013.ohapclient13;

/**
 * Description:
 * Device activity that displays device information and is able to actuate them
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
import android.view.View;
import android.widget.CompoundButton;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import android.widget.SeekBar;
import android.widget.TextView;

import java.net.MalformedURLException;
import java.util.ArrayList;


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
    private TextView textViewSeekBarValue = null;
    private TextView textViewSeekBarValueMin = null;
    private TextView textViewSeekBarValueMax = null;
    private Switch switch1 = null;
    private TextView textViewSwitchValue = null;

    // top-level central unit container
    private static CentralUnitConnection centralUnit = null;

    // intent container
    Container container;

    // active device displayed on the UI
    private Device device = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Loading XML layout and saving Bundle of instance state
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        // Getting references for Text View Widgets
        textViewContainerName = (TextView)(findViewById(R.id.textViewContainerName));
        textViewDeviceName = (TextView)(findViewById(R.id.textViewDeviceName));
        textViewDeviceDesc = (TextView)(findViewById(R.id.textViewDeviceDesc));

        // seekbar widget
        seekBar = (SeekBar)(findViewById(R.id.seekBar));
        textViewSeekBarValue = (TextView)(findViewById(R.id.textViewSeekBar)); // displays dec value
        textViewSeekBarValueMin = (TextView)(findViewById(R.id.textViewDecimalMin)); // displays dec value
        textViewSeekBarValueMax = (TextView)(findViewById(R.id.textViewDecimalMax)); // displays dec value

        // switch widget
        switch1 = (Switch)(findViewById(R.id.switch1));
        textViewSwitchValue = (TextView)(findViewById(R.id.textViewSwitch)); // displays bin value


        // Get URL from the parent intent (ContainerActivity)
        String newUrl = getIntent().getStringExtra(CENTRAL_UNIT_URL);
        final String central_unit_url = newUrl != null ? newUrl : centralUnit.getURL().toString();
        Log.i(TAG, "onCreate() New CENTRAL_UNIT_URL received from intent: " + central_unit_url);

        try {
            // Instantiating CentralUnit placeholder for all containers and observers
            centralUnit = CentralUnitConnection.getInstance();

            Log.i(TAG, "onCreate() CentralUnit: " + centralUnit + " name: " + centralUnit.getName() + " container id: " + centralUnit.getId() + " item count: " + centralUnit.getItemCount() + " listening state: " + centralUnit.isListening());

        } catch (MalformedURLException e) {
            Log.e(TAG, "onCreate() Unable to instantiate CentralUnit: " + e.getMessage());

        } catch (Exception e) {
            Log.e(TAG, "onCreate() Unhandled error occurred: " + e.getMessage());
        }

        // Get Device id from the parent intent (ContainerActivity)
        // Note: Item index is the index corresponding to the row user has clicked on the list view
        // of ContainerActivity
        long newId = getIntent().getLongExtra(DEVICE_ID, -1); // if id not found, -1 is returned
        Log.i(TAG, "onCreate() New DEVICE_ID received from intent: " + newId);
        if (newId == -1) {
            Log.e(TAG, "onCreate() No device id found. Forcing ID = 1");
        }
        final long device_id = newId != -1 ? newId: 1;


        if (centralUnit != null) {
            // get selected device from central unit
            device = (Device) centralUnit.getItemById(device_id);
            Log.d(TAG, "onCreate() Device " + device_id + ":" + device);
        }
        else {
            Log.e(TAG, "onCreate() Central device == null!");
        }



        if (device != null) {
            // sets device information on UI

            Log.i(TAG, "onCreate() Device: " + device + " name: " + device.getName() + " device id: " + device.getId() + " type: " + device.getType() + " valueType: " + device.getValueType());

            // container hierarchy
            textViewContainerName.setText( getContainerHierarchy(device) );
            // display device name and description
            textViewDeviceName.setText( device.getName());
            textViewDeviceDesc.setText( device.getDescription());


            // Update header text on the action bar
            if (device.getType() == Device.Type.ACTUATOR) {
                setTitle("ACTUATOR");
            } else if (device.getType() == Device.Type.SENSOR) {
                setTitle("SENSOR");
            } else {
                setTitle("-");
                Log.wtf(TAG, "onCreate() Unable to set App title: Invalid device type (" + device.getType() + ")");
            }


            if (device.getValueType() == Device.ValueType.BINARY) {
                // switch is visible when active device has binary value type, only

                String value = "Value: " + device.getBinaryValue();

                // value text
                textViewSwitchValue.setVisibility(View.VISIBLE);
                textViewSwitchValue.setText(value);

                if (device.getType() == Device.Type.ACTUATOR) {
                    // switch bar
                    switch1.setVisibility(View.VISIBLE);
                    switch1.setChecked(device.getBinaryValue());
                }

            } else if (device.getValueType() == Device.ValueType.DECIMAL) {
                // seekbar is visible when active device has decimal value type, only

                String value = "Value: " + device.getDecimalValue() + " " + device.getUnit();

                // value text
                textViewSeekBarValue.setVisibility(View.VISIBLE);
                textViewSeekBarValue.setText(value);

                textViewSeekBarValueMin.setVisibility(View.VISIBLE);
                String minValue = "Min: " + device.getMinValue() + " " + device.getUnit();
                textViewSeekBarValueMin.setText(minValue);

                textViewSeekBarValueMax.setVisibility(View.VISIBLE);
                String maxValue = "Max: " + device.getMaxValue() + " " + device.getUnit();
                textViewSeekBarValueMax.setText(maxValue);

                if (device.getType() == Device.Type.ACTUATOR) {
                    // seek bar
                    seekBar.setVisibility(View.VISIBLE);
                    seekBar.setProgress((int) device.getDecimalValue());
                }

            } else {
                Log.wtf(TAG, "onCreate() Unable to set device visibility: Invalid device value type (" + device.getValueType() + ")");
            }
        }

        else {
            Log.e(TAG, "onCreate() Active device == null! Unable to set View attributes!" + " Device id: " + newId + ", container id: " + centralUnit.getId());
        }


        // Implementing listener for seekbar for changing decimal value of the Device
        seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                try {
                    centralUnit.sendDecimalValueChanged(device, progress);
                    textViewSeekBarValue.setText(String.format("%1.0f %%", progress)); // display device value
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
                    centralUnit.sendBinaryValueChanged(device, isChecked);
                    textViewSwitchValue.setText(Boolean.toString(isChecked)); // display device value
                } catch (Exception e) {
                    Log.e(TAG, "onProgressChanged() Unable to device value: " + e.getMessage() );
                }
            }

        });


    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    public void onDestroy() {
        super.onDestroy();
    }

    public String getContainerHierarchy(Device device) {
        // get device parent container hierarchy

        ArrayList <String> listNames = new ArrayList();
        Container container = device.getParent();

        // add container names to list (top hierarchy is the last element)
        listNames.add(container.getName());
        while ( container.getId() != 0) {
            container = container.getParent();
            listNames.add( container.getName() );
        };

        // reverse loop through container names (from top to bottom; right to left)
        String text = listNames.get(listNames.size()-1);
        for (int i = listNames.size()-2; i >=0; i--) {
            Log.d(TAG, "onCreate() listNames[ " + i + "] = " + listNames.get(i) );
            text += "->" + listNames.get(i);
        }
        return text;
    }
}