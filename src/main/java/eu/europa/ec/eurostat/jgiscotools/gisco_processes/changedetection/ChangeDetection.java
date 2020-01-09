/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.changedetection;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.FeatureUtil;

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
	private final static Logger LOGGER = LogManager.getLogger(ChangeDetection.class.getName());

	//TODO make test class with fake dataset

	private Collection<T> fsIni, fsFin;
	private String idProp = null;

	/**
	 * @param fsIni The initial version of the dataset.
	 * @param fsFin The final version of the dataset.
	 * @param idProp The identifier column.
	 */
	public ChangeDetection(Collection<T> fsIni, Collection<T> fsFin, String idProp) {
		this.fsIni = fsIni;
		this.fsFin = fsFin;
		this.idProp = idProp;
	}

	/**
	 * Check that the id property is a true identifier, that is: It is populated and unique.
	 * @return
	 */
	public boolean checkId() {
		if( FeatureUtil.checkIdentfier(this.fsIni, this.idProp).size()>0 ) return false;
		if( FeatureUtil.checkIdentfier(this.fsFin, this.idProp).size()>0 ) return false;
		return true;
	}



	private Collection<T> deleted = null;
	private Collection<T> inserted = null;
	private Collection<T> changed = null;
	private Collection<T> unchanged = null;
	
	
	/**
	 * @return The deleted features.
	 */
	public Collection<T> getDeleted() {
		if(this.deleted == null) {
			//get fs1 ids
			//get fs2 ids
			//get fs1-fs2
			//TODO
		}
		return this.deleted;
	}

	/**
	 * @return The inserted features.
	 */
	public Collection<T> getInserted() {
		if(this.inserted == null) {
			//get fs1 ids
			//get fs2 ids
			//get fs2-fs1
			//TODO
		}
		return this.inserted;
	}

	/**
	 * @return The features that have changed.
	 */
	public Collection<T> getChanged() {
		if(this.changed == null)
			computeChangedUnchanged();
		return this.changed;
	}

	/**
	 * @return The features that have not changed.
	 */
	public Collection<T> getUnchanged() {
		if(this.unchanged == null)
			computeChangedUnchanged();
		return this.unchanged;
	}

	private void computeChangedUnchanged() {
		this.unchanged = new ArrayList<>();
		this.changed = new ArrayList<>();
		//TODO
		//get fs1 ids
		//get fs2 ids
		//get fs2 inter fs1
		//among them, check which one have changes
	}

	/**
	 * The method used to compute the change between two versions of a feature.
	 * By default, the geometries and property values should be the same.
	 */
	private ChangeCalculator<T> changeCalculator = new DefaultChangeCalculator<T>();
	public ChangeCalculator<T> getChangeCalculator() { return changeCalculator; }
	public void setChangeCalculator(ChangeCalculator<T> changeCalculator) { this.changeCalculator = changeCalculator; }




	public interface ChangeCalculator<U extends Feature> {
		boolean changed(U fIni, U fFin);
	}

	public class DefaultChangeCalculator<R extends Feature> implements ChangeCalculator<R> {
		@Override
		public boolean changed(R fIni, R fFin) {
			//if any single attribute is different, it has changed
			for(String att : fIni.getAttributes().keySet()) {
				Object attIni = fIni.getAttribute(att);
				Object attFin = fFin.getAttribute(att);
				if(!attIni.equals(attFin)) return true;
			}
			//if the geometry is different, it has changed
			if( ! fIni.getDefaultGeometry().equalsTopo(fFin.getDefaultGeometry()))
				 return true;
			return false;
		}
	}

}
