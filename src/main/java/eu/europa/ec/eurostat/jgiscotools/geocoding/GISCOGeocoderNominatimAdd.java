/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.geocoding;

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
public class GISCOGeocoderNominatimAdd extends Geocoder {

	private GISCOGeocoderNominatimAdd() {}
	private static final GISCOGeocoderNominatimAdd OBJ = new GISCOGeocoderNominatimAdd();
	/** @return the instance. */
	public static GISCOGeocoderNominatimAdd get() { return OBJ; }

	protected String toQueryURL(GeocodingAddress ad) {
		try {
			//TODO http(s)://europa.eu/webtools/rest/gisco/nominatim/search.php?q=berlin

			String query = "";

			if(ad.street != null)
				query += "&street=" + ad.street;
			else {
				String street = "";
				if(ad.housenumber != null)
					street += ad.housenumber + (ad.streetname != null? " ":"");
				if(ad.streetname != null)
					street += ad.streetname;
				if(!street.equals(""))
					query += "&street=" + street;
			}
			if(ad.postalcode != null)
				query += "&postalcode=" + ad.postalcode;
			if(ad.city != null)
				query += "&city=" + ad.city;
			if(ad.countryCode != null) {
				query += "&countrycode=" + ad.countryCode;
				query += "&country=" + ad.getCountryName();
			}

			query = URLEncoder.encode(query, "UTF-8");

			return "https://europa.eu/webtools/rest/gisco/nominatim/search?addressdetails=1" + query + "&polygon=0&viewbox=&format=json&limit=1";

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}


	protected GeocodingResult decodeResult(String queryResult) {
		//TODO better - get quality indicator
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

		//TODO add quality indicator ?
		gr.quality = -1;

		return gr;
	}

}
