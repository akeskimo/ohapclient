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

    // Central unit object
    private CentralUnitConnection centralUnit = null;
    private String url_address, port_number;

    // alert dialog box
    AlertDialog alert = null;
    private boolean alertShown = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        try {
            centralUnit = CentralUnitConnection.getInstance();
        } catch (MalformedURLException e) {
            Log.e(TAG, "onCreate() Unable to intialize CentralUnit: " + e.getMessage());
        }

        // get url from shared preferences and set it as summary
        Preference urlPref = findPreference(getString(R.string.pref_url_key));
        url_address = centralUnit.getURL().getHost();
        urlPref.setSummary( url_address);

        urlPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            // URL preference change listener
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String key = preference.getKey();
                String value = (String) newValue;

                if ( key.equals(getString(R.string.pref_url_key)) ) {
                    // url input

                    Log.d(TAG, "onSharedPreferencesChanged(): key = " + key + ", value = " + newValue );

                    if ( validateUrl(value) ) {
                        preference.setSummary( value);
                        try {
                            centralUnit.setURL(new URL(value + ":" + port_number));
                            Log.i(TAG, "setOnPreferenceChangeListener() CentralUnit URL updated: " + centralUnit.getURL().toString());
                        } catch (MalformedURLException e) {
                            Log.e(TAG, "setOnPreferenceChangeListener() Malformed URL");
                        }
                        return true;
                    }
                    else {
                        String ttl = "Invalid URL";
                        String msg = "Valid URL example:\nhttp://foo.bar.com";
                        alertDialogMessageOk(ttl, msg, "OK");
                        return false;
                    }
                }
                return false;
            }
        });

        Preference portPref = findPreference(getString(R.string.pref_port_key));
//        port_number = defaultPreferences.getString(getString(R.string.pref_port_key), "");
        port_number = Integer.toString( centralUnit.getURL().getPort() );
        portPref.setSummary( port_number );

        // set port preference change listener
        portPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            // Port preference change listener
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                String key = preference.getKey();
                String value = (String) newValue;

                if (key.equals(getString(R.string.pref_port_key))) {
                    // port input

                    Log.d(TAG, "onSharedPreferencesChanged(): key = " + key + ", value = " + newValue);

                    if (validatePort(value)) {
                        preference.setSummary(value);
                        try {
                            centralUnit.setURL(new URL(value + ":" + port_number));
                            Log.i(TAG, "setOnPreferenceChangeListener() CentralUnit URL updated: "
                                    + centralUnit.getURL().toString() );
                        } catch (MalformedURLException e) {
                            Log.e(TAG, "setOnPreferenceChangeListener() Malformed URL");
                        }
                        return true;
                    } else {
                        String ttl = "Invalid port";
                        String msg = "Port must be a number between 0-65535";
                        alertDialogMessageOk(ttl, msg, "OK");
                        return false;
                    }
                }
                return false;
            }
        });

        // get auto-connect preference
        Preference autoConnectPref = findPreference(getString(R.string.pref_autoconnect_key));

        // set auto connect preference change listener
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
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
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


