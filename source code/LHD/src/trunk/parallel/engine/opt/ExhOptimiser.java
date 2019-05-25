package trunk.parallel.engine.opt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import trunk.config.Config;
import trunk.graph.Edge;
import trunk.graph.SimpleGraph;
import trunk.graph.Vertex;
import trunk.parallel.engine.ParaConfig;
import trunk.parallel.engine.error.RelativeError;
import trunk.parallel.engine.exec.operator.BindJoin;
import trunk.parallel.engine.exec.operator.EdgeOperator;
import trunk.parallel.engine.exec.operator.HashJoin;

public class ExhOptimiser extends Optimiser {

	private Set<EdgeOperator> optimal;

	public ExhOptimiser(SimpleGraph g) {
		super(g);
		
		long start = System.nanoTime();
		initialOptimal();
		long end = System.nanoTime();
		if(ParaConfig.debug) {
			Long inter = end-start;
			System.out.println("Optimisation: "+inter.doubleValue()/1000000000);
		}
	}
	
	@Override
	public List<EdgeOperator> nextStage() {
		List<EdgeOperator> operators = null;
		if(optimal !=null) {
			operators = new ArrayList<EdgeOperator>(optimal);
			optimal = null;
		}
		return operators;
	}
	
	@Override
	public Set<Vertex> getRemainVertices() {
		Set<Vertex> remains = new HashSet<Vertex>();
		for(Vertex v:g.getVertices()) {
			if(!v.isConsumed()) {
				remains.add(v);
			}
		}
		return remains;
	}
	
	private void initialOptimal() {
		//optimal contains the sequence of the execution of edges. 
		//each element (a List<EdgeOperator>) is the edges to be executed
		//in parallel, the index of the element indicates the order of the execution
		//of these edges.
		List<Edge> edges = g.getEdges();
		optimal = getFirstStage(edges);
		if(!edges.isEmpty()) {
			Iterator<List<Edge>> orders = getPermutations(edges);
			Set<Set<EdgeOperator>> candidates;
			candidates = getOptimal(optimal,orders);
			optimal = finalize(optimal,candidates);
		}
	}
	
	/**
	 * Find out all edges having concrete node. All these edges will be executed first.
	 * @param edges
	 * @return
	 */
	private Set<EdgeOperator> getFirstStage(List<Edge> edges) {
		Set<EdgeOperator> firststage = new HashSet<EdgeOperator>();
		List<Edge> toberemove = new ArrayList<Edge>();
		for(Edge e:edges) {
			Edge se = (Edge) e;
			if(se.getV1().getNode().isConcrete() || se.getV2().getNode().isConcrete()) {
				firststage.add(new HashJoin(se));
				toberemove.add(e);
                if(Config.relative_error) {
                    RelativeError.est_resultSize.put(se.getV1(),  se.estimatedCard());
                    RelativeError.est_resultSize.put(se.getV2(), se.estimatedCard());
                }
			}

//            if(Config.relative_error){
//                RelativeError.est_resultSize.put(se.getV1(), (int)se.estimatedCard());
//                RelativeError.est_resultSize.put(se.getV2(), (int)se.estimatedCard());
//                if(se.getV1().getNode().isConcrete()) {
//                    RelativeError.est_resultSize.put(se.getV1(), 1);
//                    //RelativeError.est_resultSize.put(se.getV2(), 1);
//                }
//                else  {
//                    RelativeError.est_resultSize.put(se.getV1(), se.getTripleCount()/(se.getDistinctObject()!=0?se.getDistinctObject():1));
//                }
//                if (se.getV2().getNode().isConcrete()){
//                    RelativeError.est_resultSize.put(se.getV2(), 1);
//                    //RelativeError.est_resultSize.put(se.getV1(), 1);
//                }else {
//                    RelativeError.est_resultSize.put(se.getV2(), se.getTripleCount()/(se.getDistinctSubject()!=0?se.getDistinctSubject():1));
//                }
//            }
			
			if(!se.getV1().isConsumed()) {
				firststage.add(new BindJoin(se.getV1(),se));
				toberemove.add(e);
                if(Config.relative_error) {
                    RelativeError.est_resultSize.put(se.getV1(),  se.estimatedCard());
                    RelativeError.est_resultSize.put(se.getV2(),  se.estimatedCard());
                }

			} else if(!se.getV2().isConsumed()) {
				firststage.add(new BindJoin(se.getV2(),se));
				toberemove.add(e);
                if(Config.relative_error) {
                    RelativeError.est_resultSize.put(se.getV1(),  se.estimatedCard());
                    RelativeError.est_resultSize.put(se.getV2(),  se.estimatedCard());
                }
			}
		}
		edges.removeAll(toberemove);
		return firststage;
	}

