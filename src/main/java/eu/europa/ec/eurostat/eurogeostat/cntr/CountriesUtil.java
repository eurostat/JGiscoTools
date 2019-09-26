/**
 * 
 */
package eu.europa.ec.eurostat.eurogeostat.cntr;

import java.util.ArrayList;

import org.geotools.filter.text.cql2.CQL;
import org.locationtech.jts.geom.Geometry;
import org.opencarto.datamodel.Feature;
import org.opencarto.io.SHPUtil;

/**
 * @author julien Gaffuri
 *
 */
public class CountriesUtil {

	public static final String[] EuropeanCountryCodes = new String[] {"BE","BG","CZ","DK","DE","EE","IE","EL","ES","FR","HR","IT","CY","LV","LT","LU","HU","MT","NL","AT","PL","PT","RO","SI","SK","FI","SE","UK","IS","LI","NO","CH","ME","MK","AL","RS","TR"};

	public static Feature getCountry(String countryCode, String filePath) {
		try {
			ArrayList<Feature> fs = SHPUtil.loadSHP(filePath, CQL.toFilter("CNTR_ID = '"+countryCode+"'")).fs;
			if(fs.size() != 1) throw new Exception("Problem finding country with code: "+countryCode+". nb found="+fs.size());
			return fs.iterator().next();
		} catch (Exception e) { e.printStackTrace(); }
		return null;
	}

	public static Feature getCountry(String countryCode) {
		return getCountry(countryCode, "./src/main/resources/CNTR/CNTR_RG_01M_2016.shp");
	}

	public static Geometry getEuropeMask() {
		return SHPUtil.loadSHP("./resources/CNTR/2016/1M/LAEA/Europe_RG_01M_2016_10km.shp").fs.iterator().next().getDefaultGeometry();
	}

	public static ArrayList<Feature> getEuropeanCountries() {
		return SHPUtil.loadSHP("./resources/CNTR/2016/1M/LAEA/CNTR_RG_01M_2016.shp").fs;
	}

}
