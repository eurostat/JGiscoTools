/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.grid;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;
import eu.europa.ec.eurostat.jgiscotools.io.GeoPackageUtil;
import eu.europa.ec.eurostat.jgiscotools.io.SHPUtil;
import eu.europa.ec.eurostat.jgiscotools.routing.AccessibilityGrid;
import eu.europa.ec.eurostat.jgiscotools.routing.AccessibilityGrid.SpeedCalculator;

/**
 * @author julien Gaffuri
 *
 */
public class EurostatHospitalAccessibility {
	private static Logger logger = Logger.getLogger(EurostatHospitalAccessibility.class.getName());

	//https://krankenhausatlas.statistikportal.de/
	//show where X-border cooperation can improve accessibility
	//kernel smoothing

	//use: -Xms2G -Xmx12G
	public static void main(String[] args) throws Exception {
		logger.info("Start");

		//logger.setLevel(Level.ALL);

		String basePath = "E:/workspace/gridstat/";
		String outPath = basePath + "hospital_accessibility_output/";
		String gridpath = basePath + "data/grid/";
		String egpath = "E:/dissemination/shared-data/";
		CoordinateReferenceSystem crs = CRS.decode("EPSG:3035");


		logger.info("Load grid cells");
		int resKM = 100;
		ArrayList<Feature> cells = GeoPackageUtil.getFeatures(gridpath + "grid_"+resKM+"km.gpkg" /*,CQL.toFilter("CNTR_ID = 'BE'")*/);
		logger.info(cells.size() + " cells");


		logger.info("Load POIs");
		//TODO test others POI sources and types: tomtom, osm
		ArrayList<Feature> pois = SHPUtil.loadSHP(egpath+"ERM/ERM_2019.1_shp_LAEA/Data/GovservP.shp", CQL.toFilter("GST = 'GF0703'" /*+ " AND ICC = 'BE'"*/ )).fs;
		logger.info(pois.size() + " POIs");
		//- GST = GF0306: Rescue service
		//- GST = GF0703: Hospital service
		//- GST = GF090102: Primary education (ISCED-97 Level 1): Primary schools
		//- GST = GF0902: Secondary education (ISCED-97 Level 2, 3): Secondary schools
		//- GST = GF0904: Tertiary education (ISCED-97 Level 5, 6): Universities
		//- GST = GF0905: Education not definable by level



		//TODO show map of transport network (EGM/ERM) based on speed
		//TODO correct networks - snapping
		//TODO load other transport networks (ferry, etc?)
		logger.info("Load network sections");
		Filter fil = CQL.toFilter("EXS=28 AND RST=1" /*+ " AND ICC = 'BE'"*/);
		//EGM
		//Collection<Feature> networkSections = SHPUtil.loadSHP(egpath+"EGM/EGM_2019_SHP_20190312_LAEA/DATA/FullEurope/RoadL.shp", fil).fs;
		//ERM
		Collection<Feature> networkSections = SHPUtil.loadSHP(egpath+"ERM/ERM_2019.1_shp_LAEA/Data/RoadL_RTT_14_15_16.shp", fil).fs;
		networkSections.addAll( SHPUtil.loadSHP(egpath+"ERM/ERM_2019.1_shp_LAEA/Data/RoadL_RTT_984.shp", fil).fs );
		networkSections.addAll( SHPUtil.loadSHP(egpath+"ERM/ERM_2019.1_shp_LAEA/Data/RoadL_RTT_0.shp", fil).fs );

		logger.info(networkSections.size() + " sections loaded.");




		logger.info("Build accessibility");
		AccessibilityGrid ag = new AccessibilityGrid(cells, resKM*1000, pois, networkSections);
		ag.setEdgeWeighter(new SpeedCalculator() {
			@Override
			public double getSpeedKMPerHour(SimpleFeature sf) {
				//estimate speed of a transport section of ERM/EGM based on attributes
				//COR - Category of Road - 0 Unknown - 1 Motorway - 2 Road inside built-up area - 999 Other road (outside built-up area)
				//RTT - Route Intended Use - 0 Unknown - 16 National motorway - 14 Primary route - 15 Secondary route - 984 Local route
				String cor = sf.getAttribute("COR").toString();
				if(cor==null) { logger.warn("No COR attribute for feature "+sf.getID()); return 0; };
				String rtt = sf.getAttribute("RTT").toString();
				if(rtt==null) { logger.warn("No RTT attribute for feature "+sf.getID()); return 0; };

				//motorways
				if("1".equals(cor) || "16".equals(rtt)) return 110.0;
				//city roads
				if("2".equals(cor)) return 50.0;
				//fast roads
				if("14".equals(rtt) || "15".equals(rtt)) return 80.0;
				//local road
				if("984".equals(rtt)) return 80.0;
				return 50.0;
			}});

		logger.info("Compute accessibility");
		ag.compute();

		//logger.info("Compute accessibility indicator, with population");
		//ArrayList<HashMap<String, String>> pop = CSVUtil.load("population.csv");
		//HashMap<String, String> popData = Util.index(pop, "cellId", "value");
		//ag.computePopulationAccessibilityIndicator(popData);


		logger.info("Save data");
		CSVUtil.save(ag.getCellData(), outPath + "cell_data_"+resKM+"km.csv");
		logger.info("Save routes. Nb=" + ag.getRoutes().size());
		SHPUtil.saveSHP(ag.getRoutes(), outPath + "routes_"+resKM+"km.shp", crs);

		logger.info("End");
	}

}
