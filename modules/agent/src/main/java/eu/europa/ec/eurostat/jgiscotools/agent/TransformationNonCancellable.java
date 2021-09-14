/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.agent;

/**
 * A transformation, which cannot be cancelled.
 * 
 * @author julien Gaffuri
 *
 */public abstract class TransformationNonCancellable<T extends Agent> extends Transformation<T> {

	public TransformationNonCancellable(T agent) { super(agent); }
	@Override
	public boolean isCancelable() { return false; }

}
