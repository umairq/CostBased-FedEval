package trunk.parallel.engine.exec;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import trunk.config.Config;
import trunk.description.RemoteService;
import trunk.parallel.engine.ParaConfig;
import trunk.stream.engine.util.HypTriple;
import com.hp.hpl.jena.sparql.engine.binding.Binding;

public class TripleExecution {

	private HypTriple triple;
	private Set<Binding> results = new HashSet<Binding>();
	private Integer taskcounter = 0;
	
	public TripleExecution(HypTriple triple) {
		this.triple = triple;
	}
	
	public void addBindings(Collection<Binding> bindings) {
		synchronized (results) {
			results.addAll(bindings);
		}
	}
	
	public boolean isFinished() {
		synchronized (taskcounter) {
			if(taskcounter == 0) {
				return true;
			}
			else
				return false;
		}
	}
	
	public void taskMinus() {
		synchronized (taskcounter) {
			taskcounter--;
		}
	}
	
	public HypTriple getTriple() {
		return triple;
	}
	
	public Set<Binding> exec(List<Binding> intermediate) {
		List<RemoteService> services = triple.getServices();
		
		if(intermediate == null) {
			taskcounter = services.size();
			for(RemoteService rs:services) {
				WorkerPool pool = WorkerPool.getWorkerPool(rs);
				//addTask(pool,new QueryTask(this,rs));
				pool.submit(new QueryTask(this,rs));
			}
		}
		else {
			if(intermediate.isEmpty()) {
				return new HashSet<Binding>();
			}
			
			int interval = ParaConfig.blocksize;
			taskcounter = (int) (services.size()*(Math.ceil((double)intermediate.size()/interval)));
			int begin = 0;
			do {
				int end = begin + interval;
				end = Math.min(end, intermediate.size());
				List<Binding> sublist = intermediate.subList(begin, end);
				for(RemoteService rs:services) {
					WorkerPool pool = WorkerPool.getWorkerPool(rs);
					pool.submit(new BoundQueryTask(this,rs,sublist));
				}
				begin = end;
			}while(begin < intermediate.size());
		}
		try {
			synchronized (this) {
				this.wait();
			}
		} catch (InterruptedException e) {
			//e.printStackTrace();
			if(Config.debug)
				System.out.println(this+" is intrrupted.");
		}
		
		return results;
	}
	
	@Override
	public String toString() {
		return triple.toString();
	}
	
}
