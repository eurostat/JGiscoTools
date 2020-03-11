/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.geodiff;

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
public class DifferenceDetection {
	private final static Logger LOGGER = LogManager.getLogger(DifferenceDetection.class.getName());

	/**
	 * The dataset in its initial version.
	 */
	private Collection<Feature> fsIni;

	/**
	 * The dataset in its final version.
	 */
	private Collection<Feature> fsFin;

	/**
	 * The geometrical resolution of the dataset. Geometrical differences below this value will be ignored.
	 */
	private double resolution = -1;
	/**
	 * @return The geometrical resolution of the dataset. Geometrical differences below this value will be ignored.
	 */
	public double getResolution() { return resolution; }

	/**
	 * @param fsIni The dataset in its initial version.
	 * @param fsFin The dataset in its final version.
	 * @param resolution The geometrical resolution of the dataset. Geometrical differences below this value will be ignored.
	 */
	public DifferenceDetection(Collection<Feature> fsIni, Collection<Feature> fsFin, double resolution) {
		this.fsIni = fsIni;
		this.fsFin = fsFin;
		this.resolution = resolution;
	}
	/**
	 * @param fsIni The dataset in its initial version.
	 * @param fsFin The dataset in its final version.
	 */
	public DifferenceDetection(Collection<Feature> fsIni, Collection<Feature> fsFin) {
		this(fsIni, fsFin, -1);
	}

	private Collection<Feature> differences = null;
	private Collection<Feature> identical = null;

	/**
	 * @return The differences between the initial and final versions.
	 */
	public Collection<Feature> getDifferences() {
		if(this.differences == null) compare();
		return this.differences;
	}

	/**
	 * @return The features that have are identical.
	 */
	public Collection<Feature> getIdentical() {
		if(this.identical == null) compare();
		return this.identical;
	}


