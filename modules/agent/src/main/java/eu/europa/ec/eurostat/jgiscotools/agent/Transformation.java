/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.agent;

/**
 * A transformation to be applied by an agent, which should result on the improvement of at least one of its constraints.
 * 
 * @author julien Gaffuri
 * @param <T> 
 *
 */
public abstract class Transformation<T extends Agent> {
	//private final static Logger LOGGER = Logger.getLogger(Transformation.class.getName());

	private T agent;
	public T getAgent() { return agent; }

	public Transformation(T agent){
		this.agent = agent;
	}

	public abstract void apply();	

	public abstract boolean isCancelable();	

	public String toString(){ return getClass().getSimpleName(); }

}
