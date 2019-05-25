package trunk.parallel.engine;

import trunk.config.Config;

public class ParaConfig extends Config{
	
	//the block size of union-bind-join i.e. the number of bindings sent out per request,at least 2
	public static int blocksize = 40;
	//the timeout of each sub-queries in milliseconds. may have no effect depending on triple store
	public static int timeout = 10000;
	//the max number of requests permitted per service
	public static int THREADPERSERVICE = 10;
}
