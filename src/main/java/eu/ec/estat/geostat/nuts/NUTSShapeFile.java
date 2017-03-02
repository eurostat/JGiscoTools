/**
 * 
 */
package eu.ec.estat.geostat.nuts;

import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.opengis.filter.Filter;

import eu.ec.estat.geostat.io.ShapeFile;

/**
 * @author julien Gaffuri
 *
 */
public class NUTSShapeFile {
	//TODO handle LoD - can be 1, 3, 10, 20 or 60

	private static final String BASE_PATH = "resources/nuts_2013_shp_laea/";

	private static ShapeFile shpFileNUTS = null;
	public static ShapeFile getShpFileNUTS(){
		if(shpFileNUTS == null){
			shpFileNUTS = new ShapeFile(BASE_PATH + "NUTS_RG_01M_2013_LAEA.shp");
		}
		return shpFileNUTS;
	}


	private static Filter[] fLvl = null;
	public static Filter getFilterLvl(int lvl){
		if(fLvl == null)
			try {
				fLvl = new Filter[]{
						CQL.toFilter("STAT_LEVL_ = 0"),
						CQL.toFilter("STAT_LEVL_ = 1"),
						CQL.toFilter("STAT_LEVL_ = 2"),
						CQL.toFilter("STAT_LEVL_ = 3")
				};
			} catch (CQLException e) { e.printStackTrace(); }

		return fLvl[lvl];
	}

}
