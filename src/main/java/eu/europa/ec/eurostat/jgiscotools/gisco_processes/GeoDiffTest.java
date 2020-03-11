package eu.europa.ec.eurostat.jgiscotools.gisco_processes;

import java.util.ArrayList;
import java.util.Collection;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.feature.FeatureUtil;
import eu.europa.ec.eurostat.jgiscotools.geodiff.DifferenceDetection;
import eu.europa.ec.eurostat.jgiscotools.io.GeoPackageUtil;

public class GeoDiffTest {

	public static void main(String[] args) {
		System.out.println("Start");

		ArrayList<Feature> ini = GeoPackageUtil.getFeatures("E:\\temp\\Jorge\\geodiff-1.0\\test/INI.gpkg");
		System.out.println(ini.size());
		System.out.println(ini.get(0).getAttributes().keySet());
		ArrayList<Feature> fin = GeoPackageUtil.getFeatures("E:\\temp\\Jorge\\geodiff-1.0\\test/FIN.gpkg");
		System.out.println(fin.size());
		System.out.println(fin.get(0).getAttributes().keySet());

		FeatureUtil.setId(ini, "ID");
		FeatureUtil.setId(fin, "ID");

		DifferenceDetection dd = new DifferenceDetection(ini, fin);

		Collection<Feature> gd = dd.getDifferences();

		System.out.println("End");
	}

}
