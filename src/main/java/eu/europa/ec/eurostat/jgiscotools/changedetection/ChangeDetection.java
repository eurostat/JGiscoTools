/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.changedetection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.index.quadtree.Quadtree;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import eu.europa.ec.eurostat.jgiscotools.algo.distances.HausdorffDistance;
import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.FeatureUtil;
import eu.europa.ec.eurostat.jgiscotools.io.GeoPackageUtil;
import eu.europa.ec.eurostat.jgiscotools.util.JTSGeomUtil;

/**
 * 
 * Analyse the differences between two versions of a dataset.
 * Compute the difference.
 * Note: both datasets are suppose to have a shared stable identifier.
 * 
 * @author julien Gaffuri
 *
 */
public class ChangeDetection {
	private final static Logger LOGGER = LogManager.getLogger(ChangeDetection.class.getName());

	/**
	 * The dataset in its initial version.
	 */
	private Collection<Feature> fsIni;

	/**
	 * The dataset in its final version.
	 */
	private Collection<Feature> fsFin;

	/**
	 * The geometrical resolution of the dataset.
	 * Geometrical changes below this value will be ignored.
	 */
	private double resolution = -1;
	public double getResolution() { return resolution; }

	/**
	 * @param fsIni The dataset in its initial version.
	 * @param fsFin The dataset in its final version.
	 * @param resolution The geometrical resolution of the dataset. Geometrical changes below this value will be ignored.
	 */
	public ChangeDetection(Collection<Feature> fsIni, Collection<Feature> fsFin, double resolution) {
		this.fsIni = fsIni;
		this.fsFin = fsFin;
		this.resolution = resolution;
	}
	public ChangeDetection(Collection<Feature> fsIni, Collection<Feature> fsFin) {
		this(fsIni, fsFin, -1);
	}

	private Collection<Feature> changes = null;
	private Collection<Feature> unchanged = null;

	/**
	 * @return The changes between the initial and final versions.
	 */
	public Collection<Feature> getChanges() {
		if(this.changes == null) compare();
		return this.changes;
	}

	/**
	 * @return The features that have not changed.
	 */
	public Collection<Feature> getUnchanged() {
		if(this.unchanged == null) compare();
		return this.unchanged;
	}


	/**
	 * The attributes to ignore when comparing the changes of a feature.
	 * If one or several values of these attributes only change,
	 * then the feature is condidered as unchanged. 
	 */
	private List<String> attributesToIgnore = null;
	public List<String> getAttributesToIgnore() { return attributesToIgnore; }
	public void setAttributesToIgnore(String... attributesToIgnore) { this.attributesToIgnore = Arrays.asList(attributesToIgnore); }

	/**
	 * Indexed structure to speedup the retrieval of features by id.
	 */
	private HashMap<String,Feature> indIni, indFin;

	/**
	 * Compare both datasets.
	 * Populate the changes.
	 */
	private void compare() {
		this.changes = new ArrayList<>();
		this.unchanged = new ArrayList<>();

		//list id values
		Collection<String> idsIni = FeatureUtil.getIdValues(fsIni, null);
		Collection<String> idsFin = FeatureUtil.getIdValues(fsFin, null);

		//index features by ids
		indIni = FeatureUtil.index(fsIni, null);
		indFin = FeatureUtil.index(fsFin, null);

		//find features present in both datasets and compare them

		//compute intersection of id sets
		Collection<String> idsInter = new ArrayList<>(idsIni);
		idsInter.retainAll(idsFin);

		for(String id : idsInter) {
			//get two corresponding features
			Feature fIni = indIni.get(id);
			Feature fFin = indFin.get(id);

			//compute change between them
			Feature ch = compare(fIni, fFin, getResolution(), attributesToIgnore );

			//both versions identical. No change detected.
			if(ch == null) unchanged.add(fFin);

			//change
			else changes.add(ch);
		}

		//find deleted features

		//compute difference: ini - fin
		Collection<String> idsDiff = new ArrayList<>(idsIni);
		idsDiff.removeAll(idsFin);

		//retrieve deleted features
		for(String id : idsDiff) {
			Feature f = indIni.get(id);
			Feature ch = FeatureUtil.copy(f);
			ch.setAttribute("ch_id", f.getID());
			ch.setAttribute("change", "D");
			changes.add(ch);
		}

		//find inserted features

		//compute difference: fin - ini
		idsDiff = new ArrayList<>(idsFin);
		idsDiff.removeAll(idsIni);

		//retrieve inserted features
		for(String id : idsDiff) {
			Feature f = indFin.get(id);
			Feature ch = FeatureUtil.copy(f);
			ch.setAttribute("ch_id", f.getID());
			ch.setAttribute("change", "I");
			changes.add(ch);
		}

	}

