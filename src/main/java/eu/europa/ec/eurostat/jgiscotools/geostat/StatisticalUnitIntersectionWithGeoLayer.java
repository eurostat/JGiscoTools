/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.geostat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.TopologyException;
import org.locationtech.jts.index.strtree.STRtree;

import eu.europa.ec.eurostat.java4eurostat.base.StatsHypercube;
import eu.europa.ec.eurostat.java4eurostat.base.StatsIndex;
import eu.europa.ec.eurostat.java4eurostat.io.CSV;
import eu.europa.ec.eurostat.java4eurostat.io.DicUtil;
import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.FeatureUtil;

/**
 * @author julien Gaffuri
 *
 */
public class StatisticalUnitIntersectionWithGeoLayer {

	/**
	 * Compute statistics on geo objects at statistical units level.
	 * (Transfer information from geo layer to statistical units layer. TODO: define more generic aggregation model)
	 * 
	 * @param statUnits
	 * @param statUnitIdField
	 * @param geos
	 * @param statUnitOutFile
	 */
	public static void aggregateGeoStatsFromGeoToStatisticalUnits(Collection<Feature> statUnits, String statUnitIdField, Collection<Feature> geos, String statUnitOutFile) {
		try {
			//create out file
			File outFile_ = new File(statUnitOutFile);
			if(outFile_.exists()) outFile_.delete();
			BufferedWriter bw = new BufferedWriter(new FileWriter(outFile_, true));

			//write header
			bw.write(statUnitIdField+",number,area,length,area_density,length_density");
			bw.newLine();

			//index geo
			STRtree indGeo = FeatureUtil.getSTRtree(geos);

			int nbStats = statUnits.size();
			int statCounter = 1;
			for(Feature statUnit : statUnits) {
				String statUnitId = statUnit.getAttribute(statUnitIdField).toString();
				System.out.println(statUnitId + " " + (statCounter++) + "/" + nbStats + " " + (Math.round(10000*statCounter/nbStats))*0.01 + "%");

				//get all geo features intersecting the stat unit (with spatial index)
				Geometry StatUnitGeom = statUnit.getGeometry();
				List<?> geos_ = indGeo.query(StatUnitGeom.getEnvelopeInternal());

				//compute stat on geo features: total area/volume, number, building size distribution
				int nbGeo=0; double totalArea=0, totalLength=0;
				for(Object geo_ : geos_) {
					Feature geo = (Feature) geo_;
					try {
						nbGeo++;

						Geometry geoGeom = (Geometry) geo.getGeometry();
						if(!geoGeom.intersects(StatUnitGeom)) continue;

						Geometry inter = geoGeom.intersection(StatUnitGeom);
						totalArea += inter.getArea();
						totalLength += inter.getLength();
					} catch (TopologyException e) {
						System.err.println("Topology error for intersection computation");
					}
				}

				if(nbGeo == 0) continue;

				//store
				//"id,number,area,length,area_density,length_density");
				String line = statUnitId+","+nbGeo+","+totalArea+","+totalLength+","+totalArea/StatUnitGeom.getArea()+","+totalLength/StatUnitGeom.getArea();
				System.out.println(line);
				bw.write(line);
				bw.newLine();
			}
			bw.close();

		} catch (MalformedURLException e) { e.printStackTrace();
		} catch (IOException e) { e.printStackTrace(); }
	}