	/**
	 * The attributes to ignore when comparing the differences of a feature.
	 * If one or several values of these attributes only is different,
	 * then the feature is condidered has identical. 
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
	 * Compare both datasets. Populate the differences.
	 */
	private void compare() {
		this.differences = new ArrayList<>();
		this.identical = new ArrayList<>();

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

			//compute differences between them
			Feature ch = compare(fIni, fFin, getResolution(), attributesToIgnore );

			//both versions identical. No difference detected.
			if(ch == null) identical.add(fFin);

			//difference
			else differences.add(ch);
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
			ch.setAttribute("GeoDiff", "D");
			differences.add(ch);
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
			ch.setAttribute("GeoDiff", "I");
			differences.add(ch);
		}

	}

	/**
	 * Compare two versions of the same feature.
	 * Both features are expected to have the same identifier and the same
	 * structure (list of attributes).
	 * The differences can be either on the attribute values, or on the geometry.
	 * 
	 * @param fIni The initial version
	 * @param fFin The final version
	 * @param attributesToIgnore
	 * @param resolution The geometrical resolution of the dataset. Geometrical differences below this value will be ignored.
	 * @return A feature representing the difference.
	 */
	public static Feature compare(Feature fIni, Feature fFin, double resolution, List<String> attributesToIgnore) {
		boolean attDifference = false, geomDifference = false;
		Feature difference = new Feature();

		//compare attribute values
		int nb = 0;
		for(String att : fIni.getAttributes().keySet()) {
			Object attIni = fIni.getAttribute(att);
			Object attFin = fFin.getAttribute(att);
			if((attributesToIgnore!=null && attributesToIgnore.contains(att))
					|| attIni.equals(attFin)) {
				difference.setAttribute(att, null);
			} else {
				attDifference = true;
				difference.setAttribute(att, attFin);
				nb++;
			}
		}
		//compare geometries
		if( (resolution>0 && new HausdorffDistance(fIni.getGeometry(), fFin.getGeometry()).getDistance() > resolution)
				|| (resolution<=0 && ! fIni.getGeometry().equalsTopo(fFin.getGeometry())))
			geomDifference = true;

		//no difference: return null
		if(!attDifference && !geomDifference) return null;

		//set id
		difference.setID(fFin.getID());
		difference.setAttribute("ch_id", fFin.getID());

		//set geometry
		difference.setGeometry(fFin.getGeometry());

		//set attribute on difference
		difference.setAttribute("GeoDiff", (geomDifference?"G":"") + (attDifference?"A"+nb:""));

		return difference;
	}





	/** */
	private Collection<Feature> hausdorffGeomDifferences = null;
	/**
	 * A collection of features representing the geometrical differences.
	 * For each feature whose geometry is different, a segment
	 * representing the hausdorff distance between the initial and
	 * final geometries is created. This feature allows assessing the
	 * magnitude of the geometrical difference.
	 * @return
	 */
	public Collection<Feature> getHausdorffGeomDifferences() {
		if(hausdorffGeomDifferences==null) computeGeometryDifferences();
		return hausdorffGeomDifferences;
	}

	/** */
	private Collection<Feature> geomDifferences = null;
	/**
	 * Features representing the geometrical parts which have been
	 * deleted/inserted between the two versions.
	 * 
	 * @return
	 */
	public Collection<Feature> getGeomDifferences() {
		if(geomDifferences==null) computeGeometryDifferences();
		return geomDifferences;
	}

	/**
	 * Create the features representing the geometrical differences.
	 */
	private void computeGeometryDifferences() {

		hausdorffGeomDifferences = new ArrayList<Feature>();
		geomDifferences = new ArrayList<Feature>();
		int geomType = JTSGeomUtil.getGeomBigType(getDifferences().iterator().next().getGeometry());

		for(Feature ch : getDifferences()) {

			//consider only geometry differences
			String ct = ch.getAttribute("GeoDiff").toString();
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
			hausdorffGeomDifferences.add(hdf);

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
					f.setAttribute("GeoDiff", "D");
					geomDifferences.add(f);
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
					f.setAttribute("GeoDiff", "I");
					geomDifferences.add(f);
				}
			}

		}
	}







	/**
	 * Detect among some differences the ones are are unecessary:
	 * The deletion and insertion of features with very similar geometries.
	 * This happen when id stability is not strictly followed.
	 * 
	 * @param differences The differences to check.
	 * @param resolution The spatial resolution value to consider two geometries as similar.
	 * @return The collection of unecessary differences.
	 */
	public static Collection<Feature> findIdStabilityIssues(Collection<Feature> differences, double resolution) {

		//copy list of differences, keeping only deletions and insertions.
		ArrayList<Feature> chs = new ArrayList<>();
		for(Feature ch : differences) {
			String ct = ch.getAttribute("GeoDiff").toString();
			if("I".equals(ct) || "D".equals(ct)) chs.add(ch);
		}

		//build spatial index of the selected differences
		Quadtree ind = FeatureUtil.getQuadtree(chs);

		Collection<Feature> out = new ArrayList<>();
		while(chs.size()>0) {
			//get first element
			Feature ch = chs.get(0);
			Geometry g = ch.getGeometry();
			chs.remove(0);
			boolean b = ind.remove(g.getEnvelopeInternal(), ch);
			if(!b) LOGGER.warn("Pb");

			//get difference type
			String ct = ch.getAttribute("GeoDiff").toString();

			//try to find other difference
			Feature ch2 = null;
			Envelope env = g.getEnvelopeInternal(); env.expandBy(2*resolution);
			for(Object cho_ : ind.query(env)) {
				Feature ch_ = (Feature) cho_;
				Geometry g_ = ch_.getGeometry();

				//check difference type: it as to be different
				if(ct.equals(ch_.getAttribute("GeoDiff").toString())) continue;

				//check geometry similarity
				if( (resolution>0 && new HausdorffDistance(g, g_).getDistance() <= resolution)
						|| ( resolution<=0 && g.equalsTopo(g_) )) {
					ch2 = ch_;
					break;
				}
			}

			//no other similar difference found: go to next
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
	 * Return the differences from a version of a dataset to another one.
	 * 
	 * @param fsIni The initial dataset
	 * @param fsFin The final dataset
	 * @param resolution The geometrical resolution of the dataset. Geometrical differences below this value will be ignored.
	 * @return The differences
	 */
	public static Collection<Feature> getDifferences(Collection<Feature> fsIni, Collection<Feature> fsFin, double resolution) {
		return new DifferenceDetection(fsIni, fsFin, resolution).getDifferences();
	}

	/**
	 * Analyse the differences between two datasets to check wether they are identical.
	 * 
	 * @param fs1 The first dataset
	 * @param fs2 The second dataset
	 * @param resolution The geometrical resolution of the dataset. Geometrical differences below this value will be ignored.
	 * @return
	 */
	public static boolean equals(Collection<Feature> fs1, Collection<Feature> fs2, double resolution) {
		return new DifferenceDetection(fs1, fs2, resolution).getDifferences().size() == 0;
	}

	/**
	 * Analyse the differences between two datasets to check wether they are identical.
	 * 
	 * @param fs1 The first dataset
	 * @param fs2 The second dataset
	 * @return
	 */
	public static boolean equals(Collection<Feature> fs1, Collection<Feature> fs2) {
		return new DifferenceDetection(fs1, fs2).getDifferences().size() == 0;
	}

	/**
	 * Apply changes to features.
	 * 
	 * @param fs The features to change, in their initial state.
	 * @param differences The changes to apply.
	 */
	public static void applyChanges(Collection<Feature> fs, Collection<Feature> differences) {

		//index input features
		HashMap<String, Feature> index = FeatureUtil.index(fs, null);

		//go through differences
		for(Feature ch : differences) {

			//retrieve type of difference and change/feature id
			String ct = ch.getAttribute("GeoDiff").toString();
			String id = ch.getAttribute("ch_id").toString();

			//new feature insertion
			if("I".equals(ct)) {
				LOGGER.info("New feature inserted. id=" + id);
				Feature f = FeatureUtil.copy(ch);
				f.getAttributes().remove("GeoDiff");
				f.getAttributes().remove("ch_id");
				fs.add(f);
				continue;
			}

			//retrieve feature to be changed
			Feature f = index.get(id);

			if(f == null) {
				LOGGER.warn("Could not handle change for feature with id=" + id + ". Feature not present in initial dataset.");
				continue;
			}

			//feature deletion
			if("D".equals(ct)) {
				boolean b = fs.remove(f);
				if(!b) LOGGER.warn("Could not remove feature. id=" + id);
				LOGGER.info("Feature deleted. id=" + id);
				continue;
			}

			LOGGER.info("Feature changed. id=" + id + ". change=" + ct + ".");
			applyChange(f, ch, ct);
		}

	}

	/**
	 * Apply a change to a feature.
	 * 
	 * @param f The feature to change, in its initial state.
	 * @param diff The change. NB: this change cannot be a deletion or an insertion
	 * @param ct The type of change, if known.
	 */
	public static void applyChange(Feature f, Feature diff, String ct) {

		//retrieve change type if necessary
		if(ct==null || ct.isEmpty()) ct = diff.getAttribute("GeoDiff").toString();

		//check change type
		if("I".equals(ct) || "D".equals(ct)) {
			LOGGER.warn("Unexpected type of change ("+ct+" instead of GAX) for feature id="+f.getID()+".");
			return;
		}

		//case of geometry change
		if(ct.contains("G"))
			f.setGeometry(diff.getGeometry());

		//if no attribute change, continue
		if(!ct.contains("A")) return;

		//get number of attribute changes
		int nbAtt = Integer.parseInt( ct.replace("G", "").replace("A", "") );

		//change attributes
		int nbAtt_ = 0;
		for(Entry<String,Object> att : diff.getAttributes().entrySet()) {

			if("GeoDiff".equals(att.getKey())) continue;
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
