/*
 * This file is part of RDF Federator.
 * Copyright 2011 Olaf Goerlitz
 *
 * RDF Federator is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RDF Federator is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with RDF Federator.  If not, see <http://www.gnu.org/licenses/>.
 *
 * RDF Federator uses libraries from the OpenRDF Sesame Project licensed
 * under the Aduna BSD-style license.
 */
package de.uni_koblenz.west.evaluation;
import de.uni_koblenz.west.relative_error.CalculatePlanQuality;
import de.uni_koblenz.west.relative_error.WriteErrorData;
import info.aduna.iteration.IterationWrapper;
import org.openrdf.query.impl.TupleQueryResultImpl;
import org.openrdf.sail.helpers.SailWrapper;
import static org.openrdf.query.QueryLanguage.SPARQL;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.swing.JOptionPane;

import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.uni_koblenz.west.splendid.statistics.util.RequestCount;
import de.uni_koblenz.west.splendid.test.config.Configuration;
import de.uni_koblenz.west.splendid.test.config.ConfigurationException;
import de.uni_koblenz.west.splendid.test.config.Query;

/**
 * Evaluation of the query processing.
 *
 * @author Olaf Goerlitz
 */
public class QueryProcessingEval {

	private static final Logger LOGGER = LoggerFactory.getLogger(QueryProcessingEval.class);
	//private static final String DEFAULT_CONFIG = "setup/fed-test.properties";
	public static long timeElapsed =0;
	public static int totalTPsources = 0 ;
	//D:/Study/PhD/LargeRDFBenchQueries/queries/index/splendid/eval/federation-test.properties
	//D:/Study/PhD/LargeRDFBenchQueries/queries/res/splendid-output.txt
	//D:/Study/PhD/LargeRDFBenchQueries/queries/queries
	//D:/Study/PhD/LargeRDFBenchQueries/queries/results
	private static final String DEFAULT_CONFIG = "SPLENDID-Original/eval/federation-test.properties";
	//	private static final String DEFAULT_CONFIG = "eval/federation-test.properties";
	public static BufferedWriter bw ;
	public static QueryProvider qp;
	public static void main(String[] args) throws InterruptedException, IOException, RepositoryException, QueryEvaluationException, MalformedQueryException {

		//BufferedWriter bw;
		String configFile;
		String queriesFolder="";
		String outputFile="output.txt";
		String relerrFolder="";
		//bw= new BufferedWriter(new FileWriter("spl_results.txt",true));
		// assign configuration file
		if (args.length == 0) {
			configFile = DEFAULT_CONFIG;
			LOGGER.info("using default config: " + configFile);
		} else {
			configFile = DEFAULT_CONFIG;
			LOGGER.info("using config: " + configFile);
		}
		
		if(args.length>0) 
			configFile = args[0];
		if(args.length>1)
			outputFile= args[1];
		if(args.length>2)
			queriesFolder = args[2];
		if(args.length>3)
			relerrFolder = args[3]+"\\";
		
			
		   

		// initialize configuration and repository
		Repository repository = null;

		try {
			Configuration config = Configuration.load(configFile);
			repository = config.createRepository();
		} catch (IOException e) {
			LOGGER.error("cannot load test config: " + e.getMessage());
		} catch (ConfigurationException e) {
			LOGGER.error("failed to create repository: " + e.getMessage());
		}
		if(args.length>4)
			Configuration.relative_error = Boolean.parseBoolean(args[4]);
		bw= new BufferedWriter(new FileWriter(outputFile));
		if(Configuration.relative_error)
			WriteErrorData.pre_initfileRelativeError(relerrFolder);

		List<String> qnames;
		if(!queriesFolder.equals("")) {
			qp = new QueryProvider(queriesFolder);
			qnames = new ArrayList<String>(qp.getQueries().keySet());
		}else
			qnames = new ArrayList<String>();

		bw= new BufferedWriter(new FileWriter(args[1]));
		for(String curQueryName: qnames) {
//			File qryFile = new File("SPLENDID-Original/queries/" + curQueryName);
			File qryFile = new File(queriesFolder+"/" + curQueryName);
			 

			String curQuery = getQuery(qryFile);
			if (Configuration.relative_error)
				WriteErrorData.initializeFile(curQuery, curQueryName);
			System.out.println("\n" + qryFile.getName() + ":-------------------------------------\n" + curQuery);
				bw.write("\n"+qryFile.getName()+":-------------------------------------\n"+curQuery+"\n");
			RepositoryConnection con = repository.getConnection();
			TupleQuery tupleQuery = con.prepareTupleQuery(SPARQL, curQuery);
			//System.out.println(query.getQuery());
			//	String logDir = "D:/BigRDFBench/endpoints/logs/";
			//	System.out.println(RequestCount.getTotalEndpointRequests(logDir));
			System.out.println(tupleQuery.toString());
			long start = System.currentTimeMillis();

			TupleQueryResult res = tupleQuery.evaluate();
			
			int count = 0;
			// if(args[0].equals("-qr")){
			while (res.hasNext()) {
				BindingSet test = res.next();
//				System.out.println(count + ": " + test);
				// bw.write(count +": "+res.next()+"\n");
				if (count == 76) { //((SailBaseIteration) ((TupleQueryResultImpl) res).bindingSetIter).wrappedIter
//					System.out.println("test");
				}

				//	if(count % 1000 ==0)
				//	System.out.println(count); // one option is calculating the real cardinalitu here by computing the excludeset size
				count++;
			}
			long runTime = System.currentTimeMillis() - start;

			if (Configuration.relative_error)
				CalculatePlanQuality.writeToFile(curQueryName);
//		IterationWrapper iterationWrapper = (() ((TupleQueryResultImpl) res).bindingSetIter).wrappedIter
			// }
			// System.out.println("Source selection time  "+ timeElapsed); //((SailBaseIteration) ((TupleQueryResultImpl) res).bindingSetIter).wrappedIter
			//  System.out.println("Total triple pattern-wise selected sources: "+ totalTPsources);
			//	  bw.write("Total triple pattern-wise selected sources: "+ totalTPsources+"\n");
			//	  bw.write("Source selection time  "+ timeElapsed+"\n");
			//    if(args[0].equals("-qr")){
			System.out.println(CalculatePlanQuality.realCard.values());
			System.out.println("Query execution time (msec):" + runTime);
			  bw.write(": Query execution time (msec):"+ runTime+"\n");
			System.out.println("Total Number of Records: " + count);
			   bw.write("Total Number of Records: " + count+"\n");
			// }
			System.out.println("Done. Results written into output.txt");
			  //bw.close();
		}
		//	String logDir = args[0];
		//	System.out.println(RequestCount.getTotalEndpointRequests(logDir));
		try {
			
			bw.close();
			LOGGER.info("shutting down repository");
			repository.shutDown();
			LOGGER.info("shutdown complete");
			System.exit(0);
		} catch (RepositoryException e) {
			e.printStackTrace();
		}
		System.exit(0);


	}
	/**
	 * Load query string from file
	 * @param qryFile Query File
	 * @return query Query string
	 * @throws IOException
	 */
	public static String  getQuery(File qryFile) throws IOException {

		String query = "" ;
		BufferedReader br = new BufferedReader(new FileReader(qryFile));
		String line;
		while ((line = br.readLine()) != null)
		{
			query = query+line+"\n";
		}
		br.close();
		return query;
	}

}