	/**
	 * Compare two versions of the same feature.
	 * Both features are expected to have the same identifier and the same
	 * structure (list of attributes).
	 * The changes can be either on the attribute values, or on the geometry.
	 * 
	 * @param fIni The initial version
	 * @param fFin The final version
	 * @param attributesToIgnore
	 * @param res The geometrical resolution of the dataset. Geometrical changes below this value will be ignored.
	 * @return A feature representing the changes.
	 */
	public static Feature compare(Feature fIni, Feature fFin, double res, List<String> attributesToIgnore) {
		boolean attChanged = false, geomChanged = false;
		Feature change = new Feature();

		//compare attribute values
		int nb = 0;
		for(String att : fIni.getAttributes().keySet()) {
			Object attIni = fIni.getAttribute(att);
			Object attFin = fFin.getAttribute(att);
			if((attributesToIgnore!=null && attributesToIgnore.contains(att))
					|| attIni.equals(attFin)) {
				change.setAttribute(att, null);
			} else {
				attChanged = true;
				change.setAttribute(att, attFin);
				nb++;
			}
		}
		//compare geometries
		if( (res>0 && new HausdorffDistance(fIni.getDefaultGeometry(), fFin.getDefaultGeometry()).getDistance() <= res)
				|| (res<=0 && ! fIni.getDefaultGeometry().equalsTopo(fFin.getDefaultGeometry())))
			geomChanged = true;

		//no change: return null
		if(!attChanged && !geomChanged) return null;

		//set id
		change.setID(fFin.getID());
		change.setAttribute("ch_id", fFin.getID());

		//set geometry
		change.setDefaultGeometry(fFin.getDefaultGeometry());

		//set attribute on change
		change.setAttribute("change", (geomChanged?"G":"") + (attChanged?"A"+nb:""));

		return change;
	}





	/**
	 * Detect among some changes the ones are are unecessary:
	 * The deletion and insertion of features with very similar geometries.
	 * This happen when id stability is not strictly followed.
	 * 
	 * @param changes The changes to check.
	 * @param resolution The spatial resolution value to consider two geometries as similar.
	 * @return The collection of unecessary changes.
	 */
	public static Collection<Feature> findIdStabilityIssues(Collection<Feature> changes, double resolution) {

		//copy list of changes, keeping only deletions and insertions.
		ArrayList<Feature> chs = new ArrayList<>();
		for(Feature ch : changes) {
			String ct = ch.getAttribute("change").toString();
			if("I".equals(ct) || "D".equals(ct)) chs.add(ch);
		}

		//build spatial index of the selected changes
		Quadtree ind = FeatureUtil.getQuadtree(chs);

		Collection<Feature> out = new ArrayList<>();
		while(chs.size()>0) {
			//get first element
			Feature ch = chs.get(0);
			Geometry g = ch.getDefaultGeometry();
			chs.remove(0);
			boolean b = ind.remove(g.getEnvelopeInternal(), ch);
			if(!b) LOGGER.warn("Pb");

			//get change type
			String ct = ch.getAttribute("change").toString();

			//get try to find other change
			Feature ch2 = null;
			Envelope env = g.getEnvelopeInternal(); env.expandBy(2*resolution);
			for(Object cho_ : ind.query(env)) {
				Feature ch_ = (Feature) cho_;
				Geometry g_ = ch_.getDefaultGeometry();

				//check change type: it as to be different
				if(ct.equals(ch_.getAttribute("change").toString())) continue;

				//check geometry similarity
				if( (resolution>0 && new HausdorffDistance(g, g_).getDistance() <= resolution)
						|| ( resolution<=0 && g.equalsTopo(g_) )) {
					ch2 = ch_;
					break;
				}
			}

			//no other similar change found: go to next
			if(ch2 == null) continue;

			//remove
			chs.remove(ch2);
			b = ind.remove(ch.getDefaultGeometry().getEnvelopeInternal(), ch2);
			if(!b) LOGGER.warn("Pb");

			out.add(ch);
			out.add(ch2);
		}		
		return out;
	}





