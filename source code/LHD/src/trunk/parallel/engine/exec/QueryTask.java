package trunk.parallel.engine.exec;

import java.util.HashSet;
import java.util.Set;

import trunk.config.Config;
import trunk.description.RemoteService;
import trunk.stream.engine.util.Logging;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.sparql.ARQException;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import com.hp.hpl.jena.sparql.engine.http.QueryExceptionHTTP;
import com.hp.hpl.jena.sparql.resultset.ResultSetMem;
import com.hp.hpl.jena.sparql.syntax.ElementGroup;

public class QueryTask {
	boolean finished = false;
	protected Triple triple;
	private TripleExecution issuer;
	private RemoteService service;
	private Set<Binding> bindings;
	private int timeout = 0;
	private int MAXTRIES = 3;
	
	public QueryTask(TripleExecution te, RemoteService service) {
		this.triple = te.getTriple();
		this.service = service;
		this.issuer = te;
	}
	
	public QueryTask(Triple triple, RemoteService service, int timeout) {
		this.triple = triple;
		this.service = service;
		this.timeout = timeout;
	}
	
	public void runTask() {
		Query q = buildQuery();
		ResultSet rs = execQuery(q);
		bindings = postProcess(rs);
		finished = true;
		issuer.addBindings(bindings);
		synchronized (issuer) {
			try {
				issuer.taskMinus();
				if(issuer.isFinished()) {
					issuer.notify();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public Set<Binding> getResults() {
		return bindings;
	}
	
	public boolean isFinished() {
		return finished;
	}
	
	protected Query buildQuery() {
		Query query = new Query();
		ElementGroup elg = new ElementGroup();
		elg.addTriplePattern(triple);
		query.setQueryResultStar(true);
		query.setQueryPattern(elg);
		query.setQuerySelectType();
		return query;
	}
	
	private ResultSet execQuery(Query query) {
		ResultSet result = null;
		boolean error = false;
		String url = service.getUrl();
		QueryEngineHTTP qexec = new QueryEngineHTTP(url,query);
		if(Config.log)
			Logging.out();//record the number of outgoing requests
		qexec.addParam("timeout", String.valueOf(timeout));
		int n = 0;
		while(n < MAXTRIES) {
			try {
				error = false;
				if(query.isSelectType()) {
					result =new ResultSetMem(qexec.execSelect());
					if(Config.log)
						Logging.in(((ResultSetMem)result).size());
				}
				else
					throw new ARQException("Wrong query q_name.");
				break;
			} catch (QueryExceptionHTTP e) {
				//System.out.println(query);
				error = true;
				try {
					n++;
					Thread.sleep(200 * n);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}
		
		if(error) {
			result =new ResultSetMem();
			System.err.println("Error:\nremote service: " + url +"\nquery:\n" + query);
		}
		qexec.close();
		return result;
	}
	
	protected Set<Binding> postProcess(ResultSet rs) {
		Set<Binding> results = new HashSet<Binding>();
		while(rs.hasNext()) {
			results.add(rs.nextBinding());
		}
		return results;
	}
}
