package eu.europa.ec.eurostat.jgiscotools.gisco_processes;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.geotools.filter.text.cql2.CQL;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.GeoPackageUtil;
import eu.europa.ec.eurostat.jgiscotools.io.SHPUtil;
import eu.europa.ec.eurostat.jgiscotools.io.SHPUtil.SHPData;

public class Test {
	private static Logger logger = Logger.getLogger(Test.class.getName());


	public static void main(String[] args) throws Exception {
		logger.info("Start");

		logger.info("Load data");
		SHPData shp = SHPUtil.loadSHP("E:/dissemination/shared-data/ERM/ERM_2019.1_shp_LAEA/Data/GovservP.shp", CQL.toFilter("F_CODE = 'AX502'") );
		ArrayList<Feature> fs = shp.fs;
		CoordinateReferenceSystem crs = shp.ft.getCoordinateReferenceSystem();

		logger.debug(fs.size() + " loaded");

		logger.info("Select the nearest ones");
		Coordinate lux = new Coordinate(4041252, 2951147);
		ArrayList<Feature> out = new ArrayList<Feature>();
		for(Feature f : fs) {
			double dist = lux.distance( f.getDefaultGeometry().getCoordinate() );
			if(dist>100000) continue;
			out.add(f);
		}
		logger.debug(out.size() + " kept");

		logger.info("Save as gpkg file");
		GeoPackageUtil.save(out, "C:\\Users\\clemoki\\Desktop\\hospi_lux.gpkg", crs);

		logger.info("End");
	}

}
