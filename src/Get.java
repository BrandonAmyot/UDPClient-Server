import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;


public class Get {
	private static boolean verbose = false;
	private static boolean headers = false;	
	private static ArrayList<String> headerArr = new ArrayList<String>();
	
	// Accessor and Mutator methods
	public static boolean isVerbose() {
		return verbose;
	}
	public static void setVerbose(boolean verbose) {
		Get.verbose = verbose;
	}
	public static boolean isHeaders() {
		return headers;
	}
	public static void setHeaders(boolean headers) {
		Get.headers = headers;
	}
	
	// Make GET request
	public static void doGet(String urlShort) throws Exception {
		
		URL url = new URL(urlShort);

		String hostName = url.getHost();
		int port = 80;
				
		Socket mySocket = new Socket(hostName, port);
			
		// send request
		PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(mySocket.getOutputStream()))); 	
		
		out.println("GET " + url + " HTTP/1.0");
		out.println("Host: " + hostName);
		// if there is a header command, return specific key and value
		if(headers) {
			// i < array.length
			for(int i =0; i < headerArr.size(); i++) {
				out.println(headerArr.get(i).toString());
			}
		}
		out.println();
		out.flush();
			
		// Print the response
		BufferedReader in = new BufferedReader(new InputStreamReader(mySocket.getInputStream())); 
		
		String response;
		boolean messageBody = false;
		
		while((response = in.readLine()) != null) {
			if(verbose)
				System.out.println(response);
			else {
				if(messageBody)
					System.out.println(response);
				if(response.equals(""))
					messageBody = true;
			}
		}				
	
		in.close();
		mySocket.close();
	}
	
	public static void addHeaders(String url) {
		for(int i=0; i<url.length(); i++) {
			
			if(url.charAt(i)== '-' && url.charAt(i+1) == 'h') {
				String temp = StringUtils.substring(url, i+3);
				String header = StringUtils.substringBefore(temp, " ");
				headerArr.add(header);
			}
		}
	}
}