	private Set<Set<EdgeOperator>> getOptimal(Set<EdgeOperator> firststage, Iterator<List<Edge>> orders) {
		Map<Vertex,Double> resultsize_ = new HashMap<Vertex,Double>();
		for(EdgeOperator eo:firststage) {
			Edge edge = eo.getEdge();
			Vertex v1 = edge.getV1();
			Vertex v2 = edge.getV2();
			if(v1.getNode().isConcrete()) {
				resultsize_.put(v1, 1.0);
				resultsize_.put(v2, (double)edge.getTripleCount()/edge.getDistinctSubject());
			}
			else {
				resultsize_.put(v2, 1.0);
				resultsize_.put(v1, (double)edge.getTripleCount()/edge.getDistinctObject());
			}
		}

		if(Config.relative_error && resultsize_.size()>0) {
        //    RelativeError.est_resultSize.clear();
            RelativeError.est_resultSize.putAll(resultsize_);
        }
		double cost = Double.MAX_VALUE;
		Set<Set<EdgeOperator>> candidates = new HashSet<Set<EdgeOperator>>();
		while(orders.hasNext()) {
			List<Edge> order = orders.next();
			//for a list of edges generate the corresponding list of operator
			//that has the minimum cost. That is, for each edge, choose either
			//hash join or bind join that has lower cost
			Set<EdgeOperator> plan = new HashSet<EdgeOperator>();
			//record bound vertices and their number of bindings
			Map<Vertex,Double> resultsize = new HashMap<Vertex,Double>(resultsize_);
			double temp = 0;
			for(Edge e:order) {
				Edge edge = (Edge) e;
				Vertex v1 = edge.getV1();
				Vertex v2 = edge.getV2();
				//add all concrete vertices and their binding number (1) to the record
				if(v1.getNode().isConcrete()) {
					resultsize.put(v1, 1.0);
				}
				if(v2.getNode().isConcrete()) {
					resultsize.put(v2, 1.0);
				}

				Double size1 = resultsize.get(v1);
				Double size2 = resultsize.get(v2);
				double nj = 1*CostModel.CQ + edge.getTripleCount()*CostModel.CT;
				if(size1 == null && size2 == null) {//if this is the first edge, use HashJoin.
					temp+=nj;
					plan.add(new HashJoin(edge));
					resultsize.put(v1, (double)edge.getTripleCount());
					resultsize.put(v2, (double)edge.getTripleCount());
				} else {
					size1 = size1 == null?edge.getDistinctSubject():size1;
					size2 = size2 == null?edge.getDistinctObject():size2;
					double ssel = 1d/edge.getDistinctSubject();
					double osel = 1d/edge.getDistinctObject();
					int bindingsize = (int) Math.max(size1*ssel*edge.getTripleCount()*size2*osel, 2);
					if(size1 <= size2) {
						//if the minimum vertex is concrete (and a concrete vertex should always
						//be the minimum vertex since its binding size is 1), use hash join
						if(v1.getNode().isConcrete()) {
							temp+=nj;
							plan.add(new HashJoin(edge));
							resultsize.put(v2, (double)bindingsize);
							continue;
						}
						
						double bj = size1*CostModel.CQ + size1*ssel*edge.getTripleCount();
						if(nj>bj) {
							temp+=bj;
							plan.add(new BindJoin(v1,edge));
						} else {
							temp+=nj;
							plan.add(new HashJoin(edge));
						}
						//either join should return the same number of results
						resultsize.put(v2, (double)bindingsize);
					} else {//size2<size1
						if(v2.getNode().isConcrete()) {
							temp+=nj;
							plan.add(new HashJoin(edge));
							resultsize.put(v1, (double)bindingsize);
							continue;
						}
						
						double bj = size2*CostModel.CQ + size2*osel*edge.getTripleCount();
						if(nj>bj) {
							temp+=bj;
							plan.add(new BindJoin(v2,edge));
						} else {
							temp+=nj;
							plan.add(new HashJoin(edge));
						}
						resultsize.put(v1, (double)bindingsize);
					}
				}
			}
			if(temp<cost) {
				cost = temp;
				candidates.clear();
				candidates.add(plan);
                RelativeError.est_resultSize.putAll(resultsize);
			}
			if(temp == cost) {
				candidates.add(plan);
                RelativeError.est_resultSize.putAll(resultsize);
			}
		}
		return candidates;
	}

