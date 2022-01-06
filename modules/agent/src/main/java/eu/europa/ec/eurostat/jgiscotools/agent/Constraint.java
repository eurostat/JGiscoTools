/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.agent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A constraint.
 * 
 * @author julien Gaffuri
 * @param <T> 
 *
 */
public abstract class Constraint<T extends Agent> implements Comparable<Constraint<?>>{
	//private final static Logger LOGGER = Logger.getLogger(Constraint.class.getName());

	//the agent the constraint relates to
	private T agent;
	public T getAgent() { return agent; }

	public Constraint(T agent){
		this.agent = agent;
		computeInitialValue();
	}


	/**
	 * A constraint whose satisfaction is expected to be 0 or 10, which has to be satisfied.
	 * Example: a topological constraint.
	 * 
	 * @return
	 */
	public boolean isHard() { return false; }

	//importance (used for soft constraints only, to compute agent's overall satisfaction).
	double importance = 1;
	public double getImportance() { return importance; }
	public Constraint<T> setImportance(double importance) { this.importance = importance; return this; }

	//from 0 to 10 (satisfied)
	protected double satisfaction = 10;
	public double getSatisfaction() { return satisfaction; }
	public boolean isSatisfied(double satisfactionResolution) { return 10-this.getSatisfaction() < satisfactionResolution; }

	public void computeInitialValue() {}
	public void computeCurrentValue() {}
	public void computeGoalValue() {}
	public abstract void computeSatisfaction();

	//used to determine which constraints should be satisfied in priority
	double priority = 1;
	public double getPriority() { return priority; }
	public Constraint<T> setPriority(double priority) { this.priority = priority; return this; }

	//return a list of transformations which could, a priori, improve the constraint's satisfaction
	public List<Transformation<T>> getTransformations() {
		return new ArrayList<Transformation<T>>();
	}



	public String getMessage(){
		return new StringBuffer()
				.append(getAgent().getClass().getSimpleName()).append(",")
				.append(getAgent().getId()).append(",")
				.append(getClass().getSimpleName()).append(",")
				.append("pri=").append(getPriority()).append(",")
				.append("imp=").append(getImportance()).append(",")
				.append("s=").append(getSatisfaction())
				//TODO include constraint's position?
				.toString();
	}


	public int compareTo(Constraint<?> c) {
		return (int)(100000*(c.getPriority()-this.getPriority()));
	}

	public void clear() {
		this.agent = null;
	}




	/**
	 * Some comparators
	 */


	//comparison by priority
	public static final Comparator<Constraint<?>> COMPARATOR_CONSTR = new Comparator<>() {
		@Override
		public int compare(Constraint<?> c0, Constraint<?> c1) {
			return c0.compareTo(c1);
		}
	};

	public static final Comparator<Constraint<?>> COMPARATOR_CONSTR_BY_SATISFACTION = new Comparator<>() {
		@Override
		public int compare(Constraint<?> c0, Constraint<?> c1) {
			return (int)(100000000*(c1.getSatisfaction()-c0.getSatisfaction()));
		}
	};

}
