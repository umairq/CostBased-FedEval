package org.semagrow.semagrow.org.aksw.simba.start.semagrow;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.BasicConfigurator;
//import org.apache.log4j.BasicConfigurator;
//import org.apache.log4j.Logger;
import org.openrdf.model.Graph;
import org.openrdf.model.impl.GraphImpl;
import org.openrdf.query.BindingSet;
import org.openrdf.query.Query;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.QueryResultHandlerException;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResultHandler;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.config.RepositoryConfig;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.config.RepositoryFactory;
import org.openrdf.repository.config.RepositoryImplConfig;
import org.openrdf.repository.config.RepositoryRegistry;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.StatementCollector;
import org.openrdf.sail.config.SailConfigException;
import org.semagrow.semagrow.org.aksw.simba.start.EndpointProvider;
import org.semagrow.semagrow.org.aksw.simba.start.QueryProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.semagrow.config.SemagrowRepositoryConfig;
import eu.semagrow.core.impl.error.CalculatePlanQuality;
import eu.semagrow.core.impl.error.WriteErrorData;
import eu.semagrow.repository.SemagrowRepository;


public class QueryEvaluation {
	protected static final Logger log = LoggerFactory.getLogger(QueryEvaluation.class);
	
	QueryProvider qp;
	public static boolean relative_error = true;
	EndpointProvider ep;
	private RepositoryFactory repoFactory;
	private SemagrowRepository repository;

	public QueryEvaluation(String path,String configFilePath) throws Exception {
		qp = new QueryProvider(path);
		SemagrowRepositoryConfig repoConfig = getConfig(configFilePath);
		
		repoFactory = RepositoryRegistry.getInstance().get(repoConfig.getType());
		repository = (SemagrowRepository) repoFactory.getRepository(repoConfig);
        repository.initialize();
        
//        // remove CSV and TSV format due to bug: literals are recognized as URIs if they contain a substring parsable as URI.
//        TupleQueryResultParserRegistry registry = TupleQueryResultParserRegistry.getInstance();
//        registry.remove(registry.get(TupleQueryResultFormat.CSV));
//        registry.remove(registry.get(TupleQueryResultFormat.TSV));
//
//        BooleanQueryResultParserRegistry booleanRegistry = BooleanQueryResultParserRegistry.getInstance();
//        booleanRegistry.remove(booleanRegistry.get(BooleanQueryResultFormat.JSON));
	}
	
	class SyncTupleQueryResultHandler implements TupleQueryResultHandler {
		long resultCount = 0;
		
		@Override
		public void endQueryResult() throws TupleQueryResultHandlerException {
			
		}

		@Override
		public void handleBoolean(boolean arg0) throws QueryResultHandlerException {
			
		}

		@Override
		public void handleLinks(List<String> arg0) throws QueryResultHandlerException {
			
		}

		@Override
		public void handleSolution(BindingSet arg0) throws TupleQueryResultHandlerException {
			resultCount++;
		}

		@Override
		public void startQueryResult(List<String> arg0) throws TupleQueryResultHandlerException {
			
		}
		
	}
	
