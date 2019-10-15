/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.dasymetric;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureIterator;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.TopologyException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

import eu.europa.ec.eurostat.java4eurostat.base.Stat;
import eu.europa.ec.eurostat.java4eurostat.base.StatsHypercube;
import eu.europa.ec.eurostat.java4eurostat.base.StatsIndex;

/**
 * 
 * Operation to perform geographical disaggregation with dasymetric mapping
 * 
 * @author Julien Gaffuri
 *
 */
public class DasymetricMapping {

	//the initial statistical values to disaggregate. Index by geo -> value
	public StatsIndex statValuesInitial; //TODO use hashmap instead.

	//the initial statistical units
	private SimpleFeatureSource statUnitsInitialFeatureSource;
	private String statUnitsInitialIdFieldName;

	//the geographical features to base on to support the geographical disaggregation
	private SimpleFeatureSource geoFeatureSource;
	private String geoIdFieldName;

	//the target statistical units (as new geographical level)
	private SimpleFeatureSource statUnitsFinalFeatureSource;
	private String statUnitsFinalIdFieldName;

	//3:area, 2:line, 1: point
	//TODO use enumeration, from JTS/geotools?
	//TODO select automatically from shp schema?
	private int geomCase = 1;

	//default constructor
	public DasymetricMapping(
			int geomCase,
			StatsIndex statValuesInitial,
			SimpleFeatureSource statUnitsInitialFeatureSource,
			String statUnitsInitialIdFieldName,
			SimpleFeatureSource geoFeatureSource,
			String geoIdFieldName,
			SimpleFeatureSource statUnitsFinalFeatureSource,
			String statUnitsFinalIdFieldName
			){
		this.geomCase = geomCase;
		this.statValuesInitial = statValuesInitial;
		this.statUnitsInitialFeatureSource = statUnitsInitialFeatureSource;
		this.statUnitsInitialIdFieldName = statUnitsInitialIdFieldName;
		this.geoFeatureSource = geoFeatureSource;
		this.geoIdFieldName = geoIdFieldName;
		this.statUnitsFinalFeatureSource = statUnitsFinalFeatureSource;
		this.statUnitsFinalIdFieldName = statUnitsFinalIdFieldName;
	}

	public void run(boolean approximated){
		//Step 1: compute statistics on geo features at initial stat unit level
		if(!approximated) computeGeoStatInitial();

		//Step 2: allocate statistics at geo features level
		allocateStatGeo(approximated); //TODO make tests on geomtype there!

		//Step 3: aggregate statistics at target stat unit level
		aggregateGeoStat();
	}




	/**
	 * Run disaggregation (simplified process).
	 * This should apply only if:
	 *  - The geo objects are points
	 *  - The target SUs are mapped to exactly one single initial SU
	 */
	public void runSimplified(){
		//Step 1: compute statistics on geo features at initial stat unit level
		computeGeoStatInitial();

		//Step 1b: compute statistics on geo features at target stat unit level
		computeGeoStatFinal();

		//Step 3b: compute statistics at target stat unit level
		computeDisaggregatedStatsSimplified();
	}




	//Steps 1 and 1b
	//the output value of the first step: statistical values on the geo features, at the initial stat units level 
	public StatsHypercube geoStatsInitialHC;
	public void computeGeoStatInitial() {
		geoStatsInitialHC = computeGeoStat(statUnitsInitialFeatureSource, statUnitsInitialIdFieldName);
	}

	//Steps 1b
	//the output value of the first step: statistical values on the geo features, at the initial stat units level 
	public StatsHypercube geoStatsFinalHC;
	public void computeGeoStatFinal() {
		geoStatsFinalHC = computeGeoStat(statUnitsFinalFeatureSource, statUnitsFinalIdFieldName);
	}

