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
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;
import eu.europa.ec.eurostat.jgiscotools.routing.AccessibilityGrid;

/**
 * @author julien Gaffuri
 *
 */
public class Education1Accessibility {
	private static Logger logger = LogManager.getLogger(Education1Accessibility.class.getName());

	//show where cross-border cooperation can improve accessibility

	//use: -Xms2G -Xmx12G
	public static void main(String[] args) throws Exception {
		logger.info("Start");

		//logger.setLevel(Level.ALL);

		String basePath = "E:/workspace/basic_services_accessibility/";
		String outPath = basePath + "accessibility_output/";
		CoordinateReferenceSystem crs = CRS.decode("EPSG:3035");
		int resKM = 10;

		//set the country id (set to null for all countries)
		String cnt = "FR";

		logger.info("Load grid cells " + resKM + "km ...");
		String cellIdAtt = "GRD_ID";
		ArrayList<Feature> cells = GeoData.getFeatures(basePath + "input_data/grid_"+resKM+"km_surf.gpkg",null, cnt==null?null:CQL.toFilter("CNTR_ID = '"+cnt+"'"));
		logger.info(cells.size() + " cells");


		logger.info("Load network sections...");
		//BD TOPO
		Filter fil = CQL.toFilter("(NOT NATURE='Sentier' AND NOT NATURE='Chemin' AND NOT NATURE='Piste Cyclable' AND NOT NATURE='Escalier')");
		Collection<Feature> networkSections = GeoData.getFeatures(basePath + "input_data/test_NMCA_FR_road_tn/roads.gpkg", null, fil);
		logger.info(networkSections.size() + " sections loaded.");
		
		String label = "schools";

		logger.info("Load POIs");
		ArrayList<Feature> pois = GeoData.getFeatures(basePath + "input_data/education_services.gpkg",null, cnt==null?null:CQL.toFilter("cc = '"+cnt+"'"));
		logger.info(pois.size() + " POIs");

		logger.info("Build accessibility...");
		double minDurAccMinT = 40;
		AccessibilityGrid ag = new AccessibilityGrid(cells, cellIdAtt, resKM*1000, pois, networkSections, "TOT_P_2011", minDurAccMinT);
		//ag.setEdgeWeighter(sc);

		logger.info("Compute accessibility...");
		ag.compute();

		logger.info("Save data...");
		CSVUtil.save(ag.getCellData(), outPath + "accessibility_"+(cnt==null?"":cnt+"_")+resKM+"km"+"_"+label+".csv");
		logger.info("Save routes... Nb=" + ag.getRoutes().size());
		GeoData.save(ag.getRoutes(), outPath + "routes_"+(cnt==null?"":cnt+"_")+resKM+"km"+"_"+label+".gpkg", crs, true);

		logger.info("End");
	}

}
