package trunk.parallel.engine.main;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import trunk.config.Config;
import trunk.description.Statistics;
import trunk.description.RemoteService;
import trunk.graph.Edge;
import trunk.graph.SimpleGraph;
import trunk.graph.Vertex;
import trunk.stream.engine.util.HypTriple;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.main.StageGenerator;
/*
* Interface for execution of a basic graph pattern. A StageGenerator is registered in the context of an query execution to be found and called by the StageBuilder.
* The StageGenerator is called repeated for a basic graph pattern with each possible bindings unused to instantiate variables where possible.
* Each call of a stage generator returns a QueryIterator of solutions to the pattern for each of the possibilities (bindings) from the input query iterator.
* Result bindings to a particular input binding should use that as their parent, to pick up the variable bounds for that particular input.

 * */
public class StageGen implements StageGenerator {

	protected Statistics config;
	boolean finished = false;
	
	public StageGen(Statistics config) {
		this.config = config;
	}
	
	@Override
	public QueryIterator execute(BasicPattern pattern, QueryIterator input,
			ExecutionContext execCxt) {
		List<Edge> edges = make (pattern.getList());
		SimpleGraph g = new SimpleGraph(edges);
		
		return new BGPEval(g,input);
	}
	
	protected List<Edge> make(Collection<Triple> triples) {
		List<Edge> edges = new ArrayList<Edge>();
		trunk.lhd.sourceSelectionTime=0;
		for(Triple t:triples){
			long startTime = System.currentTimeMillis();
			List<RemoteService> services = config.getServiceRegistry().getMatchingServices(t);
			trunk.lhd.sourceSelectionTime += System.currentTimeMillis()-startTime;
			trunk.lhd.tpSources += services.size();
				if(services.isEmpty()) {
				if(Config.debug)
					System.out.println("No service for "+t);
				finished = true;
			}
			Triple hypT = new HypTriple(t,services);
			Vertex v1 = Vertex.create(t.getSubject());
			if(t.getSubject().isConcrete()) {
				v1.setBindings(null);
			}
				
			Vertex v2 = Vertex.create(t.getObject());
			if(t.getObject().isConcrete()) {
				v2.setBindings(null);
			}
			
			Edge e = new Edge(v1,v2,hypT);
			v1.addEdge(e);
			v2.addEdge(e);
			int tripleCount = 0;
			int dstObj = 0;
			int dstSub = 0;
			for(RemoteService rs:services) {
				tripleCount += rs.getTriples(t);
				dstSub+= rs.getDistinctSubject(t);
				dstObj+= rs.getDistinctObject(t);
			}
			e.setTripleCount(tripleCount);
			e.setDistinctSubject(dstSub);
			e.setDistinctObject(dstObj);
			edges.add(e);
		}
		return edges;
	}

}
