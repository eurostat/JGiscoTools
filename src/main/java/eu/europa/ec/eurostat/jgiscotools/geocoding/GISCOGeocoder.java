/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.geocoding;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;

import org.locationtech.jts.geom.Coordinate;

/**
 * Some function to geocode data based on addresses
 * using GISCO geocoder.
 * 
 * @author clemoki
 *
 */
public class GISCOGeocoder {

	private static String toQueryURL(GeocodingAddress ad) {
		try {
			String query = "";

			if(ad.city != null)
				query += "&city=" + URLEncoder.encode(ad.city, "UTF-8");
			if(ad.countryCode != null)
				query += "&country=" + URLEncoder.encode(ad.getCountryName(), "UTF-8");
			if(ad.postalcode != null)
				query += "&postalcode=" + URLEncoder.encode(ad.postalcode, "UTF-8");

			if(ad.street != null)
				query += "&street=" + URLEncoder.encode(ad.street, "UTF-8");
			else {
				String street = "";
				if(ad.housenumber != null)
					street += URLEncoder.encode(ad.housenumber, "UTF-8") + (ad.streetname != null? " ":"");
				if(ad.streetname != null)
					street += URLEncoder.encode(ad.streetname, "UTF-8");
				if(!street.equals(""))
					query += "&street=" + URLEncoder.encode(street, "UTF-8");
			}

			String url = "https://europa.eu/webtools/rest/gisco/nominatim/search?" + query + "&polygon=0&viewbox=&format=json&limit=2";
			//			String url = "http(s)://europa.eu/webtools/rest/gisco/api?q=";
			return url;

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}


	private static GeocodingResult decodeResult(String queryResult) {
		String[] parts = queryResult.split(",");
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
		GeocodingResult gr = new GeocodingResult();
		gr.position = c;

		//TODO add quality indicator

		return gr;
	}


	/**
	 * Geocode from URL.
	 * 
	 * @param url
	 * @return
	 */
	private static GeocodingResult geocodeURL(String url) {
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
			String queryResult = in.readLine();
			in.close();
			return decodeResult(queryResult);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static GeocodingResult geocode(GeocodingAddress address, boolean printURLQuery) {
		String url = toQueryURL(address);
		if(printURLQuery) System.out.println(url);
		return geocodeURL(url);
	}

}
