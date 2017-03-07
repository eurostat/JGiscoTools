/**
 * 
 */
package eu.ec.estat.geostat.nuts;

import java.util.HashMap;

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

	public static ShapeFile get(){ return get("RG"); }
	public static ShapeFile get(String type){ return get(1, type); }
	public static ShapeFile get(int lod, String type){ return get(lod, "LAEA", type); }
	public static ShapeFile get(int lod, String proj, String type){ return get(2013, lod, proj, type); }


	//filters by level
	private static Filter[] filterByLevel = null;
	public static Filter getFilterByLevel(int lvl){
		if(filterByLevel == null)
			try {
				filterByLevel = new Filter[4];
				for(int i=0; i<=3; i++) filterByLevel[i] = CQL.toFilter("STAT_LEVL_ = "+i);
			} catch (CQLException e) { e.printStackTrace(); }

		return filterByLevel[lvl];
	}
	private static Filter[] filterByLevel_ = null;
	public static Filter getFilterByLevel_(int lvl){
		if(filterByLevel_ == null)
			try {
				filterByLevel_ = new Filter[4];
				for(int i=0; i<=3; i++) filterByLevel_[i] = CQL.toFilter("STAT_LEVL_ <= "+i);
			} catch (CQLException e) { e.printStackTrace(); }

		return filterByLevel_[lvl];
	}


	//filters for nuts boundaries
	//TODO
	//EU_FLAG CC_FLAG EFTA_FLAG OTHR_CNTR_ COAS_FLAG STAT_LEVEL_



	//filters for join and sepa, by lod
	private static HashMap<Integer,Filter> filterSepaJoinLoD = null;
	public static Filter getFilterSepaJoinLoD(int lod){
		if(filterSepaJoinLoD == null)
			try {
				filterSepaJoinLoD = new HashMap<Integer,Filter>();
				filterSepaJoinLoD.put(1, CQL.toFilter("MIN_SCAL >= 1000000"));
				filterSepaJoinLoD.put(3, CQL.toFilter("MIN_SCAL >= 3000000"));
				filterSepaJoinLoD.put(10, CQL.toFilter("MIN_SCAL >= 10000000"));
				filterSepaJoinLoD.put(20, CQL.toFilter("MIN_SCAL >= 20000000"));
				filterSepaJoinLoD.put(60, CQL.toFilter("MIN_SCAL >= 60000000"));
			} catch (CQLException e) { e.printStackTrace(); }
		return filterSepaJoinLoD.get(lod);
	}

}
