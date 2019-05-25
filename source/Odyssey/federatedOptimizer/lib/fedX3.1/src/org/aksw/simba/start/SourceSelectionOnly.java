package org.aksw.simba.start;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.params.HttpParams;
import org.apache.http.impl.client.AbstractHttpClient;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.algebra.QueryRoot;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.parser.ParsedQuery;
import org.openrdf.query.parser.QueryParser;
import org.openrdf.query.parser.sparql.SPARQLParserFactory;
import org.openrdf.repository.sail.SailRepository;

import com.fluidops.fedx.Config;
import com.fluidops.fedx.FedX;
import com.fluidops.fedx.FedXFactory;
import com.fluidops.fedx.FederationManager;
import com.fluidops.fedx.cache.Cache;
import com.fluidops.fedx.optimizer.GenericInfoOptimizer;
import com.fluidops.fedx.optimizer.SourceSelection;
import com.fluidops.fedx.sail.FedXSailRepositoryConnection;
import com.fluidops.fedx.structures.Endpoint;
import com.fluidops.fedx.structures.QueryInfo;
import com.fluidops.fedx.structures.QueryType;

public class SourceSelectionOnly {
	public static int totalTpSources ;
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
//	BasicConfigurator.configure();
	//BasicConfigurator.configure();
	// TODO Auto-generated method stub
	Config.initialize();
//	Config.getConfig().set("enableMonitoring", "true");
//	Config.getConfig().set("monitoring.logQueryPlan", "true");
	List<String> endpoints = loadEndpoints();
//	String results = "D:/BigRDFBench/completeness_correctness/results.n3";
   // ResultsLoader.loadResults(results);

	
	SailRepository repo = FedXFactory.initializeSparqlFederation(endpoints);
//	AbstractHttpClient httpClient = (AbstractHttpClient)repo.getSesameClient()).getHtttpClient();
//	  HttpParams params = httpClient.getParams();
//	  params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 2000);
//	  httpClient.setParams(params);
	//String tq = "Select * where { ?s ?p ?o} limit 10";
//	File Querydir = new File("queries/");
//	File[] listOfQueryFiles = Querydir.listFiles();
//  for (File qryFile : listOfQueryFiles)
//	{	
	// File qryFile = new File(args[0]);
	 bw= new BufferedWriter(new FileWriter(new File("source-sel-only-results.txt")));	
	//String[] queries = getQueries(qryFile);
	// curQuery = args[0];
	File qryFile = new File(args[0]);
	String[] queries = getQueries(qryFile);
	//queries[0] = "Select * where { ?s ?p ?o} ";
	//System.out.println("Total queries to execute: "+ (queries.length-1) ); 	
int  totalDistSources = 0;
	for(int i=1 ; i < queries.length ; i++ )
	{
	System.out.println(i+":-------------------------------------\n"+queries[i].replace("\n", " "));
	bw.write(i+":-------------------------------------\n"+queries[i].replace("\n", " ")+"\n");
	Cache cache = FederationManager.getInstance().getCache();
	TupleQuery query = repo.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, queries[i]); 
	FedX fed = FederationManager.getInstance().getFederation();
	List<Endpoint> members = fed.getMembers();
	QueryInfo queryInfo = new QueryInfo(queries[i], getOriginalQueryType(query.getBindings()));
	GenericInfoOptimizer info = new GenericInfoOptimizer(queryInfo);
	SPARQLParserFactory factory = new SPARQLParserFactory();
	QueryParser parser = factory.getParser();
	ParsedQuery parsedQuery = parser.parseQuery(queries[i], null);
	TupleExpr parsed = parsedQuery.getTupleExpr();
	TupleExpr queryT = new QueryRoot(parsed.clone());
	info.optimize(queryT);
	SourceSelection sourceSelection = new SourceSelection(members, cache, queryInfo);
	sourceSelection.doSourceSelection(info.getStatements());
	Set<Endpoint> relevantSources = sourceSelection.getRelevantSources();	
	System.out.println("Distinct sources selected: "+ relevantSources.size());
	bw.write("Distinct sources selected: "+ relevantSources.size()+"\n");
    totalDistSources = totalDistSources+ relevantSources.size();
    
   }
	 //  }
	System.out.println("\n-----------------\nNet total Triple Pattern-wise sources selected: "+ totalTpSources);
	bw.write("\n-----------------\nNet total Triple Pattern-wise sources selected: "+ totalTpSources+"\n");
	System.out.println("Total distinct sources selected: "+ totalDistSources);
	bw.write("Total distinct sources selected: "+ totalDistSources+"\n");
	bw.flush();
	bw.close();
	       FederationManager.getInstance().shutDown();
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
private static QueryType getOriginalQueryType(BindingSet b) {
	if (b==null)
		return null;
	Value q = b.getValue(FedXSailRepositoryConnection.BINDING_ORIGINAL_QUERY_TYPE);
	if (q!=null)
		return QueryType.valueOf(q.stringValue());
	return null;
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