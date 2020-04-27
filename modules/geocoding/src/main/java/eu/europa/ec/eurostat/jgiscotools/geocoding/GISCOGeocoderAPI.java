/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.geocoding;

import java.net.URLEncoder;

import org.json.JSONArray;
import org.json.JSONObject;
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
public class GISCOGeocoderAPI extends Geocoder {

	private GISCOGeocoderAPI() {}
	private static final GISCOGeocoderAPI OBJ = new GISCOGeocoderAPI();
	/** @return the instance. */
	public static GISCOGeocoderAPI get() { return OBJ; }

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
				query += ", " + ad.countryName;

			query = URLEncoder.encode(query, "UTF-8");

			return "https://europa.eu/webtools/rest/gisco/api?q=" + query + "&limit=1";

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	protected GeocodingResult decodeResult(String queryResult) {
		GeocodingResult gr = new GeocodingResult();
		JSONObject json = (JSONObject) new JSONObject(queryResult);
		JSONArray fs = (JSONArray)json.get("features");
		if(fs.length() == 0) {
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
		return gr;
	}

}
