/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.locationtech.jts.geom.Coordinate;

import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;

/**
 * Some function to geocode data based on addresses
 * using GISCO geocoder.
 * 
 * @author clemoki
 *
 */
public class GISCOGeocoding {

	/**
	 * Make geocoding from query text.
	 * 
	 * @param query The query text (should not be already formatted as URL)
	 * @return The position, as proposed by the geocoder.
	 */
	public static Coordinate geocode(String query) {
		try {
			query = URLEncoder.encode(query, "UTF-8");
			String url = "q="+query;
			return geocodeURL(url);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Make geocoding from query text.
	 * See: https://nominatim.org/release-docs/develop/api/Search/
	 * 
	 * @return The position, as proposed by the geocoder.
	 */
	public static Coordinate geocode(Address ad) {
		try {
			String query = "";

			if(ad.city != null)
				query += "&city="+ad.city;
			if(ad.county != null)
				query += "&county="+ad.county;
			if(ad.state != null)
				query += "&state="+ad.state;
			if(ad.country != null)
				query += "&country="+ad.country;
			if(ad.postalcode != null)
				query += "&postalcode="+ad.postalcode;

			if(ad.street != null)
				query += "&street="+ad.street;
			else {
				String street = "";
				if(ad.housenumber != null)
					street += ad.housenumber + (ad.streetname != null? " ":"");
				if(ad.streetname != null)
					street += ad.streetname;
				if(!street.equals(""))
					query += "&street="+street;
			}

			System.out.println(query);
			query = URLEncoder.encode(query, "UTF-8");
			System.out.println(query);

			return geocodeURL(query);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	public static class Address {
		public String street, housenumber, streetname, city, county, state, country, postalcode;

		public Address(String street, String housenumber, String streetname, String city, String county, String state, String country, String postalcode) {
			this.street = street;
			this.housenumber = housenumber;
			this.streetname = streetname;
			this.city = city;
			this.county = county;
			this.state = state;
			this.country = country;
			this.postalcode = postalcode;
		}
	}

//	public static class NamePlace {
//		public String country, city, name;
//
//		public NamePlace(String country, String city, String name) {
//			this.name = name;
//			this.city = city;
//			this.country = country;
//		}
//	}

	/**
	 * Geocode from URL.
	 * 
	 * @param url
	 * @return
	 */
	private static Coordinate geocodeURL(String URLquery) {
		try {
			String url = "https://europa.eu/webtools/rest/gisco/nominatim/search?" + URLquery + "&polygon=0&viewbox=&format=json&limit=2";
			System.out.println(url);
			BufferedReader in = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
			String line = in.readLine();
			String[] parts = line.split(",");
			Coordinate c = new Coordinate();
			for(String part : parts) {
				if(part.contains("\"lat\":")) {
					part = part.replace("\"lat\":\"", "");
					part = part.replace("\"", "");
					c.y = Double.parseDouble(part);
				}
				if(part.contains("\"lon\":")) {
					part = part.replace("\"lon\":\"", "");
					part = part.replace("\"", "");
					c.x = Double.parseDouble(part);
				}
			}
			in.close();
			return c;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

//	public static void geocodeMT() {
//	geocode(new NamePlace(""))
//	}


	
	public static void main(String[] args) {

		
		//Works:
		//https://europa.eu/webtools/rest/gisco/nominatim/search?q=Rue%20Alphonse%20Weicker+5%+2721+Luxembourg&polygon=0&viewbox=&format=json&limit=1
		
		//https://nominatim.openstreetmap.org/search?q=Malta%20Victoria&polygon=0&format=json&limit=1
		//https://nominatim.openstreetmap.org/search?q=University%20of%20Amsterdam&polygon=0&format=json&limit=1
		
		//Doesn't work: 
		//https://europa.eu/webtools/rest/gisco/nominatim/search?q=Malta%20Victoria&polygon=0&format=json&limit=1
		//Address ad = new Address(null, "5", "Rue Alphonse Weicker", "Luxembourg", null, null, "Luxembourg", "2721");
		
		

		//1. Doesn't work
		//case sensitive
		System.out.println("1. Deconstructed Address");
		System.out.println( geocode( new Address(null, null, "Rue Alphonse Weicker", "Luxembourg", "", "", "Luxembourg", null)) );
		
		System.out.println("2. Full Address");
		//2. works only if in the right order
		//works with & without special characters and country, not without postcode 
		System.out.println( geocode("30 rue alphonse münchen luxembourg 2171") );
		//Doesn't work
		System.out.println( geocode("San Segundo,  7") );
		
		System.out.println("3. Place Names");
		System.out.println( geocode("Berlin") );
		System.out.println( geocode("Victoria, Malta") );
		System.out.println( geocode("Malta") );
		
		//Higher education institutions seem to be included but not others
		//System.out.println("4. Institution Names");
		//System.out.println(geocode("University of Amsterdam"));
		//System.out.println(geocode("Amsterdam University, Amsterdam, Netherlands"));
		//System.out.println(geocode("Archbishop Seminary, Rabat, Malta"));

	}


}

