package trunk.parallel.engine.exec.operator;

import java.awt.*;
import java.util.Set;
import java.util.Timer;

import com.hp.hpl.jena.sparql.engine.binding.Binding;

import trunk.config.Config;
import trunk.graph.Edge;
import trunk.graph.Vertex;
import trunk.parallel.engine.error.RelativeError;
import trunk.parallel.engine.error.TripleCard;
import trunk.parallel.engine.main.BGPEval;
import trunk.stream.engine.util.QueryUtil;

public class HashJoin extends EdgeOperator {

	public HashJoin(Edge e) {
		super(null, e);
	}

	@Override
	protected void exec() {
		//it's possible that both vertices are not visited, but
		//it should not occur. To simplify the code we assume
		//at least one vertex is bound.
		Vertex end;
		Set<Binding> results = null;
		results = te.exec(null);
        TripleCard tripleCard = new TripleCard();
        if(Config.relative_error){
            tripleCard.real_Card = results.size();
            tripleCard.estimated_card = edge.estimatedCard();
        }
		//if both vertices are bound
		if(edge.getV1().isBound() && edge.getV2().isBound()) {
			start = edge.getV1();
			end = edge.getV2();
			results = QueryUtil.join(results, start.getBindings());
			results = QueryUtil.join(results, end.getBindings());
			results = QueryUtil.join(results, input);
			if(Config.debug) {
				System.out.println(edge+": "+results.size());

			}
            if(Config.relative_error){
                    RelativeError.real_resultSize.put(end,(double)results.size());
                    RelativeError.real_resultSize.put(start,(double)results.size());
                    RelativeError.addJoinCard(end,(double)results.size(),edge.getTriple(),tripleCard);
                    RelativeError.addJoinCard(start,(double)results.size(),edge.getTriple(),tripleCard);

            }
			if(results.size() == 0) {
				finished = true;
				//System.out.println("SE shutdown");
				BGPEval.getCurrentExeService().shutdownNow();
			}
			if(start.getNode().isConcrete()) {
				end.setBindings(results);
				synchronized (end) {
					end.notifyAll();
				}
			}
			else {
				start.setBindings(results);
				synchronized (start) {
					start.notifyAll();
				}
			}
			start.removeEdge(edge);
			end.removeEdge(edge);
			return;
		}
		//only one vertex is bound
		if(edge.getV1().isBound()) {
			start = edge.getV1();
			end = edge.getV2();
		} else {
			start = edge.getV2();
			end = edge.getV1();
		}
		results = QueryUtil.join(results, start.getBindings());

		results = QueryUtil.join(results, input);
		if(Config.debug) {
			System.out.println(edge+": "+results.size());

		}
        if(Config.relative_error){
//            RelativeError.writeQueryPlan("\n"+"Hash Join: "+ edge +":"+results.size());
        }
        if(Config.relative_error){
            RelativeError.real_resultSize.put(end,(double)results.size());
            RelativeError.real_resultSize.put(start,(double)results.size());
            RelativeError.addJoinCard(start,(double)results.size(),edge.getTriple(),tripleCard);

        }
		if(results.size() == 0) {
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
		return "Hash join: "+edge;
	}
}
