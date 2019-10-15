/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools;

import java.util.HashMap;
import java.util.Set;

import eu.europa.ec.eurostat.java4eurostat.io.DicUtil;
import eu.europa.ec.eurostat.java4eurostat.util.Util;

/**
 * 
 * @author julien Gaffuri
 *
 */
public class GeoUtils {
	//Info from geo.dic file: http://ec.europa.eu/eurostat/estat-navtree-portlet-prod/BulkDownloadListing?sort=1&file=dic%2Fen%2Fgeo.dic
	//or use NUTS_AT_2013.csv


	/**
	 * Return if a geo code is an aggregate or not
	 * 
	 * @param geo
	 * @return
	 */
	public static boolean isAggregate(String geo){
		return "EU".equals(geo)||"EU28".equals(geo)||"EU27".equals(geo)
				||"EA".equals(geo)||"EA17".equals(geo)||"EA18".equals(geo)||"EA19".equals(geo)
				||"EEA".equals(geo);
	}


	/**
	 * Convert geo code
	 * 
	 * @param geo3
	 * @return
	 */
	public static String getCountryCode3to2(String geo3){
		if(countryCode3to2 == null) loadCountryCodeDicts();
		if(geo3.length() != 3) System.err.println("Unexpected country code: should have 3 characters: "+geo3);
		return countryCode3to2.get(geo3);
	}
	
	/**
	 * Convert geo code
	 * @param geo2
	 * @return
	 */
	public static String getCountryCode2to3(String geo2){
		if(countryCode2to3 == null) loadCountryCodeDicts();
		if(geo2.length() != 2) System.err.println("Unexpected country code: should have 2 characters: "+geo2);
		return countryCode2to3.get(geo2);
	}

	private static HashMap<String,String> countryCode3to2 = null;
	private static HashMap<String,String> countryCode2to3 = null;
	private static void loadCountryCodeDicts() {
		countryCode3to2 = DicUtil.load("resources/ctry_iso3_iso2.txt", ";");
		countryCode2to3 = Util.reverseMap(countryCode3to2);
	}

	private static Set<String> countryCodes2 = null;
	private static Set<String> countryCodes3 = null;
	public static Set<String> getCountryCodes2(){
		if(countryCodes2 == null){ loadCountryCodeDicts(); countryCodes2 = countryCode2to3.keySet(); }
		return countryCodes2;
	}
	public static Set<String> getCountryCodes3(){
		if(countryCodes3 == null){ loadCountryCodeDicts(); countryCodes3 = countryCode3to2.keySet(); }
		return countryCodes3;
	}

}
