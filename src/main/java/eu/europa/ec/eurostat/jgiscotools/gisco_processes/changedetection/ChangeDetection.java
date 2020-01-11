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

	private Collection<Feature> changes = null;
	private Collection<T> unchanged = null;

	/**
	 * @return The changes.
	 */
	public Collection<Feature> getChanges() {
		if(this.changes == null) compute();
		return this.changes;
	}

	/**
	 * @return The features that have not changed.
	 */
	public Collection<T> getUnchanged() {
		if(this.unchanged == null) compute();
		return this.unchanged;
	}


	private void compute() {
		this.changes = new ArrayList<>();
		this.unchanged = new ArrayList<>();

		//list ids
		Collection<String> idsIni = getIdValues(fsIni);
		Collection<String> idsFin = getIdValues(fsFin);

		//index features by ids
		HashMap<String,T> indIni = FeatureUtil.index(fsIni, idAtt);
		HashMap<String,T> indFin = FeatureUtil.index(fsFin, idAtt);

		//handle features present in both initial and final version

		//compute intersection of id lists
		Collection<String> idsInter = new ArrayList<>(idsIni);
		idsInter.retainAll(idsFin);

		for(String id : idsInter) {
			//get two corresponding objects
			T fIni = indIni.get(id);
			T fFin = indFin.get(id);

			//compute change between them
			Feature d = computeChange(fIni, fFin);
			if(d == null) unchanged.add(fFin);
			else changes.add(d);
		}

		//handle deleted features

		//compute difference: ini - fin
		Collection<String> idsDiff = new ArrayList<>(idsIni);
		idsDiff.removeAll(idsFin);

		//keep as deleted features
		for(String id : idsDiff) {
			T f = indIni.get(id);
			f.setAttribute("change", "D");
			changes.add(f);
		}

		//handle inserted features

		//compute difference: fin - ini
		idsDiff = new ArrayList<>(idsFin);
		idsDiff.removeAll(idsIni);

		//keep as inserted features
		for(String id : idsDiff) {
			T f = indFin.get(id);
			f.setAttribute("change", "I");
			changes.add(f);
		}

	}





	private Feature computeChange(T fIni, T fFin) {
		boolean attChanged = false, geomChanged = false;
		Feature change = new Feature();

		//attributes
		int nb = 0;
		for(String att : fIni.getAttributes().keySet()) {
			Object attIni = fIni.getAttribute(att);
			Object attFin = fFin.getAttribute(att);
			if(attIni.equals(attFin)) {
				change.setAttribute(att, null);
			} else {
				attChanged = true;
				change.setAttribute(att, attFin);
				nb++;
			}
		}
		//geometry
		if( ! fIni.getDefaultGeometry().equalsTopo(fFin.getDefaultGeometry()))
			geomChanged = true;

		//no change: return null
		if(!attChanged && !geomChanged) return null;

		//set id
		change.setID( getId(fFin) );
		if(idAtt != null) change.setAttribute(idAtt, fFin.getAttribute(idAtt));

		//set geometry
		change.setDefaultGeometry(fFin.getDefaultGeometry());

		//set attribute on change
		change.setAttribute("change", (geomChanged?"G":"") + (attChanged?"A"+nb:""));
		
		return change;
	}




	private String getId(T f) {
		return idAtt==null||idAtt.isEmpty()?f.getID() : f.getAttribute(idAtt).toString();
	}

	private Collection<String> getIdValues(Collection<T> fs) {
		ArrayList<String> out = new ArrayList<>();
		for(T f : fs) out.add(getId(f));
		return out;
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



	public static void main(String[] args) {
		LOGGER.info("Start");
		String path = "src/test/resources/change_detection/";
		String outpath = "target/";

		ArrayList<Feature> fsIni = GeoPackageUtil.getFeatures(path+"ini.gpkg");
		LOGGER.info("Ini="+fsIni.size());
		ArrayList<Feature> fsFin = GeoPackageUtil.getFeatures(path+"fin.gpkg");
		LOGGER.info("Fin="+fsFin.size());

		FeatureUtil.setId(fsIni, "id");
		FeatureUtil.setId(fsFin, "id");
		//TODO check id
		
		ChangeDetection<Feature> cd = new ChangeDetection<>(fsIni, fsFin, "id");

		Collection<Feature> unchanged = cd.getUnchanged();
		LOGGER.info("unchanged = "+unchanged.size());
		Collection<Feature> changes = cd.getChanges();
		LOGGER.info("changes = "+changes.size());

		CoordinateReferenceSystem crs = GeoPackageUtil.getCRS(path+"ini.gpkg");
		GeoPackageUtil.save(changes, outpath+"changes.gpkg", crs, true);
		GeoPackageUtil.save(unchanged, outpath+"unchanged.gpkg", crs, true);

		LOGGER.info("End");
	}

}
