package org.aksw.simba.start;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.aksw.simba.quetsal.configuration.EndpointProvider2;
import org.aksw.simba.quetsal.core.error.RelativeError;
import org.aksw.simba.quetsal.util.SimilarityConstants;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;

import com.fluidops.fedx.Config;
import com.fluidops.fedx.FedXFactory;
import com.fluidops.fedx.structures.QueryInfo;

public class QueryEvaluation {
    public static int idxCount =0;
	protected static final Logger log = LoggerFactory.getLogger(QueryEvaluation.class);

	static {
		try {
			ClassLoader.getSystemClassLoader().loadClass("org.slf4j.LoggerFactory"). getMethod("getLogger", ClassLoader.getSystemClassLoader().loadClass("java.lang.String")).
			 invoke(null,"ROOT");
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
	QueryProvider qp;

	public QueryEvaluation(String qry_dir) throws Exception {
		qp = new QueryProvider(qry_dir);
	}
	static EndpointProvider2 ep;

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception 
	{
		String cfgName = args[0];
		String repfile = args.length > 1 ? args[1] : null;
		String queries_dir = args.length > 2 ? args[2]: null;
		String endpoint_file = args.length >3 ? args[3]: null;
		String relative_file = args.length >4 ? args[4]: null;




		String host = "163.180.116.90";
		//String host = "ws24348.avicomp.com";
		//String host = "192.168.0.145";
//		String queries = "S1 S2 S3 S4 S5 S6 S7 S8 S9 S10 S11 S12 S13 S14 C1 C2 C3 C4 C6 C7 C8 C9 C10 ch1 ch2 ch3 ch4 ch5 ch6 ch7 ch8"; //"C1 C3 C5 C6 C7 C8 C9 C10 L1 L2 L3 L4 L5 L6 L7 L8";
		//String queries = "S1 S2 S3 S4 S5 S6 S7 S8 S9 S10 S11 S12 S13 S14 C1 C2 C3 C6 C7 C8 C9 C10";
		//String queries = "C10";
		String queries = ""; // S3 C6 C2 S6 C8 C10 q2 q3 q4 q5 q6 q7 q8

		List<String> endpointsMin = Arrays.asList(
//			 "http://" + host + ":8890/sparql",
				"http://" + host + ":8891/sparql",
				"http://" + host + ":8892/sparql",
				"http://" + host + ":8893/sparql",
//				"http://" + host + ":8894/sparql",
//				"http://" + host + ":8895/sparql",
				"http://" + host + ":8896/sparql",
				"http://" + host + ":8897/sparql"
//				"http://" + host + ":8898/sparql"
		);

		List<String> endpointsMax = Arrays.asList(
				"http://" + host + ":8890/sparql",
				"http://" + host + ":8891/sparql",
				"http://" + host + ":8892/sparql",
				"http://" + host + ":8893/sparql",
				"http://" + host + ":8894/sparql",
				"http://" + host + ":8895/sparql",
				"http://" + host + ":8896/sparql",
				"http://" + host + ":8897/sparql",
				"http://" + host + ":8898/sparql"

				, "http://" + host + ":8887/sparql"
				, "http://" + host + ":8888/sparql"
				, "http://" + host + ":8889/sparql"
				, "http://" + host + ":8899/sparql"
		);

		List<String> endpointsSubSet = Arrays.asList(

				"http://" + host + ":8891/sparql"
				, "http://" + host + ":8892/sparql"
				, "http://" + host + ":8893/sparql"
				, "http://" + host + ":8896/sparql"
				, "http://" + host + ":8897/sparql"
		);

		List<String> endpointsMin2 = Arrays.asList(
//			 "http://" + host + ":8887/sparql",
//			 "http://163.180.116.110:8888/sparql",
//			 "http://" + host + ":8889/sparql",
//			 "http://" + host + ":8890/sparql",
				"http://" + host + ":8891/sparql",
				"http://" + host + ":8892/sparql",
//			 "http://" + host + ":8893/sparql",
//			 "http://" + host + ":8894/sparql",
//			 "http://" + host + ":8895/sparql",
				"http://" + host + ":8896/sparql",
				"http://" + host + ":8897/sparql"
//			 "http://" + host + ":8898/sparql",
//			 "http://" + host + ":8899/sparql"
		);

		List<String> endpointsSake = Arrays.asList(
				"http://144.76.166.111:8900/sparql",
				"http://144.76.166.111:8901/sparql"
		);
//		QueryEvaluation queryEvaluation = new QueryEvaluation();
		RelativeError.pre_initfileRelativeError(relative_file);
		List<String> endpoints = endpointsSubSet;
		if(endpoint_file!=null) {
			String endpointPath = args[3];
			ep = new EndpointProvider2(endpointPath);
			endpoints = (List<String>) ep.getEndPoints();

		}
		Map<String, List<List<Object>>> reports = multyEvaluate(queries, 1, cfgName, endpoints,queries_dir);

		for (Map.Entry<String, List<List<Object>>> e : reports.entrySet())
		{
			List<List<Object>> report = e.getValue();
			String r = printReport(report);
			log.info(r);
			if (null != repfile) {
				FileUtils.write(new File(repfile + "-" + e.getKey() + ".csv"), r);
			}
		}

		System.exit(0);
	}

	public Map<String, List<List<Object>>> evaluate(String queries, String cfgName, List<String> endpoints) throws Exception {
		List<List<Object>> report = new ArrayList<List<Object>>();
		List<List<Object>> sstreport = new ArrayList<List<Object>>();
		Map<String, List<List<Object>>> result = new HashMap<String, List<List<Object>>>();
		result.put("report", report);
		result.put("sstreport", sstreport);

		List<String> qnames = Arrays.asList(queries.split(" "));
		if(queries.equals(""))
		{
			qnames = new ArrayList<>(qp.getQueries().keySet());
		}else {
			qnames = Arrays.asList(queries.split(" "));
		}
		for (String curQueryName : qnames)
		{
			List<Object> reportRow = new ArrayList<Object>();
			report.add(reportRow);
			String curQuery = qp.getQuery(curQueryName);
			reportRow.add(curQueryName);

			List<Object> sstReportRow = new ArrayList<Object>();
			sstreport.add(sstReportRow);
			sstReportRow.add(curQueryName);

			Config config = new Config(cfgName);

			////Relative Error///////////
			if(config.isRelativeError()) {
				RelativeError.config = config;
				RelativeError.setQuery_name(curQueryName);
				RelativeError.initializeFile(curQueryName,curQuery);
			}
			///////
			SailRepository repo = null;
			TupleQueryResult res = null;
			BufferedWriter writer=null;
			try {
				repo = FedXFactory.initializeSparqlFederation(config, endpoints);
//				log.setLevel(Level.OFF);
				TupleQuery query = repo.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, curQuery);
				idxCount = 0;
                long startTime = System.currentTimeMillis();

				res = query.evaluate();
				long count = 0;


				while (res.hasNext()) {
					BindingSet row = res.next();
					//System.out.println(count+": "+ row);
					count++;
				}
				System.out.println(query.toString());
                long runTime = System.currentTimeMillis() - startTime;
                if(config.isRelativeError()) {
					RelativeError.remainingTPError();
				}
				reportRow.add((Long)count); reportRow.add((Long)runTime);
				sstReportRow.add((Long)count);
				sstReportRow.add(QueryInfo.queryInfo.get().numSources.longValue());
				sstReportRow.add(QueryInfo.queryInfo.get().totalSources.longValue());
				log.info(curQueryName + ": Query exection time (msec): "+ runTime + ", Total Number of Records: " + count + ", Source count: " + QueryInfo.queryInfo.get().numSources.longValue());
				//log.info(curQueryName + ": Query exection time (msec): "+ runTime + ", Total Number of Records: " + count + ", Source Selection Time: " + QueryInfo.queryInfo.get().getSourceSelection().time);
			} catch (Throwable e) {
				e.printStackTrace();
				log.error("", e);
				File f = new File("results/" + curQueryName + ".error.txt");
				ByteArrayOutputStream os = new ByteArrayOutputStream();
				PrintStream ps = new PrintStream(os);
				e.printStackTrace(ps);
				ps.flush();
				FileUtils.write(f, os.toString("UTF8"));
				reportRow.add(null); reportRow.add(null);
			} finally {
				if (null != res) {
					res.close();
				}

				if (null != repo) {
					repo.shutDown();
				}
			}
			if(config.isRelativeError()) {
				RelativeError.writeSimilarityValue();
			}
		}
		return result;
	}

	static Map<String, List<List<Object>>> multyEvaluate(String queries, int num, String cfgName, List<String> endpoints, String query_Dir) throws Exception {
		QueryEvaluation qeval = new QueryEvaluation(query_Dir);

		Map<String, List<List<Object>>> result = null;
		for (int i = 0; i < num; ++i) {
			Map<String, List<List<Object>>> subReports = qeval.evaluate(queries, cfgName, endpoints);
			if (i == 0) {
				result = subReports;
			} else {
				//assert(report.size() == subReport.size());
				for (Map.Entry<String, List<List<Object>>> e : subReports.entrySet())
				{
					List<List<Object>> subReport = e.getValue();
					for (int j = 0; j < subReport.size(); ++j) {
						List<Object> subRow = subReport.get(j);
						List<Object> row = result.get(e.getKey()).get(j);
						row.add(subRow.get(2));
					}
				}
			}
		}

		return result;
	}
	
	static String printReport(List<List<Object>> report) {
		if (report.isEmpty()) return "";
		
		StringBuilder sb = new StringBuilder();
		sb.append("Query,#Results");
		
		List<Object> firstRow = report.get(0);
		for (int i = 2; i < firstRow.size(); ++i) {
			sb.append(",Sample #").append(i - 2);
		}
		sb.append("\n");
		for (List<Object> row : report) {
			for (int c = 0; c < row.size(); ++c) {
				sb.append(row.get(c));
				if (c != row.size() - 1) {
					sb.append(",");
				}
			}
			sb.append("\n");
		}
//        sb.append(SimilarityConstants.calculateAngleBetweenVector()+"\n");// = new

        return sb.toString();
	}


}