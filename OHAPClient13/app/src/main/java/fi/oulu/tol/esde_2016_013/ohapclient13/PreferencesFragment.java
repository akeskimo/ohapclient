package fi.oulu.tol.esde_2016_013.ohapclient13;

/**
 * Description:
 * Preference fragment that includes changing connection user preferences. The url
 * and port are validated before saving.
 *
 * Change history:
 * v1.0     Aapo Keskimolo      Initial version
 *
 * @author Aapo Keskimolo &lt;aapokesk@gmail.com>
 * @version 1.0
 */

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Intent;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.os.Bundle;
import android.util.Log;

import com.opimobi.ohap.CentralUnitConnection;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;


public class PreferencesFragment extends PreferenceFragment {
    // Preference fragment for connection settings
    // If connection settings are changed, the main activity will initiate reconnect

    // Log tag
    private final String TAG = getClass().getSimpleName();

    private CentralUnitConnection centralUnit = null;
    private String url_address;
    private String port_number;
    private AlertDialog alert = null;
    private boolean alertShown = false;

    private boolean connectionPreferenceChanged = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        // get central unit
        centralUnit = CentralUnitConnection.getInstance();

        // get url from shared preferences and set it as summary
        Preference urlPref = findPreference(getString(R.string.pref_url_key));
        Preference portPref = findPreference(getString(R.string.pref_port_key));
        Preference autoConnectPref = findPreference(getString(R.string.pref_autoconnect_key));
        Preference logPref = findPreference(getString(R.string.pref_log_key));
//        Preference shakePref = findPreference(getString(R.string.pref_reconnect_on_shake_key));

        // get default url and port
        url_address = "http://" + centralUnit.getURL().getHost();
        port_number = Integer.toString(centralUnit.getURL().getPort());
        try {
            new URL(url_address + ":" + port_number);
        } catch (MalformedURLException mue) {
            url_address = "http://192.168.0.100";
            port_number = "18001";
            Log.e(TAG, "onCreate() Malformed URL: " + mue.getMessage() + ", setting default values: \nurl_address = " + url_address + "\nport_number = " + port_number);
        }
        // set as preference summary text
        urlPref.setSummary( url_address);
        portPref.setSummary(port_number);

        urlPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            // URL preference change listener
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                String key = preference.getKey();
                String value = (String) newValue;
                String msg = "", ttl = "";

                if (key.equals(getString(R.string.pref_url_key))) {
                    // url input

                    Log.d(TAG, "onSharedPreferencesChanged(): key = " + key + ", value = " + newValue);

                    if (validateUrl(value)) {
                        preference.setSummary(value);
                        try {
                            centralUnit.setURL(new URL(value + ":" + port_number));
                            url_address = value;
                            Log.i(TAG, "setOnPreferenceChangeListener() CentralUnit URL updated: " + centralUnit.getURL().toString());
                            connectionPreferenceChanged = true;
                            return true;
                        } catch (MalformedURLException e) {
                            ttl = "Invalid URL";
                            msg = e.getMessage() + "\n\nValid URL example:\nfoo.bar.com";
                            Log.e(TAG, "setOnPreferenceChangeListener() Malformed URL: " + e.getMessage());
                        }
                    } else {
                        ttl = "Invalid URL";
                        msg = "Malformed URL \"" + value + "\"\n\nValid URL example:\nhttp://foo.bar.com";
                    }
                }
                alertDialogMessageOk(ttl, msg, "OK");
                return false;
            }
        });

        // set port preference change listener
        portPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            // Port preference change listener
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                String key = preference.getKey();
                String value = (String) newValue;
                String msg = "", ttl = "";

                if (key.equals(getString(R.string.pref_port_key))) {
                    // port input

                    Log.d(TAG, "onSharedPreferencesChanged(): key = " + key + ", value = " + value);

                    if (validatePort(value)) {
                        preference.setSummary(value);
                        try {
                            centralUnit.setURL(new URL(url_address + ":" + value));
                            port_number = value;
                            Log.i(TAG, "setOnPreferenceChangeListener() CentralUnit URL updated: "
                                    + centralUnit.getURL().toString() );
                            connectionPreferenceChanged = true;
                            return true;
                        } catch (MalformedURLException e) {
                            ttl = "Malformed URL";
                            msg = e.getMessage();
                            Log.e(TAG, "setOnPreferenceChangeListener() Malformed URL: " + e.getMessage());
                            Log.e(TAG, "setOnPreferenceChangeListener() URL: " + centralUnit.getURL().toString());
                        }
                    } else {
                        ttl = "Invalid port";
                        msg = "Port must be a number between 0-65535";
                    }
                }
                alertDialogMessageOk(ttl, msg, "OK");
                return false;
            }
        });


        autoConnectPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            // Auto-connect preference change listener
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String key = preference.getKey();
                boolean value = (boolean)newValue;

                if ( key.equals(getString(R.string.pref_autoconnect_key)) ) {
                    // auto-connect key value
                    Log.d(TAG, "onSharedPreferencesChanged(): key = " + key + ", value = " + value);
                    centralUnit.setAutoConnect( value );
                    connectionPreferenceChanged = true;
                    return true;
                }
                return false;
            }
        });

        logPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            // Message log preference on click listener
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(getActivity(), LogActivity.class);
                startActivity(intent);
                return true;
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        centralUnit.setReconnectRequest(connectionPreferenceChanged);
    }

    private boolean validateUrl( String url) {
        // Checks whether the URL address is of correct format
        boolean isValid = false;

        try {
            URL u = new URL(url);
            u.toURI(); // extra URL validation
            isValid = true;
        } catch (URISyntaxException e) {
            Log.e(TAG, "validateUrl() URI validation failed: " + e.getMessage() );
        } catch (MalformedURLException e) {
            Log.e(TAG, "validateUrl() URL validation failed: " + e.getMessage() );
        }
        return isValid;
    }

    private boolean validatePort( String port) {
        // Checks whether the URL address is of correct format
        boolean isValid = false;

        try {
            double num = Double.parseDouble(port);
            if (num >= 0 && num < 65535)
                isValid = true;
            else
                throw new NumberFormatException("Port number out of range: " + num);
        } catch (NumberFormatException e) {
            Log.e(TAG, "validateUrl() Port validation failed: " + e.getMessage() );
        }
        return isValid;
    }

    public void alertDialogMessageOk(String title, String message, String buttonText) {
        // Pop alert dialog box with 1 button

        final Builder alertBuilder = new Builder(getActivity()); // alert dialog box

        if (alertShown) {
            // dismiss the alert dialog if already visible
            alert.cancel();
            alertShown = false;
        }

        alertBuilder.setTitle(title);
        alertBuilder.setMessage(message);
        alertBuilder.setPositiveButton(buttonText, null);

        alert = alertBuilder.create();
        alert.show();
        alertShown = true;
    }
}


