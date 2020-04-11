/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.geocoding.BingGeocoder;
import eu.europa.ec.eurostat.jgiscotools.geocoding.GISCOGeocoderAPI;
import eu.europa.ec.eurostat.jgiscotools.geocoding.GISCOGeocoderNominatimDetail;
import eu.europa.ec.eurostat.jgiscotools.geocoding.base.Geocoder;
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

		String inPath = "C:\\Users\\gaffuju\\workspace\\healthcare-services\\data\\";
		String outPath = "C:\\Users\\gaffuju\\Desktop/gv/";

		//load hospital addresses - the ones with geolocation and address DE-RO
		for(String cc : new String[] {"NL"/*, "DE", "RO"*/}) {
			System.out.println("*** " + cc);
			ArrayList<Map<String, String>> data = CSVUtil.load(inPath + "csv/"+cc+".csv");
			System.out.println(data.size());

			//LocalParameters.loadProxySettings();
			//Collection<Feature> outB = validate(data, BingGeocoder.get(), "lon", "lat");
			//System.out.println("Save - " + outB.size());
			//GeoData.save(outB, outPath + "geocoderValidationBing_"+cc+".gpkg", ProjectionUtil.getWGS_84_CRS());

			Collection<Feature> outGNA = validate(data, GISCOGeocoderNominatimDetail.get(), "lon", "lat");
			System.out.println("Save - " + outGNA.size());
			GeoData.save(outGNA, outPath + "geocoderValidationGISCO_NominatimAdd_"+cc+".gpkg", ProjectionUtil.getWGS_84_CRS());

			//Collection<Feature> outGAPI = validate(data, GISCOGeocoderAPI.get(), "lon", "lat");
			//System.out.println("Save - " + outGAPI.size());
			//GeoData.save(outGAPI, outPath + "geocoderValidationGISCO_API_"+cc+".gpkg", ProjectionUtil.getWGS_84_CRS());

			//TODO add 2 other GISCO geocoders
		}

		System.out.println("End");
	}


	public static Collection<Feature> validate(Collection<Map<String, String>> data, Geocoder gc, String lonCol, String latCol) {
		ArrayList<Feature> out = new ArrayList<>();
		GeometryFactory gf = new GeometryFactory();
		for(Map<String, String> d : data) {

			//get position
			Coordinate c = new Coordinate(Double.parseDouble(d.get(lonCol)), Double.parseDouble(d.get(latCol)));

			//get address
			GeocodingAddress add = ServicesGeocoding.toGeocodingAddress(d, true);

			//get geocoder position 
			GeocodingResult gr = gc.geocode(add, true);
			System.out.println(gr.position);

			//build output feature
			Feature f = new Feature();
			LineString ls = gf.createLineString(new Coordinate[] { c, gr.position });
			f.setGeometry(ls);
			double dist = 1000 * GeoDistanceUtil.getDistanceKM(gr.position.x, gr.position.y, c.x, c.y);
			f.setAttribute("dist", dist);
			f.setAttribute("qual", gr.quality);

			out.add(f);
		}
		return out;
	}

}
