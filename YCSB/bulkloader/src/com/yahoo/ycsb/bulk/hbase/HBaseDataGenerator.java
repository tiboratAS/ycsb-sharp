package com.yahoo.ycsb.bulk.hbase;

import java.io.BufferedOutputStream;
import java.io.IOException;  
import java.io.PrintStream;

import org.apache.hadoop.conf.Configuration;  
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;  
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;  
import org.apache.hadoop.hbase.client.Put;  
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;  
import org.apache.hadoop.hbase.mapreduce.HFileOutputFormat;  
import org.apache.hadoop.hbase.util.Bytes;  
import org.apache.hadoop.io.LongWritable;  
import org.apache.hadoop.io.Text;  
import org.apache.hadoop.mapreduce.Job;  
import org.apache.hadoop.mapreduce.Mapper;  
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;  
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;  
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat; 
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class HBaseDataGenerator extends Configured implements Tool{
    
    private static final String RANGE_SIZE = "range_size";
    private static final long RANGE_SIZE_DEFAULT = 1;
    
    private static final String FAMILY_NAME = "family";
    private static final String FAMILY_NAME_DEFAULT = "family";
    
    private static final String ROW_PREFIX = "row_prefix";
    private static final String ROW_PREFIX_DEFAULT = "user";
    
    private static final String DATA_SIZE = "data_size";
    private static final int DATA_SIZE_DEFAULT = 100;
    
    private static final String COLUMN_NAME = "column_name";
    private static final String COLUMN_NAME_DEFAULT = "column";
    
    private static final String INPUT_PATH = "input_path";
    private static final String INPUT_PATH_DEFAULT = "input";
    
    private static final String OUTPUT_PATH = "output_path";
    private static final String OUTPUT_PATH_DEFAULT = "output";
    
    private static final String WORK_DIR = "work_dir";
    private static final String WORK_DIR_DEFAULT = "work_dir/";
    
    private static final String START_KEY = "start_key";
    private static final long START_KEY_DEFAULT = 0;
    
    private static final String END_KEY = "end_key";
    private static final long END_KEY_DEFAULT = 10000;
    
    private static final String TABLE_NAME = "table_name";
    private static final String TABLE_NAME_DEFAULT = "usertable";
    
    private static final String FS_NAME = "fs.default.name";
    
    private static final String MAP_REDUCE = "mapred.job.tracker";
    
    private static final String ZOOKEEPER = "hbase.zookeeper.quorum";
    
    public HBaseDataGenerator(){};
    
    public HBaseDataGenerator(Configuration conf) {
        super(conf);
    }
    
    public static class HBaseMap extends Mapper<LongWritable, Text, ImmutableBytesWritable, Put> {       
  
        private static long range_size;
        private static String family_name;
        private static String row_prefix;
        private static int data_size;
        private static String column_name;
        private static long end;
        
        protected void setup( Context context )
                throws IOException, InterruptedException {
            super.setup( context );
            Configuration conf = context.getConfiguration();
            
            range_size = conf.getLong(RANGE_SIZE, RANGE_SIZE_DEFAULT);
            
            family_name = conf.get(FAMILY_NAME, FAMILY_NAME_DEFAULT);
            row_prefix = conf.get(ROW_PREFIX, ROW_PREFIX_DEFAULT);
            data_size = conf.getInt(DATA_SIZE, DATA_SIZE_DEFAULT);
            column_name = conf.get(COLUMN_NAME, COLUMN_NAME_DEFAULT);
            end = conf.getLong(END_KEY, END_KEY_DEFAULT);
            
        }

        protected void cleanup( Context context )
            throws IOException, InterruptedException {
        }
        
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {  
            
            String line = value.toString();       
            long start_key = Long.parseLong(line);
            long end_key = start_key + range_size;
            end_key = end_key > end?end:end_key;
            
            DataGenerator gen = new DataGenerator(row_prefix, start_key, end_key, false);
            ValueGenerator vgen = new ValueGenerator(data_size, true, System.currentTimeMillis());
            Text out_key;
            long i = start_key;
            
            for (out_key = gen.getNextKeyText(); out_key != null; out_key = gen.getNextKeyText()) {
                String val = new String(vgen.getContentForCell( i, 0 ));
                
                ImmutableBytesWritable HKey = new ImmutableBytesWritable(Bytes.toBytes(out_key.toString()));  
                Put HPut = new Put(Bytes.toBytes(out_key.toString()));  
                HPut.add(Bytes.toBytes(family_name), Bytes.toBytes(column_name), Bytes.toBytes(val));  
                context.write(HKey, HPut);
                i++;
            }
            
        }   
   }

    public static void writeRanges(long start, long end,
            long range_size, PrintStream out) {
        long range_start = start;

        while (range_start <= end) {
            
            out.println( range_start );
            range_start += range_size;
        }
    }
    
    Path createInputFile( Job job ) throws IOException {
        Configuration conf = job.getConfiguration();
        
        String work_dir = conf.get(WORK_DIR, WORK_DIR_DEFAULT);
        String input_path = conf.get(INPUT_PATH, INPUT_PATH_DEFAULT);
        
        FileSystem fs      = FileSystem.get(conf);
        Path inpath        = new Path( work_dir + input_path);
        PrintStream out = new PrintStream(
                            new BufferedOutputStream(fs.create(inpath)));
        long start = conf.getLong( START_KEY, START_KEY_DEFAULT );
        long end   = conf.getLong( END_KEY, END_KEY_DEFAULT );
        
        long range_size = conf.getLong(RANGE_SIZE, RANGE_SIZE_DEFAULT);
        
        writeRanges( start, end, range_size, out );
        out.close();

        return inpath;
    }
    
    @Override
    public int run(String[] arg0) throws Exception {
	for (String s : arg0)
		System.out.println("bulk: " + s);
        Configuration conf = this.getConf();
        Configuration hbase_conf = HBaseConfiguration.create();
        HBaseConfiguration.merge(conf, hbase_conf);
        
        if (arg0.length < 3) {
            System.out.println("Usage:"+ this.getClass().getName() + " [fs.default.name] [mapred.job.tracker] [zookeeper.quorum]" + 
                    " <input path> <output path>");
            return 1;
        }
        
        conf.set(FS_NAME, arg0[0]);
        conf.set(MAP_REDUCE, arg0[1]);
        conf.set(ZOOKEEPER, arg0[2]);
        
        if (arg0.length >= 5) {
            conf.set(INPUT_PATH, arg0[3]);
            conf.set(OUTPUT_PATH, arg0[4]);
        }

	if (arg0.length >= 7) {
            conf.setLong(START_KEY, Long.parseLong(arg0[5]));
            conf.setLong(END_KEY, Long.parseLong(arg0[6]));
        }
        
        if (arg0.length >= 8) {
            conf.setLong(RANGE_SIZE, Long.parseLong(arg0[7]));
        }
        
        Job job = new Job(conf, "YCSB# Bulk Data Generator");
        job.setJarByClass(this.getClass());
        HTable hTable = new HTable(conf, conf.get(TABLE_NAME, TABLE_NAME_DEFAULT));
        job.setMapOutputKeyClass(ImmutableBytesWritable.class);  
        job.setMapOutputValueClass(Put.class);  
        job.setInputFormatClass(TextInputFormat.class);  
        job.setOutputFormatClass(HFileOutputFormat.class);  
        job.setMapperClass(HBaseMap.class);
        
        Path inputpath = createInputFile(job);
        
        FileInputFormat.setInputPaths(job, inputpath); 
        FileOutputFormat.setOutputPath(job,new Path(conf.get(WORK_DIR, WORK_DIR_DEFAULT) +
                conf.get(OUTPUT_PATH, OUTPUT_PATH_DEFAULT)));             
        HFileOutputFormat.configureIncrementalLoad(job, hTable);  
        job.waitForCompletion(true); 
                
        return 0;
    }  
    
    public static void main(String[] args) {
        Tool tool = new HBaseDataGenerator( new Configuration() );
        try {
            ToolRunner.run( tool, args );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
