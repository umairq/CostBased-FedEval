package trunk.parallel.engine.exec.operator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.sparql.engine.binding.Binding;

import trunk.config.Config;
import trunk.graph.Edge;
import trunk.graph.Vertex;
import trunk.parallel.engine.error.RelativeError;
import trunk.parallel.engine.error.TripleCard;
import trunk.parallel.engine.main.BGPEval;
import trunk.stream.engine.util.QueryUtil;

public class BindJoin extends EdgeOperator {

	public BindJoin(Vertex s, Edge e) {
		super(s, e);
	}

	@Override
	protected void exec() {
		try {
			synchronized (start) {
				if(!start.isBound()) {
					start.wait();
				}
			}
		} catch (InterruptedException e) {
			//e.printStackTrace();
			if(Config.debug)
				System.out.println(this+ " is intrrupted");
		}
		
		Vertex end;
		if(start.equals(edge.getV1())) {
			end = edge.getV2();
		} else {
			end = edge.getV1();
		}
		List<Binding> intermediate = new ArrayList<Binding>(start.getBindings());
		Set<Binding> results = te.exec(intermediate);
        TripleCard tripleCard = new TripleCard();
		if(Config.relative_error){
            RelativeError.real_resultSize.put(start,(double)results.size());
//            tripleCard.real_Card = results.size();
//            tripleCard.estimated_card = edge.getTripleCount();
            RelativeError.addJoinCard(start,(double)results.size(),edge.getTriple(),null);
        }
		if(end.isBound()) {
			results = QueryUtil.join(results, end.getBindings());
		}
		
		results = QueryUtil.join(input, results);
		if(Config.debug) {
			System.out.println(edge+": "+results.size());

		}
        if(Config.relative_error ){
            RelativeError.real_resultSize.put(end,(double)results.size());
            RelativeError.addJoinCard(end,(double)results.size(),edge.getTriple(),null);
        }
        if(Config.relative_error){
//            RelativeError.writeQueryPlan("\n"+"Bind Join: "+ edge +":"+results.size());
        }
		if(results.size()==0) {
			finished = true;
			//System.out.println("SE shutdown");
			BGPEval.getCurrentExeService().shutdownNow();
		}
		end.setBindings(results);
		synchronized (end) {
			end.notifyAll();
		}
		start.removeEdge(edge);
		end.removeEdge(edge);
	}
	
	@Override
	public String toString() {
		return "Bind join: "+ start +"--"+edge;
	}
}
