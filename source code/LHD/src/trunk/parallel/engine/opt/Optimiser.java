package trunk.parallel.engine.opt;

import java.util.List;
import java.util.Set;

import trunk.graph.SimpleGraph;
import trunk.graph.Vertex;
import trunk.parallel.engine.exec.operator.EdgeOperator;

public abstract class Optimiser {
	SimpleGraph g;
	Optimiser(SimpleGraph g) {
		this.g = g;
	}
	
	public abstract List<EdgeOperator> nextStage();
	
	public abstract Set<Vertex> getRemainVertices();
}
