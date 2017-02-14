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

	//the initial statistical values to disaggregate
	private StatsIndex statValuesInitial;

	//the initial statistical units
	private SimpleFeatureStore statUnitsInitialFeatureStore;
	private String statUnitsInitialId;

	//the geographical features
	private SimpleFeatureStore geoFeatureStore;
	private String geoId;
	//TODO handle several 

	//the target statistical units (as new geographical level)
	private SimpleFeatureStore statUnitsFinalFeatureStore;


	//default constructor
	public DasymetricMapping(SimpleFeatureStore statUnitsInitialFeatureStore,
			String statUnitsInitialId,
			StatsIndex statValuesInitial,
			SimpleFeatureStore geoFeatureStore,
			String geoId,
			SimpleFeatureStore statUnitsFinalFeatureStore){
		this.statUnitsInitialFeatureStore = statUnitsInitialFeatureStore;
		this.statUnitsInitialId = statUnitsInitialId;
		this.statValuesInitial = statValuesInitial;
		this.geoFeatureStore = geoFeatureStore;
		this.geoId = geoId;
		this.statUnitsFinalFeatureStore = statUnitsFinalFeatureStore;
	}


	public void run(){
		//Step 1: compute statistics on geo features at initial stat unit level
		computeGeoStat();

		//Step 2: allocate statistics at geo features level
		allocateStatGeo();

		//Step 3: aggregate statistics at target stat unit level
		aggregateGeoStat();
	}



	//Step 1

	//the output value of the first step: statistical values on the geo features, at the initial stat units level 
	public StatsHypercube geoStatsHC;

	public void computeGeoStat() {
		try {
			//initialise output structure
			geoStatsHC = new StatsHypercube();
			geoStatsHC.dimLabels.add("geo");
			geoStatsHC.dimLabels.add("indic");

			//prepare
			FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

			//go through statistical units
			int statCounter = 1, nbStats = statUnitsInitialFeatureStore.getFeatures().size();
			FeatureIterator<SimpleFeature> itStat = ((SimpleFeatureCollection) statUnitsInitialFeatureStore.getFeatures()).features();
			while (itStat.hasNext()) {
				SimpleFeature statUnit = itStat.next();
				String statUnitId = statUnit.getAttribute(statUnitsInitialId).toString();

				System.out.println(statUnitId + " " + (statCounter++) + "/" + nbStats + " " + (Math.round(10000*statCounter/nbStats))*0.01 + "%");

				//get all geo features intersecting the stat unit (with spatial index)
				Geometry StatUnitGeom = (Geometry) statUnit.getDefaultGeometryProperty().getValue();
				Filter f = ff.bbox(ff.property("the_geom"), statUnit.getBounds());
				FeatureIterator<SimpleFeature> itGeo = ((SimpleFeatureCollection) geoFeatureStore.getFeatures(f)).features();

				//compute stat on geo features
				int numberGeo=0; double areaGeo=0, lengthGeo=0;
				while (itGeo.hasNext()) {
					try {
						SimpleFeature geo = itGeo.next();
						Geometry geoGeom = (Geometry) geo.getDefaultGeometryProperty().getValue();
						if(!geoGeom.intersects(StatUnitGeom)) continue;

						numberGeo++;
						Geometry inter = geoGeom.intersection(StatUnitGeom);
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
				geoStatsHC.stats.add(new Stat(areaGeo/StatUnitGeom.getArea(), "indic", "area_density", "geo", statUnitId));
				geoStatsHC.stats.add(new Stat(lengthGeo/StatUnitGeom.getArea(), "indic", "length_density", "geo", statUnitId));
			}
			itStat.close();

		} catch (MalformedURLException e) { e.printStackTrace();
		} catch (IOException e) { e.printStackTrace(); }
	}



	//Step 2
	public void allocateStatGeo() {
		//TODO
	}

	//Step 3
	public void aggregateGeoStat() {
		//TODO
	}

}
