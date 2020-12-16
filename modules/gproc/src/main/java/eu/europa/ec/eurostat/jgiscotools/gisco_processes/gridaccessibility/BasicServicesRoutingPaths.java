/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridaccessibility;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.referencing.CRS;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import eu.europa.ec.eurostat.java4eurostat.base.StatsHypercube;
import eu.europa.ec.eurostat.java4eurostat.io.CSV;
import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;
import eu.europa.ec.eurostat.jgiscotools.routing.AccessibilityRoutingPaths;

/**
 * @author julien Gaffuri
 *
 */
public class BasicServicesRoutingPaths {
	private static Logger logger = LogManager.getLogger(BasicServicesRoutingPaths.class.getName());

	private static String basePath = "E:/workspace/basic_services_accessibility/";
	private static String cnt = "FR";
	private static boolean computeStats = true;

	static ArrayList<Case> cases = new ArrayList<Case>();
	static {
		try {
			//TODO group them
			cases.add(new Case("healthcare", basePath + "input_data/healthcare_services_LAEA.gpkg", cnt==null?null:CQL.toFilter("cc = '"+cnt+"'")));
			//cases.add(new Case("educ_1", basePath + "input_data/education_services_LAEA.gpkg", CQL.toFilter("levels LIKE '%1%'" + (cnt==null?"":" AND cc = '"+cnt+"'"))));
			//cases.add(new Case("educ_2", basePath + "input_data/education_services_LAEA.gpkg", CQL.toFilter("levels LIKE '%2%'" + (cnt==null?"":" AND cc = '"+cnt+"'"))));
			//cases.add(new Case("educ_3", basePath + "input_data/education_services_LAEA.gpkg", CQL.toFilter("levels LIKE '%3%'" + (cnt==null?"":" AND cc = '"+cnt+"'"))));
		} catch (CQLException e) { e.printStackTrace(); }
	}



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

		logger.info("Load network sections...");
		//TODO use more generalised road TN for healthcare ?
		Collection<Feature> networkSections = RoadBDTopo.get("cost");
		logger.info(networkSections.size() + " sections loaded.");

		logger.info("Load grid cells " + resKM + "km ...");
		//ArrayList<Feature> cells = GeoData.getFeatures(basePath + "input_data/grid_"+resKM+"km_surf.gpkg",null, CQL.toFilter("NOT TOT_P_2011=0" + (cnt==null?"":"AND CNTR_ID = '"+cnt+"'")));
		ArrayList<Feature> cells = GeoData.getFeatures(basePath + "input_data/grid_1km_surf_FRL0.gpkg",null);

		logger.info(cells.size() + " cells");

		for(Case c : cases ) {
			logger.info("Case: " + c.label);

			logger.info("Load POIs");
			ArrayList<Feature> pois = GeoData.getFeatures(c.gpkgPath, null, c.filter);
			logger.info(pois.size() + " POIs");

			logger.info("Build accessibility...");
			AccessibilityRoutingPaths ag = new AccessibilityRoutingPaths(cells, "GRD_ID", 1000*resKM, pois, "id", networkSections, "cost", 3, 50000, true);

			logger.info("Compute accessibility paths...");
			ag.compute();

			logger.info("Save routes... Nb=" + ag.getRoutes().size());
			GeoData.save(ag.getRoutes(), outPath + "routes_"+(cnt==null?"":cnt+"_")+resKM+"km"+"_"+c.label+".gpkg", crs, true);

			if(computeStats) {
				logger.info("compute stats");
				StatsHypercube hc = AccessibilityRoutingPaths.computeStats(ag.getRoutes(), "GRD_ID");

				logger.info("save stats");
				CSV.saveMultiValues(hc, basePath+"accessibility_output/routing_paths_"+c.label+"_stats.csv", "accInd");
			}
		}

		logger.info("End");
	}


	static class Case {
		public String label;
		public String gpkgPath;
		public Filter filter;
		public Case(String label, String gpkgPath, Filter filter) {
			this.label = label;
			this.gpkgPath = gpkgPath;
			this.filter = filter;
		}
	}

}