	/**
	 * Compute statistics on statistical units at geo objects level.
	 * (Transfer information from statistical units layer to geo layer. TODO: define more generic allocation model)
	 * 
	 * @param geos
	 * @param geoIdField
	 * @param statUnits
	 * @param statUnitsIdField
	 * @param statUnitValuesPath
	 * @param statUnitGeoStatValuesPath
	 * @param geoOutFile
	 */
	public static void allocateGeoStatsFromStatisticalUnitsToGeo(Collection<Feature> geos, String geoIdField, Collection<Feature> statUnits, String statUnitsIdField, String statUnitValuesPath, String statUnitGeoStatValuesPath, String geoOutFile) {
		try {
			//create out file

			File outFile_ = new File(geoOutFile);
			if(outFile_.exists()) outFile_.delete();
			BufferedWriter bw = new BufferedWriter(new FileWriter(outFile_, true));

			//write header
			bw.write(geoIdField+",value,density,_nbStatUnitIntersecting");
			bw.newLine();

			//load stat unit values
			HashMap<String, String> statUnitValue = DicUtil.load(statUnitValuesPath, ",");

			//load stat unit geostat values - area
			StatsHypercube statUnitGeoStatValues_ = CSV.load(statUnitGeoStatValuesPath, "area");
			statUnitGeoStatValues_.delete("number");
			statUnitGeoStatValues_.delete("length");
			statUnitGeoStatValues_.delete("area_density");
			statUnitGeoStatValues_.delete("length_density");
			StatsIndex statUnitGeoStatValues = new StatsIndex(statUnitGeoStatValues_,"voronoi_id");
			statUnitGeoStatValues_ = null;

			//index stat units
			STRtree indStat = FeatureUtil.getSTRtree(statUnits);

			//go through geo - purpose is to compute geo pop/density
			double geoCounter = 1;
			int nbGeo = geos.size();
			for(Feature geoUnit : geos) {
				String geoId = geoUnit.getAttribute(geoIdField).toString();
				System.out.println(geoId + " " + (geoCounter++) + "/" + nbGeo + " " + (Math.round(10000.0*geoCounter/nbGeo)*0.01) + "%");

				//get all stat units intersecting the geo (with spatial index)
				Geometry geoGeom = (Geometry) geoUnit.getGeometry();
				List<?> statUnits_ = indStat.query(geoGeom.getEnvelopeInternal());

				int nbStat = 0;
				double geoStatValue = 0;
				//geoStatValue = Sum on SUs intersecting of:  surf(geo inter su)/statUnitGeoTotalArea * statUnitValue
				for(Object stat_ : statUnits_) {
					try {
						Feature stat = (Feature) stat_;
						String statId = stat.getAttribute(statUnitsIdField).toString();

						//get stat unit geometry
						Geometry statUnitGeom = (Geometry) stat.getGeometry();
						if(!geoGeom.intersects(statUnitGeom)) continue;

						nbStat++;

						//get stat unit value
						String statValue = statUnitValue.get(statId);
						if(statValue == null || Double.parseDouble(statValue) == 0) continue;

						//get stat unit geostat values
						double statUnitGeoStatValue = statUnitGeoStatValues.getSingleValue(statId);
						if(Double.isNaN(statUnitGeoStatValue) || statUnitGeoStatValue == 0) continue;

						geoStatValue += geoGeom.intersection(statUnitGeom).getArea() / statUnitGeoStatValue * Double.parseDouble(statValue);
					} catch (TopologyException e) {
						System.err.println("Topology error.");
					}
				}

				if(nbStat == 0) continue;

				//store
				//bw.write(geoIdField+",value,density,_nbStatUnitIntersecting");
				String line = geoId+","+geoStatValue+","+geoStatValue/geoGeom.getArea()+","+nbStat;
				System.out.println(line);
				bw.write(line);
				bw.newLine();
			}
			bw.close();
		} catch (Exception e) { e.printStackTrace(); }
	}



	//TODO merge with aggregateGeoStatsFromGeoToStatisticalUnits once generic aggregation model is there ?
	public static void aggregateStatValueFomGeoValues(Collection<Feature> statUnits, String statUnitsIdField, Collection<Feature> geos, String geoIdField, String geoValuesPath, String statUnitOutFile) {
		try {
			//create out file
			File outFile_ = new File(statUnitOutFile);
			if(outFile_.exists()) outFile_.delete();
			BufferedWriter bw = new BufferedWriter(new FileWriter(outFile_, true));

			//write header
			bw.write(statUnitsIdField+",value,density,nbGeosIntersecting");
			bw.newLine();

			//load geo values
			HashMap<String, String> geoValues = DicUtil.load(geoValuesPath, ",");

			//index geo
			STRtree indGeo = FeatureUtil.getSTRtree(geos);

			//go through stat units - purpose is to compute value from geos intersecting
			int statCounter = 1;
			int nbStats = statUnits.size();
			for(Feature statUnit : statUnits) {
				String statUnitId = statUnit.getAttribute(statUnitsIdField).toString();
				System.out.println(statUnitId + " " + (statCounter++) + "/" + nbStats + " " + (Math.round(10000*statCounter/nbStats))*0.01 + "%");

				//get all geos intersecting the stat unit (with spatial index)
				Geometry statGeom = (Geometry) statUnit.getGeometry();
				List<?> geos_ = indGeo.query(statGeom.getEnvelopeInternal());

				int nbGeos = 0;
				double statValue = 0;
				//statValue = Sum on geos intersecting of:  surf(geo inter su)/surf(geo) * geoValue
				for(Object geo_ : geos_) {
					try {
						Feature geo = (Feature) geo_;
						String geoId = geo.getAttribute(geoIdField).toString();

						//get geo feature geometry
						Geometry geoGeom = (Geometry) geo.getGeometry();
						if(!geoGeom.intersects(statGeom)) continue;

						nbGeos++;

						//get geo value
						String geoValue = geoValues.get(geoId);
						if(geoValue == null || Double.parseDouble(geoValue) == 0) continue;

						statValue += geoGeom.intersection(statGeom).getArea() / geoGeom.getArea() * Double.parseDouble(geoValue);
					} catch (TopologyException e) {
						System.err.println("Topology error.");
						continue;
					}
				}

				//if(nbGeos == 0) continue;

				//store
				String line = statUnitId+","+statValue+","+statValue/statGeom.getArea()+","+nbGeos;
				System.out.println(line);
				bw.write(line);
				bw.newLine();
			}
			bw.close();
		} catch (Exception e) { e.printStackTrace(); }
	}

}
