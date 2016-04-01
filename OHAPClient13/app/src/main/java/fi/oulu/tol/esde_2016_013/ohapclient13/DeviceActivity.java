package fi.oulu.tol.esde_2016_013.ohapclient13;

/**
 * Device activity for an OHAP application.
 *
 * Change history:
 * v1.0     Aapo Keskimolo      Initial version with layout, OHAP server, 1 dummy device, widgets and listeners
 *
 * @author Aapo Keskimolo &lt;aapokesk@gmail.com>
 * @version 1.0
 */

import com.opimobi.ohap.*;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.SeekBar;
import android.widget.TextView;

import java.net.MalformedURLException;
import java.net.URL;


public class DeviceActivity extends ActionBarActivity implements CompoundButton.OnCheckedChangeListener {

    // Widgets
    private TextView textViewContainerName = null;
    private TextView textViewDeviceName = null;
    private TextView textViewDeviceDesc = null;
    private SeekBar seekBar = null;
    private Switch switch1 = null;
    private TextView textViewSwitchValue = null;

    // OHAP applications
    CentralUnit centralUnit = null;
    Device dummyDevice = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Loading XML layout
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        // Instantiating widgets
        // note: findViewById returns View object, which is a super class of Widget class. Therefore,
        // all view objects need to be down-cast to target Widgets objects.
        textViewContainerName = (TextView)(findViewById(R.id.textViewContainerName));
        textViewDeviceName = (TextView)(findViewById(R.id.textViewDeviceName));
        textViewDeviceDesc = (TextView)(findViewById(R.id.textViewDeviceDesc));

        // Instantiating seekbar (for changing decimal value)
        seekBar = (SeekBar)(findViewById(R.id.seekBar));
        // TODO Implement listener for Seekbar and method for changing the decimal value on the text widget above. You may use the Switch -method as an example. Hint: Google "Seekbar listener"
        // Registering new listener for the switch bar action
        // ...
        // Instatiating widget for displaying the current decimal value
        // ...

        // Instantiating switch object for changing binary value)
        switch1 = (Switch)(findViewById(R.id.switch1));
        // Registering new listener for the switch bar action
        switch1.setOnCheckedChangeListener(this);
        // Instatiating widget for displaying the current binary value
        textViewSwitchValue = (TextView)(findViewById(R.id.textViewSwitch));

        // Instantiating OHAP server
//        CentralUnit centralUnit = null;
        try {
            // Java enforces try-catch statement for MalformedURLException
            centralUnit = new CentralUnitConnection(new URL("http://ohap.opimobi.com:8080/"));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        centralUnit.setName("OHAP Test Server");

        // Instantiating a dummy
        // device with dummy values
        dummyDevice = new Device(centralUnit, 1, Device.Type.ACTUATOR, Device.ValueType.BINARY);
        dummyDevice.setName("Dummy Device");
        dummyDevice.setDescription("Description of the Dummy Device");
        dummyDevice.setBinaryValue(false);

        // set values on the TextView objects
        textViewDeviceName.setText(dummyDevice.getName());
        textViewDeviceDesc.setText(dummyDevice.getDescription());
        // TODO The top TextView should show the container hierarchy (not documented -> ask teacher)
        textViewContainerName.setText("Container hierarchy (TBD)");
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        // Implementation for the method that is called whenever the binary Switch value has changed
        dummyDevice.setBinaryValue(isChecked);
        textViewSwitchValue.setText(Boolean.toString(dummyDevice.getBinaryValue())); // display device value
    }

    // TODO Add SeekBar call method(s) here

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
