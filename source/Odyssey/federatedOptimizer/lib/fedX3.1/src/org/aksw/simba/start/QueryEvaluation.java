package org.aksw.simba.start;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.params.HttpParams;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.log4j.BasicConfigurator;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.sail.SailRepository;

import com.fluidops.fedx.Config;
import com.fluidops.fedx.FedXFactory;
import com.fluidops.fedx.FederationManager;
import com.fluidops.fedx.monitoring.MonitoringImpl;
import com.fluidops.fedx.monitoring.MonitoringUtil;

public class QueryEvaluation<repo> {
	
	/**
	 * write results into file
	 */
	public static BufferedWriter bw ;
	/**
	 * @param args
	 * @throws Exception 
	 */
	
public static void main(String[] args) throws Exception 
	{
	
			
//	File qryFile = new File(args[0]);
//	String[] queries = getQueries(qryFile);
	// curQuery = args[0];
//	System.out.println("Total queries to execute: "+ (queries.length-1) );
//	for(int i=1 ; i < queries.length ; i++ )		
//		System.out.println(":-------------------------------------\n"+queries[i]);
	//BasicConfigurator.configure();
	//BasicConfigurator.configure();
	// TODO Auto-generated method stub
	Config.initialize();
	Config.getConfig().set("enableMonitoring", "true");
	Config.getConfig().set("monitoring.logQueryPlan", "true");
	List<String> endpoints = loadEndpoints();
//	String results = "D:/BigRDFBench/completeness_correctness/results.n3";
   // ResultsLoader.loadResults(results);

	
	SailRepository repo = FedXFactory.initializeSparqlFederation(endpoints);
	File qryFile = new File("queries/"+args[0]);
	String qry= getQueries(qryFile);
	System.out.println(qry);
	long sTime = System.currentTimeMillis();
	
	TupleQuery query = repo.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, qry); 
	      long count = 0;
     TupleQueryResult res = query.evaluate();
  
    while(res.hasNext())  
	{
    	res.next();
	    System.out.println(count);
	  //  bw.write(count +": "+res.next()+"\n");
		count++;
	}
     
  System.out.println("Total Number of Records: " + count);
  long runTime =System.currentTimeMillis()-sTime;
  System.out.println("Query execution time (msec):"+ runTime+"\n");
 
		  MonitoringImpl ms = null ;
		  // System.out.println(QueryPlanLog.getQueryPlan());
		  MonitoringUtil.printMonitoringInformation();
		//	System.out.println(ms.getAllMonitoringInformation());
		  // System.out.println("Total Number of Records: " + count+"\n");
	//}
	// System.out.println(StatsGenerator.getFscores("D:/workspace/BigRDFBench-Utilities/results/"+queryNo, query.evaluate()));
	// System.out.println("Missing:" + StatsGenerator.getMissingResults("D:/workspace/BigRDFBench-Utilities/results/"+queryNo, res));
       FederationManager.getInstance().shutDown();
	 //  System.out.println("Done. Results written into results/"+args[1]);
	 //  bw.close();
   	//String logDir = "D:/BigRDFBench/endpoints/logs/";
	//System.out.println("Endpoint requests:" + RequestCount.getTotalEndpointRequests(logDir));
	//RequestCount.clearLogs(logDir);

	   System.exit(0);
	

	}
public static List<String> loadEndpoints() throws IOException {
	List<String> endpoints = new ArrayList<String>();
	BufferedReader br = new BufferedReader(new FileReader("endpoints"));
	String line;
	while ((line = br.readLine()) != null)
	{
		endpoints.add(line);
		System.out.println(line);
	}
    br.close();
		return endpoints;
	}
/**
 * Load query string from file
 * @param qryFile Query File
 * @return query Query string
 * @throws IOException
 */
public static String  getQueries(File qryFile) throws IOException {
	String fileStr= "" ; 
	BufferedReader br = new BufferedReader(new FileReader(qryFile));
	String line;
	while ((line = br.readLine()) != null)
	{
		fileStr = fileStr+line+"\n";
	}
    br.close();
    return fileStr ;
	//return fileStr.split("#-------------------------------------------------------");
}

}