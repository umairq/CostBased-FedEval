package trunk.parallel.engine.exec.operator;

import java.util.Set;

import com.hp.hpl.jena.sparql.engine.binding.Binding;

import trunk.config.Config;
import trunk.graph.Edge;
import trunk.graph.Vertex;
import trunk.parallel.engine.exec.TripleExecution;

/**
 * Warp an edge, a vertex providing intermediate results and the join to execute this edge
 * @author xgfd
 *
 */
public abstract class EdgeOperator implements Runnable {
	Vertex start;
	Edge edge;
	TripleExecution te;
	Set<Binding> input = null;
	boolean finished = false;
	public EdgeOperator(Vertex s,Edge e) {
		this.start = s;
		this.edge = e;
		te = new TripleExecution(edge.getTriple());
	}
	
	@Override
	public void run() {
		if(Config.debug) {
			System.out.println(this);
		}
		exec();
	}
	
	abstract protected void exec();

	public void setInput(Set<Binding> bindings) {
		this.input = bindings;
	}
	
	public boolean isFinished() {
		return finished;
	}
	
	public Vertex getStartVertex() {
		return start;
	}
	
	public Edge getEdge() {
		return edge;
	}
	
}
