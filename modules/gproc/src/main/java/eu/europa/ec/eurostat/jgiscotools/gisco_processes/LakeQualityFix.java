package eu.europa.ec.eurostat.jgiscotools.gisco_processes;

import java.util.Collection;

import org.geotools.referencing.CRS;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;
import eu.europa.ec.eurostat.jgiscotools.tesselationGeneralisation.TesselationQuality;

public class LakeQualityFix {

	public static void main(String[] args) throws Exception {
		System.out.println("start");
		
		//load input data
		Collection<Feature> lakes = GeoData.getFeatures("Y:\\JIRA-2019\\GISCO_2207_Lakes_100K\\LAKE_EURO_PL_100K_2019.shp");
		System.out.println(lakes.size());
		
		lakes = TesselationQuality.fixQuality(lakes, null, 10, false, 100000, 10000, true);
		System.out.println(lakes.size());
		
		//save
		GeoData.save(lakes, "Y:\\JIRA-2019\\GISCO_2207_Lakes_100K\\LAKE_EURO_PL_100K_2019_QC.shp", CRS.decode("EPSG:3035"));

		System.out.println("end");
	}
	
}
