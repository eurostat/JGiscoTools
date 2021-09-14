/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.agent;

/**
 * A transformation, which cannot be cancelled.
 * In theory, all transformations could be cancellable, as soon as the initial state can be stored. In practice, it is not always easy and implemented.
 * 
 * @author julien Gaffuri
 *
 */public abstract class TransformationNonCancellable<T extends Agent> extends Transformation<T> {

	public TransformationNonCancellable(T agent) { super(agent); }
	@Override
	public boolean isCancelable() { return false; }

}
