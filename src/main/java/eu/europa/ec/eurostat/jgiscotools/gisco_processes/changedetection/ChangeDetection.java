/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.changedetection;

import java.util.Collection;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;

/**
 * 
 * Analyse the differences between two versions of a datasets.
 * Compute the difference.
 * Note: both datasets are suppose to have a shared stable identifier.
 * 
 * @author julien Gaffuri
 *
 */
public class ChangeDetection<T extends Feature> {

	private Collection<T> fs1, fs2;
	private String idProp = null;

	public ChangeDetection(Collection<T> fs1, Collection<T> fs2, String idProp) {
		this.fs1 = fs1;
		this.fs2 = fs2;
		this.idProp = idProp;
	}

	public boolean checkId() {
		//TODO check if idProp is an id for both fs (populated, and unique)
		return true;
	}

	private Collection<T> deleted = null;
	public Collection<T> getDeleted() {
		if(deleted == null) {
			//get fs1 ids
			//get fs2 ids
			//get fs1-fs2
			//TODO
		}
		return deleted;
	}

	private Collection<T> inserted = null;
	public Collection<T> getInserted() {
		if(inserted == null) {
			//get fs1 ids
			//get fs2 ids
			//get fs2-fs1
			//TODO
		}
		return inserted;
	}

	private Collection<T> changed = null;
	public Collection<T> getChanged() {
		if(changed == null)
			computeChangedUnchanged();
		return changed;
	}

	private Collection<T> unchanged = null;
	public Collection<T> getUnchanged() {
		if(unchanged == null)
			computeChangedUnchanged();
		return unchanged;
	}

	private void computeChangedUnchanged() {
		//TODO
		//get fs1 ids
		//get fs2 ids
		//get fs2 inter fs1
		//among them, check which one have changes
	}


	private ChangeCalculator<T> changeCalculator = new DefaultChangeCalculator<T>();
	public ChangeCalculator<T> getChangeCalculator() { return changeCalculator; }
	public void setChangeCalculator(ChangeCalculator<T> changeCalculator) { this.changeCalculator = changeCalculator; }




	public interface ChangeCalculator<U> {
		boolean changed(U f1, U f2);
	}

	public class DefaultChangeCalculator<R> implements ChangeCalculator<R> {
		@Override
		public boolean changed(R f1, R f2) {
			//TODO compare properties
			//TODO compare geometries
			return false;
		}
	}

	//TODO decompose change calculator into geom and attributes


	//TODO make test class with fake dataset

}
