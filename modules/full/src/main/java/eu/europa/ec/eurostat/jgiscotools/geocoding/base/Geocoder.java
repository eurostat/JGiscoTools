/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.geocoding.base;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author julien Gaffuri
 *
 */
public abstract class Geocoder {
	private final static Logger LOGGER = LogManager.getLogger(Geocoder.class.getName());

	//TODO try also!
	//https://geocode.xyz/api
	//https://mapmarker.com/
	//https://docs.mapbox.com/api/search/#geocoding
	//https://www.pitneybowes.com/us/location-intelligence/geocoding-data-enrichment/spectrum-geocoding-on-demand.html
	//ESRI geocoder: 	//https://developers.arcgis.com/rest/geocode/api-reference/overview-world-geocoding-service.htm
	//only US? http://geoservices.tamu.edu/Services/Geocode/WebService/

	/**
	 * Returns the geocoder URL for an address.
	 * 
	 * @param ga
	 * @return
	 */
	protected abstract String toQueryURL(GeocodingAddress ga);

	/**
	 * Decode the output of the geocoder.
	 * 
	 * @param queryResult
	 * @return
	 */
	protected abstract GeocodingResult decodeResult(String queryResult);

	/**
	 * Geocode from URL.
	 * 
	 * @param url
	 * @return
	 */
	private GeocodingResult geocodeURL(String url) {
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

	/**
	 * Geocode an address.
	 * 
	 * @param address
	 * @param printURLQuery
	 * @return
	 */
	public GeocodingResult geocode(GeocodingAddress address, boolean printURLQuery) {
		String url = toQueryURL(address);
		if(printURLQuery) LOGGER.info(url);
		return geocodeURL(url);
	}

}
