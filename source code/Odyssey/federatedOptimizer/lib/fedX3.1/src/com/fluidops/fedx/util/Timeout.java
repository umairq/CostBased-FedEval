package com.fluidops.fedx.util;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;

public class Timeout {
   
    public static void main(String[] args){
    	//final Duration timeout = Duration.ofSeconds(1);
    	ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    	final Future<Object> handler = executor.submit(new Callable<Object>() {
    	    @Override
    	    public String call() throws Exception {
    	        return requestDataFromModem();
    	    }

			private String requestDataFromModem() throws InterruptedException {
				for(int i=1;i>0;i++){
				System.out.println("hello");
				}
				return null;
			}
    	});

    	executor.schedule(new Runnable() {
    	    @Override
    	    public void run(){
    	        handler.cancel(true);
    	    }      
    	}, 2000, TimeUnit.MILLISECONDS);

    	executor.shutdownNow();
    	
    }
   
    
}