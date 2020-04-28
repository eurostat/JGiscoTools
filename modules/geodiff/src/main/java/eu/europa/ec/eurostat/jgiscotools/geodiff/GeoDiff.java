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

import eu.europa.ec.eurostat.jgiscotools.algo.base.HausdorffDistance;
import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.FeatureUtil;
import eu.europa.ec.eurostat.jgiscotools.feature.JTSGeomUtil;
import eu.europa.ec.eurostat.jgiscotools.feature.SimpleFeatureUtil;

/**
 * 
 * Analyse the differences between two versions of a dataset.
 * Compute the difference in GeoDiff format.
 * Note: both datasets are suppose to have a shared stable identifier.
 * 
 * @author julien Gaffuri
 *
 */
public class GeoDiff {
	private final static Logger LOGGER = LogManager.getLogger(GeoDiff.class.getName());

	//TODO handle case when no common identifier exists ?

	/**
	 * The dataset in the first version.
	 */
	private Collection<Feature> fs1;

	/**
	 * The dataset in the second version.
	 */
	private Collection<Feature> fs2;

	/**
	 * The geometrical resolution of the dataset. Geometrical differences below this value will be ignored.
	 */
	private double resolution = -1;
	/**
	 * @return The geometrical resolution of the dataset. Geometrical differences below this value will be ignored.
	 */
	public double getResolution() { return resolution; }

	/**
	 * @param fs1 The dataset in the first version.
	 * @param fs2 The dataset in the second version.
	 * @param resolution The geometrical resolution of the dataset. Geometrical differences below this value will be ignored.
	 */
	public GeoDiff(Collection<Feature> fs1, Collection<Feature> fs2, double resolution) {
		this.fs1 = fs1;
		this.fs2 = fs2;

		if(this.fs1.size() == 0)
			LOGGER.warn("Dataset in first version is empty");
		if(this.fs2.size() == 0)
			LOGGER.warn("Dataset in second version is empty");

		//TODO check attributes in common
		//HashMap<String, Class<?>> attsCom = getCommonAttributes(this.fs1, this.fs2);
		//TODO

		this.resolution = resolution;
	}
	/**
	 * @param fs1 The dataset in the first version.
	 * @param fs2 The dataset in the second version.
	 */
	public GeoDiff(Collection<Feature> fs1, Collection<Feature> fs2) {
		this(fs1, fs2, -1);
	}

	private Collection<Feature> differences = null;
	private Collection<Feature> identical = null;

