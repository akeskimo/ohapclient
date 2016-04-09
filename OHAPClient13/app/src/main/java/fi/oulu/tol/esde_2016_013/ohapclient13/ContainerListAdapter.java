package fi.oulu.tol.esde_2016_013.ohapclient13;

/**
 * Description:
 * List Adapter for ContainerActivity. The List adapter has an item container and forwards the data
 * to ListView. Documentation </http://developer.android.com/guide/topics/ui/layout/listview.html>
 *
 * Change history:
 * v1.0     Aapo Keskimolo      Initial version
 *
 * @author Aapo Keskimolo &lt;aapokesk@gmail.com>
 * @version 1.0
 */

import android.content.Context;
import android.database.DataSetObserver;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.opimobi.ohap.Container;
import com.opimobi.ohap.Device;


public class ContainerListAdapter implements android.widget.ListAdapter {

    // Log tag
    private final String TAG = this.getClass().getSimpleName();

    // Container object that will store the item information displayed on ListView
    private Container container;

    // Constructor of Container object takes Container object as a parameter
    ContainerListAdapter(Container container) {
        if (container != null)
            this.container = container;
        else
            throw new NullPointerException("Container object is null!");
    }

    // This ViewHolder class contains all widgets that will be used to display row information
    // on ListView. Using existing widgets (=recycling), is much faster than creating new objects
    private static class ViewHolder {
        public TextView rowTextView;
        public ImageView imgView;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        return true; // true = interactive row
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) { }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) { }

    @Override
    public int getCount() {
        Log.d(TAG, "getCount() container.getItemCount() returned: " + container.getItemCount());
        return container.getItemCount();
    }

    @Override
    public Object getItem(int position) {
        Log.d(TAG, "getItem() container.getItem(position) returned: " + container.getItemByIndex(position) );
        return container.getItemByIndex(position);
    }

    @Override
    public long getItemId(int position) {
        Log.d(TAG, "getItemId() container.getItemByIndex(position).getId() returned: " + container.getItemByIndex(position).getId() );
        return container.getItemByIndex(position).getId();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            Context context = parent.getContext();
            LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.listview_row, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.rowTextView = (TextView)convertView.findViewById(R.id.rowTextView);
            viewHolder.imgView = (ImageView) convertView.findViewById(R.id.item_icon);
            convertView.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) convertView.getTag();
//            Log.d(TAG, "getView() No existing ViewHolder: Getting new from ConvertView wrapper");
        }

        String text = "";

        try {
            // 1. Get item in the selected position and cast it to device object
            Device device = (Device)container.getItemByIndex(position);
            // 2. Get item name
            text = device.getName();
            // 3. Set row icon to match the device type
            if (device.getType() == Device.Type.ACTUATOR)
                viewHolder.imgView.setImageResource(R.drawable.ic_lamp);
            else if (device.getType() == Device.Type.SENSOR)
                viewHolder.imgView.setImageResource(R.drawable.ic_temperaturesensor);
            else
                Log.wtf(TAG, "getView() No device type found!");

        } catch (Exception e) {
            Log.d(TAG, "getView() Unable to get device information "  + e. getMessage() );
            e.printStackTrace();
        }


        try {
            // 4. Set text on row
            viewHolder.rowTextView.setText( text );

        } catch (Exception e) {
            Log.e(TAG, "getView() Unable to set viewHolder text at position "
                    + position + " " + e.getMessage() );
            e.printStackTrace();
        }
        return convertView;
    }

    @Override
    public int getItemViewType(int position) {
        // By returning 0, the list adapter will always use the same viewType - see getViewTypeCount()
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        // This will tell the adapter framework not to create new textViews every time, which will
        // save resources and make the list view interface act faster
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }
}
