package eu.europa.ec.eurostat.jgiscotools.gisco_processes;

import java.util.ArrayList;
import java.util.Collection;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.geodiff.GeoDiff;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;

public class GeoDiffTest {

	public static void main(String[] args) {
		System.out.println("Start");

		ArrayList<Feature> ini = GeoData.getFeatures("E:\\temp\\Jorge\\geodiff-1.0\\test/INI.gpkg", "ID");
		System.out.println(ini.size());
		System.out.println(ini.get(0).getAttributes().keySet());
		ArrayList<Feature> fin = GeoData.getFeatures("E:\\temp\\Jorge\\geodiff-1.0\\test/FIN.gpkg", "ID");
		System.out.println(fin.size());
		System.out.println(fin.get(0).getAttributes().keySet());

		GeoDiff dd = new GeoDiff(ini, fin);

		Collection<Feature> gd = dd.getDifferences();

		System.out.println("End");
	}

}
