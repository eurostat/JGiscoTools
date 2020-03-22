/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.geocoding;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;

import org.locationtech.jts.geom.Coordinate;

import eu.europa.ec.eurostat.jgiscotools.gisco_processes.LocalParameters;

/**
 * @author clemoki
 *
 */
public class BingGeocoder {

	private static String key = LocalParameters.get("bing_map_api_key");

	//https://docs.microsoft.com/en-us/bingmaps/rest-services/locations/find-a-location-by-address

	//structured version
	//http://dev.virtualearth.net/REST/v1/Locations?


	//unstructured version
	//http://dev.virtualearth.net/REST/v1/Locations/{locationQuery}?includeNeighborhood={includeNeighborhood}&maxResults={maxResults}&include={includeValue}&key={BingMapsAPIKey}


	public static GeocodingResult geocode(GeocodingAddress ad) {
		try {
			String query = "";

			//&addressLine={addressLine}   //<AddressLine>1 Microsoft Way</AddressLine>  

			if(ad.street != null)
				query += "&addressLine=" + URLEncoder.encode(ad.street, "UTF-8");
			else {
				String street = "";
				if(ad.housenumber != null)
					street += ad.housenumber + (ad.streetname != null? " ":"");
				if(ad.streetname != null)
					street += ad.streetname;
				if(!street.equals(""))
					query += "&addressLine=" + URLEncoder.encode(street, "UTF-8");
			}

			if(ad.city != null)
				query += "&locality=" + URLEncoder.encode(ad.city, "UTF-8");
			if(ad.countryCode != null)
				query += "&countryRegion=" + ad.countryCode;
			if(ad.postalcode != null)
				query += "&postalCode=" + URLEncoder.encode(ad.postalcode, "UTF-8");

			//query = URLEncoder.encode(query, "UTF-8");

			return geocodeURL(query);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Geocode from URL.
	 * 
	 * @param url
	 * @return
	 */
	private static GeocodingResult geocodeURL(String URLquery) {
		try {
			String url = "http://dev.virtualearth.net/REST/v1/Locations?" + URLquery + "&maxResults=1&key=" + key;
			//url = url.replace("+", "%20");

			BufferedReader in = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
			String line = in.readLine();

			String[] parts = line.split("\"type\":\"Point\",\"coordinates\":\\[");
			line = parts[1];
			parts = line.split("\\]},");
			line = parts[0];
			parts = line.split(",");
			double lat = Double.parseDouble(parts[0]);
			double lon = Double.parseDouble(parts[1]);
			Coordinate c = new Coordinate(lon, lat);

			GeocodingResult gr = new GeocodingResult();
			gr.position = c;
			//TODO add quality indicator
			return gr;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}


/*
	public static void main(String[] args) {
		LocalParameters.loadProxySettings();
		System.out.println( geocode( new GeocodingAddress(null, null, "Rue Alphonse Weicker", "Luxembourg", "LU", null)) );
	}
*/

}
