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
import eu.europa.ec.eurostat.java4eurostat.io.CSV;
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
		String outPath = basePath + "routing_paths/";

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
				return (int)(1e6*(d1-d2));
			}
		};

		//output structure
		StatsHypercube hc = new StatsHypercube(cellIdAtt, "accInd");

		//while there are paths
		while(paths.size() >0) {
			//get cell id of the first path
			String cellId = paths.get(0).getAttribute(cellIdAtt).toString();
			if(logger.isDebugEnabled()) logger.debug(cellId);

			//get all paths of the cell
			ArrayList<Feature> paths_ = new ArrayList<Feature>();
			for(Feature path : paths)
				if(path.getAttribute(cellIdAtt).toString().equals(cellId))
					paths_.add(path);

			//remove
			paths.removeAll(paths_);

			//sort paths
			paths_.sort(c);

			//compute stats on grid cell id
			double val;

			//Compute indicator 1 - Shortest transport time to the nearest service
			//accInd = nearest
			val = Double.parseDouble(paths_.get(0).getAttribute("durationMin").toString());
			hc.stats.add(new Stat(val, "accInd", "nearest"));

			//Compute indicator 2- Average transport time to the X nearest services
			//accInd = ave3near
			int x = Math.min(3, paths_.size());
			val = 0;
			for(int i=0; i<x; i++)
				val += Double.parseDouble(paths_.get(i).getAttribute("durationMin").toString());
			val = val/x;
			hc.stats.add(new Stat(val, "accInd", "ave3near"));

			//Compute indicator 3 - Service capacity within X, Y, Z minutes
			//20', 45' and 60' for healthcare services.
			//10', 20', and 40' for primary and secondary education.
			for(int dur : new int[] {10,20,40}) {
				//accInd = cap+dur
				val = 0;
				//TODO compute value
				hc.stats.add(new Stat(val, "accInd", "cap"+dur));
			}
		}

		//save stats
		//TODO
		//CSV.saveMultiValues(hc, outPath+".csv", "accInd", "nearest", "ave3near", "cap10", "cap20", "cap40");

		logger.info("End");
	}

}
