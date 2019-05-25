package trunk.parallel.engine.main;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.openjena.atlas.io.IndentedWriter;

import trunk.config.Config;
import trunk.graph.SimpleGraph;
import trunk.graph.Vertex;
import trunk.parallel.engine.error.RelativeError;
import trunk.parallel.engine.exec.operator.EdgeOperator;
import trunk.parallel.engine.opt.ExhOptimiser;
import trunk.parallel.engine.opt.Optimiser;
import trunk.stream.engine.util.QueryUtil;

import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterRoot;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIteratorBase;
import com.hp.hpl.jena.sparql.serializer.SerializationContext;

public class BGPEval extends QueryIteratorBase {

	private SimpleGraph g;
	private Set<Binding> input = null;
	private Iterator<Binding> results;
	private static ExecutorService es;
	
	public BGPEval(SimpleGraph g, QueryIterator input) {
		this.g = g;
		if(! (input instanceof QueryIterRoot)) {
			if(Config.debug) {
				System.out.println("Not QueryIterRoot");
			}
			this.input = new HashSet<Binding>();
			while(input.hasNext()){
				this.input.add(input.next());
			}
		}
	}
	
	@Override
	public void output(IndentedWriter out, SerializationContext sCxt) {
		
	}

	@Override
	protected boolean hasNextBinding() {
		if(results == null)
			execBGP();
		return results.hasNext();
	}

	static public ExecutorService getCurrentExeService() {
		return es;
	}
	
	private void execBGP() {
		Optimiser opt = new ExhOptimiser(g);
		//Optimiser opt = new GrdyOptimiser(g);
		List<EdgeOperator> operators = opt.nextStage();
		while(operators!=null) {
			es = Executors.newCachedThreadPool();
			for(EdgeOperator eo:operators) {
				//consume the input bindings
				eo.setInput(input);
				input = null;
				try {
					es.submit(eo);
				} catch (RejectedExecutionException e) {
					if(Config.debug)
						System.out.println("Query execution is terminated.");
				}
			}
			es.shutdown();
			while (!es.isTerminated()) {
				try {
					es.awaitTermination(3600, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					System.out.println("Query execution interrupted.");
				} catch (IllegalMonitorStateException e) {
					System.out.println("IllegalMonitorStateException");
				}
			}
			
			if(Config.debug)
				System.out.println("********************");

			if(Config.relative_error) {
			    RelativeError.addEstimatedCardBGP(RelativeError.est_resultSize);
                RelativeError.print();
                RelativeError.add_plan_joincards();
                RelativeError.est_resultSize.clear();
                RelativeError.real_resultSize.clear();
            }
			
			for(EdgeOperator eo:operators) {
				if(eo.isFinished()) {
					if(Config.debug) {
						System.out.println("Query is finished by "+eo.getEdge());
					}
					results = new ArrayList<Binding>().iterator();
					return;
				}
			}
			operators = opt.nextStage();
		}
		
		//find out vertices with bindings that have not been used (joined)
		Set<Binding> bindings = null;
		for(Vertex v:opt.getRemainVertices()) {
			if(Config.debug) {
				System.out.println("Remaining vertex: "+v);
			}
			bindings = QueryUtil.join(bindings, v.getBindings());
		}
		results = bindings.iterator();
	}

	@Override
	protected Binding moveToNextBinding() {
		return results.next();
	}

	@Override
	protected void closeIterator() {
		g.getEdges().clear();
		results = null;
	}

}
