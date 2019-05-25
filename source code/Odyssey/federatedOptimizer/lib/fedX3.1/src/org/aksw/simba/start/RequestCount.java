package org.aksw.simba.start;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.commons.io.FileUtils;



public class RequestCount {

	public static void main(String[] args) throws IOException {
		String logDir = "D:/BigRDFBench/endpoints/logs/";
		System.out.println(getTotalEndpointRequests(logDir));
   //  clearLogs(logDir);
	}

	public static int getTotalEndpointRequests(String logDir) throws IOException {
		File dir = new File(logDir);
		File[] listOfFiles = dir.listFiles();
		int count = 0 ;
		for (File file : listOfFiles)
		{	
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;
			while ((line = br.readLine()) != null)
			count++;
		}
		return count ;
	}
	
	public static void clearLogs(String logDir) throws IOException {
		File dir = new File(logDir);
		File[] listOfFiles = dir.listFiles();
		int count = 0 ;
		for (File file : listOfFiles)
		{	
			PrintWriter writer = new PrintWriter(file);
			writer.print("");
			writer.close();
		}
		
	}
	
}
