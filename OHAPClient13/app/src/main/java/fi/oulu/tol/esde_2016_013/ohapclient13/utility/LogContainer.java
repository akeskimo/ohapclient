package fi.oulu.tol.esde_2016_013.ohapclient13.utility;

/**
 * MessageLog Container class that stores MessageLog objects containing TCP messages for OHAP 
 *
 * Change history:
 * v1.0     Aapo Keskimolo      Initial version
 *
 * @author Aapo Keskimolo &lt;aapokesk@gmail.com>
 * @version 1.0
 */

import java.util.ArrayList;

public class LogContainer {
	
	private ArrayList<MessageLog> messageList = new ArrayList<>();
	private static LogContainer instance = null;
	private static long messageNumber = 0;
	
	private LogContainer() {}
	
	public static LogContainer getInstance() {
		if (instance == null) {
			instance = new LogContainer();
		}
		return instance;
	}
	
	public long getItemId(int index) {
        return messageList.get(index).getId();
    }
	
	public MessageLog getItemByIndex(int index) {
        return messageList.get(index);
    }
	
	public int getItemCount() {
        return messageList.size();
    }

    public void add(MessageLog item) {
    	messageNumber++;
        messageList.add(item);
    }

    public void remove(MessageLog item) {
        messageList.remove(item);
    }

	public long getMessageNumber() {
		return messageNumber;
	}
}