	//Steps 1 and 1b
	private StatsHypercube computeGeoStat(SimpleFeatureSource statUnitsFeatureSource, String statUnitsIdFieldName ) {
		try {
			//initialise output structure
			StatsHypercube geoStatsHC = new StatsHypercube("geo", "indic");

			//prepare
			FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

			//go through statistical units
			int statCounter = 1, nbStats = statUnitsFeatureSource.getFeatures().size();
			FeatureIterator<SimpleFeature> itStat = statUnitsFeatureSource.getFeatures().features();
			while (itStat.hasNext()) {
				SimpleFeature statUnit = itStat.next();
				String statUnitId = statUnit.getAttribute(statUnitsIdFieldName).toString();

				System.out.println(statUnitId + " " + (statCounter++) + "/" + nbStats + " " + (Math.round(10000*statCounter/nbStats))*0.01 + "%");

				//get all geo features intersecting the stat unit (with spatial index)
				Geometry statUnitGeom = (Geometry) statUnit.getDefaultGeometryProperty().getValue();
				//Filter f = ff.bbox(ff.property("the_geom"), statUnit.getBounds());
				Filter f = ff.intersects(ff.property("the_geom"), ff.literal(statUnitGeom));
				FeatureIterator<SimpleFeature> itGeo = geoFeatureSource.getFeatures(f).features();

				//compute stat on geo features
				double areaGeo=0, lengthGeo=0; int numberGeo=0;
				while (itGeo.hasNext()) {
					try {
						Geometry geoGeom = (Geometry) itGeo.next().getDefaultGeometryProperty().getValue();

						if(geoGeom.isEmpty()) continue;
						numberGeo ++;

						if(geoGeom.getArea()==0 && geoGeom.getLength()==0) continue;
						//if(!statUnitGeom.intersects(geoGeom)) continue;

						Geometry inter = geoGeom.intersection(statUnitGeom);
						areaGeo += inter.getArea();
						lengthGeo += inter.getLength();
					} catch (TopologyException e) {
						System.err.println("Topology error for intersection computation");
					}
				}
				itGeo.close();

				if(numberGeo == 0) continue;

				//store
				geoStatsHC.stats.add(new Stat(numberGeo, "indic", "number", "geo", statUnitId));
				geoStatsHC.stats.add(new Stat(areaGeo, "indic", "area", "geo", statUnitId));
				geoStatsHC.stats.add(new Stat(lengthGeo, "indic", "length", "geo", statUnitId));
				double statUnitArea = statUnitGeom.getArea();
				if(statUnitArea > 0){
					geoStatsHC.stats.add(new Stat(numberGeo/statUnitArea, "indic", "density", "geo", statUnitId));
					geoStatsHC.stats.add(new Stat(areaGeo/statUnitArea, "indic", "area_density", "geo", statUnitId));
					geoStatsHC.stats.add(new Stat(lengthGeo/statUnitArea, "indic", "length_density", "geo", statUnitId));
				}
			}
			itStat.close();

			return geoStatsHC;

		} catch (Exception e) { e.printStackTrace(); }
		return null;
	}



	//Step 2: allocate statistics at geo features level

	//the output value of the second step: statistical values at geo features level
	public StatsHypercube statsGeoAllocationHC;

	public void allocateStatGeo() {
		try {
			StatsIndex geoStatsInitialHCI = new StatsIndex(geoStatsInitialHC, "indic", "geo");

			//initialise output structure
			statsGeoAllocationHC = new StatsHypercube("geo");

			//go through geo - purpose is to compute geo statistical value
			int geoCounter = 1, nbGeo = geoFeatureSource.getFeatures().size();
			FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
			FeatureIterator<SimpleFeature> itGeo = geoFeatureSource.getFeatures().features();
			while (itGeo.hasNext()) {
				SimpleFeature geoUnit = itGeo.next();
				String geoId = geoUnit.getAttribute(geoIdFieldName).toString();
				Geometry geoGeom = (Geometry) geoUnit.getDefaultGeometryProperty().getValue();

				System.out.println("geoId: " + geoId + " " + (geoCounter++) + "/" + nbGeo + " " + (Math.round(10000.0*(1.0*geoCounter)/(1.0*nbGeo))*0.01) + "%");

				//get all stat units intersecting the geo (with spatial index)
				Filter f = ff.bbox(ff.property("the_geom"), geoUnit.getBounds());
				//Filter f = ff.intersects(ff.property("the_geom"), ff.literal(geoGeom));
				FeatureIterator<SimpleFeature> itStat = statUnitsInitialFeatureSource.getFeatures(f).features();

				int nbStat = 0; double geoStatValue = 0;
				while (itStat.hasNext()) {
					try {
						SimpleFeature stat = itStat.next();
						String statId = stat.getAttribute(statUnitsInitialIdFieldName).toString();

						//get stat unit value
						double statValue = statValuesInitial.getSingleValue(statId);
						if(Double.isNaN(statValue) || statValue == 0) continue;

						//get stat unit geometry
						Geometry statUnitGeom = (Geometry) stat.getDefaultGeometryProperty().getValue();
						Geometry inter = geoGeom.intersection(statUnitGeom);
						if(inter.isEmpty()) continue;

						nbStat++;

						//increment with stat unit geostat value
						if(geomCase == 3){
							//area case
							double statUnitGeoStatValue = geoStatsInitialHCI.getSingleValue("area", statId);
							if(!Double.isNaN(statUnitGeoStatValue) && statUnitGeoStatValue != 0)
								geoStatValue += statValue * inter.getArea() / statUnitGeoStatValue;
						} else if(geomCase == 2){
							//line case
							double statUnitGeoStatValue = geoStatsInitialHCI.getSingleValue("length", statId);
							if(!Double.isNaN(statUnitGeoStatValue) && statUnitGeoStatValue != 0)
								geoStatValue += statValue * inter.getLength() / statUnitGeoStatValue;
						} else {
							//point case
							double statUnitGeoStatValue = geoStatsInitialHCI.getSingleValue("number", statId);
							if(!Double.isNaN(statUnitGeoStatValue) && statUnitGeoStatValue != 0)
								geoStatValue += statValue * inter.getCoordinates().length / statUnitGeoStatValue;
						}

					} catch (TopologyException e) {
						System.err.println("Topology error.");
					}
				}
				itStat.close();

				if(nbStat == 0) continue;

				//store
				statsGeoAllocationHC.stats.add(new Stat(geoStatValue, "geo", geoId));
				System.out.println(" -> value = "+geoStatValue);
				//statsGeoAllocationHC.stats.add(new Stat(nbStat, "indic", "number", "geo", geoId));
			}
			itGeo.close();
		} catch (Exception e) { e.printStackTrace(); }
	}