	/**
	 * A collection of features representing the geometrical changes.
	 * For each feature whose geometry has changed, a segment
	 * representing the hausdorff distance between the initial and
	 * final geometries is created. This feature allows assessing the
	 * magnitude of the geometrical change.
	 */
	private Collection<Feature> hausdorffGeomChanges = null;
	public Collection<Feature> getHausdorffGeomChanges() {
		if(hausdorffGeomChanges==null) computeGeometryChanges();
		return hausdorffGeomChanges;
	}

	/**
	 * Feature representing the geometrical parts which have been
	 * deleted/inserted between the two versions.
	 */
	private Collection<Feature> geomChanges = null;
	public Collection<Feature> getGeomChanges() {
		if(geomChanges==null) computeGeometryChanges();
		return geomChanges;
	}


	/**
	 * Create the features representing the geometrical changes.
	 */
	private void computeGeometryChanges() {

		hausdorffGeomChanges = new ArrayList<Feature>();
		geomChanges = new ArrayList<Feature>();

		for(Feature ch : getChanges()) {

			//consider only geometry changes
			String ct = ch.getAttribute("change").toString();
			if(!ct.contains("G")) continue;

			//get initial and final geometries
			String id = ch.getID();
			Geometry gIni = indIni.get(id).getDefaultGeometry();
			Geometry gFin = indFin.get(id).getDefaultGeometry();

			//hausdorff distance
			HausdorffDistance hd = new HausdorffDistance(gIni, gFin);
			Feature hdf = new Feature();
			hdf.setDefaultGeometry(hd.toGeom());
			hdf.setAttribute("ch_id", id);
			hdf.setAttribute("hdist", hd.getDistance());
			hausdorffGeomChanges.add(hdf);

			//compute added and removed parts
			Geometry gD = null;
			try {
				gD = gIni.difference(gFin);
			} catch (Exception e) {
				LOGGER.warn(e.getMessage());
			}
			if(gD!=null && !gD.isEmpty()) {
				Feature f = new Feature();
				//TODO ensure geometry type
				f.setDefaultGeometry(JTSGeomUtil.toMulti(gD));
				f.setAttribute("ch_id", id);
				f.setAttribute("type", "D");
				geomChanges.add(f);
			}

			Geometry gI = null;
			try {
				gI = gFin.difference(gIni);
			} catch (Exception e) {
				LOGGER.warn(e.getMessage());
			}
			if(gI!=null && !gI.isEmpty()) {
				Feature f = new Feature();
				f.setDefaultGeometry(JTSGeomUtil.toMulti(gI));
				f.setAttribute("ch_id", id);
				f.setAttribute("type", "I");
				geomChanges.add(f);
			}

		}
	}








	/**
	 * Return the changes from a version of a dataset to another one.
	 * 
	 * @param fsIni The initial dataset
	 * @param fsFin The final dataset
	 * @return The changes
	 */
	public static Collection<Feature> getChanges(Collection<Feature> fsIni, Collection<Feature> fsFin) {
		return new ChangeDetection(fsIni, fsFin).getChanges();
	}

	/**
	 * Analyse the differences between two datasets to check wether they are identical.
	 * 
	 * @param fs1 The first dataset
	 * @param fs2 The second dataset
	 * @return
	 */
	public static boolean equals(Collection<Feature> fs1, Collection<Feature> fs2) {
		return new ChangeDetection(fs1, fs2).getChanges().size() == 0;
	}


