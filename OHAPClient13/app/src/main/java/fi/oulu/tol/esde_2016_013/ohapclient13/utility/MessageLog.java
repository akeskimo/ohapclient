package fi.oulu.tol.esde_2016_013.ohapclient13.utility;

/**
* MessageLog class that contains OHAP TCP messages. MessageLog automatically adds itself to LogContainer object.
*
* Change history:
* v1.0     Aapo Keskimolo      Initial version
*
* @author Aapo Keskimolo &lt;aapokesk@gmail.com>
* @version 1.0
*/

import java.util.ArrayList;

public class MessageLog extends FileManagerUtility {
	// Takes care of saving file strings (= messages) into files by using 
	// FileManagerUtility. A general (CSV) file format is used, where the
	// first line contain only headers and all the sequential rows contain 
	// values. All the elements are expected to be of uniform length.
	
	// file
	private static final String filenamePrefix = "messagelog_";
	private static final String delimiter = ";";
	private String filename = "";
		
	// headers
	private String key_identifier = "identifier";
	private String key_timestamp = "timestamp";
	private String key_message_id = "message_id";
	private String key_message = "message";
	
	// values
	private long identifier = 0;
	private String timestamp = "";
	private long messageId = 0; 
	private String message = "";
	
	private String messageType, messageTime, messageDestination;
	
	
	public String getMessage() {
		return message;
	}
	
	public String getMessageType() {
		return messageType;
	}


	public String getMessageTime() {
		return messageTime;
	}


	public String getMessageDestination() {
		return messageDestination;
	}


	public MessageLog( LogContainer container, String newMessage, String messageType) {

		// fields to be saved in the csv file
		identifier = System.currentTimeMillis();
		timestamp = timeMillisToDateTime(this.identifier);
		messageId = container.getMessageNumber();// message session incrementing id
		message = newMessage;
		
		
		this.messageDestination = "CLIENT"; // handles only received messages
		this.messageType = messageType;
		this.messageTime = timestamp;
		
//		writeCsvFile();
		
		// add message object to the log container
		container.add(this);
}
	

	private void writeCsvFile() {
		// message data to log csv file on the local drive

		// log filename 
		filename = filenamePrefix + timeMillisToDate(identifier);
		
		if ( !isMessageIdUnique() ) {
			// if message index is found from the log file, return without appending
			System.out.println("Message-id " + messageId + " was found in the csv file \"" + filename +"\"");
			return;
		}
	
		// headers for csv file
		String [] headerFormat = new String [] {
				key_identifier, 
				key_timestamp, 
				key_message_id, 
				key_message };
		
		// collect the interesting information 
		String [] valueFormat = new String [] {
				Long.toString(identifier), 
				timestamp, 
				Long.toString(messageId), 
				message};
		
		valueFormat = stringArrayToCsvFormat(valueFormat);

		// create string array list for the csv headers/values
		ArrayList<String> list = new ArrayList<>();
		list.add( stringArrayToCsvString(headerFormat, delimiter ) );
		list.add( stringArrayToCsvString(valueFormat, delimiter ) );
		
		// write array list with headers/values to a csv file
		if (!isFile(filename) ) {
			saveListToCsv(list, filename, true); // create new file
		} 
		
		else {
			
			if ( checkHeadersInCsv( filename, headerFormat ) ) { 
				list.remove(0);
				saveListToCsv(list, filename, false); // append
			} 
			
			else {
				saveListToCsv(list, filename, true); // overwrite
			}
		}
		
	}


	public long getIdentifier() {
		return identifier;
	}
	
	public long getId() {
		return messageId;
	}
	
	public String getFilepath() {
		return filename;
	}
	
	public boolean isIdentifierUnique() {
		// checks whether the identifier is already in the csv file (csv should not have duplicate entries)
		
		long identifier = 0;
		
		ArrayList<String> list = readCsvToList(filename);
		
		for (int i = 1; i < list.size(); i++) { 
			String line = list.get(i);
			if ( findStringIndex( line.split(delimiter), Long.toString(identifier)) != -1) {
				return false;
			}
		}
		return true;
	}
	
	
	public boolean isMessageIdUnique() {
		// checks whether the identifier is already in the csv file (csv should not have duplicate entries)
		
		int indexMessageId;
		
		if (isFile(filename)) {
			ArrayList<String> list = readCsvToList(filename);
			// get message array index from headers
			indexMessageId = findStringIndex(list.get(0).split(delimiter), key_message_id);
			
			for (int i = 1; i < list.size(); i++) {
				String line = list.get(i);
				String[] arr = line.split(delimiter);
				if ( Long.parseLong(arr[indexMessageId]) == messageId ) {
					return false;
				}
			}
		}
		return true;
	}
	
	public void printMessageInfo() {
		System.out.println("\n\nPrinting object info:");
		System.out.println("messageType: " + messageType );
		System.out.println("messageTime: " + messageTime);
		System.out.println("messageDestination: " + messageDestination);
	}	
		

			
	public void printInfo()	{
		System.out.println("\n\nPrinting object info:");
		System.out.println("Identifier: " + identifier);
		System.out.println("Timestamp: " + timestamp);
		System.out.println("Message-id: " + messageId);
		System.out.println("Message: " + message);
	}
}
