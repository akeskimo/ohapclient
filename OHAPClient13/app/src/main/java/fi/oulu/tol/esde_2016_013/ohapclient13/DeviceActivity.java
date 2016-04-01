package fi.oulu.tol.esde_2016_013.ohapclient13;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.opimobi.ohap.*;

import java.net.MalformedURLException;
import java.net.URL;
import android.widget.Switch;
import android.widget.SeekBar;
import android.widget.TextView;


public class DeviceActivity extends ActionBarActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        CentralUnit centralUnit = null;
        try {
            centralUnit = new CentralUnitConnection(new URL("http://ohap.opimobi.com:8080/"));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        centralUnit.setName("OHAP Test Server");

        Device device = new Device(centralUnit, 1, Device.Type.ACTUATOR, Device.ValueType.BINARY);
        device.setName("Dumbo Jumbo");

    }


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

//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }
}
