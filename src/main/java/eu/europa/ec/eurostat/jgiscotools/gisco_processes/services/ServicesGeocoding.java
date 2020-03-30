/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.services;

import java.util.Collection;
import java.util.Map;

import eu.europa.ec.eurostat.jgiscotools.geocoding.base.Geocoder;
import eu.europa.ec.eurostat.jgiscotools.geocoding.base.GeocodingAddress;
import eu.europa.ec.eurostat.jgiscotools.geocoding.base.GeocodingResult;

/**
 * @author julien Gaffuri
 *
 */
public class ServicesGeocoding {

	private static GeocodingAddress toGeocodingAddress(Map<String,String> s, boolean usePostcode) {
		return new GeocodingAddress(
				null,
				s.get("house_number"),
				s.get("street"),
				s.get("city"),
				s.get("cc"),
				usePostcode? s.get("postcode") : null
				);
	}

	public static GeocodingResult get(Geocoder gc, Map<String,String> s, boolean usePostcode, boolean print) {
		return gc.geocode(toGeocodingAddress(s, usePostcode), print);
	}

	public static void set(Map<String,String> s, GeocodingResult gr) {
		s.put("lat", "" + gr.position.y);
		s.put("lon", "" + gr.position.x);
		//s.put("geo_matching", "" + gr.matching);
		//s.put("geo_confidence", "" + gr.confidence);
		s.put("geo_qual", "" + gr.quality);		
	}

	public static void set(Geocoder gc, Map<String,String> s, boolean usePostcode, boolean print) {
		GeocodingResult gr = get(gc, s, usePostcode, print);
		if(print) System.out.println(gr.position  + "  --- " + gr.quality + " --- " + gr.matching + " --- " + gr.confidence);
		set(s, gr);
	}

	public static void set(Geocoder gc, Collection<Map<String,String>> services, boolean usePostcode, boolean print) {
		int fails = 0;
		for(Map<String,String> s : services) {
			GeocodingResult gr = get(gc, s, usePostcode, print);
			if(print) System.out.println(gr.position  + "  --- " + gr.quality + " --- " + gr.matching + " --- " + gr.confidence);
			if(gr.position.getX()==0 && gr.position.getY()==0) fails++;
			set(s, gr);
		}
		System.out.println("Failures: " + fails + "/" + services.size());
	}




	public static void improve(Geocoder gc, Map<String, String> s, boolean usePostcode, boolean print) {

		//check if position is not already perfect
		int geoqIni = Integer.parseInt(s.get("geo_qual"));
		if(geoqIni == 1 || geoqIni == -1) {
			//if(print) System.out.println("Position already OK for " + s.get("id"));
			return;
		}

		//find new candidate position
		GeocodingResult gr = get(gc, s, usePostcode, print);
		if(gr.quality >= geoqIni) {
			if(print) System.out.println("No positionning improvement for " + s.get("id"));
			return;
		}

		if(print) System.out.println("Positionning improvement for " + s.get("id") + ". "+ geoqIni + " -> " + gr.quality);


	}

}
