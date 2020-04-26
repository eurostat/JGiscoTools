/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.agent;

/**
 * @author julien Gaffuri
 *
 */
public abstract class TransformationNonCancellable<T extends Agent> extends Transformation<T> {

	public TransformationNonCancellable(T agent) { super(agent); }
	@Override
	public boolean isCancelable() { return false; }

}
