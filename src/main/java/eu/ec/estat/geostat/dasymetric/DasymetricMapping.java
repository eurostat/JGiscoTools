/**
 * 
 */
package eu.ec.estat.geostat.dasymetric;

import java.io.IOException;
import java.net.MalformedURLException;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.TopologyException;

import eu.ec.estat.java4eurostat.base.Stat;
import eu.ec.estat.java4eurostat.base.StatsHypercube;
import eu.ec.estat.java4eurostat.base.StatsIndex;

/**
 * @author Julien Gaffuri
 *
 */
public class DasymetricMapping {

	//the initial statistical values to disaggregate. Index by geo -> value
	public StatsIndex statValuesInitial;

	//the initial statistical units
	private SimpleFeatureStore statUnitsInitialFeatureStore;
	private String statUnitsInitialIdFieldName;

	//the geographical features to base on to support the geographical disaggregation
	private SimpleFeatureStore geoFeatureStore;
	private String geoIdFieldName;

	//the target statistical units (as new geographical level)
	private SimpleFeatureStore statUnitsFinalFeatureStore;
	private String statUnitsFinalIdFieldName;

	//default constructor
	public DasymetricMapping(
			SimpleFeatureStore statUnitsInitialFeatureStore,
			String statUnitsInitialIdFieldName,
			StatsIndex statValuesInitial,
			SimpleFeatureStore geoFeatureStore,
			String geoIdFieldName,
			SimpleFeatureStore statUnitsFinalFeatureStore,
			String statUnitsFinalIdFieldName
			){
		this.statUnitsInitialFeatureStore = statUnitsInitialFeatureStore;
		this.statUnitsInitialIdFieldName = statUnitsInitialIdFieldName;
		this.statValuesInitial = statValuesInitial;
		this.geoFeatureStore = geoFeatureStore;
		this.geoIdFieldName = geoIdFieldName;
		this.statUnitsFinalFeatureStore = statUnitsFinalFeatureStore;
		this.statUnitsFinalIdFieldName = statUnitsFinalIdFieldName;
	}


	//this should apply only if:
	// - The geo geometrytype is
	public void runSimplified(){
		//Step 1: compute statistics on geo features at initial stat unit level
		computeGeoStatInitial();

		//Step 1: compute statistics on geo features at target stat unit level
		computeGeoStatFinal();

		//Step 3: compute statistics at target stat unit level
		computeFinalStat();
	}



	public void run(){
		//Step 1: compute statistics on geo features at initial stat unit level
		computeGeoStatInitial();

		//Step 2b: allocate statistics at geo features level
		allocateStatGeo();

		//Step 3b: aggregate statistics at target stat unit level
		aggregateGeoStat();
	}



	//Steps 1 and 2

	//the output value of the first step: statistical values on the geo features, at the initial stat units level 
	public StatsHypercube geoStatsInitialHC;
	public void computeGeoStatInitial() {
		geoStatsInitialHC = computeGeoStat(statUnitsInitialFeatureStore, statUnitsInitialIdFieldName);
	}

	//the output value of the first step: statistical values on the geo features, at the initial stat units level 
	public StatsHypercube geoStatsFinalHC;
	public void computeGeoStatFinal() {
		geoStatsFinalHC = computeGeoStat(statUnitsFinalFeatureStore, statUnitsFinalIdFieldName);
	}


