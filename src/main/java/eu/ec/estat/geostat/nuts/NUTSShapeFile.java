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
	public static ShapeFile get(int year, int lod, String proj, String type, Filter filter){
		return new ShapeFile(BASE_PATH + year + "/" + lod + "M/" + proj + "/" + type + ".shp", filter);
	}

	public static ShapeFile get(){ return get("RG"); }
	public static ShapeFile get(String type){ return get(1, type); }
	public static ShapeFile get(int lod, String type){ return get(lod, "LAEA", type); }
	public static ShapeFile get(int lod, String proj, String type){ return get(2013, lod, proj, type, null); }
	public static ShapeFile get(int lod, String proj, String type, Filter filter){ return get(2013, lod, proj, type, filter); }

	public static ShapeFile getRG(int lvl){ return getRG(1,"LAEA",lvl); }
	public static ShapeFile getRG(int lod, String proj, int lvl){ return getRG(lod, proj, getFilterByLevel(lvl)); }
	public static ShapeFile getRG(int lod, String proj, Filter filter){ return get(2013, lod, proj, "RG", filter); }

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
	//EU_FLAG, CC_FLAG, EFTA_FLAG, OTHR_CNTR_, COAS_FLAG. values are "T" or "F"
	private static HashMap<String,Filter> filtersBN_TRUE = new HashMap<String,Filter>();
	private static HashMap<String,Filter> filtersBN_FALSE = new HashMap<String,Filter>();
	public Filter getFilterBN(String property, boolean val){
		Filter f = (val?filtersBN_TRUE:filtersBN_FALSE).get(property);
		if(f==null){
			try { f = CQL.toFilter(property+" = "+(val?"TRUE":"FALSE")); } catch (CQLException e) { e.printStackTrace(); }
			(val?filtersBN_TRUE:filtersBN_FALSE).put(property,f);
		}
		return f;
	}



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






	/*
	 * For other countries
	 * RG: CNTR_ID - 2 letters country code
	 * BN: EU_FLAG, EFTA_FLAG, CC_FLAG, COAS_FLAG, OTHR_FLAG. F/T
	 * CAPT
	 * LB
	 */

	private static final String BASE_PATH_CNTR = "resources/CNTR/";

	/**
	 * @param year
	 * @param lod The level of detail, among 3, 10, 20 or 60
	 * @param proj The projection, among LAEA and ETRS89
	 * @param type The object type, among RG,BN,LB,CAPT
	 * @return
	 */
	public static ShapeFile getCNTR(int year, int lod, String proj, String type){
		return new ShapeFile(BASE_PATH_CNTR + year + "/" + lod + "M/" + proj + "/" + type + ".shp");
	}

	public static ShapeFile getCNTR(){ return getCNTR("RG"); }
	public static ShapeFile getCNTR(String type){ return getCNTR(3, type); }
	public static ShapeFile getCNTR(int lod, String type){ return getCNTR(lod, "LAEA", type); }
	public static ShapeFile getCNTR(int lod, String proj, String type){ return getCNTR(2014, lod==1?3:lod, proj, type); }

	//used to filter only neighbour countries
	public static final String CNTR_NEIG_CNTR = "CNTR_ID='SM' OR CNTR_ID='VA' OR CNTR_ID='AD' OR CNTR_ID='MC' OR CNTR_ID='LI' OR CNTR_ID='AX'"
			+ " OR CNTR_ID='BA' OR CNTR_ID='RS' OR CNTR_ID='AL' OR CNTR_ID='XK'"
			+ " OR CNTR_ID='RU' OR CNTR_ID='BY' OR CNTR_ID='UA' OR CNTR_ID='MD' OR CNTR_ID='KZ'"
			+ " OR CNTR_ID='GE' OR CNTR_ID='AM' OR CNTR_ID='AZ' OR CNTR_ID='IR' OR CNTR_ID='SY' OR CNTR_ID='LB'"
			+ " OR CNTR_ID='IQ' OR CNTR_ID='JO' OR CNTR_ID='SA' OR CNTR_ID='IL' OR CNTR_ID='PS'"
			+ " OR CNTR_ID='TN' OR CNTR_ID='DZ' OR CNTR_ID='MA' OR CNTR_ID='GI'"
			+ " OR CNTR_ID='FO' OR CNTR_ID='SJ' OR CNTR_ID='GL' OR CNTR_ID='GG' OR CNTR_ID='JE' OR CNTR_ID='IM'";



	private static final String BASE_PATH_GRATICULES = "resources/graticules/";
	public static ShapeFile getGraticules(){
		return new ShapeFile(BASE_PATH_GRATICULES + "graticules.shp");
	}
	public static final String GRATICULE_FILTER_5 = "degrees5=1";

}
