package eu.europa.ec.eurostat.jgiscotools;

import java.util.ArrayList;
import java.util.Collection;

import org.geotools.referencing.CRS;

import eu.europa.ec.eurostat.jgiscotools.algo.base.Partition;
import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;

public class Test {

	public static void main(String[] args) throws Throwable {
		System.out.println("Start");

		/*/decomposer
		ArrayList<Feature> fs = GeoData.getFeatures("/home/juju/geodata/CNTR_2016/CNTR_RG_10M_2016_4258.shp");
		System.out.println(fs.size());
		Collection<Feature> out = Decomposer.decomposeFeature(fs, true, 1000, 1000, Partition.GeomType.ONLY_AREAS, 0);
		System.out.println(fs.size());
		System.out.println(out.size());
		GeoData.save(out, "/home/juju/Bureau/gisco/tmp/decomposer_test.gpkg", CRS.decode("EPSG:4258"));*/

		/*/partitionning
		ArrayList<Feature> fs = GeoData.getFeatures("/home/juju/geodata/CNTR_2016/CNTR_RG_10M_2016_4258.shp");
		System.out.println(fs.size());
		Collection<Feature> out = Partition.getPartitionDataset(fs, true, 1000000, 1000, Partition.GeomType.ONLY_AREAS, 0);
		System.out.println(fs.size());
		System.out.println(out.size());
		GeoData.save(out, "/home/juju/Bureau/gisco/tmp/partitionning_test.gpkg", CRS.decode("EPSG:4258"));*/

		/*/partitionning BU
		ArrayList<Feature> fs = GeoData.getFeatures("/home/juju/Bureau/gisco/cnt/fr/bdtopo/067/BATIMENT.gpkg");
		System.out.println(fs.size());
		Collection<Feature> out = Partition.getPartitionDataset(fs, true, 1000, 100000, Partition.GeomType.ONLY_AREAS, 0);
		System.out.println(fs.size());
		System.out.println(out.size());
		GeoData.save(out, "/home/juju/Bureau/gisco/tmp/partitionning_buildings_test.gpkg", CRS.decode("EPSG:3035"));*/

		//TODO test BU without splitting

		System.out.println("End");
	}

}
