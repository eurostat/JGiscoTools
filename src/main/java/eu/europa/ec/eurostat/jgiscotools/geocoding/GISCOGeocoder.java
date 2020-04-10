/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.geocoding;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.locationtech.jts.geom.Coordinate;

import eu.europa.ec.eurostat.jgiscotools.geocoding.base.Geocoder;
import eu.europa.ec.eurostat.jgiscotools.geocoding.base.GeocodingAddress;
import eu.europa.ec.eurostat.jgiscotools.geocoding.base.GeocodingResult;

/**
 * Some function to geocode data based on addresses
 * using GISCO geocoder.
 * 
 * @author clemoki
 *
 */
public class GISCOGeocoder extends Geocoder {

	private GISCOGeocoder() {}
	private static final GISCOGeocoder OBJ = new GISCOGeocoder();
	/** @return the instance. */
	public static GISCOGeocoder get() { return OBJ; }

	protected String toQueryURL(GeocodingAddress ad) {
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

			String url = "https://europa.eu/webtools/rest/gisco/nominatim/search?" + query + "&polygon=0&viewbox=&format=json&limit=1";
			//String url = "https://europa.eu/webtools/rest/gisco/api?q=";
			return url;

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}


	protected GeocodingResult decodeResult(String queryResult) {
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

}