	private StatsHypercube computeGeoStat(SimpleFeatureStore statUnitsFeatureStore, String statUnitsIdFieldName ) {
		try {
			//initialise output structure
			StatsHypercube geoStatsHC = new StatsHypercube();
			geoStatsHC.dimLabels.add("geo");
			geoStatsHC.dimLabels.add("indic");

			//prepare
			FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

			//go through statistical units
			int statCounter = 1, nbStats = statUnitsFeatureStore.getFeatures().size();
			FeatureIterator<SimpleFeature> itStat = ((SimpleFeatureCollection) statUnitsFeatureStore.getFeatures()).features();
			while (itStat.hasNext()) {
				SimpleFeature statUnit = itStat.next();
				String statUnitId = statUnit.getAttribute(statUnitsIdFieldName).toString();

				System.out.println(statUnitId + " " + (statCounter++) + "/" + nbStats + " " + (Math.round(10000*statCounter/nbStats))*0.01 + "%");

				//get all geo features intersecting the stat unit (with spatial index)
				Geometry statUnitGeom = (Geometry) statUnit.getDefaultGeometryProperty().getValue();
				//Filter f = ff.bbox(ff.property("the_geom"), statUnit.getBounds());
				Filter f = ff.intersects(ff.property("the_geom"), ff.literal(statUnitGeom));
				int numberGeo = ((SimpleFeatureCollection) geoFeatureStore.getFeatures(f)).size();

				//compute stat on geo features
				double areaGeo=0, lengthGeo=0;
				FeatureIterator<SimpleFeature> itGeo = ((SimpleFeatureCollection) geoFeatureStore.getFeatures(f)).features();
				while (itGeo.hasNext()) {
					try {
						SimpleFeature geo = itGeo.next();
						Geometry geoGeom = (Geometry) geo.getDefaultGeometryProperty().getValue();
						if(geoGeom.getArea()==0 && geoGeom.getLength()==0) continue;
						if(!statUnitGeom.intersects(geoGeom)) continue;

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
				geoStatsHC.stats.add(new Stat(numberGeo/statUnitGeom.getArea(), "indic", "density", "geo", statUnitId));
				geoStatsHC.stats.add(new Stat(areaGeo/statUnitGeom.getArea(), "indic", "area_density", "geo", statUnitId));
				geoStatsHC.stats.add(new Stat(lengthGeo/statUnitGeom.getArea(), "indic", "length_density", "geo", statUnitId));
			}
			itStat.close();

			return geoStatsHC;
		} catch (MalformedURLException e) { e.printStackTrace();
		} catch (IOException e) { e.printStackTrace(); }
		return null;
	}


	//step 3
	public StatsHypercube finalStatsSimplifiedHC;

	public void computeFinalStat() {
		try {
			StatsIndex geoStatsInitialHCI = new StatsIndex(geoStatsInitialHC, "indic", "geo");
			StatsIndex geoStatsFinalHCI = new StatsIndex(geoStatsFinalHC, "indic", "geo");

			//initialise output structure
			finalStatsSimplifiedHC = new StatsHypercube();
			finalStatsSimplifiedHC.dimLabels.add("geo");

			//prepare
			FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

			//go through initial statistical units
			int statCounter = 1, nbStats = statUnitsInitialFeatureStore.getFeatures().size();
			FeatureIterator<SimpleFeature> itStatIni = ((SimpleFeatureCollection) statUnitsInitialFeatureStore.getFeatures()).features();
			while (itStatIni.hasNext()) {
				SimpleFeature statUnitIni = itStatIni.next();
				String statUnitIniId = statUnitIni.getAttribute(statUnitsInitialIdFieldName).toString();

				System.out.println(statUnitIniId + " " + (statCounter++) + "/" + nbStats + " " + (Math.round(10000*statCounter/nbStats))*0.01 + "%");

				//get all final stat units intersecting the initial stat unit (with spatial index)
				Geometry statUnitIniGeom = (Geometry) statUnitIni.getDefaultGeometryProperty().getValue();
				Filter f = ff.bbox(ff.property("the_geom"), statUnitIni.getBounds());
				FeatureIterator<SimpleFeature> itStatFin = ((SimpleFeatureCollection) statUnitsFinalFeatureStore.getFeatures(f)).features();

				String indic = "number"; //TODO generalise
				//get statistical value
				double iniStatValue = statValuesInitial.getSingleValue(statUnitIniId);
				if(Double.isNaN(iniStatValue)) continue;
				//get geostat value for ini
				double iniGeoStatValue = geoStatsInitialHCI.getSingleValue(indic, statUnitIniId);
				if(Double.isNaN(iniGeoStatValue) || iniGeoStatValue == 0) continue;

				//compute stat on stat unit fin
				while (itStatFin.hasNext()) {
					try {
						SimpleFeature statUnitFin = itStatFin.next();
						Geometry statUnitFinGeom = (Geometry) statUnitFin.getDefaultGeometryProperty().getValue();
						if(!statUnitIniGeom.intersects(statUnitFinGeom)) continue;
						Geometry inter = statUnitIniGeom.intersection(statUnitFinGeom);
						if(inter.getArea()/statUnitFinGeom.getArea() < 0.75) continue;

						String statUnitFinId = statUnitFin.getAttribute(statUnitsFinalIdFieldName).toString();

						//System.out.println("   "+statUnitFinId + " " + (inter.getArea()/statUnitFinGeom.getArea()));

						//compute value

						//get geostat value for fin
						double finGeoStatValue = geoStatsFinalHCI.getSingleValue(indic, statUnitFinId);
						if(Double.isNaN(finGeoStatValue)) continue;

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

		} catch (MalformedURLException e) { e.printStackTrace();
		} catch (IOException e) { e.printStackTrace(); }
	}





	//Step 2b

	//the output value of the second step: statistical values at geo features level
	public StatsHypercube statsGeoAllocationHC;

	public void allocateStatGeo() {
		try {
			StatsIndex geoStatsInitialHCI = new StatsIndex(geoStatsInitialHC, "indic", "geo");

			//initialise output structure
			statsGeoAllocationHC = new StatsHypercube();
			statsGeoAllocationHC.dimLabels.add("geo");

			//prepare
			FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

			//go through geo - purpose is to compute geo statistical value
			double geoCounter = 1, nbGeo = geoFeatureStore.getFeatures().size();
			FeatureIterator<SimpleFeature> itGeo = ((SimpleFeatureCollection) geoFeatureStore.getFeatures()).features();
			while (itGeo.hasNext()) {
				SimpleFeature geoUnit = itGeo.next();
				String geoId = geoUnit.getAttribute(geoIdFieldName).toString();
				Geometry geoGeom = (Geometry) geoUnit.getDefaultGeometryProperty().getValue();
				int geomCase = geoGeom.getArea()>0? 3 : geoGeom.getLength()>0? 2 : 1;

				System.out.println("geoid: " + geoId + " " + (geoCounter++) + "/" + nbGeo + " " + (Math.round(10000.0*geoCounter/nbGeo)*0.01) + "%");

				//get all stat units intersecting the geo (with spatial index)
				Filter f = ff.bbox(ff.property("the_geom"), geoUnit.getBounds());
				FeatureIterator<SimpleFeature> itStat = ((SimpleFeatureCollection) statUnitsInitialFeatureStore.getFeatures(f)).features();

				int nbStat = 0;
				double geoStatValue = 0;
				//geoStatValue = Sum on SUs intersecting of:  surf(geo inter su)/statUnitGeoTotalArea * statUnitValue
				while (itStat.hasNext()) {
					try {
						SimpleFeature stat = itStat.next();
						String statId = stat.getAttribute(statUnitsInitialIdFieldName).toString();

						//get stat unit geometry
						Geometry statUnitGeom = (Geometry) stat.getDefaultGeometryProperty().getValue();
						if(!geoGeom.intersects(statUnitGeom)) continue;

						nbStat++;

						//get stat unit value
						double statValue = statValuesInitial.getSingleValue(statId);
						if(Double.isNaN(statValue) || statValue == 0) continue;

						//increment with stat unit geostat value
						if(geomCase == 3){
							//area case
							double statUnitGeoStatValue = geoStatsInitialHCI.getSingleValue("area", statId);
							if(!Double.isNaN(statUnitGeoStatValue) && statUnitGeoStatValue != 0)
								geoStatValue += geoGeom.intersection(statUnitGeom).getArea() / statUnitGeoStatValue * statValue;
						} else if(geomCase == 2){
							//line case
							double statUnitGeoStatValue = geoStatsInitialHCI.getSingleValue("length", statId);
							if(!Double.isNaN(statUnitGeoStatValue) && statUnitGeoStatValue != 0)
								geoStatValue += geoGeom.intersection(statUnitGeom).getLength() / statUnitGeoStatValue * statValue;
						} else {
							//point case
							double statUnitGeoStatValue = geoStatsInitialHCI.getSingleValue("number", statId);
							if(!Double.isNaN(statUnitGeoStatValue) && statUnitGeoStatValue != 0)
								geoStatValue += geoGeom.intersection(statUnitGeom).getCoordinates().length / statUnitGeoStatValue * statValue;
						}

					} catch (TopologyException e) {
						System.err.println("Topology error.");
					}
				}
				itStat.close();

				if(nbStat == 0) continue;

				//store
				statsGeoAllocationHC.stats.add(new Stat(geoStatValue, "geo", geoId));
				//statsGeoAllocationHC.stats.add(new Stat(nbStat, "indic", "number", "geo", geoId));
			}
			itGeo.close();
		} catch (Exception e) { e.printStackTrace(); }
	}


	//Step 3b

	public StatsHypercube finalStatsHC;

	public void aggregateGeoStat() {
		try {
			//the statistics allocates at geo level
			StatsIndex statsGeoAllocationI = new StatsIndex(statsGeoAllocationHC, "geo");

			//initialise output structure
			finalStatsHC = new StatsHypercube();
			finalStatsHC.dimLabels.add("geo");

			//prepare
			FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

			//go through statistical units
			int statCounter = 1, nbStats = statUnitsFinalFeatureStore.getFeatures().size();
			FeatureIterator<SimpleFeature> itStat = ((SimpleFeatureCollection) statUnitsFinalFeatureStore.getFeatures()).features();
			while (itStat.hasNext()) {
				SimpleFeature statUnit = itStat.next();
				String statUnitId = statUnit.getAttribute(statUnitsFinalIdFieldName).toString();

				System.out.println(statUnitId + " " + (statCounter++) + "/" + nbStats + " " + (Math.round(10000*statCounter/nbStats))*0.01 + "%");

				//get all geo features intersecting the stat unit (with spatial index)
				Geometry statUnitGeom = (Geometry) statUnit.getDefaultGeometryProperty().getValue();
				//Filter f = ff.bbox(ff.property("the_geom"), statUnit.getBounds());
				Filter f = ff.intersects(ff.property("the_geom"), ff.literal(statUnitGeom));

				//compute stat on geo features
				double statSum=0, weightsSum=0;
				FeatureIterator<SimpleFeature> itGeo = ((SimpleFeatureCollection) geoFeatureStore.getFeatures(f)).features();
				while (itGeo.hasNext()) {
					try {
						SimpleFeature geo = itGeo.next();
						String geoId = geo.getAttribute(geoIdFieldName).toString();
						Geometry geoGeom = (Geometry) geo.getDefaultGeometryProperty().getValue();
						if(geoGeom.getArea()==0 && geoGeom.getLength()==0) continue;
						if(!statUnitGeom.intersects(geoGeom)) continue;

						int geomCase = geoGeom.getArea()>0? 3 : geoGeom.getLength()>0? 2 : 1;

						//get statistics allocates at geo level
						double geoValue = statsGeoAllocationI.getSingleValue(geoId);
						if(Double.isNaN(geoValue)) continue;

						double weight = 0;
						Geometry inter = geoGeom.intersection(statUnitGeom);
						if(geomCase == 3){
							//area case
							weight = inter.getArea();
						} else if(geomCase == 2){
							//line case
							weight = inter.getLength();
						} else {
							//point case
							weight = inter.getCoordinates().length;
						}

						weightsSum += weight;
						statSum += weight * geoValue;
					} catch (TopologyException e) {
						System.err.println("Topology error for intersection computation");
					}
				}
				itGeo.close();

				if(weightsSum == 0) continue;

				//store
				finalStatsHC.stats.add(new Stat(statSum/weightsSum, "geo", statUnitId));
			}
			itStat.close();
		} catch (MalformedURLException e) { e.printStackTrace();
		} catch (IOException e) { e.printStackTrace(); }
	}

}
