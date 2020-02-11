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

import eu.europa.ec.eurostat.jgiscotools.algo.distances.HausdorffDistance;
import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.FeatureUtil;
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
	 * The geometrical resolution of the dataset. Geometrical changes below this value will be ignored.
	 */
	private double resolution = -1;
	/**
	 * @return The geometrical resolution of the dataset. Geometrical changes below this value will be ignored.
	 */
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
	/**
	 * @param fsIni The dataset in its initial version.
	 * @param fsFin The dataset in its final version.
	 */
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
	/**
	 * @return
	 */
	public List<String> getAttributesToIgnore() { return attributesToIgnore; }
	/**
	 * @param attributesToIgnore
	 */
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
		if( (res>0 && new HausdorffDistance(fIni.getGeometry(), fFin.getGeometry()).getDistance() > res)
				|| (res<=0 && ! fIni.getGeometry().equalsTopo(fFin.getGeometry())))
			geomChanged = true;

		//no change: return null
		if(!attChanged && !geomChanged) return null;

		//set id
		change.setID(fFin.getID());
		change.setAttribute("ch_id", fFin.getID());

		//set geometry
		change.setGeometry(fFin.getGeometry());

		//set attribute on change
		change.setAttribute("change", (geomChanged?"G":"") + (attChanged?"A"+nb:""));

		return change;
	}





	/** */
	private Collection<Feature> hausdorffGeomChanges = null;
	/**
	 * A collection of features representing the geometrical changes.
	 * For each feature whose geometry has changed, a segment
	 * representing the hausdorff distance between the initial and
	 * final geometries is created. This feature allows assessing the
	 * magnitude of the geometrical change.
	 * @return
	 */
	public Collection<Feature> getHausdorffGeomChanges() {
		if(hausdorffGeomChanges==null) computeGeometryChanges();
		return hausdorffGeomChanges;
	}

	/** */
	private Collection<Feature> geomChanges = null;
	/**
	 * Features representing the geometrical parts which have been
	 * deleted/inserted between the two versions.
	 * 
	 * @return
	 */
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
		int geomType = JTSGeomUtil.getGeomBigType(getChanges().iterator().next().getGeometry());

		for(Feature ch : getChanges()) {

			//consider only geometry changes
			String ct = ch.getAttribute("change").toString();
			if(!ct.contains("G")) continue;

			//get initial and final geometries
			String id = ch.getID();
			Geometry gIni = indIni.get(id).getGeometry();
			Geometry gFin = indFin.get(id).getGeometry();

			//hausdorff distance
			HausdorffDistance hd = new HausdorffDistance(gIni, gFin);
			Feature hdf = new Feature();
			hdf.setGeometry(hd.toGeom());
			hdf.setAttribute("ch_id", id);
			hdf.setAttribute("hdist", hd.getDistance());
			hausdorffGeomChanges.add(hdf);

			//compute added parts
			{
				Geometry gD = null;
				try {
					gD = gIni.difference(gFin);
				} catch (Exception e) {
					LOGGER.warn(e.getMessage());
				}
				if(gD!=null && !gD.isEmpty()) {
					Feature f = new Feature();
					f.setGeometry(JTSGeomUtil.extract(gD, geomType));
					f.setAttribute("ch_id", id);
					f.setAttribute("change", "D");
					geomChanges.add(f);
				}
			}

			//compute removed parts
			{
				Geometry gI = null;
				try {
					gI = gFin.difference(gIni);
				} catch (Exception e) {
					LOGGER.warn(e.getMessage());
				}
				if(gI!=null && !gI.isEmpty()) {
					Feature f = new Feature();
					f.setGeometry(JTSGeomUtil.extract(gI, geomType));
					f.setAttribute("ch_id", id);
					f.setAttribute("change", "I");
					geomChanges.add(f);
				}
			}

		}
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
			Geometry g = ch.getGeometry();
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
				Geometry g_ = ch_.getGeometry();

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
			b = ind.remove(ch.getGeometry().getEnvelopeInternal(), ch2);
			if(!b) LOGGER.warn("Pb");

			out.add(ch);
			out.add(ch2);
		}		
		return out;
	}






	/**
	 * Return the changes from a version of a dataset to another one.
	 * 
	 * @param fsIni The initial dataset
	 * @param fsFin The final dataset
	 * @param resolution The geometrical resolution of the dataset. Geometrical changes below this value will be ignored.
	 * @return The changes
	 */
	public static Collection<Feature> getChanges(Collection<Feature> fsIni, Collection<Feature> fsFin, double resolution) {
		return new ChangeDetection(fsIni, fsFin, resolution).getChanges();
	}

	/**
	 * Analyse the differences between two datasets to check wether they are identical.
	 * 
	 * @param fs1 The first dataset
	 * @param fs2 The second dataset
	 * @param resolution The geometrical resolution of the dataset. Geometrical changes below this value will be ignored.
	 * @return
	 */
	public static boolean equals(Collection<Feature> fs1, Collection<Feature> fs2, double resolution) {
		return new ChangeDetection(fs1, fs2, resolution).getChanges().size() == 0;
	}

	/**
	 * Analyse the differences between two datasets to check wether they are identical.
	 * 
	 * @param fs1 The first dataset
	 * @param fs2 The second dataset
	 * @return
	 */
	public static boolean equals(Collection<Feature> fs1, Collection<Feature> fs2) {
		return equals(fs1, fs2, -1);
	}

	/**
	 * Apply changes to features.
	 * 
	 * @param fs The features to change, in their initial state.
	 * @param changes The changes to apply.
	 */
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
			applyChange(f, ch, ct);
		}

	}

	/**
	 * Apply a change to a feature.
	 * 
	 * @param f The feature to change, in its initial state.
	 * @param ch The change. NB: this change cannot be a deletion or an insertion
	 * @param ct The type of change, if known.
	 */
	public static void applyChange(Feature f, Feature ch, String ct) {

		//retrieve change type if necessary
		if(ct==null || ct.isEmpty()) ct = ch.getAttribute("change").toString();

		//check change type
		if("I".equals(ct) || "D".equals(ct)) {
			LOGGER.warn("Unexpected type of change ("+ct+" instead of GAX) for feature id="+f.getID()+".");
			return;
		}

		//case of geometry change
		if(ct.contains("G"))
			f.setGeometry(ch.getGeometry());

		//if no attribute change, continue
		if(!ct.contains("A")) return;

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
			LOGGER.warn("Unexpected number of attribute changes ("+nbAtt_+" instead of "+nbAtt+") for feature id="+f.getID()+".");		
	}

}
