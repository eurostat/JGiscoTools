/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.algo.matching;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.FeatureUtil;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;
import eu.europa.ec.eurostat.jgiscotools.util.Util;

/**
 * @author julien Gaffuri
 *
 */
public class LevenshteinMatching {
	private final static Logger LOGGER = Logger.getLogger(LevenshteinMatching.class.getName());

	/**
	 * Get Levenshtein distance between two strings.
	 * 
	 * @param s1
	 * @param s2
	 * @param toLowerCase
	 * @param trim
	 * @param stripDiacritics
	 * @param stripWeirdCaracters
	 * @return
	 */
	public static int getLevenshteinDistance(String s1, String s2, boolean toLowerCase, boolean trim, boolean stripDiacritics, boolean stripWeirdCaracters) {
		String s1_=s1, s2_=s2;
		if(toLowerCase) { s1_=s1_.toLowerCase(); s2_=s2_.toLowerCase(); }
		if(trim) { s1_=s1_.trim(); s2_=s2_.trim(); }
		if(stripDiacritics) { s1_=Util.stripDiacritics(s1_); s2_=Util.stripDiacritics(s2_); }
		if(stripWeirdCaracters) { s1_=Util.stripWeirdCaracters(s1_); s2_=Util.stripWeirdCaracters(s2_); }
		//return StringUtils.getLevenshteinDistance(s1_,s2_);
		return LevenshteinDistance.getDefaultInstance().apply(s1_, s2_);
	}

	/**
	 * A match between two string, characterised by a cost (typically the Levenshtein distance).
	 * 
	 * @author julien Gaffuri
	 *
	 */
	public static class Match {
		public String s1, s2;
		public double cost = 0;
	}

	/**
	 * Get matching between a list of string and another one.
	 * Each string of the first list are matched to a string of the second one, based on the Levenshtein distance.
	 * 
	 * @param s1s
	 * @param s2s
	 * @param toLowerCase
	 * @param trim
	 * @param stripDiacritics
	 * @param stripWeirdCaracters
	 * @return
	 */
	public static Collection<Match> getMatchingMinLevenshteinDistance(Set<String> s1s, Collection<String> s2s, boolean toLowerCase, boolean trim, boolean stripDiacritics, boolean stripWeirdCaracters) {
		Collection<Match> out = new ArrayList<Match>();
		for(String s1 : s1s) {
			//evaluate distance to each s2, keeping the minimum one
			Match m = new Match(); m.cost = Integer.MAX_VALUE; m.s1 = s1;
			for(String s2 : s2s) {
				//evaluate distance
				int d = getLevenshteinDistance(s1, s2, toLowerCase, trim, stripDiacritics, stripWeirdCaracters);
				if(d>m.cost) continue;
				m.cost = d; m.s2 = s2;
			}
			out.add(m);
		}
		return out;
	}


	/**
	 * Get matching between a list of features and another one based on string attribute values.
	 * 
	 * @param f1s
	 * @param propF1
	 * @param f2s
	 * @param propF2
	 * @param toLowerCase
	 * @param trim
	 * @param stripDiacritics
	 * @param stripWeirdCaracters
	 * @return
	 */
	public static Collection<Match> getMatchingMinLevenshteinDistance(Collection<Feature> f1s, String propF1, Collection<Feature> f2s, String propF2, boolean toLowerCase, boolean trim, boolean stripDiacritics, boolean stripWeirdCaracters) {
		return getMatchingMinLevenshteinDistance(FeatureUtil.getPropValues(f1s, propF1), FeatureUtil.getPropValues(f2s, propF2), toLowerCase, trim, stripDiacritics, stripWeirdCaracters);
	}

	/**
	 * Index matches based on the fisrt string, in order to retrieve the matched string from the input one.
	 * 
	 * @param ms
	 * @return
	 */
	public static HashMap<String, Match> index(Collection<Match> ms) {
		HashMap<String,Match> msI = new HashMap<String,Match>();
		for(Match m : ms) msI.put(m.s1, m);
		return msI;
	}

	public static boolean override(HashMap<String,Match> msI, String sOld, String sNew) {
		Match m = msI.get(sOld);
		if(m == null) {
			LOGGER.warn("Could not override "+sOld+" into "+sNew);
			return false;
		}
		m.s2 = sNew;
		m.cost = 0;
		return true;
	}

