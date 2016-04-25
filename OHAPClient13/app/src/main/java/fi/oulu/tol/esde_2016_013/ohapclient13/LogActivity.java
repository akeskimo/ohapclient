package fi.oulu.tol.esde_2016_013.ohapclient13;

/**
 * Description:
 * Log activity for displaying list view of logged OHAP TCP messages
 *
 * Change history:
 * v1.0     Aapo Keskimolo      Initial version
 *
 * @author Aapo Keskimolo &lt;aapokesk@gmail.com>
 * @version 1.0
 */

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import fi.oulu.tol.esde_2016_013.ohapclient13.utility.LogContainer;


public class LogActivity extends ActionBarActivity {

    // Log tag
    private final String TAG = this.getClass().getSimpleName();

    // Intent extras
    protected static final int SUB_ACTIVITY_REQUEST_CODE = 100;
    private final static String MESSAGE_ID = "fi.oulu.tol.esde_2016_013.ohapclient13.MESSAGE_ID";

     // singleton central unit container
    private static LogContainer logContainer = null;

    // alert dialog box
    AlertDialog alert = null;
    private boolean alertShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        // Instantiating CentralUnit placeholder for all containers and observers
        logContainer = LogContainer.getInstance();
        Log.i(TAG, "onCreate() Created new LogContainer object");

        // inflate list view
        createListView();
    }


     private void createListView() {
        try {
            // Create ListAdapter and set it to ListView
            ListView listView = (ListView) findViewById(R.id.listView);
            listView.setAdapter(new LogListAdapter(logContainer));

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Intent intent;

                Log.d(TAG, "createListView() Clicked on item " + position + ".");

                intent = new Intent(LogActivity.this, MessageLogActivity.class);
                intent.putExtra(MESSAGE_ID, position);

                startActivityForResult(intent, SUB_ACTIVITY_REQUEST_CODE);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "onCreate() Unable to generate ListView and start activity: " + e.getMessage());
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart() Called");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop() Called");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy() Called");
    }
}
