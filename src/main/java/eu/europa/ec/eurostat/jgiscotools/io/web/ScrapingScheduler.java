/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.io.web;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import eu.europa.ec.eurostat.java4eurostat.io.IOUtil;
import eu.europa.ec.eurostat.java4eurostat.io.XML;


/**
 * @author Julien Gaffuri
 *
 */
public class ScrapingScheduler {

	/**
	 * The queue of queries
	 */
	private PriorityQueue<Query> queries = new PriorityQueue<Query>();

	/**
	 * The list of query signatures currently in the queue. It is used to ensure a same query is not added twice
	 */
	private HashSet<String> querySignatures = new HashSet<String>();

	/**
	 * Add a query to the queue.
	 * 
	 * @param type
	 * @param url
	 * @param callback
	 * @return
	 */
	public boolean add(QueryType type, String url, Function callback){
		Query qu = new Query(type, url, callback);
		String sign = qu.getSignature();
		synchronized (queries) {
			if(querySignatures.contains(sign)) { System.out.println("Duplicate: "+sign); return false; }
			querySignatures.add(sign);
			return queries.add(qu);
		}
	}

	/**
	 * A query.
	 * 
	 * @author Julien Gaffuri
	 *
	 */
	private static class Query implements Comparable<Query> {
		QueryType type;
		String url;
		Function callback;

		private int id;
		private static int COUNT = 0;
		public void newId() { this.id=COUNT++; }

		public Query(QueryType type, String url, Function callback){ this.type=type; this.url=url; this.callback=callback; newId(); }
		String getSignature(){ return url; }
		public int compareTo(Query qu) { return (int)(id-qu.id); }
	}
	public enum QueryType { STRING, XML }
	public interface Function { void execute(Object data); }

	private int count = 0, regularActionFreq;
	private Function regularAction;
	private StringBuffer sb = new StringBuffer();
	public StringBuffer append(String st) { synchronized (sb) { return sb.append(st); }}

	public ScrapingScheduler(){ this(-1,null); }
	public ScrapingScheduler(int regularActionFreq, Function regularAction){
		this.regularActionFreq=regularActionFreq; this.regularAction=regularAction;
	}
	public ScrapingScheduler(int regularSaveFreq, final String path, final String fileName, boolean deleteInitialFile){
		this(regularSaveFreq, null);

		//initialise output file
		new File(path).mkdirs();
		File outFile_ = new File(path+fileName);
		try {
			if(outFile_.exists() && deleteInitialFile) outFile_.delete();
			if(!outFile_.exists()) Files.createFile(Paths.get(path+fileName));
		} catch (Exception e) { e.printStackTrace(); }

		this.regularAction = new Function() {
			public void execute(Object data) {
				System.out.println("save...");
				try {
					synchronized (sb) { 
						Files.write(Paths.get(path+fileName), sb.toString().getBytes(), StandardOpenOption.APPEND);
						sb = new StringBuffer();
					}
				} catch (IOException e) { e.printStackTrace(); }
			}
		};

	}

	/**
	 * Launch an executor "AtFixedRate".
	 * NB: several executors may be launched in parrallel in case several keys are available.
	 * 
	 * @param timeMilliSeconds
	 * @param urlKeyPart
	 * @param verbose
	 */
	public void launchExecutorAtFixedRate(int timeMilliSeconds, final String urlKeyPart, final boolean verbose){
		final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
		executor.scheduleAtFixedRate(new Runnable() {
			public void run() {
				Query qu = null;
				synchronized (queries) {
					qu = queries.poll();
					if(verbose) System.out.println("Queue size: "+queries.size());
				}

				if(qu==null){
					//no more query to execute: exit
					if(regularAction!=null && regularActionFreq>0) regularAction.execute(null);
					System.out.println("Done");
					executor.shutdown();
					return;
				}

				//execture query
				String url = qu.url + urlKeyPart;
				if(verbose) System.out.println(url);

				try {

					//get data from url
					Object data;
					if(qu.type == QueryType.XML) data = XML.parseXMLfromURL(url);
					else data = IOUtil.getDataFromURL(url);

					if(data != null){
						qu.callback.execute(data);
					} else {
						//if data is null, retry later
						System.err.println("Null data retrieved for query: " + url);
						synchronized (queries) { queries.add(qu); }
					}

					//execute regular action
					count++;
					if(regularAction!=null && regularActionFreq>0 && count>=regularActionFreq){
						regularAction.execute(null);
						count=0;
					}

				} catch (NullPointerException e) {
					System.err.println("Problem with query: " + url);
					qu.newId();
					synchronized (queries) { queries.add(qu); }
					e.printStackTrace();
				}
			}
		}, 0, timeMilliSeconds, TimeUnit.MILLISECONDS);
	}

}
