/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.geostat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;

import org.geotools.feature.FeatureIterator;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.TopologyException;
import org.opengis.feature.simple.SimpleFeature;

import eu.europa.ec.eurostat.java4eurostat.base.StatsHypercube;
import eu.europa.ec.eurostat.java4eurostat.base.StatsIndex;
import eu.europa.ec.eurostat.java4eurostat.io.CSV;
import eu.europa.ec.eurostat.java4eurostat.io.DicUtil;
import eu.europa.ec.eurostat.jgiscotools.io.ShapeFile;

/**
 * @author julien Gaffuri
 *
 */
public class StatisticalUnitIntersectionWithGeoLayer {

	/**
	 * Compute statistics on geo objects at statistical units level.
	 * (Transfer information from geo layer to statistical units layer. TODO: define more generic aggregation model)
	 * 
	 * @param statUnitsSHPFile
	 * @param statUnitIdField
	 * @param geoSHPFile
	 * @param statUnitOutFile
	 */
	public static void aggregateGeoStatsFromGeoToStatisticalUnits(String statUnitsSHPFile, String statUnitIdField, String geoSHPFile, String statUnitOutFile) {
		try {
			//create out file
			File outFile_ = new File(statUnitOutFile);
			if(outFile_.exists()) outFile_.delete();
			BufferedWriter bw = new BufferedWriter(new FileWriter(outFile_, true));

			//write header
			bw.write(statUnitIdField+",number,area,length,area_density,length_density");
			bw.newLine();

			//open statistical units and geo shapefiles
			ShapeFile statShp = new ShapeFile(statUnitsSHPFile).dispose();
			int nbStats = statShp.count();
			ShapeFile geoShp = new ShapeFile(geoSHPFile);
			//FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

			//go through statistical units
			FeatureIterator<SimpleFeature> itStat = statShp.getFeatures();
			int statCounter = 1;
			while (itStat.hasNext()) {
				SimpleFeature statUnit = itStat.next();
				String statUnitId = statUnit.getAttribute(statUnitIdField).toString();
				System.out.println(statUnitId + " " + (statCounter++) + "/" + nbStats + " " + (Math.round(10000*statCounter/nbStats))*0.01 + "%");

				//get all geo features intersecting the stat unit (with spatial index)
				Geometry StatUnitGeom = (Geometry) statUnit.getDefaultGeometryProperty().getValue();
				FeatureIterator<SimpleFeature> itGeo = geoShp.getFeatures(statUnit.getBounds(), "the_geom");

				//compute stat on geo features: total area/volume, number, building size distribution
				int nbGeo=0; double totalArea=0, totalLength=0;
				while (itGeo.hasNext()) {
					try {
						SimpleFeature geo = itGeo.next();
						nbGeo++;

						Geometry geoGeom = (Geometry) geo.getDefaultGeometryProperty().getValue();
						if(!geoGeom.intersects(StatUnitGeom)) continue;

						Geometry inter = geoGeom.intersection(StatUnitGeom);
						totalArea += inter.getArea();
						totalLength += inter.getLength();
					} catch (TopologyException e) {
						System.err.println("Topology error for intersection computation");
					}
				}
				itGeo.close();

				if(nbGeo == 0) continue;

				//store
				//"id,number,area,length,area_density,length_density");
				String line = statUnitId+","+nbGeo+","+totalArea+","+totalLength+","+totalArea/StatUnitGeom.getArea()+","+totalLength/StatUnitGeom.getArea();
				System.out.println(line);
				bw.write(line);
				bw.newLine();
			}
			itStat.close();
			bw.close();

		} catch (MalformedURLException e) { e.printStackTrace();
		} catch (IOException e) { e.printStackTrace(); }
	}


