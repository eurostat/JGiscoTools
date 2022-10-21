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
import org.geotools.filter.text.cql2.CQLException;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opengis.filter.Filter;

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

		String basePath = "/home/juju/Bureau/gisco/";
		String nutsFile = basePath + "geodata/gisco/GISCO.NUTS_RG_100K_2021_3035.gpkg";
		String clcFile = basePath + "clc/u2018_clc2018_v2020_20u1_geoPackage/DATA/U2018_CLC2018_V2020_20u1.gpkg";

		for(int nutsLevel=3; nutsLevel>=0; nutsLevel--) {

			logger.info("Load NUTS level " + nutsLevel);
			ArrayList<Feature> nuts = GeoData.getFeatures(nutsFile, "NUTS_ID", CQL.toFilter("STAT_LEVL_CODE='"+nutsLevel+"'"
					//+" AND NOT(NUTS_ID LIKE 'UK%')"
					//+" AND NOT(NUTS_ID LIKE 'TR%')"
					));
			// AND NUTS_ID LIKE 'FR%'
			// AND SHAPE_AREA<0.01
			//[OBJECTID, SHAPE_LEN, STAT_LEVL_CODE, id, NUTS_ID, SHAPE_AREA]
			logger.info(nuts.size());

			//make geometries valid
			for(Feature f : nuts) {
				if(f.getGeometry().isValid()) continue;
				f.setGeometry(f.getGeometry().buffer(0));
				logger.warn(f.getID() + " not valid. Correction = "+f.getGeometry().isValid());
			}

			//prepare output data
			Collection<Map<String, String>> out = new ArrayList<>();

			//handle all nuts regions in parallel
			nuts.parallelStream().forEach(f -> {
				String nutsId = f.getID();
				logger.info(nutsId);

				Geometry g = f.getGeometry();
				Envelope env = g.getEnvelopeInternal();

				//load clcs whithin bbox, using spatial index
				String filStr = "NOT(Code_18='523') AND BBOX(Shape,"+env.getMinX()+","+env.getMinY()+","+env.getMaxX()+","+env.getMaxY()+")";
				Filter fil = null;
				try {fil = CQL.toFilter(filStr);	} catch (CQLException e1) {				e1.printStackTrace();	}
				ArrayList<Feature> clcs = GeoData.getFeatures(clcFile, "U2018_CLC2018_V2020_20u1", "ID", fil);
				//logger.info(clc.size());

				//compute contribution of each clc polygon
				Map<String, Double> d = getTemplate();
				for(Feature clc : clcs) {

					if(! env.intersects(clc.getGeometry().getEnvelopeInternal()))
						continue;

					//compute intersection
					Geometry inter = null;
					try {
						inter = clc.getGeometry().intersection(g);
					} catch (Exception e1) {
						logger.error("Problem with intersection computation - " + e1.getClass() + " for " + nutsId);
						Geometry clcG = clc.getGeometry().buffer(0);
						g = g.buffer(0);
						inter = clcG.intersection(g);
					}

					//compute area
					double area = inter.getArea();
					if(area <= 0) continue;

					//get clc code
					String code = clc.getAttribute("Code_18").toString();
					String aggCode = getAggCode(code);
					//logger.info("   "+code+"   "+area);
					//System.out.println(aggCode);

					//add contribution
					d.put(aggCode, d.get(aggCode) + area);
				}

				//add CSV line, reformated
				Map<String, String> d_ = new HashMap<>();
				for(Entry<String, Double> e : d.entrySet())
					d_.put(e.getKey(), (Math.floor(e.getValue()/10000)/100)+"");
				d_.put("NUTS_ID", nutsId);
				out.add(d_);
			});

			logger.info("Save CSV " + out.size());
			CSVUtil.save(out, "/home/juju/Bureau/gisco/clc/clc_nuts2021_lvl"+nutsLevel+"_2018.csv");
		}

		logger.info("End");
	}


	//return the clc code - after aggregation
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


	//prepare csv line
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