	//approximated version: suppose each geo is located into a single SU. This is the case for example when geo objects are points
	public void allocateStatGeo(boolean approximated) {
		if(!approximated){
			allocateStatGeo();
			return;
		}

		try {
			//initialise output structure
			statsGeoAllocationHC = new StatsHypercube("geo");

			//go through init stats - purpose is to distribute stat value to geo objects
			int statIniCounter = 1, nbStatIni = statUnitsInitialFeatureSource.getFeatures().size();
			FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
			FeatureIterator<SimpleFeature> itStatIni = statUnitsInitialFeatureSource.getFeatures().features();
			while (itStatIni.hasNext()) {
				SimpleFeature statIni = itStatIni.next();
				String statIniId = statIni.getAttribute(statUnitsInitialIdFieldName).toString();
				Geometry statIniGeom = (Geometry) statIni.getDefaultGeometryProperty().getValue();

				//get stat unit value
				double statValue = statValuesInitial.getSingleValue(statIniId);
				if(Double.isNaN(statValue) || statValue == 0) continue;

				System.out.println("statIniId: " + statIniId + " " + (statIniCounter++) + "/" + nbStatIni + " " + (Math.round(10000.0*(1.0*statIniCounter)/(1.0*nbStatIni))*0.01) + "%");

				//get all geo intersecting the statIni (with spatial index)
				//Filter f = ff.bbox(ff.property("the_geom"), statIni.getBounds());
				Filter f = ff.intersects(ff.property("the_geom"), ff.literal(statIniGeom));
				SimpleFeatureCollection col = geoFeatureSource.getFeatures(f);
				int nbGeo = col.size();
				if(nbGeo == 0) continue;
				FeatureIterator<SimpleFeature> itGeo = col.features();

				double geoStatValue = statValue/nbGeo;
				System.out.println(" -> nbGeo = "+nbGeo+" -> value = "+geoStatValue);
				while (itGeo.hasNext()) {
					SimpleFeature geo = itGeo.next();
					String geoId = geo.getAttribute(geoIdFieldName).toString();

					//store
					statsGeoAllocationHC.stats.add(new Stat(geoStatValue, "geo", geoId));
				}
				itGeo.close();
			}
			itStatIni.close();
		} catch (Exception e) { e.printStackTrace(); }
	}



	//Step 3: aggregate statistics at target stat unit level

	public StatsHypercube finalStatsHC;

	public void aggregateGeoStat() {
		try {
			//the statistics allocates at geo level
			StatsIndex statsGeoAllocationI = new StatsIndex(statsGeoAllocationHC, "geo"); //TODO when point, should be number. When other should be length/area density

			//initialise output structure
			finalStatsHC = new StatsHypercube("geo");

			//go through statistical units
			int statCounter = 1, nbStats = statUnitsFinalFeatureSource.getFeatures().size();
			FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
			FeatureIterator<SimpleFeature> itStat = statUnitsFinalFeatureSource.getFeatures().features();
			while (itStat.hasNext()) {
				SimpleFeature statUnit = itStat.next();
				String statUnitId = statUnit.getAttribute(statUnitsFinalIdFieldName).toString();
				Geometry statUnitGeom = (Geometry) statUnit.getDefaultGeometryProperty().getValue();

				System.out.println(statUnitId + " " + (statCounter++) + "/" + nbStats + " " + (Math.round(10000.0*statCounter/(1.0*nbStats)))*0.01 + "%");

				//get all geo features intersecting the stat unit (with spatial index)
				Filter f = ff.bbox(ff.property("the_geom"), statUnit.getBounds());
				//Filter f = ff.intersects(ff.property("the_geom"), ff.literal(statUnitGeom));
				FeatureIterator<SimpleFeature> itGeo = geoFeatureSource.getFeatures(f).features();

				//compute stat on geo features
				double statSum=0, weightsSum=0;
				while (itGeo.hasNext()) {
					try {
						SimpleFeature geo = itGeo.next();
						String geoId = geo.getAttribute(geoIdFieldName).toString();
						Geometry geoGeom = (Geometry) geo.getDefaultGeometryProperty().getValue();

						if(geoGeom.isEmpty()) continue;

						//get statistics allocated at geo level
						double geoValue = statsGeoAllocationI.getSingleValue(geoId);
						if(Double.isNaN(geoValue)) continue;

						//compute intersection
						Geometry inter = geoGeom.intersection(statUnitGeom);
						if(inter.isEmpty()) continue;

						double weight = geomCase == 3? inter.getArea() : geomCase == 2? inter.getLength() : inter.getCoordinates().length;

						weightsSum += weight;
						statSum += weight * geoValue;
					} catch (TopologyException e) {
						System.err.println("Topology error for intersection computation");
					}
				}
				itGeo.close();

				if(weightsSum == 0) continue;

				//store
				double value = statSum;
				if(geomCase != 1) value /= weightsSum;
				finalStatsHC.stats.add(new Stat(value, "geo", statUnitId));
			}
			itStat.close();
		} catch (Exception e) { e.printStackTrace(); }
	}