	/**
	 * Compute statistics on statistical units at geo objects level.
	 * (Transfer information from statistical units layer to geo layer. TODO: define more generic allocation model)
	 * 
	 * @param geoSHPFile
	 * @param geoIdField
	 * @param statUnitsSHPFile
	 * @param statUnitsIdField
	 * @param statUnitValuesPath
	 * @param statUnitGeoStatValuesPath
	 * @param geoOutFile
	 */
	public static void allocateGeoStatsFromStatisticalUnitsToGeo(String geoSHPFile, String geoIdField, String statUnitsSHPFile, String statUnitsIdField, String statUnitValuesPath, String statUnitGeoStatValuesPath, String geoOutFile) {
		try {
			//create out file

			File outFile_ = new File(geoOutFile);
			if(outFile_.exists()) outFile_.delete();
			BufferedWriter bw = new BufferedWriter(new FileWriter(outFile_, true));

			//write header
			bw.write(geoIdField+",value,density,_nbStatUnitIntersecting");
			bw.newLine();

			//open geo and statistical units shapefiles
			ShapeFile geoShp = new ShapeFile(geoSHPFile).dispose();
			int nbGeo = geoShp.count();
			ShapeFile statShp = new ShapeFile(statUnitsSHPFile);

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


			//go through geo - purpose is to compute geo pop/density
			FeatureIterator<SimpleFeature> itGeo = geoShp.getFeatures();
			double geoCounter = 1;
			while (itGeo.hasNext()) {
				SimpleFeature geoUnit = itGeo.next();
				String geoId = geoUnit.getAttribute(geoIdField).toString();
				System.out.println(geoId + " " + (geoCounter++) + "/" + nbGeo + " " + (Math.round(10000.0*geoCounter/nbGeo)*0.01) + "%");

				Geometry geoGeom = (Geometry) geoUnit.getDefaultGeometryProperty().getValue();

				//get all stat units intersecting the geo (with spatial index)
				FeatureIterator<SimpleFeature> itStat = statShp.getFeatures(geoUnit.getBounds(), "the_geom");

				int nbStat = 0;
				double geoStatValue = 0;
				//geoStatValue = Sum on SUs intersecting of:  surf(geo inter su)/statUnitGeoTotalArea * statUnitValue
				while (itStat.hasNext()) {
					try {
						SimpleFeature stat = itStat.next();
						String statId = stat.getAttribute(statUnitsIdField).toString();

						//get stat unit geometry
						Geometry statUnitGeom = (Geometry) stat.getDefaultGeometryProperty().getValue();
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
				itStat.close();

				if(nbStat == 0) continue;

				//store
				//bw.write(geoIdField+",value,density,_nbStatUnitIntersecting");
				String line = geoId+","+geoStatValue+","+geoStatValue/geoGeom.getArea()+","+nbStat;
				System.out.println(line);
				bw.write(line);
				bw.newLine();
			}
			itGeo.close();
			bw.close();
		} catch (Exception e) { e.printStackTrace(); }
	}



	//TODO merge with aggregateGeoStatsFromGeoToStatisticalUnits once generic aggregation model is there ?
	public static void aggregateStatValueFomGeoValues(String statUnitsSHPFile, String statUnitsIdField, String geoSHPFile, String geoIdField, String geoValuesPath, String statUnitOutFile) {
		try {
			//create out file
			File outFile_ = new File(statUnitOutFile);
			if(outFile_.exists()) outFile_.delete();
			BufferedWriter bw = new BufferedWriter(new FileWriter(outFile_, true));

			//write header
			bw.write(statUnitsIdField+",value,density,nbGeosIntersecting");
			bw.newLine();

			//open statistical units and geo shapefiles
			ShapeFile statShp = new ShapeFile(statUnitsSHPFile).dispose();
			int nbStats = statShp.count();
			ShapeFile geoShp = new ShapeFile(geoSHPFile);

			//load geo values
			HashMap<String, String> geoValues = DicUtil.load(geoValuesPath, ",");

			//go through stat units - purpose is to compute value from geos intersecting
			FeatureIterator<SimpleFeature> itStat = statShp.getFeatures();
			int statCounter = 1;
			while (itStat.hasNext()) {
				SimpleFeature statUnit = itStat.next();
				String statUnitId = statUnit.getAttribute(statUnitsIdField).toString();
				System.out.println(statUnitId + " " + (statCounter++) + "/" + nbStats + " " + (Math.round(10000*statCounter/nbStats))*0.01 + "%");

				Geometry statGeom = (Geometry) statUnit.getDefaultGeometryProperty().getValue();

				//get all geos intersecting the stat unit (with spatial index)
				FeatureIterator<SimpleFeature> itGeo = geoShp.getFeatures(statUnit.getBounds(), "the_geom");

				int nbGeos = 0;
				double statValue = 0;
				//statValue = Sum on geos intersecting of:  surf(geo inter su)/surf(geo) * geoValue
				while (itGeo.hasNext()) {
					try {
						SimpleFeature geo = itGeo.next();
						String geoId = geo.getAttribute(geoIdField).toString();

						//get geo feature geometry
						Geometry geoGeom = (Geometry) geo.getDefaultGeometryProperty().getValue();
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
				itGeo.close();

				//if(nbGeos == 0) continue;

				//store
				String line = statUnitId+","+statValue+","+statValue/statGeom.getArea()+","+nbGeos;
				System.out.println(line);
				bw.write(line);
				bw.newLine();
			}
			itStat.close();
			bw.close();
		} catch (Exception e) { e.printStackTrace(); }
	}

}