	public static void applyChanges(Collection<Feature> fs, Collection<Feature> changes) {

		//index input features
		HashMap<String, Feature> ind = FeatureUtil.index(fs, null);

		//go through changes
		for(Feature ch : changes) {

			//retrieve type of change and change/feature id
			String ct = ch.getAttribute("change").toString();
			String id = ch.getAttribute("ch_id").toString();

			//new feature insertion
			if("I".equals(ct)) {
				LOGGER.info("New feature inserted. id="+id);
				Feature f = FeatureUtil.copy(ch);
				f.getAttributes().remove("change");
				f.getAttributes().remove("ch_id");
				fs.add(f);
				continue;
			}

			//retrieve feature to be changed
			Feature f = ind.get(id);

			if(f == null) {
				LOGGER.warn("Could not handle change for feature with id="+id+". Feature not present in initial dataset.");
				continue;
			}

			//feature deletion
			if("D".equals(ct)) {
				boolean b = fs.remove(f);
				if(!b) LOGGER.warn("Could not remove feature. id="+id);
				LOGGER.info("Feature deleted. id="+id);
				continue;
			}

			LOGGER.info("Feature changed. id="+id+". change="+ct+".");

			//case of geometry change
			if(ct.contains("G"))
				f.setDefaultGeometry(ch.getDefaultGeometry());

			//if no attribute change, continue
			if(!ct.contains("A")) continue;

			//get number of attribute changes
			int nbAtt = Integer.parseInt( ct.replace("G", "").replace("A", "") );

			//change attributes
			int nbAtt_ = 0;
			for(Entry<String,Object> att : ch.getAttributes().entrySet()) {

				if("change".equals(att.getKey())) continue;
				if("ch_id".equals(att.getKey())) continue;
				if(att.getValue() == null) continue;

				f.setAttribute(att.getKey(), att.getValue());
				nbAtt_++;
			}

			//check number of attributes changed is as expected
			if(nbAtt != nbAtt_)
				LOGGER.warn("Unexpected number of attribute changes ("+nbAtt_+" instead of "+nbAtt+") for feature id="+id+".");
		}

	}






	//TODO extract that into tests
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

		//LOGGER.info("check ids:");
		//LOGGER.info( FeatureUtil.checkIdentfier(fsIni, "id") );
		//LOGGER.info( FeatureUtil.checkIdentfier(fsFin, "id") );

		ChangeDetection cd = new ChangeDetection(fsIni, fsFin);
		//cd.setAttributesToIgnore("id","name");

		Collection<Feature> unchanged = cd.getUnchanged();
		LOGGER.info("unchanged = "+unchanged.size());
		LOGGER.info( FeatureUtil.checkIdentfier(unchanged, null) );
		Collection<Feature> changes = cd.getChanges();
		LOGGER.info("changes = "+changes.size());
		LOGGER.info( FeatureUtil.checkIdentfier(changes, null) );
		Collection<Feature> hfgeoms = cd.getHausdorffGeomChanges();
		LOGGER.info("hfgeoms = "+hfgeoms.size());
		LOGGER.info( FeatureUtil.checkIdentfier(hfgeoms, null) );
		Collection<Feature> geomch = cd.getGeomChanges();
		LOGGER.info("geomch = "+geomch.size());
		LOGGER.info( FeatureUtil.checkIdentfier(geomch, null) );
		Collection<Feature> sus = findIdStabilityIssues(changes, 50);
		LOGGER.info("suspect changes = "+sus.size());
		LOGGER.info( FeatureUtil.checkIdentfier(sus, null) );

		CoordinateReferenceSystem crs = GeoPackageUtil.getCRS(path+"ini.gpkg");
		GeoPackageUtil.save(changes, outpath+"changes.gpkg", crs, true);
		GeoPackageUtil.save(unchanged, outpath+"unchanged.gpkg", crs, true);
		GeoPackageUtil.save(hfgeoms, outpath+"hfgeoms.gpkg", crs, true);
		GeoPackageUtil.save(geomch, outpath+"geomch.gpkg", crs, true);
		GeoPackageUtil.save(sus, outpath+"suspects.gpkg", crs, true);

		LOGGER.info("--- Test equality");
		LOGGER.info( equals(fsIni, fsFin) );
		LOGGER.info( equals(fsFin, fsIni) );
		LOGGER.info( equals(fsIni, fsIni) );
		LOGGER.info( equals(fsFin, fsFin) );

		LOGGER.info("--- Test change application");
		applyChanges(fsIni, changes);
		LOGGER.info( equals(fsIni, fsFin) );

		LOGGER.info("End");
	}

}
