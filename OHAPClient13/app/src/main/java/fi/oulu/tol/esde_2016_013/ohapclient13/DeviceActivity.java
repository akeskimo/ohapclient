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

    // active device displayed on the UI
    private Device device = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // load XML layout and saving Bundle of instance state
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        // getting references for Text View Widgets
        textViewContainerName = (TextView)(findViewById(R.id.textViewContainerName));
        textViewDeviceName = (TextView)(findViewById(R.id.textViewDeviceName));
        textViewDeviceDesc = (TextView)(findViewById(R.id.textViewDeviceDesc));

        // seekbar widget
        seekBar = (SeekBar)(findViewById(R.id.seekBar));
        seekBar.setMax(Integer.parseInt(getString(R.string.seekbar_maxvalue)));
        textViewSeekBarValue = (TextView)(findViewById(R.id.textViewSeekBar)); // displays dec value
        textViewSeekBarValueMin = (TextView)(findViewById(R.id.textViewDecimalMin)); // displays dec value
        textViewSeekBarValueMax = (TextView)(findViewById(R.id.textViewDecimalMax)); // displays dec value

        // switch widget
        switch1 = (Switch)(findViewById(R.id.switch1));
        textViewSwitchValue = (TextView)(findViewById(R.id.textViewSwitch)); // displays bin value

        // get central unit
        centralUnit = CentralUnitConnection.getInstance();
        Log.i(TAG, "onCreate() CentralUnit: " + centralUnit + "\nurl: " + centralUnit.getURL() + "\nname: " + centralUnit.getName() + "\ncontainer id: " + centralUnit.getId() + "\nitem count: " + centralUnit.getItemCount() + "\nlistening state: " + centralUnit.isListening());

        // get device id from the parent intent
        long newId = getIntent().getLongExtra(DEVICE_ID, -1); // if id not found, -1 is returned
        Log.i(TAG, "onCreate() New DEVICE_ID received from intent: " + newId);
        if (newId == -1) {
            Log.e(TAG, "onCreate() No device id found. Forcing ID = 1");
        }
        final long device_id = newId != -1 ? newId: 1;

        // get selected device from central unit
        if (centralUnit != null) {
            device = (Device) centralUnit.getItemById(device_id);
            Log.d(TAG, "onCreate() Device " + device_id + ":" + device);
        }
        else {
            Log.e(TAG, "onCreate() Central device == null!");
        }



        // set device information on the activity
        if (device != null) {

            Log.i(TAG, "onCreate() Device obj: " + device + " name: " + device.getName() +
                    " device id: " + device.getId() + " type: " + device.getType() + " valueType: "
                    + device.getValueType() + " value: " + device.getBinaryValue());

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

                String value = "Value: " + String.format("%1.2f", device.getDecimalValue()) + " " + device.getUnit();

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
                    double scaledValue =
                            scale(device.getDecimalValue(), device.getMinValue(), device.getMaxValue(), 0, seekBar.getMax());
                    seekBar.setProgress((int) scaledValue);
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
//                double scaledProgress =
//                    scale(progress, 0, seekBar.getMax(), device.getMinValue(), device.getMaxValue() );
//                try {
//                    centralUnit.setDecimalValueChanged(device, scaledProgress);
//                    textViewSeekBarValue.setText("Value: " + String.format("%1.2f", scaledProgress) + " " + device.getUnit()); // display device value
//                } catch (Exception e) {
//                    Log.e(TAG, "onProgressChanged() Unable to set device value: " + e.getMessage() );
//                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                double scaledProgress =
                        scale(seekBar.getProgress(), 0, seekBar.getMax(), device.getMinValue(), device.getMaxValue() );
                try {
                    centralUnit.setDecimalValueChanged(device, scaledProgress);
                    textViewSeekBarValue.setText("Value: " + String.format("%1.2f", scaledProgress) + " " + device.getUnit()); // display device value
                } catch (Exception e) {
                    Log.e(TAG, "onProgressChanged() Unable to set device value: " + e.getMessage() );
                }
            }
        });


        // Implementing listener for switch for changing binary value of the Device
        switch1.setOnCheckedChangeListener( new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                    Log.d(TAG, "onCheckedChanged = " + isChecked);
                    centralUnit.setBinaryValueChanged(device, isChecked);
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


    public double scale(double value, double min, double max, double newMin, double newMax) {
        return (value) / (max - min) * (newMax - newMin) + newMin;
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