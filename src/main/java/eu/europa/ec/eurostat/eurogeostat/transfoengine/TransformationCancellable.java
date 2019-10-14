/**
 * 
 */
package eu.europa.ec.eurostat.eurogeostat.transfoengine;

/**
 * @author julien Gaffuri
 *
 */
public abstract class TransformationCancellable<T extends Agent> extends Transformation<T> {

	public TransformationCancellable(T agent) { super(agent); }
	@Override
	public boolean isCancelable() { return true; }
	public abstract void storeState();	
	public abstract void cancel();	

}
