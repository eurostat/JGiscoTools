package eu.europa.ec.eurostat.jgiscotools.gisco_processes.test;

import java.util.Collection;

import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.geodiff.GeoDiff;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;

public class TestsGeoDiff {


	public static void main(String[] args) throws Exception {
		System.out.println("Start");

		System.out.println("load");
		Collection<Feature> fs1 = GeoData.getFeatures("E:\\dissemination\\shared-data\\EBM\\gpkg\\EBM_2020_LAEA\\LAU.gpkg");
		System.out.println(fs1.size());
		Collection<Feature> fs2 = GeoData.getFeatures("E:\\dissemination\\shared-data\\EBM\\gpkg\\EBM_2021_LAEA\\LAU.gpkg");
		System.out.println(fs2.size());

		System.out.println("compute diff");
		GeoDiff gd = new GeoDiff(fs1, fs2, 0.1);
		Collection<Feature> diff = gd.getDifferences();
		System.out.println(diff.size());

		for(Feature f : diff) {
			System.out.println(f.getGeometry().getGeometryType());
		}

		GeoData.save(diff, "C:\\Users\\gaffuju\\Desktop\\LAU\\aaaaaaa.gpkg", CRS.decode("EPSG:3035"));

		System.out.println("End");
	}

}
