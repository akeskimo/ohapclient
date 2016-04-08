package fi.oulu.tol.esde_2016_013.ohapclient13;

/**
 * Description:
 * User preference activity the user of the UI can use to to configure settings
 *
 * Change history:
 * v1.0     Aapo Keskimolo      Initial version with layout, connect and save buttons, edit boxes for URL and port
 *
 * @author Aapo Keskimolo &lt;aapokesk@gmail.com>
 * @version 1.0
 */

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.opimobi.ohap.CentralUnit;
import com.opimobi.ohap.CentralUnitConnection;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

public class PreferenceActivity extends ActionBarActivity {

    // TODO implement drop down menu for selecting the server: the server list will be read from config file (prio med)
    // TODO make the URL and Port edit boxes less ugly (prio low)

    private final String TAG = this.getClass().getSimpleName();

//    private final static String DEFAULT_URL = "fi.oulu.tol.esde_2016_013.ohapclient13.DEFAULT_URL";
//    private final static String DEFAULT_AUTO_CONNECT = "fi.oulu.tol.esde_2016_013.ohapclient13.DEFAULT_AUTO_CONNECT";

    private EditText editTextUrl = null;
    private EditText editTextPort = null;
    private Button connectPushButton = null;
    private Button savePushButton = null;
    private CheckBox autoConnectCheckBox = null;
    private TextView urlTextView;

    private int newPort;
    private boolean portIsValid = false;
    private String newUrl;
    private boolean URLIsValid = false;

