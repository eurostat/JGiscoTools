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
public class GISCOGeocoderNominatimDetail extends Geocoder {

	private GISCOGeocoderNominatimDetail() {}
	private static final GISCOGeocoderNominatimDetail OBJ = new GISCOGeocoderNominatimDetail();
	/** @return the instance. */
	public static GISCOGeocoderNominatimDetail get() { return OBJ; }

	protected String toQueryURL(GeocodingAddress ad) {
		try {
			//version with https://europa.eu/webtools/rest/gisco/nominatim/search?addressdetails=1
			
			String query = "";

			if(ad.street != null)
				query += "&street=" + URLEncoder.encode(ad.street, "UTF-8");
			else {
				String street = "";
				if(ad.housenumber != null)
					street += ad.housenumber + (ad.streetname != null? " ":"");
				if(ad.streetname != null)
					street += ad.streetname;
				if(!street.equals(""))
					query += "&street=" + URLEncoder.encode(street, "UTF-8");
			}
			if(ad.postalcode != null)
				query += "&postalcode=" + URLEncoder.encode(ad.postalcode, "UTF-8");
			if(ad.city != null)
				query += "&city=" + URLEncoder.encode(ad.city, "UTF-8");
			if(ad.countryCode != null) {
				query += "&countrycode=" + URLEncoder.encode(ad.countryCode, "UTF-8");
				query += "&country=" + URLEncoder.encode(ad.countryName, "UTF-8");
			}

			//query = URLEncoder.encode(query, "UTF-8");

			return "https://europa.eu/webtools/rest/gisco/nominatim/search?addressdetails=1" + query + "&polygon=0&viewbox=&format=json&limit=1";

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}


	protected GeocodingResult decodeResult(String queryResult) {
		/*
		[{
		"place_id":32850086,
		"licence":"",
		"osm_type":"node",
		"osm_id":2802069236,
		"boundingbox":["50.8466259","50.8467259","5.6692597","5.6693597"],
		"lat":"50.8466759",
		"lon":"5.6693097",
		"display_name":"Chalans Maastricht, 100, Brouwersweg, Brusselsepoort, Maastricht, Limburg, Nederland, 6216EG, Nederland",
		"class":"amenity",
		"type":"dancing_school",
		"importance":0.5310000000000001,
		"address":{
			"address29":"Chalans Maastricht",
			"house_number":"100",
			"road":"Brouwersweg",
			"neighbourhood":"Brusselsepoort",
			"suburb":"Maastricht",
			"city":"Maastricht",
			"state":"Limburg",
			"postcode":"6216EG",
			"country":"Nederland",
			"country_code":"nl"}
		}]
		 */
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
