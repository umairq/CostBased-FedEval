package com.fluidops.fedx.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.aksw.simba.start.QueryEvaluation;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.sail.SailRepository;

import com.fluidops.fedx.Config;
import com.fluidops.fedx.FedXFactory;
import com.fluidops.fedx.exception.FedXException;

public class MyTimerTask extends TimerTask {
   public static List<String> endpoints;
	public static SailRepository repo ;
	public static boolean isCompleted; 
	public static long count,timeout = 180000;
	public static String curQuery ;
    public void run() {
     //   System.out.println("Timer task started at:"+new Date());
        completeTask();
   //     System.out.println("Timer task finished at:"+new Date());
    }

    private void completeTask() 
		{
    	try {
			
			TupleQuery query = repo.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, curQuery); 
		//	System.out.println("i am here ..");
		    long sTime = System.currentTimeMillis();
			TupleQueryResult res = query.evaluate();
			count = 0;
		//	 System.out.println("at start... count = "+count);
			while(res.hasNext())  
		{
				//Thread.sleep(2000);
			res.next();
		//	System.out.println(count);
			
			count++;
		} 
			System.out.println("Query execution time: "+(System.currentTimeMillis()-sTime));
			isCompleted = true ;
			}catch (Exception e) {
			System.err.println("Error occured: "+ e.getMessage());
			e.printStackTrace();
		}
		}
	
    
    public static void main(String args[]) throws Exception{
    
    	Config.initialize();
		 endpoints = QueryEvaluation.loadEndpoints();
		 repo= FedXFactory.initializeSparqlFederation(endpoints);
//			TupleQuery query = repo.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, "SELECT ?s WHERE { ?s a ?o } Limit 10"); 
//			System.out.println("i am here ..");
//		    long sTime = System.currentTimeMillis();
//			TupleQueryResult res = query.evaluate();
//			count = 0;
//			 System.out.println("at start... count = "+count);
//			while(res.hasNext())  
//		{
//			res.next();
//			System.out.println(count);
//			//Thread.sleep(1000);
//			count++;
//		} 
//			System.out.println("Query execution is completed ... ");
		 
		// bw= new BufferedWriter(new FileWriter(new File("results"+System.currentTimeMillis()+".txt")));	
			
			File qryFile = new File(args[0]);
			String[] queries = getQueries(qryFile);
			//queries[0] = "Select * where { ?s ?p ?o} ";
			//System.out.println("Total queries to execute: "+ (queries.length-1) ); 	
			long qmTime = 0, timeout = 180000 ; 
			List<String> timeOutQueries = new ArrayList<String> ();
			for(int i=1 ; i < queries.length ; i++ )
			{
			System.out.println(i+":-------------------------------------\n"+queries[i].replace("\n", " "));
	          curQuery = queries[i];
			Thread.sleep(1000);
			 count = 0;
			 isCompleted = false; 
			 try {
			// System.out.println("------------------\nQuery no started: "+q);
        TimerTask timerTask = new MyTimerTask();
        //running timer task as daemon thread
        Timer timer = new Timer(true);
       timer.schedule(timerTask, 0);
     //   System.out.println("TimerTask started");
        //cancel after sometime
       
            Thread.sleep(180000);
      
        timer.cancel();
        System.out.println("Total number of results: " + count);
        if(isCompleted==false)
        System.out.println("timeout");
			  } catch (Exception e) {
		            System.err.println("Error occured. "+ e.getMessage());;
		        }
			}	 
       System.exit(0);
       
    }
			/**
			 * Load query string from file
			 * @param qryFile Query File
			 * @return query Query string
			 * @throws IOException
			 */
			public static String[]  getQueries(File qryFile) throws IOException {
				String fileStr= "" ; 
				BufferedReader br = new BufferedReader(new FileReader(qryFile));
				String line;
				while ((line = br.readLine()) != null)
				{
					fileStr = fileStr+line+"\n";
				}
			    br.close();
			    
				return fileStr.split("#-------------------------------------------------------");
			}

}