    // singleton central unit container
    private static CentralUnit centralUnit = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preference);


        try {
            // Instantiating CentralUnit placeholder for all containers
            centralUnit = CentralUnitConnection.getInstance(); // get instance of singleton

            // Log container information
            Log.i(TAG, "onCreate() centralUnit obj: " + centralUnit
                    + " name: " + centralUnit.getName()
                    + " container id: " + centralUnit.getId()
                    + " item count: " + centralUnit.getItemCount()
                    + " listening state: " + centralUnit.isListening());
        } catch (MalformedURLException e) {
            Log.e(TAG, "onCreate() URL is invalid: " + e.getMessage() );
        } catch (Exception e) {
            Log.e(TAG, "onCreate() Unknown error occurred: " + e.getMessage());
            e.getStackTrace();
        }

        editTextUrl = (EditText) findViewById(R.id.editTextUrl);
        editTextPort = (EditText) findViewById(R.id.editTextPort);
        savePushButton = (Button) findViewById(R.id.saveButton);
        connectPushButton = (Button) findViewById(R.id.connectButton);
        autoConnectCheckBox = (CheckBox)findViewById(R.id.checkBoxAutoConnect);
        urlTextView = (TextView)findViewById(R.id.textViewServerAddress);

        urlTextView.setText(centralUnit.getURL().toString());
        urlTextView.setTextColor(Color.BLACK);

        Log.d(TAG, "onCreate() autoConnectCheckBox.isChecked() = " + autoConnectCheckBox.isChecked() );


        autoConnectCheckBox.setOnClickListener(new View.OnClickListener() {
            // auto-connect checkbox listener
            @Override
            public void onClick(View v) {
                // callback
                CheckBox b = (CheckBox) v;
                if (!autoConnectCheckBox.isChecked()) {
                    autoConnectCheckBox.setChecked(false);
                } else {
                    autoConnectCheckBox.setChecked(true);
                    //            sendConnectRequest();
                }
                Log.d(TAG, "onClickRadioButtonCallback() autoConnectCheckBox.isChecked() = " + autoConnectCheckBox.isChecked());
                popBox("Action not implemented.");

            }
        });


        savePushButton.setOnClickListener(new View.OnClickListener() {
            // save button listener
            @Override
            public void onClick(View v) {
                // callback
                if (portIsValid & URLIsValid) {
                    savePreferences();
                    popBox("Saved");
                }
                else {
                    popBox("Invalid URL or Port!");
                }
            }
        });

        connectPushButton.setOnClickListener(new View.OnClickListener() {
            // connect button listener
            @Override
            public void onClick(View v) {
                // callback
                sendConnectRequest();
            }
        });


        editTextPort.addTextChangedListener(new TextWatcher() {
            // port text changed listener

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                // callback
                String editedText = s.toString();
                try {
                    // validate port and paint the text with red when its invalid
                    double value = Double.parseDouble(editedText);
                    if (value >= 0 && value < 65535) {
                        editTextPort.setTextColor(Color.BLACK);
                        newPort = (int) value; // save port to variable
                        portIsValid = true;
                    } else {
                        editTextPort.setTextColor(Color.RED);
                        portIsValid = false;
//                        Log.d(TAG,"Port size is out of bounds [0," + 65535 + "]");
                    }
                } catch (NumberFormatException e) {
                    editTextPort.setTextColor(Color.RED);
                    portIsValid = false;
//                    Log.d(TAG, "addTextChangedListener()::afterTextChanged() Unable to parse string");
                }
            }
        });


        editTextUrl.addTextChangedListener(new TextWatcher() {
            // url text changed listener
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                // callback
                String editedText = s.toString();
                try {
                    // validate url address and paint the text with red if address is invalid
                    editTextUrl.setTextColor(Color.BLACK);
//                    if ( !editedText.matches("^(http)://.*$") ) {
//                        editedText = "http://" + editedText;
//                    } else if ( !editedText.matches("^(https)://.*$") {
//                        editedText = "https://" + editedText;
//                    }
                    editedText = "http://" + editedText;
//                    Log.d(TAG, "addTextChangedListener()::afterTextChanged() New edited text: " + editedText);
                    URL u = new URL(editedText);
                    u.toURI(); // extra URL validation
                    newUrl = editedText; // save url to variable
                    URLIsValid = true;
                } catch (URISyntaxException e) {
                    editTextUrl.setTextColor(Color.RED);
                    URLIsValid = false;
                } catch (MalformedURLException e) {
                    editTextUrl.setTextColor(Color.RED);
                    URLIsValid = false;
                }
            }
        });
    }

    private void popBox(String message) {
        Log.d(TAG, "popBox() Message \"" + message + "\" sent to Toast.");
        Toast.makeText(PreferenceActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    private void savePreferences() {
        if (URLIsValid & portIsValid) {
            String address = this.newUrl + ":" + this.newPort + "/";
            try {
                URL u = new URL(address);
                centralUnit.setURL(u);
                urlTextView.setText(centralUnit.getURL().toString());
                saveToFile(u.toString());
            } catch (MalformedURLException e) {
                Log.e(TAG, "savePreferences() Unable to set CentralUnit URL address \"" + address + ", reason: " + e.getMessage());
                popBox("Unable to save:" + e.getMessage() );
            }
        }
        else {
            popBox("Invalid URL or Port");
        }
    }


    private boolean sendConnectRequest() {
        Log.d(TAG, "sendConnectRequest() Not implemented.");
        popBox("Action not implemented.");
        return true;
    }


    private void saveToFile(String string) {
        // writes string to internal storage file

        String filename = "config";
        FileOutputStream outputStream;

        try {
            outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(string.getBytes());
            outputStream.close();
            Log.i(TAG, "saveToFile() Successfully saved to file: " + string);

        } catch (Exception e) {
            Log.e(TAG, "saveToFile() Unexpected write error: " + e.getMessage());
        }
    }


    public String readFromFile() {
        // reads string from internal storage file

        String filename = "config";
        String string = "";

        try {
            FileInputStream buf = openFileInput(filename);
            int content;
            while ((content = buf.read()) != -1) {
                string += (char)content;
            }
            buf.close();
            Log.i(TAG, "readFromFile() Successfully read from file: " + string );

        } catch (FileNotFoundException e) {
            Log.e(TAG, "readFromFile() File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "readFromFile() IO error: " + e.getMessage());
        }
        return string;
    }



    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy() Activity has been terminated.");
        super.onDestroy();
    }
}
