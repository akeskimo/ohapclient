package fi.oulu.tol.esde_2016_013.ohapclient13;

/**
 * Description:
 * Preference activity for user preferences. Preference fragment is used for content (level 1
 * hierarchy only) to support future extensions.
 *
 * Change history:
 * v1.0     Aapo Keskimolo      Initial version
 *
 * @author Aapo Keskimolo &lt;aapokesk@gmail.com>
 * @version 1.0
 */

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

public class PreferencesActivity extends ActionBarActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new PreferencesFragment())
                .commit();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }
}
