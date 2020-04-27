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
 * @author clemoki
 *
 */
public class BingGeocoder extends Geocoder {

	//TODO add key in constructor?
	public static String key = null;

	//https://docs.microsoft.com/en-us/bingmaps/rest-services/locations/find-a-location-by-address

	//structured version
	//http://dev.virtualearth.net/REST/v1/Locations?
	//unstructured version
	//http://dev.virtualearth.net/REST/v1/Locations/{locationQuery}?includeNeighborhood={includeNeighborhood}&maxResults={maxResults}&include={includeValue}&key={BingMapsAPIKey}

	private BingGeocoder() {}
	private static final BingGeocoder OBJ = new BingGeocoder();
	/** @return the instance. */
	public static BingGeocoder get() { return OBJ; }

	protected String toQueryURL(GeocodingAddress ad) {
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

			String url = "http://dev.virtualearth.net/REST/v1/Locations?" + query + "&maxResults=1&key=" + key;

			return url;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	protected GeocodingResult decodeResult(String queryResult) {

		String[] parts = queryResult.split("\"type\":\"Point\",\"coordinates\":\\[");
		String s = parts[1];
		parts = s.split("\\]},");
		s = parts[0];
		parts = s.split(",");
		double lat = Double.parseDouble(parts[0]);
		double lon = Double.parseDouble(parts[1]);
		Coordinate c = new Coordinate(lon, lat);

		GeocodingResult gr = new GeocodingResult();
		gr.position = c;

		//"matchCodes":["Good"]}]}],
		//Good Ambiguous UpHierarchy
		parts = queryResult.split("matchCodes\":\\[\"");
		s = parts[1];
		parts = s.split("\"\\]");
		gr.matching = parts[0];


		//"confidence":"High",
		//High Medium Low
		parts = queryResult.split("confidence\":\"");
		s = parts[1];
		parts = s.split("\",");
		gr.confidence = parts[0];

		if(gr.confidence.equals("High") && gr.matching.equals("Good")) gr.quality = 1;
		else if(gr.confidence.equals("Low")) gr.quality = 3;
		else gr.quality = 2;

		return gr;
	}

}
