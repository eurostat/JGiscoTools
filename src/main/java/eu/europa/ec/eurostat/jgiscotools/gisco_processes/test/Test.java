package eu.europa.ec.eurostat.jgiscotools.gisco_processes.test;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.filter.text.cql2.CQL;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.GeoData;
import eu.europa.ec.eurostat.jgiscotools.io.SHPUtil;

public class Test {
	private static Logger logger = LogManager.getLogger(Test.class.getName());


	public static void main(String[] args) throws Exception {
		logger.info("Start");

		logger.info("Load data");
		String file = "E:/dissemination/shared-data/ERM/ERM_2019.1_shp_LAEA/Data/GovservP.shp";
		ArrayList<Feature> fs = SHPUtil.getFeatures(file, CQL.toFilter("F_CODE = 'AX502'") );
		CoordinateReferenceSystem crs = SHPUtil.getCRS(file);

		logger.debug(fs.size() + " loaded");

		logger.info("Select the nearest ones");
		Coordinate lux = new Coordinate(4041252, 2951147);
		ArrayList<Feature> out = new ArrayList<Feature>();
		for(Feature f : fs) {
			double dist = lux.distance( f.getGeometry().getCoordinate() );
			if(dist>100000) continue;
			out.add(f);
		}
		logger.debug(out.size() + " kept");

		logger.info("Save as gpkg file");
		GeoData.save(out, "C:\\Users\\clemoki\\Desktop\\hospi_lux.gpkg", crs, true);

		logger.info("End");
	}

}
