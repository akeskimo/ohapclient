package fi.oulu.tol.esde_2016_013.ohapclient13;

/**
 * Description:
 * Container Activity for an OHAP client application for monitoring and actuating house devices via
 * remote control Android application. This activity is the main entry point for OHAPClient13.
 *
 * Change history:
 * v1.0     Aapo Keskimolo      Initial version
 * v1.1     Aapo Keskimolo      Display device value on ListView
 *
 * @author Aapo Keskimolo &lt;aapokesk@gmail.com>
 * @version 1.1
 */

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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

import java.net.MalformedURLException;
import java.net.URL;


public class ContainerActivity extends ActionBarActivity {

    // TODO Add recovery scenario to all exceptions where possible (prio HIGH)

    // Log tag
    private final String TAG = this.getClass().getSimpleName();

    // Intent extras
    private final static String CENTRAL_UNIT_URL = "fi.oulu.tol.esde_2016_013.ohapclient13.CENTRAL_UNIT_URL";
    private final static String DEVICE_ID = "fi.oulu.tol.esde_2016_013.ohapclient13.DEVICE_ID";

    // singleton central unit container
    private static CentralUnit centralUnit = null;

    // central unit url
    URL url = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_container);

        try {
        // Instantiating CentralUnit placeholder
            centralUnit = CentralUnitConnection.getInstance(); // get instance of singleton

            // Set container name
            centralUnit.setName(getResources().getString(R.string.centralunit_name) );

            // Log container information
            Log.i(TAG, "onCreate() centralUnit obj: " + centralUnit
                    + " name: " + centralUnit.getName()
                    + " container id: " + centralUnit.getId()
                    + " item count: " + centralUnit.getItemCount()
                    + " listening state: " + centralUnit.isListening());

        } catch (MalformedURLException e) {
            Log.e(TAG, "onCreate() URL is invalid: " + e.getMessage());
        }

        // loads preferences from a file
        loadPreferences();

        // initializes list view
        createListView();

    }

    private void loadPreferences() {
        // Loads Default Shared Preferences

        SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(this);
        String port_key, url_key, port_value, url_value;

        // load port settings
        port_key = getString(R.string.pref_port_key);
        port_value = shared.getString( port_key, "");
        Log.i(TAG, "loadPreferences() Preference loaded, key = " + port_key + ", value = " + port_value);

        // load url settings
        url_key = getString(R.string.pref_url_key);
        url_value = shared.getString( url_key, "");
        Log.i(TAG, "loadPreferences() Preference loaded, key = " + url_key + ", value = " + url_value);

        String address = url_value + ":" + port_value + "/";
        try {
            centralUnit.setURL(new URL(address));
            Log.i(TAG, "loadPreferences() CentralUnit set with new URL: " + address);
        } catch (MalformedURLException e) {
            Log.e(TAG, "loadPreferences() Unable to set CentralUnit URL address \"" + address + ", reason: " + e.getMessage());
        }
    }

    private void createListView() {
        try {
            // Create ListAdapter and set it to ListView
            ListView listView = (ListView) findViewById(R.id.listView);
            listView.setAdapter(new ContainerListAdapter(centralUnit));

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Toast.makeText(ContainerActivity.this, "Device " + (position + 1) + " selected", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(ContainerActivity.this, DeviceActivity.class);
                    intent.putExtra(CENTRAL_UNIT_URL, getResources().getString(R.string.server_url)); // pass url to intent
                    intent.putExtra(DEVICE_ID, position + 1); // pass id to intent
                    startActivity(intent);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "onCreate() Unable to generate ListView and start activity: " + e.getMessage());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        createListView();
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

        int id = item.getItemId();

        if (id == R.id.menu_settings) {
            // start preference activity
            Intent intent = new Intent(ContainerActivity.this, PreferencesActivity.class);
            startActivity(intent);
        }

        if (id == R.id.menu_container) {
            // Current activity, do nothing
            Toast.makeText( this, getString(R.string.menu_click_toast), Toast.LENGTH_SHORT).show();
        }

        if (id == R.id.menu_help) {
            // start helper activity
            Intent intent = new Intent(ContainerActivity.this, HelperActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy() Activity has been terminated.");
        super.onDestroy();
    }
}
