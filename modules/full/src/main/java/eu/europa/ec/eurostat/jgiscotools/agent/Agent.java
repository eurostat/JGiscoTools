/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.agent;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.europa.ec.eurostat.jgiscotools.util.Util;

/**
 * @author julien Gaffuri
 *
 */
public abstract class Agent {
	public final static Logger LOGGER = LogManager.getLogger(Agent.class.getName());

	private static int ID_COUNT=1;	
	private String id;
	public String getId() { return id; }
	public Agent setId(String id) { this.id = id; return this; }

	private boolean frozen = false;
	public boolean isFrozen() { return frozen; }
	public void freeze() { frozen = true; }
	public void unFreeze() { frozen = false; }

	public Agent(Object object){
		this.object = object;
		id="ag"+(ID_COUNT++);
	}

	private Object object;
	public Object getObject() { return object; }

	private List<Constraint<?>> constraints = new ArrayList<Constraint<?>>();
	public List<Constraint<?>> getConstraints() { return constraints; }
	public boolean addConstraint(Constraint<?> c) { return constraints.add(c); }
	public boolean removeConstraint(Constraint<?> c) { return constraints.remove(c); }
	public void clearConstraints() { constraints.clear(); }

	public Constraint<?> getConstraint(Class<?> cl){
		for(Constraint<?> c : getConstraints()) if(cl.isInstance(c)) return c;
		return null;
	}

	//from 0 to 10 (satisfied)
	protected double satisfaction = 10;
	public double getSatisfaction() { return satisfaction; }
	public static double SATISFACTION_RESOLUTION = 0.00001;
	public boolean isSatisfied(){ return 10-this.getSatisfaction() < SATISFACTION_RESOLUTION; }

	//by default, the average of the satisfactions of the soft constraints. 0 if any hard constraint is unsatisfied.
	public void computeSatisfaction() {
		if(isDeleted() || constraints.size()==0) { satisfaction=10; return; }
		satisfaction=0; double sImp=0;
		for(Constraint<?> c : constraints){
			c.computeCurrentValue();
			c.computeGoalValue();

			c.computeSatisfaction();
			if(c.getSatisfaction()<0) LOGGER.warn("Constraint with negative satisfaction found: "+c.getMessage());
			if(c.getSatisfaction()>10) LOGGER.warn("Constraint with satisfaction above 10: "+c.getMessage());

			if(c.isHard() && c.getSatisfaction()<10) {
				satisfaction = 0;
				return;
			}
			if(c.isHard()) continue;
			satisfaction += c.getImportance() * c.getSatisfaction();
			sImp += c.getImportance();
		}
		if(sImp==0) satisfaction = 10; else satisfaction /= sImp ;
	}

	//flag to mark that the agent is deleted
	private boolean deleted = false;
	public boolean isDeleted() { return deleted; }
	public void setDeleted(boolean v) { this.deleted = v; }


	//retrieve list of candidate transformations to try improving agent's satisfaction
	@SuppressWarnings("unchecked")
	public List<Transformation<?>> getTransformations(){
		List<Transformation<?>> tr = new ArrayList<Transformation<?>>();
		if(isDeleted()) return tr;

		constraints.sort(Constraint.COMPARATOR_CONSTR);
		for(Constraint<?> c : constraints) {
			if(c.getSatisfaction()==10) continue;
			tr.addAll(c.getTransformations());
		}
		return tr;
	}


	//lifecycle of the agent
	public void activate() { //activate(null); }
		//public void activate(PrintWriter logWriter) {
		if(LOGGER.isTraceEnabled()) LOGGER.trace("Activate agent: "+this.id);

		if(isFrozen() || isDeleted()) return;

		//compute satisfaction
		this.computeSatisfaction();
		if(LOGGER.isTraceEnabled()) LOGGER.trace(" satisf = "+this.getSatisfaction());

		//satisfaction perfect: nothing to do.
		if(isSatisfied()) return;

		double sat1 = this.getSatisfaction();

		//get list of candidate transformations from agent
		List<Transformation<?>> ts = this.getTransformations();
		while(ts.size()>0){
			Transformation<?> t = ts.get(0);
			ts.remove(0);

			//save current state
			if(t.isCancelable()) ((TransformationCancellable<?>)t).storeState();

			//apply transformation
			if(LOGGER.isTraceEnabled()) LOGGER.trace(" apply "+t.toString()+" on "+this.toString() );
			t.apply();

			//TODO check proposing constraint satisfaction improvement first. Propose generic validity function?

			//get new satisfaction
			this.computeSatisfaction();
			double sat2 = this.getSatisfaction();
			if(LOGGER.isTraceEnabled()) LOGGER.trace(" satisf = "+this.getSatisfaction());

			//log
			//if(logWriter != null) logWriter.println( getMessage(t, sat1, sat2) );

			if(isSatisfied()) {
				//perfect state reached: end
				return;
			} else if(sat2 - sat1 > SATISFACTION_RESOLUTION){
				//improvement: get new list of candidate transformations
				ts = this.getTransformations();
				sat1 = sat2;
			} else {
				//no improvement: go back to previous state, if possible
				if(t.isCancelable())
					((TransformationCancellable<?>)t).cancel();
				else if(sat2 - sat1 < 0)
					LOGGER.warn("Non cancellable transformation "+t.getClass().getSimpleName()+" resulted in satisfaction decrease for agent "+this.getId() + "   SatIni="+sat1+" --- satFin="+sat2+" --- diff="+(sat2-sat1));
			}
		}
	}

	/*
	private String getMessage(Transformation<?> t, double sat1, double sat2){
		double diff = sat2-sat1;
		return new StringBuffer()
				.append(diff>0?"#":diff==0?"0":"-").append(",")
				.append(isDeleted()?"DEL":"").append(",")
				.append(this.getClass().getSimpleName()).append(",")
				.append(this.getId()).append(",")
				.append(t.toString()).append(",")
				.append(Util.round(diff, 3)).append(",")
				.append(Util.round(sat1, 3)).append(",")
				.append(Util.round(sat2, 5))
				.toString();
	}
	 */

	public String toString(){
		return getClass().getSimpleName()+"-"+getId()+" (satisf="+Util.round(satisfaction,3)+",nbContr="+constraints.size()+",obj="+getObject().toString()+")";
	}



	/*/produce and save a report on agents' states
	public static void saveStateReport(Collection<?> agents, String outPath, String outFile){
		if(agents == null || agents.size()==0) { LOGGER.warn("Could not export report on agents: Empty collection. "+outFile); return; }
		ArrayList<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
		for(Object ag_ : agents){
			Agent ag = (Agent)ag_;
			HashMap<String, Object> d = new HashMap<String, Object>();
			ag.computeSatisfaction();
			d.put("id", ag.id);
			for(Constraint<?> c:ag.constraints)
				d.put(c.getClass().getSimpleName(), ""+c.getSatisfaction());
			d.put("satisfaction", ""+ag.getSatisfaction());
			data.add(d);
		}
		CSVUtil.save(data, outPath, outFile);
	}*/

	public void clear() {
		this.object = null;
		for(Constraint<?> c : getConstraints()) c.clear();
		clearConstraints();
	}

}