	public List<List<Object>> evaluate(String queries,String relativeErrorFilePAth) throws Exception {
		List<List<Object>> report = new ArrayList<List<Object>>();
		//umair
		if(relative_error)
			WriteErrorData.pre_initfileRelativeError(relativeErrorFilePAth);
        List<String> qnames = null;
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
			//umair
			if(relative_error)
				WriteErrorData.initializeFile(curQuery, curQueryName);
			//ParsedOperation pO = QueryParserUtil.parseOperation(QueryLanguage.SPARQL, curQuery, null);
			RepositoryConnection repCon = this.repository.getConnection();
			try {
				Query tempq = repCon.prepareQuery(QueryLanguage.SPARQL, curQuery);
				TupleQuery q = (TupleQuery)tempq;
				
				SyncTupleQueryResultHandler rhandler = new SyncTupleQueryResultHandler();
				long startTime = System.currentTimeMillis();
				
				q.evaluate(rhandler);
	            long runTime = System.currentTimeMillis() - startTime;
				  
	            //umair
	            if(relative_error) {
				try {
					CalculatePlanQuality.writeToFile(curQueryName);
				} catch (Exception e) {
					e.printStackTrace();
				}
	            }
			    reportRow.add((Long)rhandler.resultCount); reportRow.add((Long)runTime);

			    log.info(curQueryName + ": Query exection time (msec): "+ runTime + ", Total Number of Records: " + rhandler.resultCount);
			} catch (Exception e) {
				reportRow.add(null); reportRow.add(null);
			} finally {
				repCon.close();
	        }
		}
		return report;
	}
	
    public void shutDown(){
        try {
            this.repository.shutDown();
            RepositoryRegistry.getInstance().remove(repoFactory);
        } catch (RepositoryException ex) {}
    }
    
    private SemagrowRepositoryConfig getConfig(String configFile) {

        try {
            if(configFile.equals("")){configFile="repo\\repositorysmall.ttl";}
            File file = FileUtils.getFile(configFile);
            Graph configGraph = parseConfig(file);
            RepositoryConfig repConf = RepositoryConfig.create(configGraph, null);
            repConf.validate();
            RepositoryImplConfig implConf = repConf.getRepositoryImplConfig();
            return (SemagrowRepositoryConfig)implConf;
        } catch (RepositoryConfigException e) {
            e.printStackTrace();
            return new SemagrowRepositoryConfig();
        } catch (SailConfigException | IOException | NullPointerException e) {
            e.printStackTrace();
            return new SemagrowRepositoryConfig();
        }
    }
    
    protected Graph parseConfig(File file) throws SailConfigException, IOException {

	    if(file==null){
	        throw new FileNotFoundException("file not found");
        }
        RDFFormat format = Rio.getParserFormatForFileName(file.getAbsolutePath());
        if (format==null)
            throw new SailConfigException("Unsupported file format: " + file.getAbsolutePath());
        RDFParser parser = Rio.createParser(format);
        Graph model = new GraphImpl();
        parser.setRDFHandler(new StatementCollector(model));
        InputStream stream = new FileInputStream(file);

        try {
            parser.parse(stream, file.getAbsolutePath());
        } catch (Exception e) {
            throw new SailConfigException("Error parsing file!");
        }

        stream.close();
        return model;
    }
    
	public static void main(String[] args) throws Exception {
		log.info("Start");
		File repfile = args.length > 0 ? new File(args[0]) : null;
        BasicConfigurator.configure();
		//List<List<Object>> report = multyEvaluate("S1 S2 S3 S4 S5 S6 S7 S8 S9 S10 S11 S12 S13 S14 C2 C3 C4 C7 C8 C10", 1);
        List<List<Object>> report = null;
        if(args.length < 2) {
            report = multyEvaluate("q1", 1,"queries","","");

        }else {
            if(args.length<5)
            {
                log.info("Please provide following input arguments \n" +
                        "1) path of result file \n" +
                        "2) path of queries folder \n" +
                        "3) path of relative error similarity folder \n" +
                        "4) config file path");
            }else {
            	relative_error= Boolean.parseBoolean(args[4]);
                report = multyEvaluate("", 1, args[1], args[2],args[3]);
            }
        }
        if(args.length>0) {
            String r = printReport(report);
            log.info(r);
            if (null != repfile) {
                FileUtils.write(repfile, r);
            }
        }

		System.exit(0);
	}

	static List<List<Object>> multyEvaluate(String queries, int num, String path, String relErrorFilePath, String configFilePath) throws Exception {
		QueryEvaluation qeval = new QueryEvaluation(path,configFilePath);
		try {
			List<List<Object>> report = null;
			for (int i = 0; i < num; ++i) {
				List<List<Object>> subReport = qeval.evaluate(queries,relErrorFilePath);
				if (i == 0) {
					report = subReport;
				} else {
					assert(report.size() == subReport.size());
					for (int j = 0; j < subReport.size(); ++j) {
						List<Object> subRow = subReport.get(j);
						List<Object> row = report.get(j);
						row.add(subRow.get(2));
					}
				}
			}
			
			return report;
		} finally {
			qeval.shutDown();
		}
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
			for (Object cell : row) {
				sb.append(cell);
				if (cell != row.get(row.size() - 1)) {
					sb.append(",");
				}
			}
			sb.append("\n");
		}
		return sb.toString();
	}
}
