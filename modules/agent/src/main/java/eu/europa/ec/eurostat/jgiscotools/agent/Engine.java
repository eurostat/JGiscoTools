/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.agent;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * An engine for the orchestration of agents executions.
 * 
 * @author julien Gaffuri
 *
 */
public class Engine<T extends Agent> {
	public final static Logger LOGGER = LogManager.getLogger(Engine.class.getName());

	/**
	 * The agents to process
	 */
	private List<T> agents;

	public Engine(Collection<T> agents/*, String logFilePath*/){
		this.agents = new ArrayList<T>();
		if(agents != null) this.agents.addAll(agents);
		//this.logFilePath = logFilePath;
	}

	//TODO implement/test other activation methods
	/**
	 * Activate the agents as a queue (FIFO)
	 * 
	 * @return this
	 */
	public Engine<T> activateQueue(){
		for(Agent agent : agents) {
			if(agent.isFrozen()) continue;
			if(LOGGER.isTraceEnabled()) LOGGER.trace("Activate agent "+agent.getId());
			agent.activate(/*getLogWriter()*/);
		}
		return this;
	}

	/**
	 * Sort agents by id.
	 * 
	 * @return this
	 */
	public Engine<T> sort() {
		if(agents == null) return this;
		agents.sort(new Comparator<T>() {
			public int compare(T a0, T a1) { return a0.getId().compareTo(a1.getId()); }
		});
		return this;
	}

	/**
	 * Shuffle the list of agents.
	 * 
	 * @return this
	 */
	public Engine<T> shuffle() { Collections.shuffle(agents); return this; }

	/**
	 * Clear list of agents.
	 * 
	 * @return this
	 */
	public Engine<T> clear() {
		if(this.agents != null) this.agents.clear();
		return this;
	}



	
	/**
	 * Get the list of insatisfied constraints of some agents, ordered by satisfaction.
	 * 
	 * @param agents
	 * @param satisfactionThreshold
	 * @return
	 */
	public static <T extends Agent> ArrayList<Constraint<?>> getUnsatisfiedConstraints(Collection<T> agents, double satisfactionThreshold){
		ArrayList<Constraint<?>> out = new ArrayList<Constraint<?>>();
		if(agents == null) return out;
		for(Agent ag : agents){
			ag.computeSatisfaction();
			if(ag.isSatisfied()) continue;
			for(Constraint<?> c : ag.getConstraints())
				if(!c.isSatisfied(satisfactionThreshold)) out.add(c);
		}
		Collections.sort(out, Constraint.COMPARATOR_CONSTR_BY_SATISFACTION);
		Collections.reverse(out);
		return out;
	}


	public Engine<T> runEvaluation(String outFilePath, boolean overrideFile){
		try {
			File f = new File(outFilePath);
			if(overrideFile && f.exists()) f.delete();
			if(!f.exists()) f.createNewFile();
			BufferedWriter bw = new BufferedWriter(new FileWriter(f, true));

			for(Agent ag : agents) {
				ag.computeSatisfaction();
				if(ag.isSatisfied()) continue;
				for(Constraint<?> c : ag.getConstraints())
					if(!c.isSatisfied(Agent.SATISFACTION_RESOLUTION)) {
						bw.write(c.getMessage());
						bw.write("\n");
					}
			}
			bw.close();
		} catch (Exception e) { e.printStackTrace(); }
		return this;
	}




	/*
	public Stats getSatisfactionStats(boolean refreshSatisfactionValues){
		ArrayList<Double> s = new ArrayList<Double>();
		for(Agent agent : agents){
			if(agent.isDeleted()) continue;
			if(refreshSatisfactionValues) agent.computeSatisfaction();
			s.add(new Double(agent.getSatisfaction()));
		}
		double[] s_ = ArrayUtils.toPrimitive(s.toArray(new Double[s.size()]));
		Stats st = new Stats();
		st.max = StatUtils.max(s_);
		st.min = StatUtils.min(s_);
		st.mean = StatUtils.mean(s_);
		st.median = StatUtils.percentile(s_,50);
		st.q1 = StatUtils.percentile(s_,25);
		st.q2 = StatUtils.percentile(s_,75);
		st.std = Math.sqrt(StatUtils.variance(s_));
		st.rms = Math.sqrt(StatUtils.sumSq(s_)/s_.length);
		return st;
	}
	public class Stats{
		public double max,min,mean,median,q1,q2,std,rms;
		public void print(){ System.out.println(getSummary()); }
		public String getSummary() {
			return new StringBuffer()
					.append("Max = " + max + "\n")
					.append("Min = " + min + "\n")
					.append("Mean = " + mean + "\n")
					.append("Median = " + median + "\n")
					.append("Q1 = " + q1 + "\n")
					.append("Q2 = " + q2 + "\n")
					.append("Std = " + std + "\n")
					.append("RMS = " + rms + "\n")
					.toString();
		}
	}
	 */



	//file logging capability
	/*private String logFilePath = null;
	private PrintWriter logWriter = null;
	private PrintWriter getLogWriter() {
		if(logWriter == null && logFilePath != null)
			try {
				File f = new File(logFilePath);
				if(f.exists()) f.delete();
				f.createNewFile();
				logWriter = new PrintWriter(f);
			} catch (Exception e) { e.printStackTrace(); }
		return logWriter;
	}
	public void printLog(String mes) {
		if(getLogWriter()!=null)
			getLogWriter().println(mes);
	}

	public void closeLogger(){
		if(logWriter == null) return;
		logWriter.close();
		logWriter = null;
	}*/

}
