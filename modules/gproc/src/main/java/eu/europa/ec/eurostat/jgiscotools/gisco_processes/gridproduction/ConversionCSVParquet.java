package eu.europa.ec.eurostat.jgiscotools.gisco_processes.gridproduction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import eu.europa.ec.eurostat.jgiscotools.ParquetUtil;
import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;

public class ConversionCSVParquet {
	static Logger logger = LogManager.getLogger(ConversionCSVParquet.class.getName());

	public static String basePath = "/home/juju/Bureau/gisco/grid_dissemination/";

	//the different resolutions, in KM
	public static int[] resKMs = new int[] {100,50,20,10,5,2,1};


	//use: -Xms8g -Xmx24g
	public static void main(String[] args) throws Exception {
		logger.info("Start");

		//get crs
		CoordinateReferenceSystem crs = GeoData.getCRS(basePath+"in/grid_100km_point.gpkg");

		for(int resKM : resKMs) {
			logger.info("res " + resKM + "km");

			logger.info("load intial GPKG");
			Collection<Feature> fs = GeoData.getFeatures(basePath+"in/grid_"+resKM+"km_point.gpkg");
			logger.info(fs.size());
			//System.out.println(fs.iterator().next().getAttributes().keySet());
			//[DIST_BORD, TOT_P_2018, TOT_P_2006, GRD_ID, TOT_P_2011, Y_LLC, CNTR_ID, NUTS2016_3, NUTS2016_2, NUTS2016_1, NUTS2016_0, LAND_PC, X_LLC, NUTS2021_3, NUTS2021_2, DIST_COAST, NUTS2021_1, NUTS2021_0]

			logger.info("prepare CSV");
			Collection<Map<String, String>> data = new ArrayList<>();
			for(Feature f : fs) {
				HashMap<String,String> d = new HashMap<>();
				//copy attributes
				for(Entry<String, Object> att : f.getAttributes().entrySet())
					d.put(att.getKey(), att.getValue().toString());
				//release a bit memory progressively
				f.getAttributes().clear();
				f.setGeometry(null);
			}
			fs.clear(); fs = null;
			logger.info(data.size());

			logger.info("save CSV");
			CSVUtil.save(data, basePath+"in/grid_"+resKM+"km.csv");
			data.clear(); data = null;

			logger.info("convert to parquet");
			ParquetUtil.convertCSVToParquet(basePath+"in/grid_"+resKM+"km.csv", basePath+"in/", "grid_"+resKM+"km", "GZIP");
		}

		logger.info("End");
	}

}
