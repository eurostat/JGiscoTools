/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.geocoding.BingGeocoder;
import eu.europa.ec.eurostat.jgiscotools.geocoding.GISCOGeocoder;
import eu.europa.ec.eurostat.jgiscotools.geocoding.base.GeocodingAddress;
import eu.europa.ec.eurostat.jgiscotools.geocoding.base.GeocodingResult;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;
import eu.europa.ec.eurostat.jgiscotools.io.GeoData;
import eu.europa.ec.eurostat.jgiscotools.util.ProjectionUtil;

public class GeocodingTest {

	public static void main(String[] args) {
		System.out.println("Start");

		//load hospital addresses
		ArrayList<Map<String, String>> data = CSVUtil.load("C:\\Users\\gaffuju\\workspace\\healthcare-services\\data\\csv/all.csv");
		System.out.println(data.size());

		Collections.shuffle(data);
		int i=0;

		ArrayList<Feature> out = new ArrayList<>();
		GeometryFactory gf = new GeometryFactory();
		for(Map<String, String> d : data) {
			if(i++>20) break;

			GeocodingAddress add = new GeocodingAddress();

			//compute position with bing geocoder
			GeocodingResult grB = BingGeocoder.get().geocode(add, true);
			//compute position with gisco geocoder
			GeocodingResult grG = GISCOGeocoder.get().geocode(add, true);

			Feature f = new Feature();
			LineString ls = gf.createLineString(new Coordinate[] { grB.position, grG.position });
			f.setGeometry(ls);
			f.setAttribute("Bing_q", grB.quality);
			f.setAttribute("GISCO_q", grG.quality);
			f.setAttribute("best", grG.quality>grB.quality?"bing":grG.quality<grB.quality?"gisco":"draw");
			out.add(f);
		}

		//save
		System.out.println(out.size());
		GeoData.save(out, "C:\\Users\\gaffuju\\Desktop/geocoderValidation.gpkg", ProjectionUtil.getWGS_84_CRS());

		System.out.println("End");
	}

}
