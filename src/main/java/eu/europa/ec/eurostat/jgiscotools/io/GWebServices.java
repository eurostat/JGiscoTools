package eu.europa.ec.eurostat.jgiscotools.io;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.httpclient.util.URIUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import eu.europa.ec.eurostat.java4eurostat.util.Util;
import eu.europa.ec.eurostat.jgiscotools.gisco_processes.services.ProxySetter;
import eu.europa.ec.eurostat.jgiscotools.io.web.HTTPUtil;

/**
 * @author julien Gaffuri
 *
 */
public class GWebServices {
	final static Logger LOGGER = LogManager.getLogger(GWebServices.class.getName());

	public static String gKey = ProxySetter.get("google_API_key");
	public static String cs = ProxySetter.get("google_API_cx");

	/**
	 * Get the URL of the first website returned by a query
	 * 
	 * @param searchQuery
	 * @return
	 */
	public static String getURL(String searchQuery) {
		try {
			URLConnection conn = new URL("https://www.googleapis.com/customsearch/v1?key="+gKey+"&cx="+cs+"&q="+URIUtil.encodeQuery(searchQuery)).openConnection();
			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			JSONObject jsonObject = (JSONObject) new JSONParser().parse(in);
			JSONArray res = (JSONArray) jsonObject.get("items");

			if(res.size()==0){
				LOGGER.warn("   No site found for: "+searchQuery);
				return null;
			}

			return (String) ((JSONObject) res.iterator().next()).get("link");
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}



	/**
	 * Result of a geocoding query
	 * 
	 * @author Julien Gaffuri
	 */
	public static class GMapGeocodingResult {
		//OK,OVER_QUERY_LIMIT,ZERO_RESULTS
		public String status;
		//lon,lat
		public double[] pos; //lon, lat
		public boolean severalFound=false;
	}

	/**
	 * return geocoding from a query
	 * 
	 * @param searchQuery
	 * @return
	 */
	public static GMapGeocodingResult getLocation(String searchQuery) {
		try {
			URLConnection conn = new URL( "https://maps.googleapis.com/maps/api/place/textsearch/json?sensor=false&key="+gKey+"&query="+URIUtil.encodeQuery(searchQuery)).openConnection();
			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			JSONObject jsonObject = (JSONObject) new JSONParser().parse(in);

			GMapGeocodingResult lres = new GMapGeocodingResult();
			lres.status = (String) jsonObject.get("status");
			if(!"OK".equals(lres.status))
				return lres;

			JSONArray res = (JSONArray) jsonObject.get("results");

			if(res.size()>1){
				System.err.println("   Warning: several locations found for "+searchQuery);
				lres.severalFound=true;
			}

			JSONObject loc= (JSONObject)((JSONObject)((JSONObject) res.iterator().next()).get("geometry")).get("location");
			lres.pos = new double[]{((Number)loc.get("lng")).doubleValue(), ((Number)loc.get("lat")).doubleValue()};
			return lres;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * return geocoding from an address query
	 * 
	 * @param addressQuery
	 * @return
	 */
	public static GMapGeocodingResult getLocationFromAddress(String addressQuery) {
		try {
			URLConnection conn = new URL("http://maps.googleapis.com/maps/api/geocode/json?sensor=true&address="+URIUtil.encodeQuery(addressQuery)).openConnection();
			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			JSONObject jsonObject = (JSONObject) new JSONParser().parse(in);

			GMapGeocodingResult lres = new GMapGeocodingResult();
			lres.status = (String) jsonObject.get("status");
			if(!"OK".equals(lres.status))
				return lres;

			JSONArray res = (JSONArray) jsonObject.get("results");

			if(res.size()>1){
				System.err.println("   Warning: "+res.size()+" locations found for "+addressQuery);
				lres.severalFound=true;
			}

			JSONObject loc= (JSONObject)((JSONObject)((JSONObject) res.iterator().next()).get("geometry")).get("location");
			lres.pos = new double[]{((Number)loc.get("lng")).doubleValue(), ((Number)loc.get("lat")).doubleValue()};
			return lres;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}



	/**
	 * The value returned if the service failed
	 */
	public final static double NO_VALUE_RETURNED = -99999;

	/**
	 * The quota of point number per query (21 july 2010 is 2500)
	 * NB: "this limit may be changed in the future without notice"
	 * (http://code.google.com/intl/fr/apis/maps/documentation/elevation/#Limits)
	 */
	public static int GMAP_QUOTA = 2500;


	/**
	 * Get elevation values of a list of coordinate points from GMap web service
	 * 
	 * @param lats The latitude coordinates.
	 * @param lons The longitude coordinates.
	 * @return The elevation values.
	 */
	public static double[] getElevation(double[] lats, double[] lons) {
		return getElevation(lats, lons, false);
	}

	/**
	 * Retrieve the elevation at a position using googlemap web service
	 * 
	 * @param lat the latitude of the point
	 * @param lon the longitude of the point
	 * @param sensor true if the data comes from a sensor
	 * @return the elevation value (returns NO_VALUE_RETURNED in case of problem)
	 */
	public static double getElevation(double lat, final double lon, boolean sensor) {
		return getElevation(new double[]{lat}, new double[]{lon}, sensor)[0];
	}

	/**
	 * Retrieve the elevation at a position using googlemap web service
	 * 
	 * @param lat the latitude of the point
	 * @param lon the longitude of the point
	 * @return the elevation value (returns NO_VALUE_RETURNED in case of problem)
	 */
	public static double getElevation(double lat, final double lon) {
		return getElevation(lat, lon, false);
	}


	/**
	 * Get elevation values of a list of coordinate points from GMap web service
	 * 
	 * @param lats The latitude coordinates.
	 * @param lons The longitude coordinates.
	 * @param sensor See http://code.google.com/intl/fr/apis/maps/documentation/elevation/
	 * @return The elevation values.
	 */
	public static double[] getElevation(double[] lats, double[] lons, boolean sensor) {
		if( lats == null || lons == null ) {
			LOGGER.error("Null latitude or longitude table");
			return null;
		}
		if( lats.length != lons.length ) {
			LOGGER.error("Latitude and longitude tables have different sizes: " + lats.length + " and " + lons.length);
			return null;
		}
		if( lats.length == 0 ) return new double[0];
		if( lats.length > GMAP_QUOTA ) {
			LOGGER.error("Quota exceeded - limit value is " + GMAP_QUOTA);
			return new double[0];
		}

		double[] elevations = new double[lats.length];

		StringBuffer strb = new StringBuffer ("http://maps.google.com/maps/api/elevation/xml?locations=");
		for(int i=0; i<lats.length; i++) {
			if(i>0) strb.append("|");
			strb.append(Util.round(lats[i], 5));
			strb.append(",");
			strb.append(Util.round(lons[i], 5));
		}
		strb.append("&sensor=");
		strb.append(sensor);

		String url = strb.toString();
		if(LOGGER.isTraceEnabled()) LOGGER.trace(url);

		InputStream data = null;
		try {
			data = HTTPUtil.executeQuery(url);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		if(data==null) {
			LOGGER.warn("GMap query returned a null object");
			return null;
		}

		Document XMLdoc = XMLUtils.parse(data);
		if (XMLdoc == null) {
			LOGGER.warn("Failed to get altitude from google map web service: returned data is not XML.");
			return null;
		}

		/*
		Example of XML stream returned by the web service:
		<ElevationResponse>
			<status>OK</status>
			<result>
				<location>
					<lat>39.7381500</lat>
					<lng>-104.9747000</lng>
				</location>
				<elevation>1616.0150146</elevation>
			</result>
			<result>
				<location>
					<lat>36.4555560</lat>
					<lng>-116.8666670</lng>
				</location>
				<elevation>-50.7890358</elevation>
			</result>
			<result>
				<location>
					<lat>36.4700000</lat>
					<lng>-116.8800000</lng>
				</location>
				<elevation>-72.2511444</elevation>
			</result>
		</ElevationResponse>
		 */

		Element elevationResponseElt = (Element)XMLdoc.getElementsByTagName("ElevationResponse").item(0);
		if (elevationResponseElt == null) {
			LOGGER.warn("Failed to get altitude from google map web service: bad XML format.");
			return null;
		}

		Element statusElt = (Element)elevationResponseElt.getElementsByTagName("status").item(0);
		if (statusElt == null) {
			LOGGER.warn("Failed to get altitude from google map web service: bad XML format.");
			return null;
		}

		String status = statusElt.getFirstChild().getNodeValue();
		if( "OVER_QUERY_LIMIT".equalsIgnoreCase( status ) ) {
			//wait a while
			//try { Thread.sleep(101); } catch (InterruptedException e) {}
			//return getElevation(lats, lons, sensor);
			LOGGER.warn("Failed to get altitude from google map web service: quota exceeded - " + url);
			return null;
		}
		else if( "INVALID_REQUEST".equalsIgnoreCase( status ) ) {
			LOGGER.warn("Failed to get altitude from google map web service: invalid request - " + url);
			return null;
		}
		else if( ! "OK".equalsIgnoreCase( status ) ) {
			LOGGER.warn("Failed to get altitude from google map web service (status = " + status + " )");
			return null;
		}

		NodeList resList = elevationResponseElt.getElementsByTagName("result");
		for(int i=0; i<resList.getLength(); i++) {
			Element resultElt = (Element)resList.item(i);

			Element elevationElt = (Element)resultElt.getElementsByTagName("elevation").item(0);
			if (elevationElt == null) {
				elevations[i] = NO_VALUE_RETURNED;
				continue;
			}

			String str = elevationElt.getFirstChild().getNodeValue();
			if (str == null) {
				elevations[i] = NO_VALUE_RETURNED;
				continue;
			}
			elevations[i] =  Double.parseDouble( str );
		}
		return elevations;
	}



	public static GMapGeocodingResult findLocation(SimpleFeature f, String query) {
		return findLocation(f, query, null, null);
	}
	public static GMapGeocodingResult findLocation(SimpleFeature f, String query, String xAtt, String yAtt) {
		try {
			GMapGeocodingResult lres = getLocationFromAddress(query);
			//LocationResult lres = getLocation(query);
			if("OK".equals(lres.status)) {
				if(xAtt != null) f.setAttribute(xAtt, lres.pos[0]);
				if(yAtt != null) f.setAttribute(yAtt, lres.pos[1]);
				f.setAttribute("the_geom", new GeometryFactory().createPoint(new Coordinate(lres.pos[0], lres.pos[1])));
			} else if("ZERO_RESULTS".equals(lres.status)) {
				if(xAtt != null) f.setAttribute(xAtt, -1);
				if(yAtt != null) f.setAttribute(yAtt, -1);
			} else if("OVER_QUERY_LIMIT".equals(lres.status)) {
				LOGGER.info("   "+lres.status+" pause...");
				Thread.sleep(10000);
			} else {
				LOGGER.info("Not found: " + lres.status);
			}
			return lres;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}


	public static void main(String[] args) {
		ProxySetter.loadProxySettings();
	}

}
