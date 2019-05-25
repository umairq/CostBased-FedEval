package org.aksw.simba.quetsal.configuration;

import org.aksw.simba.start.Utility;

import java.io.File;
import java.io.FileFilter;
import java.util.*;

public class EndpointProvider2 {
    List<String> endpoints = new ArrayList<>();

    public EndpointProvider2(String path) {
        File f = new File(path);
        if (!f.exists() || !f.isDirectory()) {
            throw new RuntimeException(String.format("%1$s is not a directory", f.getAbsolutePath()));
        }
        List<File> queries = Arrays.asList(f.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isFile();
            }

        }));
        for (File qfl : queries)
        {
            if(qfl.getName().startsWith("endpoints"))
            endpoints.addAll( Utility.readFileLines(qfl));
        }
    }
//
//    public String getEndpoint(String queryName) {
//        return endpoints.get(queryName);
//    }

//    public Map<String, String> getEndpoints() {
//        return endpoints;
//    }
    public List<String> getEndPoints(){
        return endpoints;
    }
}
