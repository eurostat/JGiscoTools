/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.geocoding;

import java.net.URLEncoder;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
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
			//version with https://europa.eu/webtools/rest/gisco/api?q=
			String query = "";

			if(ad.street != null)
				query += ad.street;
			else {
				String street = "";
				if(ad.housenumber != null)
					street += ad.housenumber + (ad.streetname != null? " ":"");
				if(ad.streetname != null)
					street += ad.streetname;
				if(!street.equals(""))
					query += street;
			}

			if(ad.postalcode != null)
				query += ", " + ad.postalcode;
			if(ad.city != null)
				query += " " + ad.city;
			if(ad.countryCode != null)
				query += ", " + ad.getCountryName();

			query = URLEncoder.encode(query, "UTF-8");

			return "https://europa.eu/webtools/rest/gisco/api?q=" + query + "&limit=1";

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	protected GeocodingResult decodeResult(String queryResult) {
		GeocodingResult gr = new GeocodingResult();
		try {
			JSONObject json = (JSONObject) new JSONParser().parse(queryResult);
			JSONArray fs = (JSONArray)json.get("features");
			if(fs.size() == 0) {
				gr.position = new Coordinate(0,0);
				gr.quality = 3;
				return gr;
			}
			JSONObject f = (JSONObject)fs.get(0);
			JSONArray c = (JSONArray)((JSONObject)f.get("geometry")).get("coordinates");
			gr.position = new Coordinate(
					Double.parseDouble(c.get(0).toString()),
					Double.parseDouble(c.get(1).toString())
					);
			gr.quality = -1;
		} catch (ParseException e) {
			System.err.println("Could not parse JSON: " + queryResult);
			e.printStackTrace();
		}
		return gr;
	}

}
