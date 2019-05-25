package trunk.stream.engine.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import trunk.description.RemoteService;


import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecException;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.binding.BindingFactory;
import com.hp.hpl.jena.sparql.engine.binding.BindingMap;
import com.hp.hpl.jena.sparql.expr.E_Equals;
import com.hp.hpl.jena.sparql.expr.E_LogicalAnd;
import com.hp.hpl.jena.sparql.expr.E_LogicalOr;
import com.hp.hpl.jena.sparql.expr.Expr;
import com.hp.hpl.jena.sparql.expr.ExprVar;
import com.hp.hpl.jena.sparql.expr.nodevalue.NodeValueNode;
import com.hp.hpl.jena.sparql.syntax.ElementFilter;
import com.hp.hpl.jena.sparql.syntax.ElementGroup;

public class QueryUtil {

	public static Query buildQuery(Triple t, Binding pre) {
		Node s = replacewithBinding(t.getSubject(), pre);
		Node p = replacewithBinding(t.getPredicate(), pre);
		Node o = replacewithBinding(t.getObject(), pre);
		/*if(s.isConcrete() && o.isConcrete())
			System.out.println("WTF");*/
		Triple tri = new Triple(s,p,o);
		Query query = new Query();
		ElementGroup elg = new ElementGroup();
		elg.addTriplePattern(tri);
		query.setQueryResultStar(true);
		query.setQueryPattern(elg);
		return query;
	}
	
	public static Node replacewithBinding(Node node, Binding b) {
    	if(b == null)
    		return node;
    	
        if (node.isVariable() && b.contains(Var.alloc(node))) {
            Node n = b.get(Var.alloc(node));
            if (n== null) return node; // should not happen!
            if (n.isBlank()) {
                  // we do not support blank nodes
                throw new QueryExecException("Cannot handle Blank Nodes over different graphs.");
            } else {
                return n;
            }
        }
        return node;
    }
	
	public static ElementFilter buildFilter(ElementGroup elg, List<Binding> bindings) {
		List<Var> vars = new ArrayList<Var>();
		vars.addAll(elg.varsMentioned());
		
		Expr expr = null;
		for(Binding b:bindings) {
			Expr and = null;
			for(Var v:vars) {
				if(b.contains(v)) {
					ExprVar eVar = new ExprVar(v);
					NodeValueNode eVal = new NodeValueNode(b.get(v));
					E_Equals eq = new E_Equals(eVar,eVal);
					if(and == null)//is this the first result filter? can use true as the initial value of and to avoid the if clause
						and = eq;
					else
						and = new E_LogicalAnd(and,eq);//do the logical and with previous result filter
				}
			}
			
			if(and != null) {
				if(expr == null) 
					expr = and;
				else
					expr = new E_LogicalOr(expr,and);
			}
				
		}
		
		if(expr != null)
			return new ElementFilter(expr);
			
		return null;
	}
	
	public static Set<Binding> concatenate(Set<Binding> left, Set<Binding> right) {
		Set<Binding> results = new HashSet<Binding>();
		results.addAll(left);
		for(Binding l:left) {
			for(Binding r:right) {
				Binding temp = new BindingMap();
				temp.addAll(l);
				temp.addAll(r);
				results.add(temp);
			}
		}
		return results;
		
	}
	

	/**
	 * Used for pruning only
	 * @param left The original bindings
	 * @param right Pruned bindings
	 * @return
	 */
	public static Set<Binding> join_prune(Set<Binding> left, Set<Binding> right) {
		Set<Binding> results = new HashSet<Binding>();
		if(left.size() ==0 || right.size() == 0)
			return results;
		
		/*Binding lb = left.iterator().next();
		Binding rb = right.iterator().next();
		Binding temL = lb;
		Binding temR = rb;
		temL.
		//System.out.print(temL.size() +":"+temR.size()+"\n");
		if(temL.size() == temR.size()) {
			results.addAll(right);
			return results;
		}*/
		for(Binding l:left) {
			Iterator<Var> lvar = l.vars();
			List<Var> lvars = new ArrayList<Var>();
			while(lvar.hasNext())
				lvars.add(lvar.next());
			
			for(Binding r:right) {
				Iterator<Var> rvar = r.vars();
				List<Var> rvars = new ArrayList<Var>();
				while(rvar.hasNext())
					rvars.add(rvar.next());
				if(lvars.size() == rvars.size()) {//if left contains the same variables as right, then right is the result
					return right;
				}
				
				Binding b = join_prune(l,r);
				if(b != null)
					results.add(b);
			}
		}
		return results;
	}

	/**
	 * {@code r} contains only one variable
	 * @param l
	 * @param r
	 * @return
	 */
	private static Binding join_prune(Binding l, Binding r) {
		Iterator<Var> vars = r.vars();
		Var v = vars.next();
		if(!l.get(v).equals(r.get(v))) {
			return null;
		}
		return l;
	}
	
	
	public static Set<Binding> join(Set<Binding> left, Set<Binding> right) {
		
		
		if(left == null) {
			return right;
		}
		
		if(right == null) {
			return left;
		}
		
		Set<Binding> results = new HashSet<Binding>();
		if(left.size() ==0 || right.size() == 0)
			return results;
		
		if(left.equals(right)) {
			return left;
		}
		Set<Var> joinVars = getJoinVars(left,right);
		return hashJoin(left,right,joinVars);
	}
	
	static private Set<Var> getJoinVars(Collection<Binding> left,Collection<Binding> right) {
		if(left==null || right==null)
			return null;
		Set<Var> joinVars = new HashSet<Var>();
		Iterator<Var> l = left.iterator().next().vars();
		Binding r = right.iterator().next();
		while(l.hasNext()) {
			Var v = l.next();
			if(r.contains(v)) {
				joinVars.add(v);
			}
		}
		
		return joinVars;
	}
	
	static private Set<Binding> hashJoin(Collection<Binding> left,Collection<Binding> right,Set<Var> vars) {
		Set<Binding> results = new HashSet<Binding>();
		if(vars.isEmpty()) {
			//create cross product
			for(Binding l:left) {
				for(Binding r:right) {
					Binding joinBinding = new BindingMap();
					joinBinding.addAll(l);
					joinBinding.addAll(r);
					results.add(joinBinding);
				}
				
			}
			return results;
		}
		
		if(left.size() > right.size()) {
			Collection<Binding> temp = left;
			left = right;
			right = temp;
		}
		
		Map<Binding,List<Binding>> hashBindingMap = new HashMap<Binding,List<Binding>>();
		List<Var> joinVars = new ArrayList<Var>(vars);
		for(Binding l:left) {
			Binding joinBinding = new BindingMap();
			for(Var var:joinVars) {
				joinBinding.add(var, l.get(var));
			}
			if(hashBindingMap.get(joinBinding)==null) {
				hashBindingMap.put(joinBinding, new ArrayList<Binding>());
			}
			hashBindingMap.get(joinBinding).add(l);
		}
		
		for(Binding r:right) {
			Binding joinBinding = new BindingMap();
			for(Var var:joinVars) {
				joinBinding.add(var, r.get(var));
			}
			if(hashBindingMap.get(joinBinding) != null) {
				for(Binding l:hashBindingMap.get(joinBinding)) {
					Binding join = BindingFactory.create();
					join.addAll(l);
					Iterator<Var> iter = r.vars();
					while(iter.hasNext()) {
						Var v = iter.next();
						if(!joinBinding.contains(v)) {
							join.add(v, r.get(v));
						}
					}
					results.add(join);
				}
			}
		}
		return results;
	}
	
}
