/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.grid.geomprod;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;

import eu.europa.ec.eurostat.jgiscotools.algo.Partition;
import eu.europa.ec.eurostat.jgiscotools.algo.Partition.GeomType;
import eu.europa.ec.eurostat.jgiscotools.algo.Partition.PartitionedOperation;
import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.FeatureUtil;
import eu.europa.ec.eurostat.jgiscotools.io.GeoPackageUtil;

/**
 * @author julien Gaffuri
 *
 */
public class Decompose {
	static Logger logger = Logger.getLogger(Decompose.class.getName());

	public static void main(String[] args) throws Throwable {
		logger.info("Start");

		String path = "E:/workspace/gridstat/data/CNTR_100k/";

		logger.info("Load data...");
		Collection<Feature> fs = GeoPackageUtil.getFeatures(path+"CNTR_RG_100K_union_LAEA.gpkg", CQL.toFilter("CNTR_ID='BE'"));
		System.out.println(fs.size());


		logger.info("Run decomposition...");
		final Collection<Geometry> landGeometries = new ArrayList<>();
		PartitionedOperation op = new PartitionedOperation() {
			@Override
			public void run(Partition p) {
				System.out.println(p.getCode());
				landGeometries.addAll(FeatureUtil.getGeometriesSimple(p.getFeatures()));
			}
		};
		Partition.runRecursively(fs, op, 100000, 10000, true, GeomType.ONLY_AREAS, 0);
		System.out.println(landGeometries.size());

		logger.info("Save...");
		GeoPackageUtil.saveGeoms(landGeometries, path + "land_areas.gpkg", CRS.decode("EPSG:3035"));

		logger.info("End");

	}

}
