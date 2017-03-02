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

	private static final String BASE_PATH = "resources/NUTS/";

	public static ShapeFile get(){ return get("RG"); }
	public static ShapeFile get(String type){ return get(1, type); }
	public static ShapeFile get(int lod, String type){ return get(lod, "LAEA", type); }
	public static ShapeFile get(int lod, String proj, String type){ return get(2013, lod, proj, type); }

	/**
	 * @param year
	 * @param lod The level of detail, among 1, 3, 10, 20 or 60
	 * @param proj The projection, among LAEA and ETRS89
	 * @param type The object type, among RG,BN,LB,JOIN,SEPA
	 * @return
	 */
	public static ShapeFile get(int year, int lod, String proj, String type){
		return new ShapeFile(BASE_PATH + year + "/" + lod + "M/" + proj + "/" + type + ".shp");
	}


	private static Filter[] fLvl = null;
	public static Filter getFilterLvl(int lvl){
		if(fLvl == null)
			try {
				fLvl = new Filter[4];
				for(int i=0; i<=3; i++) fLvl[i] = CQL.toFilter("STAT_LEVL_ = "+i);
			} catch (CQLException e) { e.printStackTrace(); }

		return fLvl[lvl];
	}

}
