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
public class GISCOGeocoderNominatimQuery extends Geocoder {

	private GISCOGeocoderNominatimQuery() {}
	private static final GISCOGeocoderNominatimQuery OBJ = new GISCOGeocoderNominatimQuery();
	/** @return the instance. */
	public static GISCOGeocoderNominatimQuery get() { return OBJ; }

	protected String toQueryURL(GeocodingAddress ad) {
		try {
			//version with: http(s)://europa.eu/webtools/rest/gisco/nominatim/search.php?q=

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

			return "https://europa.eu/webtools/rest/gisco/nominatim/search.php?q=" + query + "&polygon=0&viewbox=&format=json&limit=1";

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	protected GeocodingResult decodeResult(String queryResult) {
		GeocodingResult gr = new GeocodingResult();
		try {
			JSONArray arr = ((JSONArray)new JSONParser().parse(queryResult));
			if(arr.size() == 0) {
				gr.position = new Coordinate(0,0);
				gr.quality = 3;
				return gr;
			}
			JSONObject json = (JSONObject) arr.get(0);
			double lon = Double.parseDouble(json.get("lon").toString());
			double lat = Double.parseDouble(json.get("lat").toString());
			gr.position = new Coordinate(lon, lat);
			//TODO add quality indicator. base on importance?
			gr.quality = -1;
		} catch (ParseException e) {
			System.err.println("Could not parse JSON: " + queryResult);
			e.printStackTrace();
		}
		return gr;
	}

}
