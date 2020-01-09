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
	private String idAtt = null;

	private Collection<String> idsIni, idsFin;

	/**
	 * @param fsIni The initial version of the dataset.
	 * @param fsFin The final version of the dataset.
	 * @param idAtt The identifier column.
	 */
	public ChangeDetection(Collection<T> fsIni, Collection<T> fsFin, String idAtt) {
		this.fsIni = fsIni;
		this.fsFin = fsFin;
		this.idAtt = idAtt;

		//extract ids of initial and final features
		idsIni = getIdValues(fsIni);
		idsFin = getIdValues(fsFin);
	}

	/**
	 * Check that the id attribute is a true identifier, that is: It is populated and unique.
	 * @return
	 */
	public boolean checkId() {
		if( FeatureUtil.checkIdentfier(this.fsIni, this.idAtt).size()>0 ) return false;
		if( FeatureUtil.checkIdentfier(this.fsFin, this.idAtt).size()>0 ) return false;
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

			//compute difference: ini - fin
			Collection<String> idsDiff = new ArrayList<>(idsIni);
			idsDiff.removeAll(idsFin);

			//get corresponding features
			//TODO use index
			this.deleted = new ArrayList<>();
			for(T fIni : fsIni) {
				String id = fIni.getAttribute(idAtt).toString();
				if(idsDiff.contains(id)) this.deleted.add(fIni);
			}

			//check
			if(this.deleted.size() != idsDiff.size())
				LOGGER.warn("Unexpected number of deleted features found.");
		}
		return this.deleted;
	}

	/**
	 * @return The inserted features.
	 */
	public Collection<T> getInserted() {
		if(this.inserted == null) {

			//compute difference: fin - ini
			Collection<String> idsDiff = new ArrayList<>(idsFin);
			idsDiff.removeAll(idsIni);

			//get corresponding features
			//TODO use index
			this.inserted = new ArrayList<>();
			for(T fFin : fsFin) {
				String id = fFin.getAttribute(idAtt).toString();
				if(idsDiff.contains(id)) this.inserted.add(fFin);
			}

			//check
			if(this.inserted.size() != idsDiff.size())
				LOGGER.warn("Unexpected number of inserted features found.");
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

		//compute intersection
		Collection<String> idsInter = new ArrayList<>(idsIni);
		idsInter.retainAll(idsFin);

		for(String id : idsInter) {
			//get two corresponding objects
			//TODO index them by id
			T fIni = null;
			T fFin = null;

			//compare them and add to relevant list
			boolean b = getChangeCalculator().changed(fIni, fFin);
			if(!b) {
				unchanged.add(fFin);
			} else {
				changed.add(fFin); //TODO fFin or fIni?
				//TODO add info on change
			}
		}
	}

	/**
	 * The method used to compute the change between two versions of a feature.
	 * By default, the geometries and attribute values should be the same.
	 */
	private ChangeCalculator<T> changeCalculator = new DefaultChangeCalculator<T>();
	public ChangeCalculator<T> getChangeCalculator() { return changeCalculator; }
	public void setChangeCalculator(ChangeCalculator<T> changeCalculator) { this.changeCalculator = changeCalculator; }




	public interface ChangeCalculator<U extends Feature> {
		//TODO chould return change data
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


	private Collection<String> getIdValues(Collection<T> fs) {
		ArrayList<String> out = new ArrayList<>();
		for(T f : fs) out.add(f.getAttribute(idAtt).toString());
		return out;
	}

}
