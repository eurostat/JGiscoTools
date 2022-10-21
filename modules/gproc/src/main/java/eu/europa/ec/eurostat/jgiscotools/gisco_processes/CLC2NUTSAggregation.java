/**
 * 
 */
package eu.europa.ec.eurostat.jgiscotools.gisco_processes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.filter.text.cql2.CQL;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.io.CSVUtil;
import eu.europa.ec.eurostat.jgiscotools.io.geo.GeoData;

/**
 * @author Julien Gaffuri
 *
 */
public class CLC2NUTSAggregation {
	private static Logger logger = LogManager.getLogger(CLC2NUTSAggregation.class.getName());

	//use: -Xms2G -Xmx12G
	public static void main(String[] args) throws Throwable {
		logger.info("Start");

		String nutsFile = "/home/juju/Bureau/gisco/geodata/gisco/GISCO.NUTS_RG_100K_2021_3035.gpkg";
		String clcFile = "/home/juju/Bureau/gisco/clc/u2018_clc2018_v2020_20u1_geoPackage/DATA/U2018_CLC2018_V2020_20u1.gpkg";

		logger.info("Load NUTS level 3");
		ArrayList<Feature> nuts = GeoData.getFeatures(nutsFile, "NUTS_ID", CQL.toFilter("STAT_LEVL_CODE='3'")); // AND SHAPE_AREA<0.01
		//[OBJECTID, SHAPE_LEN, STAT_LEVL_CODE, id, NUTS_ID, SHAPE_AREA]
		logger.info(nuts.size());

		Collection<Map<String, String>> out = new ArrayList<>();

		for(Feature f : nuts) {
			//Feature f = nuts.get(0);
			String nutsId = f.getID();
			logger.info(nutsId);
			Map<String, Double> d = getTemplate();

			Geometry g = f.getGeometry();
			Envelope env = g.getEnvelopeInternal();

			//load clcs using spatial index
			String filStr = "NOT(Code_18='523') AND BBOX(Shape,"+env.getMinX()+","+env.getMinY()+","+env.getMaxX()+","+env.getMaxY()+")";
			ArrayList<Feature> clcs = GeoData.getFeatures(clcFile, "U2018_CLC2018_V2020_20u1", "ID", CQL.toFilter(filStr));
			//logger.info(clc.size());

			for(Feature clc : clcs) {
				if(! env.intersects(clc.getGeometry().getEnvelopeInternal()))
					continue;
				//compute intersection
				Geometry inter = clc.getGeometry().intersection(g);
				double area = inter.getArea();
				if(area<=0) continue;

				//get code
				String code = clc.getAttribute("Code_18").toString();
				String aggCode = getAggCode(code);
				//logger.info("   "+code+"   "+area);
				//System.out.println(aggCode);

				//add contribution
				d.put(aggCode, d.get(aggCode) + area);
			}

			Map<String, String> d_ = new HashMap<>();
			for(Entry<String, Double> e : d.entrySet())
				d_.put(e.getKey(), (Math.floor(e.getValue()/10000)/100)+"");
			d_.put("NUTS_ID", nutsId);
			out.add(d_);
		}

		logger.info("Save CSV " + out.size());
		CSVUtil.save(out, "/home/juju/Bureau/gisco/clc/clc_nuts2021_lvl3_2018.csv");

		logger.info("End");
	}


	private static String getAggCode(String code) {

		String f = code.substring(0, 1);
		if("1".equals(f)) return "artif";
		if("4".equals(f) || "5".equals(f)) return "water";

		f = code.substring(0, 2);
		if("21".equals(f) || "22".equals(f)) return "agri";
		if("23".equals(f) || "24".equals(f)) return "past";
		if("31".equals(f)) return "forest";
		if("32".equals(f)) return "shrub";
		if("33".equals(f)) return "open";

		System.err.println("Unexpected CLC code: "+code);
		return null;
	}


	private static Map<String, Double> getTemplate() {
		Map<String, Double> d = new HashMap<>();
		d.put("artif", 0.0);
		d.put("agri", 0.0);
		d.put("past", 0.0);
		d.put("forest", 0.0);
		d.put("shrub", 0.0);
		d.put("open", 0.0);
		d.put("water", 0.0);
		return d;
	}

}
