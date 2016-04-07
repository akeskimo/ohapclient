package fi.oulu.tol.esde_2016_013.ohapclient13;

import android.util.Log;

import com.opimobi.ohap.CentralUnit;
import com.opimobi.ohap.CentralUnitConnection;
import com.opimobi.ohap.Device;
import com.opimobi.ohap.Item;

import java.net.MalformedURLException;
import java.net.URL;
import junit.framework.*;

/**
 *
 * Test class for testing OHAP container class
 *
 * For documentation:
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */

public class OHAPTest extends TestCase {

    // Log tag
    private final String TAG = this.getClass().getSimpleName();

    // OHAP applications
    CentralUnit centralUnit = null;
    Device dummyDevice = null;

    // Global Variables
    private Device.Type activeDeviceType = null;
    private Device.ValueType activeDeviceValueType = null;
    private String activeDeviceName = "";
    long device_id = 1;

    public OHAPTest() {
        try {
            centralUnit = CentralUnitConnection.getInstance();
        } catch (MalformedURLException e) {
            Log.e(TAG, "OHAPContainerTest() " + e.getMessage() );
        }
        centralUnit.startListening();
        activeDeviceName = "Dummy Device";
        activeDeviceType = Device.Type.ACTUATOR;
        activeDeviceValueType = Device.ValueType.BINARY;
        dummyDevice = new Device(centralUnit,device_id,activeDeviceType, activeDeviceValueType);
    }

    public void testCase01() {
        // test that item is added to container
        Item item = centralUnit.getItemById(device_id);
        assertEquals( activeDeviceName, item.getName());
        assertEquals( device_id, item.getId());
        assertEquals( 1, centralUnit.getItemCount());
    }

    public void testCase02() {
        // start and stop listening
        assertEquals(true, centralUnit.isListening());
        centralUnit.stopListening();
        assertEquals(false, centralUnit.isListening());
        centralUnit.startListening();
        assertEquals(true, centralUnit.isListening());

    }

    public void testCase03() {
        // set and get binary value of the device
        dummyDevice.setBinaryValue(true);
        assertEquals(true, dummyDevice.getBinaryValue());

        dummyDevice.setBinaryValue(false);
        assertEquals( false, dummyDevice.getBinaryValue() );
    }

    public void testCase04() {
        // set and get binary value of the device
        dummyDevice.setBinaryValue(true);
        assertEquals(true, dummyDevice.getBinaryValue());

        dummyDevice.setBinaryValue(false);
        assertEquals( false, dummyDevice.getBinaryValue() );
    }

}
