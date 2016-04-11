package fi.oulu.tol.esde_2016_013.ohapclient13;

/**
 * Description:
 * Preference fragment that includes changing URL, Port and Auto-connect boolean value. The url and
 * port are validated before saving.
 *
 * Change history:
 * v1.0     Aapo Keskimolo      Initial version
 *
 * @author Aapo Keskimolo &lt;aapokesk@gmail.com>
 * @version 1.0
 */


import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.util.Log;

import com.opimobi.ohap.CentralUnit;
import com.opimobi.ohap.CentralUnitConnection;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;


public class PreferencesFragment extends PreferenceFragment {

    // Log tag
    private final String TAG = getClass().getSimpleName();

    // Central unit object
    private CentralUnit centralUnit = null;

    private String url_address, port_number;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // load preferences from XML source
        addPreferencesFromResource(R.xml.preferences);

        // get preferences from application context
        // Note: fragment does not have context thus it has to be requested from the activity the
        // fragment is attached to
        SharedPreferences defaultPreferences =
                PreferenceManager.getDefaultSharedPreferences(this.getActivity());



        // get url from shared preferences and set it as summary
        Preference urlPref = findPreference(getString(R.string.pref_url_key));
        url_address = defaultPreferences.getString(getString(R.string.pref_url_key), "");
        urlPref.setSummary( url_address);

        urlPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                String key = preference.getKey();
                String value = (String) newValue;

                if ( key.equals(getString(R.string.pref_url_key)) ) {
                    // url input

                    Log.d(TAG, "onSharedPreferencesChanged(): key = " + key + ", value = " + newValue );

                    if ( validateUrl(value) ) {
                        preference.setSummary( value);
                        return true;
                    }
                    else {
                        builder.setTitle("Invalid URL address");
                        builder.setMessage("Valid URL example:\nhttp://foo.bar.com");
                        builder.setPositiveButton(android.R.string.ok, null);
                        builder.show();
                        return false;
                    }

                }

                return false;
            }
        });

        // get port from shared preferences and set it a summary
        Preference portPref = findPreference(getString(R.string.pref_port_key));
        port_number = defaultPreferences.getString(getString(R.string.pref_port_key), "");
        portPref.setSummary( port_number );

        // set port preference change listener
        portPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                String key = preference.getKey();
                String value = (String) newValue;

                if ( key.equals(getString(R.string.pref_port_key)) ) {
                    // port input

                    Log.d(TAG, "onSharedPreferencesChanged(): key = " + key + ", value = " + newValue );

                    if ( validateUrl(value) ) {
                        preference.setSummary( value);
                        return true;
                    }
                    else {
                        builder.setTitle("Invalid port");
                        builder.setMessage("Port must be a number between 0-65535");
                        builder.setPositiveButton(android.R.string.ok, null);
                        builder.show();
                        return false;
                    }

                }
                return false;
            }
        });

        try {
            centralUnit = CentralUnitConnection.getInstance();
        } catch (MalformedURLException e) {
            Log.e(TAG, "onCreate() Unable to intialize CentralUnit: " + e.getMessage());
        }
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
}
