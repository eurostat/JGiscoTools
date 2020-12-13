/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridaccessibility;

import java.util.ArrayList;
import java.util.Comparator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.europa.ec.eurostat.java4eurostat.base.Stat;
import eu.europa.ec.eurostat.java4eurostat.base.StatsHypercube;
import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;

/**
 * @author clemoki
 *
 */
public class RoutingPathsStats {
	private static Logger logger = LogManager.getLogger(RoutingPathsStats.class.getName());

	/** @param args **/
	public static void main(String[] args) {
		logger.info("Start");

		String basePath = "E:/workspace/basic_services_accessibility/";
		String outPath = basePath + "accessibility_output/";

		String cellIdAtt = "GRD_ID";

		logger.info("Load routing paths...");
		ArrayList<Feature> paths = GeoData.getFeatures(outPath+"routes_FR_1km_schools.gpkg");
		logger.info(paths.size() + " paths");

		//comparator
		Comparator<Feature> c = new Comparator<Feature>() {
			@Override
			public int compare(Feature f1, Feature f2) {
				double d1 = Double.parseDouble(f1.getAttribute("durationMin").toString());
				double d2 = Double.parseDouble(f2.getAttribute("durationMin").toString());
				return (int)(1e6*(d2-d1));
			}
		};

		//
		StatsHypercube sh = new StatsHypercube(cellIdAtt, "accInd");

		//while there are paths
		while(paths.size() >0) {
			//get cell id of the first path
			String cellId = paths.get(0).getAttribute(cellIdAtt).toString();
			logger.info(cellId);

			//get all paths of the cell
			ArrayList<Feature> paths_ = new ArrayList<Feature>();
			for(Feature path : paths)
				if(path.getAttribute(cellIdAtt).toString().equals(cellId))
					paths_.add(path);

			//remove
			paths.removeAll(paths_);

			//sort paths
			paths.sort(c);

			//compute stats on grid cell id
			double val;

			//Compute indicator 1 - Shortest transport time to the nearest service
			//accInd = nearest
			val = 0;
			//TODO compute value
			sh.stats.add(new Stat(0, "accInd", "nearest"));

			//Compute indicator 2- Average transport time to the X nearest services
			//accInd = ave3near
			val = 0;
			//TODO compute value
			sh.stats.add(new Stat(0, "accInd", "ave3near"));

			//Compute indicator 3 - Service capacity within X, Y, Z minutes
			//20', 45' and 60' for healthcare services.
			//10', 20', and 40' for primary and secondary education.
			//accInd = cap10
			val = 0;
			//TODO compute value
			sh.stats.add(new Stat(0, "accInd", "cap10"));
			//accInd = cap20
			val = 0;
			//TODO compute value
			sh.stats.add(new Stat(0, "accInd", "nearest"));
			//accInd = cap40
			val = 0;
			//TODO compute value
			sh.stats.add(new Stat(val, "accInd", "cap40"));
		}

		//save stats
		//TODO

		logger.info("End");
	}

}
