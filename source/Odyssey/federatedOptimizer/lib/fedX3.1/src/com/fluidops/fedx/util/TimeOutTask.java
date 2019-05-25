package com.fluidops.fedx.util;

import java.util.Timer;
import java.util.TimerTask;

public class TimeOutTask {

	public static void main(String[] args) {
		 final Timer timer = new Timer();
	    timer.schedule(new TimerTask() {
	        int n = 0;
	        @Override
	        public void run() {
	            System.out.println(n);
	          n++;
	          
	                timer.cancel();
	            
	        }
	    },10000,1000);
	}

}
