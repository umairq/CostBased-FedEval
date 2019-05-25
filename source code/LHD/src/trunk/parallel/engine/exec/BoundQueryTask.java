package trunk.parallel.engine.exec;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import trunk.description.RemoteService;
import trunk.stream.engine.util.QueryUtil;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.sparql.syntax.Element;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.binding.BindingMap;
import com.hp.hpl.jena.sparql.syntax.ElementGroup;
import com.hp.hpl.jena.sparql.syntax.ElementTriplesBlock;
import com.hp.hpl.jena.sparql.syntax.ElementUnion;

public class BoundQueryTask extends QueryTask {

	private List<Binding> bindings;
	
	public BoundQueryTask(TripleExecution te, RemoteService service, List<Binding> bindings) {
		super(te, service);
		this.bindings = bindings;
	}
	
	@Override
	protected Query buildQuery() {
		Query query = new Query();
		ElementGroup elg = new ElementGroup();
		ElementUnion eu = new ElementUnion();
		for(int i=0;i<bindings.size();i++) {
			Binding binding = bindings.get(i);
			Node subject = QueryUtil.replacewithBinding(triple.getSubject(), binding);
			Node predicate = QueryUtil.replacewithBinding(triple.getPredicate(), binding);
			Node object = QueryUtil.replacewithBinding(triple.getObject(), binding);
			if(subject.isVariable()) {
				subject = Var.createVariable(subject.getName()+"_"+i);
			}
			
			if(predicate.isVariable()) {
				predicate = Var.createVariable(predicate.getName()+"_"+i);
			}
			
			if(object.isVariable()) {
				object = Var.createVariable(object.getName()+"_"+i);
			}
			
			Triple newtriple = new Triple(subject,predicate,object);
			ElementTriplesBlock tb = new ElementTriplesBlock();
			tb.addTriple(newtriple);
			eu.addElement(tb);
		}
		
		List<Element> elements = eu.getElements();
		
		if(elements.size() > 1) {
			elg.addElement(eu);
		}
		else {
			elg.addTriplePattern(((ElementTriplesBlock)elements.get(0)).getPattern().get(0));
		}
		query.setQueryPattern(elg);
		query.setQueryResultStar(true);
		query.setQuerySelectType();
		return query;
	}

	@Override
	protected Set<Binding> postProcess(ResultSet rs) {
		Set<Binding> results = new HashSet<Binding>();
		while(rs.hasNext()) {
			Binding binding = rs.nextBinding();
			/*if(!b.vars().hasNext())
				System.out.println(b);*/
			Var var = binding.vars().next();
			int ind = var.getName().lastIndexOf("_");
			String num = var.getName().substring(ind+1);
			String vname = var.getName().substring(0,ind);
			int index = Integer.parseInt(num);
			Binding extend = new BindingMap();//TODO do not use a new binding map
			extend.addAll(bindings.get(index));
			extend.add(Var.alloc(vname), binding.get(var));
			results.add(extend);
		}
		return results;
	}
}
