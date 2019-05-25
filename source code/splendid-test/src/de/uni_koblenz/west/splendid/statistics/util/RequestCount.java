package de.uni_koblenz.west.splendid.statistics.util;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;


public class RequestCount {

	public static void main(String[] args) throws IOException {
	//	String logDir = "D:/BigRDFBench/endpoints/logs/";
		String logDir = args[0];
		System.out.println(getTotalEndpointRequests(logDir));
       
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
			{
			count++;
			//System.out.println(file.getName()+": "+line);
			}
			FileWriter writer = new FileWriter(file.getAbsolutePath());
            BufferedWriter bufferedWriter = new BufferedWriter(writer);
		}
		return count ;
	}
	
}
