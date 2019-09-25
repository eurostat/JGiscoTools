/**
 * 
 */
package eu.europa.ec.eurostat.eurogeostat.cntr;

import java.util.ArrayList;

import org.locationtech.jts.geom.Geometry;
import org.opencarto.datamodel.Feature;
import org.opencarto.io.SHPUtil;

/**
 * @author julien Gaffuri
 *
 */
public class CountriesUtil {

	public static Geometry getEuropeMask() {
		return SHPUtil.loadSHP("./resources/CNTR/2016/1M/LAEA/Europe_RG_01M_2016_10km.shp").fs.iterator().next().getDefaultGeometry();
	}

	public static ArrayList<Feature> getEuropeanCountries() {
		return SHPUtil.loadSHP("./resources/CNTR/2016/1M/LAEA/CNTR_RG_01M_2016.shp").fs;
	}

}
