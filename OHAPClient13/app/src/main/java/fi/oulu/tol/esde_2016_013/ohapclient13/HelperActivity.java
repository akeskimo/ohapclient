package fi.oulu.tol.esde_2016_013.ohapclient13;

/**
 * Description:
 * Helper activity for displaying information about the OHAPClient13 application
 *
 * Change history:
 * v1.0     Aapo Keskimolo      Initial version
 *
 * @author Aapo Keskimolo &lt;aapokesk@gmail.com>
 * @version 1.0
 */

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

public class HelperActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_helper);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

}
