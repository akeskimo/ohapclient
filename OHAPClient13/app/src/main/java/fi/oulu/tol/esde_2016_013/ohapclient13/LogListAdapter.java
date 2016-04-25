package fi.oulu.tol.esde_2016_013.ohapclient13;

/**
 * Description:
 * List Adapter for LogActivity.
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
import android.widget.TextView;

import fi.oulu.tol.esde_2016_013.ohapclient13.utility.LogContainer;
import fi.oulu.tol.esde_2016_013.ohapclient13.utility.MessageLog;

public class LogListAdapter implements android.widget.ListAdapter {

    // Log tag
    private final String TAG = this.getClass().getSimpleName();

    // Message class object that will store the item information displayed on ListView
    private LogContainer logContainer = null;
    private MessageLog messageLog = null;

    // Constructor assigns logContainer object to be able to have access to the CentralUnit methods
    LogListAdapter(LogContainer logContainer) {
        if (logContainer != null)
            this.logContainer = logContainer;
        else
            throw new NullPointerException("Message object is null!");
    }

    // This ViewHolder class contains all widgets that will be used to display row information
    // on ListView. Using existing widgets (=recycling), is much faster than creating new objects
    private static class ViewHolder {
        public TextView textViewDestination;
        public TextView textViewMessageType;
        public TextView textViewTime;
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
        Log.d(TAG, "getCount() logContainer.getItemCount() returned: " + logContainer.getItemCount());
        return logContainer.getItemCount();
    }

    @Override
    public Object getItem(int position) {
        Log.d(TAG, "getItem() logContainer.getItem(position) returned: " + logContainer.getItemByIndex(position) );
        return logContainer.getItemByIndex(position);
    }

    @Override
    public long getItemId(int position) {
        Log.d(TAG, "getItemId() logContainer.getItemByIndex(position).getId() returned: " + logContainer.getItemByIndex(position).getId() );
        return logContainer.getItemByIndex(position).getId();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // View inflater service
        // uses Viewholder class to store the currently visible views that are recycled
        ViewHolder viewHolder;
        if (convertView == null) {
            Context context = parent.getContext();
            LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.log_row, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.textViewDestination = (TextView)convertView.findViewById(R.id.textViewDestination);
            viewHolder.textViewMessageType = (TextView) convertView.findViewById(R.id.textViewMessageType);
            viewHolder.textViewTime = (TextView) convertView.findViewById(R.id.textViewTime);
            convertView.setTag(viewHolder);
            Log.d(TAG, "getView() View objects created.");
        }
        else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        try {
            // get message object
            messageLog = logContainer.getItemByIndex(position);

            // Set textview properties
            viewHolder.textViewDestination.setText(messageLog.getMessageDestination());
            viewHolder.textViewMessageType.setText(messageLog.getMessageType());
            viewHolder.textViewTime.setText(messageLog.getMessageTime());

        } catch (Exception e) {
            Log.d(TAG, "getView() Unable to set text on view "  + e. getMessage() );
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
