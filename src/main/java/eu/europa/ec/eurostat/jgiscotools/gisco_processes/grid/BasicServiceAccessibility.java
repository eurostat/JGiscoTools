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
import eu.europa.ec.eurostat.jgiscotools.routing.AccessibilityGrid;
import eu.europa.ec.eurostat.jgiscotools.routing.AccessibilityGrid.SpeedCalculator;

/**
 * @author julien Gaffuri
 *
 */
public class BasicServiceAccessibility {
	private static Logger logger = Logger.getLogger(BasicServiceAccessibility.class.getName());

	//show where cross-border cooperation can improve accessibility

	//use: -Xms2G -Xmx12G
	public static void main(String[] args) throws Exception {
		logger.info("Start");

		//logger.setLevel(Level.ALL);

		String basePath = "E:/workspace/gridstat/";
		String outPath = basePath + "accessibility_output/";
		String egPath = "E:/dissemination/shared-data/";
		CoordinateReferenceSystem crs = CRS.decode("EPSG:3035");

		//set the country id (set to null for all countries)
		String cnt = null;

		int resKM = 10;
		logger.info("Load grid cells " + resKM + "km ...");
		String cellIdAtt = "GRD_ID";
		ArrayList<Feature> cells = GeoPackageUtil.getFeatures(basePath + "grid/grid_"+resKM+"km.gpkg" , cnt==null?null:CQL.toFilter("CNTR_ID = '"+cnt+"'"));
		logger.info(cells.size() + " cells");


		logger.info("Load network sections...");

		//ERM
		//TODO add other transport networks (ferry, etc?)
		//EXS Existence Category - RST Road Surface Type
		Filter fil = CQL.toFilter("((EXS=28 OR EXS=0) AND (RST=1 OR RST=0))" + (cnt==null?"":" AND (ICC = '"+cnt+"')") );
		Collection<Feature> networkSections = GeoPackageUtil.getFeatures(egPath+ "ERM/gpkg/ERM_2019.1_LAEA/RoadL.gpkg", fil);
		//Collection<Feature> networkSections = SHPUtil.getFeatures(egpath+"ERM/shp-gdb/ERM_2019.1_shp_LAEA/Data/RoadL_RTT_14_15_16.shp", fil);
		//networkSections.addAll( SHPUtil.getFeatures(egpath+"ERM/shp-gdb/ERM_2019.1_shp_LAEA/Data/RoadL_RTT_984.shp", fil) );
		//networkSections.addAll( SHPUtil.getFeatures(egpath+"ERM/shp-gdb/ERM_2019.1_shp_LAEA/Data/RoadL_RTT_0.shp", fil) );
		SpeedCalculator sc = new SpeedCalculator() {
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
			}
		};
		logger.info(networkSections.size() + " sections loaded.");

		//TODO test tomtom
		/*
		U:/GISCO/archive/road-network/raw-deliveries/tomtom/2018/EETN2018/EU/eur2018_12_000/shpd/mn/lux/lux/luxlux___________00.shp
		0 to 5
		U:/GISCO/archive/road-network/raw-deliveries/tomtom/2018/EETN2018/EU/eur2018_12_000/shpd/mn/lux/lux/luxlux___________mn.shp
		MINUTES
		 */

		//- GST = GF0306: Rescue service
		//- GST = GF0703: Hospital service
		//- GST = GF090102: Primary education (ISCED-97 Level 1): Primary schools
		//- GST = GF0902: Secondary education (ISCED-97 Level 2, 3): Secondary schools
		//- GST = GF0904: Tertiary education (ISCED-97 Level 5, 6): Universities
		//- GST = GF0905: Education not definable by level

		//TODO define object



		final class Case {
			String label, filter;
			double minDurAccMinT;
			public Case(String label, String filter, double minDurAccMinT) {
				this.label = label;
				this.filter = filter;
				this.minDurAccMinT = minDurAccMinT;
			}
		};

		for(Case c : new Case[] {
				new Case("healthcare", "GST = 'GF0703' OR GST = 'GF0306'", 15),
				new Case("educ1", "GST = 'GF090102'", 10),
				new Case("educ2", "GST = 'GF0902'", 20),
				new Case("educ3", "GST = 'GF0904'", 60)
		}) {

			logger.info("Load POIs " + c.label + "...");
			ArrayList<Feature> pois = GeoPackageUtil.getFeatures(egPath+"ERM/gpkg/ERM_2019.1_LAEA/GovservP.gpkg", CQL.toFilter("("+c.filter +")"+ (cnt==null?"":" AND (ICC = '"+cnt+"')") ));
			logger.info(pois.size() + " POIs");


			logger.info("Build accessibility...");
			AccessibilityGrid ag = new AccessibilityGrid(cells, cellIdAtt, resKM*1000, pois, networkSections, "TOT_P_2011", c.minDurAccMinT);
			ag.setEdgeWeighter(sc);

			logger.info("Compute accessibility...");
			ag.compute();

			logger.info("Save data...");
			CSVUtil.save(ag.getCellData(), outPath + "accessibility_"+(cnt==null?"":cnt+"_")+resKM+"km"+"_"+c.label+".csv");
			logger.info("Save routes... Nb=" + ag.getRoutes().size());
			GeoPackageUtil.save(ag.getRoutes(), outPath + "routes_"+(cnt==null?"":cnt+"_")+resKM+"km"+"_"+c.label+".gpkg", crs, true);

		}

		logger.info("End");
	}

}
