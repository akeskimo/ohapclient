package fi.oulu.tol.esde_2016_013.ohapclient13;

/**
 * Description:
 * Container Activity for an OHAP client application for monitoring and actuating house devices via
 * remote control Android application. This class acts as main entry point to OHAPClient13.
 *
 * Change history:
 * v1.0     Aapo Keskimolo      Initial version
 *
 * @author Aapo Keskimolo &lt;aapokesk@gmail.com>
 * @version 1.0
 */

import android.content.Intent;
import android.os.Bundle;
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
import com.opimobi.ohap.Container;

import java.net.MalformedURLException;
import java.net.URL;


public class ContainerActivity extends ActionBarActivity {

    // TODO Add exception handling to all methods that access objects (prio HIGH)
    // TODO check the stability of the application by running the emulator (prio HIGH)
    // TODO action bar in container activity should have 3 icons: Container|Device|Settings (prio low)


    // Log tag
    private final String TAG = this.getClass().getSimpleName();

    // Intent extras
    private final static String CENTRAL_UNIT_URL = "fi.oulu.tol.esde_2016_013.ohapclient13.CENTRAL_UNIT_URL";
    private final static String DEVICE_ID = "fi.oulu.tol.esde_2016_013.ohapclient13.DEVICE_ID";
//    private final static String DEFAULT_URL = "fi.oulu.tol.esde_2016_013.ohapclient13.DEFAULT_URL";
//    private final static String DEFAULT_AUTO_CONNECT = "fi.oulu.tol.esde_2016_013.ohapclient13.DEFAULT_AUTO_CONNECT";

    // singleton central unit container
    private static CentralUnit centralUnit = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_container);

        // loads preferences from a file
        loadPreferences();

        try {
            // Instantiating CentralUnit placeholder for all containers
            centralUnit = CentralUnitConnection.getInstance(); // get instance of singleton
            centralUnit.setName(getResources().getString(R.string.centralunit_name) );

            // Log container information
            Log.i(TAG, "onCreate() centralUnit obj: " + centralUnit
                    + " name: " + centralUnit.getName()
                    + " container id: " + centralUnit.getId()
                    + " item count: " + centralUnit.getItemCount()
                    + " listening state: " + centralUnit.isListening());

            ListView listView = (ListView) findViewById(R.id.listView);
            listView.setAdapter(new ContainerListAdapter(centralUnit));

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Toast.makeText(ContainerActivity.this, "Device " + (position+1) + " selected", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(ContainerActivity.this, DeviceActivity.class);
                    intent.putExtra(CENTRAL_UNIT_URL, getResources().getString(R.string.server_url) ); // pass url to intent
                    intent.putExtra(DEVICE_ID, position+1); // pass id to intent
                    startActivity(intent);
                }
            });
        } catch (MalformedURLException e) {
            Log.e(TAG, "onCreate() URL is invalid: " + e.getMessage() );
        } catch (Exception e) {
            Log.e(TAG, "onCreate() Unknown error occurred: " + e.getMessage());
            e.getStackTrace();
        }
    }

    private boolean loadPreferences() {
        Log.d(TAG, "loadPreferences() Method not implemented");
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_container, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.container_settings) {
            Intent intent = new Intent(ContainerActivity.this, PreferenceActivity.class);
            startActivity(intent);
//            intent.getStringExtra(DEFAULT_URL); // pass url to intent
//            intent.getBooleanExtra(DEFAULT_AUTO_CONNECT, true); // pass url to intent
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
