package trunk.parallel.engine;


import trunk.description.Statistics;
import trunk.graph.Vertex;
import trunk.parallel.engine.main.StageGen;


import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.sparql.ARQInternalErrorException;
import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.core.DatasetGraph;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.Plan;
import com.hp.hpl.jena.sparql.engine.QueryEngineFactory;
import com.hp.hpl.jena.sparql.engine.QueryEngineRegistry;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterRoot;
import com.hp.hpl.jena.sparql.engine.main.QC;
import com.hp.hpl.jena.sparql.engine.main.QueryEngineMain;
import com.hp.hpl.jena.sparql.engine.main.StageBuilder;
import com.hp.hpl.jena.sparql.engine.main.StageGenerator;
import com.hp.hpl.jena.sparql.util.Context;
import com.hp.hpl.jena.sparql.util.Symbol;

public class ParaEng extends QueryEngineMain {

	public ParaEng(Query query, DatasetGraph dataset, Binding input,
			Context context) {
		super(query, dataset, input, context);
	}

	static private QueryEngineFactory factory = new ParaEngineFactory();
	
	static public void register() {
		QueryEngineRegistry.addFactory(factory);
	}
	
    static public void unregister() {
    	QueryEngineRegistry.removeFactory(factory);
    }
    
    static public QueryEngineFactory getFactory() {
    	return factory;
    }
    
	@Override
    public QueryIterator eval(Op op, DatasetGraph dsg, Binding input, Context context)
    {
		Vertex.resetVertexPool();
		Symbol property = Symbol.create("config");
		Statistics config = (Statistics) context.get(property);
    	StageGenerator sg;
    	sg = new StageGen(config);
    	StageBuilder.setGenerator(context, sg) ;
        ExecutionContext execCxt = new ExecutionContext(context, dsg.getDefaultGraph(), dsg, QC.getFactory(context)) ;
        QueryIterator qIter1 = QueryIterRoot.create(input, execCxt) ;
        QueryIterator qIter = QC.execute(op, qIter1, execCxt) ;
        return qIter ;
    }
	
}


class ParaEngineFactory implements QueryEngineFactory	{
	
	public boolean accept(Query query, DatasetGraph dataset, Context context) {
		return true;
	}

    public Plan create(Query query, DatasetGraph dataset, Binding initial, Context context) {
        ParaEng engine = new ParaEng(query, dataset, initial, context) ;
        return engine.getPlan();
    }

    public boolean accept(Op op, DatasetGraph dataset, Context context) {   // Refuse to accept algebra expressions directly.
        return false ;
    }

    public Plan create(Op op, DatasetGraph dataset, Binding inputBinding, Context context) {   // Shodul notbe called because acceept/Op is false
        throw new ARQInternalErrorException("QueryEngine: factory called directly with an algebra expression") ;
    }
}