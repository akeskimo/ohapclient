package fi.oulu.tol.esde_2016_013.ohapclient13;

/**
 * Message activity for displaying the contents of OHAP TCP messages
 *
 * Change history:
 * v1.0     Aapo Keskimolo      Initial version
 *
 * @author Aapo Keskimolo &lt;aapokesk@gmail.com>
 * @version 1.0
 */

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.opimobi.ohap.CentralUnitConnection;

import fi.oulu.tol.esde_2016_013.ohapclient13.utility.LogContainer;
import fi.oulu.tol.esde_2016_013.ohapclient13.utility.MessageLog;


public class MessageLogActivity extends ActionBarActivity {
    //////////////////////////////////////////////////////////////////////////////////
    // Device Activity for OHAPClient13 application
    //////////////////////////////////////////////////////////////////////////////////

    // Log tag
    private final String TAG = this.getClass().getSimpleName();

    // Intent extras
//    protected static final int SUB_ACTIVITY_REQUEST_CODE = 100;
    private final static String MESSAGE_ID = "fi.oulu.tol.esde_2016_013.ohapclient13.MESSAGE_ID";

    // Widgets
    private TextView textViewMessage = null;

    // singleton central unit container
    private static CentralUnitConnection centralUnit = null;

    // message log
    LogContainer logContainer = null;
    MessageLog messageLog = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Loading XML layout and saving Bundle of instance state
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        // Getting references for Text View Widgets
        textViewMessage = (TextView)(findViewById(R.id.textViewMessage));

        // get log container instance
        logContainer = LogContainer.getInstance();
        Log.d(TAG, "onCreate() Log container has " + logContainer.getItemCount() + " items.");

        // Get Message id from the parent intent (LogActivity)
        // Note: Item index is the index corresponding to the row user has clicked on the list view
        // of LogActivity
        int newId = getIntent().getIntExtra(MESSAGE_ID, -1); // if id not found, -1 is returned
        Log.i(TAG, "onCreate() New MESSAGE_ID received from intent: " + newId);
        if (newId == -1) {
            Log.e(TAG, "onCreate() No message id found. Forcing = 0 (top message displayed only)");
        }
        final int message_id = newId != -1 ? newId: 1;

        // get selected device from message log container
        messageLog = logContainer.getItemByIndex(message_id);
        Log.d(TAG, "onCreate() Message " + message_id + "\n" + messageLog.getMessage());

        if (messageLog != null) {
            textViewMessage.setText(messageLog.getMessage());
        } else {
            Log.e(TAG, "onCreate() Returned message log object = null");
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