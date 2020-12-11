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
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;
import eu.europa.ec.eurostat.jgiscotools.routing.AccessibilityGrid;
import eu.europa.ec.eurostat.jgiscotools.routing.AccessibilityGrid.SpeedCalculator;

/**
 * @author julien Gaffuri
 *
 */
public class BasicServiceAccessibility {
	private static Logger logger = LogManager.getLogger(BasicServiceAccessibility.class.getName());

	//show where cross-border cooperation can improve accessibility

	//use: -Xms2G -Xmx12G
	public static void main(String[] args) throws Exception {
		logger.info("Start");

		//logger.setLevel(Level.ALL);

		String basePath = "E:/workspace/gridstat/";
		String outPath = basePath + "accessibility_output/";
		CoordinateReferenceSystem crs = CRS.decode("EPSG:3035");
		int resKM = 10;

		//set the country id (set to null for all countries)
		String cnt = null;

		logger.info("Load grid cells " + resKM + "km ...");
		String cellIdAtt = "GRD_ID";
		ArrayList<Feature> cells = GeoData.getFeatures(basePath + "input_data/grid_"+resKM+"km.gpkg",null, cnt==null?null:CQL.toFilter("CNTR_ID = '"+cnt+"'"));
		logger.info(cells.size() + " cells");


		logger.info("Load network sections...");
		//BD TOPO
		Filter fil = CQL.toFilter("(NATURE != 'Sentier' AND NATURE != 'Chemin' AND NATURE != 'Piste Cyclable' AND NATURE != 'Escalier')");
		Collection<Feature> networkSections = GeoData.getFeatures(basePath + "input_data/test_NMCA_FR_road_tn/roads.gpkg", null, fil);
		SpeedCalculator sc = new SpeedCalculator() {
			@Override
			public double getSpeedKMPerHour(SimpleFeature sf) {
				//estimate speed of a transport section of ERM/EGM based on attributes
				//COR - Category of Road - 0 Unknown - 1 Motorway - 2 Road inside built-up area - 999 Other road (outside built-up area)
				//RTT - Route Intended Use - 0 Unknown - 16 National motorway - 14 Primary route - 15 Secondary route - 984 Local route
				//String cor = sf.getAttribute("COR").toString();
				//if(cor==null) { logger.warn("No COR attribute for feature "+sf.getID()); return 0; };
				//String rtt = sf.getAttribute("RTT").toString();
				//if(rtt==null) { logger.warn("No RTT attribute for feature "+sf.getID()); return 0; };

				//motorways
				//if("1".equals(cor) || "16".equals(rtt)) return 110.0;
				//city roads
				//if("2".equals(cor)) return 50.0;
				//fast roads
				//if("14".equals(rtt) || "15".equals(rtt)) return 80.0;
				//local road
				//if("984".equals(rtt)) return 80.0;
				return 50.0;
			}
		};
		logger.info(networkSections.size() + " sections loaded.");

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
			ArrayList<Feature> pois = GeoData.getFeatures("ERM/gpkg/ERM_2019.1_LAEA/GovservP.gpkg", null, CQL.toFilter("("+c.filter +")"+ (cnt==null?"":" AND (ICC = '"+cnt+"')") ));
			logger.info(pois.size() + " POIs");


			logger.info("Build accessibility...");
			AccessibilityGrid ag = new AccessibilityGrid(cells, cellIdAtt, resKM*1000, pois, networkSections, "TOT_P_2011", c.minDurAccMinT);
			ag.setEdgeWeighter(sc);

			logger.info("Compute accessibility...");
			ag.compute();

			logger.info("Save data...");
			CSVUtil.save(ag.getCellData(), outPath + "accessibility_"+(cnt==null?"":cnt+"_")+resKM+"km"+"_"+c.label+".csv");
			logger.info("Save routes... Nb=" + ag.getRoutes().size());
			GeoData.save(ag.getRoutes(), outPath + "routes_"+(cnt==null?"":cnt+"_")+resKM+"km"+"_"+c.label+".gpkg", crs, true);

		}

		logger.info("End");
	}

}