	//Step 3b: compute statistics at target stat unit level
	public StatsHypercube finalStatsSimplifiedHC;

	public void computeDisaggregatedStatsSimplified() {
		try {
			StatsIndex geoStatsInitialHCI = new StatsIndex(geoStatsInitialHC, "indic", "geo");
			StatsIndex geoStatsFinalHCI = new StatsIndex(geoStatsFinalHC, "indic", "geo");

			//initialise output structure
			finalStatsSimplifiedHC = new StatsHypercube("geo");

			//prepare
			FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

			//go through initial statistical units
			int statCounter = 1, nbStats = statUnitsInitialFeatureSource.getFeatures().size();
			FeatureIterator<SimpleFeature> itStatIni = statUnitsInitialFeatureSource.getFeatures().features();
			while (itStatIni.hasNext()) {
				SimpleFeature statUnitIni = itStatIni.next();
				String statUnitIniId = statUnitIni.getAttribute(statUnitsInitialIdFieldName).toString();

				System.out.println(statUnitIniId + " " + (statCounter++) + "/" + nbStats + " " + (Math.round(10000*statCounter/nbStats))*0.01 + "%");

				//get statistical value
				double iniStatValue = statValuesInitial.getSingleValue(statUnitIniId);
				if(Double.isNaN(iniStatValue)) continue;
				//get geostat value for ini
				String indic = "number"; //TODO generalise, using geomCase
				double iniGeoStatValue = geoStatsInitialHCI.getSingleValue(indic, statUnitIniId);
				if(Double.isNaN(iniGeoStatValue) || iniGeoStatValue == 0) continue;

				//get all final stat units intersecting the initial stat unit (with spatial index)
				Geometry statUnitIniGeom = (Geometry) statUnitIni.getDefaultGeometryProperty().getValue();
				//Filter f = ff.bbox(ff.property("the_geom"), statUnitIni.getBounds());
				Filter f = ff.intersects(ff.property("the_geom"), ff.literal(statUnitIniGeom));
				FeatureIterator<SimpleFeature> itStatFin = statUnitsFinalFeatureSource.getFeatures(f).features();

				//compute stat on stat unit final
				while (itStatFin.hasNext()) {
					try {
						SimpleFeature statUnitFin = itStatFin.next();
						String statUnitFinId = statUnitFin.getAttribute(statUnitsFinalIdFieldName).toString();

						//get geostat value for fin
						double finGeoStatValue = geoStatsFinalHCI.getSingleValue(indic, statUnitFinId);
						if(Double.isNaN(finGeoStatValue)) continue;

						Geometry statUnitFinGeom = (Geometry) statUnitFin.getDefaultGeometryProperty().getValue();
						//if(!statUnitIniGeom.intersects(statUnitFinGeom)) continue;
						Geometry inter = statUnitIniGeom.intersection(statUnitFinGeom);
						if(inter.getArea()/statUnitFinGeom.getArea() < 0.75) continue;

						//System.out.println("   "+statUnitFinId + " " + (inter.getArea()/statUnitFinGeom.getArea()));

						//compute value
						double value = iniStatValue * finGeoStatValue / iniGeoStatValue;

						//store
						finalStatsSimplifiedHC.stats.add(new Stat(value, "geo", statUnitFinId));
					} catch (TopologyException e) {
						System.err.println("Topology error for intersection computation");
					}
				}
				itStatFin.close();
			}
			itStatIni.close();
		} catch (Exception e) { e.printStackTrace(); }
	}

}
