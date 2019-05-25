package trunk.parallel.engine.exec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import trunk.config.Config;
import trunk.description.RemoteService;
import trunk.parallel.engine.ParaConfig;

public class WorkerPool {
	private static Map<RemoteService,WorkerPool> pools = new HashMap<RemoteService,WorkerPool>();
	private List<QueryTask> tasks = new ArrayList<QueryTask>();
	private ThreadGroup workers;
	private boolean abort = false;
	
	private WorkerPool(RemoteService service) {
		workers = new ThreadGroup(service.getUrl());
		for(int i=0;i<ParaConfig.THREADPERSERVICE;i++) {
			Worker worker = new Worker(workers,workers.getName()+"-i");
			worker.start();
		}
	}
	
	static public WorkerPool getWorkerPool(RemoteService service) {
		synchronized (pools) {
			WorkerPool wp = pools.get(service);
			if(wp == null) {
				wp = new WorkerPool(service);
				pools.put(service, wp);
			}
			return wp;
		}
	}
	
	public void submit(QueryTask task) {
		synchronized (tasks) {
			tasks.add(task);
			tasks.notify();
		}
		if(Config.debug) {
			//System.out.println(service.getUrl()+" Active: "+activeworker);
		}
	}
	
	public void abort() {
		abort =true;
	}
	
	private class Worker extends Thread{
		
		public Worker(ThreadGroup workers, String name) {
			super(workers,name);
		}

		@Override
		public void run() {
			while(!abort) {
				try {
					QueryTask current = null;
					synchronized (tasks) {
						if(!tasks.isEmpty()) {
							current = tasks.remove(0);
						}
					}
					if(current != null) {
						current.runTask();
						current = null;
					}
					synchronized (tasks) {
						if(tasks.isEmpty()) {
							tasks.wait();
						}
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
