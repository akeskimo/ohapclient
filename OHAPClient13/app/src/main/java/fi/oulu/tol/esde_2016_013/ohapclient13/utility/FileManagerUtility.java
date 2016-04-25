package fi.oulu.tol.esde_2016_013.ohapclient13.utility;

/**
 * Generic abstract utility class that contains methods for Java IO reading/writing and string manipulation
 *
 * Change history:
 * v1.0     Aapo Keskimolo      Initial version
 *
 * @author Aapo Keskimolo &lt;aapokesk@gmail.com>
 * @version 1.0
 */

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
//import java.nio.file.FileSystems;
//import java.nio.file.Files;
//import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

public abstract class FileManagerUtility {
	
	public static void saveListToCsv(ArrayList<String> list, String filename, boolean overwrite) {
		// saves list to array in csv format
		
		try {
			FileWriter file;
			
			if (isFile(filename) && !overwrite) {
				// if file exists -> append 
				file = new FileWriter(filename, true);
			} else {
				// if file does not exist or overwrite is forced-> create new file
				file = new FileWriter(filename);
			}
			
			PrintWriter writer = new PrintWriter(file);
			
			int i;
			for (i = 0; i < list.size(); i++ ) {
				String string = list.get(i);
				writer.print(string);
				writer.println();
//				System.out.println("Wrote row: " + string );
			}
			
			writer.close();
			
		} catch (IOException ioe) {
			logError(ioe);
		}
	}
	
	
	public static String stringArrayToCsvString(String [] stringArray, String delimiter) {
		// converts string array to string with delimiters

		String string = "";
		for (int i = 0; i < stringArray.length-1; ++i) {
			string += stringArray[i] + delimiter;
		}
		string += stringArray[stringArray.length-1];
		
		return string;
	}
	
	
	public static ArrayList<String> readCsvToList(String filename) {
		// reads csv file into array list 
		
		ArrayList<String> list = new ArrayList<String>();
		
		if (isFile(filename)) {
			
			try {
				Scanner sc = new Scanner(new File(filename));
				sc.useDelimiter(System.getProperty("line.separator"));
				
				while (sc.hasNext()) {
				    String line = sc.next();
				    list.add(line);
	//			    String cells[] = line.split("\t");
	//			    System.out.println(cells.length);
	//			    System.out.println(line);
				}
				
				
	//			private static final int BUFFER_SIZE = 2048;
				
				// Using character reading (prints out strange 'k' as '\n' character)
				// problem: characters /r/n are very difficult to separate
	//			FileReader file = new FileReader(filename);
	//			BufferedReader reader = new BufferedReader(file);
	//
	//			// Read by character
	//			while( (c = reader.read()) != -1) {
	//				
	//				buffer[i] = (char) c;
	//
	//				if (c == '\n' || c == '\r') {
	//
	//					// handle newline
	//					char [] array = new char[i];
	//					System.arraycopy(buffer, 0, array, 0, i);
	//					string = String.valueOf(array);
	//					list.add(string);
	//					System.out.println("Read row: " + string );
	//					i = 0;
	//				}
	//				i++;
	//			}
				
				
	////		problem: does not recognize \r\n -> reads too many lines
	//			while ( (line = reader.readLine()) != null) {
	//				System.out.print("Read line: " + line);
	//				list.add(line);
	//			}
							
	//			reader.close();
				
				sc.close();
				
			} catch (IOException ioe) {
				logError(ioe);
			}
		}
		
		return list;
	}
	
	
	public static String[] stringArrayToCsvFormat(String [] strings) {
		
		for (int i = 0; strings.length < i; i++) {
			
			String str = strings[i];
			
			// strip traling/leading white space 
			str = str.trim();

			// surround values with double-quotes, if value has
			// a) spaces in between
			// b) new line 
			String newLine = System.getProperty("line.separator");
			String whitespace = " ";
			if (str.contains(newLine) || str.contains(whitespace)) {
				str = "\"" + str + "\"";
			}
			
		}
		
		return strings;
	}

	

	public static boolean checkHeadersInCsv( String filename, String [] headersFormat) {

		ArrayList<String> list = FileManagerUtility.readCsvToList( filename);
		String [] headers;
		
		String headerLine = list.get(0);
		headers = headerLine.split(";");
		// TODO trim format

//		System.out.println("Original: " + Arrays.toString(headersFormat) );
//		System.out.println("Csv: " + Arrays.toString(headers));
		
		if (Arrays.toString(headersFormat).equals(Arrays.toString(headers))) {
			return true;
		}
		
		return false;
		
	}
	
	
	public static String timeMillisToDateTime(long timestamp) {
		//convert milliseconds to datetime
		
		Date date = new Date(timestamp);
		DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS");
		String datetime = dateFormat.format(date);
		
		return datetime;
	}
	
	public static String timeMillisToDate(long timestamp) {
		//convert milliseconds to short date format YYYYMMDD (useful as file prefix)
		
		Date date = new Date(timestamp);
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
		String date_yyyymmdd = dateFormat.format(date);
		
		return date_yyyymmdd;
	}
	
	
	public static int countLines(String filename) throws IOException {
		// count lines '\r' in a string 
	    InputStream is = new BufferedInputStream(new FileInputStream(filename));
	    try {
	        byte[] c = new byte[1024];
	        int count = 0;
	        int readChars = 0;
	        boolean empty = true;
	        while ((readChars = is.read(c)) != -1) {
	            empty = false;
	            for (int i = 0; i < readChars; ++i) {
	                if (c[i] == '\r') {
	                    ++count;
	                }
	            }
	        }
	        return (count == 0 && !empty) ? 1 : count;
	    } finally {
	        is.close();
	    }
	}
	

	public static void printList(ArrayList<String> list1) {
		// print array list
		for (int i = 0; i < list1.size(); i++) {
			System.out.println( list1.get(i));
		}
	}
	
//	public static void deleteFile( String filepath) {
//		// delete file (if exists)
//		Path path = FileSystems.getDefault().getPath(filepath);
//		try {
//			Files.deleteIfExists(path);
//		} catch (IOException e) {
//			logError(e);
//		}
//	}
	
	public static boolean isFile(String filepath) {
		// checks whether filepath (string) exists
		File f = new File(filepath);		
		if (f.exists() && !f.isDirectory()) {
			return true;
		} 
		return false;
	}
	
	public static void logError( Exception e) {
		// log facility (may be overwritten)
		System.out.println("Error occurred: " + e.getMessage() );
		e.printStackTrace();
	}
	
	public static int findStringIndex(String[] stringArray, String string) {
		// returns the index of the string in a string array
		// returns -1 if no string is found
		for (int index = 0; index < string.length(); index++) {
			if ( stringArray[index].equals(string) ) {
				return index;
			}
		}

		return -1;
	}
	
	public static void printMap( Map values) {
		// print map with iterator
	    Iterator it = values.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pair = (Map.Entry)it.next();
	        System.out.println(pair.getKey() + " = " + pair.getValue());
	        it.remove(); // avoids a ConcurrentModificationException
	    }
	}
	
	
	public String[] formatValuesToCsv(String [] strings) {
		// format string array to csv format
		
		for (int i = 0; strings.length < i; i++) {
			
			String str = strings[i];
			
			// strip traling/leading white space 
			str = str.trim();

			// surround values with double-quotes, if value has
			// a) spaces in between
			// b) new line 
			String newLine = System.getProperty("line.separator");
			String whitespace = " ";
			if (str.contains(newLine) || str.contains(whitespace)) {
				str = "\"" + str + "\"";
			}
			
		}
		return strings;
		
	}
	
}