	/**
	 * @return The differences between the two versions.
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
	private HashMap<String,Feature> ind1, ind2;

	/**
	 * Compare both datasets. Populate the differences.
	 */
	private void compare() {
		this.differences = new ArrayList<>();
		this.identical = new ArrayList<>();

		//list id values
		Collection<String> ids1 = FeatureUtil.getIdValues(fs1, null);
		Collection<String> ids2 = FeatureUtil.getIdValues(fs2, null);

		//index features by ids
		ind1 = FeatureUtil.index(fs1, null);
		ind2 = FeatureUtil.index(fs2, null);

		//find features present in both datasets and compare them

		//compute intersection of id sets
		Collection<String> idsInter = new ArrayList<>(ids1);
		idsInter.retainAll(ids2);

		for(String id : idsInter) {
			//get two corresponding features
			Feature f1 = ind1.get(id);
			Feature f2 = ind2.get(id);

			//compute differences between them
			Feature ch = compare(f1, f2, getResolution(), attributesToIgnore );

			//both versions identical. No difference detected.
			if(ch == null) identical.add(f2);

			//difference
			else differences.add(ch);
		}

		//find deleted features

		//compute difference: 1 - 2
		Collection<String> idsDiff = new ArrayList<>(ids1);
		idsDiff.removeAll(ids2);

		//retrieve deleted features
		for(String id : idsDiff) {
			Feature f = ind1.get(id);
			Feature ch = FeatureUtil.copy(f);
			ch.setAttribute("GeoDiff", "D");
			differences.add(ch);
		}

		//find inserted features

		//compute difference: 2 - 1
		idsDiff = new ArrayList<>(ids2);
		idsDiff.removeAll(ids1);

		//retrieve inserted features
		for(String id : idsDiff) {
			Feature f = ind2.get(id);
			Feature ch = FeatureUtil.copy(f);
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
	 * @param fs1 The dataset in the first version
	 * @param fs2 The dataset in the second version
	 * @param attributesToIgnore
	 * @param resolution The geometrical resolution of the dataset. Geometrical differences below this value will be ignored.
	 * @return A feature representing the difference.
	 */
	public static Feature compare(Feature fs1, Feature fs2, double resolution, List<String> attributesToIgnore) {
		boolean attDifference = false, geomDifference = false;
		Feature difference = new Feature();

		//compare attribute values
		int nb = 0;
		for(String att : fs1.getAttributes().keySet()) {
			Object att1 = fs1.getAttribute(att);
			Object att2 = fs2.getAttribute(att);
			if((attributesToIgnore!=null && attributesToIgnore.contains(att))
					|| att1.equals(att2)) {
				difference.setAttribute(att, null);
			} else {
				attDifference = true;
				difference.setAttribute(att, att2);
				nb++;
			}
		}
		//compare geometries
		if( (resolution>0 && new HausdorffDistance(fs1.getGeometry(), fs2.getGeometry()).getDistance() > resolution)
				|| (resolution<=0 && ! fs1.getGeometry().equalsTopo(fs2.getGeometry())))
			geomDifference = true;

		//no difference: return null
		if(!attDifference && !geomDifference) return null;

		//set id
		difference.setID(fs2.getID());

		//set geometry
		difference.setGeometry(fs2.getGeometry());

		//set attribute on difference
		difference.setAttribute("GeoDiff", (geomDifference?"G":"") + (attDifference?"A"+nb:""));

		return difference;
	}





	/** */
	private Collection<Feature> hausdorffGeomDifferences = null;
	/**
	 * A collection of features representing the geometrical differences.
	 * For each feature whose geometry is different, a segment
	 * representing the hausdorff distance between the first and
	 * second version geometries is created. This feature allows assessing the
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

			//get first and second version geometries
			String id = ch.getID();
			Geometry g1 = ind1.get(id).getGeometry();
			Geometry g2 = ind2.get(id).getGeometry();

			//hausdorff distance
			HausdorffDistance hd = new HausdorffDistance(g1, g2);
			Feature hdf = new Feature();
			hdf.setGeometry(hd.toGeom());
			hdf.setID(id);
			hdf.setAttribute("id", id);
			hdf.setAttribute("hdist", hd.getDistance());
			hausdorffGeomDifferences.add(hdf);

			//compute added parts
			{
				Geometry gD = null;
				try {
					gD = g1.difference(g2);
				} catch (Exception e) {
					LOGGER.warn(e.getMessage());
				}
				if(gD!=null && !gD.isEmpty()) {
					Feature f = new Feature();
					f.setGeometry(JTSGeomUtil.extract(gD, geomType));
					f.setID(id+"_D");
					f.setAttribute("id", id+"_D");
					f.setAttribute("GeoDiff", "D");
					geomDifferences.add(f);
				}
			}

			//compute removed parts
			{
				Geometry gI = null;
				try {
					gI = g2.difference(g1);
				} catch (Exception e) {
					LOGGER.warn(e.getMessage());
				}
				if(gI!=null && !gI.isEmpty()) {
					Feature f = new Feature();
					f.setGeometry(JTSGeomUtil.extract(gI, geomType));
					f.setID(id+"_I");
					f.setAttribute("id", id+"_I");
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
	 * @param fs1 The dataset first version
	 * @param fs2 The dataset second version
	 * @param resolution The geometrical resolution of the dataset. Geometrical differences below this value will be ignored.
	 * @return The differences
	 */
	public static Collection<Feature> getDifferences(Collection<Feature> fs1, Collection<Feature> fs2, double resolution) {
		return new GeoDiff(fs1, fs2, resolution).getDifferences();
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
		return new GeoDiff(fs1, fs2, resolution).getDifferences().size() == 0;
	}

	/**
	 * Analyse the differences between two datasets to check wether they are identical.
	 * 
	 * @param fs1 The first dataset
	 * @param fs2 The second dataset
	 * @return
	 */
	public static boolean equals(Collection<Feature> fs1, Collection<Feature> fs2) {
		return new GeoDiff(fs1, fs2).getDifferences().size() == 0;
	}

	/**
	 * Apply changes to features.
	 * 
	 * @param fs The features to change, in their initial version.
	 * @param differences The changes to apply.
	 */
	public static void applyChanges(Collection<Feature> fs, Collection<Feature> differences) {

		//index input features
		HashMap<String, Feature> index = FeatureUtil.index(fs, null);

		//go through differences
		for(Feature ch : differences) {

			//retrieve type of difference and change/feature id
			String ct = ch.getAttribute("GeoDiff").toString();
			String id = ch.getID();

			//new feature insertion
			if("I".equals(ct)) {
				LOGGER.info("New feature inserted. id=" + id);
				Feature f = FeatureUtil.copy(ch);
				f.getAttributes().remove("GeoDiff");
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
	 * @param f The feature to change, in its initial version.
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
			if(att.getValue() == null) continue;

			f.setAttribute(att.getKey(), att.getValue());
			nbAtt_++;
		}

		//check number of attributes changed is as expected
		if(nbAtt != nbAtt_)
			LOGGER.warn("Unexpected number of attribute changes ("+nbAtt_+" instead of "+nbAtt+") for feature id="+f.getID()+".");		
	}

	public static HashMap<String,Class<?>> getCommonAttributes(Collection<Feature> fs1, Collection<Feature> fs2) {
		//get schemas of both collections
		HashMap<String,Class<?>> types1 = SimpleFeatureUtil.getAttributeGeomTypes(fs1);
		HashMap<String,Class<?>> types2 = SimpleFeatureUtil.getAttributeGeomTypes(fs1);

		//get intersection
		HashMap<String, Class<?>> out = new HashMap<>();
		for(Entry<String, Class<?>> e1 : types1.entrySet()) {
			Class<?> v2 = types2.get(e1.getKey());
			if(v2 == null) continue;
			if(v2 != e1.getValue()) continue;
			out.put(e1.getKey(), e1.getValue());
		}

		return out;
	}

}
