package com.yahoo.ycsb.workloads;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.yahoo.ycsb.DB;
import com.yahoo.ycsb.WorkloadException;
import com.yahoo.ycsb.generator.UniformIntegerGenerator;

public class BulkloadWorkload extends CoreWorkload {
    
    private static final String FS_NAME = "fs.default.name";
    
    private static final String MAP_REDUCE = "mapred.job.tracker";
    
    private static final String ZOOKEEPER = "hbase.zookeeper.quorum";
    
    private static final String BULK = "bulk_load";
    private static final String BULK_DEFAULT = "false";
    
    private static final String IN_PATH = "in_path";
    private static final String OUT_PATH = "out_path";
    
    private static final String START_KEY = "start_key";
    private static final String END_KEY = "end_key";
    private static final String RANGE_SIZE = "range_size";
    
    private static String fs_name;
    private static String map_reduce;
    private static String zookeeper;
    
    private static String in_path;
    private static String out_path;
    
    private static String start_key;
    private static String end_key;
    private static String range_size;
    
    private static boolean do_bulk = false;
    
    public void init(Properties p) throws WorkloadException
    {
        super.init(p);
        
        fs_name = p.getProperty(FS_NAME, null);
        map_reduce = p.getProperty(MAP_REDUCE, null);
        zookeeper = p.getProperty(ZOOKEEPER, null);
        
        do_bulk = Boolean.parseBoolean(p.getProperty(BULK, BULK_DEFAULT));
        
        in_path = p.getProperty(IN_PATH, null);
        out_path = p.getProperty(OUT_PATH, null);
        
	start_key = p.getProperty(START_KEY, null);
	end_key = p.getProperty(END_KEY, null);
	range_size = p.getProperty(RANGE_SIZE, null);
        
        if (do_bulk && (fs_name == null || map_reduce == null || zookeeper == null)) {
            System.out.println("No enough parameters for bulk loading!");
            return;
        }
        
    }
    
    @Override
    public boolean doBulkload(DB db, Object threadstate) {
        
	String params[];
	if (in_path == null || out_path == null) {
		params = new String[3];
	} else if (start_key == null || end_key == null){
		params = new String[5];
	} else if (range_size == null) {
		params = new String[7];
	} else {
		params = new String[8];
	}
        params[0] = fs_name;
        params[1] = map_reduce;
        params[2] = zookeeper;
        
        if (in_path != null && out_path != null) {
            params[3] = in_path;
            params[4] = out_path;
	} 
	if (start_key != null && end_key != null) {
	    params[5] = start_key;
	    params[6] = end_key;
	} 
	if (range_size != null) {
	    params[7] = range_size;
	}
        
        db.bulkload(table, params);
        
        return true;
    }
}
