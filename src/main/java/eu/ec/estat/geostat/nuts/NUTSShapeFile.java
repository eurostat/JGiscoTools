/**
 * 
 */
package eu.ec.estat.geostat.nuts;

import eu.ec.estat.geostat.io.ShapeFile;

/**
 * @author julien Gaffuri
 *
 */
public class NUTSShapeFile {

	private static final String BASE_PATH = "resources/nuts_2013_shp_laea/";

	private static ShapeFile shpFileNUTS = null;
	public static ShapeFile getShpFileNUTS(){
		if(shpFileNUTS == null){
			shpFileNUTS = new ShapeFile(BASE_PATH + "NUTS_RG_01M_2013_LAEA.shp");
		}
		return shpFileNUTS;
	}


}
