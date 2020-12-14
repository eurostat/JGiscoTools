/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridaccessibility;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;
import eu.europa.ec.eurostat.jgiscotools.routing.AccessibilityRoutingPaths;
import eu.europa.ec.eurostat.jgiscotools.routing.SpeedCalculator;

/**
 * @author julien Gaffuri
 *
 */
public class BasicServicesRoutingPaths {
	private static Logger logger = LogManager.getLogger(BasicServicesRoutingPaths.class.getName());

	//show where cross-border cooperation can improve accessibility

	//use: -Xms2G -Xmx12G
	/** @param args 
	 * @throws Exception **/
	public static void main(String[] args) throws Exception {
		logger.info("Start");

		//set logger level
		//Configurator.setLevel(AccessibilityRoutingPaths.class.getName(), Level.ALL);

		String basePath = "E:/workspace/basic_services_accessibility/";
		String outPath = basePath + "routing_paths/";
		CoordinateReferenceSystem crs = CRS.decode("EPSG:3035");
		int resKM = 1;
		//set the country id (set to null for all countries)
		String cnt = "FR";

		logger.info("Load POIs");
		//TODO decompose by education type
		//TODO make health as well
		//String serviceType = "healthcare";
		//ArrayList<Feature> pois = GeoData.getFeatures(basePath + "input_data/"+serviceType+"_services_LAEA.gpkg",null, cnt==null?null:CQL.toFilter("cc = '"+cnt+"'"));
		String serviceType = "education_1"; int level = 1;
		ArrayList<Feature> pois = GeoData.getFeatures(basePath + "input_data/"+serviceType+"_services_LAEA.gpkg",null, CQL.toFilter("levels LIKE '%"+level+"%'" + cnt==null?"":"AND cc = '"+cnt+"'") );
		logger.info(pois.size() + " POIs");

		logger.info("Load network sections...");
		Collection<Feature> networkSections = RoadBDTopo.get();
		SpeedCalculator sc = RoadBDTopo.getSpeedCalculator();
		logger.info(networkSections.size() + " sections loaded.");

		logger.info("Load grid cells " + resKM + "km ...");
		ArrayList<Feature> cells = GeoData.getFeatures(basePath + "input_data/grid_"+resKM+"km_surf.gpkg",null, CQL.toFilter("NOT TOT_P_2011=0" + (cnt==null?"":"AND CNTR_ID = '"+cnt+"'")));
		logger.info(cells.size() + " cells");

		logger.info("Build accessibility...");
		AccessibilityRoutingPaths ag = new AccessibilityRoutingPaths(cells, "GRD_ID", 1000*resKM, pois, "id", networkSections, 4, 50000);
		ag.setEdgeWeighter(sc);

		logger.info("Compute accessibility paths...");
		ag.compute();

		logger.info("Save routes... Nb=" + ag.getRoutes().size());
		GeoData.save(ag.getRoutes(), outPath + "routes_"+(cnt==null?"":cnt+"_")+resKM+"km"+"_"+serviceType+".gpkg", crs, true);

		logger.info("End");
	}

}