	private Set<EdgeOperator> finalize(Set<EdgeOperator> firststage, Set<Set<EdgeOperator>> plans) {
		Set<EdgeOperator> optimal = null;
		int count = 0;
		for(Set<EdgeOperator> plan:plans) {
			/*List<List<EdgeOperator>> temp;
			temp = parallelise(firststage,plan);
			//keep the plan with minimum depth, which means minimum
			//steps are needed to execute a query
			if(optimal == null) {
				optimal = temp;
			} else
			if(temp.size()<optimal.size()) {
				optimal = temp;
			}*/
			int _count = 0;
			for(EdgeOperator eo:plan) {
				if(eo instanceof HashJoin) {
					_count++;
				}
			}
			if(_count>count) {
				count = _count;
				plan.addAll(firststage);
				optimal = plan;
			}
		}
		if(optimal == null) {
			optimal = plans.iterator().next();
			optimal.addAll(firststage);
		}
		return optimal;
	}

	/*private List<List<EdgeOperator>> parallelise(List<List<EdgeOperator>> firststage, List<EdgeOperator> plan) {
		List<List<EdgeOperator>> prllplan = new ArrayList<List<EdgeOperator>>();
		prllplan.add(new ArrayList<EdgeOperator>(firststage.get(0)));
		List<Set<Vertex>> dependency = new ArrayList<Set<Vertex>>();
		//initial dependency
		dependency.add(new HashSet<Vertex>());
		for(EdgeOperator eo:prllplan.get(0)) {
			dependency.get(0).add(eo.getEdge().getV1());
			dependency.get(0).add(eo.getEdge().getV2());
		}
		
		
		for(EdgeOperator eo:plan) {
			Vertex start = eo.getStartVertex();
			if(start == null) {//no dependency needed
				prllplan.get(0).add(eo);
				dependency.get(0).add(eo.getEdge().getV1());
				dependency.get(0).add(eo.getEdge().getV2());
			} else {//check dependency
				for(int i=0;i<dependency.size();i++) {
					
					if(dependency.get(i).contains(start)) {//if the start vertex has been bound
						try {
							prllplan.get(i+1).add(eo);
							dependency.get(i+1).add(eo.getEdge().getV1());
							dependency.get(i+1).add(eo.getEdge().getV2());
						}
						catch (Exception e) {
							prllplan.add(new ArrayList<EdgeOperator>());
							dependency.add(new HashSet<Vertex>());
							prllplan.get(i+1).add(eo);
							dependency.get(i+1).add(eo.getEdge().getV1());
							dependency.get(i+1).add(eo.getEdge().getV2());
						}
						break;
					}//try the next plan_num
				}
			}
		}
		return prllplan;
	}*/
	
	
	/**
	 * Generate the permutations of the given set of objects 
	 * @param items the set of objects
	 * @return a list of all permutations
	 */
	private <T> Iterator<List<T>> getPermutations(Collection<T> items) {
		
		/*//Recursive approach
		List<List<T>> results = new ArrayList<List<T>>();
		if(items.size() == 1) {
			List<T> temp = new ArrayList<T>();
			temp.add(items.iterator().next());
			results.add(temp);
			return results;
		}
		for(T current:items) {
			Set<T> next = new HashSet<T>();
			next.addAll(items);
			next.remove(current);
			List<List<T>> subResults = getPermutations(next);
			for(List<T> solution:subResults) {
				solution.add(0, current);
				results.add(solution);
			}
		}
		return results;*/
		return new Permutor<T>(items);
	}
	
	/**
	 * An iterative approach
	 * @param plans
	 * @param items
	 * @return
	 *//*
	private <T> List<List<T>> getPermutations(List<List<T>> plans, Collection<T> items) {
		if(plans.get(0).size() == items.size())
			return plans;
		List<List<T>> partial = new ArrayList<List<T>>();
		for(List<T> plan:plans) {
			for(T item:items) {
				if(plan.contains(item))
					continue;
				List<T> temp = new ArrayList<T>();
				temp.addAll(plan);
				temp.add(item);
				partial.add(temp);
			}
		}
			
		return getPermutations(partial,items);
	}*/
}


class Permutor<T> implements Iterator<List<T>> {

	private Iterator<List<T>> iter = null;
	private Iterator<T> self = null;
	private Collection<T> items;
	private T current = null;
	
	public Permutor(Collection<T> items) {
		this.items = items;
		self = items.iterator();
	}
	
	@Override
	public boolean hasNext() {
		if(iter == null) {
			return self.hasNext();
		}
		if(iter.hasNext() || self.hasNext()) {
			return true;
		}
		return false;
	}

	@Override
	public List<T> next() {
		List<T> next;
		if(items.size() == 1) {
			next = new ArrayList<T>();
			next.add(self.next());
			return next;
		}
		while(true) {
			if(current == null) {
			current = self.next();
			List<T> temp = new ArrayList<T>(items);
			temp.remove(current);
				iter = new Permutor<T>(temp);
			}
			if(!iter.hasNext()) {
				current = null;
				continue;
			}
			next = iter.next();
			next.add(0,current);
			return next;
			}
	}

	@Override
	public void remove() {
		
	}
	
}