	/**
	 * Set the geometry of some input features based on the geometry of some other features and a matching between them.
	 * 
	 * @param fs
	 * @param fsJoinProp
	 * @param matching
	 * @param fsGeomI
	 * @param withGeomAttribute
	 */
	public static void joinGeometry(Collection<Feature> fs, String fsJoinProp, HashMap<String,Match> matching, HashMap<String,Feature> fsGeomI, boolean withGeomAttribute) {
		for(Feature f : fs) {
			Match m = matching.get(f.getAttribute(fsJoinProp));
			if(m==null) {
				LOGGER.warn("No matching found to join geometry to feature with "+fsJoinProp+" = "+f.getAttribute(fsJoinProp));
				continue;
			}
			Feature fGeom = fsGeomI.get(m.s2);
			if(fGeom==null) {
				LOGGER.warn("No feature found to join geometry to feature with "+fsJoinProp+" = "+f.getAttribute(fsJoinProp) + " which corresponds to matching " + m.s1 + " <> " + m.s2);
				continue;
			}
			Geometry g = fGeom.getDefaultGeometry();
			if(g==null) {
				LOGGER.warn("No geometry for feature with id="+ fGeom.getID() + " which corresponds to matching " + m.s1 + " <> " + m.s2);
				continue;
			}
			f.setDefaultGeometry(g);
			if(withGeomAttribute) f.setAttribute("geom", g.toText());
		}
	}

	/**
	 * Retrieve the geometry of some features based on geometries of other features.
	 * The join is made on a property using Levenshtein distance.
	 * 
	 * @param fs
	 * @param fsJoinProp
	 * @param fsGeom
	 * @param fsGeomJoinProp
	 * @param withGeomAttribute
	 * @param toLowerCase
	 * @param trim
	 * @param stripDiacritics
	 * @param stripWeirdCaracters
	 * @return
	 */
	public static Collection<Match> joinGeometry(Collection<Feature> fs, String fsJoinProp, Collection<Feature> fsGeom, String fsGeomJoinProp, HashMap<String,String> overrides, boolean withGeomAttribute, boolean toLowerCase, boolean trim, boolean stripDiacritics, boolean stripWeirdCaracters) {
		//compute matching
		Collection<Match> ms = getMatchingMinLevenshteinDistance(fs,fsJoinProp, fsGeom, fsGeomJoinProp, toLowerCase, trim, stripDiacritics, stripWeirdCaracters);
		HashMap<String,Match> msI = index(ms);

		if(overrides != null)
			//apply overrides
			for(Entry<String,String> o : overrides.entrySet())
				override(msI, o.getKey(), o.getValue());

		//join geometries to features
		HashMap<String,Feature> locsI = FeatureUtil.index(fsGeom, fsGeomJoinProp);
		joinGeometry(fs, fsJoinProp, msI, locsI, withGeomAttribute);

		return ms;
	}
	public static Collection<Match> joinGeometry(Collection<Feature> fs, String fsJoinProp, Collection<Feature> fsGeom, String fsGeomJoinProp, HashMap<String,String> overrides, boolean withGeomAttribute) {
		return joinGeometry(fs,fsJoinProp,fsGeom,fsGeomJoinProp,overrides,withGeomAttribute,true,true,true,true);
	}




	/**
	 * Save the matching as CSV file.
	 * 
	 * @param ms
	 * @param outFile
	 */
	public static void saveAsCSV(Collection<Match> ms, String outFile) {
		ArrayList<HashMap<String, String>> data = new ArrayList<>();
		for(Match m : ms) {
			HashMap<String, String> data_ = new HashMap<>();
			data_.put("s1", m.s1);
			data_.put("s2", m.s2);
			data_.put("cost", m.cost+"");
			data.add(data_);
		}
		data.sort(new Comparator<HashMap<String, String>>() {
			@Override
			public int compare(HashMap<String, String> m1, HashMap<String, String> m2) {
				return (int) (1000*(Double.parseDouble(m2.get("cost").toString()) - Double.parseDouble(m1.get("cost").toString())));
			}
		});
		CSVUtil.save(data, outFile);
	}

}
