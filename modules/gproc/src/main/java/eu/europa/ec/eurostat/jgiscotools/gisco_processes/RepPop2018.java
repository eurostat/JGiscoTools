package eu.europa.ec.eurostat.jgiscotools.gisco_processes;

import java.util.ArrayList;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.geo.CRSUtil;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;

public class RepPop2018 {

	//-Xms8g -Xmx24g
	public static void main(String[] args) {
		System.out.println("Start");

		String folder = "C:\\Users\\gaffuju\\Desktop\\";

		for(String g : new String[] {"surf", "point"}) {
			System.out.println(g);

			System.out.println("Load...");
			ArrayList<Feature> fs = GeoData.getFeatures(folder + "grid_1km_"+g+"_2018.gpkg");
			System.out.println(fs.size());

			System.out.println("Set...");
			for(Feature f : fs) {
				Object pop2018 = f.getAttribute("TOT_P_2018");
				if(pop2018 == null || "".equals(pop2018.toString()))
					f.setAttribute("TOT_P_2018", 0);
				else
					f.setAttribute("TOT_P_2018", Integer.parseInt(pop2018.toString()));
			}
			System.out.println(fs.size());

			System.out.println("Save...");
			GeoData.save(fs, folder + "grid_1km_"+g+"_2018_.gpkg", CRSUtil.getETRS89_LAEA_CRS(), true);
		}

		System.out.println("End");
	}

}
