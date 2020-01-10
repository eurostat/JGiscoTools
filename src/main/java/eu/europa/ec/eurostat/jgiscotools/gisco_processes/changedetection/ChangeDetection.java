/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.changedetection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.FeatureUtil;
import eu.europa.ec.eurostat.jgiscotools.io.GeoPackageUtil;

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

	private Collection<T> fsIni;
	private Collection<T> fsFin;
	private String idAtt = null;

	/**
	 * @param fsIni The initial version of the dataset.
	 * @param fsFin The final version of the dataset.
	 * @param idAtt The identifier column. Set to null if the default getId() value should be used.
	 */
	public ChangeDetection(Collection<T> fsIni, Collection<T> fsFin, String idAtt) {
		this.fsIni = fsIni;
		this.fsFin = fsFin;
		this.idAtt = idAtt;
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
			if(idsIni == null) prepare();

			//compute difference: ini - fin
			Collection<String> idsDiff = new ArrayList<>(idsIni);
			idsDiff.removeAll(idsFin);

			//get corresponding features
			this.deleted = new ArrayList<>();
			for(String id : idsDiff)
				this.deleted.add(indIni.get(id));

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
			if(idsFin == null) prepare();

			//compute difference: fin - ini
			Collection<String> idsDiff = new ArrayList<>(idsFin);
			idsDiff.removeAll(idsIni);

			//get corresponding features
			this.inserted = new ArrayList<>();
			for(String id : idsDiff)
				this.inserted.add(indFin.get(id));

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
		if(this.changed == null) computeChangedUnchanged();
		return this.changed;
	}

	/**
	 * @return The features that have not changed.
	 */
	public Collection<T> getUnchanged() {
		if(this.unchanged == null) computeChangedUnchanged();
		return this.unchanged;
	}

	private void computeChangedUnchanged() {
		this.unchanged = new ArrayList<>();
		this.changed = new ArrayList<>();

		//compute intersection
		if(idsIni == null) prepare();
		Collection<String> idsInter = new ArrayList<>(idsIni);
		idsInter.retainAll(idsFin);

		for(String id : idsInter) {
			//get two corresponding objects
			T fIni = indIni.get(id);
			T fFin = indFin.get(id);

			//compare them and add to relevant list
			boolean b = getChangeCalculator().changed(fIni, fFin);
			(b?changed:unchanged).add(fFin);
		}
	}



	//delta

	private Collection<Feature> delta = null;
	public Collection<Feature> getDelta() {
		if(this.delta == null) computeDelta();
		return this.delta;
	}

	private void computeDelta() {
		//TODO How to represent that?
		//for each pair of object, the info which allows updating it
		//deleted + inserted: the object with change description
		//attribute change: the objects with changed attributes only - number of changed attributes
		//geometry change: the object with new geometry? No: one with removed part + one with added part + info on geom change amount
		//geometry + attribute change
		//or: a CSV file with change description. fields: type of change (deletion, insertion, change) + geom change data + attribute change data
	}




	/**
	 * The method used to compute the change between two versions of a feature.
	 * By default, the geometries and attribute values should be the same.
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




	private String getId(T f) {
		return idAtt==null||idAtt.isEmpty()?f.getID() : f.getAttribute(idAtt).toString();
	}

	private Collection<String> idsIni, idsFin;
	private HashMap<String,T> indIni, indFin;

	private void prepare() {
		idsIni = getIdValues(fsIni);
		idsFin = getIdValues(fsFin);
		indIni = FeatureUtil.index(fsIni, idAtt);
		indFin = FeatureUtil.index(fsFin, idAtt);
	}

	private Collection<String> getIdValues(Collection<T> fs) {
		ArrayList<String> out = new ArrayList<>();
		for(T f : fs) out.add(getId(f));
		return out;
	}




	public static void main(String[] args) {
		LOGGER.info("Start");
		String path = "src/test/resources/change_detection/";
		String outpath = "target/";

		ArrayList<Feature> fsIni = GeoPackageUtil.getFeatures(path+"ini.gpkg");
		LOGGER.info("Ini="+fsIni.size());
		ArrayList<Feature> fsFin = GeoPackageUtil.getFeatures(path+"fin.gpkg");
		LOGGER.info("Fin="+fsFin.size());

		ChangeDetection<Feature> cd = new ChangeDetection<>(fsIni, fsFin, "id");
		boolean b = cd.checkId();
		LOGGER.info(b);

		Collection<Feature> deleted = cd.getDeleted();
		LOGGER.info("deleted="+deleted.size());
		Collection<Feature> inserted = cd.getInserted();
		LOGGER.info("inserted="+inserted.size());
		Collection<Feature> changed = cd.getChanged();
		LOGGER.info("changed="+changed.size());
		Collection<Feature> unchanged = cd.getUnchanged();
		LOGGER.info("unchanged="+unchanged.size());


		CoordinateReferenceSystem crs = GeoPackageUtil.getCRS(path+"ini.gpkg");
		GeoPackageUtil.save(deleted, outpath+"deleted.gpkg", crs, true);
		GeoPackageUtil.save(inserted, outpath+"inserted.gpkg", crs, true);
		GeoPackageUtil.save(changed, outpath+"changed.gpkg", crs, true);
		GeoPackageUtil.save(unchanged, outpath+"unchanged.gpkg", crs, true);

		LOGGER.info("End");
	}

}
