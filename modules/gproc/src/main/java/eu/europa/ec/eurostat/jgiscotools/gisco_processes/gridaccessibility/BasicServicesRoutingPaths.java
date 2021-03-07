/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridaccessibility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.operation.linemerge.LineMerger;
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
	private static int resKM = 1;
	private static List<String> poiTypes = Arrays.asList(new String[] {"healthcare", "educ_1", "educ_2"});

	//use: -Xms2G -Xmx12G
	/** @param args 
	 * @throws Exception **/
	public static void main(String[] args) throws Exception {
		logger.info("Start");

		//set logger level
		//Configurator.setLevel(AccessibilityRoutingPaths.class.getName(), Level.ALL);

		String outPath = basePath + "routing_paths/";
		CoordinateReferenceSystem crs = CRS.decode("EPSG:3035");
		//set the country id (set to null for all countries)


		logger.info("Load grid cells " + resKM + "km ...");
		ArrayList<Feature> cells = GeoData.getFeatures(basePath + "input_data/grid_1km_surf_FRL0.gpkg",null);
		logger.info(cells.size() + " cells");


		for(String rnw : new String[] { "osm", "nmca", "tomtom" } ) {

			logger.info("Load network sections " + rnw + "...");
			Collection<Feature> networkSections =
					rnw.equals("nmca") ? RoadBDTopo.get("cost")
							: rnw.equals("osm") ? RoadOSM.get("cost")
									: rnw.equals("tomtom") ? RoadTomtom.get("cost")
											: null;
			logger.info(networkSections.size() + " sections loaded.");

			logger.info("Make routable");
			networkSections = decomposeSectionsRoutable(networkSections);
			logger.info(networkSections.size() + " sections.");
			GeoData.save(networkSections, outPath + "test_routable_networks/"+rnw+".gpkg", crs);

			if(true) continue;

			logger.info("Build accessibility...");
			AccessibilityRoutingPaths ag = new AccessibilityRoutingPaths(cells, "GRD_ID", 1000*resKM, "id", networkSections, "cost", 3, 50000);

			logger.info("Load POI and add POIs");
			if(poiTypes.contains("healthcare"))
				ag.addPOIs("healthcare", GeoData.getFeatures(basePath + "input_data/healthcare_services_LAEA.gpkg", null, cnt==null?null:CQL.toFilter("cc = '"+cnt+"'")));
			if(poiTypes.contains("educ_1"))
				ag.addPOIs("educ_1", GeoData.getFeatures(basePath + "input_data/education_services_LAEA.gpkg", null, CQL.toFilter("levels LIKE '%1%'" + (cnt==null?"":" AND cc = '"+cnt+"'"))));
			if(poiTypes.contains("educ_2"))
				ag.addPOIs("educ_2", GeoData.getFeatures(basePath + "input_data/education_services_LAEA.gpkg", null, CQL.toFilter("levels LIKE '%2%'" + (cnt==null?"":" AND cc = '"+cnt+"'"))));


			logger.info("Compute accessibility paths...");
			ag.compute(true);


			//save ouput paths
			if(poiTypes.contains("healthcare")) {
				logger.info("Save routes healthcare... Nb=" + ag.getRoutes("healthcare").size());
				GeoData.save(ag.getRoutes("healthcare"), outPath + "routes_"+(cnt==null?"":cnt+"_")+rnw+"_healthcare.gpkg", crs, true);
			}
			if(poiTypes.contains("educ_1")) {
				logger.info("Save routes educ_1... Nb=" + ag.getRoutes("educ_1").size());
				GeoData.save(ag.getRoutes("educ_1"), outPath + "routes_"+(cnt==null?"":cnt+"_")+rnw+"_educ_1.gpkg", crs, true);
			}
			if(poiTypes.contains("educ_2")) {
				logger.info("Save routes educ_2... Nb=" + ag.getRoutes("educ_2").size());
				GeoData.save(ag.getRoutes("educ_2"), outPath + "routes_"+(cnt==null?"":cnt+"_")+rnw+"_educ_2.gpkg", crs, true);
			}

			if(computeStats) {
				for(String poiType : poiTypes) {
					logger.info("compute stats");
					StatsHypercube hc = AccessibilityRoutingPaths.computeStats(ag.getRoutes(poiType), "GRD_ID");

					logger.info("save stats");
					CSV.saveMultiValues(hc, basePath+"accessibility_output/routing_paths_"+rnw+"_"+poiType+"_stats.csv", "accInd");
				}
			}
		}

		logger.info("End");
	}




	//TODO move to graph builder
	public static Collection<Feature> decomposeSectionsRoutable(Collection<Feature> sections) {

		//build line merger
		LineMerger lm = new LineMerger();
		for(Feature f : sections)
			if(f.getGeometry()!=null && !f.getGeometry().isEmpty())
				lm.add(f.getGeometry());

		//run linemerger
		Collection<?> ls_ = lm.getMergedLineStrings();
		lm = null;

		//index lines
		STRtree index = new STRtree();
		for(Object ls : ls_) index.insert(((Geometry)ls).getEnvelopeInternal(), ls);
		ls_.clear(); ls_ = null;

		//build output features
		Collection<Feature> out = new ArrayList<>();
		for(Feature f : sections) {
			Geometry g = f.getGeometry();
			if(g==null || g.isEmpty()) continue;

			//get lines nearby feature geometry
			List<?> ls = index.query(g.getEnvelopeInternal());
			for(Object line_ : ls) {
				Geometry line = (Geometry)line_;
				if(! g.getEnvelopeInternal().intersects(line.getEnvelopeInternal())) continue;
				if(! g.contains(line)) continue;

				//make new feature
				Feature f2 = new Feature();
				f2.setGeometry(line);
				f2.getAttributes().putAll(f.getAttributes());
				out.add(f2);
			}
		}

		return out;
	}

}
