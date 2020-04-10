/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import org.geotools.referencing.GeodeticCalculator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.geocoding.BingGeocoder;
import eu.europa.ec.eurostat.jgiscotools.geocoding.GISCOGeocoder;
import eu.europa.ec.eurostat.jgiscotools.geocoding.base.GeocodingAddress;
import eu.europa.ec.eurostat.jgiscotools.geocoding.base.GeocodingResult;
import eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.ServicesGeocoding;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;
import eu.europa.ec.eurostat.jgiscotools.io.GeoData;
import eu.europa.ec.eurostat.jgiscotools.util.GeoDistanceUtil;
import eu.europa.ec.eurostat.jgiscotools.util.ProjectionUtil;

public class GeocodingTest {

	public static void main(String[] args) {
		System.out.println("Start");

		//load hospital addresses
		ArrayList<Map<String, String>> data = CSVUtil.load("C:\\Users\\gaffuju\\workspace\\healthcare-services\\data\\csv/all.csv");
		System.out.println(data.size());

		Collections.shuffle(data);
		int i=0; int nb = 5;

		LocalParameters.loadProxySettings();

		ArrayList<Feature> out = new ArrayList<>();
		GeometryFactory gf = new GeometryFactory();
		for(Map<String, String> d : data) {
			if(i++ >= nb) break;

			//get address
			GeocodingAddress add = ServicesGeocoding.toGeocodingAddress(d, true);

			//compute position with bing geocoder
			GeocodingResult grB = BingGeocoder.get().geocode(add, true);
			System.out.println(grB.position);

			//compute position with gisco geocoder
			GeocodingResult grG = GISCOGeocoder.get().geocode(add, true);
			System.out.println(grG.position);

			//build output feature
			Feature f = new Feature();
			LineString ls = gf.createLineString(new Coordinate[] { grB.position, grG.position });
			f.setGeometry(ls);
			f.setAttribute("Bing_q", grB.quality);
			f.setAttribute("GISCO_q", grG.quality);
			f.setAttribute("best", grG.quality>grB.quality?"bing":grG.quality<grB.quality?"gisco":"draw");

			double dist = 1000 * GeoDistanceUtil.getDistanceKM(grB.position.x, grB.position.y, grG.position.x, grG.position.y);
			f.setAttribute("dist", dist);
			if(dist>5000) System.err.println((int)dist); else System.out.println((int)dist);

			out.add(f);
		}

		System.out.println("Save - " + out.size());
		GeoData.save(out, "C:\\Users\\gaffuju\\Desktop/geocoderValidation.gpkg", ProjectionUtil.getWGS_84_CRS());

		System.out.println("End");
	}

}
