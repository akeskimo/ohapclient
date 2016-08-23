package fi.oulu.tol.esde_2016_013.ohapclient13;

/**
 * Description:
 * List Adapter for ContainerActivity. The List adapter has an item container and forwards the data
 * to ListView. Documentation </http://developer.android.com/guide/topics/ui/layout/listview.html>
 *
 * Change history:
 * v1.0     Aapo Keskimolo      Initial version
 * v1.1     Aapo Keskimolo      ListView displays value of the device
 *
 * @author Aapo Keskimolo &lt;aapokesk@gmail.com>
 * @version 1.1
 */

import android.content.Context;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.opimobi.ohap.Container;
import com.opimobi.ohap.Device;
import com.opimobi.ohap.EventSource;
import com.opimobi.ohap.Item;


public class ContainerListAdapter implements android.widget.ListAdapter, EventSource.Listener<Container,Item> {

    // Log tag
    private final String TAG = this.getClass().getSimpleName();

    // Container object that will store the item information that will be displayed on ListView
    private Container container;

    // Contains the array of all observers that will be notified upon onEvent -callback
    private final DataSetObservable dataSetObservable = new DataSetObservable();


    public ContainerListAdapter(Container container) {
        // Constructor assigns container that will be displayed on the listview

        if (container != null) {
            this.container = container;

            // Register event listeners when container is added with an item
            container.itemAddedEventSource.addListener(this);
            container.itemRemovedEventSource.addListener(this);

        } else {
            throw new NullPointerException("Container object is null!");
        }
    }

    public void registerDataSetObserver(DataSetObserver observer) {
        dataSetObservable.registerObserver(observer);
    }

    public void unregisterDataSetObserver(DataSetObserver observer) {
        dataSetObservable.unregisterObserver(observer);
    }

    @Override
    public void onEvent(Container container, Item item) {
        Log.d(TAG, "onEvent() Container: " + container.getId() + " has changed item: " + item.getId());
        dataSetObservable.notifyChanged();
    }

    private static class ViewHolder {
        // This ViewHolder class contains all widgets that will be used to display row information
        // on ListView. Using existing widgets (=recycling), is much faster than creating new objects

        public TextView rowTextView;
        public ImageView imgView;
        public TextView rowTextViewValue;
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
    public int getCount() {
//        Log.d(TAG, "getCount() container.getItemCount() returned: " + container.getItemCount());
        return container.getItemCount();
    }

    @Override
    public Object getItem(int position) {
//        Log.d(TAG, "getItem() container.getItem(position) returned: " + container.getItemByIndex(position) );
        return container.getItemByIndex(position);
    }

    @Override
    public long getItemId(int position) {
//        Log.d(TAG, "getItemId() container.getItemByIndex(position).getId() returned: " + container.getItemByIndex(position).getId() );
        return container.getItemByIndex(position).getId();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // View inflater service. Uses Viewholder class to store the visible views references

        ViewHolder viewHolder;
        String text = "";

        if (convertView == null) {
            Context context = parent.getContext();
            LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.listview_row, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.rowTextView = (TextView)convertView.findViewById(R.id.rowTextView);
            viewHolder.imgView = (ImageView) convertView.findViewById(R.id.item_icon);
            viewHolder.rowTextViewValue = (TextView) convertView.findViewById(R.id.rowTextViewValue);
            convertView.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) convertView.getTag();
//            Log.d(TAG, "getView() No existing ViewHolder: Getting new View from context");
        }

        try {

            if ( container.getItemByIndex(position) instanceof Container ) {
                // clicked item is a container

                Container subContainer = (Container)container.getItemByIndex(position);
                text = subContainer.getName();

                // Set displayed row icon and text
                viewHolder.imgView.setImageResource(R.drawable.ic_container);
                viewHolder.rowTextViewValue.setText("Items: " + subContainer.getItemCount());
                viewHolder.rowTextViewValue.setTextColor( Color.GRAY );

            } else {
                // clicked item is a device

                Device device = (Device) container.getItemByIndex(position);
                text = device.getName();

                // Set displayed row icon and device value

                if (device.getValueType() == Device.ValueType.BINARY) {
                    // type binary

                    viewHolder.imgView.setImageResource(R.drawable.ic_lamp);
                    if (device.getBinaryValue()) {
                        viewHolder.rowTextViewValue.setText("ON");
                        viewHolder.rowTextViewValue.setTextColor(Color.CYAN);
                    } else {
                        viewHolder.rowTextViewValue.setText("OFF");
                        viewHolder.rowTextViewValue.setTextColor(Color.GRAY);
                    }

                } else if (device.getValueType() == Device.ValueType.DECIMAL) {
                    // type decimal

                    viewHolder.imgView.setImageResource(R.drawable.ic_temperaturesensor);
                    if (device.getDecimalValue() != 0) {
                        viewHolder.rowTextViewValue.setText(
                                String.format("%1.1f " + device.getUnit(), device.getDecimalValue()));
                        viewHolder.rowTextViewValue.setTextColor(Color.CYAN);
                    } else {
                        viewHolder.rowTextViewValue.setText("OFF");
                        viewHolder.rowTextViewValue.setTextColor(Color.GRAY);
                    }

                } else {
                    Log.wtf(TAG, "getView() No device type found!");
                }
            }

        } catch (Exception e) {
            Log.d(TAG, "getView() Unable to get device information "  + e. getMessage() );
        }


        try {
            // set text to display item name
            viewHolder.rowTextView.setText( text );

        } catch (Exception e) {
            Log.e(TAG, "getView() Unable to set viewHolder text at position "
                    + position + " " + e.getMessage() );
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
