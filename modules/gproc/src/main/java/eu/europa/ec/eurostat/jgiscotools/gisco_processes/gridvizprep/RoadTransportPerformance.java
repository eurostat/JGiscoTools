package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridvizprep;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.europa.ec.eurostat.jgiscotools.CommandUtil;
import eu.europa.ec.eurostat.jgiscotools.GeoTiffUtil;

public class RoadTransportPerformance {
	static Logger logger = LogManager.getLogger(RoadTransportPerformance.class.getName());

	// the target resolutions
	//private static int[] resolutions = new int[] { 1000, 2000, 5000, 10000, 20000, 50000, 100000 };
	private static int[] resolutions = new int[] { 100000, 50000/*, 20000, 10000, 5000, 2000, 1000*/ };
	private static String basePath = "/home/juju/Bureau/gisco/grid_accessibility/regio_road_perf/";

	// -Xms4g -Xmx16g
	public static void main(String[] args) throws Throwable {
		logger.info("Start");

		//resampling();
		tiling();

		logger.info("End");
	}




	private static void resampling() {

		//population within a 90-minute drive:
		//Population in a neighbourhood of 120 km radius
		//Transport performance by car:
		for(String in : new String[] {"ROAD_ACC_1H30", "POPL_PROX_120KM", "ROAD_PERF_1H30"}) {

			String inF = basePath + "road_transport_performance_grid_datasets/"+in+".tif";

			for (int res : resolutions) {
				logger.info("Tiling " + res + "m");

				String outF = basePath +in+"_"+ res + ".tif";
				//https://gdal.org/programs/gdalwarp.html#gdalwarp
				String cmd = "gdalwarp "+ inF +" "+outF+" -tr "+res+" "+res+" -r average";

				logger.info(cmd);
				CommandUtil.run(cmd);
			}
		}

	}



	// tile all resolutions
	private static void tiling() {

		for (int res : resolutions) {
			logger.info("Tiling " + res + "m");

			String in;

			in = "ROAD_ACC_1H30";
			logger.info("Load grid cells " + in);
			ArrayList<Map<String, String>> cellsRA = GeoTiffUtil.loadCells(
					basePath +in+"_"+ res + ".tif",
					new String[] {"ra"},
					(v)->{ return v[0]==-1; }
					);
			logger.info(cellsRA.size());

			in = "POPL_PROX_120KM";
			logger.info("Load grid cells " + in);
			ArrayList<Map<String, String>> cellsPP = GeoTiffUtil.loadCells(
					basePath +in+"_"+ res + ".tif",
					new String[] {"pp"},
					(v)->{ return v[0]==-1; }
					);
			logger.info(cellsPP.size());

			in = "ROAD_PERF_1H30";
			logger.info("Load grid cells " + in);
			ArrayList<Map<String, String>> cellsRP = GeoTiffUtil.loadCells(
					basePath +in+"_"+ res + ".tif",
					new String[] {"rp"},
					(v)->{ return v[0]==-1; }
					);
			logger.info(cellsRP.size());


			logger.info("Join 1");
			ArrayList<Map<String, String>> cells = joinBothSides("GRD_ID", cellsRA, cellsPP, "");

			/*
				logger.info("Build tiles");
				GridTiler gst = new GridTiler(cells, "GRD_ID", new Coordinate(0, 0), 128);

				gst.createTiles();
				logger.info(gst.getTiles().size() + " tiles created");

				logger.info("Save");
				String outpath = basePath + "out/" + res + "m";
				gst.saveCSV(outpath);
				gst.saveTilingInfoJSON(outpath, "Road transport performance " + res + "m");
			 */
			//}
		}

	}




	private static ArrayList<Map<String, String>> joinBothSides(String idProp, ArrayList<Map<String, String>> data1, ArrayList<Map<String, String>> data2, String defaultValue) {
		//special cases
		if(data1.size() ==0) return data2;
		if(data2.size() ==0) return data1;

		//get all ids
		HashSet<String> ids = new HashSet<>();
		for(Map<String, String> c : data1) ids.add(c.get(idProp));
		for(Map<String, String> c : data2) ids.add(c.get(idProp));

		//index data1 and data2 by id
		HashMap<String,Map<String,String>> ind1 = new HashMap<>();
		for(Map<String, String> e : data1) ind1.put(e.get(idProp), e);
		HashMap<String,Map<String,String>> ind2 = new HashMap<>();
		for(Map<String, String> e : data2) ind2.put(e.get(idProp), e);

		//get key sets
		Set<String> ks1 = data1.get(0).keySet();
		Set<String> ks2 = data2.get(0).keySet();

		//build output
		ArrayList<Map<String, String>> out = new ArrayList<>();
		for(String id : ids) {
			Map<String, String> e1 = ind1.get(id);
			Map<String, String> e2 = ind2.get(id);

			//make template
			Map<String, String> e = new HashMap<>();
			for(String k : ks1) if(k!=idProp) e.put(k, defaultValue);
			for(String k : ks2) if(k!=idProp) e.put(k, defaultValue);
			e.put(idProp, id);

			System.out.println(e);

			
			out.add(e);
		}

		return out;
	}


}
