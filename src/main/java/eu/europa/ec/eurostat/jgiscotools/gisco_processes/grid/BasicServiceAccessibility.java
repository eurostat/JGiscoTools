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
public class BasicServiceAccessibility {
	private static Logger logger = Logger.getLogger(BasicServiceAccessibility.class.getName());

	//show where cross-border cooperation can improve accessibility

	//use: -Xms2G -Xmx12G
	public static void main(String[] args) throws Exception {
		logger.info("Start");

		//logger.setLevel(Level.ALL);

		String basePath = "E:/workspace/gridstat/";
		String outPath = basePath + "accessibility_output/";
		String gridpath = basePath + "data/grid/";
		CoordinateReferenceSystem crs = CRS.decode("EPSG:3035");

		//set the country id (set to null for all countries)
		String cnt = null;

		int resKM = 5;
		logger.info("Load grid cells " + resKM + "km ...");
		String cellIdAtt = "GRD_ID";
		ArrayList<Feature> cells = GeoPackageUtil.getFeatures(gridpath + "grid_"+resKM+"km.gpkg" , cnt==null?null:CQL.toFilter("CNTR_ID = '"+cnt+"'"));
		logger.info(cells.size() + " cells");


		logger.info("Load network sections...");
		//ERM
		//TODO show map of transport network (EGM/ERM) based on speed
		//TODO add other transport networks (ferry, etc?)
		//EXS Existence Category - RST Road Surface Type
		Filter fil = CQL.toFilter("((EXS=28 OR EXS=0) AND (RST=1 OR RST=0))" + (cnt==null?"":" AND (ICC = '"+cnt+"')") );
		//Collection<Feature> networkSections = GeoPackageUtil.getFeatures(basePath+"data/RoadL.gpkg", fil);
		String egpath = "E:/dissemination/shared-data/";
		Collection<Feature> networkSections = SHPUtil.getFeatures(egpath+"ERM/shp-gdb/ERM_2019.1_shp_LAEA/Data/RoadL_RTT_14_15_16.shp", fil);
		networkSections.addAll( SHPUtil.getFeatures(egpath+"ERM/shp-gdb/ERM_2019.1_shp_LAEA/Data/RoadL_RTT_984.shp", fil) );
		networkSections.addAll( SHPUtil.getFeatures(egpath+"ERM/shp-gdb/ERM_2019.1_shp_LAEA/Data/RoadL_RTT_0.shp", fil) );
		logger.info(networkSections.size() + " sections loaded.");


		//- GST = GF0306: Rescue service
		//- GST = GF0703: Hospital service
		//- GST = GF090102: Primary education (ISCED-97 Level 1): Primary schools
		//- GST = GF0902: Secondary education (ISCED-97 Level 2, 3): Secondary schools
		//- GST = GF0904: Tertiary education (ISCED-97 Level 5, 6): Universities
		//- GST = GF0905: Education not definable by level

		//TODO define object
		for(Object accMap : new Object[] {
				new Object[] { "healthcare", "GST = 'GF0703' OR GST = 'GF0306'" } ,
				//new Object[] { "educ3", "GST = 'GF0904'" },
				new Object[] { "educ2", "GST = 'GF0902'" },
				new Object[] { "educ1", "GST = 'GF090102'" }} ) {

			String poiLabel = ((Object[])accMap)[0].toString();
			String poiFilter = ((Object[])accMap)[1].toString();

			logger.info("Load POIs " + poiLabel + "...");
			ArrayList<Feature> pois = GeoPackageUtil.getFeatures(basePath+"/data/GovservP.gpkg", CQL.toFilter("("+poiFilter +")"+ (cnt==null?"":" AND (ICC = '"+cnt+"')") ));
			logger.info(pois.size() + " POIs");


			logger.info("Build accessibility...");
			AccessibilityGrid ag = new AccessibilityGrid(cells, cellIdAtt, resKM*1000, pois, networkSections, "TOT_P_2011");
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
			//TODO
			//ag.minDurAccMinT = 

			logger.info("Compute accessibility...");
			ag.compute();

			logger.info("Save data...");
			CSVUtil.save(ag.getCellData(), outPath + "accessibility_"+(cnt==null?"":cnt+"_")+resKM+"km"+"_"+poiLabel+".csv");
			logger.info("Save routes... Nb=" + ag.getRoutes().size());
			GeoPackageUtil.save(ag.getRoutes(), outPath + "routes_"+(cnt==null?"":cnt+"_")+resKM+"km"+"_"+poiLabel+".gpkg", crs, true);

		}

		logger.info("End");
	}

}
